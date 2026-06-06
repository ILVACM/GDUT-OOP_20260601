# SQLite 性能优化与避坑备忘录

> 记录 SQLite 在 Spring Boot + JPA 环境下的特定优化策略，包括外键约束激活、WAL 模式开启、并发写入限制及 Hibernate 方言配置。同时给出本项目（英语在线学习系统）的具体配置建议。
>
> **全局约束对齐**：[01-Global-Standards.md §7](file:///d:/GDUT-OOP_20260601/wiki/01-Global-Standards.md#7-sqlite-特殊规范概要)
>
> **数据来源**：[02-Data-Dictionary.md](file:///d:/GDUT-OOP_20260601/wiki/02-Data-Dictionary.md)

---

## 1. SQLite 与其他数据库的关键差异

| 维度 | SQLite | MySQL/PostgreSQL |
|---|---|---|
| 部署形态 | 进程内文件型 | 独立服务进程 |
| 并发写 | 全局写锁（一次仅 1 个写事务） | 行级锁 |
| 外键默认 | **关闭** | 开启 |
| 事务隔离 | Serializable only | 多种级别 |
| 适用规模 | 中小规模（< 10 万 QPS） | 大规模 |

> **本项目选型原因**：课程作业规模小（题库数百到数千题、考生数十人）、单机部署、零运维。SQLite 是合理选型；不需要 MySQL/PG 的复杂度。

---

## 2. application.yaml 推荐配置

```yaml
spring:
  application:
    name: backend

  datasource:
    url: jdbc:sqlite:${user.dir}/../Data/English.sqlite
    driver-class-name: org.sqlite.JDBC
    hikari:
      # SQLite 单文件，连接池无需过大
      maximum-pool-size: 5
      minimum-idle: 1
      # 每次新建连接时执行的初始化 SQL
      connection-init-sql: |
        PRAGMA foreign_keys = ON;
        PRAGMA journal_mode = WAL;
        PRAGMA synchronous = NORMAL;
        PRAGMA busy_timeout = 5000;
        PRAGMA temp_store = MEMORY;

  jpa:
    database-platform: org.hibernate.community.dialect.SQLiteDialect
    hibernate:
      ddl-auto: validate   # 严禁使用 update/create, 必须先建表
    properties:
      hibernate:
        format_sql: true
        show_sql: false
        globally_quoted_identifiers: true
        jdbc:
          time_zone: UTC
          batch_size: 30
        order_inserts: true
        order_updates: true

logging:
  level:
    org.hibernate.SQL: INFO
```

> **强约束**：`ddl-auto: validate` 表示 JPA 启动时校验 Entity 与 DB 一致性，**不修改表结构**。所有 DDL 必须先在 `schema.sql` 编写并人工执行。

> **`globally_quoted_identifiers=true` 说明**：此配置使 Hibernate 在生成 SQL 时对所有标识符加双引号（如 `"user"` / `"all"`），避免 SQLite 中 `user` / `all` 等 SQL 保留字与列名冲突。本项目 `score.all` 和 `score.user`/`score.exam` 字段名与 SQL 关键字同名，**必须开启此配置**。

---

## 3. 关键 PRAGMA 设置详解

### 3.1 `PRAGMA foreign_keys = ON`（**必开**）

SQLite 默认**关闭外键约束**。不开启则 `score.user` 指向不存在的 `user.id` 也能写入。

```sql
PRAGMA foreign_keys = ON;
```

> **Spring Boot 中必须通过 HikariCP `connection-init-sql` 注入**（不是 `data.sql`），确保每个连接初始化时都生效。

### 3.2 `PRAGMA journal_mode = WAL`（强烈推荐）

WAL（Write-Ahead Logging）模式允许**读不阻塞写、写不阻塞读**，极大提升并发性能。

```sql
PRAGMA journal_mode = WAL;
```

启用后会生成 `Data/English.sqlite-wal` 和 `Data/English.sqlite-shm` 辅助文件，**也需加入 `.gitignore`**。

### 3.3 `PRAGMA synchronous = NORMAL`（折中）

| 模式 | 安全性 | 性能 |
|---|---|---|
| FULL | 最高（断电不丢） | 慢 |
| **NORMAL**（推荐） | 平衡 | 中 |
| OFF | 最低（可能丢） | 最快 |

> **课程作业选 NORMAL** 即可，崩溃时最多丢失最后 1 个事务。

### 3.4 `PRAGMA busy_timeout = 5000`（**必设**）

写锁冲突时等待 5 秒，避免立即抛 `SQLITE_BUSY` 异常。

```sql
PRAGMA busy_timeout = 5000;  -- 单位毫秒
```

### 3.5 `PRAGMA temp_store = MEMORY`

临时表 / 排序使用内存而非磁盘，加速 GROUP BY / ORDER BY。

---

## 4. 并发写入陷阱与缓解

### 4.1 问题

SQLite 写锁是**全局**的：任何写事务都会锁住整个 DB 文件。

> 典型报错：`SQLITE_BUSY: database is locked`

### 4.2 缓解策略

| 策略 | 实施方式 |
|---|---|
| 缩短事务 | 写操作快速完成，**严禁**在事务中调用外部 API / 渲染 HTML |
| `busy_timeout` | 等待 5s 后重试 |
| 避免长事务 | 批处理时分批提交（每批 ≤ 100 条） |
| 应用层排队 | 写入密集场景使用 `BlockingQueue` + 单线程消费 |

### 4.3 本项目特殊场景

- **学生提交答卷**：写 `score` + 更新 `question.correct` 2~10 次写操作，总耗时应 < 100ms
- **自动组卷**：可能 `INSERT exam` + 多次 `UPDATE question SET use = use + 1`，建议分批
- **批量导入题目**：单次 ≤ 100 题，超过则前端分批

---

## 5. 字符串与 JSON 字段处理

### 5.1 TEXT 字段无长度限制

SQLite 的 TEXT 字段可存储任意长度字符串。`VARCHAR(255)` 在 SQLite 中仅是"提示"，不强制截断。

### 5.2 JSON 字段处理

本项目使用 TEXT 存储 JSON，参考 [02-Data-Dictionary.md §4](file:///d:/GDUT-OOP_20260601/wiki/02-Data-Dictionary.md#4-单表--json-扩展字段设计规范)：

- DB 层：纯 TEXT，无类型
- Java 层：String 接收，Jackson 序列化/反序列化
- 索引：JSON 字段**不建索引**；按内容查询用 `LIKE '%"id"%'`

### 5.3 JSON 函数（可选）

SQLite 自 3.38 起支持 `json_extract`、`json_array_length` 等函数，**需启用 JSON1 扩展**（Hibernate 6.5+ 自动包含）：

```sql
-- 提取 question.answer 中的 correctOption（仅当 answer 是 JSON）
SELECT json_extract(answer, '$.correctOption') FROM question WHERE id = 123;
```

> **本项目不推荐使用 JSON 函数**：会引入 SQL 复杂度和方言依赖，统一在 Service 层用 Jackson 处理。

---

## 6. 日期时间处理

### 6.1 推荐存储

所有日期时间字段存为 `TEXT`（ISO 8601 格式），如 `2026-06-15T09:00:00`，便于：
- 跨平台兼容
- 直接 `ORDER BY` 字符串排序（与时间顺序一致）
- 无需考虑时区转换

```java
@Column(name = "starttime", nullable = false)
private String starttime;  // 参考 02-Data-Dictionary.md §4.3 exam 表；v3.0.0 使用 String (ISO 8601) 而非 LocalDateTime，因 Hibernate dialect 类型推断问题导致 ddl-auto=validate 校验失败
```

> **v3.0.0 变更说明**：由于 Hibernate 社区 SQLite 方言在 `ddl-auto=validate` 模式下对 `LocalDateTime` 的类型推断与实际 TEXT 列不匹配（方言将 Java 类型映射为非 TEXT 类型，而 SQLite 实际存储为 TEXT），导致启动校验失败。因此 v3.0.0 起改用 `String` 类型存储 ISO 8601 格式的日期时间字符串。

### 6.2 Hibernate 自动转换

Spring Data JPA + Hibernate 6.x 自动将 `LocalDateTime` 序列化为 ISO 8601 字符串存入 TEXT。

### 6.3 测试环境配置

项目使用独立的测试配置 `application-test.yaml`：

| 配置项 | 生产环境 | 测试环境 |
|---|---|---|
| 数据源路径 | `${user.dir}/../Data/English.sqlite` | `${user.dir}/target/test-data/test.sqlite` |
| `ddl-auto` | `validate` | `none`（由 `schema/*.sql` 初始化） |
| HikariCP 最大连接 | 5 | 1（SQLite 单写限制） |
| 自动配置排除 | 无 | 排除 `DataJdbcRepositoriesAutoConfiguration`（SQLite 不支持 JDBC 方言） |
| DDL 初始化 | 手动执行 `scripts/*.sql` | `spring.sql.init.mode=always` + `schema-locations=classpath:schema/*.sql` |

> **注意**：测试环境需排除 `DataJdbcRepositoriesAutoConfiguration`，因为 `spring-boot-starter-data-jdbc-test` 依赖会触发该自动配置，而 SQLite 不支持 JDBC 方言，导致启动失败。此排除通过 `application-test.yaml` 的 `spring.autoconfigure.exclude` 配置完成。

---

## 7. 备份策略

### 7.1 文件型备份（最简单）

```bash
# 每日定时任务
cp Data/English.sqlite Data/backup/English_$(date +%Y%m%d).sqlite
```

> **注意**：WAL 模式下，备份前应执行 `PRAGMA wal_checkpoint(TRUNCATE)` 强制合并 WAL 到主文件，确保备份完整。

### 7.2 在线备份 API（推荐）

```java
// 每次执行备份前调用
@Service
public class BackupService {
    
    @Transactional
    public void checkpoint() {
        entityManager.createNativeQuery("PRAGMA wal_checkpoint(TRUNCATE)")
            .executeUpdate();
    }
}
```

### 7.3 备份文件管理

- 保留最近 7 天每日备份 + 最近 4 周每周备份
- 备份文件存放 `Data/backup/`，**不提交 Git**

---

## 8. .gitignore 配置

```gitignore
# SQLite 数据库文件
Data/*.sqlite
Data/*.sqlite-wal
Data/*.sqlite-shm
Data/backup/

# IDE
.idea/
*.iml
.vscode/

# Maven / npm
target/
node_modules/
```

---

## 9. 性能监控与诊断

### 9.1 慢查询日志

```yaml
logging:
  level:
    org.hibernate.SQL: DEBUG
    org.hibernate.orm.jdbc.bind: TRACE
```

### 9.2 SQL 执行计划

```sql
EXPLAIN QUERY PLAN
SELECT * FROM question WHERE type = 'SingleChoice';
```

检查是否使用 `idx_question_type` 索引：

```
SCAN TABLE question USING INDEX idx_question_type
```

### 9.3 索引有效性

```sql
-- 查看所有索引
SELECT name, tbl_name, sql FROM sqlite_master WHERE type = 'index';

-- 分析表
ANALYZE question;
```

---

## 10. 常见错误与解决方案

| 错误 | 原因 | 解决 |
|---|---|---|
| `SQLITE_BUSY: database is locked` | 并发写竞争 | 设置 `busy_timeout=5000` + 缩短事务 |
| `NOT NULL constraint failed: user.name` | 必填字段未传值 | DTO 校验 `@NotBlank` + Service 兜底 |
| `UNIQUE constraint failed: user.name` | 用户名重复 | Service 层前置校验 + 捕获异常转 4104 |
| `FOREIGN KEY constraint failed` | 外键约束被触发 | 先清理子表数据，或临时 `PRAGMA foreign_keys = OFF`（仅调试） |
| `database disk image is malformed` | 文件损坏 | 从最近备份恢复；检查磁盘空间与断电情况 |
| `no such table: user` | 表未创建 | 手动执行 `schema.sql` 初始化 |

---

## 11. 本项目特别注意事项

### 11.1 题目表的 JSON 字段

`question.answer`（含 options 子字段）为 TEXT：

- **反序列化必须做异常处理**：JSON 格式错误时不能让整个查询失败
- **写入时序列化后存入**：直接存 Java 对象会导致 `@Lob` 等注解误用

### 11.2 答题统计的并发更新

`question` 表的 `use` / `correct` 字段 在大量学生同时提交时会竞争写锁。缓解：

- 提交答卷接口改为**异步处理**（`@Async`）统计更新
- 或**批量缓存**后定时刷盘（牺牲实时性）

### 11.3 考试时间窗的状态轮询

系统需识别 `running` 状态，有两种实现：

| 方案 | 复杂度 | 实时性 |
|---|---|---|
| 启动时 + 定时任务扫描更新 DB 状态 | 低 | 分钟级 |
| 查询时**实时计算** `now() BETWEEN start_time AND end_time` | 中 | 实时 |

> **本项目推荐方案 2**：避免后台调度复杂度，状态字段仅作"用户友好显示"。

---

## 12. 迁移路径（远期考虑）

当系统规模增长需要迁移到 MySQL/PostgreSQL 时：

1. 调整 `spring.datasource` 配置
2. 替换 Hibernate 方言
3. `PRAGMA` 语句移除
4. 调整自增主键策略（AUTOINCREMENT → AUTO_INCREMENT / SERIAL）
5. 利用 MyBatis Migration 工具迁移数据

> **当前阶段无需考虑**：课程作业规模不会触发迁移阈值。

---

## 13. 相关文档

- 全局规范：[01-Global-Standards.md §7](file:///d:/GDUT-OOP_20260601/wiki/01-Global-Standards.md#7-sqlite-特殊规范概要)
- 数据定义：[02-Data-Dictionary.md](file:///d:/GDUT-OOP_20260601/wiki/02-Data-Dictionary.md)
- 模块索引：[00-INDEX.md §5](file:///d:/GDUT-OOP_20260601/wiki/00-INDEX.md#5-模块索引)

---

## 14. 版本与变更记录

| 日期 | 版本 | 变更说明 |
|---|---|---|
| 2026-06-03 | v1.0.0 | 初始版本，记录 SQLite 与 MySQL/PG 差异、application.yaml 配置、PRAGMA 设置、并发写入陷阱、JSON 处理、日期时间、备份策略、.gitignore、性能监控、常见错误 |
| 2026-06-04 | v1.1.0 | 补充测试环境配置对比表（生产 vs 测试）、globally_quoted_identifiers 说明、DataJdbcRepositoriesAutoConfiguration 排除说明 |
| 2026-06-06 | **v2.0.0** | **Wiki 系统性校准**：① 确认所有 PRAGMA 配置与 application.yaml 实际值一致（foreign_keys=ON, journal_mode=WAL, synchronous=NORMAL, busy_timeout=5000, temp_store=MEMORY）；② 新增版本记录 |
