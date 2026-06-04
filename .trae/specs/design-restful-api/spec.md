# RESTful API 接口设计 Spec

## Why
当前项目已完成数据层（Entity + Repository）和业务层（Service + DTO），但缺少统一的 RESTful API 接口文档来指导前后端联调。需要基于项目已有的 4 表设计（user / question / exam / score）和 wiki 规范，设计一套完整的接口清单。

## What Changes
- 创建完整的 RESTful API 接口文档，覆盖 6 个核心业务模块
- 所有接口统一使用 `/api/v1/` 前缀，遵循项目 wiki 定义的 URL 命名规范
- 所有接口统一返回 `Result<T>` 格式
- 补充"答题自动保存"接口设计（用户需求矩阵中新增）

## Impact
- Affected specs: 本项目所有 Controller 层开发
- Affected code: `backend/src/main/java/com/cps/backend/modules/*/controller/`
- 输出产物: `wiki/API-Reference.md`（Wiki 格式接口文档）

---

## 第一部分：需求合理性与架构分析

### 1.1 业务模块分析与优化方案

| 用户需求 | 原需求瑕疵 | 优化方案 |
|---|---|---|
| **用户管理** | 需求中"新增用户（单个）"缺少登录/注册分离 | 拆分为 `POST /auth/login`（登录）和 `POST /auth/register`（注册），管理员独立管理接口 |
| **题库管理** | 用户未提及"按类型/关键字检索"，但这是题库核心场景 | 保留 wiki 定义的 `GET /questions` 分页筛选接口 |
| **试卷管理** | 用户需求中提到"Paper表"，但项目实际用 `exam.question_sum` JSON 承载 | 遵循项目实际设计：试卷即考试，通过 Exam 表的 `question_sum` 字段表达，不引入独立 Paper 表 |
| **考试管理** | 无瑕疵，直接对齐 wiki 定义 | 保持 `draft→publish→running→done` 状态机 |
| **答题自动保存** | wiki 中未定义此场景，但用户需求明确提出"防丢失" | 新增 `PUT /api/v1/exams/{examId}/draft` 接口，周期性保存草稿答案，不判分、不写 score 表 |
| **交卷判分** | wiki 已有定义，但无"自动保存→最终提交"的衔接 | 提交接口校验草稿一致性，确保数据完整 |
| **数据统计** | wiki 已定义，无瑕疵 | 保持只读查询特性 |

### 1.2 全局鉴权机制

| 机制 | 实现方式 |
|---|---|
| **Token 格式** | JWT（HS256 对称加密），有效期 24 小时 |
| **传递方式** | `Authorization: Bearer <token>` 请求头 |
| **Token 载荷** | `{ "sub": "userId", "name": "zhangsan", "type": "student", "iat": ..., "exp": ... }` |
| **公开接口** | `POST /auth/login`、`POST /auth/register` 无需 Token |
| **角色拦截** | 使用 `@RequireRole` 注解，拦截器从 JWT 解析 `type` 字段校验权限 |
| **异常码** | 4101 未登录、4102 Token 过期、4103 无权限 |

### 1.3 全局异常处理机制

| 异常类型 | 处理策略 | HTTP 状态 | 业务码 |
|---|---|---|---|
| `BusinessException` | 透传 code + message | 200（业务异常不走 HTTP 状态码） | 按模块定义 |
| `IllegalArgumentException` | 参数校验失败 | 200 | 4000 |
| 其他 `Exception` | 兜底，返回"系统繁忙" | 200 | 5000 |

### 1.4 基于项目实际的调整优化

| 调整项 | 原因 |
|---|---|
| URL 前缀使用 `/api/v1/` 而非 wiki 草案的 `/api/` | 遵循用户需求矩阵中的最佳实践 |
| 批量删除使用 `DELETE /api/v1/resources` + Body 传 ID 数组 | 符合 REST 最佳实践 |
| `score.all` 在 API 响应中重命名为 `totalScore` | 避免与 SQL 关键字 `ALL` 冲突 |
| `score.user` / `score.exam` 在响应中重命名为 `userId` / `examId` | API 字段语义更清晰 |
| 不引入 Paper 相关接口 | 项目实际使用 Exam + `question_sum` JSON 方案 |

---

## 第二部分：RESTful API 接口清单

### 2.0 统一响应体规范

```json
// 成功响应
{
  "code": 200,
  "message": "success",
  "data": { ... }
}

// 分页响应
{
  "code": 200,
  "message": "success",
  "data": {
    "content": [ ... ],
    "totalElements": 100,
    "totalPages": 5,
    "page": 0,
    "size": 20
  }
}

// 错误响应
{
  "code": 4101,
  "message": "未登录或 Token 已过期",
  "data": null
}
```

### M01 - 用户认证与权限管理

| 编号 | 接口名称 | 方法 | URL | 鉴权 | 请求参数 | 响应数据 |
|---|---|---|---|---|---|---|
| U01 | 用户登录 | POST | `/api/v1/auth/login` | 公开 | Body: `{ name, password }` | `{ token, user: { id, name, type, status } }` |
| U02 | 用户注册 | POST | `/api/v1/auth/register` | 公开 | Body: `{ name, password, type }` | `{ id, name, type, status }` |
| U03 | 注销登录 | POST | `/api/v1/auth/logout` | 任意登录用户 | 无 | 空 |
| U04 | 当前用户信息 | GET | `/api/v1/auth/me` | 任意登录用户 | 无 | `{ id, name, type, status }` |
| U05 | 修改密码 | POST | `/api/v1/auth/password` | 任意登录用户 | Body: `{ oldPassword, newPassword }` | 空 |
| U06 | 用户列表 | GET | `/api/v1/users` | admin | Query: `?page=0&size=20&type=student&status=1` | `PageResult<UserVO>` |
| U07 | 创建用户 | POST | `/api/v1/users` | admin | Body: `{ name, password, type }` | `{ id, name, type, status }` |
| U08 | 更新用户 | PUT | `/api/v1/users/{id}` | admin | Body: `{ name, type, status }` | `{ id, name, type, status }` |
| U09 | 用户状态切换 | PATCH | `/api/v1/users/{id}/status` | admin | Body: `{ status }` | 空 |
| U10 | 删除用户 | DELETE | `/api/v1/users/{id}` | admin | Path: id | 空 |
| U11 | 批量删除用户 | DELETE | `/api/v1/users/batch` | admin | Body: `[1, 2, 3]` | 空 |

### M02 - 题库管理

| 编号 | 接口名称 | 方法 | URL | 鉴权 | 请求参数 | 响应数据 |
|---|---|---|---|---|---|---|
| Q01 | 创建题目 | POST | `/api/v1/questions` | teacher/admin | Body: `{ type, context, img, answer }` | `QuestionVO` |
| Q02 | 批量导入题目 | POST | `/api/v1/questions/batch` | teacher/admin | Body: `[{ type, context, img, answer }, ...]` | `{ successCount, failCount, errors: [{ index, message }] }` |
| Q03 | 题目详情 | GET | `/api/v1/questions/{id}` | teacher/admin | Path: id | `QuestionVO` |
| Q04 | 题目列表 | GET | `/api/v1/questions` | teacher/admin | Query: `?page=0&size=20&type=SingleChoice&keyword=hello` | `PageResult<QuestionVO>` |
| Q05 | 更新题目 | PUT | `/api/v1/questions/{id}` | teacher/admin | Path: id, Body: `{ type, context, img, answer }` | `QuestionVO` |
| Q06 | 删除题目 | DELETE | `/api/v1/questions/{id}` | teacher/admin | Path: id | 空 |
| Q07 | 批量删除题目 | DELETE | `/api/v1/questions/batch` | teacher/admin | Body: `[1, 2, 3]` | 空 |

### M03 - 考试与组卷

| 编号 | 接口名称 | 方法 | URL | 鉴权 | 请求参数 | 响应数据 |
|---|---|---|---|---|---|---|
| E01 | 手动组卷 | POST | `/api/v1/exams/manual` | teacher/admin | Body: `{ exam, starttime, endtime, items: [{ questionId, score }] }` | `ExamVO` |
| E02 | 自动组卷 | POST | `/api/v1/exams/auto` | teacher/admin | Body: `{ exam, starttime, endtime, autoRule: { totalQuestions, totalScore, typeFilter?, usePenalty? } }` | `ExamVO` |
| E03 | 考试详情 | GET | `/api/v1/exams/{id}` | teacher/admin | Path: id | `ExamVO` |
| E04 | 学生视角预览 | GET | `/api/v1/exams/{id}/preview` | student | Path: id | `ExamForStudentVO`（答案已脱敏） |
| E05 | 更新考试 | PUT | `/api/v1/exams/{id}` | teacher(自己)/admin | Path: id, Body: `{ exam, starttime, endtime, items }` | `ExamVO` |
| E06 | 发布考试 | POST | `/api/v1/exams/{id}/publish` | teacher(自己)/admin | Path: id | 空 |
| E07 | 撤回考试 | POST | `/api/v1/exams/{id}/withdraw` | teacher(自己)/admin | Path: id | 空 |
| E08 | 删除考试 | DELETE | `/api/v1/exams/{id}` | teacher(自己)/admin | Path: id | 空 |
| E09 | 考试列表 | GET | `/api/v1/exams` | teacher/admin | Query: `?page=0&size=20&status=publish` | `PageResult<ExamVO>` |
| E10 | 可参加考试 | GET | `/api/v1/exams/available` | student | 无 | `List<ExamForStudentVO>` |
| E11 | 草稿答案保存 | PUT | `/api/v1/exams/{examId}/draft` | student | Path: examId, Body: `{ answers: [{ questionId, userAnswer }] }` | 空 |
| E12 | 加载草稿答案 | GET | `/api/v1/exams/{examId}/draft` | student | Path: examId | `{ answers: [{ questionId, userAnswer }] }` |

### M04 - 分数与统计

| 编号 | 接口名称 | 方法 | URL | 鉴权 | 请求参数 | 响应数据 |
|---|---|---|---|---|---|---|
| S01 | 提交答卷 | POST | `/api/v1/exams/{examId}/submit` | student | Path: examId, Body: `{ answers: [{ questionId, userAnswer }] }` | `ScoreVO` |
| S02 | 评卷（简答题） | POST | `/api/v1/scores/{scoreId}/grade-essay` | teacher/admin | Path: scoreId, Body: `{ questionId, score, comment? }` | `ScoreVO` |
| S03 | 我的成绩列表 | GET | `/api/v1/scores/me` | 任意登录用户 | Query: `?page=0&size=20` | `PageResult<ScoreListVO>` |
| S04 | 分数详情 | GET | `/api/v1/scores/{id}` | student(自己)/teacher/admin | Path: id | `ScoreVO` |
| S05 | 考试成绩列表 | GET | `/api/v1/exams/{examId}/scores` | teacher/admin | Path: examId, Query: `?page=0&size=20` | `PageResult<ScoreListVO>` |
| S06 | 我的错题集 | GET | `/api/v1/scores/me/mistakes` | student | Query: `?page=0&size=20` | `PageResult<MistakeItemVO>` |
| S07 | 考试统计 | GET | `/api/v1/statistics/exams/{examId}` | teacher/admin | Path: examId | `ExamStatisticsVO` |
| S08 | 题目统计 | GET | `/api/v1/statistics/questions` | teacher/admin | Query: `?page=0&size=20&sortBy=accuracy` | `PageResult<QuestionStatisticsVO>` |
| S09 | 单题统计详情 | GET | `/api/v1/statistics/questions/{id}` | teacher/admin | Path: id | `QuestionStatisticsVO` |

---

## 第三部分：核心复杂接口的详细设计

### 3.1 答题自动保存接口

**接口**: `PUT /api/v1/exams/{examId}/draft`

**场景**: 学生在答题过程中，前端每隔 30 秒自动调用此接口保存当前作答内容到内存缓存（Redis 或 ConcurrentHashMap），防止浏览器崩溃或断网导致数据丢失。草稿答案不写入 score 表，不触发判分。

**请求示例**:
```json
{
  "answers": [
    { "questionId": 1, "userAnswer": "A" },
    { "questionId": 2, "userAnswer": ["A", "C"] },
    { "questionId": 3, "userAnswer": "Hello world" }
  ]
}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

**后端处理逻辑**:
1. 校验考试状态为 `running`（或 `publish` 且已在时间窗内）
2. 校验当前用户是该考试的考生
3. 校验 `questionId` 属于该考试的 `question_sum.items`
4. 将答案写入内存缓存（key: `"exam:{examId}:draft:{userId}"`，TTL: 2 小时）
5. 前端需做防抖处理（debounce 500ms），避免频繁请求
6. **无需事务**（不写数据库），纯缓存操作
7. 交卷时自动加载草稿作为提交数据

**加载草稿接口** `GET /api/v1/exams/{examId}/draft`:
```json
// 响应
{
  "code": 200,
  "message": "success",
  "data": {
    "answers": [
      { "questionId": 1, "userAnswer": "A" },
      { "questionId": 2, "userAnswer": ["A", "C"] }
    ]
  }
}
```

---

### 3.2 交卷判分接口

**接口**: `POST /api/v1/exams/{examId}/submit`

**场景**: 学生完成答题后点击"提交交卷"，后端从缓存加载草稿答案（或使用请求中的答案），执行统一判分，写入 score 表并更新题目统计。

**请求示例**:
```json
{
  "answers": [
    { "questionId": 1, "userAnswer": "A" },
    { "questionId": 2, "userAnswer": ["A", "C"] },
    { "questionId": 3, "userAnswer": false },
    { "questionId": 4, "userAnswer": ["went", "goes"] },
    { "questionId": 5, "userAnswer": "This is my essay answer..." }
  ]
}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "userId": 1001,
    "name": "张三",
    "examId": 12,
    "examName": "2026 春季英语期中考试",
    "totalScore": 85,
    "maxScore": 100,
    "accuracy": 0.85,
    "detail": [
      {
        "questionId": 1,
        "type": "SingleChoice",
        "userAnswer": "A",
        "correctAnswer": "A",
        "score": 5,
        "maxScore": 5,
        "isCorrect": true
      },
      {
        "questionId": 2,
        "type": "MultipleChoice",
        "userAnswer": ["A", "C"],
        "correctAnswer": ["A", "D"],
        "score": 0,
        "maxScore": 5,
        "isCorrect": false
      },
      {
        "questionId": 3,
        "type": "Judge",
        "userAnswer": false,
        "correctAnswer": true,
        "score": 0,
        "maxScore": 5,
        "isCorrect": false
      },
      {
        "questionId": 4,
        "type": "Fill",
        "userAnswer": ["went", "goes"],
        "correctAnswer": ["went", "goes"],
        "score": 5,
        "maxScore": 5,
        "isCorrect": true
      },
      {
        "questionId": 5,
        "type": "Essay",
        "userAnswer": "This is my essay answer...",
        "correctAnswer": null,
        "score": 0,
        "maxScore": 20,
        "isCorrect": null
      }
    ]
  }
}
```

**后端处理逻辑**:
1. **校验**：考试状态为 `running`、未超时、未重复提交
2. **加载草稿**：优先从请求中取答案；若请求中无答案则从缓存加载草稿
3. **解析快照**：读取 `exam.question_sum` 获取题序和分值
4. **批量查询**：使用 `questionRepository.findAllById(questionIds)` 批量加载题目（防 N+1）
5. **逐题判分**：
   - SingleChoice: 精确匹配 `correctOption`
   - MultipleChoice: 集合完全匹配 `correctOptions`
   - Judge: 布尔值匹配
   - Fill: 逐空匹配（忽略大小写和首尾空格）
   - Essay: `score=0`, `isCorrect=null`（待教师评卷）
6. **事务内操作**（`@Transactional(rollbackFor = Exception.class)`）：
   - 写入 `score` 表（一人一考唯一，重复提交时拒绝）
   - 对于 `isCorrect=true` 的客观题，执行 `question.correct += 1`
7. **清理缓存**：删除该用户的草稿缓存
8. **返回结果**：构造 `ScoreVO` 返回

---

### 3.3 批量导入题目接口

**接口**: `POST /api/v1/questions/batch`

**场景**: 教师通过 JSON 数组批量上传题目，单次最多 100 题。需要逐题校验 answer JSON 结构，部分失败不影响成功题目，最终返回成功/失败统计。

**请求示例**:
```json
[
  {
    "type": "SingleChoice",
    "context": "What is the past tense of 'go'?",
    "img": 0,
    "answer": {
      "version": 1,
      "correctOption": "B",
      "options": ["A", "B", "C", "D"]
    }
  },
  {
    "type": "Fill",
    "context": "She ___ (go) to school every day.",
    "img": 0,
    "answer": {
      "version": 1,
      "blanks": ["goes"]
    }
  },
  {
    "type": "SingleChoice",
    "context": "Invalid question with wrong answer format",
    "img": 0,
    "answer": {
      "version": 1
    }
  }
]
```

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "successCount": 2,
    "failCount": 1,
    "errors": [
      {
        "index": 2,
        "message": "SingleChoice 题目缺少 required 字段: correctOption"
      }
    ]
  }
}
```

**后端处理逻辑**:
1. 校验数组长度 ≤ 100
2. 逐题校验 `answer` JSON 结构（根据 `type` 校验必填字段）
3. **使用事务**：所有合法题目在一个事务内批量插入（`questionRepository.saveAll()`）
4. 不合法题目记录错误信息，不影响合法题目的入库
5. 返回成功数和失败详情

---

## 第四部分：数据模型定义（响应 VO）

### 4.1 用户相关

```json
// UserVO
{
  "id": 1001,
  "name": "zhangsan",
  "type": "student",
  "status": 1
}

// LoginResp
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "user": {
    "id": 1001,
    "name": "zhangsan",
    "type": "student",
    "status": 1
  }
}

// BatchImportResult
{
  "successCount": 2,
  "failCount": 1,
  "errors": [
    { "index": 2, "message": "错误描述" }
  ]
}
```

### 4.2 题目相关

```json
// QuestionVO
{
  "id": 42,
  "type": "SingleChoice",
  "context": "What is the past tense of 'go'?",
  "img": 0,
  "answer": {
    "version": 1,
    "correctOption": "B",
    "options": ["A", "B", "C", "D"]
  },
  "use": 10,
  "correct": 7,
  "accuracy": 0.7
}
```

### 4.3 考试相关

```json
// ExamVO（教师视角）
{
  "id": 12,
  "exam": "2026 春季英语期中考试",
  "status": "publish",
  "starttime": "2026-06-15T09:00:00",
  "endtime": "2026-06-15T11:00:00",
  "questionItems": [
    { "questionId": 1, "score": 5, "type": "SingleChoice" },
    { "questionId": 2, "score": 10, "type": "MultipleChoice" }
  ]
}

// ExamForStudentVO（学生视角，答案已脱敏）
{
  "id": 12,
  "exam": "2026 春季英语期中考试",
  "starttime": "2026-06-15T09:00:00",
  "endtime": "2026-06-15T11:00:00",
  "questionItems": [
    {
      "questionId": 1,
      "type": "SingleChoice",
      "context": "What is the past tense of 'go'?",
      "img": 0,
      "options": ["A. go", "B. went", "C. gone", "D. going"],
      "score": 5
    }
  ]
}

// ExamQuestionDraft
{
  "answers": [
    { "questionId": 1, "userAnswer": "B" },
    { "questionId": 2, "userAnswer": ["A", "C"] }
  ]
}
```

### 4.4 分数相关

```json
// ScoreVO
{
  "id": 1,
  "userId": 1001,
  "name": "张三",
  "examId": 12,
  "examName": "2026 春季英语期中考试",
  "totalScore": 85,
  "maxScore": 100,
  "accuracy": 0.85,
  "detail": [
    {
      "questionId": 1,
      "type": "SingleChoice",
      "userAnswer": "A",
      "correctAnswer": "A",
      "score": 5,
      "maxScore": 5,
      "isCorrect": true
    }
  ]
}

// ScoreListVO（列表简版）
{
  "id": 1,
  "examId": 12,
  "examName": "2026 春季英语期中考试",
  "totalScore": 85,
  "maxScore": 100,
  "accuracy": 0.85,
  "submitTime": "2026-06-15T10:30:00"
}

// MistakeItemVO
{
  "questionId": 42,
  "type": "MultipleChoice",
  "context": "Which of the following are correct?",
  "options": ["A", "B", "C", "D"],
  "userAnswer": ["A", "B"],
  "correctAnswer": ["A", "C"],
  "examId": 12,
  "examName": "2026 春季英语期中考试"
}
```

### 4.5 统计相关

```json
// ExamStatisticsVO
{
  "examId": 12,
  "examName": "2026 春季英语期中考试",
  "participantCount": 50,
  "submitCount": 48,
  "passRate": 0.75,
  "averageScore": 72.5,
  "maxScore": 98,
  "minScore": 30,
  "medianScore": 75,
  "scoreDistribution": {
    "0-59": 5,
    "60-69": 10,
    "70-79": 15,
    "80-89": 12,
    "90-100": 6
  }
}

// QuestionStatisticsVO
{
  "questionId": 42,
  "type": "MultipleChoice",
  "use": 50,
  "correct": 25,
  "accuracyRate": 0.5
}
```

### 4.6 分页响应

```json
// PageResult<T>
{
  "content": [ ... ],
  "totalElements": 100,
  "totalPages": 5,
  "page": 0,
  "size": 20
}
```
