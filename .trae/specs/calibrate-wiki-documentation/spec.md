# Wiki 系统性更新与校准 Spec

## Why
项目后端全链路已完成（71 个 Java 文件、41 个 Controller 端点、43 个 DTO 类、73 个测试全部通过），但 Wiki 文档中存在多处与实际代码不一致的数据（端点数、DTO 计数、目录状态等），以及缺少项目背景、测试结果、性能优化记录、开发问题与解决方案等关键章节。需全面校准 Wiki，使其成为项目最终状态的权威信息源，为后续报告撰写提供可靠参考资料。

## What Changes
- **校准数据计数**：修正 Wiki 中与实际代码不符的端点数、DTO 数、测试数、文件数等统计信息
- **校准目录状态**：修正 Data/img/ 目录状态（已存在，含 1 个文件）
- **新增项目背景章节**：补充项目缘起、需求背景、预期目标
- **新增测试结果章节**：补充 73 个测试的分布、覆盖范围与运行方式
- **新增开发问题与解决方案章节**：记录 JPA/SQLite 适配过程中的典型问题与处置方案
- **新增性能优化记录章节**：记录 SQLite PRAGMA 配置、连接池调优、N+1 防护措施
- **校准 API 接口文档**：确保 M01-M04 各模块 API 表格与实际 Controller 端点 100% 对齐
- **补充 M04 ScoreController 端点详情**：部分统计类端点在 M04 文档中描述不够精确
- **补充 Answer 多态 DTO 清单**：M02 实际有 EssayAnswer.java，Wiki 需补全
- **统一版本记录**：所有文档添加 v4.0.0 校准变更记录

## Impact
- Affected specs: 所有 Wiki 文档
- Affected code: 无代码变更，仅更新 `wiki/` 目录下 7 个 Markdown 文件
- 关系：本 spec 依赖于前期已完成的 spec（implement-business-logic-layer, fix-business-logic-gaps, design-restful-api, build-vue3-frontend），是对这些 spec 实施成果的文档化总结

## ADDED Requirements

### Requirement: 项目背景章节
00-INDEX.md SHALL 新增"项目背景"章节，包含以下内容：
- 项目缘起：《面向对象程序设计》课程期末实践
- 需求背景：英语在线学习系统的教学场景（题库—组卷—考试—统计）
- 预期目标：支撑教师维护题目、灵活组卷、组织考试、记录成绩、生成统计报表；为学生提供在线答题、查看分数与个性化错题分析
- 分角色设计：学生 / 老师 / 管理员差异化功能视图

### Requirement: 测试结果记录章节
00-INDEX.md SHALL 新增"测试结果"章节，包含：
- 测试总数与分布：73 个测试方法（Service 层 4 个测试类 + Repository 层 4 个测试类 + 应用启动测试 1 个）
- 测试环境：独立 `application-test.yaml` 配置 + `schema/*.sql` 初始化
- 测试覆盖模块：M01 用户认证、M02 题库管理、M03 考试与组卷、M04 分数与统计
- 运行方式：`mvn test` 命令
- 测试结果：全部通过（0 失败，0 跳过）

### Requirement: 开发问题与解决方案章节
02-Data-Dictionary.md SHALL 新增"开发过程中遇到的问题与解决方案"章节，记录：
1. **Boolean vs Integer 类型不匹配**：`ddl-auto=validate` 模式下 `Boolean` 映射 `TINYINT` 与 DDL `INTEGER` 不一致 → 改用 `Integer`（0/1）
2. **LocalDateTime vs String 时间字段**：Hibernate 社区方言对 `LocalDateTime` → TEXT 自动转换在 validate 模式下类型推断差异 → 改用 `String`（ISO 8601）
3. **Score 外键映射方式**：4 表独立设计，无 JPA `@ManyToOne` 关联 → 使用 `@Column` 而非 `@JoinColumn`
4. **主键类型选择**：SQLite INTEGER 自增范围足够 → 使用 `Integer` 而非 `Long`
5. **SQLite JDBC 方言不支持**：`spring-boot-starter-data-jdbc-test` 自动配置触发失败 → `application-test.yaml` 排除 `DataJdbcRepositoriesAutoConfiguration`

### Requirement: 性能优化记录章节
00-INDEX.md SHALL 新增"性能优化措施"章节，引用 SQLite-Optimization.md 并概要记录：
- HikariCP 连接池配置（max=5, min-idle=1，适配 SQLite 单文件特性）
- 5 条 PRAGMA 初始化（foreign_keys=ON, journal_mode=WAL, synchronous=NORMAL, busy_timeout=5000, temp_store=MEMORY）
- JPA 批量配置（batch_size=30, order_inserts, order_updates）
- N+1 防护：4 张核心表独立设计（无 `@ManyToOne`/`@OneToMany`），零 N+1 风险；跨表查询使用 `findAllById` 批量加载
- `globally_quoted_identifiers=true` 避免 `user`/`all` 等 SQL 关键字冲突

### Requirement: 验证方法章节
00-INDEX.md SHALL 新增"验证方法"章节，包含：
- 后端启动验证：`mvn spring-boot:run`，访问 `http://localhost:8080`
- 测试验证：`mvn test`
- 数据库文件验证：`Data/English.sqlite` 存在且可连接
- API 契约验证：通过 Postman/curl 测试统一返回格式 `Result<T>`

## MODIFIED Requirements

### Requirement: 00-INDEX.md 当前实现状态
原实现状态说明需更新为精确数据：
- 后端：71 个 Java 文件（M01: 12, M02: 16, M03: 15, M04: 19, common: 8, 根包: 1）
- Controller 端点：41 个（M01: 11, M02: 8, M03: 10, M04: 10, DraftController: 2）
- DTO/VO 类：43 个（M01: 7, M02: 12, M03: 10, M04: 14, common: 2）
- 测试：73 个全部通过（9 个测试文件：4 Service + 4 Repository + 1 Application）
- 前端：❌ 未创建
- Data/img/ 目录：✅ 已存在（含 1 个文件：4.png）
- 根级 .gitignore：❌ 未创建

### Requirement: 01-Global-Standards.md 分层职责表格
修改 6.1 后端分层目录树中各模块的 DTO 计数说明，与实际代码对齐：
- M01: 7 个 DTO（LoginReq/Resp, RegisterReq, UserVO, ChangePasswordReq, UserStatusReq, BatchDeleteReq）
- M02: 12 个 DTO（含 Answer 多态 5 种 + QuestionCreateReq/UpdateReq/QueryReq/VO + BatchImportResult + QuestionPreviewVO + EssayAnswer）
- M03: 10 个 DTO（ExamCreateManualReq/AutoReq, ExamVO/ForStudentVO, ExamQuestionVO/ForStudentVO/ItemReq, QuestionSum/SumItem, AutoRule）
- M04: 14 个 DTO（ExamSubmitReq, EssayGradeReq, AnswerItem, ScoreVO/ListVO/Detail, DetailItem/VO, Summary, UserExamHistoryVO, MistakeItemVO, ExamStatisticsVO, QuestionStatisticsVO）

### Requirement: M04-Score-Statistics.md ScoreController 端点
M04 §6.2 分数查询表格需校准，实际端点为：
- `GET /api/v1/scores/me` → 我的成绩
- `GET /api/v1/scores/me/mistakes` → 我的错题集
- `GET /api/v1/scores/{id}` → 分数详情
- `GET /api/v1/exams/{examId}/scores` → 某考试的所有考生成绩
- `GET /api/v1/statistics/exams/{examId}` → 考试统计
- `GET /api/v1/statistics/questions` → 题目统计（分页/排序）
- `GET /api/v1/statistics/questions/{id}` → 单题详细统计

原描述中"ScoreController（9 端点）"应修正为"ScoreController（7 端点）+ 2 判分端点 = 共 9 个业务端点"（不含 DraftController 的 2 个）。

### Requirement: 所有 Wiki 文档版本记录
所有 7 个 Wiki 文件需在版本记录中新增 v4.0.0 条目：
```
| 2026-06-06 | v4.0.0 | Wiki 系统性校准：① 精确统计数据（71 Java 文件、41 端点、43 DTO、73 测试）；② 校准 Data/img/ 目录状态；③ 新增项目背景、测试结果、开发问题与解决方案、性能优化、验证方法章节；④ API 文档与 Controller 端点 100% 对齐；⑤ 补充 EssayAnswer DTO |
```

## REMOVED Requirements
无移除项。
