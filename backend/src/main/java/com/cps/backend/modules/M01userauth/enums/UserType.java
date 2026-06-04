package com.cps.backend.modules.M01userauth.enums;

import lombok.Getter;

/**
 * 用户类型枚举。
 * <p>
 * SQLite 中以字符串字面量存储（与 {@link #name()} 一致）。
 * 对应字段：{@code user.type}。
 * </p>
 *
 * <p>参考 02-Data-Dictionary.md §5.1 用户类型枚举。</p>
 */
@Getter
public enum UserType {

    /** 学生 */
    student("学生"),

    /** 教师 */
    teacher("老师"),

    /** 管理员 */
    admin("管理员");

    /** 人类可读的中文描述，便于前端展示 */
    private final String description;

    UserType(String description) {
        this.description = description;
    }
}
