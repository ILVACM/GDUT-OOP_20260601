# 业务逻辑层系统性完善 Spec

## Why
当前后端数据层（Entity + Enum + Repository）已完成，但业务逻辑层（Service + DTO + 公共基础设施）完全缺失。需要系统性实现所有业务模块的 Service 层和 DTO 层，为后续 Controller 层调用做好准备，确保业务逻辑可独立运行并通过单元测试验证。

## What Changes
- 新增 `common/api/Result<T>` 统一返回类
- 新增 `common/exception/BusinessException` 业务异常类
- 新增 `common/exception/GlobalExceptionHandler` 全局异常处理器
- 新增 M01 模块 DTO（LoginReq、RegisterReq、ChangePasswordReq、UserStatusReq、LoginResp、UserVO）
- 新增 M01 模块 Service（UserService：用户 CRUD、登录校验、密码修改、状态启停）
- 新增 M02 模块 DTO（QuestionCreateReq、QuestionUpdateReq、QuestionQueryReq、QuestionVO、Answer 多态模型）
- 新增 M02 模块 Service（QuestionService：题目 CRUD、批量导入、答案 JSON 校验、题内统计自维护）
- 新增 M03 模块 DTO（ExamCreateManualReq、ExamCreateAutoReq、AutoRule、ExamQuestionItemReq、ExamVO、ExamForStudentVO 等）
- 新增 M03 模块 Service（ExamService：手动/自动组卷、考试状态机、发布/撤回/删除、状态实时判定）
- 新增 M04 模块 DTO（ExamSubmitReq、AnswerItem、EssayGradeReq、ScoreVO、MistakeItemVO、ExamStatisticsVO 等）
- 新增 M04 模块 Service（ScoreService：答题提交与判分、教师评卷、统计报表、错题集）
- 新增各模块 Service 单元测试

## Impact
- Affected specs: M01 用户认证、M02 题库管理、M03 考试与组卷、M04 分数与统计
- Affected code: `backend/src/main/java/com/cps/backend/` 下新增 common、各模块 service/dto 包；`backend/src/test/` 下新增 Service 测试

## ADDED Requirements

### Requirement: 公共基础设施
系统 SHALL 提供统一的 API 响应封装 `Result<T>`、业务异常类 `BusinessException` 和全局异常处理器 `GlobalExceptionHandler`，严格遵循 01-Global-Standards.md §3-4 定义。

#### Scenario: 成功响应
- **WHEN** Service 方法正常返回业务数据
- **THEN** Controller 可通过 `Result.success(data)` 包装为 `{ "code": 200, "message": "success", "data": ... }`

#### Scenario: 业务异常
- **WHEN** Service 层检测到业务规则违反（如用户名重复、考试状态不允许操作）
- **THEN** 抛出 `BusinessException(code, message)`，由全局异常处理器捕获并返回 `Result.error(code, message)`

#### Scenario: 未知异常兜底
- **WHEN** 系统发生未预期异常
- **THEN** 全局异常处理器返回 `Result.error(5000, "系统繁忙，请稍后重试")`，不泄露堆栈信息

### Requirement: M01 用户认证 Service
系统 SHALL 提供 UserService 实现用户注册、登录校验、信息查询、密码修改、状态启停等业务逻辑。

#### Scenario: 用户注册
- **WHEN** 管理员提交注册请求（name + password + type）
- **THEN** 校验 name 唯一性，密码 BCrypt 加密后持久化，status 默认为 1（启用），返回 UserVO（不含 password）

#### Scenario: 用户登录
- **WHEN** 用户提交 name + password
- **THEN** 按 name 查询用户，BCrypt 校验密码，校验 status=1（启用），返回 LoginResp（含 token 和 UserVO）

#### Scenario: 修改密码
- **WHEN** 用户提交旧密码和新密码
- **THEN** 校验旧密码正确，新密码 BCrypt 加密后更新，返回成功

#### Scenario: 用户状态启停
- **WHEN** 管理员修改某用户 status
- **THEN** 更新 status 字段；若禁用的是最后一个 admin 则拒绝（4103）

#### Scenario: 删除用户
- **WHEN** 管理员删除某用户
- **THEN** 检查该用户是否有关联未结束考试/未批改答卷；如有则禁用而非删除；不可自删除

### Requirement: M02 题库管理 Service
系统 SHALL 提供 QuestionService 实现题目 CRUD、批量导入、答案 JSON 多态校验、题内统计自维护等业务逻辑。

#### Scenario: 创建题目
- **WHEN** 教师提交题目（type + context + img + answer JSON）
- **THEN** 按 type 校验 answer JSON 结构匹配，use/correct 初始化为 0，持久化后返回 QuestionVO

#### Scenario: 批量导入
- **WHEN** 教师提交题目数组（不超过 100 题）
- **THEN** 逐题校验 answer JSON 结构，批量持久化，返回创建的题目列表

#### Scenario: 答案 JSON 校验
- **WHEN** 创建或更新题目时
- **THEN** 根据 QuestionType 校验 answer JSON 结构：SingleChoice 需含 correctOption + options；MultipleChoice 需含 correctOptions + options；Judge 需含 correct；Fill 需含 blanks；Essay 需含 reference

#### Scenario: 题内统计自维护
- **WHEN** 组卷时题目被纳入 exam.question_sum.items
- **THEN** 在同一事务内执行 question.use += 1

### Requirement: M03 考试与组卷 Service
系统 SHALL 提供 ExamService 实现手动组卷、自动组卷、考试状态机管理、发布/撤回/删除等业务逻辑。

#### Scenario: 手动组卷
- **WHEN** 教师提交手动组卷请求（exam + starttime + endtime + items[]）
- **THEN** 校验所有 questionId 存在，构造 question_sum JSON 快照，status 初始化为 draft，事务内同时为每题 use += 1

#### Scenario: 自动组卷
- **WHEN** 教师提交自动组卷请求（exam + starttime + endtime + autoRule）
- **THEN** 按 typeFilter 过滤候选集，校验候选数 >= totalQuestions，按 usePenalty 加权随机或完全随机抽题，等分 + 余数分摊计算每题分值，落库为 draft 状态

#### Scenario: 发布考试
- **WHEN** 教师发布 draft 状态考试
- **THEN** status 从 draft 变为 publish

#### Scenario: 撤回考试
- **WHEN** 教师撤回 publish 状态考试
- **THEN** status 从 publish 变为 draft

#### Scenario: 删除考试
- **WHEN** 教师删除 draft 状态考试
- **THEN** 物理删除；非 draft 状态拒绝删除（4303）

#### Scenario: 状态实时判定
- **WHEN** 查询考试信息时
- **THEN** 对 publish/running 状态按时间窗实时计算：starttime <= now < endtime 为 running，now >= endtime 为 done

#### Scenario: 学生获取考试题目
- **WHEN** 学生请求考试预览
- **THEN** 按 questionId 从 question 表读取题目内容，剔除 answer 中的判分关键字段后返回

### Requirement: M04 分数与统计 Service
系统 SHALL 提供 ScoreService 实现答题提交与自动判分、教师评卷、分数查询、统计报表、错题集等业务逻辑。

#### Scenario: 提交答卷
- **WHEN** 学生提交答卷（examId + answers[]）
- **THEN** 校验考试 running 状态、校验未重复提交、解析 question_sum、批量加载 question（防 N+1）、逐题判分、事务内持久化 score + 更新 question.correct

#### Scenario: 客观题自动判分
- **WHEN** 判分 SingleChoice/MultipleChoice/Judge/Fill 题型
- **THEN** 按题型规则比对 userAnswer 与 correctAnswer，正确得满分否则 0 分

#### Scenario: 简答题待评卷
- **WHEN** 判分 Essay 题型
- **THEN** 初始 score=0，isCorrect=null，待教师评卷

#### Scenario: 教师评卷
- **WHEN** 教师对 Essay 题打分
- **THEN** 更新 score.detail 中对应题的 score/isCorrect，重算 score.all，若 isCorrect 从 null/true 变为 true 则 question.correct += 1

#### Scenario: 统计报表
- **WHEN** 教师/管理员请求考试统计
- **THEN** 返回 ExamStatisticsVO（参与人数、提交人数、通过率、平均分、最高/最低/中位数、分数分布）

#### Scenario: 错题集
- **WHEN** 学生请求错题集
- **THEN** 从 score.detail 筛选 isCorrect=false 的题，去重后返回 MistakeItemVO 列表

### Requirement: DTO 隔离与转换
系统 SHALL 确保所有 Entity 与 DTO 完全隔离，Service 层负责 Entity ↔ DTO 转换，禁止 Entity 直接作为 API 响应。

#### Scenario: Entity → VO 转换
- **WHEN** Service 方法返回数据给 Controller
- **THEN** 必须将 Entity 转换为 VO（不含 password 等敏感字段，JSON 字段反序列化为强类型结构）

#### Scenario: Req → Entity 转换
- **WHEN** Service 方法接收请求创建/更新实体
- **THEN** 将请求 DTO 转换为 Entity 后持久化

### Requirement: 事务控制
系统 SHALL 确保所有写操作在 Service 层加 `@Transactional`，涉及多表写入的复杂业务指定 `rollbackFor = Exception.class`。

#### Scenario: 组卷事务
- **WHEN** 手动/自动组卷
- **THEN** INSERT exam + UPDATE question.use 在同一事务内，任一失败全部回滚

#### Scenario: 判分事务
- **WHEN** 提交答卷
- **THEN** INSERT/UPSERT score + UPDATE question.correct 在同一事务内

### Requirement: 业务码规范
系统 SHALL 严格遵循 01-Global-Standards.md §3.3 定义的业务码区间。

#### Scenario: 业务异常码
- **THEN** 4000-4099 通用参数错误、4100-4199 用户认证、4200-4299 题库、4300-4399 考试、4400-4499 分数、5000-5099 系统异常

### Requirement: 单元测试
系统 SHALL 为每个 Service 提供单元测试，覆盖核心业务逻辑路径。

#### Scenario: Service 测试覆盖
- **THEN** 每个模块的 Service 测试覆盖正常路径 + 关键异常路径（如重复提交、状态不允许操作、候选题不足等）
