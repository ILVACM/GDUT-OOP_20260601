# M03 - 考试与组卷模块

> 包含考试生命周期管理（草稿 / 已发布 / 进行中 / 已结束）、手动组卷、自动组卷（随机抽题 + 条件抽签）、考试题目快照化等业务场景描述、API 接口定义及关键 DTO 设计。
>
> **数据来源**：[02-Data-Dictionary.md §4.3 考试表](file:///d:/GDUT-OOP_20260601/wiki/02-Data-Dictionary.md#43-考试表-exam)
>
> **API 契约遵循**：[01-Global-Standards.md §2](file:///d:/GDUT-OOP_20260601/wiki/01-Global-Standards.md#2-统一-api-响应契约)

---

## 1. 业务场景

| 场景 | 描述 | 角色 |
|---|---|---|
| 创建考试（手动组卷） | 设置考试名、时间窗，教师从题库逐题挑选 | 老师、管理员 |
| 创建考试（自动组卷） | 设置考试名、时间窗、抽题规则（`totalQuestions` / `totalScore` / `typeFilter` / `usePenalty`），系统随机抽题 | 老师、管理员 |
| 编辑考试 | **仅 `draft` 状态**可改 | 老师（自己）、管理员 |
| 发布考试 | `draft` → `publish`；按时间窗自动进入 `running` | 老师、管理员 |
| 撤回考试 | `publish` → `draft`（仅未到 `starttime`） | 老师、管理员 |
| 删除考试 | **仅 `draft` 状态**可删 | 老师、管理员 |
| 考试列表（教师/管理员） | 按 `status` / 时间筛选全部 | 老师、管理员 |
| 学生考试列表 | 仅显示 `publish` / `running` 状态 | 学生 |

> **v2.0.0 简化**：v1.x 中的"归档"（`ARCHIVED`）状态在 v2.0.0 **删除**；考试终态为 `done`。`duration_minutes`（时长）、`description`（描述）、`assembly_mode`（组卷模式枚举）、`total_score`（冗余总分）、`creator_id`（创建者）字段**全部移除**。

---

## 2. 考试状态机（4 态）

```
       发布             自动：starttime ≤ now < endtime
draft ──────→ publish ─────────────────────────────→ running
   │              │                                     │
   │ 删除（仅 draft） 撤回（仅 publish）                          │ 自动：now ≥ endtime
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

> **v2.0.0 简化**：v1.x 的 5 态（`DRAFT` / `PUBLISHED` / `ONGOING` / `FINISHED` / `ARCHIVED`）合并为 4 态，**删除 `ARCHIVED`**。枚举名全部小写化：`DRAFT` → `draft`、`PUBLISHED` → `publish`、`ONGOING` → `running`、`FINISHED` → `done`。
>
> **状态流转由系统自动驱动**：`starttime ≤ now < endtime` 视为 `running`；`now ≥ endtime` 视为 `done`。查询时按时间窗实时计算，定时任务可周期性同步落库。
>
> **完整状态机定义**：参见 [02-Data-Dictionary.md §4.3.2](file:///d:/GDUT-OOP_20260601/wiki/02-Data-Dictionary.md#432-状态机4-态) 与 [§5.3 ExamStatus](file:///d:/GDUT-OOP_20260601/wiki/02-Data-Dictionary.md#53-examstatus考试状态)。

---

## 3. 组卷模式

### 3.1 手动组卷

教师从 [M02 题库](file:///d:/GDUT-OOP_20260601/wiki/modules/M02-Question-Bank.md) 中挑选题目，组装为考试。

**输入**（创建考试 + 组卷请求）：

```json
{
  "exam": "2026 春季英语期中考试",
  "starttime": "2026-06-15T09:00:00",
  "endtime": "2026-06-15T11:00:00",
  "items": [
    { "questionId": 101, "score": 5 },
    { "questionId": 205, "score": 10 }
  ]
}
```

**后端处理**：

1. 校验所有 `questionId` 存在
2. 校验总分 = `SUM(score)`
3. 构造 `question_sum` JSON（快照），写入 `exam` 表
4. **事务内** 对每道被抽中题目执行 `use += 1`
5. INSERT INTO exam (`status='draft'`)

> **v2.0.0 简化**：v1.x 手动组卷的 `orderIndex` 字段**移除**（题目顺序由 `items` 数组顺序决定，JSON 数组天然有序）。

### 3.2 自动组卷（随机抽签 + 条件抽签）

教师仅指定题目数量与规则，系统从题库中**带条件随机**抽取指定数量的题目。

**输入**（自动组卷请求）：

```json
{
  "exam": "随机组卷 - 期末复习",
  "starttime": "2026-06-20T10:00:00",
  "endtime": "2026-06-20T11:00:00",
  "autoRule": {
    "totalQuestions": 20,
    "totalScore": 100,
    "typeFilter": ["SingleChoice", "Judge"],
    "usePenalty": true
  }
}
```

**`autoRule` 字段说明**：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `totalQuestions` | int | ✓ | 抽取的题目数量 |
| `totalScore` | int | ✓ | 试卷总分 |
| `typeFilter` | List<QuestionType> | ✗ | 题型过滤（v1.x 不支持，v2.0.0 新增） |
| `usePenalty` | boolean | ✗ | 是否对 `use` 频次高的题目降权（v1.x 不支持，v2.0.0 新增） |

**后端处理**（核心算法）：

```java
public List<ExamQuestionItem> autoAssemble(AutoRule rule) {
    // 1. 候选集：按 typeFilter 过滤（若有）
    List<Question> candidates;
    if (rule.getTypeFilter() != null && !rule.getTypeFilter().isEmpty()) {
        candidates = questionRepository.findByTypeIn(rule.getTypeFilter());
    } else {
        candidates = questionRepository.findAll();
    }

    // 2. 校验候选数是否足够
    if (candidates.size() < rule.getTotalQuestions()) {
        throw new BusinessException(4302,
            "题库中可用题目不足：需要 " + rule.getTotalQuestions() + " 道，实际 " + candidates.size() + " 道");
    }

    // 3. 加权随机（use 降权可选）
    List<Question> picked;
    if (Boolean.TRUE.equals(rule.getUsePenalty())) {
        // 权重 = 1 / (1 + use)，热点题抽中概率降低
        picked = weightedRandomPick(candidates, rule.getTotalQuestions());
    } else {
        // 完全随机
        Collections.shuffle(candidates);
        picked = candidates.subList(0, rule.getTotalQuestions());
    }

    // 4. 等分计算每题分值
    int scoreEach = rule.getTotalScore() / rule.getTotalQuestions();
    int remainder = rule.getTotalScore() % rule.getTotalQuestions();

    List<ExamQuestionItem> result = new ArrayList<>();
    for (int i = 0; i < picked.size(); i++) {
        int score = scoreEach + (i < remainder ? 1 : 0);
        result.add(new ExamQuestionItem(picked.get(i).getId(), score));
    }
    return result;
}
```

> **v2.0.0 相对 v1.x 的关键升级**：
> - **条件抽签**：支持 `typeFilter`（题型过滤）— 题目类型的"条件抽签"基础版
> - **频次降权**：支持 `usePenalty`（use 降权）— 避免抽中已被组卷多次的"热点题"
> - **不支持** `difficulty`（难度）过滤（v2.0.0 已删除 difficulty 字段）
> - **不支持** "question_property"（性质）过滤（v1.1.0 已删除 question_property 表）
>
> **设计取舍**：
> - 候选不足时**直接拒绝组卷**，而非"凑数"
> - 分值采用等分 + 余数分摊到前 N 道题，保证 `SUM(score) == totalScore` 严格成立
> - 抽题使用 `Collections.shuffle` 内存随机；题库规模大时可改用 SQL `ORDER BY RANDOM() LIMIT ?`

### 3.3 落库规范（手动 / 自动统一）

- `exam.status` **必须初始化为 `'draft'`**。
- `question_sum.items` 顺序即为试卷题序。
- 落库时**同时**为每个被抽中题目执行 `use += 1`（在同一个 Service 事务内）。
- `question_sum` 是**快照**：组卷后即使 `question` 表内容修改 / 删除，已组卷考试不受影响。

---

## 4. 考试题目快照（`question_sum`）

`exam.question_sum` 字段是 JSON 文本，存储**组卷时的题目快照**，详见 [02-Data-Dictionary.md §4.3.1](file:///d:/GDUT-OOP_20260601/wiki/02-Data-Dictionary.md#431-question_sum-json-字段结构)：

```json
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

> **v2.0.0 关键简化**：v1.x 的 `exam.questions` JSON 内嵌套 `snapshot: { content, options, answer }`（题目内容+选项+答案的完整快照）**移除**。v2.0.0 的 `question_sum` 仅存 `questionId` + `score` + `type` 三元组，**不**冗余存储题干/选项/答案内容。
>
> 设计权衡：
> - **优点**：JSON 体积小得多，写入 / 解析快，避免大字段对 SQLite 性能的影响
> - **代价**：判分时必须 JOIN `question` 表获取 `answer` JSON（详见 [M04 §3 判分流程](file:///d:/GDUT-OOP_20260601/wiki/modules/M04-Score-Statistics.md)）
> - **题序保留**：`items` 数组顺序即为试卷题序，无需 `orderIndex` 字段
>
> **答案下发安全**：学生获取考试题目时，服务端**按 `questionId` 读取原题**并**剔除 `answer` 字段**再返回给学生 VO（防 API 泄露答案）。

---

## 5. 关键 DTO

### 5.1 创建考试请求

```java
// 手动组卷
public record ExamCreateManualReq(
    @NotBlank String exam,                              // 考试名（v1.x: name → exam）
    @NotNull @Future String starttime,
    @NotNull @Future String endtime,
    @NotEmpty List<ExamQuestionItemReq> items           // v1.x: questionItems → items
) {}

public record ExamQuestionItemReq(
    @NotNull Integer questionId,
    @NotNull @Min(1) Integer score
    // v1.x: orderIndex 字段移除
) {}

// 自动组卷（v2.0.0 增强：支持 typeFilter + usePenalty）
public record ExamCreateAutoReq(
    @NotBlank String exam,
    @NotNull @Future String starttime,
    @NotNull @Future String endtime,
    @NotNull @Valid AutoRule autoRule
) {}

public record AutoRule(
    @NotNull @Min(1) Integer totalQuestions,
    @NotNull @Min(1) Integer totalScore,
    List<QuestionType> typeFilter,    // v1.x 不支持
    Boolean usePenalty                // v1.x 不支持
) {}
```

### 5.2 响应 VO

```java
// 考试 VO（教师视角）
public record ExamVO(
    Integer id,
    String exam,                       // 考试名
    ExamStatus status,
    String starttime,
    String endtime,
    List<ExamQuestionVO> questionItems // items + 题序（顺序即数组顺序）
) {}

// 学生视角 VO（不含答案）
public record ExamForStudentVO(
    Integer id,
    String exam,
    String starttime,
    String endtime,
    List<ExamQuestionForStudentVO> questionItems
) {}

public record ExamQuestionForStudentVO(
    Integer questionId,
    QuestionType type,                 // 冗余自 question_sum.items[].type
    String context,                    // 从 question 表读取
    Integer img,                       // 从 question 表读取
    Object answer,                     // 从 question.answer 反序列化（多态）
    List<String> options,              // 从 question.answer.options 提取（仅单选/多选）
    Integer score
    // 注：answer 字段为"考试视图"，由后端特殊构造
    //     仅在考试时间窗内返回给学生，且仅包含"用于前端交互的格式"
) {}
```

> **v2.0.0 简化**：v1.x VO 中的 `description` / `durationMinutes` / `assemblyMode` / `totalScore`（冗余字段） / `creatorId` / `creatorName` / `createdAt` 字段**全部移除**。`name` → `exam`，`questions` → `questionItems`。

---

## 6. API 接口定义

| 方法 | 路径 | 鉴权 | 描述 | 请求 | 响应 |
|---|---|---|---|---|---|
| POST | `/api/exams/manual` | 老师 / 管理员 | 创建考试（手动组卷） | `ExamCreateManualReq` | `ExamVO` |
| POST | `/api/exams/auto` | 老师 / 管理员 | 创建考试（自动组卷） | `ExamCreateAutoReq` | `ExamVO` |
| GET | `/api/exams/{id}` | 老师 / 管理员 | 考试详情 | — | `ExamVO` |
| GET | `/api/exams/{id}/preview` | 老师 / 管理员 | 学生视角预览（脱敏） | — | `ExamForStudentVO` |
| PUT | `/api/exams/{id}` | 老师（自己） / 管理员 | 修改（**仅 `draft` 状态**） | `ExamCreateManualReq` | `ExamVO` |
| POST | `/api/exams/{id}/publish` | 老师（自己） / 管理员 | 发布（`draft` → `publish`） | — | `Result<Void>` |
| POST | `/api/exams/{id}/withdraw` | 老师（自己） / 管理员 | 撤回（`publish` → `draft`） | — | `Result<Void>` |
| DELETE | `/api/exams/{id}` | 老师（自己） / 管理员 | 删除（**仅 `draft` 状态**） | — | `Result<Void>` |
| GET | `/api/exams` | 老师 / 管理员 | 分页查询（按 `status` / 时间） | `?status=&page=&size=` | `PageResult<ExamVO>` |
| GET | `/api/exams/available` | 学生 | 列出可参加的考试（`publish` / `running`） | — | `List<ExamForStudentVO>` |

> **v2.0.0 简化**：
> - 路径前缀 `/api/v1/` → `/api/`
> - **移除** `PATCH /api/v1/exams/{id}/archive`（无 `ARCHIVED` 状态）
> - `POST /api/v1/exams`（手动/自动合用）→ 拆分为 `/api/exams/manual` + `/api/exams/auto`，行为更明确

---

## 7. 业务规则

1. **草稿可改、发布后不可改**：**仅 `draft` 状态**可修改；`publish` / `running` / `done` 状态**均不可改**（含时间窗、组卷内容、名称等所有字段）。如需调整，须先撤回为 `draft`。
2. **撤回限制**：仅 `publish` 状态可撤回为 `draft`；`running` / `done` 后不可撤回。
3. **时间校验**：`endtime > starttime`；`starttime` 必须在未来（创建时校验）。
4. **总分校验**：手动组卷必须满足 `SUM(score) = totalScore`；自动组卷系统按等分 + 余数分摊保证严格成立。
5. **删除保护**：**仅 `draft` 状态**可物理删除；`publish` 之后**不可删除**（防止已发布的考试链接被打散）。
6. **并发保护**：v2.0.0 不使用乐观锁（[01-Global-Standards.md §4.1.1](file:///d:/GDUT-OOP_20260601/wiki/01-Global-Standards.md#4111-已知例外2026-06-04-起生效) 已知例外），组卷 / 发布的并发安全由 SQLite 串行写 + 业务层事务兜底。
7. **答案下发安全**：学生获取考试题目的接口**严禁返回 `answer` 字段**（含 `correctOption` / `blanks` / `correct` / `keywords` 等所有判分关键字段）。
8. **判分时 JOIN**：判分时从 `question` 表读 `answer` JSON，按 `question_sum.items[].questionId` 顺序遍历判分（详见 [M04 §3](file:///d:/GDUT-OOP_20260601/wiki/modules/M04-Score-Statistics.md)）。

---

## 8. 关键业务流

### 8.1 自动组卷流程

```
老师                                后端                                 DB
  |  POST /api/exams/auto           |                                   |
  |  { autoRule: {totalQuestions: 20, totalScore: 100, ... } }          |
  | ------------------------------> |                                   |
  |                                 |  校验时间、总分                    |
  |                                 |  SELECT * FROM question           |
  |                                 |  WHERE type IN (filter) [可选]    |
  |                                 |  -------------------------------> |
  |                                 |  <------------------------------- |
  |                                 |  校验候选数 >= totalQuestions      |
  |                                 |  [usePenalty] 加权 / 完全随机     |
  |                                 |  Collections.shuffle + 前 N 道    |
  |                                 |  等分 + 余数分摊计算每题分值     |
  |                                 |  构造 question_sum JSON            |
  |                                 |  BEGIN TX                          |
  |                                 |  INSERT INTO exam (status='draft')|
  |                                 |  -------------------------------> |
  |                                 |  for each picked:                 |
  |                                 |  UPDATE question                  |
  |                                 |  SET use = use + 1 WHERE id = ?  |
  |                                 |  -------------------------------> |
  |                                 |  COMMIT TX                        |
  |  200 { ExamVO }                 |                                   |
  | <------------------------------ |                                   |
```

> **事务边界**：`INSERT exam` 与 `UPDATE question.use` 必须在同一事务内，任一失败则全部回滚，防止"考试已组卷但 use 未递增"或反之的不一致。

### 8.2 学生开始考试流程

```
学生                                后端                                 DB
  |  GET /api/exams/{id}/preview    |                                   |
  | ------------------------------> |                                   |
  |                                 |  校验 status=publish/running       |
  |                                 |  读 exam.question_sum（items）     |
  |                                 |  对每题:                            |
  |                                 |    SELECT context,img,answer,type  |
  |                                 |    FROM question WHERE id = ?     |
  |                                 |    剔除 answer 字段中的判分关键值 |
  |                                 |  构造 ExamForStudentVO            |
  |  200 { ExamForStudentVO }       |                                   |
  | <------------------------------ |                                   |
```

### 8.3 状态机自动判定逻辑

```java
// 查询时实时计算（推荐）
public ExamStatus resolveCurrentStatus(Exam exam, LocalDateTime now) {
    if (exam.getStatus() == ExamStatus.draft || exam.getStatus() == ExamStatus.done) {
        return exam.getStatus();  // 终态不变
    }
    LocalDateTime starttime = LocalDateTime.parse(exam.getStarttime());
    LocalDateTime endtime = LocalDateTime.parse(exam.getEndtime());
    if (now.isBefore(starttime)) return ExamStatus.publish;
    if (now.isAfter(endtime)) return ExamStatus.done;
    return ExamStatus.running;
}
```

定时任务可周期性（建议每分钟一次）将 `publish` + 已到时间窗的批量更新为 `running`，`running` + 已过 endtime 的批量更新为 `done`，便于按 status 字段查询而无需 JOIN 计算。

---

## 9. 待办与扩展

| 编号 | 内容 | 优先级 |
|---|---|---|
| W1 | 组卷预览（自动生成前先展示抽题结果，确认后再保存） | 中 |
| W2 | 试卷防作弊：从题库随机打乱选项顺序 | 中 |
| W3 | 考试时间到自动提交 | 高 |
| W4 | 考试暂停 / 恢复（断网场景） | 低 |
| W5 | 自动组卷按 `difficulty` 过滤（待 difficulty 字段回归） | 低 |
| W6 | 自动组卷 `usePenalty` 高级策略（按 `accuracy` 综合降权等） | 低 |

---

## 10. 实现状态（更新日期：2026-06-04）

> 本节追踪该模块各层代码的实际实现状态。

| 层级 | 实现状态 | 说明 |
|---|---|---|
| Entity | ✅ 已实现 | 字段与 02-Data-Dictionary.md 完全一致 |
| Enum | ✅ 已实现 | |
| Repository | ✅ 已实现 | 核心查询方法已实现 |
| Controller | ❌ 未实现 | API 端点尚未开发 |
| Service | ❌ 未实现 | 业务逻辑尚未开发 |
| DTO | ❌ 未实现 | 请求/响应 Record 尚未开发 |

---

## 11. 相关文档

- 数据定义：[02-Data-Dictionary.md §4.3 考试表](file:///d:/GDUT-OOP_20260601/wiki/02-Data-Dictionary.md#43-考试表-exam)
- question_sum 快照：[02-Data-Dictionary.md §4.3.1](file:///d:/GDUT-OOP_20260601/wiki/02-Data-Dictionary.md#431-question_sum-json-字段结构)
- 状态机：[02-Data-Dictionary.md §4.3.2](file:///d:/GDUT-OOP_20260601/wiki/02-Data-Dictionary.md#432-状态机4-态)
- 枚举定义：[02-Data-Dictionary.md §5.3 ExamStatus](file:///d:/GDUT-OOP_20260601/wiki/02-Data-Dictionary.md#53-examstatus考试状态)
- 业务矩阵：[02-Data-Dictionary.md §7.3 组卷功能](file:///d:/GDUT-OOP_20260601/wiki/02-Data-Dictionary.md#73-组卷功能m03) / [§7.4 考试管理](file:///d:/GDUT-OOP_20260601/wiki/02-Data-Dictionary.md#74-考试管理m03)
- 数据流转图：[02-Data-Dictionary.md §8](file:///d:/GDUT-OOP_20260601/wiki/02-Data-Dictionary.md#8-数据流转图与说明)
- 题库模块：[M02-Question-Bank.md](file:///d:/GDUT-OOP_20260601/wiki/modules/M02-Question-Bank.md)
- 答题 / 判分：[M04-Score-Statistics.md §3 答题提交](file:///d:/GDUT-OOP_20260601/wiki/modules/M04-Score-Statistics.md)
- 全局规范：[01-Global-Standards.md](file:///d:/GDUT-OOP_20260601/wiki/01-Global-Standards.md)
