# Wiki 调整计划：彻底删除 `question_property` 与"题目性质"概念

> **任务定位**：根据用户决策，将 `question_property` 表及其衍生的"题目性质"业务概念从整个 wiki 体系中**彻底清除**，并将自动组卷简化为"随机抽题"的最简实现。

---

## 1. 当前状态分析

### 1.1 待删除元素的"引用网络"

`question_property` 表虽然只是 02-Data-Dictionary.md 中的一个表，但围绕它形成了**完整的业务能力链**：

```
question_property 表（核心）
   ↑
   │ 外键 / 关联
   │
   ├── question.property_ids（JSON 字段）  ←—— question 表的设计依赖
   ├── 02-Data-Dictionary §3.3 字段定义
   ├── 02-Data-Dictionary §2 表清单 T3
   ├── 02-Data-Dictionary §4 JSON 字段规范表
   ├── 02-Data-Dictionary §6.1 Entity 示例
   ├── 02-Data-Dictionary §7 TODO 项
   ├── 02-Data-Dictionary §8 版本记录
   │
   ├── 业务功能
   │     ├── M02 §3 题目性质建模章节
   │     ├── M02 §6.2 题目性质 API
   │     ├── M02 DTO：QuestionPropertyCreateReq / QuestionPropertyVO
   │     ├── M02 DTO：QuestionCreateReq.propertyIds / QuestionQueryReq.propertyId / QuestionVO.properties
   │     ├── M02 §7 业务规则 5（引用检查）
   │     ├── M02 §8.1/8.2 流程图引用
   │
   ├── 跨模块依赖
   │     ├── M03 自动组卷：AutoConstraint.propertyIds
   │     ├── M03 DTO：ExamCreateAutoReq
   │     ├── M04 错题集查询参数：?propertyId=
   │
   └── 全局影响
         ├── 00-INDEX §3 业务功能矩阵
         ├── 00-INDEX §5 模块索引 M02 描述
         ├── M01 §2.2 权限矩阵
         ├── _legacy_course-modules §2 映射表
         ├── SQLite-Optimization §11.1 JSON 字段列表
```

### 1.2 自动组卷简化的方向

原设计有 4 维度（题型 + 数量 + 难度区间 + 性质），用户决定简化为"随机抽题"。

**最简实现方案**：
- 输入：仅 `totalQuestions`（数量 N）
- 系统：`SELECT N random questions WHERE status=1`
- 分值：自动 = `totalScore / N`（每题等分），或固定每题 1 分
- 不支持的复杂规则：题型分布、难度筛选、性质筛选

---

## 2. 目标数据模型

### 2.1 删表后的表清单

| 编号 | 表名 | 变更 |
|---|---|---|
| T1 | `user` | 不变 |
| T2 | `question` | **删除 `property_ids` 字段** |
| T3 | ~~`question_property`~~ | **整表删除** |
| T4 | `exam` | 不变 |
| T5 | `score` | 不变 |
| T6 | `answer_statistics` | 不变 |

最终：**5 张表**（原 6 张 - 1 张）。

### 2.2 question 表字段调整

| 字段 | 变更 |
|---|---|
| `property_ids` | **删除**（随表整列移除） |
| 其他 12 个字段 | 不变 |

### 2.3 exam 表关联调整

- `exam.questions` JSON 字段中每题的 `snapshot` **不再包含任何"性质"维度**
- 自动组卷 API 输入仅需 `totalQuestions` 一个参数

---

## 3. 文件级变更详细方案

### 3.1 [02-Data-Dictionary.md](file:///d:/GDUT-OOP_20260601/wiki/02-Data-Dictionary.md) —— 数据字典【重度修改】

**删除内容**：

| 行号区域 | 内容 | 操作 |
|---|---|---|
| L60 | `T3` 表清单行 | 删除整行 |
| L65 | T3 脚注（提到 question_property 显式建表） | 删除整段引用 |
| L125 | `property_ids` 字段行 | 删除整行 |
| L160-178 | §3.3 整个小节（题目性质字典表字段定义 + 使用方式 + 设计权衡） | 删除整个小节 |
| L317 | JSON 字段规范表中的 `property_ids` 行 | 删除整行 |
| L336 | §5 通用审计字段中"除用户表、题目性质表..."表述 | 改为"除用户表外" |
| L418-419 | Entity 示例中 `propertyIds` 字段两行 | 删除 |
| L446 | §7 TODO 项 T3（题目性质多级分类） | 删除整行 |
| L456 | §8 版本记录 v1.0.0 中"T3 question_property 显式建表"描述 | 改写为"T3 question_property 表已废弃" |

**调整内容**：
- §2 表清单：表数从 6 变 5，编号 T1/T2/T3/T4/T5（重新分配，T3 变 exam）
- §1 ER 关系总览图：移除 `question_property` 节点相关箭头（实际图中无直接节点，但关联关系需重述）
- §1 关系说明：移除 `question ← question_property` 一行
- §3.2 题目表：字段数从 13 变 12
- §3 重新编号：原 §3.4 考试表 → §3.3 考试表；§3.5 分数表 → §3.4；§3.6 答题统计表 → §3.5
- 新增 v1.1.0 版本记录

**§1 关系说明表调整**：

删除 `question ← question_property` 行（M:N 关联）。但实际上当前表里没有这一行（只有 `user → question` 等）。让我再核实一下 — grep 结果显示 §1 关系表没有直接提及 question_property。OK，这一项无需调整。

### 3.2 [M02-Question-Bank.md](file:///d:/GDUT-OOP_20260601/wiki/modules/M02-Question-Bank.md) —— 题库模块【重度修改】

**删除内容**：

| 区域 | 内容 |
|---|---|
| L1 标题 | "题库与题目性质管理" → 改为"题库管理" |
| L3 头注 | 移除"题目性质（元数据）管理" |
| L5 数据来源引用 | 删除"§3.3 题目性质字典表"引用 |
| L17 业务场景 | 删除"按类型 / 难度 / **性质** / 关键字 过滤"中的"性质" |
| L20 业务场景 | 整行"题目性质维护"删除 |
| L49-94 | **整个 §3 章节**（题目性质（元数据）建模）删除 |
| L133 | `QuestionCreateReq` 中 `List<Long> propertyIds` 删除 |
| L145 | `QuestionQueryReq` 中 `Long propertyId` 删除 |
| L153 | 题目性质创建请求标题（仅标题，无 DTO 内容 — 已含在 §3 中删除） |
| L175 | `QuestionVO` 中 `List<QuestionPropertyVO> properties` 删除 |
| L183 | 题目性质 VO 标题（已含在 §3 中删除） |
| L211-218 | 整个 §6.2 题目性质相关 API 表删除 |
| L228 | §7 业务规则 5（题目性质引用检查）删除 |
| L246 | §8.1 创建流程中"校验 propertyIds 存在性"步骤删除 |
| L256-269 | §8.2 题目检索（按性质筛选）流程图整段删除 → 改为更通用的"按关键字检索"流程 |
| L303 | §10 W3 题目标签 TODO（与性质相关的扩展项）删除 |
| L310 | 文档底部相关文档引用中"§3.3 题目性质字典表"删除 |

**新增内容**：
- §1 业务场景：移除"按性质"过滤后，调整为"按类型/难度/关键字过滤"
- 模块名 / 标题更新为"题库管理"（不再含"题目性质"）

### 3.3 [M03-Exam-Assembly.md](file:///d:/GDUT-OOP_20260601/wiki/modules/M03-Exam-Assembly.md) —— 考试与组卷【中度修改】

**删除内容**：

| 区域 | 内容 |
|---|---|
| L17 业务场景"自动组卷" | "配置规则（**数量、类型、难度、性质**）" → 改为"配置数量，系统随机抽题" |
| L95-130 | §3.2 自动组卷整段：保留核心抽题逻辑，但删除 `AutoRule.constraints` 多约束结构 |
| L102 | 自动组卷规则示例中的 `propertyIds` 字段删除 |
| L114 | 抽题算法注释中"+ 性质"删除 |
| L223 | `AutoConstraint` 中 `List<Long> propertyIds` 删除 |

**调整内容**：
- `AutoRule` 简化为只含 `totalQuestions`（和可选 `totalScore`）
- 算法伪代码简化为单行随机抽题
- 业务规则：删除"按性质抽题"相关规则

**新版 AutoRule 示例**：
```java
public record AutoRule(
    @NotNull @Min(1) Integer totalQuestions,
    Integer totalScore  // 可选；默认 = totalQuestions * 5
) {}
```

**新版算法伪代码**：
```java
public List<ExamQuestionItem> autoAssemble(AutoRule rule) {
    // 按 status=1 随机抽 N 道题
    List<Question> candidates = questionRepository.findByStatus(QuestionStatus.PUBLISHED);
    Collections.shuffle(candidates);
    int scoreEach = (rule.getTotalScore() != null ? rule.getTotalScore() : rule.getTotalQuestions() * 5) 
                    / rule.getTotalQuestions();
    return candidates.subList(0, rule.getTotalQuestions()).stream()
        .map(q -> new ExamQuestionItem(q.getId(), scoreEach))
        .toList();
}
```

### 3.4 [M04-Score-Statistics.md](file:///d:/GDUT-OOP_20260601/wiki/modules/M04-Score-Statistics.md) —— 分数与统计【轻度修改】

**删除内容**：

| 区域 | 内容 |
|---|---|
| L280 | 错题集 API 查询参数 `?propertyId=&page=` 中 `propertyId` 参数删除 |

**调整内容**：
- 错题集 API 查询参数简化为 `?page=&size=`
- 移除文档任何对按性质筛选错题的描述

### 3.5 [00-INDEX.md](file:///d:/GDUT-OOP_20260601/wiki/00-INDEX.md) —— 项目总览【轻度修改】

**删除/调整**：

| 区域 | 内容 |
|---|---|
| L46 | 业务功能矩阵中"题目性质管理"行整行删除 |
| L48 | "自动组卷"行：删"性质"二字 |
| L97 | 模块索引 M02 描述：删"题目性质元数据、" |

### 3.6 [M01-User-Auth.md](file:///d:/GDUT-OOP_20260601/wiki/modules/M01-User-Auth.md) —— 用户认证【轻度修改】

**删除内容**：

| 区域 | 内容 |
|---|---|
| L49 | 权限矩阵中"题目性质管理"行整行删除 |

### 3.7 [SQLite-Optimization.md](file:///d:/GDUT-OOP_20260601/wiki/references/SQLite-Optimization.md) —— SQLite 优化【轻度修改】

**删除/调整**：

| 区域 | 内容 |
|---|---|
| L295 | §11.1 中"question.answer / question.options / question.property_ids" 列表删除 `property_ids` |
| L295 附近 | 检查是否还有其他提及 `property_ids` 的位置 |

### 3.8 [_legacy_course-modules.md](file:///d:/GDUT-OOP_20260601/wiki/modules/_legacy_course-modules.md) —— 旧模块归档【微调】

**调整**：

| 区域 | 内容 |
|---|---|
| L26 | 业务概念映射：删"+ 题目性质"，简化为"题库" |
| §4 演进时间线 | 删掉"题目性质"等历史表述的提及（如有） |

---

## 4. 实施步骤

| 步骤 | 动作 | 输出 | 依赖 |
|---|---|---|---|
| S1 | 修订 02-Data-Dictionary.md | 删除 T3、property_ids、相关脚注、Entity 示例、版本记录 | 无 |
| S2 | 修订 M02-Question-Bank.md | 删除整个题目性质章节、相关 DTO/API/规则 | S1 |
| S3 | 修订 M03-Exam-Assembly.md | 简化自动组卷为随机抽题 | S1 |
| S4 | 修订 M04-Score-Statistics.md | 删除错题集 propertyId 参数 | 无 |
| S5 | 修订 00-INDEX.md | 删除题目性质管理行 | 无 |
| S6 | 修订 M01-User-Auth.md | 删除权限矩阵中"题目性质管理"行 | 无 |
| S7 | 修订 SQLite-Optimization.md | 删除 property_ids 字段 | S1 |
| S8 | 修订 _legacy_course-modules.md | 简化业务概念映射 | 无 |
| S9 | 全 wiki 校验 | grep "question_property\|property_ids\|propertyIds\|propertyId\|题目性质" 全部应为 0 | S1-S8 |
| S10 | 交叉链接完整性检查 | 抽查 5 个 file:// 链接仍可点击 | S1-S8 |
| S11 | 02-Data-Dictionary.md 版本记录追加 v1.1.0 | 增补变更说明 | S9 |

---

## 5. 关键设计决策（已由用户确认）

| 决策 | 内容 | 状态 |
|---|---|---|
| D1 | 彻底删除"题目性质"业务概念 | ✓ 已确认 |
| D2 | 同步删除 question.property_ids 字段 | ✓ 已确认（D1 派生） |
| D3 | 自动组卷仅支持随机抽题（无任何规则） | ✓ 已确认 |
| D4 | M02 模块名变更为"题库管理" | 由 D1 派生，需执行 |
| D5 | 错题集查询不再支持按性质过滤 | 由 D1 派生，需执行 |
| D6 | 不引入"标签"等替代方案 | ✓ 已确认（用户明确"最简实现"） |

---

## 6. 验证标准

整合完成后逐项验证：

| 项 | 验证方法 | 通过标准 |
|---|---|---|
| V1 | `grep -r "question_property" wiki/` | 0 命中 |
| V2 | `grep -r "property_ids\|propertyIds" wiki/` | 0 命中 |
| V3 | `grep -r "题目性质" wiki/` | 0 命中 |
| V4 | `grep -r "propertyId" wiki/` | 0 命中（除注释中"property field" 等无关词） |
| V5 | 检查 02-Data-Dictionary §2 表清单 | 5 张表，编号 T1-T5 |
| V6 | 检查 question 表字段数 | 12 个字段（不含 property_ids） |
| V7 | 检查 M02 模块标题 | 不含"题目性质" |
| V8 | 检查 M03 AutoRule | 仅 totalQuestions，可选 totalScore |
| V9 | 检查 00-INDEX §3 业务功能矩阵 | 不含"题目性质管理"行 |
| V10 | 检查 M01 权限矩阵 | 不含"题目性质管理"行 |
| V11 | 检查 SQLite-Optimization §11.1 | 不含 property_ids 字段 |
| V12 | 交叉链接完整性 | 抽查 5 个 file:// 链接全部有效 |
| V13 | 数据模型自洽性 | 题目表不含任何"性质"维度引用 |

---

## 7. 风险与缓解

| 风险 | 缓解策略 |
|---|---|
| 误删非"题目性质"相关的 property 字样 | 严格按 grep 模式 `property_ids\|propertyIds\|propertyId` 验证，仅删这些精准匹配 |
| 章节编号错位（删除 §3.3 后 §3.4 变 §3.3） | 全部使用 `replace_all=false` 增量编辑，并人工复核最终 §3 子节编号 |
| 自动组卷简化后业务功能是否仍够用 | 用户已明确"最简实现"，无需验证业务完整性 |
| wiki 链接锚点漂移 | §3.3 锚点变成 §3.3 考试表（其他文件引用需同步更新） |

---

## 8. 执行开关

- **本次计划不进入执行**：进入 Phase 4 通知用户审阅
- **用户接受后**：从 S1 开始按顺序执行；不再做澄清
- **执行模式**：按 S1→S11 顺序连续推进，每完成一组文件报告一次进度
