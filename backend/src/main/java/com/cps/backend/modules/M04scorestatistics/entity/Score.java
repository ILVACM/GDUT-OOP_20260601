package com.cps.backend.modules.M04scorestatistics.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 分数实体。
 *
 * <p>对应数据表：{@code score}。参考 02-Data-Dictionary.md §4.4 分数表 score。</p>
 *
 * <p><b>字段说明要点</b>：</p>
 * <ul>
 *   <li>{@code user} - 考生 id，<b>物理外键</b> → {@code user.id}（4 张表中仅有的两处物理外键之一）。</li>
 *   <li>{@code exam} - 考试 id，<b>物理外键</b> → {@code exam.id}。</li>
 *   <li>{@code all} - 总分（与 SQL 关键字 {@code ALL} 同名，DB 端双引号转义）。</li>
 *   <li>{@code detail} - JSON 文本，含逐题明细 + summary（参考 §4.4.1）。</li>
 * </ul>
 *
 * <p><b>关于 {@code @JoinColumn} vs {@code @Column} 的判断</b>：</p>
 * <p>本计划采用 {@code @Column(name="user")} / {@code @Column(name="exam")}，理由：</p>
 * <ol>
 *   <li>本类字段类型为 {@link Long}（非实体对象），使用 {@code @JoinColumn} 必须配 {@code @ManyToOne} 关联，与 §2 ER 图和 §10 M3"4 张表均独立"的设计冲突。</li>
 *   <li>物理外键语义由 DDL 的 {@code FOREIGN KEY} 约束承载（参考 scripts/table_score.sql）。</li>
 *   <li>若未来需要 JPA 实体关联，可平滑升级为 {@code @ManyToOne} + {@code @JoinColumn}。</li>
 * </ol>
 *
 * <p><b>使用约束</b>：</p>
 * <ul>
 *   <li>本 Entity 仅作为持久化映射，<b>禁止</b>作为 API 响应（参考 01-Global-Standards.md §4.1 J1）。</li>
 *   <li>本版本不引入 {@code @Version} 乐观锁（参考 02-Data-Dictionary.md §1.3）。</li>
 *   <li>UNIQUE({@code user}, {@code exam}) 保证一人一考仅一条记录；重复提交走 UPSERT（参考 §4.4 唯一约束）。</li>
 *   <li>删除 user/exam 前必须先清理本表（PRAGMA foreign_keys=ON 时触发 {@code FOREIGN KEY constraint failed}）。</li>
 *   <li>{@code all} 的非负由 DB CHECK 约束保证，DTO 层校验在后续业务任务中通过 {@code @Min(0)} 实现。</li>
 * </ul>
 */
@Entity
@Table(name = "score")
@Getter
@Setter
@NoArgsConstructor
public class Score {

    /**
     * 主键，SQLite 自增 INTEGER。
     * <p>{@code columnDefinition = "INTEGER"} 强制告诉 Hibernate DDL 列类型为 INTEGER，
     * 满足 ddl-auto=validate 模式下与 {@code scripts/table_score.sql} 元数据校验一致。
     * Java 端类型仍为 {@link Integer}（参考 02-Data-Dictionary.md §4.4）。</p>
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "INTEGER")
    private Integer id;

    /**
     * 考生 id，物理外键 → {@code user.id}。
     * <p>DB 列名与 Java 字段同名（均为 {@code user}），需显式标注（参考 §9 字段命名映射约定）。</p>
     * <p>{@code columnDefinition = "INTEGER"} 强制 DDL 列类型为 INTEGER，避免 Integer 默认映射的 validate 校验失败。</p>
     */
    @Column(name = "user", nullable = false, columnDefinition = "INTEGER")
    private Integer user;

    /**
     * 考试 id，物理外键 → {@code exam.id}。
     * <p>DB 列名与 Java 字段同名（均为 {@code exam}），需显式标注。</p>
     * <p>{@code columnDefinition = "INTEGER"} 强制 DDL 列类型为 INTEGER，避免 Integer 默认映射的 validate 校验失败。</p>
     */
    @Column(name = "exam", nullable = false, columnDefinition = "INTEGER")
    private Integer exam;

    /**
     * 总分。
     * <p>DB CHECK：{@code all >= 0}。</p>
     * <p>DB 列名 {@code all} 与 SQL 关键字同名，由 SQLite 双引号转义（参考 §9）。</p>
     */
    @Column(name = "all", nullable = false)
    private Integer all;

    /**
     * 答题明细 JSON 文本。
     * <p>结构参考 §4.4.1：含 {@code version} / {@code items[]} / {@code summary{}}。</p>
     * <p>Java 端以 {@link String} 接收，由 Service 层用 Jackson 序列化/反序列化。</p>
     */
    @Column(name = "detail", nullable = false, columnDefinition = "TEXT")
    private String detail;
}
