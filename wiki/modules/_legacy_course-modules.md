# 旧模块归档说明（历史追溯）

> 本文件是项目 wiki 体系演进的**唯一历史追溯文档**。所有被废弃的旧模块均在此处集中记录，确保任何外部引用都能找到对应的新模块映射。
>
> **状态**：归档（read-only）。不再向本文件追加新内容。

---

## 1. 废弃背景

项目 wiki 体系在 **v1.0.0（2026-06-03）** 进行了一次**主题重塑**：从最初基于"课程（Course）"的泛化教育平台模型，重组为基于"**题库（Question Bank）/ 考试（Exam）**"的英语专项学习系统模型。

废弃原因：

1. **业务领域错配**：原 M02-M04 模块（课程管理 / 选课 / 课程成绩）描述的是"MOOC 平台"业务流，与实际项目"英语在线学习与考试系统"差异巨大
2. **需求确认变更**：基于 `temp/temp.txt` 草案确认本项目核心业务为"题库—组卷—考试—统计"，而非"课程—选课—成绩"
3. **AI 协作效率**：保留主题错配的模块会导致 AI 在生成代码时产生概念混淆（如将"考试"与"课程"等价处理）

---

## 2. 旧模块与新模块映射

| 旧模块 | 文件 | 处置 | 对应新模块 | 业务概念映射 |
|---|---|---|---|---|
| M01-User-Auth | M01-User-Auth.md | **保留并重写** | [M01-User-Auth.md](file:///d:/GDUT-OOP_20260601/wiki/modules/M01-User-Auth.md) | 用户认证主题一致，保留文件，重写以适配"管理员二级分类" |
| M02-Course-Management | M02-Course-Management.md | **废弃** | [M02-Question-Bank.md](file:///d:/GDUT-OOP_20260601/wiki/modules/M02-Question-Bank.md) | "课程" → "题库" |
| M03-Enrollment | M03-Enrollment.md | **废弃** | [M03-Exam-Assembly.md](file:///d:/GDUT-OOP_20260601/wiki/modules/M03-Exam-Assembly.md) | "选课" → "参加考试"（组卷） |
| M04-Grade-Report | M04-Grade-Report.md | **废弃** | [M04-Score-Statistics.md](file:///d:/GDUT-OOP_20260601/wiki/modules/M04-Score-Statistics.md) | "课程成绩" → "考试分数 + 答题统计" |

---

## 3. 旧模块内容快照

### 3.1 原 M01-User-Auth.md（旧版，仅标题占位）

```markdown
# M01 - 用户认证与权限管理模块
> 包含用户注册、登录、JWT Token 生成与校验、角色权限拦截等业务场景描述、API 接口定义及关键 DTO 设计。
```

**新版差异**：
- 新增"管理员二级分类"（系统管理员 / 普通管理员）
- 新增"用户启用/禁用"管理接口
- 新增"修改密码"接口
- 完善 DTO / VO 定义
- 详细文档见 [M01-User-Auth.md](file:///d:/GDUT-OOP_20260601/wiki/modules/M01-User-Auth.md)

### 3.2 原 M02-Course-Management.md（已废弃）

```markdown
# M02 - 课程管理模块
> 包含课程的 CRUD、上下架状态流转、富文本大纲管理等业务场景描述、API 接口定义及关键 DTO 设计。
```

**业务不适用原因**：本项目无"课程"实体；教学内容单元是"题目"和"考试"，非"课程"。

### 3.3 原 M03-Enrollment.md（已废弃）

```markdown
# M03 - 选课与退课核心业务模块
> 包含学生选课/退课逻辑、课程容量校验、并发超卖控制（乐观锁/原子更新）等核心业务场景、API 接口及事务注意事项。
```

**业务不适用原因**：本项目无"选课"概念；学生通过"参加考试"参与学习。

**可借鉴的设计模式**：原 M03 提到的乐观锁/原子更新思路，在 [M03-Exam-Assembly.md](file:///d:/GDUT-OOP_20260601/wiki/modules/M03-Exam-Assembly.md) §3.2 自动组卷抽题场景仍适用（防重复抽题）。

### 3.4 原 M04-Grade-Report.md（已废弃）

```markdown
# M04 - 成绩管理与统计报表模块
> 包含教师录入成绩、学生成绩查询、班级/课程成绩统计分析等业务场景描述、API 接口定义及复杂查询优化方案。
```

**业务不适用原因**：本项目无"班级"概念；成绩是按"考试"组织而非"课程"。

**可借鉴的设计模式**：原 M04 提到的复杂查询优化方案，在 [M04-Score-Statistics.md](file:///d:/GDUT-OOP_20260601/wiki/modules/M04-Score-Statistics.md) §9 复杂查询优化中继承。

---

## 4. 演进时间线

| 日期 | 事件 | 处置 |
|---|---|---|
| 2026-06-03 之前 | wiki 仅有 3 个全局基座 + 4 个空模块（M01-M04），主题为"课程" | 全部为占位文件，无实质内容 |
| 2026-06-03 | 由 `temp/temp.txt` 项目需求草案整合为 wiki 正式体系 | M01 重写；M02/M03/M04 物理删除；新建 M02-Question-Bank / M03-Exam-Assembly / M04-Score-Statistics；创建本归档说明 |

---

## 5. 引用本归档文档的入口

- [00-INDEX.md §5](file:///d:/GDUT-OOP_20260601/wiki/00-INDEX.md#5-模块索引) —— 模块索引中"历史模块说明"段落
- [00-INDEX.md §8](file:///d:/GDUT-OOP_20260601/wiki/00-INDEX.md#8-版本与变更记录) —— v1.0.0 变更说明

---

## 6. 不再更新声明

本文件是历史档案，**不再追加新内容**。如未来再有模块重塑，请新建 `_legacy_v2-xxx-modules.md` 等命名区分。
