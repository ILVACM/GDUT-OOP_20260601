package com.cps.backend.modules.M02questionbank.entity;

import com.cps.backend.modules.M02questionbank.enums.QuestionType;
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
 * 题目实体。
 *
 * <p>对应数据表：{@code question}。参考 02-Data-Dictionary.md §4.2 题目表 question。</p>
 *
 * <p><b>字段说明要点</b>：</p>
 * <ul>
 *   <li>{@code type} - 题目类型枚举，TEXT 存储 {@code name()} 字面量。</li>
 *   <li>{@code context} - 题干具体文本内容。</li>
 *   <li>{@code img} - 是否带图。{@code true}(1) 时按 id 在 {@code .\Data\img\{id}.{png|jpg|jpeg|gif}} 匹配图片（参考 §4.2.2）。</li>
 *   <li>{@code answer} - JSON 文本，按 type 自适应 5 种结构（参考 §4.2.1）。</li>
 *   <li>{@code use} - 组卷被抽中次数。组卷时 {@code use += 1}（参考 §4.2.3）。</li>
 *   <li>{@code correct} - 判分正确次数。<b>不变量</b>：{@code 0 <= correct <= use}（DB CHECK 保证）。</li>
 * </ul>
 *
 * <p><b>使用约束</b>：</p>
 * <ul>
 *   <li>本 Entity 仅作为持久化映射，<b>禁止</b>作为 API 响应（参考 01-Global-Standards.md §4.1 J1）。</li>
 *   <li>本版本不引入 {@code @Version} 乐观锁（参考 02-Data-Dictionary.md §1.3）。</li>
 *   <li>{@code use} / {@code correct} 自维护必须在 Service 层事务内同步写入（参考 M04 §7.5.1）。</li>
 *   <li>本表无物理外键；与 exam 通过 JSON 快照（{@code exam.question_sum}）逻辑引用。</li>
 *   <li>{@code use} / {@code correct} 的非负与大小关系由 DB CHECK 约束保证，DTO 层校验在后续业务任务中通过 {@code @Min}/{@code @Max} 实现。</li>
 * </ul>
 */
@Entity
@Table(name = "question")
@Getter
@Setter
@NoArgsConstructor
public class Question {

    /**
     * 主键，SQLite 自增 INTEGER。
     * <p>{@code columnDefinition = "INTEGER"} 强制告诉 Hibernate DDL 列类型为 INTEGER，
     * 满足 ddl-auto=validate 模式下与 {@code scripts/table_question.sql} 元数据校验一致。
     * Java 端类型仍为 {@link Integer}（参考 02-Data-Dictionary.md §4.2）。</p>
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "INTEGER")
    private Integer id;

    /** 题目类型枚举：SingleChoice/MultipleChoice/Judge/Fill/Essay */
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    private QuestionType type;

    /** 题干具体文本内容 */
    @Column(name = "context", nullable = false, columnDefinition = "TEXT")
    private String context;

    /**
     * 是否带图。
     * <p>{@code 1} 表示带图片，按 id 匹配 {@code .\Data\img\{id}.{ext}}（参考 §4.2.2）。</p>
     * <p>SQLite 以 INTEGER(0/1) 存储布尔值，Java 端使用 {@link Integer}（参考 §10.1 M9）。</p>
     */
    @Column(name = "img", nullable = false, columnDefinition = "INTEGER")
    private Integer img;

    /**
     * 题目答案 JSON 文本。
     * <p>按 {@code type} 自适应 5 种结构：单选/多选/判断/填空/简答（参考 §4.2.1）。</p>
     * <p>Java 端以 {@link String} 接收，由 Service 层用 Jackson 序列化/反序列化。</p>
     */
    @Column(name = "answer", nullable = false, columnDefinition = "TEXT")
    private String answer;

    /**
     * 被抽中次数。
     * <p>组卷时 {@code use += 1}（参考 §4.2.3）。DB CHECK：{@code use >= 0}。</p>
     */
    @Column(name = "use", nullable = false)
    private Integer use;

    /**
     * 被回答正确次数。
     * <p>学生判分正确时 {@code correct += 1}（参考 §4.2.3）。DB CHECK：{@code 0 <= correct <= use}。</p>
     */
    @Column(name = "correct", nullable = false)
    private Integer correct;
}
