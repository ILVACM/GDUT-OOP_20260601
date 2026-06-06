# 全局开发规范与 API 契约

> 本文档定义项目的统一 API 响应契约、全局异常处理规范、JPA 与数据库规范、代码分层标准、前端规范以及 SQLite 特殊约束。是所有编码任务的"硬约束基线"，与 [00-INDEX.md](file:///d:/GDUT-OOP_20260601/wiki/00-INDEX.md) §7 AI 协作边界具有同等强制力。

---

## 1. 技术栈硬约束

| 维度 | 约束 | 违反示例 |
|---|---|---|
| 后端语言 | 必须使用 JDK 21 特性（Record、Pattern Matching） | 使用 JDK 8 语法 |
| 后端 ORM | 仅允许 Spring Data JPA | 引入 MyBatis-Plus |
| 前端框架 | 仅允许 Vue 3 + `<script setup>` Composition API | 使用 Options API、Vue 2 |
| 前端状态 | 仅允许 Pinia | 使用 Vuex |
| 数据库 | 仅允许 SQLite（xerial 驱动） | 切换 MySQL / H2 |
| 包管理 | 后端 Maven、前端 npm | 混用 Gradle / yarn |

技术栈完整定义参见 [00-INDEX.md](file:///d:/GDUT-OOP_20260601/wiki/00-INDEX.md) §2。

---

## 2. 前后端 RESTful API 接口规范

### 2.1 URL 路径设计原则

- **基础路径**：所有业务接口必须以 `/api/v1/` 开头。
- **资源命名**：一律使用**小写、复数名词**，单词间用中划线 `-` 连接（如 `/api/v1/user-profiles`）。
- **层级限制**：URL 嵌套最多两层（如 `/api/v1/courses/{courseId}/students`），禁止更深层级。
- **动作特例**：非标准 CRUD 的复杂业务操作，允许使用 `POST /资源/动词`（如 `POST /api/v1/courses/{id}/publish`）。

### 2.2 HTTP 方法约定

| 方法 | 语义 | 场景示例 |
|---|---|---|
| `GET` | 查询 | `GET /api/v1/courses` (列表), `GET /api/v1/courses/1` (详情) |
| `POST` | 新增/复杂动作| `POST /api/v1/courses` (创建), `POST /api/v1/auth/login` (登录) |
| `PUT` | 修改 | `PUT /api/v1/courses/1` (更新课程信息) |
| `DELETE`| 删除 | `DELETE /api/v1/courses/1` (删除/下架课程) |

---

## 3. 统一 API 响应契约

### 3.1 `Result<T>` 统一返回类

**所有 Controller 方法的返回值必须为 `Result<T>`**，禁止直接返回 `Entity`、`Map`、`List` 或自定义结构。

```java
package com.cps.backend.common.api;

import lombok.Data;
import java.io.Serializable;

/**
 * 统一 API 响应包装器。
 * 任何 Controller 方法都禁止返回裸类型，必须包装为 Result<T>。
 */
@Data
public class Result<T> implements Serializable {

    /** 业务状态码：200 成功；4xx 业务异常；5xx 系统异常 */
    private int code;
    /** 人类可读的提示信息 */
    private String message;
    /** 业务数据载荷 */
    private T data;

    public static <T> Result<T> success() {
        return success(null);
    }

    public static <T> Result<T> success(T data) {
        Result<T> r = new Result<>();
        r.setCode(200);
        r.setMessage("success");
        r.setData(data);
        return r;
    }

    public static <T> Result<T> error(int code, String message) {
        Result<T> r = new Result<>();
        r.setCode(code);
        r.setMessage(message);
        r.setData(null);
        return r;
    }
}
```

### 3.2 响应格式

```json
// 成功响应
{ "code": 200, "message": "success", "data": { /* 业务数据 */ } }

// 业务失败响应
{ "code": 4001, "message": "用户名或密码错误", "data": null }

// 系统异常响应
{ "code": 5000, "message": "系统繁忙，请稍后重试", "data": null }
```

### 3.3 业务码约定

| 区间 | 含义 | 示例 |
|---|---|---|
| 200 | 成功 | — |
| 4000-4099 | 通用参数 / 请求错误 | 4000 参数校验失败 |
| 4100-4199 | 用户认证相关 | 4101 未登录；4102 Token 过期；4103 无权限 |
| 4200-4299 | 题库相关 | 4201 题目不存在 |
| 4300-4399 | 考试相关 | 4301 考试时间窗外 |
| 4400-4499 | 分数相关 | 4401 已提交答卷 |
| 5000-5099 | 系统异常 | 5000 内部错误 |

---

## 4. 全局异常处理

### 4.1 全局异常处理器

```java
package com.cps.backend.common.exception;

import com.cps.backend.common.api.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 业务异常：按异常中的 code 与 message 透传 */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusiness(BusinessException e) {
        log.warn("业务异常 code={} message={}", e.getCode(), e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    /** 参数校验失败 */
    @ExceptionHandler(IllegalArgumentException.class)
    public Result<Void> handleIllegalArgument(IllegalArgumentException e) {
        return Result.error(4000, e.getMessage());
    }

    /** 兜底异常：避免堆栈泄露给前端 */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleAny(Exception e) {
        log.error("系统异常", e);
        return Result.error(5000, "系统繁忙，请稍后重试");
    }
}
```

### 4.2 业务异常类

```java
public class BusinessException extends RuntimeException {
    private final int code;
    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }
    public int getCode() { return code; }
}
```

### 4.3 禁止事项

- **禁止** Controller 中 `try-catch` 后直接返回非 `Result` 结构
- **禁止** 异常处理器中返回 HTTP 500 状态码 + 堆栈信息
- **禁止** 在业务代码中抛出 `NullPointerException` 等未包装异常

---

## 5. JPA 规范

### 5.1 强制项

| 编号 | 规范 | 理由 |
|---|---|---|
| J1 | Entity 与 DTO **完全隔离** | 防 JSON 循环引用、防敏感字段泄露 |
| J2 | 涉及关联查询必须使用 `@EntityGraph` 或 `JOIN FETCH` | 防 N+1 查询 |
| J3 | 复杂统计使用 `@Query` JPQL，不在循环中调 Repository | 性能与可读性 |
| J4 | 所有 Entity 必须显式标注 `@Table(name = "...")` | 与数据字典表名一致 |
| J5 | 主键策略统一为 `IDENTITY`（SQLite 自增） | 与 §X SQLite 主键策略一致 |
| J6 | 涉及并发的实体（题库抽题、答题提交）必须使用 `@Version` 乐观锁 | 防超卖 / 重复提交 |

#### 5.1.1 已知例外（2026-06-04 起生效）

| 例外 | 说明 | 替代方案 |
|---|---|---|
| J6 乐观锁 | v3.0.0 4 张核心表均**不引入** `@Version` | 业务层互斥校验 + SQLite 串行写兜底（参考 [02-Data-Dictionary.md §1.3](file:///d:/GDUT-OOP_20260601/wiki/02-Data-Dictionary.md#13-v200-主动放弃的硬约束)） |
| SQLite 布尔字段 | `user.status` / `question.img` 使用 `Integer`（0/1）而非 `Boolean` | `ddl-auto=validate` 模式下 `Boolean` 映射为 `TINYINT` 与 DDL `INTEGER` 不一致（参考 [02-Data-Dictionary.md §1.4](file:///d:/GDUT-OOP_20260601/wiki/02-Data-Dictionary.md#14-v300-java-类型校准说明)） |
| 时间字段 | `exam.starttime` / `exam.endtime` 使用 `String`（ISO 8601）而非 `LocalDateTime` | Hibernate 社区方言对 `LocalDateTime` → TEXT 的自动转换在 `validate` 模式下存在类型推断差异（参考 [02-Data-Dictionary.md §1.4](file:///d:/GDUT-OOP_20260601/wiki/02-Data-Dictionary.md#14-v300-java-类型校准说明)） |
| Score 外键映射 | `score.user` / `score.exam` 使用 `@Column` 而非 `@JoinColumn` | 4 表独立设计（无 JPA `@ManyToOne` 关联），物理 FK 由 DDL 承载（参考 [02-Data-Dictionary.md §10 M4](file:///d:/GDUT-OOP_20260601/wiki/02-Data-Dictionary.md#10-jpa-实体映射约束)） |

### 5.2 Entity 示例

```java
@Entity
@Table(name = "user")  // 参考 02-Data-Dictionary.md §4.1
@Getter @Setter
@NoArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "INTEGER")
    private Integer id;

    @Column(nullable = false, unique = true, length = 50)
    private String name;

    @Column(nullable = false, length = 100)
    private String password;  // BCrypt 哈希

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private UserType type;

    @Column(name = "status", nullable = false, columnDefinition = "INTEGER")
    private Integer status;  // 0/1 整数，1=启用，0=禁用
}
```

### 5.3 DTO 隔离示例

```java
// 入参 DTO：仅包含创建/更新所需字段
public record UserCreateReq(
    @NotBlank String username,
    @NotBlank @Size(min = 6, max = 32) String password,
    @NotNull UserType userType
) {}

// 出参 VO：仅包含前端展示所需字段（不含 password 哈希等）
public record UserVO(
    Integer id,
    String name,
    UserType type,
    Integer status
) {}

// 转换发生在 Service 层
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    public UserVO create(UserCreateReq req) {
        // 业务校验、密码加密、持久化...
        User saved = userRepository.save(entity);
        return toVO(saved);  // Entity → VO 转换
    }
}
```

### 5.4 Repository 示例（含 N+1 防护）

```java
// v3.0.0 示例：4 张核心表均无外键关联（独立实体），故零 N+1 风险。
// 此处演示按状态 + 时间窗的常用查询写法。
public interface ExamRepository extends JpaRepository<Exam, Integer> {

    // 正确：使用 @EntityGraph 显式声明（v3.0.0 无关联实体，attributePaths 为空）
    @EntityGraph(attributePaths = {})
    List<Exam> findByStatus(ExamStatus status);

    // 正确：使用 JPQL 显式字段查询（v3.0.0：starttime/endtime 为 String 类型）
    @Query("SELECT e FROM Exam e WHERE e.starttime BETWEEN :from AND :to ORDER BY e.starttime ASC")
    List<Exam> findByTimeWindow(@Param("from") String from, @Param("to") String to);

    // 错误示例（v1.x 写法，v3.0.0 已删除 creator_id 字段，禁止使用）
    // @Query("SELECT e FROM Exam e JOIN FETCH e.questions WHERE e.creatorId = :creatorId")
    // List<Exam> findByCreatorIdWithQuestions(@Param("creatorId") Long creatorId);
}
```

---

## 6. 代码分层

### 6.1 后端分层

```
com.cps.backend
├── common/
│   ├── api/         # Result<T>, PageResult<T>          ← ✅ 已实现
│   ├── exception/   # BusinessException, GlobalExceptionHandler  ← ✅ 已实现
│   ├── config/      # WebMvcConfig（拦截器注册）           ← ✅ 已实现
│   └── security/    # JwtUtil, JwtAuthenticationInterceptor, @RequireRole  ← ✅ 已实现
├── modules/
│   ├── M01userauth/        # 包名（注意无横杠）
│   │   ├── controller/     # UserController              ← ✅ 已实现
│   │   ├── service/        # UserService                 ← ✅ 已实现
│   │   ├── repository/     # UserRepository              ← ✅ 已实现
│   │   ├── entity/         # User                        ← ✅ 已实现
│   │   ├── dto/            # 7 个 DTO（LoginReq/Resp, RegisterReq, UserVO, ChangePasswordReq, UserStatusReq, BatchDeleteReq）  ← ✅ 已实现
│   │   └── enums/          # UserType                    ← ✅ 已实现
│   ├── M02questionbank/
│   │   ├── controller/     # QuestionController          ← ✅ 已实现
│   │   ├── service/        # QuestionService             ← ✅ 已实现
│   │   ├── repository/     # QuestionRepository          ← ✅ 已实现
│   │   ├── entity/         # Question                    ← ✅ 已实现
│   │   ├── dto/            # 12 个 DTO（QuestionCreateReq/UpdateReq/QueryReq/VO, BatchImportResult, QuestionPreviewVO, Answer 多态 5 种+EssayAnswer）  ← ✅ 已实现
│   │   └── enums/          # QuestionType                ← ✅ 已实现
│   ├── M03examassembly/
│   │   ├── controller/     # ExamController              ← ✅ 已实现
│   │   ├── service/        # ExamService                 ← ✅ 已实现
│   │   ├── repository/     # ExamRepository              ← ✅ 已实现
│   │   ├── entity/         # Exam                        ← ✅ 已实现
│   │   ├── dto/            # 10 个 DTO（ExamCreateManualReq/AutoReq, ExamVO/ForStudentVO, ExamQuestionVO/ForStudentVO/ItemReq, QuestionSum/Item, AutoRule）  ← ✅ 已实现
│   │   └── enums/          # ExamStatus                  ← ✅ 已实现
│   └── M04scorestatistics/
│       ├── controller/     # ScoreController, DraftController  ← ✅ 已实现
│       ├── service/        # ScoreService, DraftCacheService   ← ✅ 已实现
│       ├── repository/     # ScoreRepository             ← ✅ 已实现
│       ├── entity/         # Score                       ← ✅ 已实现
│       ├── dto/            # 14 个 DTO（ExamSubmitReq, EssayGradeReq, AnswerItem, ScoreVO/ListVO/Detail, DetailItem/VO, Summary, UserExamHistoryVO, MistakeItemVO, ExamStatisticsVO, QuestionStatisticsVO）  ← ✅ 已实现
│       └── enums/          # （无独立枚举，复用其他模块）     ← ✅
└── BackendApplication.java  ← ✅ 已实现
```

> **当前实现状态**（2026-06-06）：后端全链路已完成 ✅——共 **71 个 Java 文件**、**41 个 Controller 端点**、**43 个 DTO/VO 类**、**73 个单元测试全部通过**（详见 [00-INDEX.md §9 测试结果](file:///d:/GDUT-OOP_20260601/wiki/00-INDEX.md#9-测试结果)）。

### 6.2 分层职责

| 层 | 职责 | 禁止事项 |
|---|---|---|
| Controller | 接收请求、参数校验、调用 Service、返回 `Result<T>` | 写业务逻辑、直接操作 Repository |
| Service | 业务逻辑、事务控制、Entity ↔ DTO 转换 | 直接返回 Entity |
| Repository | 数据库访问，仅做 CRUD 与查询 | 写业务逻辑 |
| Entity | 与数据字典表严格对应 | 作为 API 响应 |
| DTO | 跨层数据传输 | 反向依赖 Entity 注解 |

### 6.3 事务控制

- 写操作（创建 / 更新 / 删除）必须在 Service 方法上加 `@Transactional`
- 涉及多表写入的复杂业务（组卷、判分）需指定 `rollbackFor = Exception.class`
- 禁止在 Controller 上使用 `@Transactional`

---

## 7. 前端规范

### 7.1 项目结构

```
frontend/src/
├── api/             # 按模块划分的 Axios 调用文件（user.ts、question.ts...）
├── router/          # Vue Router 配置，含 beforeEach 权限守卫
├── stores/          # Pinia stores（userStore、examStore...）
├── views/           # 页面级组件
├── components/      # 通用组件
├── utils/           # 工具函数（request.ts Axios 封装等）
└── main.ts
```

### 7.2 强制项

| 编号 | 规范 | 理由 |
|---|---|---|
| F1 | 组件一律使用 `<script setup>` 语法 | 与 Vue 3 推荐一致 |
| F2 | Axios `baseURL` 一律配置为 `/api`，**严禁硬编码** `http://localhost:8080` | 跨环境零侵入 |
| F3 | Axios 必须配置 `response` 拦截器，**统一解包** `Result<T>`（取 `data` 字段） | 前端无需到处 `if (res.code === 200)` |
| F4 | Axios 必须配置 `request` 拦截器，自动注入 JWT Token | 统一鉴权 |
| F5 | 路由守卫（`router.beforeEach`）必须校验角色权限 | 防越权访问 |
| F6 | 表单提交按钮需做防抖（debounce） | 防重复提交 |

### 7.3 Axios 封装示例

```typescript
// src/utils/request.ts
import axios, { type AxiosInstance, type AxiosResponse } from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'

const instance: AxiosInstance = axios.create({
  baseURL: '/api',  // F2: 严禁硬编码后端地址
  timeout: 10000,
})

// 请求拦截：注入 Token
instance.interceptors.request.use((config) => {
  const userStore = useUserStore()
  if (userStore.token) {
    config.headers.Authorization = `Bearer ${userStore.token}`
  }
  return config
})

// 响应拦截：统一解包 Result<T>
instance.interceptors.response.use(
  (response: AxiosResponse) => {
    const { code, message, data } = response.data
    if (code === 200) return data
    ElMessage.error(message || '请求失败')
    return Promise.reject(new Error(message))
  },
  (error) => {
    ElMessage.error(error.message || '网络异常')
    return Promise.reject(error)
  }
)

export default instance
```

### 7.4 跨域处理

- **开发环境**：Vite Proxy（`vite.config.ts` 的 `server.proxy['/api']`）
- **生产环境**：Nginx 反向代理
- 禁止在后端 Controller 上加 `@CrossOrigin` 解决跨域

---

## 8. SQLite 特殊规范（概要）

SQLite 在 JPA 环境下有若干特殊行为，详细避坑指南见 [references/SQLite-Optimization.md](file:///d:/GDUT-OOP_20260601/wiki/references/SQLite-Optimization.md)。概要如下：

| 编号 | 规范 | 说明 |
|---|---|---|
| Q1 | 外键约束默认**关闭**，必须 `PRAGMA foreign_keys = ON` | 否则外键约束形同虚设 |
| Q2 | 启用 WAL 模式提升并发读性能 | `PRAGMA journal_mode = WAL` |
| Q3 | 主键策略使用 `IDENTITY`（SQLite 自增） | 避免 UUID 等额外类型 |
| Q4 | 避免长事务（> 1s），SQLite 写锁全局生效 | 高并发场景需限流 |
| Q5 | 数据库文件 `Data/*.sqlite` 必须加入 `.gitignore` | 防止二进制冲突 |
| Q6 | JPA 配置 `globally_quoted_identifiers=true` | 避免 `user` / `all` 等 SQL 关键字与 SQLite 列名冲突（实际配置已启用） |
| Q7 | SQLite 布尔字段使用 `Integer`（0/1）而非 `Boolean` | `ddl-auto=validate` 下 `Boolean` 映射 `TINYINT` 与 DDL `INTEGER` 不一致（详见 §5.1.1） |
| Q8 | 时间字段使用 `String`（ISO 8601）而非 `LocalDateTime` | Hibernate 社区方言类型推断差异（详见 §5.1.1） |

---

## 9. 注释与文档规范

### 9.1 关键代码注释

- 涉及 wiki 规范引用的代码需标注：

```java
// 参考 02-Data-Dictionary.md §3.2 题目表设计
// 参考 M02-Question-Bank.md §3 答案 JSON 扩展规范
```

### 9.2 Wiki 文档维护

- 任何对架构、字段、接口的修改**必须先更新 wiki**，再修改代码
- wiki 中的"TODO/待选型"标签需在版本记录中跟踪处置

---

## 10. 与 SOP.md 关系

项目内另存有 [temp/在线学习系统-全栈开发工作流 (SOP).md](file:///d:/GDUT-OOP_20260601/temp/在线学习系统-全栈开发工作流%20(SOP).md) 描述了阶段性开发工作流（数据设计→基础设施→后端→前端→部署）。该文件聚焦"何时做、按何顺序做"，本文件聚焦"做什么、怎么做、不能做什么"，两者互补不冲突。

---

## 11. 版本与变更记录

| 日期 | 版本 | 变更说明 |
|---|---|---|
| 2026-06-03 | v1.0.0 | 初始版本，定义 API 契约、全局异常处理、JPA 规范、代码分层、前端规范、SQLite 特殊规范 |
| 2026-06-04 | v1.1.0 | 新增 §5.1.1 已知例外（放弃 J6 乐观锁硬约束）；新增 Score 外键映射例外说明 |
| 2026-06-05 | v2.0.0 | 配合后端全链路完成校准：更新 §6.1 分层目录状态标注为已实现 |
| 2026-06-06 | **v3.0.0** | **Wiki 系统性校准**：① 精确 DTO 计数（M01:7, M02:12, M03:10, M04:14）；② 更新实现状态为精确数据（71 文件、41 端点、43 DTO、73 测试） |
