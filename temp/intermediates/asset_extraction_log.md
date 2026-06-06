# Agent 协作开发资产提取日志

> 生成时间: 2026-06-05
> 扫描模式: 只读（未对任何源文件进行写入或修改操作）
> 扫描范围: 项目仓库根目录下全部文件（代码、文档、配置脚本）

---

## 1. 专用开发文档（Wiki 知识库）

| 编号 | 文件路径 | 角色定位 | 关键内容摘要 |
|------|----------|----------|--------------|
| D1 | `wiki/00-INDEX.md` | 项目总索引 | 技术栈矩阵、业务功能矩阵、模块索引、AI 协作边界 |
| D2 | `wiki/01-Global-Standards.md` | 全局规范 | Result<T> 契约、全局异常处理、JPA 规范、代码分层、前端规范、SQLite 规范 |
| D3 | `wiki/02-Data-Dictionary.md` | 数据字典 | 4 张核心表字段定义、JSON 扩展规范、枚举定义、JPA 映射约束 |
| D4 | `wiki/modules/M01-User-Auth.md` | M01 模块文档 | 角色权限矩阵、JWT 规范、密码安全、API 接口定义 |
| D5 | `wiki/modules/M02-Question-Bank.md` | M02 模块文档 | 题目 CRUD、答案 JSON 扩展、题内统计自维护、答案反序列化多态 |
| D6 | `wiki/modules/M03-Exam-Assembly.md` | M03 模块文档 | 考试状态机(4态)、手动/自动组卷算法、快照机制、DTO 设计 |
| D7 | `wiki/modules/M04-Score-Statistics.md` | M04 模块文档 | 判分流程、教师评卷、统计报表、防 N+1 查询优化 |
| D8 | `wiki/references/SQLite-Optimization.md` | 技术参考 | PRAGMA 配置、并发写入陷阱、备份策略、迁移路径 |
| D9 | `wiki/modules/_legacy_course-modules.md` | 历史归档 | 废弃课程模块归档 |

## 2. 规则集文件 (Rulesets)

| 编号 | 文件路径 | 约束类型 | 关键规则 |
|------|----------|----------|----------|
| R1 | `.trae/rules/开发指导.md` | AI 协作规则 | 渐进式上下文加载协议、渐进式上下文加载协议、绝对禁止事项(5条)、代码生成风格 |
| R2 | `wiki/00-INDEX.md §7` | AI 协作边界 | 两阶段加载、Think Aloud 格式、Red Lines |
| R3 | `wiki/01-Global-Standards.md §1` | 技术栈硬约束 | 禁止引入 MyBatis/MyBatis-Plus、禁止 Vue2/Options API |
| R4 | `wiki/01-Global-Standards.md §4.1` | JPA 强制规范 | Entity/DTO隔离、N+1防护、@Version例外、@Column显式标注 |
| R5 | `wiki/01-Global-Standards.md §6.3` | 事务控制规范 | 写操作@Transactional、rollbackFor=Exception.class |

## 3. 提示词工程产物 (Prompt Engineering Artifacts)

| 编号 | 文件路径 | 产物类型 | 说明 |
|------|----------|----------|------|
| P1 | `.trae/rules/开发指导.md` | 结构化 Prompt 模板 | `<context_loading>` 标签格式、两阶段加载指令 |
| P2 | `.trae/specs/design-restful-api/spec.md` | 需求规格书 | RESTful API 契约设计 |
| P3 | `.trae/specs/implement-business-logic-layer/spec.md` | 业务逻辑规格书 | 14 个 Requirement + 46 个 Scenario |
| P4 | `.trae/specs/fix-business-logic-gaps/spec.md` | 业务逻辑补充规格书 | 修复业务逻辑缺口 |
| P5 | `.trae/specs/build-vue3-frontend/spec.md` | 前端规格书 | Vue3 前端构建 |
| P6 | `temp/在线学习系统-全栈开发工作流 (SOP).md` | 工作流 SOP | 5 阶段开发流程 |

## 4. 自动化流程脚本

| 编号 | 文件路径 | 功能 |
|------|----------|------|
| S1 | `scripts/table_user.sql` | 用户表 DDL |
| S2 | `scripts/table_question.sql` | 题目表 DDL |
| S3 | `scripts/table_exam.sql` | 考试表 DDL |
| S4 | `scripts/table_score.sql` | 分数表 DDL |
| S5 | `backend/src/test/resources/schema/01_user.sql` | 测试用用户表 DDL |
| S6 | `backend/src/test/resources/schema/02_question.sql` | 测试用题目表 DDL |
| S7 | `backend/src/test/resources/schema/03_exam.sql` | 测试用考试表 DDL |
| S8 | `backend/src/test/resources/schema/04_score.sql` | 测试用分数表 DDL |

## 5. 配置脚本

| 编号 | 文件路径 | 功能 |
|------|----------|------|
| C1 | `backend/src/main/resources/application.yaml` | 生产环境配置（PRAGMA/JPA/连接池） |
| C2 | `backend/src/test/resources/application-test.yaml` | 测试环境配置（独立SQLite/排除DataJDBC） |
| C3 | `backend/pom.xml` | Maven 依赖与构建配置 |
| C4 | `frontend-Vue/package.json` | npm 依赖配置 |
| C5 | `frontend-Vue/vite.config.ts` | Vite 构建与代理配置 |

## 6. 代码分层特征（自动化流程证据）

| 层级 | 包路径 | 实现特征 |
|------|--------|----------|
| common/api | `com.cps.backend.common.api` | Result\<T\>、PageResult\<T\> 统一返回 |
| common/exception | `com.cps.backend.common.exception` | BusinessException + GlobalExceptionHandler |
| common/security | `com.cps.backend.common.security` | JwtUtil + JwtAuthenticationInterceptor + @RequireRole |
| common/config | `com.cps.backend.common.config` | WebMvcConfig 拦截器注册 |
| modules/M01 | `com.cps.backend.modules.M01userauth` | controller/service/repository/entity/dto/enums |
| modules/M02 | `com.cps.backend.modules.M02questionbank` | controller/service/repository/entity/dto/enums |
| modules/M03 | `com.cps.backend.modules.M03examassembly` | controller/service/repository/entity/dto/enums |
| modules/M04 | `com.cps.backend.modules.M04scorestatistics` | controller/service/repository/entity/dto |

## 7. 合规性声明

本次扫描全程采用**只读模式**，仅使用 Read/LS/Glob 等工具读取文件内容，**未对任何源文件进行写入、修改、删除或结构调整操作**。所有生成的输出文件均存放于 `temp/outputs/` 和 `temp/intermediates/` 目录下，与源代码库物理隔离。
