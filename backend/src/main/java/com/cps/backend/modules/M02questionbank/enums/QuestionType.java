package com.cps.backend.modules.M02questionbank.enums;

import lombok.Getter;

/**
 * 题目类型枚举。
 * <p>
 * SQLite 中以字符串字面量存储（与 {@link #name()} 一致）。
 * 对应字段：{@code question.type}。
 * </p>
 *
 * <p>题型特性对比：</p>
 * <ul>
 *   <li>{@link #SingleChoice}   - 需要 options，系统自动判分</li>
 *   <li>{@link #MultipleChoice} - 需要 options，系统自动判分</li>
 *   <li>{@link #Judge}          - 无 options，系统自动判分</li>
 *   <li>{@link #Fill}           - 无 options，系统按空匹配自动判分</li>
 *   <li>{@link #Essay}          - 无 options，需教师评卷</li>
 * </ul>
 *
 * <p>参考 02-Data-Dictionary.md §5.2 题目类型枚举。</p>
 */
@Getter
public enum QuestionType {

    /** 单选题 */
    SingleChoice("单选题"),

    /** 多选题 */
    MultipleChoice("多选题"),

    /** 判断题 */
    Judge("判断题"),

    /** 填空题 */
    Fill("填空题"),

    /** 简答题 */
    Essay("简答题");

    private final String description;

    QuestionType(String description) {
        this.description = description;
    }
}
