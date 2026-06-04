# 后端业务逻辑层审查修复与优化 Spec

## Why
审查报告 `.trae/documents/后端业务逻辑层支撑能力审查报告.md` 发现 M03 考试模块编辑功能完全缺失（阻塞前端开发），M01 删除级联检查未实现（数据完整性风险），以及多项中等/低优先级瑕疵。需在 Controller 层开发前修复所有业务逻辑缺口，确保 Service 层功能完整闭环。

## What Changes
- **M03 考试编辑**：新增 `ExamService.edit()` 方法，支持仅 draft 状态可编辑，含 question_sum 快照重建与 use 统计增减
- **M02 decrementUse**：新增 `QuestionService.decrementUse()` + `QuestionRepository.decrementUse()` JPQL，支撑考试编辑时旧题目的 use 回退
- **M01 删除级联检查**：`UserService.delete()` 中注入 ScoreRepository，删除前检查关联分数记录，有则禁用而非删除
- **M01 注册角色限制**：`UserService.register()` 限制自助注册仅 student 角色，teacher/admin 由 admin 通过 `update()` 创建
- **M04 ScoreVO 补全 userName**：ScoreService 注入 UserRepository，在 toScoreVO 中补全 userName 字段
- **M02 删除引用提示**：`QuestionService.delete()` 删除前检查题目是否被考试引用（question_sum JSON 中存在 questionId），给出警告信息

## Impact
- Affected specs: `implement-business-logic-layer`（前置 spec，已完成）
- Affected code:
  - `ExamService.java` — 新增 edit() 方法
  - `QuestionService.java` — 新增 decrementUse() 方法
  - `QuestionRepository.java` — 新增 decrementUse() JPQL
  - `UserService.java` — 修改 delete() 级联检查、修改 register() 角色限制
  - `ScoreService.java` — 注入 UserRepository、修改 toScoreVO() 补全 userName
  - `QuestionService.java` — 修改 delete() 增加引用检查
  - 对应测试类需同步更新

## ADDED Requirements

### Requirement: M03 考试编辑功能
系统 SHALL 提供 `ExamService.edit(Integer id, ExamCreateManualReq req)` 方法，允许教师编辑草稿状态的考试。

#### Scenario: 成功编辑草稿考试
- **WHEN** 教师调用 edit() 修改 draft 状态考试的信息（名称、时间、题目列表）
- **THEN** 系统校验状态为 draft、时间窗合法、所有 questionId 存在后，重建 question_sum 快照，处理 use 统计增减（移除的旧题 decrementUse、新增的题 incrementUse），更新 exam 记录并返回 ExamVO

#### Scenario: 非草稿状态编辑拒绝
- **WHEN** 教师尝试编辑 publish/running/done 状态的考试
- **THEN** 抛出 BusinessException(4303, "仅草稿状态可编辑")

#### Scenario: 编辑时 use 统计正确增减
- **WHEN** 编辑考试移除题目 A、新增题目 B
- **THEN** 题目 A 的 use -= 1，题目 B 的 use += 1；保留不变的题目 use 不变

### Requirement: M02 decrementUse 统计回退
系统 SHALL 提供 `QuestionService.decrementUse(Integer questionId)` 方法，在考试编辑移除题目时回退 use 计数。

#### Scenario: 成功回退 use
- **WHEN** 调用 decrementUse 且题目存在且 use > 0
- **THEN** 题目 use -= 1

#### Scenario: use 已为 0 时回退
- **WHEN** 调用 decrementUse 且题目 use = 0
- **THEN** SQL WHERE 条件 `q.use > 0` 不匹配，返回 updated=0，抛出 BusinessException

### Requirement: M01 删除用户级联检查
系统 SHALL 在删除用户前检查该用户是否有关联的分数记录。

#### Scenario: 用户无关联分数记录
- **WHEN** 删除用户且该用户无 score 记录
- **THEN** 正常删除

#### Scenario: 用户有关联分数记录
- **WHEN** 删除用户且该用户有 score 记录
- **THEN** 不执行删除，改为将用户 status 设为 0（禁用），抛出 BusinessException(4103, "该用户存在考试记录，已禁用而非删除")

### Requirement: M01 注册角色限制
系统 SHALL 限制 register() 方法仅允许注册 student 角色。

#### Scenario: 学生自助注册
- **WHEN** 调用 register() 且 type=student
- **THEN** 正常注册

#### Scenario: 非学生角色注册拒绝
- **WHEN** 调用 register() 且 type=teacher 或 admin
- **THEN** 抛出 BusinessException(4103, "仅支持学生自助注册，教师/管理员请由管理员创建")

### Requirement: M04 ScoreVO 补全 userName
系统 SHALL 在 ScoreService 的 toScoreVO() 方法中通过 UserRepository 补全 userName 字段。

#### Scenario: 分数记录关联的用户存在
- **WHEN** toScoreVO() 查询 score.user 对应的 User
- **THEN** ScoreVO.userName 填充为 user.name

#### Scenario: 分数记录关联的用户已被删除
- **WHEN** toScoreVO() 查询 score.user 对应的 User 不存在
- **THEN** ScoreVO.userName 为 null（不抛异常）

### Requirement: M02 删除题目引用检查
系统 SHALL 在删除题目前检查该题目是否被任何考试的 question_sum JSON 引用。

#### Scenario: 题目未被任何考试引用
- **WHEN** 删除题目且无考试引用
- **THEN** 正常删除

#### Scenario: 题目已被考试引用
- **WHEN** 删除题目且有考试引用（question_sum JSON 中包含该 questionId）
- **THEN** 仍执行删除（快照机制保证历史考试不受影响），但在返回信息中包含警告提示

## MODIFIED Requirements

### Requirement: M01 UserService.delete() 级联处理
原实现直接删除用户，现修改为：删除前检查 ScoreRepository.findByUser(userId)，如有记录则禁用而非删除。移除原有的 TODO 注释。

### Requirement: M01 UserService.register() 角色校验
原实现允许注册任意角色，现修改为：仅允许 type=student，其他角色抛出 BusinessException。

### Requirement: M04 ScoreService.toScoreVO() userName 填充
原实现 userName 字段为 null 并注释"Controller 层补充"，现修改为：注入 UserRepository，在 toScoreVO 中查询并填充。

## REMOVED Requirements
无移除项。
