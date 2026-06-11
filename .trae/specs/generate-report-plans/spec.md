# 实验报告编写方案生成 Spec

## Why
广东工业大学《面向对象软件设计与建模》课程要求提交三份实验报告（需求分析、系统设计、实验总结和感想）。需要基于项目代码和Wiki文档的深度分析，为每份报告生成详细的编写方案大纲（含Mermaid UML图草案），确保用例覆盖所有Controller端点，内容有代码依据。

## What Changes
- 在 `temp/report/` 目录下生成3份Markdown编写方案文档：
  - `需求分析编写方案.md` — 文档一的详细大纲
  - `系统设计编写方案.md` — 文档二的详细大纲
  - `实验总结编写方案.md` — 文档三的详细大纲
- 在 `temp/report/系统理解总结.md` 中输出系统整体理解总结

## Impact
- Affected code: 无代码变更，仅新增方案文档
- Affected specs: 无

---

## ADDED Requirements

### Requirement: 系统理解总结
系统 SHALL 输出约500字的系统整体理解，涵盖系统定位、角色划分、核心业务流程、技术架构特点。

#### Scenario: 生成系统理解总结
- **WHEN** 用户请求生成报告方案
- **THEN** 在 `temp/report/系统理解总结.md` 中输出完整的系统理解总结，包含：
  - 系统定位：英语在线学习系统
  - 角色划分：学生/教师/管理员三种角色及其权限
  - 核心业务流程：出题→组卷→考试→判分→统计
  - 技术架构特点：三层架构+Common基础设施、单表+JSON设计、JWT无状态认证

### Requirement: 需求分析编写方案
系统 SHALL 生成文档一「需求分析」的详细编写方案，严格覆盖实验指导书要求的6个章节。

#### Scenario: 生成需求分析方案
- **WHEN** 用户请求生成报告方案
- **THEN** 在 `temp/report/需求分析编写方案.md` 中输出方案，包含以下完整内容：

**1. 在线学习系统的背景及意义**
- 内容：英语在线教育市场背景、传统考试痛点、系统价值
- 数据来源：README.md 项目介绍
- 预估篇幅：800字

**2. 硬件环境、软件环境以及开发工具**
- 内容：硬件（开发机配置）、软件（JDK 21 / Spring Boot 4.0.6 / SQLite / Vue 3）、工具（IntelliJ IDEA / Git / Trae CN）
- 数据来源：README.md 技术栈、application.yaml 配置
- 预估篇幅：500字

**3. 系统的ER图以及实体类图**
- ER图：4张表（user/question/exam/score）及其关系，用Mermaid erDiagram语法
- 实体类图：User/Question/Exam/Score的属性与操作，用Mermaid classDiagram语法
- 数据来源：02-Data-Dictionary.md §2 ER关系总览、§4 字段级定义、Entity代码
- 预估篇幅：1页ER图 + 1页类图 + 800字说明

**4. 子系统用例图**
- M01 用户认证模块用例图（11个用例）
- M02 题库管理模块用例图（8个用例）
- M03 组卷与考试模块用例图（9个用例）
- M04 成绩统计模块用例图（9个用例）
- 每个用例图用Mermaid语法给出
- 数据来源：各Controller的所有端点反推
- 预估篇幅：4页用例图 + 800字说明

**5. 子系统各个用例的描述**
- 每个模块的所有用例描述表格（不是选几个，是全部）
- 表格字段：用例编号、用例名称、参与者、前置条件、主事件流、后置条件
- M01用例列表（11个）：登录、注册、登出、获取当前用户、修改密码、分页查询用户、创建用户、更新用户、更新用户状态、删除用户、批量删除用户
- M02用例列表（8个）：创建题目、批量导入题目、查询题目详情、分页查询题目列表、更新题目、删除题目、批量删除题目、随机获取题目
- M03用例列表（9个）：创建手动组卷考试、创建自动组卷考试、获取可参加考试列表、获取考试详情、学生预览考试、修改考试、发布考试、撤回考试、删除考试、分页查询考试列表
- M04用例列表（9个）：提交答卷、教师评卷、查询我的成绩、查询我的错题集、查询分数详情、查询考试所有考生分数、考试统计报表、题目统计列表、单题详细统计
- 数据来源：Controller端点 + Service业务逻辑 + Wiki模块文档
- 预估篇幅：37个用例 × 约150字/用例 = 约6000字

**6. 用例描述所涉及的时序图**
- 每个用例对应1张时序图，用Mermaid sequenceDiagram语法
- 覆盖所有37个用例
- 数据来源：Controller → Service → Repository 调用链
- 预估篇幅：37张时序图

### Requirement: 系统设计编写方案
系统 SHALL 生成文档二「系统设计」的详细编写方案，严格覆盖实验指导书要求的5个章节。

#### Scenario: 生成系统设计方案
- **WHEN** 用户请求生成报告方案
- **THEN** 在 `temp/report/系统设计编写方案.md` 中输出方案，包含以下完整内容：

**1. 子系统架构图或类包图**
- 三层架构图（Controller → Service → Repository）+ Common基础设施层
- 类包图展示包组织结构（common / modules/M01~M04）
- 用Mermaid graph/classDiagram语法
- 数据来源：项目目录结构、01-Global-Standards.md §6 代码分层
- 预估篇幅：2页图 + 1000字说明

**2. 每一层的类文件结构**
- Controller层：4个Controller类及其方法列表
- Service层：4个Service类及其方法签名
- Repository层：4个Repository接口及其查询方法
- Entity层：4个Entity类及其字段
- DTO层：30+个Record/VO类
- Common层：Result/PageResult/BusinessException/GlobalExceptionHandler/JwtUtil/JwtAuthenticationInterceptor/RequireRole/WebMvcConfig
- 数据来源：项目代码文件
- 预估篇幅：2000字

**3. 每个用例的详细时序图（VOPC）**
- 与需求分析时序图对应，但增加VOPC类图视角
- 每个用例的参与类类图，标注方法调用关系
- 数据来源：Service层方法实现
- 预估篇幅：37张VOPC类图

**4. 核心类的详细定义**
- 至少覆盖：UserService、QuestionService、ExamService、ScoreService、JwtUtil、Result<T>
- 每个类包含：属性列表 + 方法签名 + 关键逻辑说明
- 用Mermaid classDiagram语法
- 数据来源：Service代码、Common代码
- 预估篇幅：6个类 × 约300字 = 约2000字

**5. 数据库表结构设计**
- 4张表的完整字段定义表（字段名、类型、约束、默认值、说明）
- 索引定义
- 外键关系
- JSON字段结构说明
- 数据来源：02-Data-Dictionary.md §4、scripts/table_*.sql
- 预估篇幅：4张表 × 约300字 = 约1500字

### Requirement: 实验总结编写方案
系统 SHALL 生成文档三「实验总结和感想」的详细编写方案，严格覆盖实验指导书要求的3个章节。

#### Scenario: 生成实验总结方案
- **WHEN** 用户请求生成报告方案
- **THEN** 在 `temp/report/实验总结编写方案.md` 中输出方案，包含以下完整内容：

**1. 系统扩展性和灵活性设计**
- 从代码中提炼的设计模式与扩展性措施：
  - JSON多态答案结构（5种题型适配）
  - 考试状态机（4态流转）
  - DTO隔离（Entity禁止裸奔）
  - 统一异常处理（GlobalExceptionHandler）
  - 统一响应封装（Result<T>）
  - JWT无状态认证
  - @RequireRole注解式权限控制
  - 快照式组卷（question_sum JSON）
  - 题内统计自维护（use/correct字段）
- 数据来源：代码中的设计模式实现、Wiki文档
- 预估篇幅：1500字

**2. AI工具使用情况与问题解决**
- AI工具：Trae CN / Qwen / GLM 的使用场景与效果
- 开发环境描述
- 典型问题与解决方案（参考02-Data-Dictionary.md §10.2 的5个问题）
- 实验感想
- 数据来源：02-Data-Dictionary.md §10.2、项目实际开发过程
- 预估篇幅：1500字

**3. 完整AI提示词附录**
- 整理项目开发过程中使用的AI提示词
- 数据来源：Wiki文档中的AI协作规范、项目实际使用情况
- 预估篇幅：按实际内容

### Requirement: 风险提示与建议
系统 SHALL 在方案中列出分析中发现的潜在问题或需要注意的地方。

#### Scenario: 输出风险提示
- **WHEN** 用户请求生成报告方案
- **THEN** 在方案中明确列出以下风险：
  - 前端未实现（Vue 3项目不存在），报告中需说明
  - DraftController/DraftCacheService在代码中不存在（Wiki中提及但未实现）
  - 某些用例可能需要合并描述（如分页查询类用例）
  - UML图需人工审核后转换为图片插入Word
  - 实验报告封面需使用指定docx模板
