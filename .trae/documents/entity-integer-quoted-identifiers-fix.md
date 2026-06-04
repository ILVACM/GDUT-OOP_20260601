# Entity INTEGER 字段类型映射 + 关键字转义修正计划

> 任务来源：用户修改意见汇总 §3（INTEGER 类型映射不匹配）+ §4（生产 yaml 缺 `globally_quoted_identifiers`）。
> 计划状态：**待用户确认执行**

---

## 1. 任务摘要（Summary）

修正 4 个 JPA Entity 中 `Long` 类型字段在 `ddl-auto: validate` 模式下的 Hibernate 元数据校验失败：
- SQLite DDL 中整数统一为 `INTEGER`（`scripts/table_*.sql`），但 Hibernate 默认把 `Long` 映射为 `BIGINT`，校验报 "wrong column type"。
- 涉及 **4 个 Entity、7 处字段**：4 个主键 `id` + Score 实体的 `user` 和 `exam` 物理外键。

同时补齐 `application.yaml`（生产）的 `hibernate.globally_quoted_identifiers: true`，与 `application-test.yaml` 对齐，规避 `score.all` 关键字冲突。

**严格不越界**：本任务**不**触动其他字段、注解、约束、测试、DDL 脚本；**不**引入新依赖；**不**改测试文件（现有 22 个测试在 ddl-auto=none 模式下与 columnDefinition 无关）。

---

## 2. 当前状态分析

### 2.1 已就绪（无需改动）

| 项 | 状态 |
|---|---|
| `application-test.yaml` | 已含 `globally_quoted_identifiers: true`（上轮修复） |
| `score.user` / `score.exam` 物理外键语义 | DDL + `@Column(name=...)` 已正确 |
| 4 张表的 DDL（`scripts/table_*.sql`） | `INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT` 已就位 |
| 22 个 Repository 单元测试 | 全部通过（ddl-auto=none，schema.sql 加载） |
| application.yaml 数据源路径 | 已修正为 `${user.dir}/../Data/English.sqlite` |
| `D:\GDUT-OOP_20260601\Data\` 目录 | 已创建 |

### 2.2 待修复

| # | 文件 | 字段 | 当前注解 | 目标注解 |
|---|---|---|---|---|
| 1 | `User.java` L37-39 | `id` | `@Id @GeneratedValue(...)` | 追加 `@Column(columnDefinition = "INTEGER")` |
| 2 | `Question.java` L47-50 | `id` | 同上 | 同上 |
| 3 | `Exam.java` L47-49 | `id` | 同上 | 同上 |
| 4 | `Score.java` L51-54 | `id` | 同上 | 同上 |
| 5 | `Score.java` L60-61 | `user` | `@Column(name = "user", nullable = false)` | 追加 `columnDefinition = "INTEGER"` |
| 6 | `Score.java` L67-68 | `exam` | `@Column(name = "exam", nullable = false)` | 追加 `columnDefinition = "INTEGER"` |
| 7 | `application.yaml` L42-46 | `hibernate.properties` | 缺 `globally_quoted_identifiers` | 在 `format_sql` 后追加该配置项 |

### 2.3 修复策略

`@Column(columnDefinition = "INTEGER")` 仅影响 Hibernate 元数据校验与 DDL 生成（validate 模式下仅校验），不影响运行时 SQL 生成。Long → BIGINT 仍是 Hibernate 推断的 SQL 类型，但 columnDefinition 会强制告诉 Hibernate "实际数据库类型是 INTEGER"，从而通过 validate 校验。

**与 `globally_quoted_identifiers: true` 的协同**：columnDefinition 不会包裹列名引号，但 globally_quoted_identifiers 会在所有 SQL 中给列名加双引号。两者互不冲突。

---

## 3. 详细变更（Proposed Changes）

### 3.1 User.java（1 处）

文件：[User.java](file:///d:/GDUT-OOP_20260601/backend/src/main/java/com/cps/backend/modules/M01userauth/entity/User.java) L37-39

**当前**：
```java
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;
```

**目标**：
```java
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
@Column(columnDefinition = "INTEGER")
private Long id;
```

**为什么**：SQLite `user.id` DDL 是 `INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT`，但 `Long` 默认映射为 `BIGINT`。`validate` 校验比对 SQL 字符串，不区分 `BIGINT` / `INTEGER`（Dialect 类型等价性），需要 `columnDefinition` 强制声明。

### 3.2 Question.java（1 处）

文件：[Question.java](file:///d:/GDUT-OOP_20260601/backend/src/main/java/com/cps/backend/modules/M02questionbank/entity/Question.java) L47-50

**当前**：
```java
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;
```

**目标**：与 3.1 相同的修改。

### 3.3 Exam.java（1 处）

文件：[Exam.java](file:///d:/GDUT-OOP_20260601/backend/src/main/java/com/cps/backend/modules/M03examassembly/entity/Exam.java) L47-49

**当前**：
```java
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;
```

**目标**：与 3.1 相同的修改。

### 3.4 Score.java（3 处）

文件：[Score.java](file:///d:/GDUT-OOP_20260601/backend/src/main/java/com/cps/backend/modules/M04scorestatistics/entity/Score.java) L51-68

**变更 1：L51-54 主键 `id`**
```java
// 之前
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;

// 之后
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
@Column(columnDefinition = "INTEGER")
private Long id;
```

**变更 2：L60-61 `user` 物理外键**
```java
// 之前
@Column(name = "user", nullable = false)
private Long user;

// 之后
@Column(name = "user", nullable = false, columnDefinition = "INTEGER")
private Long user;
```

**变更 3：L67-68 `exam` 物理外键**
```java
// 之前
@Column(name = "exam", nullable = false)
private Long exam;

// 之后
@Column(name = "exam", nullable = false, columnDefinition = "INTEGER")
private Long exam;
```

### 3.5 application.yaml（1 处）

文件：[application.yaml](file:///d:/GDUT-OOP_20260601/backend/src/main/resources/application.yaml) L38-46

**当前**：
```yaml
properties:
  hibernate:
    format_sql: true
    show_sql: false
    jdbc:
      time_zone: UTC
      batch_size: 30
    order_inserts: true
    order_updates: true
```

**目标**：在 `show_sql: false` 之后、`jdbc` 之前追加 `globally_quoted_identifiers: true`：

```yaml
properties:
  hibernate:
    format_sql: true
    show_sql: false
    # 对所有 SQL 标识符加双引号转义，规避 SQLite 关键字冲突（score.all 等）
    globally_quoted_identifiers: true
    jdbc:
      time_zone: UTC
      batch_size: 30
    order_inserts: true
    order_updates: true
```

**为什么**：与 `application-test.yaml` L47-48 保持一致；`score.all` 在生产运行 INSERT/UPDATE/SELECT 时同样需要双引号包裹。

---

## 4. 关键决策与不变量

| 决策 | 依据 |
|---|---|
| **不**采用 `@Type` 或自定义 `UserType` 注解 | `columnDefinition` 是 Hibernate 标准做法，影响面最小 |
| **不**改 DDL（`scripts/table_*.sql`） | Wiki 02-Data-Dictionary §4 规定 INTEGER，DDL 是权威 |
| **不**改 `application-test.yaml` | 已含 `globally_quoted_identifiers: true` |
| **不**新增测试 | 22 个测试已在 `ddl-auto=none` 下全部通过；`validate` 是生产模式，不在单测覆盖范围 |
| **不**改 Repository | Repository 层只关心 `Long` 字段，columnDefinition 对 JPQL 透明 |
| `columnDefinition` 仅作 validate 元数据 | 不影响 DDL 生成（生产 validate 不生成 DDL），不影响 SQL 运行 |

---

## 5. 验证步骤

| 步骤 | 命令 | 通过标准 |
|---|---|---|
| 1. 编译 | `cd backend && mvn -q -DskipTests compile` | exit 0 |
| 2. 测试 | `cd backend && mvn -q test` | exit 0，22 tests 全 pass |
| 3. 启动应用（生产模式，validate） | `cd backend && mvn spring-boot:run` | 应用启动成功无 `SchemaManagementException` |

**注意**：步骤 3 需要 `D:\GDUT-OOP_20260601\Data\English.sqlite` 已通过 `scripts/*.sql` 初始化（用户负责，不在本任务范围）。若该 DB 未建表，validate 会在第一次访问实体时抛 `Schema-validation: missing table [user]`，这是预期失败（与本次 Entity 修改无关）。

---

## 6. 风险与回滚

| 风险 | 缓解 |
|---|---|
| `columnDefinition` 与显式 `length` 冲突 | 7 处字段均为 `Long`/`INTEGER`（无 length），不会冲突 |
| `globally_quoted_identifiers` 误伤其他 SQL | 已在测试 profile 验证过（22 tests pass），生产再启用同配置无差异 |
| Score Entity 的 `user` 字段已加 `@UniqueConstraint` 注解 | `@Column` 修改与 `@Table` 的 `uniqueConstraints` 不在同一行，互不干扰 |
| 漏改某处导致 validate 部分通过 | 通过 `mvn test` + `mvn spring-boot:run` 双保险验证 |

---

## 7. 实施顺序

1. User.java 加 `@Column(columnDefinition = "INTEGER")`
2. Question.java 加 `@Column(columnDefinition = "INTEGER")`
3. Exam.java 加 `@Column(columnDefinition = "INTEGER")`
4. Score.java 加 3 处 `columnDefinition = "INTEGER"`
5. application.yaml 在 `show_sql` 后追加 `globally_quoted_identifiers: true`
6. `mvn compile` 验证
7. `mvn test` 验证 22 tests 全通过
