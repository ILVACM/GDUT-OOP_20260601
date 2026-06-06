> 本文档为《面向对象软件设计与建模》课程期末实验指导书归档，作为本项目需求与设计的原始依据。
>
> 原文档：`面向对象软件设计与建模实验指导书2026.doc`（旧版 Word 二进制格式，无法直接解析为 Markdown）
>
> 本归档版本基于 wiki 项目文档反向重构而成，保留了指导书中的核心业务需求、技术要求和设计约束，确保项目知识库的完整性与可追溯性。

---

# 《面向对象软件设计与建模》期末实验指导书

## 一、实验目的

1. 掌握面向对象分析与设计（OOAD）的基本方法和 UML 建模技术
2. 能够使用 Spring Boot + Vue 3 全栈技术完成一个完整的信息系统开发
3. 实践前后端分离架构、RESTful API 设计、数据库建模等工程技能
4. 培养团队协作、文档编写和项目管理能力

---

## 二、实验题目

**在线学习系统**（Online Learning System）

以英语在线学习系统为载体，贯穿"题库—组卷—考试—统计"完整教学流程，实现一个支撑教师维护题目、灵活组卷、组织考试、记录成绩、生成统计报表的全流程教学活动的 Web 应用系统。

---

## 三、需求背景

在英语教学中，教师需要：
- 维护题目库（含单选、多选、判断、填空、简答五种题型）
- 灵活组卷（手动挑题或按规则自动抽题）
- 组织在线考试（发布时间窗、状态机管理）
- 记录成绩与生成统计报表

学生需要：
- 在线答题与查看分数
- 个性化错题分析

---

## 四、功能要求

### 4.1 用户认证与权限管理（M01）

| 功能 | 说明 |
|------|------|
| 用户注册 | 学生自助注册账号 |
| 用户登录 | 用户名密码登录，JWT Token 认证 |
| 角色管理 | 三类角色：student（学生）、teacher（老师）、admin（管理员） |
| 用户 CRUD | 管理员可创建、查询、更新、删除用户 |
| 状态启停 | 管理员可启用/禁用用户账号 |
| 批量删除 | 支持批量删除/禁用用户 |
| 密码修改 | 用户可修改自身密码 |

**角色权限边界**：
- `admin`：拥有所有用户管理权限
- `teacher`：仅可查询/更新自身账号
- `student`：仅可查询自身账号 + 修改自身密码

### 4.2 题库管理（M02）

| 功能 | 说明 |
|------|------|
| 题目创建 | 录入单题，支持 5 种题型（SingleChoice / MultipleChoice / Judge / Fill / Essay） |
| 题目批量导入 | 通过 JSON 数组批量上传题目 |
| 题目查询 | 按 id 查询、分页/筛选（按 type / 关键字 / use 排序） |
| 题目更新 | 修改题干和答案 |
| 题目删除 | 硬删除（警告：已被组卷的题目删除会破坏快照引用） |
| 批量删除 | 批量删除题目 |
| 随机获取题目 | 自动组卷用，支持 type 过滤和排除 |

**答案 JSON 扩展规范**：
- 题目答案以 JSON 文本存储在 `answer` 字段中
- 不同题型对应不同的 JSON 结构（options、correctOption、blanks、reference 等）
- 每个 JSON 文档必须包含 `version` 字段

**图片标识**：
- `img` 字段为 Integer 类型（0/1），标识题目是否带图片
- 带图题目在 `.\Data\img\` 目录下按题目 id 数字匹配同名图片

### 4.3 考试与组卷（M03）

#### 4.3.1 手动组卷
- 教师从题库逐题挑选
- 指定考试名称、时间窗、每题分值
- 组装为草稿状态（status=draft）的考试

#### 4.3.2 自动组卷
- 指定题目数量和总分
- 支持题型过滤（typeFilter）
- 支持 use 频次降权（usePenalty），避免抽中热点题
- 使用随机抽签策略
- 落库为草稿状态（status=draft）

#### 4.3.3 考试管理

| 功能 | 说明 |
|------|------|
| 发布考试 | draft → publish |
| 撤回考试 | publish → draft |
| 删除考试 | 仅 draft 状态可删 |
| 考试列表 | 按 status / 时间筛选 |
| 考试详情 | 读取 exam + question_sum 快照 |

#### 4.3.4 考试状态机（4 态）

```
       发布             自动：starttime ≤ now < endtime
draft ──────→ publish ─────────────────────────────→ running
   │              │                                     │
   │ 删除（仅 draft） 撤回（仅 publish）                         │ 自动：now ≥ endtime
   │              ↓                                     ↓
   │           draft                                   done
   │
   └──→ 删除（仅 draft）
```

| 状态 | 含义 | 可执行操作 |
|------|------|-----------|
| `draft` | 草稿，未发布 | 编辑、删除、发布 |
| `publish` | 已发布，等待开考 | 撤回为 draft |
| `running` | 进行中 | 仅查看、提交答卷 |
| `done` | 已结束 | 仅查看、统计 |

### 4.4 分数与统计（M04）

#### 4.4.1 答题提交与判分
- 学生提交答卷，系统自动判分（单选/多选/判断/填空）
- 简答题（Essay）需教师评卷
- 一人一考仅一条成绩记录（UPSERT 语义）
- 成绩明细以 JSON 格式存储，包含每题得分、正确率统计

#### 4.4.2 统计报表

| 报表 | 数据源 | 计算方式 |
|------|--------|----------|
| 考试通过率 | score 表 | COUNT(WHERE all >= passScore) / COUNT(*) per exam |
| 成绩分布 | score 表 | 按分数段分桶聚合 |
| 个人错题集 | score.detail.items[] | 筛选 isCorrect = false 的题目 |
| 题目质量分析 | question.use / question.correct | accuracy = correct / use，识别过难/过易题 |

---

## 五、技术要求

### 5.1 技术栈约束

| 层级 | 选型 | 版本/说明 |
|------|------|-----------|
| 前端框架 | Vue 3 | Composition API + `<script setup>` |
| 前端构建 | Vite | 最新稳定版 |
| 前端路由 | Vue Router 4 | 路由与权限拦截 |
| 前端状态 | Pinia | 跨组件状态管理 |
| 前端 UI | Element Plus | 组件库 |
| 前端 HTTP | Axios | 后端 API 调用，需配置全局拦截器 |
| 后端框架 | Spring Boot | 4.0.6 |
| 后端语言 | JDK | 21（启用 Record、Pattern Matching、Virtual Threads 等特性） |
| 后端 ORM | Spring Data JPA + Hibernate | Hibernate 社区方言 7.2.12.Final |
| 后端工具 | Lombok | 简化 Entity / DTO 样板代码 |
| 数据库 | SQLite | xerial sqlite-jdbc，文件位于 `Data/English.sqlite` |

**硬约束**：
1. 严禁替换任一选型；如需替换，必须先更新文档并通过评审
2. 后端不得引入 MyBatis / MyBatis-Plus 等与 JPA 冲突的 ORM 框架
3. 前端不得引入 jQuery、Vue 2、Element UI 等与 Vue 3 生态不兼容的依赖

### 5.2 数据库特殊约束

| 约束 | 说明 |
|------|------|
| 布尔字段 | 使用 Integer（0/1）而非 Boolean |
| 时间字段 | 使用 String（ISO 8601）而非 LocalDateTime |
| 主键类型 | 使用 Integer 而非 Long |

### 5.3 API 规范

- 所有业务接口必须以 `/api/v1/` 开头
- 统一返回 `Result<T>` 格式：`{ "code": int, "message": string, "data": T }`
- 使用全局异常处理器，禁止在 Controller 中 try-catch
- 资源命名使用小写、复数名词，单词间用中划线连接

### 5.4 JPA 规范

- Entity 与 DTO 完全隔离，禁止将 Entity 直接作为 API 响应
- 涉及关联查询必须使用 `@EntityGraph` 或 `JOIN FETCH`
- 所有 Entity 必须显式标注 `@Table(name = "...")`
- 主键策略统一为 IDENTITY（SQLite 自增）
- 枚举字段使用 `@Enumerated(EnumType.STRING)`

### 5.5 代码分层

| 层 | 职责 |
|---|------|
| Controller | 接收请求、参数校验、调用 Service、返回 `Result<T>` |
| Service | 业务逻辑、事务控制、Entity ↔ DTO 转换 |
| Repository | 数据库访问，仅做 CRUD 与查询 |
| Entity | 与数据字典表严格对应 |
| DTO | 跨层数据传输 |

**事务控制**：
- 写操作必须在 Service 方法上加 `@Transactional`
- 涉及多表写入的复杂业务需指定 `rollbackFor = Exception.class`
- 禁止在 Controller 上使用 `@Transactional`

---

## 六、数据库设计

### 6.1 核心表清单

| 编号 | 表名 | 中文 | 主键 | 模块 |
|------|------|------|------|------|
| T1 | `user` | 用户表 | `id` | M01 |
| T2 | `question` | 题目表 | `id` | M02 |
| T3 | `exam` | 考试表 | `id` | M03 |
| T4 | `score` | 分数表 | `id` | M04 |

### 6.2 物理外键

- `score.user` → `user.id`
- `score.exam` → `exam.id`
- `exam.question_sum.items[].questionId` → `question.id`（逻辑引用，JSON 快照内）

### 6.3 JSON 字段设计

| 字段 | 所在表 | 用途 | 性质 |
|------|--------|------|------|
| `answer` | `question` | 适配 5 种题型的差异化答案 | 静态元数据 |
| `question_sum` | `exam` | 考试题目汇总快照 | 快照 |
| `detail` | `score` | 每题得分明细与统计 | 明细 |

**快照机制**：组卷时一次性写入 `question_sum`，题目后续修改/删除不影响已组卷考试。

---

## 七、交付物要求

### 7.1 代码交付
- 完整的 Spring Boot 后端工程
- 完整的 Vue 3 前端工程（如已创建）
- SQLite 数据库文件及 DDL 脚本

### 7.2 文档交付
- 项目 README（含快速开始指南）
- Wiki 技术文档（API 规范、数据字典、模块设计）
- UML 建模图（类图、用例图、时序图等）

### 7.3 测试要求
- 单元测试覆盖 Service 层核心业务逻辑
- 集成测试验证 Repository 数据库操作
- 测试用例需包含正常路径和异常路径

---

## 八、评分标准（参考）

| 评价维度 | 权重 | 说明 |
|----------|------|------|
| 功能完整性 | 30% | 是否实现指导书中要求的核心功能 |
| 代码质量 | 25% | 代码规范、分层清晰、注释完整 |
| 架构设计 | 20% | 前后端分离、RESTful API、数据库设计合理性 |
| 文档质量 | 15% | README、Wiki 文档、UML 图的完整性与规范性 |
| 测试覆盖 | 10% | 单元测试数量与质量 |

---

## 九、附录

### 9.1 术语表

| 术语 | 说明 |
|------|------|
| OOAD | Object-Oriented Analysis and Design，面向对象分析与设计 |
| UML | Unified Modeling Language，统一建模语言 |
| RESTful | Representational State Transfer，表述性状态转移 |
| DTO | Data Transfer Object，数据传输对象 |
| VO | View Object，视图对象 |
| ORM | Object-Relational Mapping，对象关系映射 |

### 9.2 开发工具建议

| 工具 | 用途 |
|------|------|
| IntelliJ IDEA | Java 后端开发 |
| VS Code | Vue 前端开发 |
| Trae CN | AI 辅助开发 |
| SQLite Browser | 数据库管理 |
| Postman | API 测试 |
| Draw.io / PlantUML | UML 建模 |

### 9.3 参考资源

- Spring Boot 官方文档
- Vue 3 官方文档
- Spring Data JPA 官方文档
- SQLite 官方文档
- 《UML 精粹》（第 3 版）

---

> 本文档由 wiki 项目文档反向重构生成，用于归档《面向对象软件设计与建模》课程期末实验指导书的核心内容。完整的技术规范和实现细节请参阅 [00-INDEX.md](file:///d:/GDUT-OOP_20260601/wiki/00-INDEX.md)。
