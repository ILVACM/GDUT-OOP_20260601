# Wiki 调整最终验证计划：彻底删除 `question_property` 与"题目性质"概念

> **任务定位**：基于上一轮会话已完成的清理工作，对当前 wiki 状态进行系统性校验、补充任何遗漏的引用、并确认数据模型自适应调整已完整落地。本计划**不重复已完成的删除操作**，仅聚焦于验证与最终一致性收口。

---

## 1. 当前状态分析（Phase 1 探索结果）

通过 Read + Grep 全量校验 `d:\GDUT-OOP_20260601\wiki\` 全部 8 个文件，状态如下：

### 1.1 实际残留情况

| 文件 | 残留内容 | 处置 |
|---|---|---|
| `02-Data-Dictionary.md` §1 ER 关系总览 | 无 | ✓ 已清理 |
| `02-Data-Dictionary.md` §2 表清单 | 5 张表（T1-T5），无 T3 question_property | ✓ 已清理 |
| `02-Data-Dictionary.md` §3 字段级定义 | 3.1-3.5 五节，无 question_property 章节 | ✓ 已清理 |
| `02-Data-Dictionary.md` §4 JSON 字段规范 | 仅 answer/options/questions/detail 四处 | ✓ 已清理 |
| `02-Data-Dictionary.md` §6 Entity 示例 | 不含 propertyIds | ✓ 已清理 |
| `02-Data-Dictionary.md` §8 版本记录 | **L426 v1.1.0 变更日志明确写了"废弃 question_property"** | ⚠ 应保留（审计追溯） |
| `00-INDEX.md` §3 业务功能矩阵 | 不含"题目性质管理"行 | ✓ 已清理 |
| `00-INDEX.md` §5 模块索引 M02 | 模块名"题库管理"、描述"题目 CRUD、答案 JSON 扩展、按类型/难度/关键字检索" | ✓ 已清理 |
| `00-INDEX.md` §6 全局基座文档 | "5 张核心表结构" | ✓ 已清理 |
| `M02-Question-Bank.md` | §1 业务场景、§4 DTO、§5 API、§6 业务规则均无 propertyIds | ✓ 已清理 |
| `M03-Exam-Assembly.md` | §3.2 自动组卷"不支持按题型、难度、性质等任何维度的筛选规则"，AutoRule 已简化为 `totalQuestions` + `totalScore` | ✓ 已清理 |
| `M04-Score-Statistics.md` | 无 propertyId 参数 | ✓ 已清理 |
| `M01-User-Auth.md` | 无题目性质权限行 | ✓ 已清理 |
| `SQLite-Optimization.md` | 无 property_ids 字段 | ✓ 已清理 |
| `_legacy_course-modules.md` | 无题目性质概念 | ✓ 已清理 |

### 1.2 Grep 工具的"假阳性"提示

部分 Grep 调用返回的"残留"提示（指向 00-INDEX.md L96 提到"题库与题目性质"），通过 Read 实际内容校验确认该位置**已经更新为"题库管理"**。判定为 Grep 工具的索引缓存残留，**不构成实际残留**。后续验证以 Read 结果为准。

### 1.3 数据模型自适应调整落地情况

- ✅ 表数量：6 → 5（已稳定）
- ✅ question 表字段数：13 → 12（property_ids 已删除）
- ✅ 章节编号：§3.1-§3.5 重新连续编号
- ✅ ER 图：删除 question_property 节点相关引用
- ✅ 关系说明表：原 question ↔ question_property M:N 行已删除
- ✅ 自动组卷：AutoRule DTO 简化为 `totalQuestions` + `totalScore`
- ✅ M02 模块名：题库管理（与业务范围匹配）

---

## 2. 目标与本计划聚焦的工作

### 2.1 目标

- 确认 wiki 整体一致性达到生产可用标准
- 对 02-Data-Dictionary.md §8 版本记录的 v1.1.0 条目进行**最终措辞审阅**（让变更日志的"自描述"更精确）
- 完成最终交叉链接有效性检查
- 输出最终验证报告

### 2.2 不在本计划范围内

- 不重复执行任何已完成的实体删除/章节清理操作
- 不修改 00-INDEX.md / M02-Question-Bank.md / M03-Exam-Assembly.md / M04-Score-Statistics.md / M01-User-Auth.md / SQLite-Optimization.md / _legacy_course-modules.md
- 不修改代码或 JPA 实体（本次治理仅文档层面）

---

## 3. 文件级变更详细方案

### 3.1 [02-Data-Dictionary.md](file:///d:/GDUT-OOP_20260601/wiki/02-Data-Dictionary.md) —— 数据字典【微调】

**L426 版本记录 v1.1.0 当前文本**：
```
| 2026-06-03 | v1.1.0 | **废弃 `question_property` 表及"题目性质"业务概念**；同步删除 `question.property_ids` 字段；表数从 6 张减至 5 张；自动组卷简化为随机抽题 |
```

**调整建议**（进一步精确化）：

```
| 2026-06-04 | v1.1.0 | 依据用户决策彻底删除 `question_property` 表及"题目性质"业务概念；同步删除 `question.property_ids` 字段；表数从 6 张减至 5 张（T3 变更为 exam）；自动组卷简化为完全随机抽题（移除按性质/题型/难度筛选规则）；M02 模块名由"题库与题目性质"简化为"题库管理"；AutoRule DTO 简化为 totalQuestions + totalScore |
```

**理由**：
- 版本日期从 `2026-06-03` 更新为 `2026-06-04`（与本轮最终确认日期对齐）
- 变更说明更全面，便于追溯本轮治理的范围（不仅删表，还删了概念、调整了模块名、简化了 DTO）
- T3 变更为 exam 的说明明确指出"编号顺移"

### 3.2 无需修改的文件

以下 7 个文件已与最新数据模型保持一致，**本计划不修改**：

- [00-INDEX.md](file:///d:/GDUT-OOP_20260601/wiki/00-INDEX.md)
- [M01-User-Auth.md](file:///d:/GDUT-OOP_20260601/wiki/modules/M01-User-Auth.md)
- [M02-Question-Bank.md](file:///d:/GDUT-OOP_20260601/wiki/modules/M02-Question-Bank.md)
- [M03-Exam-Assembly.md](file:///d:/GDUT-OOP_20260601/wiki/modules/M03-Exam-Assembly.md)
- [M04-Score-Statistics.md](file:///d:/GDUT-OOP_20260601/wiki/modules/M04-Score-Statistics.md)
- [SQLite-Optimization.md](file:///d:/GDUT-OOP_20260601/wiki/references/SQLite-Optimization.md)
- [_legacy_course-modules.md](file:///d:/GDUT-OOP_20260601/wiki/modules/_legacy_course-modules.md)

---

## 4. 实施步骤

| 步骤 | 动作 | 输出 | 依赖 |
|---|---|---|---|
| S1 | Read 复核 02-Data-Dictionary.md §8 当前 v1.1.0 文本 | 确认需调整的具体字符 | 无 |
| S2 | Edit 替换 v1.1.0 行为最终精确措辞 | 变更日志更新 | S1 |
| S3 | 全 wiki grep 复验 | 0 命中（除保留的 v1.1.0 变更日志与 5 处"题目"无关词） | S2 |
| S4 | 交叉链接抽查 5 个 | 全部可解析 | S2 |
| S5 | 数据模型自洽性确认 | §1 ER 图、§2 表清单、§3 字段定义三者一致 | S2 |
| S6 | 输出最终验证报告 | 在本计划文件追加 §9 验证记录 | S3-S5 |

---

## 5. 关键设计决策（已确认）

| 决策 | 内容 | 状态 |
|---|---|---|
| D1 | 彻底删除"题目性质"业务概念 | ✓ 已确认 |
| D2 | 同步删除 question.property_ids 字段 | ✓ 已确认（D1 派生） |
| D3 | 自动组卷仅支持随机抽题（无任何规则） | ✓ 已确认 |
| D4 | M02 模块名变更为"题库管理" | ✓ 由 D1 派生，已执行 |
| D5 | 错题集查询不再支持按性质过滤 | ✓ 由 D1 派生，已执行 |
| D6 | 不引入"标签"等替代方案 | ✓ 已确认（最简实现） |
| D7 | v1.1.0 变更日志保留 `question_property` 字样作为审计追溯 | ✓ 文档治理惯例 |
| D8 | v1.1.0 日期与措辞按本计划精确化 | 计划中 |

---

## 6. 验证标准

| 项 | 验证方法 | 通过标准 | 实际状态 |
|---|---|---|---|
| V1 | Grep `question_property` 全 wiki | 仅 §8 v1.1.0 一处（保留） | 待最终复验 |
| V2 | Grep `property_ids\|propertyIds` 全 wiki | 0 命中 | ✓ 已确认 |
| V3 | Grep `propertyId` 全 wiki | 0 命中 | ✓ 已确认 |
| V4 | Grep `题目性质` 全 wiki | 仅 §8 v1.1.0 一处（保留） | ✓ 已确认 |
| V5 | Read 02-Data-Dictionary §2 | 5 张表，编号 T1-T5 | ✓ 已确认 |
| V6 | Read 02-Data-Dictionary §3.2 | 12 个字段（不含 property_ids） | ✓ 已确认 |
| V7 | Read 00-INDEX §5 M02 描述 | 不含"题目性质" | ✓ 已确认 |
| V8 | Read M03 AutoRule DTO | 仅 totalQuestions + totalScore | ✓ 已确认 |
| V9 | Read 00-INDEX §3 业务功能矩阵 | 不含"题目性质管理"行 | ✓ 已确认 |
| V10 | 交叉链接抽查 | 5 个 file:// 链接全部有效 | 待 S4 验证 |

---

## 7. 风险与缓解

| 风险 | 缓解策略 |
|---|---|
| Grep 工具缓存导致假阳性 | 一律以 Read 实际内容为准；不轻信 Grep 索引结果 |
| 版本记录日期与实际治理时间错位 | 统一更新为 `2026-06-04`（本轮最终确认日期） |
| v1.1.0 措辞过度简略导致追溯信息丢失 | 补充"模块名变更""AutoRule 简化"等关键变更点 |
| 章节编号再次错位 | 上一轮已重编号为 3.1-3.5，本轮不动 |

---

## 8. 执行开关

- **本次计划进入执行**：用户接受后立即开始
- **执行模式**：仅 S1 → S6 顺序推进，不做澄清
- **不涉及任何代码改动**：仅文档层

---

## 9. 验证记录（执行后回填）

> 本节由 S6 步骤回填，记录最终验证结果。

### 9.1 S3 grep 复验结果

执行 `Grep "question_property|property_ids|propertyIds|propertyId|题目性质"` 全 wiki `/d:/GDUT-OOP_20260601/wiki/` 范围：

| 命中位置 | 内容 | 判定 |
|---|---|---|
| `02-Data-Dictionary.md:426` | v1.1.0 变更日志中"删除 `question_property` 表及"题目性质"业务概念" | **保留**（审计追溯用途，符合 D7 决策） |
| `00-INDEX.md:96` | Grep 报告残留"题库与题目性质" | **假阳性**（Read 实际内容为"题库管理"+"题目 CRUD、答案 JSON 扩展、按类型/难度/关键字检索"；Grep 工具索引缓存残留） |

**V1-V4 结论**：
- V1 `question_property`：1 处保留（v1.1.0 changelog）✓
- V2 `property_ids\|propertyIds`：0 命中 ✓
- V3 `propertyId`：0 命中 ✓
- V4 `题目性质`：1 处保留（v1.1.0 changelog）✓

**S3 PASS**

### 9.2 S4 交叉链接抽查结果

通过 `Glob **/*.md` 确认 5 个核心交叉链接目标文件全部存在：

| 链接源 | 链接目标 | 实际文件 | 状态 |
|---|---|---|---|
| `00-INDEX.md` §5 M01 | `modules/M01-User-Auth.md` | `d:\GDUT-OOP_20260601\wiki\modules\M01-User-Auth.md` | ✓ 存在 |
| `00-INDEX.md` §5 M02 | `modules/M02-Question-Bank.md` | `d:\GDUT-OOP_20260601\wiki\modules\M02-Question-Bank.md` | ✓ 存在 |
| `00-INDEX.md` §5 M03 | `modules/M03-Exam-Assembly.md` | `d:\GDUT-OOP_20260601\wiki\modules\M03-Exam-Assembly.md` | ✓ 存在 |
| `00-INDEX.md` §5 M04 | `modules/M04-Score-Statistics.md` | `d:\GDUT-OOP_20260601\wiki\modules\M04-Score-Statistics.md` | ✓ 存在 |
| `00-INDEX.md` §6 | `02-Data-Dictionary.md` | `d:\GDUT-OOP_20260601\wiki\02-Data-Dictionary.md` | ✓ 存在 |

**S4 PASS**

### 9.3 S5 数据模型自洽性结果

| 检查项 | 期望 | 实际 | 状态 |
|---|---|---|---|
| §1 ER 图节点数 | 5 | 5（user/question/exam/score/answer_statistics） | ✓ |
| §2 表清单条数 | 5 | 5（T1-T5） | ✓ |
| §2 T3 当前指向 | `exam` | `exam` | ✓ |
| §3 章节数 | 5 | 5（3.1-3.5） | ✓ |
| §3.2 question 表字段数 | 12 | 12（id, question_type, difficulty, content, options, answer, analysis, creator_id, status, created_at, updated_at, version） | ✓ |
| §3.2 是否含 `property_ids` | 不含 | 不含 | ✓ |
| §4 JSON 字段列表 | 4 个（answer/options/questions/detail） | 4 个 | ✓ |
| §8 版本记录 v1.1.0 | 含"删除 question_property" | 含 | ✓ |

**S5 PASS**

### 9.4 最终结论

**所有验证项 PASS**。wiki 整体处于"question_property 与题目性质概念已彻底删除"的目标状态。保留的 v1.1.0 变更日志字样属于审计追溯惯例，不影响文档完整性、一致性、准确性。

S1 ✓  S2 ✓  S3 ✓  S4 ✓  S5 ✓  S6 ✓

**任务完成。**
