package com.cps.backend.modules.M03examassembly.entity;

import com.cps.backend.modules.M03examassembly.enums.ExamStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 考试实体。
 *
 * <p>对应数据表：{@code exam}。参考 02-Data-Dictionary.md §4.3 考试表 exam。</p>
 *
 * <p><b>字段说明要点</b>：</p>
 * <ul>
 *   <li>{@code exam} - 考试名称（v1.x 的 {@code name} 重命名以避免与关键字冲突）。</li>
 *   <li>{@code status} - 考试状态枚举：draft/publish/running/done（参考 §4.3.2 状态机）。</li>
 *   <li>{@code starttime} / {@code endtime} - ISO 8601 字符串存储（参考 SQLite-Optimization §6）。</li>
 *   <li>{@code questionSum} - JSON 快照，组卷时一次性写入；题目后续修改不影响已组卷考试（参考 §4.3.1、§8.3）。</li>
 * </ul>
 *
 * <p><b>使用约束</b>：</p>
 * <ul>
 *   <li>本 Entity 仅作为持久化映射，<b>禁止</b>作为 API 响应（参考 01-Global-Standards.md §4.1 J1）。</li>
 *   <li>本版本不引入 {@code @Version} 乐观锁（参考 02-Data-Dictionary.md §1.3）。</li>
 *   <li>{@code questionSum} 是快照：组卷后即使 {@code question} 表内容修改/删除，已组卷考试不受影响。</li>
 *   <li>{@code status} 字段查询时实时计算（参考 §4.3.2）；{@code publish}→{@code running}→{@code done} 由系统按时间窗自动判定。</li>
 * </ul>
 */
@Entity
@Table(name = "exam")
@Getter
@Setter
@NoArgsConstructor
public class Exam {

    /**
     * 主键，SQLite 自增 INTEGER。
     * <p>{@code columnDefinition = "INTEGER"} 强制告诉 Hibernate DDL 列类型为 INTEGER，
     * 满足 ddl-auto=validate 模式下与 {@code scripts/table_exam.sql} 元数据校验一致。
     * Java 端类型仍为 {@link Integer}（参考 02-Data-Dictionary.md §4.3）。</p>
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "INTEGER")
    private Integer id;

    /**
     * 考试名称。
     * <p>DB 列名与 Java 字段同名（均为 {@code exam}），无需额外 {@code @Column} 标注。</p>
     */
    @Column(name = "exam", nullable = false)
    private String exam;

    /** 考试状态枚举：draft/publish/running/done；状态机参考 §4.3.2 */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ExamStatus status;

    /**
     * 考试开始时间。
     * <p>SQLite 中以 ISO 8601 字符串存储（参考 SQLite-Optimization §6）。</p>
     * <p>Java 端类型为 {@link String}，与 DB TEXT 列类型一致，
     * 满足 ddl-auto=validate 模式下 schema 校验。</p>
     */
    @Column(name = "starttime", nullable = false, columnDefinition = "TEXT")
    private String starttime;

    /**
     * 考试结束时间。
     * <p>DB CHECK：{@code endtime > starttime}。</p>
     * <p>Java 端类型为 {@link String}，与 DB TEXT 列类型一致，
     * 满足 ddl-auto=validate 模式下 schema 校验。</p>
     */
    @Column(name = "endtime", nullable = false, columnDefinition = "TEXT")
    private String endtime;

    /**
     * 考试题目汇总，JSON 快照。
     * <p>结构参考 §4.3.1：包含 {@code version} / {@code items[]} / {@code totalQuestions} / {@code totalScore}。</p>
     * <p>Java 端以 {@link String} 接收，由 Service 层用 Jackson 序列化/反序列化。</p>
     * <p>Java 字段名 {@code questionSum} 与 DB 列名 {@code question_sum} 是 v2.0.0 唯一 snake_case ↔ camelCase 转换点（参考 §9）。</p>
     */
    @Column(name = "question_sum", nullable = false, columnDefinition = "TEXT")
    private String questionSum;
}
