# 数据格式整理 - Wiki 一致性校验计划

> 本计划是 [data-format-organization-wiki-refactor-plan.md](file:///d:/GDUT-OOP_20260601/.trae/documents/data-format-organization-wiki-refactor-plan.md) 的**校验收尾阶段**。前序阶段已完成 4 张核心表的重写、模块文档同步、全局规范例外声明等。
>
> 本阶段任务：**对所有 Wiki 文档进行"链接 / 版本号 / 枚举 / 残留字段"四项一致性扫描与修复**，确保文档体系对算法设计与实现具备"开箱即用"的指导力。

---

## 1. 探索结论（当前状态盘点）

通过逐文件读取 `wiki/` 目录 + 4 轮 Grep 扫描（关键词：`username/user_type/userId`、`examId/totalScore/totalQuestions`、`difficulty/options/analysis/creator_id/admin_subtype/display_name`、`SINGLE_CHOICE/.../ARCHIVED`、`question_property/property_ids/questionType/content/status/created_at/updated_at/version`、`orderIndex/durationMinutes/...`），发现以下**需要修复的不一致项**（不含"v1.x → v2.0.0 历史变更说明"中刻意保留的旧字段名 / 旧枚举——这些是历史对照，属于"正确的历史"）：

### 1.1 必须修复的实质性错误

| 编号 | 文件 | 行号 | 问题描述 | 修复方向 |
|---|---|---|---|---|
| E1 | [00-INDEX.md](file:///d:/GDUT-OOP_20260601/wiki/00-INDEX.md) | 109 | "5 张核心表结构、字段、JSON 扩展" | → "**4 张**核心表结构、字段、JSON 扩展" |
| E2 | [00-INDEX.md](file:///d:/GDUT-OOP_20260601/wiki/00-INDEX.md) | 96 | M02 核心职责 "按类型/难度/关键字检索" | → "按类型/关键字检索"（v2.0.0 已删除 difficulty） |
| E3 | [01-Global-Standards.md](file:///d:/GDUT-OOP_20260601/wiki/01-Global-Standards.md) | 189, 197 | DTO/VO 示例 `String username` | → `String name`（与 v2.0.0 一致） |
| E4 | [01-Global-Standards.md](file:///d:/GDUT-OOP_20260601/wiki/01-Global-Standards.md) | 191, 198 | DTO/VO 示例 `UserType userType` | → `UserType type` |
| E5 | [01-Global-Standards.md](file:///d:/GDUT-OOP_20260601/wiki/01-Global-Standards.md) | 220-227 | Repository 示例 `JOIN FETCH e.questions` / `e.creatorId` | → 替换为 v2.0.0 正确示例（按 `examStatus` 查 + `@EntityGraph`） |
| E6 | [M04-Score-Statistics.md](file:///d:/GDUT-OOP_20260601/wiki/modules/M04-Score-Statistics.md) | 201 | `MistakeItemVO` `String content` | → `String context`（v2.0.0 字段名） |
| E7 | [M02-Question-Bank.md](file:///d:/GDUT-OOP_20260601/wiki/modules/M02-Question-Bank.md) | 62-66 | `answer` JSON 示例中 `options` 字段在 `SingleChoice` / `MultipleChoice` 行内重复出现两次 | 表格列名为 `answer` JSON，但内含 `options` 是合法的（v2.0.0 合并入 answer）。**保留现状，不修**。 |
| E8 | [references/SQLite-Optimization.md](file:///d:/GDUT-OOP_20260601/wiki/references/SQLite-Optimization.md) | 74, 135, 136, 178, 257, 283, 284, 295, 302, 309 | 10 处使用旧字段名 / 旧枚举 / 旧表名：`score.user_id`、`answer_statistics`、`@Column(name = "created_at")`、`question_type = 'SINGLE_CHOICE'`、`user.username`、`question.options`（独立字段）、`appear_count` / `correct_count`、`ONGOING` 状态 | 全部替换为 v2.0.0 对应字段 / 表 / 枚举 |
| E9 | [02-Data-Dictionary.md](file:///d:/GDUT-OOP_20260601/wiki/02-Data-Dictionary.md) | 235, 237, 241 | 状态机图中使用大写 `DRAFT`（与下方小写 `draft` 枚举不一致） | → 改为小写 `draft`，与 §5.3 枚举保持一致 |
| E10 | [01-Global-Standards-v2.md](file:///d:/GDUT-OOP_20260601/wiki/01-Global-Standards-v2.md) | 全文 | 临时遗留文件（上次会话 PowerShell 复制覆盖中间产物） | → **删除**该文件 |
| E11 | [wiki/01-Global-Standards-v2.md](file:///d:/GDUT-OOP_20260601/wiki/01-Global-Standards-v2.md) | 206, 207 | Repository 示例中 `e.questions` / `e.creatorId`（与 E5 同源） | 与 E5 一同修复（删除该临时文件后无须单独修） |

### 1.2 链接锚点与版本号检查（通过，无需修复）

| 检查项 | 结果 |
|---|---|
| `01-Global-Standards.md` → `02-Data-Dictionary.md#4-字段级定义` | ✓ 有效（§4 标题为 "字段级定义"） |
| `02-Data-Dictionary.md` → `01-Global-Standards.md#4-jpa-规范` | ✓ 有效 |
| `02-Data-Dictionary.md` → `01-Global-Standards.md#4111-已知例外...` | ⚠ 中文锚点较长，但 GitHub / VS Code 渲染兼容，**保留** |
| `00-INDEX.md` 引用 v1.0.0 / v1.0.1 / v2.0.0 | ✓ 与 [02-Data-Dictionary.md](file:///d:/GDUT-OOP_20260601/wiki/02-Data-Dictionary.md) §12 一致 |
| `02-Data-Dictionary.md` v1.0.0 / v1.1.0 / v2.0.0 三段变更记录 | ✓ 时间线自洽（v1.0.0→v1.1.0→v2.0.0） |
| 模块文档引用 §4.1 / §4.2 / §4.3 / §4.4 / §5.1 / §5.2 / §5.3 | ✓ 全部命中 |
| 业务矩阵 §7.1~§7.5 内部链接 | ✓ 有效 |
| 数据流转 §8.x 内部链接 | ✓ 有效 |

### 1.3 枚举命名一致性检查（通过，无需修复）

| 枚举 | 现态 | 涉及位置 | 判定 |
|---|---|---|---|
| `UserType` | `student` / `teacher` / `admin` | 全部使用小写 | ✓ 一致 |
| `QuestionType` | `SingleChoice` / `MultipleChoice` / `Judge` / `Fill` / `Essay` | CamelCase | ✓ 一致 |
| `ExamStatus` | `draft` / `publish` / `running` / `done` | 小写（除 E9 状态机图） | ✓ 一致（E9 单独修） |
| v1.x 旧枚举提及 | `SINGLE_CHOICE` / `DRAFT` / `ARCHIVED` 等 | 仅出现在 "v1.x→v2.0.0 变更说明" 中 | ✓ 历史对照，保留 |
| v1.x 旧字段提及 | `username` / `difficulty` / `created_at` 等 | 仅出现在 "实施备注" / "历史变更说明" | ✓ 历史对照，保留 |

### 1.4 残留字段扫描（除 E3-E6、E8、E11 外，无额外残留）

| 表 | v2.0.0 字段 | 残留旧字段（除历史备注） |
|---|---|---|
| `user` | `id` / `name` / `password` / `type` / `status` | 无 |
| `question` | `id` / `type` / `context` / `img` / `answer` / `use` / `correct` | 无 |
| `exam` | `id` / `exam` / `status` / `starttime` / `endtime` / `question_sum` | 无 |
| `score` | `id` / `user` / `exam` / `all` / `detail` | 无 |

---

## 2. 实施步骤

按风险从低到高、修复面从大到小排序：

### 2.1 步骤 1：删除临时遗留文件（最低风险）

- 操作：删除 `wiki/01-Global-Standards-v2.md`（上次会话的 PowerShell 中间产物，内容已被 `01-Global-Standards.md` 覆盖）。
- 验证：`ls wiki/` 中无该文件。

### 2.2 步骤 2：修复 [00-INDEX.md](file:///d:/GDUT-OOP_20260601/wiki/00-INDEX.md) 的 2 处

- **E1**：第 109 行 "5 张核心表" → "4 张核心表"。
- **E2**：第 96 行 M02 核心职责 "按类型/难度/关键字检索" → "按类型/关键字检索"。
- 工具：使用 `Edit` 工具精确定位修复（基于行号上下文）。

### 2.3 步骤 3：修复 [01-Global-Standards.md](file:///d:/GDUT-OOP_20260601/wiki/01-Global-Standards.md) DTO/Repository 示例（E3-E5）

- **E3 + E4**（第 189-198 行）：将 `UserCreateReq` / `UserVO` 示例改为：
  ```java
  public record UserCreateReq(
      @NotBlank String name,
      @NotBlank @Size(min = 6, max = 32) String password,
      @NotNull UserType type
  ) {}
  
  public record UserVO(
      Long id,
      String name,
      UserType type,
      Boolean status
  ) {}
  ```
- **E5**（第 220-227 行）：将 Repository 示例改为 v2.0.0 实际可用的 `ExamRepository` 示例（基于 `examStatus` 过滤，避免引用 `e.questions` / `e.creatorId`）：
  ```java
  public interface ExamRepository extends JpaRepository<Exam, Long> {
  
      // v2.0.0 示例：按状态查考试（无外键关联，零 N+1 风险）
      @EntityGraph(attributePaths = {})  // 显式声明：exam 无关联实体
      List<Exam> findByStatus(ExamStatus status);
  
      // 正确：使用 JPQL 显式字段查询（避免懒加载）
      @Query("SELECT e FROM Exam e WHERE e.starttime BETWEEN :from AND :to ORDER BY e.starttime ASC")
      List<Exam> findByTimeWindow(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
  }
  ```

### 2.4 步骤 4：修复 [M04-Score-Statistics.md](file:///d:/GDUT-OOP_20260601/wiki/modules/M04-Score-Statistics.md) MistakeItemVO

- **E6**（第 201 行）：`String content` → `String context`。
- 同步检查文件内其他可能残留的 `getContent()` 调用（第 389 行注释）。注释中的 `getContent → getContext` 是变更说明，可保留；但如代码段内有 `getContent()` 调用应改为 `getContext()`（本次探索未发现，仅注释层级）。

### 2.5 步骤 5：修复 [02-Data-Dictionary.md](file:///d:/GDUT-OOP_20260601/wiki/02-Data-Dictionary.md) 状态机图

- **E9**（第 235、237、241 行）：将状态机 ASCII 图中的 `DRAFT` 统一改为 `draft`，与下方小写枚举对齐。

### 2.6 步骤 6：修复 [references/SQLite-Optimization.md](file:///d:/GDUT-OOP_20260601/wiki/references/SQLite-Optimization.md) 10 处旧字段

| 行号 | 现内容 | 修复 |
|---|---|---|
| 74 | `score.user_id` 指向不存在的 `user.id` | → `score.user`（物理外键名 = `user`） |
| 135 | 写 `score` + 更新 `answer_statistics` | → 写 `score` + 更新 `question.correct`（v2.0.0 字段） |
| 136 | 多次 `UPSERT answer_statistics` | → 多次 `UPDATE question SET use/correct` |
| 178 | `@Column(name = "created_at", ...)` | → `@Column(name = "starttime", ...)` + 改用 `LocalDateTime` 注释（§6.1 ISO 8601 演示） |
| 257 | `question_type = 'SINGLE_CHOICE' AND status = 1` | → `type = 'SingleChoice'`（v2.0.0 无 `status` 字段，question 表无 status） |
| 283 | `NOT NULL constraint failed: user.username` | → `NOT NULL constraint failed: user.name` |
| 284 | `UNIQUE constraint failed: user.username` | → `UNIQUE constraint failed: user.name` |
| 295 | `question.answer` / `question.options` 均为 TEXT | → `question.answer`（含 options 子字段）为 TEXT |
| 302 | `answer_statistics` 表的 `appear_count` / `correct_count` | → `question` 表的 `use` / `correct` 字段 |
| 309 | 系统需识别 `ONGOING` 状态 | → 系统需识别 `running` 状态 |

### 2.7 步骤 7：最终一致性再校验

执行第二轮 Grep 扫描，验证所有 E1-E11 已修复且未引入新不一致：

```bash
# 验证 username / user_type / userId 已无残留（除历史备注）
grep -rn "username\|user_type\|userId" wiki/ --include="*.md" | grep -v "v1.x\|历史\|备注\|user.username"

# 验证 SINGLE_CHOICE / DRAFT / ARCHIVED / ONGOING 等已无残留（除历史备注）
grep -rn "SINGLE_CHOICE\|MULTIPLE_CHOICE\|TRUE_FALSE\|FILL_BLANK\|ESSAY\|ARCHIVED\|DRAFT\|PUBLISHED\|ONGOING\|FINISHED" wiki/ --include="*.md" | grep -v "v1.x\|v1.1.0\|5 态\|历史"

# 验证 5 张表 / answer_statistics 已无残留
grep -rn "5 张核心表\|5 张表\|answer_statistics" wiki/ --include="*.md" | grep -v "v1.x\|删除\|v2.0.0 重大"

# 验证临时文件已删除
ls wiki/01-Global-Standards-v2.md  # 应当 file not found
```

---

## 3. 关键澄清（已通过文档上下文解决）

实施前**无需**再次询问用户：

1. **历史变更说明中的旧字段名 / 旧枚举是否要保留**？
   - **决策**：保留。理由：作为"v1.x → v2.0.0 演进轨迹"的历史说明，新人阅读时能清晰看到"我们删了什么、为什么删"。删除会失去版本演化的可追溯性。
2. **01-Global-Standards-v2.md 是否需要保留**？
   - **决策**：删除。理由：上次会话的 PowerShell 中间产物，内容已被 01-Global-Standards.md 完整覆盖；保留会造成"权威源分裂"。

---

## 4. 验证清单（实施完成后逐项打勾）

- [ ] E1 00-INDEX.md "5 张" → "4 张"
- [ ] E2 00-INDEX.md M02 核心职责删除"难度"
- [ ] E3 01-Global-Standards.md `username` → `name`
- [ ] E4 01-Global-Standards.md `userType` → `type`
- [ ] E5 01-Global-Standards.md Repository 示例改写（去除 `e.questions` / `e.creatorId`）
- [ ] E6 M04-Score-Statistics.md MistakeItemVO `content` → `context`
- [ ] E8 references/SQLite-Optimization.md 10 处旧字段/旧枚举/旧表名替换
- [ ] E9 02-Data-Dictionary.md 状态机图 `DRAFT` → `draft`
- [ ] E10 删除 01-Global-Standards-v2.md
- [ ] 第二轮 Grep 扫描无新增残留

---

## 5. 风险与回滚

| 风险 | 缓解措施 |
|---|---|
| `Edit` 工具因上下文不唯一失败 | 使用更大上下文或 `replace_all`（全局替换已确认唯一的字段名） |
| 修改状态机图破坏 ASCII 对齐 | 替换后逐行检查 ASCII 对齐（4 态简图，对齐要求低） |
| 误改历史变更说明中的旧字段名 | 修复时严格限定行号；如不确定，保留原样 |

**回滚方案**：每个文件的修改通过 `Edit` 工具的 `old_string` 即可反向 `Edit` 复原；如发生灾难性错误，可用 `git checkout wiki/<file>` 还原（git 历史保留）。

---

## 6. 不在本次范围内

- 不修改后端代码（保持"Wiki 先行，代码暂不动"的边界）
- 不修改 SOP.md / temp/ 下历史文件
- 不增加新章节 / 不重写文档结构（仅在现有结构上做小范围修复）
- 不重新评估 §11 开放点 O1-O8（这些是 [02-Data-Dictionary.md](file:///d:/GDUT-OOP_20260601/wiki/02-Data-Dictionary.md) 已记录的"待评审项"，由用户后续决策）
