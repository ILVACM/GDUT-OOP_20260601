# Checklist

## 数据准确性校验
- [x] 00-INDEX.md 中 Java 文件总数标注为 71（M01:12 + M02:16 + M03:15 + M04:19 + common:8 + 根包:1）
- [x] 00-INDEX.md 中 Controller 端点总数标注为 41（M01:11 + M02:8 + M03:10 + M04:10 + DraftController:2）
- [x] 00-INDEX.md 中 DTO/VO 类总数标注为 43（M01:7 + M02:12 + M03:10 + M04:14 + common:2）
- [x] 00-INDEX.md 中测试总数标注为 73 个全部通过（9 个测试文件）
- [x] 00-INDEX.md 中 Data/img/ 目录状态标注为 ✅ 已存在
- [x] 00-INDEX.md 中前端状态标注为 ❌ 未创建
- [x] 01-Global-Standards.md §6.1 DTO 计数与实际一致

## 新增内容完整性校验
- [x] 00-INDEX.md 新增"项目背景"章节，包含项目缘起、需求背景、预期目标、分角色设计
- [x] 00-INDEX.md 新增"测试结果"章节，包含 73 个测试分布、环境、覆盖模块、运行方式、结果
- [x] 00-INDEX.md 新增"性能优化措施"章节，包含 HikariCP、PRAGMA、JPA 批量配置、N+1 防护、globally_quoted_identifiers
- [x] 00-INDEX.md 新增"验证方法"章节，包含后端启动、测试运行、数据库验证、API 契约验证
- [x] 02-Data-Dictionary.md 新增"开发过程中遇到的问题与解决方案"章节，记录 5 个典型问题
- [x] M02-Question-Bank.md 补全 EssayAnswer.java 在 DTO 清单中

## API 文档对齐校验
- [x] M01-User-Auth.md §4 API 表格与实际 UserController 11 个端点一致
- [x] M02-Question-Bank.md §7 API 表格与实际 QuestionController 8 个端点一致
- [x] M03-Exam-Assembly.md §6 API 表格与实际 ExamController 10 个端点一致
- [x] M04-Score-Statistics.md §6 API 表格与实际 ScoreController(9) + DraftController(2) 一致
- [x] M04-Score-Statistics.md 中 ScoreController 端点说明精确为 7+2=9（不含 DraftController）

## 版本记录完整性校验
- [x] 00-INDEX.md §12 版本记录新增 v4.0.0 条目
- [x] 01-Global-Standards.md §11 新增 v3.0.0 版本记录（校准条目）
- [x] 02-Data-Dictionary.md §12 版本记录新增 v4.0.0 条目
- [x] M01-User-Auth.md §11 新增 v3.0.0 版本记录（校准条目）
- [x] M02-Question-Bank.md §14 新增 v3.0.0 版本记录（校准条目）
- [x] M03-Exam-Assembly.md §12 新增 v3.0.0 版本记录（校准条目）
- [x] M04-Score-Statistics.md §13 新增 v3.0.0 版本记录（校准条目）
- [x] SQLite-Optimization.md §14 新增 v2.0.0 版本记录（校准条目）

## 格式与链接校验
- [x] 所有新增的 file:/// 链接指向的文档路径存在且有效
- [x] 所有章节编号与层级结构正确
- [x] Markdown 格式无语法错误（表格对齐、代码块闭合、标题层级正确）
