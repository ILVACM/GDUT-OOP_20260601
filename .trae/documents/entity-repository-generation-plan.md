# 实体类与 JPA 数据访问层生成计划

> 任务来源：根据 `wiki/02-Data-Dictionary.md v2.0.0` 在 `backend/src` 目录下创建 4 张核心表的 Entity、Repository、枚举及配套测试。
>
> 计划状态：**待用户确认执行**

---

## 1. 任务摘要（Summary）

在已存在的 Spring Boot 4.0.6 + JDK 21 + JPA + SQLite 后端骨架上，依据 `02-Data-Dictionary.md v2.0.0` 的 4 张核心表（`user` / `question` / `exam` / `score`）定义，对齐执行以下工作：

1. 补齐 `application.yaml`（生产 + 测试 profile 分离），使项目可启动、可单元测试。
2. 生成 3 个枚举：`UserType` / `QuestionType` / `ExamStatus`。
3. 生成 4 个 JPA Entity，字段、类型、约束严格对齐 §4 表。
4. 生成 4 个 `JpaRepository` 接口，遵循 Spring Data 命名规范。
5. 生成 4 个 Repository 单元测试（真实 SQLite，ddl-auto=create-drop）。
6. 验证 `mvn compile` 与 `mvn test` 通过。

**严格不越界**：本任务**不**创建 Service / Controller / DTO（Wiki 规范中"Entity 不能作为 API 响应"，DTO 属于后续业务任务）；**不**执行生产 DDL（脚本已存在 `scripts/*.sql`，由用户控制）；**不**引入新依赖。

---

## 2. 当前状态分析（Current State Analysis）

### 2.1 已就绪（无需改动）

| 项 | 路径 | 状态 |
|---|---|---|
| Maven 父工程 | `backend/pom.xml` | 已配 JDK 21、spring-data-jpa、sqlite-jdbc、lombok（runtime + test 全套） |
| 启动类 | `backend/src/main/java/com/cps/backend/BackendApplication.java` | `@SpringBootApplication`，无需改 |
| 上下文加载测试 | `backend/src/test/java/com/cps/backend/BackendApplicationTests.java` | 空 `@SpringBootTest`，需补 datasource 才能跑通 |
| 4 张表 DDL | `scripts/table_{user,question,exam,score}.sql` | 已包含 `CHECK` / `UNIQUE` / `FOREIGN KEY` |
| Wiki 权威规范 | `wiki/02-Data-Dictionary.md` | §4 字段级定义 / §5 枚举定义 / §10 实体映射约束 / §11 开放点 |
| Wiki 全局规范 | `wiki/01-Global-Standards.md` | §4 JPA 规范 J1-J6 / §5 分层 / §7 SQLite 概要 |
| SQLite 调优备忘 | `wiki/references/SQLite-Optimization.md` | §2 推荐 yaml / §3 PRAGMA 设置 |

### 2.2 待生成

- `application.yaml` 缺数据源 + JPA + PRAGMA。
- `application-test.yaml` 不存在。
- 4 个 Entity / 4 个 Repository / 3 个枚举 / 4 个 Repository 测试**全部为空**。

### 2.3 关键设计决策（计划阶段已固定）

| 决策点 | 取值 | 依据 |
|---|---|---|
| 主键策略 | `GenerationType.IDENTITY` | Wiki §10.1 实体示例 + J5 |
| 枚举存储 | `@Enumerated(EnumType.STRING)` + `length=20` | Wiki §10.1 + M8 |
| Boolean 字段 | Java 用 `Boolean`（包装类） | Wiki §10.1 M9 |
| JSON 字段 | `String` + `@Column(columnDefinition="TEXT")` | Wiki §4 + M5 |
| `score.user` / `score.exam` 物理外键字段注解 | `@Column(name="user"/"exam", nullable=false)` | **M3 "4 张表均独立" 优先于 §10.1 示例中的 `@JoinColumn`**（`Long` 不是实体对象，使用 `@JoinColumn` 违反 JPA 语义；物理外键语义由 DDL 的 `FOREIGN KEY` 承载） |
| `exam.questionSum` 字段命名 | Java 字段 `questionSum`，列 `question_sum` | Wiki §9 字段命名映射唯一例外 |
| `score.all` 字段 | Java 字段 `all`，列 `all`（双引号转义由 SQLite 处理） | Wiki §4.4 + §9 |
| 测试数据库 | 真实 SQLite 临时文件 `target/test-data/test.sqlite`，`ddl-auto=create-drop` | 用户已确认 |
| 生产数据库 | `Data/English.sqlite`，`ddl-auto=validate` | Wiki §10 SQLite-Optimization §2 |

### 2.4 与文档的潜在冲突处理

| 冲突点 | 文档 A | 文档 B | 本计划处置 |
|---|---|---|---|
| `score.user` 注解 | §4.4 字段定义用 `Long user` | §10.1 实体示例用 `@JoinColumn(name="user")` | **采用 `@Column(name="user")`**：`@JoinColumn` 必须配 `@ManyToOne` 实体关联；§2 ER 图与 M3 明确"4 张表独立"，物理外键由 DDL 承载。**实施时在 Entity 上加注释说明此判断**，并在 plan 备注中提示用户确认。 |
| `score.exam` 注解 | 同上 | 同上 | 同上 |
| 乐观锁 / 审计字段 | 01 §4.1 J6 / §5 强制 | 02 §1.3 主动放弃 | **不引入** `version` / `created_at` / `updated_at`（02-Data-Dictionary §1.3 优先） |

---

## 3. 目录与文件清单（Proposed Changes）

### 3.1 新建枚举（3 个）

| 文件 | 内容要点 |
|---|---|
| `backend/src/main/java/com/cps/backend/modules/M01userauth/enums/UserType.java` | `student` / `teacher` / `admin`，每项带 `description` |
| `backend/src/main/java/com/cps/backend/modules/M02questionbank/enums/QuestionType.java` | `SingleChoice` / `MultipleChoice` / `Judge` / `Fill` / `Essay` |
| `backend/src/main/java/com/cps/backend/modules/M03examassembly/enums/ExamStatus.java` | `draft` / `publish` / `running` / `done` |

> 三个枚举的字面量、构造器、`getDescription()` 完整照搬 `02-Data-Dictionary.md §5`，并补充 `// 参考 02-Data-Dictionary.md §5.x` 注释。

### 3.2 新建 Entity（4 个）

| 文件 | 表名 | 字段 | 关键注解 |
|---|---|---|---|
| `backend/src/main/java/com/cps/backend/modules/M01userauth/entity/User.java` | `user` | `id` / `name` / `password` / `type` / `status` | `@Table("user")` + 主键 IDENTITY + `name` UNIQUE + `type` 枚举 + `status` Boolean 包装 |
| `backend/src/main/java/com/cps/backend/modules/M02questionbank/entity/Question.java` | `question` | `id` / `type` / `context` / `img` / `answer` / `use` / `correct` | 枚举 + TEXT JSON + Boolean 包装 + 整数非负 |
| `backend/src/main/java/com/cps/backend/modules/M03examassembly/entity/Exam.java` | `exam` | `id` / `exam` / `status` / `starttime` / `endtime` / `questionSum` | 同名列名 `exam` / `@Column(name="question_sum")` / `LocalDateTime` |
| `backend/src/main/java/com/cps/backend/modules/M04scorestatistics/entity/Score.java` | `score` | `id` / `user` / `exam` / `all` / `detail` | `@Column(name="user")` / `@Column(name="exam")` / `@Column(name="all")` / JSON TEXT |

每个 Entity 头部注释：
- `// 参考 02-Data-Dictionary.md §4.x <表名> 表`
- 字段级注释：标注 Wiki 章节 + 业务含义 + 关键约束（如 `0 <= correct <= use`）
- 类级 `@Table` 显式标注

每个 Entity 末尾（Javadoc）需说明：
- "本 Entity 仅作为持久化映射，**禁止**作为 API 响应（参考 01-Global-Standards.md §4.1 J1）"
- "本版本不引入 `@Version` 乐观锁（参考 02-Data-Dictionary.md §1.3）"

### 3.3 新建 Repository（4 个）

| 文件 | 继承 | 关键方法 |
|---|---|---|
| `backend/src/main/java/com/cps/backend/modules/M01userauth/repository/UserRepository.java` | `JpaRepository<User, Long>` | `Optional<User> findByName(String)`、`List<User> findByTypeAndStatus(...)`、`boolean existsByName(String)` |
| `backend/src/main/java/com/cps/backend/modules/M02questionbank/repository/QuestionRepository.java` | `JpaRepository<Question, Long>` | `List<Question> findByType(QuestionType)`、`@Query` 按 `type` + `context LIKE` 关键字分页、`@Modifying` 自维护 `use`/`correct` |
| `backend/src/main/java/com/cps/backend/modules/M03examassembly/repository/ExamRepository.java` | `JpaRepository<Exam, Long>` | `List<Exam> findByStatus(ExamStatus)`、`@Query` 时间窗查询（参考 01-Global-Standards §4.4） |
| `backend/src/main/java/com/cps/backend/modules/M04scorestatistics/repository/ScoreRepository.java` | `JpaRepository<Score, Long>` | `Optional<Score> findByUserAndExam(Long, Long)`、`List<Score> findByUser(Long)`、`List<Score> findByExam(Long)`、`@Modifying` UPSERT 一人一考 |

> 所有 Repository 使用 `@EntityGraph(attributePaths = {})`（v2.0.0 4 表无关联，符合 01-Global-Standards §4.4 注释）；复杂查询使用 `@Query` JPQL；写操作加 `@Modifying` + `@Transactional`。

### 3.4 修改 application.yaml（生产）

文件路径：`backend/src/main/resources/application.yaml`

在现有 `spring.application.name: backend` 下补充：

```yaml
spring:
  datasource:
    url: jdbc:sqlite:${user.dir}/Data/English.sqlite
    driver-class-name: org.sqlite.JDBC
    hikari:
      maximum-pool-size: 5
      minimum-idle: 1
      connection-init-sql: |
        PRAGMA foreign_keys = ON;
        PRAGMA journal_mode = WAL;
        PRAGMA synchronous = NORMAL;
        PRAGMA busy_timeout = 5000;
        PRAGMA temp_store = MEMORY;
  jpa:
    database-platform: org.hibernate.community.dialect.SQLiteDialect
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        format_sql: true
        show_sql: false
        jdbc:
          time_zone: UTC
          batch_size: 30
          order_inserts: true
          order_updates: true
logging:
  level:
    org.hibernate.SQL: INFO
```

> **数据源/方言依据**：[SQLite-Optimization.md §2](file:///d:/GDUT-OOP_20260601/wiki/references/SQLite-Optimization.md)。
> **风险提示**：生产 `Data/English.sqlite` 当前可能未初始化（DDL 写在 `scripts/`，未执行）。本次任务**不主动执行** DDL，留在 `src/test/resources/schema-test.sql` 仅供测试使用。用户需在生产部署前手动执行 `scripts/table_*.sql`。

### 3.5 新建 application-test.yaml

文件路径：`backend/src/test/resources/application-test.yaml`

```yaml
spring:
  datasource:
    url: jdbc:sqlite:${project.build.directory}/test-data/test.sqlite
    driver-class-name: org.sqlite.JDBC
    hikari:
      maximum-pool-size: 1
      minimum-idle: 0
      connection-init-sql: |
        PRAGMA foreign_keys = ON;
        PRAGMA busy_timeout = 5000;
  jpa:
    database-platform: org.hibernate.community.dialect.SQLiteDialect
    hibernate:
      ddl-auto: create-drop
```

通过 `@ActiveProfiles("test")` 或测试类 `@SpringBootTest(properties = ...)` 切换。

### 3.6 新建测试 DDL

文件路径：`backend/src/test/resources/schema-test.sql`

将 `scripts/table_*.sql` 内容**复制整合**到单一文件，便于 `ddl-auto=create-drop` 前的回退校验（实际 ddl-auto 已接管创建，schema-test.sql 主要作为"测试模式应包含哪些表"的文档化留底，不被 JPA 自动加载）。也可以省略此文件，仅靠 ddl-auto 即可。

**决策**：不创建独立 `schema-test.sql`，直接让 `ddl-auto=create-drop` 基于 Entity 自动建表（仅限 test profile，生产 validate 仍依赖 `scripts/*.sql` 人工执行）。

### 3.7 新建单元测试（4 个）

| 文件 | 覆盖 | 关键断言 |
|---|---|---|
| `backend/src/test/java/com/cps/backend/modules/M01userauth/repository/UserRepositoryTest.java` | save / findById / findByName / existsByName / findByTypeAndStatus | `name` UNIQUE 触发异常、密码字段原样存（非明文转换） |
| `backend/src/test/java/com/cps/backend/modules/M02questionbank/repository/QuestionRepositoryTest.java` | save / findByType / 关键字 LIKE 分页 / `@Modifying` use+=1 | JSON 字段原样存取、`use` 自维护事务 |
| `backend/src/test/java/com/cps/backend/modules/M03examassembly/repository/ExamRepositoryTest.java` | save / findByStatus / 时间窗 JPQL | `questionSum` 字段原样存取、4 状态枚举往返一致 |
| `backend/src/test/java/com/cps/backend/modules/M04scorestatistics/repository/ScoreRepositoryTest.java` | save / findByUserAndExam / 物理外键约束 / 一人一考 UPSERT | 先插 user/exam 再插 score，物理外键生效；删 user/exam 应触发 `FOREIGN KEY constraint failed` |

测试注解统一：`@SpringBootTest` + `@ActiveProfiles("test")` + `@Transactional`（测试方法结束自动回滚，互不污染）。

**不**为枚举单独写测试（值枚举不写单测是行业惯例；如必要可后续补充，本次不增加范围）。

### 3.8 不创建的文件（明确范围）

- ❌ `common/api/Result.java`（用户没要求；后续 M01 任务会建）
- ❌ `common/exception/BusinessException.java` / `GlobalExceptionHandler.java`（同上）
- ❌ Service / Controller / DTO（任何业务层；属后续任务）
- ❌ `BackendApplicationTests.java` 的 datasource 修复（已用 `@ActiveProfiles("test")` 隔离，不影响主测试类；但 `BackendApplicationTests` 仍可能因缺生产 DB 跑挂——**决策**：在主测试类加 `@ActiveProfiles("test")` 切到测试 profile，避免默认加载生产 DB）

### 3.9 修改 BackendApplicationTests

文件路径：`backend/src/test/java/com/cps/backend/BackendApplicationTests.java`

补 `@ActiveProfiles("test")`，使上下文加载测试也走测试 SQLite，避免对生产 `Data/English.sqlite` 的强依赖。

---

## 4. 关键字段验证逻辑（实施细节）

虽然 Wiki 中"字段验证"主要指 Bean Validation（`@NotBlank` / `@Size`）写在 DTO 上，但本次任务不创建 DTO。在 Entity 层补一些"硬约束兜底"以防止误用：

| 字段 | 验证手段 | 理由 |
|---|---|---|
| `User.name` | `@Column(nullable=false, unique=true, length=50)` | 与 DDL 一致；UNIQUE 由 DB 强制 |
| `User.password` | `@Column(nullable=false, length=100)` | 长度容纳 BCrypt 哈希（60 字符） |
| `Question.use` | `@Column(nullable=false)` + `Integer`（包装类）+ `@Min(0)` | Wiki §4.2 CHECK `use >= 0` |
| `Question.correct` | `@Column(nullable=false)` + `@Min(0)` | Wiki §4.2 CHECK `correct >= 0` |
| `Score.all` | `@Column(nullable=false, name="all")` + `@Min(0)` | Wiki §4.4 CHECK `all >= 0` |
| 枚举字段 | `@Enumerated(EnumType.STRING)` + `@Column(nullable=false, length=20)` | Wiki §10.1 M8 |
| LocalDateTime 字段 | `Java 21` 默认 `LocalDateTime`，无注解 | Hibernate 6.x 自动 ISO 8601 ↔ TEXT |

> **关于"业务层字段验证"**：`@NotBlank` / `@Size` 等运行时校验通过 DTO 注解实现，符合 Wiki §4.1 J1 隔离原则。本次不写 DTO 也就无需这些注解；如要前置校验，由 Service 层在事务入口 `IllegalArgumentException` 兜底（参考 01-Global-Standards §3）。

---

## 5. 关键假设与决策（Assumptions & Decisions）

1. **数据源 SQLite 路径**：`${user.dir}/Data/English.sqlite` 直接复用 Wiki 推荐；用户可后续通过环境变量覆盖。
2. **测试 SQLite 路径**：`${project.build.directory}/test-data/test.sqlite`，`mvn clean` 自动清理。
3. **生产 DDL 不自动执行**：本次不引入 `data.sql` / Flyway；用户负责 `scripts/*.sql` 的执行时序。
4. **`@JoinColumn` vs `@Column` 决策**：采用 `@Column`，与 M3 "4 表独立" 一致；Entity 头部加 Javadoc 解释此决策。
5. **不做 `@SpringBootApplication` 包扫描冲突检查**：`BackendApplication` 已位于 `com.cps.backend` 根包，会自动扫到 `com.cps.backend.modules.*`。
6. **Score 实体的 `user` / `exam` 不创建 JPA 关联对象**（即不写 `private User user;`）：仅保留 `private Long user;`。这是 Wiki §2 ER 图与 M3 的明确要求。
7. **`@Modifying` 自维护写在 Repository**：参考 M04 §7.5.1 答题判分事务，`@Modifying @Query("UPDATE Question q SET q.use = q.use + 1 WHERE q.id = :id")` 仅作 Repository 层基础设施，**不**触发业务事务逻辑（业务编排后续在 Service 写）。

---

## 6. 验证步骤（Verification）

| 步骤 | 命令 | 通过标准 |
|---|---|---|
| 1. 编译 | `cd backend && ./mvnw -q -DskipTests compile`（或 `mvnw.cmd`） | exit 0，0 error |
| 2. 测试 | `cd backend && ./mvnw -q test` | exit 0，4 个 Repository 测试 + 上下文测试全部 PASS |
| 3. 启动主应用（人工） | `cd backend && ./mvnw spring-boot:run`（需 `Data/English.sqlite` 已建） | 应用启动成功并打印方言 / 启动时间 |
| 4. （可选）启动验证 | 启动后无 `SchemaManagementException` 报错 | 说明 `validate` 模式下 Entity 与 DDL 一致 |

**主风险点**：`mvnw` 在 Windows 上是 `.cmd` 后缀，需用 `mvnw.cmd`；当前 `backend/temp/mvnw` 与 `mvnw.cmd` 已存在（已在 `backend/temp/`，需确认是否在根目录可执行）。

---

## 7. 实施顺序（建议一次性按序执行）

1. 创建 3 个枚举
2. 创建 4 个 Entity
3. 创建 4 个 Repository
4. 修改 `application.yaml`（生产）
5. 新建 `application-test.yaml`（测试 profile）
6. 修改 `BackendApplicationTests.java`（加 `@ActiveProfiles("test")`）
7. 新建 4 个 Repository 测试
8. 跑 `mvn compile` → `mvn test`
9. 修复任何编译 / 测试错误

---

## 8. 风险与回滚

| 风险 | 缓解 |
|---|---|
| `Data/English.sqlite` 不存在导致 `BackendApplicationTests` 失败 | 已通过 `@ActiveProfiles("test")` 切到 test SQLite |
| Hibernate 6.5+ 对 SQLite 枚举 TEXT 长度敏感 | 已统一 `length=20/30`，按 §10.1 实体示例 |
| `score.user` 注解与 §10.1 示例冲突 | 采用 `@Column` 并在 Entity 头部 Javadoc 说明；用户后续若坚持 `@JoinColumn` 可一行替换 |
| `ddl-auto=validate` 在生产启动时若 DB 未建表会失败 | 文档已声明由用户负责执行 `scripts/*.sql`；本次不引入自动初始化 |
| Windows 长路径 / 中文路径 | 使用 `${user.dir}` 相对路径，与 Wiki 一致 |
