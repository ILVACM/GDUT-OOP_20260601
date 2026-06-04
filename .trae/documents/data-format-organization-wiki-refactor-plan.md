# 数据格式整理与 Wiki 重构实施计划

> 计划目标：按用户最新规范（4 张表 + 严格字段裁剪 + 小写枚举命名）系统性重写数据字典章节，补充业务矩阵与数据流转图，保持代码层零改动。  
> 计划范围：**仅 Wiki 文档**（02-Data-Dictionary.md、00-INDEX.md、模块文档 M01-M04）。不涉及任何 `.java`/`.xml`/`.yaml` 文件修改。  
> 关键决策已与用户确认（2026-06-04）：严格 4 表 / 小写 CamelCase 枚举 / 删除 answer_statistics。

---

## 1. 阶段一 · 探索结论

### 1.1 当前状态

| 维度 | 现状 |
|---|---|
| 后端代码 | 仅 `BackendApplication.java`，**无任何 Entity/Repository/Service/Controller**，"保持代码实现结构"在该项目下无冲突。 |
| 现有 Wiki | 5 张表（user / question / exam / score / **answer_statistics**）+ 大量扩展字段 + UPPERCASE 枚举。最新版本 `02-Data-Dictionary.md v1.1.0`（2026-06-04）。 |
| 业务模块文档 | `M01-User-Auth.md` / `M02-Question-Bank.md` / `M03-Exam-Assembly.md` / `M04-Score-Statistics.md` 均与 v1.1.0 数据字典强绑定，引用具体字段名与枚举值。 |
| 全局规范 | `01-Global-Standards.md` §4.1 J6 强制要求乐观锁 `@Version`、§5 强制要求审计字段 `created_at/updated_at`。本次重写将**主动放弃**这两项硬约束（用户已确认严格 4 表），需在计划中说明冲突处置。 |

### 1.2 新旧规范差异摘要

| 维度 | 旧 Wiki v1.1.0 | 新规范（用户输入） | 处置 |
|---|---|---|---|
| 表数量 | 5 张（含 answer_statistics） | 4 张 | **删除** answer_statistics |
| 枚举命名 | UPPERCASE（STUDENT/SINGLE_CHOICE/DRAFT） | 小写/CamelCase（student/SingleChoice/draft） | **全局替换** |
| user 字段 | id, username, password, user_type, admin_subtype, display_name, email, status, created_at, updated_at | id, name, password, type, status | **裁剪** 5 字段 |
| question 字段 | id, question_type, difficulty, content, options, answer, analysis, creator_id, status, created_at, updated_at, version | id, type, context, img, answer, use, correct | **裁剪** 7 字段，移除审计/乐观锁 |
| exam 字段 | id, name, description, start_time, end_time, duration_minutes, status, assembly_mode, questions(JSON), total_score, creator_id, created_at, updated_at | id, exam, status, starttime, endtime, question_sum(JSON) | **裁剪** 6 字段，移除审计/乐观锁/creator |
| score 字段 | id, user_id, exam_id, total_score, detail(JSON), submit_time, duration_seconds, status, created_at | id, user, exam, all, detail(JSON) | **裁剪** 5 字段 |
| 状态类型 | user.status: `Integer`（0/1）| user.status: `Boolean` | **类型变更** |
| 题型枚举 | SINGLE_CHOICE/MULTIPLE_CHOICE/TRUE_FALSE/FILL_BLANK/ESSAY | SingleChoice/MultipleChoice/Judge/Fill/Essay | **Judge 替代 TRUE_FALSE** |
| exam 状态机 | DRAFT → PUBLISHED → ONGOING → FINISHED → ARCHIVED（5 态） | draft → publish → running → done（4 态，无 ARCHIVED） | **简化为 4 态** |
| 乐观锁 | @Version 强制（J6） | 移除 version 字段 | **放弃 J6 对这 4 张表的强制** |
| 审计字段 | created_at/updated_at 强制（§5） | 移除 | **放弃 §5 对这 4 张表的强制** |

---

## 2. 阶段二 · 关键澄清（已完成）

通过 AskUserQuestion 确认的 3 项决策：

1. **数据表与字段范围** → 严格按 4 表新规范（仅列字段）
2. **枚举值命名约定** → 采用新规范的小写/CamelCase
3. **答题统计表** → 删除 `answer_statistics` 表（用 `question.use/correct` 替代）

无遗留未澄清项。

---

## 3. 阶段三 · 实施计划

### 3.1 实施总览

| 步骤 | 操作 | 文件 | 优先级 |
|---|---|---|---|
| 3.2 | 整段重写数据字典主文件 | [02-Data-Dictionary.md](file:///d:/GDUT-OOP_20260601/wiki/02-Data-Dictionary.md) | **P0** |
| 3.3 | 同步更新全局索引（表清单/版本记录/模块映射） | [00-INDEX.md](file:///d:/GDUT-OOP_20260601/wiki/00-INDEX.md) | **P0** |
| 3.4 | 同步更新全局规范 J6/§5 冲突说明 | [01-Global-Standards.md](file:///d:/GDUT-OOP_20260601/wiki/01-Global-Standards.md) | **P0** |
| 3.5 | 同步更新 4 个模块文档的字段引用与枚举引用 | [M01-User-Auth.md](file:///d:/GDUT-OOP_20260601/wiki/modules/M01-User-Auth.md) / [M02-Question-Bank.md](file:///d:/GDUT-OOP_20260601/wiki/modules/M02-Question-Bank.md) / [M03-Exam-Assembly.md](file:///d:/GDUT-OOP_20260601/wiki/modules/M03-Exam-Assembly.md) / [M04-Score-Statistics.md](file:///d:/GDUT-OOP_20260601/wiki/modules/M04-Score-Statistics.md) | **P1** |
| 3.6 | 校验：链接/版本号/枚举一致性 | 全部 Wiki 文件 | **P0** |

### 3.2 步骤详解

#### 步骤 3.2 · 整段重写 [02-Data-Dictionary.md](file:///d:/GDUT-OOP_20260601/wiki/02-Data-Dictionary.md)

**操作**：`Write` 工具整文件覆盖（先 `Read` 现有内容已读过）。

**新文件结构**（章节顺序）：

1. **§1 文档说明**：定位、权威性、变更原则、与 01-Global-Standards 的关系、放弃的硬约束（J6 / §5）说明。
2. **§2 ER 关系总览**：4 张表的关系图（user 1:N question, user 1:N score, exam 1:N score, question 1:1 题内统计字段 use/correct）。
3. **§3 表清单**：精简为 4 张表（user/question/exam/score），标注主键、模块归属、删除 `answer_statistics` 的说明。
4. **§4 字段级定义**（核心，每张表一节）：
   - 每节采用 **双向映射表** 格式：列依次为「字段名称（snake_case / camelCase 对照）| Java 数据类型 | SQLite 数据类型 | 约束条件 | 默认值 | 业务含义」。
   - **`§4.1 user 表`**：id(Long/INTEGER PK AI) / name(String/TEXT NOT NULL) / password(String/TEXT NOT NULL) / type(枚举 UserType: student/teacher/admin / TEXT NOT NULL DEFAULT 'student') / status(Boolean/INTEGER NOT NULL DEFAULT 1)。**显式说明** username→name 的语义不变性。
   - **`§4.2 question 表`**：id / type(枚举 QuestionType: SingleChoice/MultipleChoice/Judge/Fill/Essay) / context(String/TEXT NOT NULL) / img(Boolean/INTEGER NOT NULL DEFAULT 0，**显式记录** ".\\Data\\img\\{id}.* 路径规则") / answer(String 存 JSON / TEXT NOT NULL，**给出 5 种题型的 JSON Schema 草案**) / use(int/INTEGER NOT NULL DEFAULT 0, CHECK >=0) / correct(int/INTEGER NOT NULL DEFAULT 0, CHECK >=0 AND <= use)。
   - **`§4.3 exam 表`**：id / exam(String/TEXT NOT NULL，考试名称) / status(枚举 ExamStatus: draft/publish/running/done / TEXT NOT NULL DEFAULT 'draft') / starttime(LocalDateTime / TEXT NOT NULL) / endtime(LocalDateTime / TEXT NOT NULL, CHECK > starttime) / question_sum(String 存 JSON / TEXT NOT NULL，**给出 JSON Schema 草案**：包含 `totalQuestions` / `totalScore` / `items: [{questionId, score}]`)。
   - **`§4.4 score 表`**：id / user(Long/INTEGER NOT NULL，物理外键 → user.id) / exam(Long/INTEGER NOT NULL，物理外键 → exam.id) / all(int/INTEGER NOT NULL, CHECK >=0) / detail(String 存 JSON / TEXT NOT NULL，**给出 JSON Schema 草案**：包含 `items: [{questionId, userAnswer, correctAnswer, score, isCorrect}]` / `summary: {correctCount, totalCount, accuracy}`)。
5. **§5 枚举定义**（新增独立章节）：列出 UserType / QuestionType / ExamStatus 的完整 Java 枚举源码草案，与 SQLite 存储值一一对应。
6. **§6 "单表 + JSON" 扩展字段设计规范**（精简自 v1.1.0）：仅保留 `question.answer` / `exam.question_sum` / `score.detail` 三处 JSON 字段，删除 `question.options`（合并入 answer）。
7. **§7 业务矩阵设计**（新增核心章节）：
   - **§7.1 用户管理**：通过 user 表 → 增删改查、状态启停、按 type 角色筛选。标注对应模块 M01、API 路径草案（`/api/users`）、DTO 草案。
   - **§7.2 题库管理**：通过 question 表 → 增删改查、img 标志、图片路径匹配规则、按 type 检索、use/correct 统计自维护（提交答卷时 `use+=1`，判分正确时 `correct+=1`）。标注对应模块 M02、API 路径草案。
   - **§7.3 组卷功能**：
     - 手动组卷：教师从 question 表挑选 → 写入 exam.question_sum JSON → 落 exam 表 status=draft。
     - 自动组卷：随机抽签 + 条件抽签（type 筛选、use 频次降权避免抽中热点题）→ 同上手动落 exam 草稿。
     - 落库规范：exam.status 必须初始化为 `draft`，question_sum.items 顺序即为试卷题序。
   - **§7.4 考试管理**：exam 表 → 状态机 draft→publish→running→done、自动时间窗判定（starttime ≤ now < endtime ⇒ running；now ≥ endtime ⇒ done）、学生侧仅看到 running 状态。
   - **§7.5 成绩管理**：score 表 → 提交答卷 → 自动/手动判分 → 写入 score.all + score.detail → 题内统计字段自更新（use++ / correct++）。统计报表：按考试聚合正确率、按学生聚合错题集。
8. **§8 数据流转图与说明**（新增核心章节）：
   - ASCII 流程图：题目录入 → 组卷 → 考试发布 → 学生作答 → 判分 → 成绩记录 → 题内统计自维护 → 报表生成。
   - 标注 4 张表之间的物理 FK 方向（user ← score.user, exam ← score.exam）。
   - 标注 JSON 字段"快照"与"明细"的边界（question_sum 是组卷时快照、detail 是答题时明细）。
9. **§9 字段命名映射约定**（新增）：DB 字段 snake_case ↔ Java 字段 camelCase 对照表（如 `user.type` ↔ `user.type`，无需转换；`score.user` ↔ `score.user`，无需转换；`score.all` ↔ `score.all`，保留以避免与 SQL 关键字 `all` 冲突的显式说明）。
10. **§10 JPA 实体映射约束**（精简自 v1.1.0）：删除 M6（@Version 不再适用）；保留 M1-M5、M7。
11. **§11 待确认 / TODO 项**：列出未在用户规范中明确但实施时需补全的开放点（见 §3.7）。
12. **§12 版本与变更记录**：追加 v2.0.0 条目，记录本次重写范围（与用户决策 1-3 一致）。

**JSON Schema 草案**（在 Wiki 中以代码块形式呈现，标注"草案，实施前需评审"）：

```jsonc
// question.answer - 按 QuestionType 适配
// SingleChoice:  { "correctOption": "A" }
// MultipleChoice: { "correctOptions": ["A", "C"] }
// Judge: { "correct": true }
// Fill: { "blanks": ["went", "goes"] }
// Essay: { "reference": "...", "keywords": ["..."], "scoreRule": "按要点给分" }
```

```jsonc
// exam.question_sum - 组卷快照
{
  "version": 1,
  "items": [
    { "questionId": 123, "score": 5 },
    { "questionId": 124, "score": 10 }
  ],
  "totalQuestions": 2,
  "totalScore": 15
}
```

```jsonc
// score.detail - 答题明细
{
  "version": 1,
  "items": [
    { "questionId": 123, "userAnswer": "B", "correctAnswer": "B", "score": 5, "isCorrect": true }
  ],
  "summary": { "correctCount": 1, "totalCount": 1, "accuracy": 1.0 }
}
```

#### 步骤 3.3 · 同步更新 [00-INDEX.md](file:///d:/GDUT-OOP_20260601/wiki/00-INDEX.md)

**修改点**：
- §3 业务功能矩阵：补充"组卷结果自动生成 draft 状态 exam"的明确表述。
- §5 模块索引：M02-M04 的"核心职责"列更新（删 `answer_statistics` 相关描述，删 difficulty/options/analysis 字段引用）。
- §8 版本与变更记录：追加 2026-06-04 v1.0.1 条目，注明本次为"配合 02-Data-Dictionary.md v2.0.0 同步更新"。

#### 步骤 3.4 · 同步更新 [01-Global-Standards.md](file:///d:/GDUT-OOP_20260601/wiki/01-Global-Standards.md)

**修改点**：在 §4.1 强制项表格下方**新增 §4.1.1 已知例外**章节：

```markdown
### 4.1.1 已知例外（2026-06-04 起生效）

| 模块/表 | 例外内容 | 原因 |
|---|---|---|
| 4 张核心表（user/question/exam/score） | **不强制** J6 乐观锁 `@Version` | 用户最新规范（02-Data-Dictionary.md v2.0.0）已删除 `version` 字段；并发安全由 SQLite 串行写 + 业务层校验兜底 |
| 4 张核心表 | **不强制** §5 审计字段 `created_at/updated_at` | 同上；如需审计日志，远期通过独立 `operation_log` 表实现 |
```

其余章节（J1-J5、J7 等）保持不变。

#### 步骤 3.5 · 同步更新 4 个模块文档

| 文件 | 修改点 |
|---|---|
| [M01-User-Auth.md](file:///d:/GDUT-OOP_20260601/wiki/modules/M01-User-Auth.md) | 字段引用：`username` → `name`；枚举引用：`STUDENT/TEACHER/ADMIN` → `student/teacher/admin`；删除 `admin_subtype` 相关段落。 |
| [M02-Question-Bank.md](file:///d:/GDUT-OOP_20260601/wiki/modules/M02-Question-Bank.md) | 题型枚举：`SINGLE_CHOICE` → `SingleChoice`（及全部 5 个）；删除 `difficulty/options/analysis` 字段相关描述；新增 `img` 标志 + `use/correct` 字段说明；JSON 字段从 4 个减为 1 个（仅 `answer`）。 |
| [M03-Exam-Assembly.md](file:///d:/GDUT-OOP_20260601/wiki/modules/M03-Exam-Assembly.md) | exam 字段：`name/description/duration_minutes/assembly_mode/total_score/creator_id/questions` → `exam/question_sum`（保留 6 字段）；状态机 5 态 → 4 态（删除 ARCHIVED）；JSON 字段从 `questions` 改为 `question_sum`。 |
| [M04-Score-Statistics.md](file:///d:/GDUT-OOP_20260601/wiki/modules/M04-Score-Statistics.md) | score 字段：`user_id/exam_id/total_score/submit_time/duration_seconds/status` → `user/exam/all/detail`；删除 `answer_statistics` 表整章；新增"题内统计自维护"流程描述（写入 score 时同步 question.use/correct）。 |

**注意**：步骤 3.5 涉及 4 个文件的字段级批量替换，工作量较大，但属于**纯文本同步**（不涉及设计变更），可由实施者按 3.2 的新规范逐文件机械替换。

#### 步骤 3.6 · 校验

- [ ] 全文搜索 Wiki 目录确认无残留 `answer_statistics` 字符串
- [ ] 全文搜索 Wiki 目录确认无残留 `STUDENT/SINGLE_CHOICE/DRAFT` 等 UPPERCASE 枚举值
- [ ] 全文搜索 Wiki 目录确认无残留 `username/admin_subtype/difficulty/options/analysis/creator_id/created_at/updated_at/version` 等被裁剪字段名
- [ ] 4 个模块文档的"数据来源"引用链接全部指向新数据字典锚点
- [ ] 00-INDEX.md §8 版本记录与 02-Data-Dictionary.md §12 一致

### 3.7 待评审延伸点（实施前向用户提交，非计划内决策）

下列内容在用户规范中**未明确**但实施时不可避免，实施阶段会在 Wiki 中以"草案/TODO"标注，并建议在评审后定稿：

| 编号 | 开放点 | 草案建议 | 备注 |
|---|---|---|---|
| O1 | `score.user` 与 `score.exam` 物理外键约束的具体行为 | 建议开启 SQLite 外键（PRAGMA foreign_keys=ON）后启用物理 FK，删除 user/exam 时先清理 score | 与 v1.1.0 一致 |
| O2 | `question.answer` JSON 是否需要 `version` 字段 | 建议保留 `{"version":1, ...}` 便于未来升级 | 沿用 §4.1 通用规范 |
| O3 | 自动组卷"条件抽签"支持哪些过滤维度 | 草案支持 `type`（题型）与 `correct/use` 比例（避免热点题） | 用户规范提及"如题目类型、难度等"，但 difficulty 已被裁剪 |
| O4 | `use`/`correct` 自更新的写入位置 | Service 层事务内同步写入 question | 防止数据漂移 |
| O5 | `img=1` 时图片扩展名匹配策略 | Wiki 仅说明"相同数字编号的图片（不包括扩展名）"，实施时建议在 Wiki 补充 `.png/.jpg/.jpeg/.gif` 支持范围 | 实际匹配由代码决定，Wiki 留口 |

### 3.8 不在本次实施范围内（明确排除）

- ❌ 修改任何后端 `.java` 文件（用户明确要求"保持代码实现结构"）
- ❌ 修改 `application.yaml`、`pom.xml`、`Data/English.sqlite`
- ❌ 创建新的 Wiki 顶层文件（业务矩阵与数据流转图作为 02-Data-Dictionary.md 章节）
- ❌ 触碰 `temp/`、`_legacy_course-modules.md`
- ❌ 前端代码（frontend 目录尚未创建）
- ❌ JSON Schema 的最终定稿（标记为草案，待 O1-O5 评审）

---

## 4. 阶段四 · 验证清单

实施完成后逐项勾选：

- [ ] **V1 文档可读性**：4 张表的字段表格列顺序与用户原始规范 1:1 对齐（Java类型列在前、SQLite类型列在后、约束、说明）。
- [ ] **V2 双向映射**：每张表的字段表中均同时出现 Java 数据类型与 SQLite 数据类型两列。
- [ ] **V3 枚举全文统一**：随机抽 10 处枚举引用，全部为小写/CamelCase。
- [ ] **V4 业务矩阵完整性**：§7 含用户管理、题库管理、组卷功能（手动+自动）、考试管理、成绩管理 5 个子节。
- [ ] **V5 数据流转图**：§8 含 ASCII 流程图，覆盖"组卷→考试→答题→判分→统计"完整链路。
- [ ] **V6 模块文档同步**：M01-M04 全部更新完毕，无残留旧字段名/枚举值。
- [ ] **V7 版本记录一致**：00-INDEX.md §8 与 02-Data-Dictionary.md §12 与 01-Global-Standards.md 备注时间戳一致。
- [ ] **V8 代码零改动**：`git status` 确认无 `backend/src/**` 路径下文件变动。

---

## 5. 风险与回滚

| 风险 | 触发条件 | 回滚方案 |
|---|---|---|
| 模块文档同步遗漏字段 | V6 未通过 | 重新执行 3.5 步骤的字段替换表 |
| 枚举命名风格冲突（UPPERCASE 残留） | V3 未通过 | `Grep` 全文扫描后批量替换 |
| 全局规范 J6 例外未被显式声明 | 01-Global-Standards §4.1.1 缺失 | 回退 3.4 步骤补加例外章节 |
| 用户对 JSON Schema 草案不满意 | 评审阶段反馈 | 在 02-Data-Dictionary.md §4.2/4.3/4.4 内调整，保持 TODO 标记 |
| 业务矩阵粒度与用户预期不符 | 用户反馈 | 在 §7 内调整子节切分，保留"题库/组卷/考试/成绩"四大主线 |

---

## 6. 关键参考链接

- 用户规范原文：本计划输入
- 旧数据字典：[02-Data-Dictionary.md v1.1.0](file:///d:/GDUT-OOP_20260601/wiki/02-Data-Dictionary.md)
- 全局索引：[00-INDEX.md](file:///d:/GDUT-OOP_20260601/wiki/00-INDEX.md)
- 全局规范：[01-Global-Standards.md](file:///d:/GDUT-OOP_20260601/wiki/01-Global-Standards.md)
- 模块文档：[M01](file:///d:/GDUT-OOP_20260601/wiki/modules/M01-User-Auth.md) / [M02](file:///d:/GDUT-OOP_20260601/wiki/modules/M02-Question-Bank.md) / [M03](file:///d:/GDUT-OOP_20260601/wiki/modules/M03-Exam-Assembly.md) / [M04](file:///d:/GDUT-OOP_20260601/wiki/modules/M04-Score-Statistics.md)

---

**计划版本**：v1.0  
**制定日期**：2026-06-04  
**预计实施工时**：3.2 ≈ 60%、3.3 ≈ 10%、3.4 ≈ 5%、3.5 ≈ 20%、3.6 ≈ 5%
