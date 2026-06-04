# M04 - 分数记录与统计模块

> 包含学生答题提交、自动判分 / 教师评卷、分数持久化、题内统计自维护（`question.use` / `question.correct`）、统计报表等业务场景描述、API 接口定义及关键 DTO 设计。
>
> **数据来源**：[02-Data-Dictionary.md §4.4 分数表](file:///d:/GDUT-OOP_20260601/wiki/02-Data-Dictionary.md#44-分数表-score)
>
> **API 契约遵循**：[01-Global-Standards.md §2](file:///d:/GDUT-OOP_20260601/wiki/01-Global-Standards.md#2-统一-api-响应契约)

> **v2.0.0 重大简化**：v1.x 中的独立 `answer_statistics`（答题统计表）在 v2.0.0 **彻底删除**。原 `answer_statistics` 承担的"按题目维度的出现次数 / 正确次数统计"功能，由 `question` 表内的 `use` / `correct` 字段直接承载。

---

## 1. 业务场景

| 场景 | 描述 | 角色 |
|---|---|---|
| 学生提交答卷 | 一次性提交所有题目答案 | 学生 |
| 系统自动判分 | 客观题（单选 / 多选 / 判断 / 填空）自动比对答案 | 系统 |
| 教师评卷 | 主观题（简答）由教师手动打分 | 老师 |
| 分数记录查询 | 学生查看自己的历史成绩 | 学生 |
| 分数管理 | 老师 / 管理员查看某考试的所有考生成绩 | 老师 / 管理员 |
| 题内统计 | 从 `question.use` / `question.correct` 字段读取 | 老师 / 管理员 |
| 统计报表 | 考试通过率、成绩分布、个人错题集 | 老师 / 管理员、学生 |

---

## 2. v2.0.0 关键变化：删除 answer_statistics 表

| 维度 | v1.x（answer_statistics） | v2.0.0（question.use / correct） |
|---|---|---|
| 存储位置 | 独立第 5 张表 | question 表内两个字段 |
| 粒度 | 一行 = 一道题目 | 一行 = 一道题目 |
| 写入时机 | 组卷时 `appear_count += 1`；判分正确时 `correct_count += 1` | 组卷时 `question.use += 1`；判分正确时 `question.correct += 1` |

> **删除原因**：v2.0.0 整体精简数据模型，4 张表 + JSON 字段即可满足全部业务需求。

---

## 3. 答题提交与判分

### 3.1 提交答卷请求

```java
public record ExamSubmitReq(
    @NotNull Integer examId,
    @NotEmpty List<AnswerItem> answers
) {}

public record AnswerItem(
    @NotNull Integer questionId,
    JsonNode userAnswer
) {}
```

> **v2.0.0 简化**：v1.x 的 `durationSeconds`（实际用时）字段**移除**。

### 3.2 判分流程（含题内统计自维护）

```java
public Score submitExam(ExamSubmitReq req, Long userId) {
    Exam exam = examRepository.findById(req.getExamId())
        .orElseThrow(() -> new BusinessException(4301, "考试不存在"));

    // 1. 状态校验：必须 running
    if (resolveCurrentStatus(exam, LocalDateTime.now()) != ExamStatus.running) {
        throw new BusinessException(4301, "考试不在进行中");
    }

    // 2. 重复提交校验
    if (scoreRepository.findByUserAndExam(userId, exam.getId()).isPresent()) {
        throw new BusinessException(4401, "已提交过答卷");
    }

    // 3. 解析 exam.question_sum
    QuestionSum sum = parseQuestionSum(exam.getQuestionSum());
    List<Long> questionIds = sum.getItems().stream()
        .map(QuestionSumItem::getQuestionId).toList();

    // 4. 批量加载 question 表（防 N+1）
    Map<Long, Question> qMap = questionRepository.findAllById(questionIds).stream()
        .collect(Collectors.toMap(Question::getId, q -> q));

    // 5. 逐题判分
    int totalScore = 0;
    List<DetailItem> detailItems = new ArrayList<>();
    for (QuestionSumItem item : sum.getItems()) {
        Question q = qMap.get(item.getQuestionId());
        AnswerItem userAns = req.getAnswers().stream()
            .filter(a -> a.getQuestionId().equals(item.getQuestionId()))
            .findFirst()
            .orElse(new AnswerItem(item.getQuestionId(), null));

        GradingResult gr = gradeOne(q, userAns, item.getScore());
        totalScore += gr.getScore();
        detailItems.add(new DetailItem(
            item.getQuestionId(), userAns.getUserAnswer(),
            q.getAnswer(),
            gr.getScore(), gr.getIsCorrect()
        ));
    }

    // 6. 事务内持久化 score + 题内统计自维护
    Score score = new Score();
    score.setUser(userId);
    score.setExam(exam.getId());
    score.setAll(totalScore);
    score.setDetail(toJson(detailItems));
    scoreRepository.save(score);

    for (DetailItem d : detailItems) {
        if (Boolean.TRUE.equals(d.getIsCorrect())) {
            questionRepository.incrementCorrect(d.getQuestionId());
        }
    }
    return score;
}
```

### 3.3 判分规则（按题型）

| 题型 | 判分逻辑 |
|---|---|
| `SingleChoice` | `userAnswer == correctOption` ? `score` : `0` |
| `MultipleChoice` | `userAnswer 集合 == correctOptions 集合` ? `score` : `0` |
| `Judge` | `userAnswer == correct` ? `score` : `0` |
| `Fill` | 逐空匹配，**全部正确**得满分 |
| `Essay` | 初始 `score = 0`，`isCorrect = null`；由教师手动评卷 |

> **v2.0.0 题型命名变化**：`SINGLE_CHOICE` → `SingleChoice`、`MULTIPLE_CHOICE` → `MultipleChoice`、`TRUE_FALSE` → `Judge`、`FILL_BLANK` → `Fill`、`ESSAY` → `Essay`。

---

## 4. 教师评卷

### 4.1 评卷请求

```java
public record EssayGradeReq(
    @NotNull Integer scoreId,
    @NotNull Integer questionId,
    @NotNull @Min(0) Integer score,
    String comment
) {}
```

### 4.2 评卷流程

```java
public ScoreVO gradeEssay(EssayGradeReq req, Long teacherId) {
    Score score = scoreRepository.findById(req.getScoreId())
        .orElseThrow(() -> new BusinessException(4401, "分数记录不存在"));

    ExamDetail detail = parseDetail(score.getDetail());
    DetailItem item = detail.getItems().stream()
        .filter(i -> i.getQuestionId().equals(req.getQuestionId()))
        .findFirst().orElseThrow();

    Boolean wasCorrect = item.getIsCorrect();

    item.setScore(req.getScore());
    item.setIsCorrect(req.getScore() == item.getMaxScore() ? Boolean.TRUE : Boolean.FALSE);

    int newTotal = detail.getItems().stream()
        .mapToInt(d -> d.getScore() == null ? 0 : d.getScore()).sum();
    score.setAll(newTotal);
    score.setDetail(toJson(detail));
    scoreRepository.save(score);

    // 题内统计自维护
    if (wasCorrect != Boolean.TRUE && item.getIsCorrect() == Boolean.TRUE) {
        questionRepository.incrementCorrect(item.getQuestionId());
    }
    return toScoreVO(score);
}
```

> **v2.0.0 简化**：v1.x 的 `creatorId` 权限校验（仅出卷老师可评）**移除**——v2.0.0 无 `creator_id` 字段，**所有老师/管理员均可评卷**。

---

## 5. 关键 DTO

### 5.1 响应 VO

```java
public record ScoreVO(
    Integer id,
    Integer user,
    String name,
    Integer exam,
    String examName,
    Integer all,
    Integer maxScore,
    Double accuracy,
    List<DetailItemVO> detail
) {}

public record MistakeItemVO(
    Integer questionId,
    QuestionType type,                // v1.x: questionType → type
    String context,                   // 从 question.context 读取（v2.0.0 字段名）
    List<String> options,             // 从 question.answer.options 提取
    JsonNode userAnswer,
    JsonNode correctAnswer,
    Integer examId,
    String examName
    // v1.x: examTime 字段移除（无 submit_time）
) {}
```

### 5.2 统计 VO

```java
public record ExamStatisticsVO(
    Integer examId,
    String examName,
    Integer participantCount,
    Integer submitCount,
    Double passRate,
    Double averageScore,
    Integer maxScore,
    Integer minScore,
    Integer medianScore,
    Map<String, Integer> scoreDistribution
) {}

public record QuestionStatisticsVO(
    Integer questionId,
    QuestionType type,
    Integer use,
    Integer correct,
    Double accuracyRate
) {}

public record UserExamHistoryVO(
    Integer examId,
    String examName,
    Integer all,
    Integer maxScore
) {}
```

---

## 6. API 接口定义

### 6.1 答题与判分

| 方法 | 路径 | 鉴权 | 描述 | 请求 | 响应 |
|---|---|---|---|---|---|
| POST | `/api/v1/exams/{examId}/submit` | 学生 | 提交答卷 | `ExamSubmitReq` | `ScoreVO` |
| POST | `/api/v1/scores/{scoreId}/grade-essay` | 老师 / 管理员 | 评卷 | `EssayGradeReq` | `ScoreVO` |

### 6.2 分数查询

| 方法 | 路径 | 鉴权 | 描述 | 请求 | 响应 |
|---|---|---|---|---|---|
| GET | `/api/v1/scores/me` | 任意登录用户 | 我的所有成绩（分页） | `?page=0&size=20` | `PageResult<ScoreListVO>` |
| GET | `/api/v1/scores/me/mistakes` | 学生 | 我的错题集（分页） | `?page=0&size=20` | `PageResult<MistakeItemVO>` |
| GET | `/api/v1/scores/{id}` | 任意登录用户 | 分数详情 | — | `ScoreVO` |
| GET | `/api/v1/exams/{examId}/scores` | 老师 / 管理员 | 某考试的所有考生分数（分页） | `?page=0&size=20` | `PageResult<ScoreListVO>` |

### 6.3 统计报表

| 方法 | 路径 | 鉴权 | 描述 | 请求 | 响应 |
|---|---|---|---|---|---|
| GET | `/api/v1/statistics/exams/{examId}` | 老师 / 管理员 | 考试统计 | — | `ExamStatisticsVO` |
| GET | `/api/v1/statistics/questions` | 老师 / 管理员 | 题目统计（分页/排序） | `?page=0&size=20&sortBy=use` | `PageResult<QuestionStatisticsVO>` |
| GET | `/api/v1/statistics/questions/{id}` | 老师 / 管理员 | 单题详细统计 | — | `QuestionStatisticsVO` |

### 6.4 草稿缓存（新增）

| 方法 | 路径 | 鉴权 | 描述 | 请求 | 响应 |
|---|---|---|---|---|---|
| PUT | `/api/v1/exams/{examId}/draft` | 学生 | 保存草稿答案（考试中自动保存） | `DraftSaveReq` | `Result<Void>` |
| GET | `/api/v1/exams/{examId}/draft` | 学生 | 加载草稿答案 | — | `List<AnswerItem>` |

> **API 前缀**：`/api/v1/`（与代码实际一致）。
> **路由顺序说明**：`/scores/me` 和 `/scores/me/mistakes` 必须在 `/scores/{id}` 之前定义，否则 Spring 会将 "me" 误解析为 id。

---

## 7. 业务规则

1. **重复提交拒绝**：基于 `score` 表的 `uk_score_user_exam` UNIQUE 约束，重复提交抛 `4401`。
2. **时间窗外提交拒绝**：考试 `endtime` 后提交抛 `4301`。
3. **提交后不可修改**：分数记录一经写入不可由学生再次提交。
4. **简答题必走评卷**：`Essay` 题型初始 `isCorrect = null`，待教师评卷。
5. **判分幂等性**：教师可重复评同一题（修改分数），系统以最后一次评分为准；`question.correct` 增量更新。
6. **错题集去重**：同一道题被多个考试考错，在错题集中**只展示一次**（取最近一次错误记录）。
7. **统计实时性**：题内统计（`question.use` / `question.correct`）在提交时即时更新。
8. **物理外键**：建议开启 SQLite 外键（`PRAGMA foreign_keys = ON`）后启用 `score.user` / `score.exam` 物理 FK。

---

## 8. 关键业务流

### 8.1 学生答题提交流程（含题内统计自维护）

```
学生 → POST /api/exams/123/submit
  → 校验 exam.status = running
  → 校验时间窗
  → 校验未提交
  → 解析 exam.question_sum
  → 批量加载 question (防 N+1)
  → 逐题判分
  → BEGIN TX
  → INSERT INTO score
  → for each correct objective:
       UPDATE question SET correct = correct + 1
  → COMMIT TX
  ← ScoreVO
```

> **事务边界**：`INSERT score` 与 `UPDATE question.correct` **必须在同一事务内**。

---

## 9. 复杂查询优化

### 9.1 防 N+1

```java
// 错误：循环查 question
for (DetailItem d : items) {
    Question q = questionRepository.findById(d.getQuestionId()).get();  // N+1!
    d.setQuestionContent(q.getContext());
}

// 正确：批量查询
List<Long> qIds = items.stream().map(DetailItem::getQuestionId).toList();
Map<Long, Question> qMap = questionRepository.findAllById(qIds).stream()
    .collect(Collectors.toMap(Question::getId, q -> q));
items.forEach(d -> {
    Question q = qMap.get(d.getQuestionId());
    if (q != null) d.setQuestionContent(q.getContext());
});
```

### 9.2 题目统计查询（从 question 表直接派生）

```java
public interface QuestionRepository extends JpaRepository<Question, Integer> {

    @Query("""
        SELECT q FROM Question q
        WHERE q.use > 5
        ORDER BY (q.correct * 1.0 / q.use) ASC
    """)
    List<Question> findHardQuestions(Pageable pageable);

    @Query("""
        SELECT q FROM Question q
        WHERE q.use > 5
          AND (q.correct * 1.0 / q.use) < 0.3
        ORDER BY q.use DESC
    """)
    List<Question> findFrequentlyFailedQuestions();
}
```

> **v2.0.0 简化**：v1.x 的 `AnswerStatisticsRepository` 在 v2.0.0 **整体删除**。原统计查询改写为直接基于 `Question` 实体的 JPQL。

### 9.3 增量更新 SQL

```java
@Modifying
@Query("UPDATE Question q SET q.use = q.use + 1 WHERE q.id = :id")
int incrementUse(@Param("id") Integer id);

@Modifying
@Query("UPDATE Question q SET q.correct = q.correct + 1 WHERE q.id = :id")
int incrementCorrect(@Param("id") Integer id);
```

---

## 10. 待办与扩展

| 编号 | 内容 | 优先级 |
|---|---|---|
| W1 | 填空题"按空给分" | 中 |
| W2 | 错题本收藏 | 中 |
| W3 | 成绩导出 Excel | 中 |
| W4 | 雷达图 / 趋势图分析 | 低 |
| W5 | 题目知识点关联 | 低 |
| W6 | 答题用时记录 | 低 |

---

## 11. 实现状态（更新日期：2026-06-04）

> 本节追踪该模块各层代码的实际实现状态。

| 层级 | 实现状态 | 说明 |
|---|---|---|
| Entity | ✅ 已实现 | 字段与 02-Data-Dictionary.md 完全一致 |
| Enum | ✅ 已实现 | （无独立枚举，复用 M02/M03） |
| Repository | ✅ 已实现 | findByUserAndExam / findByUser / findByExam / upsertScore（原生 UPSERT） |
| Controller | ✅ 已实现 | ScoreController（9 端点）+ DraftController（2 端点）|
| Service | ✅ 已实现 | ScoreService（submitExam/gradeEssay/getMyScores/getMyMistakes/findById/getExamScores/getExamStatistics/getQuestionStatisticsPaginated/getQuestionStatisticById）+ DraftCacheService（saveDraft/loadDraft）|
| DTO | ✅ 已实现 | ExamSubmitReq, AnswerItem, ScoreVO/Detail/ListVO, DetailItem/VO, MistakeItemVO, EssayGradeReq, ExamStatisticsVO, QuestionStatisticsVO, UserExamHistoryVO, Summary |

---

## 12. 相关文档

- 数据定义：[02-Data-Dictionary.md §4.4 分数表](file:///d:/GDUT-OOP_20260601/wiki/02-Data-Dictionary.md#44-分数表-score)
- 题内统计自维护：[02-Data-Dictionary.md §4.2.3](file:///d:/GDUT-OOP_20260601/wiki/02-Data-Dictionary.md#423-use--correct-自维护规则)
- 枚举定义：[02-Data-Dictionary.md §5.2 QuestionType](file:///d:/GDUT-OOP_20260601/wiki/02-Data-Dictionary.md#52-questiontype题目类型)
- 业务矩阵：[02-Data-Dictionary.md §7.5 成绩管理](file:///d:/GDUT-OOP_20260601/wiki/02-Data-Dictionary.md#75-成绩管理m04)
- 数据流转图：[02-Data-Dictionary.md §8](file:///d:/GDUT-OOP_20260601/wiki/02-Data-Dictionary.md#8-数据流转图与说明)
- 考试模块：[M03-Exam-Assembly.md](file:///d:/GDUT-OOP_20260601/wiki/modules/M03-Exam-Assembly.md)
- 题库模块：[M02-Question-Bank.md](file:///d:/GDUT-OOP_20260601/wiki/modules/M02-Question-Bank.md)
- 全局规范：[01-Global-Standards.md](file:///d:/GDUT-OOP_20260601/wiki/01-Global-Standards.md)
