# 方案 A：新增单题随机获取接口 — 支持前端自动组卷逐题筛选

## 问题背景

自动组卷流程中，前端预期的交互模式是"逐题获取 → 前端筛选采纳 → 本地缓存 → 最终统一提交"。当前后端仅有 `POST /api/v1/exams/auto`（一键组卷模式），不支持逐题获取。

## 目标

在 M02 题库模块新增 `GET /api/v1/questions/random` 接口，支持按题型筛选并排除已采纳题目，每次仅返回单题 ID 及必要信息（不含答案），供前端展示给教师决定是否采纳。

## 实施步骤

### Step 1: 新增 DTO — `QuestionPreviewVO`

**文件**: `backend/src/main/java/com/cps/backend/modules/M02questionbank/dto/QuestionPreviewVO.java`

```java
package com.cps.backend.modules.M02questionbank.dto;

import com.cps.backend.modules.M02questionbank.enums.QuestionType;

/**
 * 题目预览 VO（用于自动组卷单题获取场景）。
 * 不包含 answer 字段，防止教师预览时看到答案。
 */
public record QuestionPreviewVO(
    Integer id,
    QuestionType type,
    String context,
    Integer img
) {}
```

**说明**: 与 `QuestionVO` 的区别在于不包含 `answer`、`use`、`correct`、`accuracy` 字段，仅返回教师预览所需的题目信息，防止答案泄露。

### Step 2: 新增 Repository 方法

**文件**: `backend/src/main/java/com/cps/backend/modules/M02questionbank/repository/QuestionRepository.java`

在 `QuestionRepository` 接口中新增方法：

```java
/**
 * 随机获取一道题目（排除已采纳的题目）。
 * @param type 题型筛选（可选）
 * @param excludedIds 已采纳的题目 ID 列表
 * @return 单道题目
 */
@Query("""
    SELECT q FROM Question q
    WHERE (:type IS NULL OR q.type = :type)
      AND (:excludedIds IS NULL OR q.id NOT IN :excludedIds)
    ORDER BY FUNCTION('RANDOM')
    """)
Optional<Question> findRandomQuestion(@Param("type") QuestionType type,
                                       @Param("excludedIds") List<Integer> excludedIds);
```

**说明**:
- SQLite 的随机排序函数为 `RANDOM()`，使用 Hibernate `FUNCTION('RANDOM')` 兼容 SQLite
- 使用 `Optional` 返回，支持无结果时的空值处理
- `excludedIds` 为 null 时不限制排除

### Step 3: 新增 Service 方法

**文件**: `backend/src/main/java/com/cps/backend/modules/M02questionbank/service/QuestionService.java`

在 `QuestionService` 中新增方法：

```java
/**
 * 随机获取单道题目（用于自动组卷逐题筛选）。
 * 不操作数据库（仅查询），返回题目预览信息（不含答案）。
 */
public QuestionPreviewVO getRandomQuestion(QuestionType type, List<Integer> excludedIds) {
    return questionRepository.findRandomQuestion(type, excludedIds)
        .map(q -> new QuestionPreviewVO(
            q.getId(),
            q.getType(),
            q.getContext(),
            q.getImg()
        ))
        .orElseThrow(() -> new BusinessException(4203, "没有符合条件的可用题目"));
}
```

**说明**:
- 仅查询，不修改数据，无需 `@Transactional`
- 业务码 4203 对应"题库资源不足"场景
- 返回 `QuestionPreviewVO`，不包含 answer 字段

### Step 4: 新增 Controller 端点

**文件**: `backend/src/main/java/com/cps/backend/modules/M02questionbank/controller/QuestionController.java`

在 `QuestionController` 的 `listQuestions` 方法之前（避免路径冲突），新增端点：

```java
/**
 * 随机获取单道题目（用于自动组卷逐题筛选）。
 * GET /api/v1/questions/random?type=SingleChoice&excludedIds=1,2,3
 * 参考 M03-Exam-Assembly.md — 自动组卷单题获取模式
 */
@GetMapping("/questions/random")
@RequireRole({UserType.teacher, UserType.admin})
public Result<QuestionPreviewVO> getRandomQuestion(
        @RequestParam(required = false) QuestionType type,
        @RequestParam(required = false) List<Integer> excludedIds) {
    QuestionPreviewVO vo = questionService.getRandomQuestion(type, excludedIds);
    return Result.success(vo);
}
```

**说明**:
- `GET /questions/random` 必须在 `GET /questions/{id}` 之前定义（Spring 精确路径优先匹配）
- 实际放在 `listQuestions`（`GET /questions`）之前即可
- `excludedIds` 为可选参数，前端传入已采纳的题目 ID 列表

## 前端交互示例

```
// 前端循环 N 次自动组卷
1. GET /api/v1/questions/random?type=SingleChoice&excludedIds=1,5,8
   → 返回 { id: 12, type: "SingleChoice", context: "...", img: 0 }
2. 教师查看题目，点击"采纳"
3. 前端本地缓存 questionId: 12
4. 前端下次请求时 excludedIds=1,5,8,12（追加）
5. 循环直到组满

// 组满后，前端统一提交
POST /api/v1/exams/manual
{
  "exam": "2026 春季英语期中考试",
  "starttime": "2026-06-15T09:00:00",
  "endtime": "2026-06-15T11:00:00",
  "items": [
    { "questionId": 12, "score": 5 },
    { "questionId": 15, "score": 5 },
    ...
  ]
}
```

## 现有接口不变

- `POST /api/v1/exams/auto` 保留，作为"一键组卷"备选方案
- 前端可选择使用逐题筛选模式或一键组卷模式

## 验证步骤

1. 运行 `mvn compile` 确认编译通过
2. 测试 `GET /api/v1/questions/random` 返回单题（无 excludedIds）
3. 测试 `GET /api/v1/questions/random?excludedIds=1,2,3` 正确排除题目
4. 测试空题库时返回业务码 4203 错误
5. 确认返回的 VO 不包含 answer 字段
