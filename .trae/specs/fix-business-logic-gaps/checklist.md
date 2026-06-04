# 后端业务逻辑层审查修复验证清单

## M03 考试编辑功能
- [x] `QuestionRepository.java` 新增 `decrementUse()` JPQL 方法，含 `@Modifying` + `WHERE q.use > 0` 防护
- [x] `QuestionService.java` 新增 `decrementUse(Integer)` 方法，调用 Repository + updated=0 异常处理
- [x] `ExamService.java` 新增 `edit(Integer, ExamCreateManualReq)` 方法
- [x] edit() 仅 draft 状态可编辑，非 draft 抛出 BusinessException(4303)
- [x] edit() 校验时间窗（endtime > starttime，ISO 8601 格式）
- [x] edit() 校验所有 questionId 存在
- [x] edit() 重建 question_sum 快照（新 items + totalQuestions + totalScore）
- [x] edit() 正确处理 use 统计增减：移除的旧题 decrementUse、新增的题 incrementUse、保留的题不变
- [x] edit() 标注 `@Transactional(rollbackFor = Exception.class)`
- [x] ExamServiceTest 新增测试覆盖编辑成功/状态拒绝/questionId不存在/use统计增减/时间校验

## M01 删除用户级联检查
- [x] `UserService.java` 注入 ScoreRepository
- [x] delete() 方法删除前检查 `scoreRepository.findByUser(userId)` 是否有记录
- [x] 有分数记录时：设 status=0 禁用 + 抛出 BusinessException(4103, "该用户存在考试记录，已禁用而非删除")
- [x] 无分数记录时：正常删除
- [x] 移除原有 TODO 注释
- [x] UserServiceTest 新增测试：删除有分数记录的用户时禁用而非删除

## M01 注册角色限制
- [x] `UserService.register()` 在 name 唯一校验后增加 type 校验
- [x] 仅允许 type=student，teacher/admin 抛出 BusinessException(4103)
- [x] UserServiceTest 新增测试：注册 teacher/admin 角色时拒绝

## M04 ScoreVO 补全 userName
- [x] `ScoreService.java` 注入 UserRepository
- [x] toScoreVO() 中通过 `userRepository.findById(score.getUser())` 查询用户名
- [x] 用户存在时 userName 填充为 user.name，不存在时为 null（不抛异常）
- [x] 移除原有 `// userName 需跨模块查 user 表，Controller 层补充` 注释
- [x] ScoreServiceTest 验证 ScoreVO 中 userName 正确填充

## M02 删除题目引用检查
- [x] `QuestionService.delete()` 删除前查询 exam 表检查 question_sum JSON 引用
- [x] 被引用时仍执行删除（快照机制保证），但记录警告日志
- [x] QuestionServiceTest 新增测试：删除被引用的题目时仍可删除

## 全量测试
- [x] `mvn test` 所有测试通过（含原有 73 个 + 新增测试）
