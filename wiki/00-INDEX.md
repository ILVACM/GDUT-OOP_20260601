# 在线学习系统 - AI 开发全局索引

> 本文档是项目唯一权威信息源（Single Source of Truth）的总入口，定义项目简介、技术栈、业务功能、目录结构、模块索引以及 AI 协作规范。

***

## 1. 项目简介

本项目为**英语在线学习系统**（English Online Learning System），是大三下学期《面向对象程序设计》课程的期末实践项目。系统以"题库—组卷—考试—统计"为主线，支撑教师维护题目、灵活组卷、组织考试、记录成绩、生成统计报表的全流程教学活动，同时为学生提供在线答题、查看分数与个性化错题分析的能力。

系统分角色（学生 / 老师 / 管理员）提供差异化的功能视图与权限边界，并通过结构化数据存储保证历史考试可追溯、题目统计可分析。

***

## 2. 技术栈矩阵

| 层级      | 选型                          | 版本/说明                                          | 用途                                             |
| ------- | --------------------------- | ---------------------------------------------- | ---------------------------------------------- |
| 前端框架    | Vue 3                       | npm 24.15.0，Composition API + `<script setup>` | 单页应用视图层                                        |
| 前端构建    | Vite                        | 最新稳定版                                          | 开发服务器 + 生产构建                                   |
| 前端路由    | Vue Router 4                | —                                              | 路由与权限拦截                                        |
| 前端状态    | Pinia                       | —                                              | 跨组件状态管理                                        |
| 前端 UI   | Element Plus                | —                                              | 组件库                                            |
| 前端 HTTP | Axios                       | —                                              | 后端 API 调用，需配置全局拦截器                             |
| 后端框架    | Spring Boot                 | 4.0.6                                          | RESTful 接口与依赖注入容器                              |
| 后端语言    | JDK                         | 21                                             | 启用 Record、Pattern Matching、Virtual Threads 等特性 |
| 后端 ORM  | Spring Data JPA + Hibernate | Hibernate 社区方言 7.2.12.Final                  | 数据库持久化                                         |
| 后端工具    | Lombok                      | —                                              | 简化 Entity / DTO 样板代码                           |
| 数据库     | SQLite                      | xerial sqlite-jdbc                             | 文件型关系数据库，文件位于 `Data/English.sqlite`            |
| 部署      | Docker / Docker Compose     | （可选，远期）                                        | 容器化打包与一键启动                                     |

> **硬约束**：
>
> 1. 严禁替换任一选型；如需替换，必须先更新本文档与 [01-Global-Standards.md](file:///d:/GDUT-OOP_20260601/wiki/01-Global-Standards.md) 并通过评审。
> 2. 后端不得引入 MyBatis / MyBatis-Plus 等与 JPA 冲突的 ORM 框架。
> 3. 前端不得引入 jQuery、Vue 2、Element UI 等与 Vue 3 生态不兼容的依赖。
> 4. **SQLite 特殊约束**：布尔字段使用 `Integer`（0/1）而非 `Boolean`；时间字段使用 `String`（ISO 8601）而非 `LocalDateTime`；主键类型为 `Integer` 而非 `Long`（详见 [01-Global-Standards.md §4.1.1](file:///d:/GDUT-OOP_20260601/wiki/01-Global-Standards.md) 与 [02-Data-Dictionary.md §1.4](file:///d:/GDUT-OOP_20260601/wiki/02-Data-Dictionary.md)）。

***

## 3. 业务功能矩阵

| 业务功能   | 功能说明                          | 归属模块      | 角色        |
| ------ | ----------------------------- | --------- | --------- |
| 用户管理   | 注册、登录、信息维护、角色分配（student/teacher/admin）、状态启停 | M01 用户认证  | 全角色       |
| 题库管理   | 题目 CRUD、批量导入、按类型/关键字检索；题内统计自维护（use/correct）  | M02 题库管理  | 老师、管理员    |
| 手动组卷   | 教师从题库逐题挑选，组装为草稿状态（status=draft）的考试           | M03 考试与组卷 | 老师        |
| 自动组卷   | 指定题目数量，系统从题库随机抽题（支持 type 过滤 / use 降权），落库为 draft 状态 | M03 考试与组卷 | 老师        |
| 考试管理   | 考试发布 / 撤回、时间窗控制、状态机（draft→publish→running→done） | M03 考试与组卷 | 老师、管理员    |
| 答题与判分  | 学生在线作答、系统自动判分（单选/多选/判断/填空）/ 教师评卷（简答）        | M04 分数与统计 | 学生、老师     |
| 分数记录   | 持久化每次考试成绩（score.all + score.detail JSON），支持明细追溯 | M04 分数与统计 | 全角色       |
| 题目质量统计  | 通过 `question.use` / `question.correct` 计算正确率，识别过难/过易题 | M04 分数与统计 | 老师、管理员    |
| 数据统计报表 | 考试通过率、成绩分布、个人错题集          | M04 分数与统计 | 老师、管理员、学生 |

***

## 4. 目录结构

```
GDUT-OOP_20260601/
├── backend/                 # Spring Boot 后端工程
│   ├── src/main/java/       # Java 源码，按 com.cps.backend.* 分层
│   │   └── com/cps/backend/
│   │       ├── modules/     # 4 个业务模块（M01~M04）
│   │       │   ├── M01userauth/      # entity/enum/repository ✅ 已实现
│   │       │   ├── M02questionbank/  # entity/enum/repository ✅ 已实现
│   │       │   ├── M03examassembly/  # entity/enum/repository ✅ 已实现
│   │       │   └── M04scorestatistics/ # entity/repository ✅ 已实现
│   │       └── BackendApplication.java
│   ├── src/main/resources/  # application.yaml（生产配置）
│   ├── src/test/            # 单元测试与集成测试（73 个测试 ✅ 全部通过，4 Service + 4 Repository + 1 Application）
│   │   └── resources/       # application-test.yaml + schema/*.sql
│   └── pom.xml              # Maven 配置（Spring Boot 4.0.6）
├── frontend/                # 【待创建】Vue 3 前端工程（尚未初始化）
├── Data/                    # SQLite 数据库文件目录
│   ├── English.sqlite       # 运行时数据库（不提交 Git）
│   └── img/                 # 题目图片存储目录（已存在，含示例图片）
├── scripts/                 # DDL 脚本（table_user/question/exam/score.sql）
├── wiki/                    # 项目权威规范文档
│   ├── 00-INDEX.md          # 本文件
│   ├── 01-Global-Standards.md
│   ├── 02-Data-Dictionary.md
│   ├── modules/             # 业务模块设计文档
│   └── references/          # 技术参考与备忘录
└── temp/                    # 临时草稿区（已停止维护）
```

> **关于 frontend/ 目录**：当前仓库尚未初始化前端工程。后端全栈（数据层 + 业务层 + 基础设施）已完成，可依据 §2 技术栈创建前端。
>
> **当前实现状态**（2026-06-06）：后端全链路已完成 ✅——共 **71 个 Java 文件**（M01:12, M02:16, M03:15, M04:19, common:8, 根包:1），**41 个 Controller 端点**（M01:11, M02:8, M03:10, M04:10, DraftController:2），**43 个 DTO/VO 类**（M01:7, M02:12, M03:10, M04:14, common:2），**73 个单元测试全部通过**（9 个测试文件）；前端尚未创建；Data/img/ 目录已存在（含 1 个文件）。

***

## 5. 模块索引

| 编号  | 模块      | 文档                                                                                           | 核心职责                       |
| --- | ------- | -------------------------------------------------------------------------------------------- | -------------------------- |
| M01 | 用户认证与权限 | [M01-User-Auth.md](file:///d:/GDUT-OOP_20260601/wiki/modules/M01-User-Auth.md)               | 三类角色认证、Token 签发、权限拦截       |
| M02 | 题库管理  | [M02-Question-Bank.md](file:///d:/GDUT-OOP_20260601/wiki/modules/M02-Question-Bank.md)       | 题目 CRUD、答案 JSON 扩展、按类型/难度/关键字检索 |
| M03 | 考试与组卷   | [M03-Exam-Assembly.md](file:///d:/GDUT-OOP_20260601/wiki/modules/M03-Exam-Assembly.md)       | 考试生命周期、手动 / 自动组卷策略         |
| M04 | 分数与统计   | [M04-Score-Statistics.md](file:///d:/GDUT-OOP_20260601/wiki/modules/M04-Score-Statistics.md) | 成绩单、答题统计、统计报表              |

> **历史模块说明**：原 wiki 中存在基于"课程"主题的模块文件（课程管理 / 选课 / 课程成绩），已废弃并归档于 [\_legacy\_course-modules.md](file:///d:/GDUT-OOP_20260601/wiki/modules/_legacy_course-modules.md)。

***

## 6. 全局基座文档

| 文档                                                                                                       | 作用                           | 何时必读                     |
| -------------------------------------------------------------------------------------------------------- | ---------------------------- | ------------------------ |
| [01-Global-Standards.md](file:///d:/GDUT-OOP_20260601/wiki/01-Global-Standards.md)                       | API 契约、全局异常、JPA 规范、代码分层、前端规范 | 任何编码任务前                  |
| [02-Data-Dictionary.md](file:///d:/GDUT-OOP_20260601/wiki/02-Data-Dictionary.md)                         | 5 张核心表结构、字段、JSON 扩展、JPA 映射约束 | 涉及 Entity / Repository 时 |
| [references/SQLite-Optimization.md](file:///d:/GDUT-OOP_20260601/wiki/references/SQLite-Optimization.md) | SQLite 在 JPA 环境下的特殊配置与避坑     | 涉及数据库连接 / 性能调优时          |

***

## 7. AI 协作边界（必须遵守）

> 本节是项目"宪法"条款，**所有 AI 生成代码、架构建议、问题解答都必须严格遵守**，与 `.trae/rules/开发指导.md` 同等优先级。

### 7.1 渐进式上下文加载协议

AI 在处理任何请求时**必须**遵循两阶段加载：

1. **阶段一（强制全局基座）**：读取 `00-INDEX.md`、`01-Global-Standards.md`、`02-Data-Dictionary.md` 三个全局基座文件。
2. **阶段二（按需模块加载）**：根据任务业务领域，**仅精准加载** `modules/` 与 `references/` 中与任务直接相关的子文件。

AI 在生成最终答案前**必须**外化思考过程，使用如下格式：

```text
<context_loading>
1. 已加载全局基座：00-INDEX, 01-Global-Standards, 02-Data-Dictionary。
2. 任务分析：当前任务涉及 [具体业务/技术点]。
3. 渐进加载：按需读取 [具体子模块/参考文件路径]。若无需额外加载则填"无"。
4. 核心约束提取：[列出 1-3 条从文档中提取的、与当前任务最相关的强制规范]。
</context_loading>
```

### 7.2 绝对禁止事项

| 编号 | 禁止内容                                                | 后果                    |
| -- | --------------------------------------------------- | --------------------- |
| R1 | Controller 直接返回 Entity、Map 或自定义结构（必须返回 `Result<T>`） | 违反 API 契约             |
| R2 | 将 JPA Entity 暴露为前端响应（必须经 DTO 转换）                    | 引发 JSON 循环引用 / 敏感字段泄露 |
| R3 | 循环中调用 Repository / 未使用 `@EntityGraph` 引发 N+1        | 性能雪崩                  |
| R4 | 擅自修改数据字典字段（必须先在 02-Data-Dictionary.md 提请变更）         | 文档与代码失同步              |
| R5 | 一次性加载整个 `wiki/` 目录 / 使用 `@workspace` 盲目搜索           | 上下文溢出、注意力稀释           |

***

## 8. 项目背景

> 本项目为《面向对象程序设计》课程的期末实践项目，以英语在线学习系统为载体，贯穿"题库—组卷—考试—统计"完整教学流程。

### 8.1 项目缘起

英语在线学习系统是大三下学期《面向对象程序设计》课程的期末实践项目。项目以"题库—组卷—考试—统计"为主线，模拟真实教学场景中的考试管理全流程。

### 8.2 需求背景

在英语教学中，教师需要：
- 维护题目库（含单选、多选、判断、填空、简答五种题型）
- 灵活组卷（手动挑题或按规则自动抽题）
- 组织在线考试（发布时间窗、状态机管理）
- 记录成绩与生成统计报表

学生需要：
- 在线答题与查看分数
- 个性化错题分析

### 8.3 预期目标

- 支撑教师维护题目、灵活组卷、组织考试、记录成绩、生成统计报表的全流程教学活动
- 为学生提供在线答题、查看分数与个性化错题分析的能力
- 通过结构化数据存储保证历史考试可追溯、题目统计可分析

### 8.4 分角色设计

系统分三类角色提供差异化功能视图：
- **student（学生）**：参加考试、查看成绩、错题集
- **teacher（老师）**：题库 CRUD、组卷、考试管理、评卷、统计报表
- **admin（管理员）**：全部权限 + 用户管理

***

## 9. 测试结果

### 9.1 测试概况

| 指标 | 数值 |
|---|---|
| 测试总数 | 73 个 |
| 通过 | 73 个 |
| 失败 | 0 个 |
| 跳过 | 0 个 |
| 测试文件数 | 9 个 |

### 9.2 测试分布

| 层级 | 测试文件 | 数量 |
|---|---|---|
| Service 层 | UserServiceTest, QuestionServiceTest, ExamServiceTest, ScoreServiceTest | 4 个 |
| Repository 层 | UserRepositoryTest, QuestionRepositoryTest, ExamRepositoryTest, ScoreRepositoryTest | 4 个 |
| 应用层 | BackendApplicationTests | 1 个 |

### 9.3 测试环境

- 独立测试配置：`backend/src/test/resources/application-test.yaml`
- 数据库初始化：`schema/*.sql` DDL 脚本自动加载
- SQLite 测试数据库：`target/test-data/test.sqlite`
- 排除 `DataJdbcRepositoriesAutoConfiguration`（SQLite 不支持 JDBC 方言）

### 9.4 覆盖模块

- M01 用户认证：注册、登录、密码修改、状态启停、用户 CRUD、批量删除
- M02 题库管理：题目创建、批量导入、答案 JSON 校验、题内统计自维护
- M03 考试与组卷：手动/自动组卷、考试状态机、发布/撤回/删除
- M04 分数与统计：答题提交、自动判分、教师评卷、统计报表、草稿缓存

### 9.5 运行方式

```bash
cd backend
mvn test
```

预期输出：`Tests run: 73, Failures: 0, Errors: 0, Skipped: 0`

***

## 10. 性能优化措施

> 详细避坑指南参见 [references/SQLite-Optimization.md](file:///d:/GDUT-OOP_20260601/wiki/references/SQLite-Optimization.md)

### 10.1 HikariCP 连接池调优

| 配置 | 值 | 说明 |
|---|---|---|
| maximum-pool-size | 5 | SQLite 单文件，无需大连接池 |
| minimum-idle | 1 | 保持最低 1 个空闲连接 |

### 10.2 SQLite PRAGMA 初始化

每次新建连接时执行 5 条 PRAGMA：
1. `foreign_keys = ON`：开启外键约束（SQLite 默认关闭）
2. `journal_mode = WAL`：WAL 模式，读不阻塞写
3. `synchronous = NORMAL`：折中安全性与性能
4. `busy_timeout = 5000`：写锁冲突等待 5 秒
5. `temp_store = MEMORY`：临时表使用内存加速排序

### 10.3 JPA 批量配置

| 配置 | 值 |
|---|---|
| batch_size | 30 |
| order_inserts | true |
| order_updates | true |

### 10.4 N+1 查询防护

- 4 张核心表独立设计（无 `@ManyToOne`/`@OneToMany` 关联），零 N+1 风险
- 跨表查询使用 `findAllById` 批量加载（判分时加载题目详情）

### 10.5 标识符引用

- `globally_quoted_identifiers=true`：避免 `user`/`all` 等 SQL 关键字与列名冲突

***

## 11. 验证方法

### 11.1 后端启动验证

```bash
cd backend
mvn spring-boot:run
```

启动成功后访问 `http://localhost:8080`，应返回 Spring Boot 默认错误页面（因无根路径映射）。

### 11.2 测试验证

```bash
cd backend
mvn test
```

预期输出：`Tests run: 73, Failures: 0, Errors: 0, Skipped: 0`

### 11.3 数据库文件验证

- 文件路径：`Data/English.sqlite`
- 可使用 [SQLite Browser](https://sqlitebrowser.org/) 或 `sqlite3` 命令行工具打开
- 应包含 4 张表：`user`、`question`、`exam`、`score`

### 11.4 API 契约验证

所有接口统一返回 `Result<T>` 格式：

```json
// 成功
{ "code": 200, "message": "success", "data": { ... } }

// 业务异常
{ "code": 4001, "message": "错误描述", "data": null }
```

可使用 Postman 或 curl 测试：
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"name":"test","password":"123456"}'
```

***

## 12. 版本与变更记录

| 日期         | 版本     | 变更说明                                              |
| ---------- | ------ | ------------------------------------------------- |
| 2026-06-03 | v1.0.0 | 由 temp.txt 草案整合为 wiki 正式规范；重命名模块 M02-M04；建立权威文档体系 |
| 2026-06-04 | v1.0.1 | 配合 [02-Data-Dictionary.md v2.0.0](file:///d:/GDUT-OOP_20260601/wiki/02-Data-Dictionary.md) 同步更新：业务功能矩阵与模块索引按 4 表新规范重写（删除 `answer_statistics`、枚举小写/CamelCase、状态机 4 态）；[01-Global-Standards.md §4.1.1](file:///d:/GDUT-OOP_20260601/wiki/01-Global-Standards.md) 新增放弃 J6/§5 硬约束的例外声明 |
| 2026-06-04 | v1.1.0 | 配合 [02-Data-Dictionary.md v3.0.0](file:///d:/GDUT-OOP_20260601/wiki/02-Data-Dictionary.md) 全面校准：① 技术栈矩阵补充 Hibernate 社区方言版本号；② 硬约束新增第 4 条 SQLite 特殊约束（Integer 布尔 / String 时间 / Integer 主键）；③ 目录结构更新为实际状态（标注已实现/待实现）；④ 新增当前实现状态说明（数据层完成、业务层待开发、前端待创建） |
| 2026-06-05 | v2.0.0 | **后端全链路完成校准**：① 更新实现状态为后端全链路完成（Controller/Service/DTO/JWT/PageResult/Draft 缓存全部实现）；② 统一 API 前缀为 `/api/v1/`（与代码实际一致）；③ 补充 register、batch delete、random question、draft save/load 等新增接口；④ 更新测试计数为 73 个全部通过；⑤ 全局 API 接口文档与代码对齐 |
| 2026-06-06 | v4.0.0 | **Wiki 系统性校准**：① 精确统计数据（71 Java 文件、41 端点、43 DTO、73 测试）；② 校准 Data/img/ 目录状态；③ 新增项目背景（§8）、测试结果（§9）、性能优化措施（§10）、验证方法（§11）四大章节；④ API 文档与 Controller 端点 100% 对齐 |

*** 
