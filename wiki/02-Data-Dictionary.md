# 数据字典与 JPA 实体映射（v4.0.0）

> 本文档是系统数据库结构的**唯一权威定义**。所有 JPA Entity、SQL DDL、Repository 查询都必须严格对齐本文档。如发现本文档设计有缺陷，必须先在本文档提出修改并经评审，再修改代码。
>
> **重要**：本文档采用 **Java ↔ SQLite 双向数据类型映射** 表格形式整理，所有字段同时给出 Java 数据类型与 SQLite 数据类型，便于跨层对照。
>
> **v3.0.0 重大变更**：基于后端数据层实际实现状态全面校准文档。核心修正：① 主键与外键 Java 类型由 `Long` 修正为 `Integer`（SQLite INTEGER 自增范围足够且与 `columnDefinition = "INTEGER"` 一致）；② `user.status` / `question.img` 由 `Boolean` 修正为 `Integer`（SQLite 以 INTEGER 0/1 存储布尔值，JPA `ddl-auto=validate` 要求 Java 类型与 DDL 列类型严格一致）；③ `exam.starttime` / `exam.endtime` 由 `LocalDateTime` 修正为 `String`（ISO 8601 文本存储，避免 Hibernate 方言转换问题）；④ Score 实体外键字段采用 `@Column` 而非 `@JoinColumn`（保持 4 表独立设计，详见 §10 M4）；⑤ 新增 §13 实现状态追踪。详见 §12 版本记录。

---

## 1. 文档说明

### 1.1 定位

本文档是项目**第 0 层**基座文档（与 [00-INDEX.md](file:///d:/GDUT-OOP_20260601/wiki/00-INDEX.md)、[01-Global-Standards.md](file:///d:/GDUT-OOP_20260601/wiki/01-Global-Standards.md) 同级）。所有 Entity/DDL/Repository 编写前**必须**先读本文件，再按需读取对应模块文档（M01-M04）。

### 1.2 与 01-Global-Standards 的关系

- 通用 API 契约、异常处理、代码分层、前端规范等"做什么"层面的约束，遵循 01-Global-Standards.md。
- 本文档专注"表结构 + 字段映射 + 业务矩阵"层面的约束。
- 冲突时优先级：本文档字段定义 > 01-Global-Standards 通用规则。

### 1.3 v2.0.0 主动放弃的硬约束

为保持规范精简，本次重写**主动放弃**以下两条原 [01-Global-Standards.md §4.1 J6 / §5](file:///d:/GDUT-OOP_20260601/wiki/01-Global-Standards.md#4-jpa-规范) 强制要求：

| 原约束 | v2.0.0 处置 | 替代方案 |
|---|---|---|
| J6 乐观锁 `@Version`（强并发安全） | 4 张核心表均**不引入** `version` 字段 | 业务层互斥校验 + SQLite 串行写兜底 |
| §5 审计字段 `created_at/updated_at` | 4 张核心表均**不引入** | 远期通过独立 `operation_log` 表实现审计追溯 |

此例外已同步在 [01-Global-Standards.md §4.1.1](file:///d:/GDUT-OOP_20260601/wiki/01-Global-Standards.md) 显式声明。

### 1.4 v3.0.0 Java 类型校准说明

v3.0.0 基于后端数据层实际实现（Entity + Repository + DDL + 测试）对文档进行了全面校准。以下类型变更均源于 `ddl-auto=validate` 模式下 Hibernate 对 Java 类型与 DDL 列类型严格一致的要求：

| 字段 | v2.0.0 文档类型 | v3.0.0 实际类型 | 校准原因 |
|---|---|---|---|
| 所有 `id` 主键 | `Long` | `Integer` | SQLite 自增 INTEGER 与 Java `Integer` 对齐；`columnDefinition = "INTEGER"` 显式声明 |
| `score.user` / `score.exam` 外键 | `Long` | `Integer` | 与主键类型一致，物理外键引用 `user.id` / `exam.id`（均为 `Integer`） |
| `user.status` | `Boolean` | `Integer` | SQLite 以 `INTEGER(0/1)` 存储布尔值，`validate` 模式下 `Boolean` 映射为 `TINYINT` 导致校验失败 |
| `question.img` | `Boolean` | `Integer` | 同上 |
| `exam.starttime` / `exam.endtime` | `LocalDateTime` | `String` | SQLite TEXT 存储 ISO 8601 字符串；Hibernate 社区方言对 `LocalDateTime` → TEXT 的自动转换在 `validate` 模式下存在类型推断差异，直接使用 `String` 避免问题 |
| `score.user` / `score.exam` 映射 | `@JoinColumn` | `@Column` | 4 表独立设计（无 JPA `@ManyToOne` 关联），物理外键由 DDL `FOREIGN KEY` 约束承载（详见 §10 M4） |

---

## 2. ER 关系总览

```
                          ┌────────────────┐
                          │     user       │ 1
                          │  (用户表)       │
                          │ id PK          │
                          └───────┬────────┘
                                  │ 1
                                  │
                  ┌───────────────┴───────────────┐
                  │ N                             │ N
          ┌───────▼─────────┐             ┌───────▼─────────┐
          │    question     │             │      score      │
          │   (题目表)        │             │    (分数表)      │
          │ id PK           │             │ id PK           │
          │ use, correct ◀──┼───统计自维护──┤ user (FK)       │
          │ (内禀字段)        │             │ exam (FK) ──┐   │
          └───────┬─────────┘             │ all, detail │   │
                  │ 1                     └───────┬─────┘   │
                  │ N                             │ 1       │ N
                  │                               │         │
                  │   ┌───────────────────────────┘         │
                  │   │                                     │
                  │   ▼                                     │
                  │ ┌────────────────┐ 1               ┌─────▼──────┐
                  │ │      exam      │ ◀───────────────│            │
                  │ │   (考试表)      │ N               │            │
                  │ │ id PK          │                 │            │
                  │ │ question_sum   │                 │            │
                  │ │ (JSON 快照)     │                 └────────────┘
                  │ └────────────────┘
                  │
                  │ 关联方式：question_sum JSON 字段"快照式"引用，
                  │          无中间表，题目后续修改不影响已组卷考试
```

**关系说明**：

| 关系 | 类型 | 表达方式 |
|---|---|---|
| user → question | 隐式 1 : N | **不建外键字段**；`question` 不存 `creator_id`，通过 `score` 反向追溯出题老师 |
| user → score (考生) | 1 : N | `score.user` 物理外键 → `user.id` |
| exam → score (被考) | 1 : N | `score.exam` 物理外键 → `exam.id` |
| exam → question (组卷) | M : N（快照） | 由 `exam.question_sum` JSON 字段表达（组卷时一次性快照） |
| question 题内统计 | 1 : 1（逻辑） | `question.use` / `question.correct` 字段在判分时由 Service 同步维护 |

> **设计权衡**：考试与题目采用 JSON 字段而非中间表，理由是"组卷"是一次性快照行为，题目后续修改/删除不应回溯影响已组卷的考试。题内统计直接落在 `question` 表上，避免额外连接查询。

---

## 3. 表清单

| 编号 | 表名（英文） | 中文 | 主键 | 物理外键 | 模块 |
|---|---|---|---|---|---|
| T1 | `user` | 用户表 | `id` | — | M01 |
| T2 | `question` | 题目表 | `id` | — | M02 |
| T3 | `exam` | 考试表 | `id` | — | M03 |
| T4 | `score` | 分数表 | `id` | `user` → user.id，`exam` → exam.id | M04 |

> **历史变更**：v1.x 版本曾存在第 5 张表 `answer_statistics`（答题统计表）。v2.0.0 起**已删除**，相关统计功能由 `question.use` / `question.correct` 字段直接承载，简化数据模型。

---

## 4. 字段级定义

> **字段命名约定**：本版本 4 张表的字段名在 DB 与 Java 端**完全一致**（无 snake_case ↔ camelCase 转换需求），如 `user.type`、`score.all` 等。这是 v2.0.0 的有意简化，便于跨层对照与 SQL 直查。

### 4.1 用户表 `user`

| 字段名称 | Java 数据类型 | SQLite 数据类型 | 约束条件 | 默认值 | 业务含义 |
|---|---|---|---|---|---|
| `id` | `Integer` | `INTEGER` | NOT NULL，主键，自增（AUTOINCREMENT） | — | 唯一标识每个用户 |
| `name` | `String` | `TEXT` | NOT NULL | — | 用户名 |
| `password` | `String` | `TEXT` | NOT NULL | — | 用户登录密码（建议 BCrypt 哈希，**严禁明文**） |
| `type` | `UserType`（枚举） | `TEXT` | NOT NULL | `'student'` | 用户类型枚举：`student` / `teacher` / `admin` |
| `status` | `Integer` | `INTEGER` | NOT NULL | `1` | 用户状态，`1` 表示启用，`0` 表示禁用（v3.0.0：由 `Boolean` 修正为 `Integer`，与 SQLite INTEGER 存储一致） |

**索引建议**：

| 索引名 | 字段 | 唯一性 | 用途 |
|---|---|---|---|
| `idx_user_name` | `name` | UNIQUE | 登录查询（建议加 UNIQUE，避免重名） |
| `idx_user_type` | `type` | — | 按角色筛选 |

> **实施备注**：v1.x 版本中字段为 `username` / `user_type` / `admin_subtype` / `display_name` / `email` / `status(INTEGER)`。v2.0.0 简化为 `name` / `type` / `status(Boolean)`，删除 admin 细分角色与扩展信息字段；如需扩展，建议通过未来 `user_profile` 关联表实现（不在本版本）。

---

### 4.2 题目表 `question`

| 字段名称 | Java 数据类型 | SQLite 数据类型 | 约束条件 | 默认值 | 业务含义 |
|---|---|---|---|---|---|
| `id` | `Integer` | `INTEGER` | NOT NULL，主键，自增 | — | 唯一标识每个题目 |
| `type` | `QuestionType`（枚举） | `TEXT` | NOT NULL | — | 题目类型枚举：`SingleChoice` / `MultipleChoice` / `Judge` / `Fill` / `Essay` |
| `context` | `String` | `TEXT` | NOT NULL | — | 题目题干具体文本内容 |
| `img` | `Integer` | `INTEGER` | NOT NULL | `0` | 题目图片标识：`1` 表示带图片，`0` 表示不带图片（v3.0.0：由 `Boolean` 修正为 `Integer`）。**若为 1，则在路径 `.\\Data\\img\\` 下匹配与当前题目 `id` 相同数字编号的图片（不包括扩展名）** |
| `answer` | `String`（JSON 文本） | `TEXT` | NOT NULL | — | 题目答案，按 `type` 自适应不同 JSON 结构（见 §4.2.1） |
| `use` | `int`（`Integer`） | `INTEGER` | NOT NULL，CHECK `>= 0` | `0` | 题目在考试中出现的次数（被抽中次数） |
| `correct` | `int`（`Integer`） | `INTEGER` | NOT NULL，CHECK `>= 0 AND <= use` | `0` | 题目在考试中被回答正确的次数 |

#### 4.2.1 `answer` JSON 字段结构（按 `type` 适配）

> **草案**（实施前需评审；详见 §11 开放点 O2）

```jsonc
// SingleChoice（单选题）
{ "version": 1, "correctOption": "A", "options": ["A", "B", "C", "D"] }

// MultipleChoice（多选题）
{ "version": 1, "correctOptions": ["A", "C"], "options": ["A", "B", "C", "D"] }

// Judge（判断题）
{ "version": 1, "correct": true }

// Fill（填空题）
{ "version": 1, "blanks": ["went", "goes"] }

// Essay（简答题）
{ "version": 1, "reference": "参考答案文本", "keywords": ["关键点1", "关键点2"], "scoreRule": "按要点给分" }
```

**字段语义说明**：

- `options`（SingleChoice / MultipleChoice）：选项标签列表（仅 A/B/C/D 等标签，不含选项文本；选项文本由 `context` 描述或前端展示逻辑承担）。
- `correctOption`（SingleChoice）：单选正确选项标签。
- `correctOptions`（MultipleChoice）：多选正确选项标签数组。
- `correct`（Judge）：判断题正确答案，`true` 或 `false`。
- `blanks`（Fill）：填空题所有空的正确答案数组，按空出现顺序排列。
- `reference` / `keywords` / `scoreRule`（Essay）：简答题参考答案 + 关键词 + 评分规则。

#### 4.2.2 `img` 路径匹配规则

- `img = 0`：题目无图，前端不渲染图片区域。
- `img = 1`：题目带图，前端**按题目 id 数字**在 `.\Data\img\` 目录下匹配同名图片文件，扩展名可为 `.png` / `.jpg` / `.jpeg` / `.gif` 中任意一种（具体支持范围由实现决定，详见 §11 O5）。
- **示例**：题目 `id=42` 且 `img=1`，则尝试加载 `.\Data\img\42.png` / `.\Data\img\42.jpg` 等，找到第一个存在即用。

#### 4.2.3 `use` / `correct` 自维护规则

| 触发时机 | 写入逻辑 |
|---|---|
| 组卷时，题目被纳入 `exam.question_sum.items` | `use += 1` |
| 学生提交答卷且该题判分 `isCorrect = true` | `correct += 1` |
| 学生提交答卷但该题判分 `isCorrect = false` 或未作答 | `use`、`correct` 均不变 |

> **不变量约束**：`0 <= correct <= use`（已在表 CHECK 中声明）。自维护必须在 Service 层事务内执行（详见 §7.5）。

**索引建议**：

| 索引名 | 字段 | 唯一性 | 用途 |
|---|---|---|---|
| `idx_question_type` | `type` | — | 按题型检索 |
| `idx_question_use` | `use` | — | 热度统计 / 降权抽题 |

> **实施备注**：v1.x 版本中字段为 `question_type` / `difficulty` / `content` / `options(JSON)` / `analysis` / `creator_id` / `status` / `created_at` / `updated_at` / `version`。v2.0.0 精简为 7 字段，`options` JSON 合并入 `answer`，`difficulty` 暂不引入（远期可加），无审计与乐观锁。

---

### 4.3 考试表 `exam`

| 字段名称 | Java 数据类型 | SQLite 数据类型 | 约束条件 | 默认值 | 业务含义 |
|---|---|---|---|---|---|
| `id` | `Integer` | `INTEGER` | NOT NULL，主键，自增 | — | 唯一标识每个考试 |
| `exam` | `String` | `TEXT` | NOT NULL | — | 考试名称，用于标识考试 |
| `status` | `ExamStatus`（枚举） | `TEXT` | NOT NULL | `'draft'` | 考试状态枚举：`draft` / `publish` / `running` / `done` |
| `starttime` | `String` | `TEXT`（ISO 8601 字符串） | NOT NULL | — | 考试开始时间（v3.0.0：由 `LocalDateTime` 修正为 `String`，避免 Hibernate 方言类型转换问题） |
| `endtime` | `String` | `TEXT`（ISO 8601 字符串） | NOT NULL，CHECK `endtime > starttime` | — | 考试结束时间（v3.0.0：同上） |
| `question_sum` | `String`（JSON 文本） | `TEXT` | NOT NULL | — | 考试题目汇总，组卷时一次性快照（见 §4.3.1） |

#### 4.3.1 `question_sum` JSON 字段结构

> **草案**（实施前需评审；详见 §11 开放点 O2）

```jsonc
{
  "version": 1,
  "items": [
    { "questionId": 123, "score": 5,  "type": "SingleChoice" },
    { "questionId": 124, "score": 10, "type": "MultipleChoice" }
  ],
  "totalQuestions": 2,
  "totalScore": 15
}
```

**字段语义说明**：

- `version`：JSON 结构版本号，便于未来升级兼容。
- `items`：考试包含的所有题目快照数组，**顺序即为试卷题序**。
- `items[].questionId`：题目 id（外键 → question.id，逻辑引用）。
- `items[].score`：该题在本次考试中的分值。
- `items[].type`：冗余存储题型枚举，便于前端快速判型展示（无需二次查表）。
- `totalQuestions`：题目总数（冗余，便于快速校验）。
- `totalScore`：试卷总分（冗余，便于快速展示）。

#### 4.3.2 状态机（4 态）

```
       发布             自动：starttime ≤ now < endtime
draft ──────→ publish ─────────────────────────────→ running
   │              │                                     │
   │ 删除（仅 draft） 撤回（仅 publish）                         │ 自动：now ≥ endtime
   │              ↓                                     ↓
   │           draft                                   done
   │
   └──→ 删除（仅 draft）
```

| 状态 | 含义 | 可执行操作 |
|---|---|---|
| `draft` | 草稿，未发布 | 编辑、删除、发布（→ publish） |
| `publish` | 已发布，等待开考 | 撤回为 `draft` |
| `running` | 进行中（`starttime ≤ now < endtime`） | 仅查看、提交答卷 |
| `done` | 已结束（`now ≥ endtime`） | 仅查看、统计 |

> **状态流转约束**：
> - `draft` ↔ `publish` 由教师操作触发。
> - `publish` → `running` 与 `running` → `done` 由系统按时间窗**自动判定**（查询时计算，不强制落库更新；如需落库可由定时任务周期性同步）。
> - **`done` 是终态**，本版本不引入 `archived` 归档态（详见 §11）。

**索引建议**：

| 索引名 | 字段 | 唯一性 | 用途 |
|---|---|---|---|
| `idx_exam_status` | `status` | — | 按状态筛选 |
| `idx_exam_time` | `starttime`, `endtime` | — | 时间窗查询 |

> **实施备注**：v1.x 版本中字段为 `name` / `description` / `start_time` / `end_time` / `duration_minutes` / `assembly_mode` / `questions(JSON)` / `total_score` / `creator_id` / `created_at` / `updated_at`，状态机为 5 态（含 ARCHIVED）。v2.0.0 精简为 6 字段，状态机 4 态。

---

### 4.4 分数表 `score`

| 字段名称 | Java 数据类型 | SQLite 数据类型 | 约束条件 | 默认值 | 业务含义 |
|---|---|---|---|---|---|
| `id` | `Integer` | `INTEGER` | NOT NULL，主键，自增 | — | 唯一标识每条分数记录 |
| `user` | `Integer` | `INTEGER` | NOT NULL，**物理外键** → `user.id` | — | 关联用户表用户 id，标识考生（v3.0.0：由 `Long` 修正为 `Integer`，映射方式由 `@JoinColumn` 修正为 `@Column`） |
| `exam` | `Integer` | `INTEGER` | NOT NULL，**物理外键** → `exam.id` | — | 关联考试表考试 id，标识被考（v3.0.0：同上） |
| `all` | `int`（`Integer`） | `INTEGER` | NOT NULL，CHECK `>= 0` | — | 用户在考试中的总分数 |
| `detail` | `String`（JSON 文本） | `TEXT` | NOT NULL | — | 用户在考试中的每个题目分数明细（见 §4.4.1） |

#### 4.4.1 `detail` JSON 字段结构

> **草案**（实施前需评审；详见 §11 开放点 O2）

```jsonc
{
  "version": 1,
  "items": [
    {
      "questionId": 123,
      "userAnswer": "B",
      "correctAnswer": "B",
      "score": 5,
      "isCorrect": true
    },
    {
      "questionId": 124,
      "userAnswer": ["A", "C"],
      "correctAnswer": ["A", "D"],
      "score": 0,
      "isCorrect": false
    }
  ],
  "summary": {
    "correctCount": 1,
    "totalCount": 2,
    "accuracy": 0.5
  }
}
```

**字段语义说明**：

- `version`：JSON 结构版本号。
- `items`：逐题答题明细数组，与 `exam.question_sum.items` 顺序一致。
- `items[].userAnswer`：考生作答（多态：单选为字符串，多选为数组，填空为数组，简答为字符串）。
- `items[].correctAnswer`：标准答案（从 `question.answer` JSON 反序列化得到）。
- `items[].score`：该题实际得分。
- `items[].isCorrect`：判分结果（客观题系统判分；主观题 `Essay` 需教师评卷后写入）。
- `summary`：聚合统计（`correctCount` 正确题数、`totalCount` 总题数、`accuracy` 正确率）。

**唯一约束建议**：

| 约束名 | 字段组合 | 唯一性 | 用途 |
|---|---|---|---|
| `uk_score_user_exam` | `user`, `exam` | UNIQUE | 一人一考仅一条记录，重复提交时执行 UPDATE |

**索引建议**：

| 索引名 | 字段 | 用途 |
|---|---|---|
| `idx_score_user` | `user` | 个人成绩查询 |
| `idx_score_exam` | `exam` | 考试聚合统计 |

**物理外键**：建议开启 SQLite 外键（`PRAGMA foreign_keys = ON`）后启用物理 FK；删除 `user`/`exam` 时需先清理 `score` 记录（详见 §11 开放点 O1）。

> **实施备注**：v1.x 版本中字段为 `user_id` / `exam_id` / `total_score` / `submit_time` / `duration_seconds` / `status` / `created_at`。v2.0.0 精简为 5 字段，`user_id` → `user`、`exam_id` → `exam`、`total_score` → `all`（命名简化），移除提交时间/用时/状态字段（远期按需补回）。

---

## 5. 枚举定义

### 5.1 `UserType`（用户类型）

```java
package com.cps.backend.modules.M01userauth.enums;

/**
 * 用户类型枚举。SQLite 中以字符串字面量存储（与 name() 一致）。
 * 字段：user.type
 */
public enum UserType {
    /** 学生 */
    student("学生"),
    /** 教师 */
    teacher("老师"),
    /** 管理员 */
    admin("管理员");

    private final String description;

    UserType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
```

### 5.2 `QuestionType`（题目类型）

```java
package com.cps.backend.modules.M02questionbank.enums;

/**
 * 题目类型枚举。SQLite 中以字符串字面量存储（与 name() 一致）。
 * 字段：question.type
 *
 * 题型特性对比：
 * - SingleChoice   需要 options，系统自动判分
 * - MultipleChoice 需要 options，系统自动判分
 * - Judge          无 options，系统自动判分
 * - Fill           无 options，系统按空匹配自动判分
 * - Essay          无 options，需教师评卷
 */
public enum QuestionType {
    SingleChoice("单选题"),
    MultipleChoice("多选题"),
    Judge("判断题"),
    Fill("填空题"),
    Essay("简答题");

    private final String description;

    QuestionType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
```

### 5.3 `ExamStatus`（考试状态）

```java
package com.cps.backend.modules.M03examassembly.enums;

/**
 * 考试状态枚举。SQLite 中以字符串字面量存储（与 name() 一致）。
 * 字段：exam.status
 *
 * 状态机：
 *   draft ↔ publish（教师操作）
 *   publish → running（系统：starttime ≤ now < endtime）
 *   running → done（系统：now ≥ endtime）
 *   done 为终态
 */
public enum ExamStatus {
    draft("草稿"),
    publish("已发布"),
    running("进行中"),
    done("已结束");

    private final String description;

    ExamStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
```

> **JPA 映射**：所有枚举均使用 `@Enumerated(EnumType.STRING)` 注解 + `@Column(name = "...")` 显式标注，确保 SQLite 中存储的字符串与枚举 `name()` 字面量一致（student / SingleChoice / draft 等）。

---

## 6. "单表 + JSON" 扩展字段设计规范

为兼顾灵活性与查询性能，本系统在以下三处使用 JSON 字段（v1.x 的 `question.options` 已合并入 `question.answer`，故从 4 处减为 3 处）：

| 字段 | 所在表 | 用途 |
|---|---|---|
| `answer` | `question` | 适配 5 种题型的差异化答案（含 options） |
| `question_sum` | `exam` | 考试题目汇总快照 |
| `detail` | `score` | 每题得分明细与统计 |

### 6.1 通用规范

1. **所有 JSON 字段在 DB 中均为 `TEXT` 类型**，Java 端使用 `String` 接收，由 Service 层用 Jackson 序列化/反序列化。
2. **每个 JSON 文档必须有 `version` 字段**（默认 `1`），便于未来结构升级兼容。
3. **JSON 字段不建立 DB 层索引**；按内容查询需在 Service 层用 JPQL `LIKE` 模拟，复杂检索使用冗余字段（如 `question.type` / `question.use` 已为查询优化的冗余字段）。
4. **Entity 中 JSON 字段不暴露为前端 VO**，需在 DTO 中转换为强类型结构（参考 [01-Global-Standards.md §4.3](file:///d:/GDUT-OOP_20260601/wiki/01-Global-Standards.md#4-jpa-规范)）。

### 6.2 性能兜底：冗余字段

对于"高频按 JSON 内容查询"的场景，需在 DB 中额外维护冗余字段以支持索引。本版本已落实：

- `question.type` ← 冗余自 `question.answer` 内的题型（虽 v2.0.0 已将 `type` 提升为顶层字段，但 JSON 内仍可独立出现）
- `question.use` / `question.correct` ← 题内统计冗余字段，避免聚合查询
- `exam.question_sum.items[].type` ← 冗余题型，避免前端判型时回查 question 表

---

## 7. 业务矩阵设计

> 本章基于 4 张核心表，给出 5 大业务模块的端到端设计，包括数据来源、核心动作、API 路径草案、DTO 草案与状态/规则约束。

### 7.1 用户管理（M01）

**数据来源**：`user` 表（§4.1）

| 业务动作 | 描述 | SQL/JPA 草案 | API 路径草案 |
|---|---|---|---|
| 用户注册 | 学生自助注册 | `INSERT INTO user (name, password, type, status) VALUES (?, ?, ?, 1)` | `POST /api/v1/auth/register` |
| 用户登录 | 按 name 查 password 哈希比对 | `SELECT id, password, type, status FROM user WHERE name = ?` | `POST /api/v1/auth/login` |
| 用户查询（按 id） | 查单个用户详情 | `SELECT * FROM user WHERE id = ?` | `GET /api/v1/auth/me` |
| 用户查询（列表/分页） | 按 type / status 筛选 | `SELECT * FROM user WHERE type = ? AND status = ? LIMIT ? OFFSET ?` | `GET /api/v1/users?type=student&status=1` |
| 用户创建 | 管理员创建任意角色用户 | `INSERT INTO user (name, password, type, status) VALUES (?, ?, ?, 1)` | `POST /api/v1/users` |
| 用户更新 | 修改 name / type / password | `UPDATE user SET name = ?, type = ?, password = ? WHERE id = ?` | `PUT /api/v1/users/{id}` |
| 用户禁用/启用 | 状态启停 | `UPDATE user SET status = ? WHERE id = ?` | `PATCH /api/v1/users/{id}/status` |
| 用户删除 | 硬删除（有考试记录则禁用） | `DELETE FROM user WHERE id = ?` | `DELETE /api/v1/users/{id}` |
| 批量删除 | 批量删除/禁用用户 | 循环调用单条删除逻辑 | `DELETE /api/v1/users/batch` |

**DTO 草案**（Java 21 Record）：

```java
// 入参
public record UserCreateReq(@NotBlank String name, @NotBlank @Size(min=6, max=32) String password, @NotNull UserType type) {}

// 出参 VO（不含 password 哈希！）
public record UserVO(Integer id, String name, UserType type, Integer status) {}
```

**角色权限边界**：
- `admin` 拥有所有用户管理权限。
- `teacher` 仅可查询/更新自身账号。
- `student` 仅可查询自身账号 + 修改自身密码。

### 7.2 题库管理（M02）

**数据来源**：`question` 表（§4.2）

| 业务动作 | 描述 | API 路径草案 |
|---|---|---|
| 题目创建 | 录入单题，含 answer JSON 自适应 | `POST /api/v1/questions` |
| 题目批量导入 | JSON 数组批量上传 | `POST /api/v1/questions/batch` |
| 题目查询（按 id） | 查单题详情（含 answer） | `GET /api/v1/questions/{id}` |
| 题目查询（分页/筛选） | 按 type / 关键字（context LIKE）/ use 排序 | `GET /api/v1/questions?type=SingleChoice&keyword=...&page=0&size=20` |
| 题目更新 | 修改题干/答案 | `PUT /api/v1/questions/{id}` |
| 题目删除 | 硬删除（**警告**：已被组卷的题目删除会破坏 `exam.question_sum` 快照引用） | `DELETE /api/v1/questions/{id}` |
| 批量删除 | 批量删除题目 | `DELETE /api/v1/questions/batch` |
| 随机获取题目 | 自动组卷用，支持 type 过滤和排除 | `GET /api/v1/questions/random?type=...&excludedIds=...` |
| 题内统计自维护 | 组卷时 `use += 1`；判分正确时 `correct += 1` | 内部接口（无 HTTP） |

**`img=1` 的图片路径匹配规则**：

```
文件路径模板：.\Data\img\{questionId}.{ext}
支持扩展名：.png / .jpg / .jpeg / .gif（具体支持范围由实现决定，详见 §11 O5）
匹配策略：按 id 数字查找，找到第一个存在的扩展名即用
```

**DTO 草案**：

```java
public record QuestionCreateReq(
    @NotNull QuestionType type,
    @NotBlank String context,
    Boolean img,            // 可选，默认 false
    @NotNull JsonNode answer // 多态 JSON
) {}
```

### 7.3 组卷功能（M03）

**数据来源**：`question` 表（抽题源） + `exam` 表（落库目标）

#### 7.3.1 手动组卷

**流程**：

1. 教师从 question 表逐题挑选（前端多选 / 拖拽排序）。
2. 前端提交 `{ examName, starttime, endtime, items: [{questionId, score}] }`。
3. Service 层：
   - 校验所有 questionId 存在。
   - 构造 `question_sum` JSON（见 §4.3.1）。
   - 落 `exam` 表，**`status` 强制为 `'draft'`**。

**API 路径草案**：`POST /api/v1/exams/manual`

#### 7.3.2 自动组卷

**流程**：

1. 教师提交 `{ examName, starttime, endtime, rule: { totalQuestions, totalScore, typeFilter?, usePenalty? } }`。
2. Service 层：
   - 若有 `typeFilter`（如 `["SingleChoice", "Judge"]`），先用 `WHERE type IN (...)` 过滤候选集。
   - 若有 `usePenalty`，对 `use` 较高的题目加权降权（避免抽中热点题）。
   - 使用 `ORDER BY RANDOM() LIMIT totalQuestions` 或加权随机抽签。
   - 校验总分匹配（题目数 × 平均分 ≈ totalScore，必要时按题型分值模板补足）。
   - 构造 `question_sum` JSON。
   - 落 `exam` 表，`status = 'draft'`。

**API 路径草案**：`POST /api/v1/exams/auto`

#### 7.3.3 落库规范（手动 / 自动统一）

- `exam.status` **必须初始化为 `'draft'`**。
- `question_sum.items` 顺序即为试卷题序。
- 落库时**同时**为每个被抽中题目执行 `use += 1`（在同一个 Service 事务内）。
- `question_sum` 是**快照**：组卷后即使 `question` 表内容修改 / 删除，已组卷考试不受影响。

### 7.4 考试管理（M03）

**数据来源**：`exam` 表（§4.3）

| 业务动作 | 描述 | API 路径草案 |
|---|---|---|
| 创建考试 | 同 7.3 手动/自动组卷 | （见 7.3） |
| 发布考试 | `draft` → `publish` | `POST /api/v1/exams/{id}/publish` |
| 撤回考试 | `publish` → `draft` | `POST /api/v1/exams/{id}/withdraw` |
| 删除考试 | 仅 `draft` 状态可删 | `DELETE /api/v1/exams/{id}` |
| 考试列表（教师/管理员） | 按 status / 时间筛选全部 | `GET /api/v1/exams?status=...&from=...&to=...` |
| 考试列表（学生） | 仅返回 `publish` / `running` 状态 | `GET /api/v1/exams/available` |
| 考试详情 | 读取 `exam` + `question_sum`（不读 question 原表） | `GET /api/v1/exams/{id}` |
| 状态同步 | 定时任务按时间窗批量更新 `status` 字段 | （内部任务） |

**状态判定规则**（查询时实时计算，落库由定时任务兜底）：

```java
public ExamStatus resolveCurrentStatus(Exam exam, LocalDateTime now) {
    if (exam.getStatus() == ExamStatus.draft || exam.getStatus() == ExamStatus.done) {
        return exam.getStatus();  // 终态不变
    }
    // v3.0.0：starttime/endtime 为 String（ISO 8601），需解析为 LocalDateTime 后比较
    LocalDateTime start = LocalDateTime.parse(exam.getStarttime());
    LocalDateTime end = LocalDateTime.parse(exam.getEndtime());
    if (now.isBefore(start)) return ExamStatus.publish;
    if (now.isAfter(end) || now.equals(end)) return ExamStatus.done;
    return ExamStatus.running;
}
```

### 7.5 成绩管理（M04）

**数据来源**：`score` 表（§4.4）+ `question` 表（题内统计自维护）

#### 7.5.1 答题提交与判分流程

```
[学生] 提交答卷 POST /api/scores/submit
   ↓
[Controller] 入参校验 {examId, items: [{questionId, userAnswer}], ...}
   ↓
[Service] 1. 校验考试状态为 running
        2. 读取 exam.question_sum（确定题序与分值）
        3. 对每题执行判分逻辑：
           - SingleChoice / MultipleChoice / Judge / Fill：系统自动比对 question.answer
           - Essay：暂存 userAnswer，isCorrect=null，score=0（待教师评卷）
        4. 计算总分 score.all = Σ items[].score
        5. 构造 score.detail JSON（含 summary）
        6. 事务内 UPSERT score 表（一人一考唯一）
        7. 事务内：for each isCorrect=true → question.correct += 1
        8. 事务提交
   ↓
[Response] { scoreId, all, detail }
```

#### 7.5.2 教师评卷（仅 Essay）

- API：`POST /api/scores/{scoreId}/grade`，body 为 `{ items: [{questionId, score, comment}] }`。
- 评卷完成后：刷新 `score.all`、更新 `score.detail.items[].score/isCorrect`、若 `isCorrect` 从 `null` 变为 `true` 则 `question.correct += 1`。

#### 7.5.3 统计报表

| 报表 | 数据源 | 计算方式 |
|---|---|---|
| 考试通过率 | `score` 表 | `COUNT(WHERE all >= passScore) / COUNT(*)` per `exam` |
| 成绩分布 | `score` 表 | 按分数段分桶聚合 |
| 个人错题集 | `score.detail.items[]` | 筛选 `isCorrect = false` 的题 |
| 题目质量分析 | `question.use` / `question.correct` | `accuracy = correct / use`，识别过难 / 过易题 |
| 教师工作量 | 不直接统计（v2.0.0 移除 creator_id） | 远期通过 `operation_log` 补充 |

---

## 8. 数据流转图与说明

### 8.1 端到端业务流程图

```
┌────────────┐    录入     ┌────────────┐
│  老师/管理  │ ─────────▶ │  question  │
│  员        │            │   表        │
└────────────┘            └─────┬──────┘
                                │
                                │ 抽题（手动/自动）
                                ▼
┌────────────┐    组卷     ┌────────────┐
│  老师/管理  │ ─────────▶ │    exam    │  status=draft
│  员        │            │   表        │  question_sum=JSON 快照
└────────────┘            └─────┬──────┘
                                │
                                │ 发布
                                ▼
                          ┌────────────┐
                          │   exam     │  status=publish
                          │   表       │
                          └─────┬──────┘
                                │ 系统按时间窗自动
                                ▼
                          ┌────────────┐
                          │   exam     │  status=running
                          │   表       │
                          └─────┬──────┘
                                │ 学生作答
                                ▼
┌────────────┐    提交答卷   ┌────────────┐
│  学生      │ ─────────▶  │  Service   │
└────────────┘             │ (判分)     │
                           └─────┬──────┘
                                 │
              ┌──────────────────┼──────────────────┐
              ▼                  ▼                  ▼
        ┌──────────┐       ┌──────────┐       ┌──────────┐
        │  score   │       │ question │       │  score   │
        │  表      │       │ .correct │       │  .detail │
        │  .all    │       │  += 1    │       │  (JSON)  │
        └────┬─────┘       └────┬─────┘       └────┬─────┘
             │                  │                  │
             │                  │ 统计报表          │
             │                  ▼                  ▼
             │            ┌──────────────────────────┐
             │            │  报表/个人错题集/质量分析  │
             │            └──────────────────────────┘
             ▼
        ┌──────────────────┐
        │ 系统自动：endtime │
        │  exam.status=done │
        └──────────────────┘
```

### 8.2 表间物理外键方向

```
user (id) ◀──── score.user       [1:N，用户 → 分数]
exam (id) ◀──── score.exam       [1:N，考试 → 分数]
question (id) ◀─ exam.question_sum.items[].questionId  [逻辑引用，JSON 快照内]
user ←──── score ←──── exam     [user 与 exam 无直接外键]
```

- 物理外键：**仅 `score.user` 与 `score.exam` 两处**。
- 逻辑外键：`exam.question_sum.items[].questionId` 通过 JSON 文本引用 `question.id`，**不建立 DB 层 FK**。
- 删除策略：删除 `user` 或 `exam` 前必须先清理 `score` 记录；删除 `question` **不受限制**（快照机制保证历史考试不受影响，但 `question.use/correct` 历史统计会永久停留在被删除题目上，建议在删除前由管理员确认）。

### 8.3 JSON 字段"快照"与"明细"边界

| JSON 字段 | 性质 | 生成时机 | 后续修改影响 |
|---|---|---|---|
| `exam.question_sum` | **快照** | 组卷时一次性写入 | 题目后续修改 / 删除**不影响**已组卷考试 |
| `score.detail` | **明细** | 答题提交时写入 | 评卷时增量更新（仅 Essay 改 score/isCorrect） |
| `question.answer` | **静态元数据** | 题目创建时写入 | 题目更新时被覆盖（**不推荐**：会破坏快照考试的标准答案判定） |

> **设计红线**：题目更新时**不得修改** `question.answer` JSON 内与 `exam.question_sum` 中已存在题目的判分关键字段（如 `correctOption` / `blanks`）。如必须调整，建议创建新题目并更新 exam 快照（远期需引入"题目版本"机制，本版本不实现）。

---

## 9. 字段命名映射约定

v2.0.0 起，**4 张表的字段名在 DB 与 Java 端完全一致**，无 snake_case ↔ camelCase 转换需求。

| DB 字段 | Java 字段 | 说明 |
|---|---|---|
| `user.id` | `user.id` | 主键 |
| `user.type` | `user.type` | 无需转换（type 已是 camelCase） |
| `user.status` | `user.status` | 无需转换 |
| `question.context` | `question.context` | 无需转换（v1.x 的 `content` 已重命名为 `context`） |
| `question.img` | `question.img` | 无需转换 |
| `question.use` | `question.use` | 无需转换（与 SQL 关键字 `USE` 区分大小写） |
| `question.correct` | `question.correct` | 无需转换 |
| `exam.exam` | `exam.exam` | **特殊**：DB 与 Java 同名（v1.x 的 `name` 已重命名为 `exam` 以避免与关键字冲突） |
| `exam.starttime` | `exam.starttime` | 无下划线 |
| `exam.endtime` | `exam.endtime` | 无下划线 |
| `exam.question_sum` | `exam.questionSum` | **唯一例外**：DB 用 snake_case 表达复合词，Java 端按 camelCase 转换。**但 JPA `@Column(name = "question_sum")` 必须显式标注**。 |
| `score.user` | `score.user` | 物理外键，字段名与表名冲突，**需在 JPA 中通过 `@Column(name = "user")` 显式标注**（v3.0.0：由 `@JoinColumn` 修正为 `@Column`，保持 4 表独立设计）。 |
| `score.exam` | `score.exam` | 同上，`@Column(name = "exam")` 显式标注。 |
| `score.all` | `score.all` | **特殊**：与 SQL 关键字 `ALL` 区分大小写，SQLite 默认不敏感，需在 JPQL 中使用反引号或重命名（远期如冲突可改为 `total`）。本版本按用户规范保留 `all`。 |
| `score.detail` | `score.detail` | 无需转换 |

> **历史变更说明**：v1.x 的 `user.username` / `user.user_type` / `question.question_type` / `question.difficulty` / `exam.start_time` / `exam.end_time` / `exam.questions` / `score.user_id` / `score.exam_id` / `score.total_score` 在 v2.0.0 全部统一为单字 camelCase 或同名。

---

## 10. JPA 实体映射约束

| 编号 | 约束 | 原因 |
|---|---|---|
| M1 | Entity 类使用 Lombok `@Getter @Setter @NoArgsConstructor` | 简化样板 |
| M2 | 字段类型严格对应 §4 表，**禁止** Java 字段类型与 DB 不一致（如 DB INTEGER 不可映射为 `String`） | 防隐式转换 Bug |
| M3 | 本版本无 `@ManyToOne` / `@OneToMany` 关联（4 张表均独立），故无 N+1 风险 | — |
| M4 | 物理外键字段使用 `@Column(name = "user")` / `@Column(name = "exam")` 显式标注（v3.0.0 修正：**不使用** `@JoinColumn`，因字段类型为 `Integer` 而非实体对象，与 4 表独立设计一致。物理外键语义由 DDL `FOREIGN KEY` 约束承载） | 与数据字典表名一致 |
| M5 | JSON 字段使用 `@Column(name = "...", columnDefinition = "TEXT")` 标注 | 显式声明 |
| ~~M6~~ | **已废弃**：v2.0.0 不再强制 `@Version` 乐观锁（详见 §1.3） | 主动放弃硬约束 |
| M7 | Repository 命名遵循 Spring Data 规范（`findByXxx`、`existsByXxx`），复杂查询使用 `@Query` JPQL | 可读性 |
| M8 | 枚举字段必须 `@Enumerated(EnumType.STRING)` + `@Column(name = "...", length = 20)` | 字符串存储与 `name()` 一致 |
| M9 | SQLite 布尔字段使用 `@Column(nullable = false, columnDefinition = "INTEGER")` + Java `Integer`（v3.0.0 修正：`ddl-auto=validate` 模式下 `Boolean` 映射为 `TINYINT` 与 DDL `INTEGER` 不一致，改用 `Integer` 存储 0/1） | 类型安全与 DDL 一致 |

### 10.1 实体示例

```java
@Entity
@Table(name = "user")  // 参考 §4.1
@Getter @Setter
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "INTEGER")
    private Integer id;

    @Column(nullable = false, unique = true, length = 50)
    private String name;

    @Column(nullable = false, length = 100)
    private String password;  // BCrypt 哈希

    @Enumerated(EnumType.STRING)  // M8
    @Column(name = "type", nullable = false, length = 20)
    private UserType type;

    @Column(name = "status", nullable = false, columnDefinition = "INTEGER")
    private Integer status;  // M9: 0/1 整数，1=启用，0=禁用
}
```

```java
@Entity
@Table(name = "question")  // 参考 §4.2
@Getter @Setter
@NoArgsConstructor
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "INTEGER")
    private Integer id;

    @Enumerated(EnumType.STRING)  // M8
    @Column(name = "type", nullable = false, length = 30)
    private QuestionType type;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String context;

    @Column(name = "img", nullable = false, columnDefinition = "INTEGER")
    private Integer img;  // M9: 0/1 整数，1=带图，0=无图

    @Column(nullable = false, columnDefinition = "TEXT")
    private String answer;  // JSON

    @Column(nullable = false)
    private Integer use;

    @Column(nullable = false)
    private Integer correct;
}
```

```java
@Entity
@Table(name = "score")  // 参考 §4.4
@Getter @Setter
@NoArgsConstructor
public class Score {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "INTEGER")
    private Integer id;

    @Column(name = "user", nullable = false, columnDefinition = "INTEGER")  // M4: @Column 非 @JoinColumn
    private Integer user;

    @Column(name = "exam", nullable = false, columnDefinition = "INTEGER")  // M4
    private Integer exam;

    @Column(name = "all", nullable = false)  // 显式声明 SQL 关键字列
    private Integer all;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String detail;  // JSON
}
```

---

## 10.2 开发过程中遇到的问题与解决方案（v4.0.0 新增）

> 本节记录 JPA/SQLite 适配过程中的典型问题与处置方案。

### 问题 1：Boolean vs Integer 类型不匹配

**现象**：在 `ddl-auto=validate` 模式下，Entity 中 `Boolean` 类型的字段（如 `user.status`、`question.img`）被 Hibernate 映射为 `TINYINT`，与 DDL 中 `INTEGER` 类型不一致，导致启动校验失败。

**原因**：Hibernate 社区 SQLite 方言将 Java `Boolean` 映射为 SQL `TINYINT`，而 SQLite 实际以 `INTEGER` 存储布尔值（0/1）。

**解决方案**：将 Entity 中布尔字段类型由 `Boolean` 改为 `Integer`（0/1），并显式声明 `columnDefinition = "INTEGER"`。

**影响范围**：`User.status`、`Question.img`

### 问题 2：LocalDateTime vs String 时间字段

**现象**：Entity 中 `LocalDateTime` 类型的字段（如 `exam.starttime`、`exam.endtime`）在 `ddl-auto=validate` 模式下类型推断失败。

**原因**：Hibernate 社区 SQLite 方言对 `LocalDateTime` → TEXT 的自动转换在 validate 模式下存在类型推断差异（方言将 Java 类型映射为非 TEXT 类型，而 SQLite 实际存储为 TEXT）。

**解决方案**：将时间字段类型由 `LocalDateTime` 改为 `String`，存储 ISO 8601 格式字符串（如 `"2026-06-15T09:00:00"`）。

**影响范围**：`Exam.starttime`、`Exam.endtime`

### 问题 3：Score 外键映射方式

**现象**：4 张核心表为独立设计（无 JPA `@ManyToOne`/`@OneToMany` 关联），但初始文档建议 Score 实体使用 `@JoinColumn`。

**原因**：4 表独立设计下，物理外键字段类型为 `Integer`（而非实体对象），使用 `@JoinColumn` 会导致类型不匹配。

**解决方案**：Score 实体中外键字段（`user`、`exam`）使用 `@Column(name = "user")` / `@Column(name = "exam")` 显式标注。物理外键语义由 DDL `FOREIGN KEY` 约束承载。

**影响范围**：`Score.user`、`Score.exam`

### 问题 4：主键类型选择

**现象**：初始文档使用 `Long` 作为主键类型。

**原因**：SQLite 自增 INTEGER 范围（最大 2^63-1）对于课程作业规模完全足够，且 DDL 中显式声明 `columnDefinition = "INTEGER"`。

**解决方案**：将所有主键/外键 Java 类型由 `Long` 改为 `Integer`。

**影响范围**：所有 Entity 的 `id` 字段、`Score` 的外键字段

### 问题 5：SQLite JDBC 自动配置失败

**现象**：测试环境启动时，`spring-boot-starter-data-jdbc-test` 依赖触发 `DataJdbcRepositoriesAutoConfiguration`，而 SQLite 不支持 JDBC 方言，导致测试启动失败。

**原因**：Spring Boot 自动检测到 `spring-data-jdbc` 依赖存在时，会尝试配置 JDBC Repository 支持。

**解决方案**：在 `application-test.yaml` 中通过 `spring.autoconfigure.exclude` 排除 `DataJdbcRepositoriesAutoConfiguration`。

**影响范围**：仅测试环境（`application-test.yaml`）

---

## 11. 待确认 / TODO 项

> 下列内容在用户规范中**未明确**但实施时不可避免。建议在评审后定稿。

| 编号 | 内容 | 草案建议 | 影响范围 |
|---|---|---|---|
| O1 | `score.user` 与 `score.exam` 物理外键约束的具体行为 | 建议开启 SQLite 外键（`PRAGMA foreign_keys=ON`）后启用物理 FK，删除 `user`/`exam` 时先清理 `score` | M04 |
| O2 | JSON Schema 是否需要 `version` 字段 | 建议**全部**保留 `{"version":1, ...}` 便于未来升级兼容 | M02 / M03 / M04 |
| O3 | 自动组卷"条件抽签"支持哪些过滤维度 | 草案支持 `typeFilter`（题型数组）+ `usePenalty`（use 频次降权）。**不**支持 `difficulty`（v2.0.0 已删除该字段） | M03 |
| O4 | `use`/`correct` 自更新的写入位置 | Service 层事务内同步写入 `question` 表（与 score 写入同一事务） | M04 |
| O5 | `img=1` 时图片扩展名匹配策略 | Wiki 仅说明"相同数字编号的图片（不包括扩展名）"，建议在 Wiki 补充 `.png` / `.jpg` / `.jpeg` / `.gif` 支持范围；具体匹配顺序由实现决定 | M02 |
| O6 | `user.name` 是否加 UNIQUE 约束 | 建议加 UNIQUE 避免重名（计划中已加索引建议，需评审确认） | M01 |
| O7 | exam 状态机是否需要 `archived` 归档态 | v2.0.0 按用户规范保留 4 态，**不引入** archived；远期若需历史归档，建议通过 `operation_log` 或独立 `archived_exam` 视图实现 | M03 |
| O8 | `score.all` 与 SQL 关键字 `ALL` 的兼容性 | SQLite 默认大小写不敏感，可能在某些 SQL 工具中产生歧义；建议远期改名为 `total`（本版本按用户规范保留） | M04 |

---

## 12. 版本与变更记录

| 日期 | 版本 | 变更说明 |
|---|---|---|
| 2026-06-03 | v1.0.0 | 由 `temp.txt §4` 整合为正式数据字典；明确 JSON 字段结构与版本号；增加审计字段与乐观锁规范 |
| 2026-06-04 | v1.1.0 | 依据用户决策彻底删除 `question_property` 表及"题目性质"业务概念；同步删除 `question.property_ids` 字段；表数从 6 张减至 5 张（T3 变更为 exam）；自动组卷简化为完全随机抽题（移除按性质/题型/难度筛选规则）；M02 模块名由"题库与题目性质"简化为"题库管理"；AutoRule DTO 简化为 totalQuestions + totalScore |
| 2026-06-04 | **v2.0.0** | **重大重写**：① 表数从 5 张减为 **4 张**（**删除 `answer_statistics` 表**，统计功能由 `question.use` / `question.correct` 字段承载）；② 字段严格按用户最新规范裁剪：移除审计字段 `created_at/updated_at`、乐观锁 `version`、扩展字段 `admin_subtype/email/display_name/difficulty/options/analysis/creator_id/description/duration_minutes/assembly_mode/total_score/submit_time/duration_seconds/score.status`；③ 枚举命名 **全局改为小写/CamelCase**（`student` / `SingleChoice` / `draft`），同步删除 `true_false` → `Judge`；④ 文档结构升级为 **Java ↔ SQLite 双向映射表** 格式；⑤ 新增 **§5 枚举定义**、**§7 业务矩阵设计**、**§8 数据流转图**、**§9 字段命名映射约定** 四大章节；⑥ 同步在 [01-Global-Standards.md §4.1.1](file:///d:/GDUT-OOP_20260601/wiki/01-Global-Standards.md) 声明放弃 J6 / §5 硬约束的例外 |
| 2026-06-06 | **v4.0.0** | **Wiki 系统性校准**：① 新增 §10.2 开发问题与解决方案（记录 5 个典型问题与处置方案）；② 校准 Data/img/ 目录状态 |

---

## 13. 实现状态追踪（v3.0.0 新增）

> 本节追踪各层数据相关代码的实际实现状态，与 Wiki 文档定义对照。更新日期：2026-06-06。

### 13.1 后端数据层实现状态

| 层级 | 模块 | 实现状态 | 说明 |
|---|---|---|---|
| Entity | M01 User | ✅ 已实现 | `User.java`，字段与 §4.1 完全一致 |
| Entity | M02 Question | ✅ 已实现 | `Question.java`，字段与 §4.2 完全一致 |
| Entity | M03 Exam | ✅ 已实现 | `Exam.java`，字段与 §4.3 完全一致 |
| Entity | M04 Score | ✅ 已实现 | `Score.java`，字段与 §4.4 完全一致 |
| Enum | M01 UserType | ✅ 已实现 | `UserType.java`，与 §5.1 完全一致 |
| Enum | M02 QuestionType | ✅ 已实现 | `QuestionType.java`，与 §5.2 完全一致 |
| Enum | M03 ExamStatus | ✅ 已实现 | `ExamStatus.java`，与 §5.3 完全一致 |
| Repository | M01 UserRepository | ✅ 已实现 | `findByName` / `existsByName` / `findByTypeAndStatus` / `findByType` |
| Repository | M02 QuestionRepository | ✅ 已实现 | `findByType` / `searchByKeyword`（分页）/ `incrementUse` / `incrementCorrect` |
| Repository | M03 ExamRepository | ✅ 已实现 | `findByStatus` / `findByTimeWindow` / `findByStatusNot` |
| Repository | M04 ScoreRepository | ✅ 已实现 | `findByUserAndExam` / `findByUser` / `findByExam` / `upsertScore`（原生 UPSERT） |
| DDL | 4 张表 | ✅ 已实现 | `scripts/table_*.sql` + `backend/src/test/resources/schema/*.sql` |
| 测试 | 4 个 Repository | ✅ 已实现 | 共 73 个测试方法（含 Service 层），全部通过 |

### 13.2 后端业务层实现状态

| 层级 | 实现状态 | 说明 |
|---|---|---|
| Controller | ✅ 已实现 | 4 个 Controller（User/Question/Exam/Score）+ 1 DraftController，共 40 个端点 |
| Service | ✅ 已实现 | 4 个 Service + 1 DraftCacheService，完整业务逻辑 + 事务控制 |
| DTO | ✅ 已实现 | 30+ Record/VO 类，覆盖所有请求/响应场景 |
| `Result<T>` 统一返回 | ✅ 已实现 | `common/api/Result.java` |
| `PageResult<T>` 分页包装 | ✅ 已实现 | `common/api/PageResult.java` |
| 全局异常处理器 | ✅ 已实现 | `common/exception/GlobalExceptionHandler.java` |
| JWT 认证 | ✅ 已实现 | JwtUtil + JwtAuthenticationInterceptor + @RequireRole 注解 |
| WebMvcConfig | ✅ 已实现 | 拦截器注册配置 |

### 13.3 前端实现状态

| 层级 | 实现状态 | 说明 |
|---|---|---|
| Vue 3 项目 | ❌ 未创建 | `frontend/` 目录不存在 |
| 路由/状态管理 | ❌ 未创建 | — |
| 页面组件 | ❌ 未创建 | — |

### 13.4 基础设施实现状态

| 项目 | 实现状态 | 说明 |
|---|---|---|
| `application.yaml` | ✅ 已实现 | 生产配置，含 PRAGMA / HikariCP / JPA validate |
| `application-test.yaml` | ✅ 已实现 | 测试 profile，排除 DataJdbc 自动配置 |
| `Data/English.sqlite` | ✅ 已存在 | 生产数据库文件 |
| `Data/img/` | ✅ 已存在 | 含示例图片（4.png） |
| 根级 `.gitignore` | ❌ 未创建 | 仅有 `backend/.gitignore` |
| Docker 配置 | ❌ 未创建 | — |
