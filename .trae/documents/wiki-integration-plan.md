# Wiki 系统整合策略：temp.txt → wiki/

> **任务定位**：将 `temp/temp.txt`（项目需求与数据设计草案）整合为 wiki 系统中的权威规范文档。
> **执行原则**：temp.txt 原文不改动（由用户人工处置），仅写入 wiki/ 下文件；表述允许重新组织为专业无歧义的语言。

---

## 1. 探索结论（Current State Analysis）

### 1.1 temp.txt 内容剖析

| 段落 | 关键信息 | 性质 |
|---|---|---|
| §1 项目主题 | 英语在线学习系统 | 全局背景 |
| §2 系统功能 | 题目管理（题库）/ 题目性质管理 / 自动组卷 / 手动组卷 / 考试管理 / 用户管理（3 类角色）/ 数据统计 | 业务功能清单 |
| §3 技术栈 | Vue3 (npm 24.15.0) + Spring Boot 4 + JPA + JDK21 + SQLite；前端 Vite/Vue Router/Pinia/Element Plus/Axios | 全局技术约束 |
| §4 数据设计 | 5 张表：用户表 / 题目表 / 考试表 / 分数表 / 答题统计表 | 数据字典草案 |

**关键细节识别**：
- "题目性质管理" = 题目分类/标签/难度等元数据管理
- "自动组卷" vs "手动组卷" = 抽题策略两种模式
- 题目答案、考试题目、分数细分均使用 **JSON 扩展字段**（重要架构决策）
- 用户类型枚举中"管理员"内部细分（系统管理员 / 普通管理员）需明确

### 1.2 wiki 现状

- 所有 wiki 文件**仅有标题占位**，无实质内容
- 现有模块命名（M01-User-Auth / M02-Course-Management / M03-Enrollment / M04-Grade-Report）**全部基于"课程"模式**，与实际项目（题库/考试）**主题错配**
- 全局基座 3 文件（00-INDEX / 01-Global-Standards / 02-Data-Dictionary）也仅有标题

### 1.3 后端现状对照

| 项 | temp.txt 声明 | pom.xml 实际 | 一致性 |
|---|---|---|---|
| Spring Boot | 4 | 4.0.6 | ✓ |
| JDK | 21 | 21 | ✓ |
| ORM | JPA | spring-boot-starter-data-jpa | ✓ |
| DB | SQLite | sqlite-jdbc | ✓ |
| Lombok | 隐含（行业惯例） | 已引入 | ✓ |
| Data 文件 | 隐含 | `Data/English.sqlite` 存在 | ✓ |
| 前端项目 | Vue3 | **不存在** | ✗ 待补 |
| application.yaml | 未提 | 仅有 `spring.application.name`，**无数据库配置、无 JPA 配置** | ✗ 待补 |

### 1.4 潜在冲突清单

| 冲突编号 | 描述 | 严重度 |
|---|---|---|
| C1 | 现有 4 个模块文件 M01-M04 全部基于"课程"主题，与本项目（题库/考试）错配 | 高 |
| C2 | 现有模块文件虽然空，但文件名被 `00-INDEX.md` 之外的 wiki 内部交叉引用（如 SOP 中提及）需确认 | 中 |
| C3 | temp.txt 中题目答案/考试题目/分数细分均使用 JSON 扩展字段，与"严格 3NF"可能冲突，需在数据字典明确 | 中 |
| C4 | "管理员"内部细分（系统管理员/普通管理员）在 temp.txt 中表述不清晰，需在 wiki 中明确"超管"角色权限 | 中 |
| C5 | "题目性质"是 temp.txt 独有概念，对应字段未在题目表中显式定义，需在 wiki 推断为元数据表或扩展字段 | 中 |
| C6 | 分数表 + 答题统计表 两表设计在 temp.txt 中，可能与单一"成绩表"模型有重复，**用户决策保留双表** | 低（已确认） |
| C7 | "题目正确"统计是否需要排除未作答情况，temp.txt 未提，需在 wiki 中标注 TODO | 低 |

---

## 2. 整合策略（Proposed Changes）

### 2.1 目标 wiki 文档体系（重命名后）

```
wiki/
├── 00-INDEX.md                    # 项目总览（技术栈、功能、目录、模块索引）
├── 01-Global-Standards.md         # 全局开发规范（API 契约、Result T、全局异常、JPA 规范、分层）
├── 02-Data-Dictionary.md          # 数据字典（5 张表 ER、字段、JSON 扩展设计、JPA 映射约束）
├── modules/
│   ├── M01-User-Auth.md            # 用户认证与权限（三类角色 + 管理员二级分类）
│   ├── M02-Question-Bank.md       # 【新】题库与题目性质管理
│   ├── M03-Exam-Assembly.md       # 【新】考试管理 + 自动/手动组卷
│   ├── M04-Score-Statistics.md    # 【新】分数记录 + 答题统计 + 数据统计报表
│   └── _legacy_course-modules.md  # 【新】归档说明：解释 M02/M03/M04 重命名前因，避免外部引用断裂
└── references/
    └── SQLite-Optimization.md     # 保留 + 补充 SQLite 在本项目场景的具体配置
```

### 2.2 文件级变更详细方案

#### 2.2.1 [00-INDEX.md](file:///d:/GDUT-OOP_20260601/wiki/00-INDEX.md) —— 项目总览

**写入内容**：
- §1 项目简介（基于 temp.txt §1 重写）
- §2 技术栈矩阵（基于 temp.txt §3 重写，含版本号）
- §3 业务功能矩阵（基于 temp.txt §2 重写为功能 ↔ 模块映射表）
- §4 目录结构（前端/后端/wiki 三大块）
- §5 模块索引（指向 4 个新模块）
- §6 AI 协作边界（从开发指导.md 同步核心规则）

**格式标准**：
- 一级标题 `#`，二级 `##`，三级 `###`
- 表格使用 GFM 表格语法
- 代码块使用三个反引号 + 语言标签
- 关键术语首次出现需用 `术语` 形式

#### 2.2.2 [01-Global-Standards.md](file:///d:/GDUT-OOP_20260601/wiki/01-Global-Standards.md) —— 全局开发规范

**写入内容**（结合 SOP.md 中的实战规范与开发指导.md）：
- §1 技术栈硬约束（来自 temp.txt §3）
- §2 统一 API 响应契约 `Result<T>`（来自 SOP.md）
- §3 全局异常处理 `@RestControllerAdvice`（来自 SOP.md）
- §4 JPA 规范：禁止 N+1、必须 DTO 隔离、乐观锁策略
- §5 代码分层：Controller / Service / Repository / Entity / DTO
- §6 前端规范：Vue3 `<script setup>`、Axios baseURL 规则、Element Plus 引入
- §7 SQLite 特殊规范（外键开启、WAL 模式——仅作指引，详细见 references/）

**与 SOP.md 关系**：SOP.md 保留为"开发工作流"，本文件聚焦"规范定义"，避免内容重复但允许相互引用

#### 2.2.3 [02-Data-Dictionary.md](file:///d:/GDUT-OOP_20260601/wiki/02-Data-Dictionary.md) —— 数据字典

**写入内容**（基于 temp.txt §4 重写为专业数据字典）：
- §1 ER 关系总览图（文字描述）
- §2 表清单（5 张表，标注主外键）
- §3 字段级定义（每张表一节）：
  - §3.1 用户表 user
  - §3.2 题目表 question（含题目性质字段定义）
  - §3.3 考试表 exam
  - §3.4 分数表 score
  - §3.5 答题统计表 answer_statistics
- §4 "单表+JSON"扩展字段设计规范（明确 JSON 字段约束）
- §5 索引建议（主键、外键、唯一约束）
- §6 软删除/审计字段策略
- §7 JPA 实体映射约束（@EntityGraph、@Version 等）

**关键决策点**：
- "题目性质"（来自 temp.txt §2 业务功能）→ 建议作为 `question` 表的 JSON 扩展字段或独立 `question_property` 字典表，**计划中标注两种方案供用户选型**
- "管理员二级分类"（系统/普通）→ 在 user 表中以 `user_type` + `admin_subtype` 复合枚举表达

#### 2.2.4 [modules/M01-User-Auth.md](file:///d:/GDUT-OOP_20260601/wiki/modules/M01-User-Auth.md) —— 用户认证

**写入内容**：
- 保留原文件名（M01-User-Auth 与用户/认证主题匹配）
- §1 业务场景：注册/登录/Token/角色拦截
- §2 三类角色 + 管理员二级分类的枚举设计
- §3 关键 DTO：LoginReq / LoginResp / UserVO
- §4 API 接口定义（占位，等具体编码时再细化）
- §5 密码加密策略（BCrypt 等）

#### 2.2.5 [modules/M02-Question-Bank.md](file:///d:/GDUT-OOP_20260601/wiki/modules/M02-Question-Bank.md) —— 题库管理【新】

**写入内容**：
- §1 业务场景：题目 CRUD、题目性质管理（CRUD 字典/标签）、按类型/性质检索
- §2 五种题目类型枚举（单选/多选/判断/填空/简答）
- §3 题目性质（元数据）建模方案（**待选型**）：
  - 方案 A：题目表内 JSON 字段
  - 方案 B：独立字典表 `question_property`（更利于筛选与统计）
- §4 答案 JSON 扩展规范
- §5 API 接口骨架

#### 2.2.6 [modules/M03-Exam-Assembly.md](file:///d:/GDUT-OOP_20260601/wiki/modules/M03-Exam-Assembly.md) —— 考试与组卷【新】

**写入内容**：
- §1 业务场景：考试 CRUD、考试时间窗、组卷
- §2 手动组卷：教师从题库选题，组装为考试
- §3 自动组卷：基于规则（数量/类型/难度/性质）抽题
- §4 考试题目 JSON 扩展字段结构规范
- §5 API 接口骨架
- §6 事务/并发注意事项（防组卷期间题目被删）

#### 2.2.7 [modules/M04-Score-Statistics.md](file:///d:/GDUT-OOP_20260601/wiki/modules/M04-Grade-Report.md) —— 分数与统计【新】

**写入内容**：
- §1 业务场景：学生答题提交、自动判分/教师评卷、分数记录、统计报表
- §2 分数表 vs 答题统计表的边界澄清
  - 分数表 score：每次考试的"成绩单"（一人一考一行）
  - 答题统计表 answer_statistics：每道题被作答情况（用于题目维度统计）
- §3 细分 JSON 字段结构（每题得分明细）
- §4 数据统计 API：考试通过率、题目正确率、用户成绩分布
- §5 复杂查询优化（@Query JPQL，避免 N+1）

#### 2.2.8 [modules/_legacy_course-modules.md](file:///d:/GDUT-OOP_20260601/wiki/modules/_legacy_course-modules.md) —— 旧模块归档说明【新】

**写入内容**：
- 解释原 M01-M04 中 M02-Course-Management / M03-Enrollment / M04-Grade-Report 被废弃的原因
- 保留 M01-User-Auth.md 不变
- 说明新模块如何对应旧业务概念（如"课程"在本项目 = "考试"）
- 添加历史时间戳与变更原因

> **注意**：M01 保留是因为主题契合；M02/M03/M04 删除但创建归档说明文档，确保**任何外部引用都不会成为"死链"**。

#### 2.2.9 [references/SQLite-Optimization.md](file:///d:/GDUT-OOP_20260601/wiki/references/SQLite-Optimization.md) —— SQLite 优化备忘录

**追加内容**：
- §X 本项目特殊场景补充（题目答案 JSON 索引策略、答题统计高写入优化等）
- §Y application.yaml 中 SQLite 的完整推荐配置（datasource + jpa + hikari）

### 2.3 实施步骤（分步整合计划）

| 步骤 | 动作 | 输出文件 | 依赖 |
|---|---|---|---|
| S1 | 撰写 00-INDEX.md | wiki/00-INDEX.md | 无 |
| S2 | 撰写 01-Global-Standards.md | wiki/01-Global-Standards.md | S1（链接） |
| S3 | 撰写 02-Data-Dictionary.md | wiki/02-Data-Dictionary.md | S1 |
| S4 | 撰写 M01-User-Auth.md（沿用并填充） | wiki/modules/M01-User-Auth.md | S2, S3 |
| S5 | 撰写 M02-Question-Bank.md（新建） | wiki/modules/M02-Question-Bank.md | S3 |
| S6 | 撰写 M03-Exam-Assembly.md（新建） | wiki/modules/M03-Exam-Assembly.md | S3, S5 |
| S7 | 撰写 M04-Score-Statistics.md（新建） | wiki/modules/M04-Score-Statistics.md | S3, S5, S6 |
| S8 | 撰写 _legacy_course-modules.md（归档说明） | wiki/modules/_legacy_course-modules.md | S7 |
| S9 | 删除旧模块文件 | 删除：M02-Course-Management.md, M03-Enrollment.md | S8 |
| S10 | 追加 SQLite 优化（本项目场景） | wiki/references/SQLite-Optimization.md | 无 |
| S11 | 全局交叉链接检查 | （检查动作，无文件输出） | S1-S10 |
| S12 | temp.txt 处置说明 | （在 00-INDEX.md 添加脚注，由用户人工删除/归档 temp.txt） | S1 |

> **步骤 S9 的合理性**：用户决策"全部废弃，从零开始命名模块"，故 M02/M03 文件名被替换；M01 保留。旧文件物理删除，但在 `_legacy_course-modules.md` 中记录原因与映射关系，保证可追溯。

### 2.4 格式标准（与现有规范保持一致）

| 维度 | 标准 | 来源 |
|---|---|---|
| 文件命名 | `XX-Name.md`（XX 为 2 位序号），模块用 `M##-Name.md` | 现有 wiki 文件命名 |
| 标题层级 | `#` 一级、`##` 二级、`###` 三级 | 通用 Markdown 规范 |
| 代码块 | 三反引号 + 语言标签（java/vue/sql/yaml/bash） | 开发指导.md |
| 表格 | GFM 表格语法 | 通用 |
| 引用 | 使用 `file:///` 协议的可点击链接 | 开发指导.md |
| 注释规范 | 关键代码需标注 `// 参考 Xxx.md 第 X.X 节` | 开发指导.md |
| Emoji | 禁止（除非用户明确要求） | 开发指导.md |
| 任务标记 | TODO/待选型/待确认 需在文档中显式标注 | 本计划 |

---

## 3. 待用户确认的设计决策（Assumptions & Decisions）

以下决策基于 temp.txt 模糊表述推断，**需在执行前由用户确认**：

| 决策编号 | 决策点 | 推荐方案 | 备选方案 |
|---|---|---|---|
| D1 | "题目性质"建模 | 独立字典表 `question_property`（便于筛选统计） | 题目表内 JSON 字段 |
| D2 | 管理员二级分类 | `user_type` + `admin_subtype` 双枚举字段 | 单字段枚举值扩展为 4 种 |
| D3 | "自动组卷"规则引擎 | 在 wiki 中明确为"基于数量/类型/难度/性质的规则筛选"，简单实现 | 引入规则引擎（Drools 等），**不推荐**（与 OOP 课程作业量不匹配） |
| D4 | 答题统计表是否包含未作答 | 计入"出现"，未作答不算"正确" | 引入"已作答"中间态 |
| D5 | 题目难度字段 | 新增 `difficulty` 字段（1-5 整数），题目表内 | 不显式定义，由 JSON 扩展 |
| D6 | 旧模块删除 vs 保留文件名 | 物理删除旧 M02/M03 文件，新建归档说明 | 保留旧文件 + 标记 DEPRECATED |

**计划中默认采用推荐方案**；若用户对任何决策有异议，需在执行前回复。

---

## 4. 验证标准（Verification Steps）

整合完成后需逐项验证：

| 验证项 | 验证方法 | 通过标准 |
|---|---|---|
| V1 完整性 | 逐条对照 temp.txt 4 大段落 | §1-§4 全部信息点均被映射到至少一个 wiki 文件，无内容丢失 |
| V2 无重复 | grep 检查关键短语 | 同一信息在 wiki 中只出现 1 次（除交叉引用） |
| V3 模块对齐 | 列出 wiki 模块清单 | 模块名严格对应"题库/考试/分数"主题，无"课程/选课"残留 |
| V4 数据字典准确性 | 对照 5 张表定义 | 字段名、类型、外键关系清晰且与 temp.txt 一致 |
| V5 链接有效性 | 人工抽查 wiki 内 5 个链接 | 全部可点击、目标文件存在 |
| V6 与后端工程一致性 | 对照 pom.xml / application.yaml | 文档中声明的版本（Spring Boot 4 / JDK 21 / SQLite）与实际工程匹配 |
| V7 SOP 与开发指导兼容性 | 交叉阅读 SOP.md 与 01-Global-Standards.md | 不冲突，必要时互相引用 |
| V8 语言规范性 | 抽样阅读 3 个 wiki 文件 | 无歧义表述、无口语化、用词专业统一 |
| V9 AI 协作规则保留 | 比对 开发指导.md 核心条款 | 渐进式加载、Result T、DTO 隔离、N+1 禁止等条款在 01 中均有体现 |
| V10 旧文件处理 | 检查 modules/ 目录 | 仅存在 M01/M02/M03/M04 + _legacy 归档说明，无"课程"主题文件 |

---

## 5. 风险与缓解

| 风险 | 缓解策略 |
|---|---|
| 一次性重写 9 个 wiki 文件工作量大、易遗漏 | 按 S1→S12 顺序执行，每步完成后口头/文字确认再进入下一步 |
| 用户对 D1-D6 默认方案有异议导致返工 | 在执行第一步 S1 之前，集中确认 D1-D6 |
| 旧文件被外部脚本/链接引用 | S8 归档说明 + S9 删除前用 grep 全仓搜索引用 |
| JSON 扩展字段设计不规范导致后续 JPA 落地困难 | 在 02-Data-Dictionary.md §4 明确"JSON 字段的命名/Schema/索引"约束 |

---

## 6. 执行开关

- **本次计划不进入执行**：进入 Phase 4 通知用户审阅
- **用户接受后**：从 S1 开始按顺序执行；执行前若用户对 D1-D6 有意见，先调整再执行
- **执行模式**：每次写一个文件，完成后简短报告，等待用户继续（如不打断则连续推进）
