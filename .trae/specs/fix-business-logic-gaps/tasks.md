# Tasks

- [x] Task 1: M03 考试编辑功能（🔴 高优先级 — 阻塞前端开发）
  - [x] SubTask 1.1: 在 `QuestionRepository.java` 中新增 `decrementUse(@Param("id") Integer id)` JPQL 方法 — `@Modifying @Query("UPDATE Question q SET q.use = q.use - 1 WHERE q.id = :id AND q.use > 0")`
  - [x] SubTask 1.2: 在 `QuestionService.java` 中新增 `decrementUse(Integer questionId)` 方法 — 调用 Repository 的 decrementUse，updated=0 时抛出 BusinessException(4201)
  - [x] SubTask 1.3: 在 `ExamService.java` 中新增 `edit(Integer id, ExamCreateManualReq req)` 方法 — 仅 draft 可编辑 + 时间窗校验 + questionId 存在校验 + 重建 question_sum 快照 + 旧题 decrementUse/新题 incrementUse + 更新 exam 记录
  - [x] SubTask 1.4: 在 `ExamServiceTest.java` 中新增测试 — 覆盖编辑成功/draft以外状态拒绝/questionId不存在/use统计增减/时间校验失败

- [x] Task 2: M01 删除用户级联检查（🟡 中优先级）
  - [x] SubTask 2.1: 修改 `UserService.java` — 注入 ScoreRepository，在 delete() 方法中删除前检查 `findByUser(userId)` 是否有记录，有则禁用(status=0)而非删除，抛出 BusinessException(4103, "该用户存在考试记录，已禁用而非删除")
  - [x] SubTask 2.2: 修改 `UserServiceTest.java` — 新增测试：删除有分数记录的用户时禁用而非删除

- [x] Task 3: M01 注册角色限制（🟡 中优先级）
  - [x] SubTask 3.1: 修改 `UserService.java` 的 register() 方法 — 在 name 唯一校验后增加 type 校验：仅允许 student，teacher/admin 抛出 BusinessException(4103, "仅支持学生自助注册，教师/管理员请由管理员创建")
  - [x] SubTask 3.2: 修改 `UserServiceTest.java` — 新增测试：注册 teacher/admin 角色时拒绝

- [x] Task 4: M04 ScoreVO 补全 userName（🟢 低优先级）
  - [x] SubTask 4.1: 修改 `ScoreService.java` — 注入 UserRepository，在 toScoreVO() 中通过 `userRepository.findById(score.getUser())` 查询用户名填充 userName（用户不存在时为 null）
  - [x] SubTask 4.2: 修改 `ScoreServiceTest.java` — 验证 ScoreVO 中 userName 正确填充

- [x] Task 5: M02 删除题目引用检查（🟢 低优先级）
  - [x] SubTask 5.1: 修改 `QuestionService.java` 的 delete() 方法 — 删除前查询所有 exam 记录，解析 question_sum JSON 检查是否包含该 questionId，如有则在删除后日志记录警告（不阻止删除，因快照机制保证历史考试不受影响）
  - [x] SubTask 5.2: 修改 `QuestionServiceTest.java` — 新增测试：删除被考试引用的题目时仍可删除但有警告日志

- [x] Task 6: 全量测试验证
  - [x] SubTask 6.1: 运行 `mvn test` 确保所有测试（含新增）全部通过

# Task Dependencies
- [Task 1] SubTask 1.1 → SubTask 1.2 → SubTask 1.3 → SubTask 1.4（顺序依赖）
- [Task 2] 独立，可与 Task 1 并行
- [Task 3] 独立，可与 Task 1/2 并行
- [Task 4] 独立，可与 Task 1/2/3 并行
- [Task 5] 独立，可与 Task 1/2/3/4 并行
- [Task 6] depends on [Task 1, 2, 3, 4, 5] — 所有修改完成后执行全量测试
