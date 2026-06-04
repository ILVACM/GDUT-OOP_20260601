# Tasks

- [x] Task 1: 创建公共基础设施（common 包）
  - [x] SubTask 1.1: 创建 `common/api/Result.java` — 统一 API 响应包装器，遵循 01-Global-Standards.md §3.1
  - [x] SubTask 1.2: 创建 `common/exception/BusinessException.java` — 业务异常类，含 code + message，遵循 01-Global-Standards.md §4.2
  - [x] SubTask 1.3: 创建 `common/exception/GlobalExceptionHandler.java` — 全局异常处理器，处理 BusinessException / IllegalArgumentException / 兜底 Exception，遵循 01-Global-Standards.md §4.1

- [x] Task 2: 实现 M01 用户认证模块 DTO + Service
  - [x] SubTask 2.1: 创建 M01 DTO — LoginReq、RegisterReq、ChangePasswordReq、UserStatusReq、LoginResp、UserVO（Java 21 Record），遵循 M01-User-Auth.md §3
  - [x] SubTask 2.2: 创建 `UserService.java` — 实现用户注册（BCrypt 加密 + name 唯一校验）、登录校验（BCrypt 比对 + status 校验）、用户查询（按 id / 分页列表）、密码修改、状态启停（admin 保护）、删除用户（级联检查 + 自删除保护），遵循 M01-User-Auth.md §4/§6/§7
  - [x] SubTask 2.3: 创建 `UserServiceTest.java` — 覆盖注册成功/用户名重复、登录成功/密码错误/用户禁用、密码修改成功/旧密码错误、状态启停/最后一个 admin 保护、删除/自删除保护/级联禁用

- [x] Task 3: 实现 M02 题库管理模块 DTO + Service
  - [x] SubTask 3.1: 创建 M02 DTO — QuestionCreateReq、QuestionUpdateReq、QuestionQueryReq、QuestionVO，以及答案多态模型（Answer sealed interface + SingleChoiceAnswer/MultipleChoiceAnswer/JudgeAnswer/FillAnswer/EssayAnswer record），遵循 M02-Question-Bank.md §6
  - [x] SubTask 3.2: 创建 `QuestionService.java` — 实现题目创建（answer JSON 按 type 校验）、批量导入（≤100 题）、按 id 查询、分页筛选（type + keyword）、更新题目、删除题目、题内统计自维护（incrementUse/incrementCorrect），遵循 M02-Question-Bank.md §7/§8/§9
  - [ ] SubTask 3.3: 创建 `QuestionServiceTest.java` — 覆盖创建成功/answer JSON 校验失败、批量导入成功/超限、查询分页、更新/删除、题内统计自维护

- [x] Task 4: 实现 M03 考试与组卷模块 DTO + Service
  - [x] SubTask 4.1: 创建 M03 DTO — ExamCreateManualReq、ExamQuestionItemReq、ExamCreateAutoReq、AutoRule、ExamVO、ExamQuestionVO、ExamForStudentVO、ExamQuestionForStudentVO，以及 QuestionSum/QuestionSumItem JSON 模型，遵循 M03-Exam-Assembly.md §5
  - [x] SubTask 4.2: 创建 `ExamService.java` — 实现手动组卷（校验 questionId 存在 + 构造 question_sum 快照 + 事务内 use += 1）、自动组卷（typeFilter 过滤 + usePenalty 加权随机 + 等分余数分摊）、考试详情查询、学生视角预览（剔除 answer 判分关键字段）、发布（draft→publish）、撤回（publish→draft）、删除（仅 draft）、状态实时判定（resolveCurrentStatus）、考试列表查询，遵循 M03-Exam-Assembly.md §3/§6/§7/§8
  - [ ] SubTask 4.3: 创建 `ExamServiceTest.java` — 覆盖手动组卷成功/questionId 不存在、自动组卷成功/候选不足/typeFilter 过滤/usePenalty 降权、发布/撤回/删除状态校验、状态实时判定、学生预览答案脱敏

- [x] Task 5: 实现 M04 分数与统计模块 DTO + Service
  - [x] SubTask 5.1: 创建 M04 DTO — ExamSubmitReq、AnswerItem、EssayGradeReq、ScoreVO、DetailItemVO、MistakeItemVO、ExamStatisticsVO、QuestionStatisticsVO、UserExamHistoryVO，以及 ScoreDetail/DetailItem JSON 模型，遵循 M04-Score-Statistics.md §5
  - [x] SubTask 5.2: 创建 `ScoreService.java` — 实现答题提交与判分（状态校验 + 重复提交校验 + 批量加载 question 防 N+1 + 逐题判分 + 事务内持久化 score + 更新 question.correct）、教师评卷（更新 detail + 重算 all + 更新 question.correct）、分数查询（个人/考试/详情）、错题集（去重）、统计报表（考试统计 + 题目统计），遵循 M04-Score-Statistics.md §3/§4/§6/§7/§8/§9
  - [x] SubTask 5.3: 创建 `ScoreServiceTest.java` — 覆盖提交答卷成功/考试非 running/重复提交、客观题判分（各题型）、Essay 待评卷、教师评卷/更新 correct 统计、错题集去重、统计报表计算

- [x] Task 6: 补充 pom.xml 依赖（如需）
  - [x] SubTask 6.1: 确认并添加 BCrypt（spring-security-crypto）、Jackson（已含于 spring-boot-starter-webmvc）、Validation（spring-boot-starter-validation）等必要依赖

# Task Dependencies
- [Task 2] depends on [Task 1] — UserService 需要 Result、BusinessException
- [Task 3] depends on [Task 1] — QuestionService 需要 Result、BusinessException
- [Task 4] depends on [Task 1, Task 3] — ExamService 依赖 QuestionService（incrementUse）和公共基础设施
- [Task 5] depends on [Task 1, Task 3, Task 4] — ScoreService 依赖 QuestionService（incrementCorrect）、ExamService（状态判定）和公共基础设施
- [Task 6] 可与 [Task 1] 并行 — 依赖补充独立于代码编写
