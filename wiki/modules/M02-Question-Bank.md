# M02 - 题库管理模块

> 包含题目 CRUD、批量导入、按类型 / 关键字检索、答案 JSON 自适应、题内统计自维护（`use` / `correct`）等业务场景描述、API 接口定义及关键 DTO 设计。
>
> **数据来源**：[02-Data-Dictionary.md §4.2 题目表](file:///d:/GDUT-OOP_20260601/wiki/02-Data-Dictionary.md#42-题目表-question)
>
> **API 契约遵循**：[01-Global-Standards.md §2](file:///d:/GDUT-OOP_20260601/wiki/01-Global-Standards.md#2-统一-api-响应契约)

---

## 1. 业务场景

| 场景 | 描述 | 角色 |
|---|---|---|
| 题目创建 | 教师录入单道题目（题干 + 可选图片 + 答案 JSON） | 老师、管理员 |
| 题目批量导入 | 通过 JSON 数组批量上传 | 老师、管理员 |
| 题目查询 | 分页、按 `type` / 关键字（`context` LIKE）过滤 | 老师、管理员 |
| 题目修改 | 修改题干或答案 | 老师、管理员 |
| 题目删除 | 硬删除（**警告**：已被组卷的题目删除会破坏 `exam.question_sum` 快照引用） | 老师、管理员 |
| 题目预览 | 教师在组卷前可预览完整题目 | 老师 |
| 题内统计自维护 | 组卷时 `use += 1`；判分正确时 `correct += 1` | 内部（Service 层事务内） |

> **v2.0.0 设计原则**：本模块**不记录**题目分类、标签、性质、难度、解析等扩展元数据。每道题仅包含 `id` / `type` / `context` / `img` / `answer` / `use` / `correct` 7 个内禀字段。

---

## 2. 题目类型枚举（v2.0.0 命名变更）

```java
public enum QuestionType {
    SingleChoice("单选题"),
    MultipleChoice("多选题"),
    Judge("判断题"),
    Fill("填空题"),
    Essay("简答题");
}
```

| 题型 | 是否需要 options | 是否系统自动判分 | 是否需要教师评卷 |
|---|---|---|---|
| `SingleChoice` | 是 | 是 | 否 |
| `MultipleChoice` | 是 | 是 | 否 |
| `Judge` | 否 | 是 | 否 |
| `Fill` | 否 | 是（按空匹配） | 否 |
| `Essay` | 否 | 否 | 是 |

> **v1.x → v2.0.0 枚举重命名对照**：
> - `SINGLE_CHOICE` → `SingleChoice`
> - `MULTIPLE_CHOICE` → `MultipleChoice`
> - `TRUE_FALSE` → `Judge`
> - `FILL_BLANK` → `Fill`
> - `ESSAY` → `Essay`

---

## 3. 答案 JSON 扩展规范

> 详细 JSON Schema 参见 [02-Data-Dictionary.md §4.2.1](file:///d:/GDUT-OOP_20260601/wiki/02-Data-Dictionary.md#421-answer-json-字段结构按-type-适配)。

| 题型 | answer JSON |
|---|---|
| `SingleChoice` | `{ "version": 1, "correctOption": "B", "options": ["A","B","C","D"] }` |
| `MultipleChoice` | `{ "version": 1, "correctOptions": ["A","C"], "options": ["A","B","C","D"] }` |
| `Judge` | `{ "version": 1, "correct": true }` |
| `Fill` | `{ "version": 1, "blanks": ["went", "goes"] }` |
| `Essay` | `{ "version": 1, "reference": "...", "keywords": ["..."], "scoreRule": "..." }` |

---

## 4. `img` 标志与图片路径匹配规则

```
文件路径模板：.\Data\img\{questionId}.{ext}
支持扩展名：.png / .jpg / .jpeg / .gif
匹配策略：按 id 数字查找，找到第一个存在的扩展名即用
```

> **注意**：v2.0.0 中 `img` 仅为 Boolean 标志位，**不**在 `question` 表中存图片路径或文件名。

---

## 5. 题内统计自维护（use / correct）

| 字段 | 含义 | 自增时机 |
|---|---|---|
| `use` | 题目被纳入考试的次数 | 组卷时 `use += 1`（详见 [M03 §3](file:///d:/GDUT-OOP_20260601/wiki/modules/M03-Exam-Assembly.md)） |
| `correct` | 题目在考试中被回答正确的次数 | 判分 `isCorrect = true` 时 `correct += 1`（详见 [M04 §3](file:///d:/GDUT-OOP_20260601/wiki/modules/M04-Score-Statistics.md)） |

> **不变量**：`0 <= correct <= use`。自维护**必须**在 Service 层事务内执行。

---

## 6. 关键 DTO

### 6.1 请求 DTO

```java
public record QuestionCreateReq(
    @NotNull QuestionType type,
    @NotBlank String context,
    Boolean img,
    @NotNull JsonNode answer
) {}

public record QuestionQueryReq(
    QuestionType type,
    String keyword,
    Integer page,
    Integer size,
    String sort
) {}
```

### 6.2 响应 VO

```java
public record QuestionVO(
    Integer id,
    QuestionType type,
    String context,
    Integer img,
    Answer answer,
    Integer use,
    Integer correct,
    Double accuracy
) {}
```

---

## 7. API 接口定义

| 方法 | 路径 | 鉴权 | 描述 |
|---|---|---|---|
| GET | `/api/v1/questions/random` | 老师 / 管理员 | 随机获取单道题目（自动组卷用） |
| POST | `/api/v1/questions` | 老师 / 管理员 | 创建题目 |
| POST | `/api/v1/questions/batch` | 老师 / 管理员 | 批量导入 |
| GET | `/api/v1/questions/{id}` | 老师 / 管理员 | 题目详情 |
| PUT | `/api/v1/questions/{id}` | 老师 / 管理员 | 更新题目 |
| DELETE | `/api/v1/questions/{id}` | 老师 / 管理员 | 删除题目（**警告**：破坏快照引用） |
| GET | `/api/v1/questions` | 老师 / 管理员 | 分页查询 |
| DELETE | `/api/v1/questions/batch` | 老师 / 管理员 | 批量删除题目 |

> **API 前缀**：`/api/v1/`（与代码实际一致）。
> **路由顺序说明**：`/questions/random` 必须在 `/questions/{id}` 之前定义，否则 Spring 会将 "random" 误解析为 ID。

---

## 8. 业务规则

1. **创建权限**：仅老师 / 管理员可创建题目；学生角色 API 端直接拒绝（403）。
2. **修改归属**：v2.0.0 无 `creator_id` 字段，**所有老师/管理员均可修改任意题目**。
3. **删除保护**：v2.0.0 **无软删除/归档机制**，硬删除即可。已被纳入考试的题目被删除后，**该考试仍可正常进行**（因快照已固化），但题内统计（`use` / `correct`）将永久停留在被删除题目上。
4. **批量导入限制**：单次不超过 100 题。
5. **答案强校验**：根据 `type` 校验 `answer` JSON 结构匹配。
6. **题干长度限制**：1~5000 字符。
7. **img 标志同步**：创建题目时 `img=1` 时，DB 分配 id 后前端需将图片上传到 `.\Data\img\{id}.{ext}`。

---

## 9. 关键业务流

### 9.1 题目创建流程

```
老师 → POST /api/questions
  → 校验 answer JSON 结构
  → Jackson 序列化 answer
  → INSERT INTO question (use=0, correct=0)
  → 反序列化 answer → 强类型
  → 计算 accuracy
  ← QuestionVO
```

### 9.2 题内统计自维护触发点

```
[组卷]   questionService.incrementUse(questionId)
[判分]   questionService.incrementCorrect(questionId)
            │
            ▼
   Service 层事务内
   UPDATE question SET use = use + 1 WHERE id = ?
```

> **事务边界**：组卷 / 判分与 `question.use` / `question.correct` 写入必须在**同一事务**内。

---

## 10. 答案反序列化的多态处理

```java
public Answer parseAnswer(String json, QuestionType type) {
    return switch (type) {
        case SingleChoice   -> objectMapper.readValue(json, SingleChoiceAnswer.class);
        case MultipleChoice -> objectMapper.readValue(json, MultipleChoiceAnswer.class);
        case Judge          -> objectMapper.readValue(json, JudgeAnswer.class);
        case Fill           -> objectMapper.readValue(json, FillAnswer.class);
        case Essay          -> objectMapper.readValue(json, EssayAnswer.class);
    };
}
```

---

## 11. 待办与扩展

| 编号 | 内容 | 优先级 |
|---|---|---|
| W1 | Excel 批量导入 | 中 |
| W2 | 题目版本历史 | 中 |
| W3 | 题目收藏 / 错误本 | 低 |
| W4 | 难度字段 difficulty 回归 | 低 |
| W5 | analysis 答案解析字段回归 | 低 |

---

## 12. 实现状态（更新日期：2026-06-04）

> 本节追踪该模块各层代码的实际实现状态。

| 层级 | 实现状态 | 说明 |
|---|---|---|
| Entity | ✅ 已实现 | 字段与 02-Data-Dictionary.md 完全一致 |
| Enum | ✅ 已实现 | QuestionType |
| Repository | ✅ 已实现 | findByType / searchByKeyword（分页）/ incrementUse / incrementCorrect |
| Controller | ✅ 已实现 | 8 个端点（random/create/batch/getById/update/delete/list/batchDelete） |
| Service | ✅ 已实现 | create/batchCreate/findById/search/update/delete/getRandomQuestion |
| DTO | ✅ 已实现 | QuestionCreateReq/UpdateReq/QueryReq/VO, BatchImportResult, QuestionPreviewVO, Answer 多态（5 种） |

---

## 13. 相关文档

- 数据定义：[02-Data-Dictionary.md §4.2 题目表](file:///d:/GDUT-OOP_20260601/wiki/02-Data-Dictionary.md#42-题目表-question)
- 答案 JSON：[02-Data-Dictionary.md §4.2.1](file:///d:/GDUT-OOP_20260601/wiki/02-Data-Dictionary.md#421-answer-json-字段结构按-type-适配)
- img 路径规则：[02-Data-Dictionary.md §4.2.2](file:///d:/GDUT-OOP_20260601/wiki/02-Data-Dictionary.md#422-img-路径匹配规则)
- 题内统计：[02-Data-Dictionary.md §4.2.3](file:///d:/GDUT-OOP_20260601/wiki/02-Data-Dictionary.md#423-use--correct-自维护规则)
- 枚举定义：[02-Data-Dictionary.md §5.2 QuestionType](file:///d:/GDUT-OOP_20260601/wiki/02-Data-Dictionary.md#52-questiontype题目类型)
- 业务矩阵：[02-Data-Dictionary.md §7.2 题库管理](file:///d:/GDUT-OOP_20260601/wiki/02-Data-Dictionary.md#72-题库管理m02)
- 全局规范：[01-Global-Standards.md](file:///d:/GDUT-OOP_20260601/wiki/01-Global-Standards.md)
- 后续模块：[M03-Exam-Assembly.md](file:///d:/GDUT-OOP_20260601/wiki/modules/M03-Exam-Assembly.md)
