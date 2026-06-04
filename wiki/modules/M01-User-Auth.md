# M01 - 用户认证与权限管理模块

> 包含用户注册、登录、JWT Token 签发与校验、角色权限拦截等业务场景描述、API 接口定义及关键 DTO 设计。
>
> **数据来源**：[02-Data-Dictionary.md §4.1 用户表](file:///d:/GDUT-OOP_20260601/wiki/02-Data-Dictionary.md#41-用户表-user)
>
> **API 契约遵循**：[01-Global-Standards.md §2](file:///d:/GDUT-OOP_20260601/wiki/01-Global-Standards.md#2-统一-api-响应契约)

---

## 1. 业务场景

| 场景 | 描述 | 角色 |
|---|---|---|
| 用户注册 | 管理员创建账号或学生自助注册 | 管理员 / 学生自助 |
| 用户登录 | 校验 `name` + 密码，签发 JWT Token | 全角色 |
| 当前用户信息 | 从 Token 解析并返回登录用户 | 全角色 |
| 用户列表 | 分页查询、按 `type` 角色筛选 | 管理员 |
| 用户启用 / 禁用 | 修改 `status` 字段（`true` 启用 / `false` 禁用） | 管理员 |
| 角色权限拦截 | 不同角色访问不同前端路由 / 后端接口 | 全角色 |

---

## 2. 角色与权限矩阵

### 2.1 角色枚举（v2.0.0 简化）

```java
public enum UserType {
    student("学生"),
    teacher("老师"),
    admin("管理员");
}
```

### 2.2 权限矩阵

| 功能 | `student` | `teacher` | `admin` |
|---|---|---|---|
| 登录 | ✓ | ✓ | ✓ |
| 查看自己的分数 | ✓ | — | — |
| 查看所有分数 | — | ✓（仅自己出的考试） | ✓ |
| 题库 CRUD | — | ✓ | ✓ |
| 创建考试 | — | ✓ | ✓ |
| 阅卷 / 评卷 | — | ✓ | ✓ |
| 用户管理（CRUD） | — | — | ✓ |
| 数据统计报表 | ✓（仅自己） | ✓（自己出的考试） | ✓ |
| 系统配置 | — | — | ✓ |

> **v2.0.0 变化**：删除 `admin_subtype` 二级分类，`admin` 统一拥有全部后台权限。

---

## 3. 关键 DTO

### 3.1 请求 DTO

```java
public record LoginReq(@NotBlank String name, @NotBlank String password) {}
public record RegisterReq(
    @NotBlank @Size(min = 4, max = 30) String name,
    @NotBlank @Size(min = 6, max = 32) String password,
    @NotNull UserType type
) {}
public record ChangePasswordReq(
    @NotBlank String oldPassword,
    @NotBlank @Size(min = 6, max = 32) String newPassword
) {}
public record UserStatusReq(@NotNull Integer status) {}
```

### 3.2 响应 VO

```java
public record LoginResp(String token, UserVO user) {}
public record UserVO(Integer id, String name, UserType type, Integer status) {
    // 注：password 字段严禁出现在 VO 中
}
```

---

## 4. API 接口定义

| 方法 | 路径 | 鉴权 | 描述 | 请求 | 响应 |
|---|---|---|---|---|---|
| POST | `/api/auth/login` | 公开 | 登录 | `LoginReq` | `LoginResp` |
| POST | `/api/auth/logout` | 任意登录用户 | 注销 | — | `Result<Void>` |
| GET | `/api/auth/me` | 任意登录用户 | 当前用户 | — | `UserVO` |
| POST | `/api/auth/password` | 任意登录用户 | 修改自己的密码 | `ChangePasswordReq` | `Result<Void>` |
| GET | `/api/users` | admin | 分页查询用户 | `?page=0&size=20&type=student` | `PageResult<UserVO>` |
| POST | `/api/users` | admin | 创建用户 | `RegisterReq` | `UserVO` |
| PUT | `/api/users/{id}` | admin | 更新用户 | `RegisterReq` | `UserVO` |
| PATCH | `/api/users/{id}/status` | admin | 启用 / 禁用 | `UserStatusReq` | `Result<Void>` |
| DELETE | `/api/users/{id}` | admin | 删除用户 | — | `Result<Void>` |

---

## 5. JWT Token 规范

### 5.1 Token 载荷

```json
{ "sub": "1001", "name": "zhangsan", "type": "student", "iat": 1717400000, "exp": 1717486400 }
```

### 5.2 Token 配置

| 项 | 值 | 说明 |
|---|---|---|
| 算法 | HS256 | 对称加密 |
| 有效期 | 24 小时 | 长会话可考虑 Refresh Token（远期） |
| Header | `Authorization: Bearer <token>` | 符合 OAuth 2.0 风格 |

### 5.3 角色拦截

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireRole {
    UserType[] value();
}

@RequireRole(UserType.admin)
@DeleteMapping("/users/{id}")
public Result<Void> deleteUser(@PathVariable Long id) { ... }
```

---

## 6. 密码安全

- **加密算法**：BCrypt（`BCryptPasswordEncoder`）
- **强度要求**：最低 6 位
- **明文禁止**：DB 严禁明文；日志严禁打印密码

---

## 7. 业务规则

1. **用户名唯一性**：`name` 全局唯一（DB UNIQUE + Service 校验双保险）。
2. **admin 保护**：始终存在至少一个 `admin`；删除最后一个拒绝。
3. **自删除保护**：用户不能删除自己。
4. **删除用户级联**：删除前检查未结束考试 / 未批改答卷；如有则**禁用**而非删除。

---

## 8. 待办与扩展

| 编号 | 内容 | 优先级 |
|---|---|---|
| W1 | Refresh Token 机制 | 中 |
| W2 | 登录失败限流 | 中 |
| W3 | 第三方登录 | 低 |
| W4 | 操作日志 | 低 |
| W5 | 用户扩展属性 | 低 |

---

## 9. 实现状态（更新日期：2026-06-04）

> 本节追踪该模块各层代码的实际实现状态。

| 层级 | 实现状态 | 说明 |
|---|---|---|
| Entity | ✅ 已实现 | 字段与 02-Data-Dictionary.md 完全一致 |
| Enum | ✅ 已实现 | |
| Repository | ✅ 已实现 | 核心查询方法已实现 |
| Controller | ❌ 未实现 | API 端点尚未开发 |
| Service | ❌ 未实现 | 业务逻辑尚未开发 |
| DTO | ❌ 未实现 | 请求/响应 Record 尚未开发 |

---

## 10. 相关文档

- 数据定义：[02-Data-Dictionary.md §4.1 用户表](file:///d:/GDUT-OOP_20260601/wiki/02-Data-Dictionary.md#41-用户表-user)
- 枚举定义：[02-Data-Dictionary.md §5.1 UserType](file:///d:/GDUT-OOP_20260601/wiki/02-Data-Dictionary.md#51-usertype用户类型)
- 业务矩阵：[02-Data-Dictionary.md §7.1 用户管理](file:///d:/GDUT-OOP_20260601/wiki/02-Data-Dictionary.md#71-用户管理m01)
- 全局规范：[01-Global-Standards.md](file:///d:/GDUT-OOP_20260601/wiki/01-Global-Standards.md)
- 模块索引：[00-INDEX.md §5](file:///d:/GDUT-OOP_20260601/wiki/00-INDEX.md#5-模块索引)
