package com.cps.backend.modules.M01userauth.entity;

import com.cps.backend.modules.M01userauth.enums.UserType;
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
 * 用户实体。
 *
 * <p>对应数据表：{@code user}。参考 02-Data-Dictionary.md §4.1 用户表 user。</p>
 *
 * <p><b>使用约束</b>：</p>
 * <ul>
 *   <li>本 Entity 仅作为持久化映射，<b>禁止</b>作为 API 响应（参考 01-Global-Standards.md §4.1 J1）。</li>
 *   <li>本版本不引入 {@code @Version} 乐观锁（参考 02-Data-Dictionary.md §1.3 主动放弃的硬约束）。</li>
 *   <li>{@code name} 字段建议 UNIQUE，由 DB 层 {@code idx_user_name} 唯一索引强制。</li>
 *   <li>{@code password} 严禁明文存储，应用层必须 BCrypt 哈希后再写入。</li>
 * </ul>
 */
@Entity
@Table(name = "user")
@Getter
@Setter
@NoArgsConstructor
public class User {

    /**
     * 主键，SQLite 自增 INTEGER。
     * <p>{@code columnDefinition = "INTEGER"} 强制告诉 Hibernate DDL 列类型为 INTEGER，
     * 满足 ddl-auto=validate 模式下与 {@code scripts/table_user.sql} 元数据校验一致。
     * Java 端类型仍为 {@link Integer}（参考 02-Data-Dictionary.md §4.1）。</p>
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "INTEGER")
    private Integer id;

    /** 用户名；UNIQUE，避免重名；参考 §4.1 索引 idx_user_name */
    @Column(nullable = false, unique = true, length = 50)
    private String name;

    /** 登录密码（应用层 BCrypt 哈希，**严禁明文**） */
    @Column(nullable = false, length = 100)
    private String password;

    /** 用户类型枚举；TEXT 存储 {@code name()} 字面量：student/teacher/admin */
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private UserType type;

    /**
     * 用户状态，{@code 1} 启用，{@code 0} 禁用。
     * <p>SQLite 以 INTEGER(0/1) 存储布尔值，Java 端使用 {@link Integer}（参考 §10.1 M9）。</p>
     */
    @Column(name = "status", nullable = false, columnDefinition = "INTEGER")
    private Integer status;
}
