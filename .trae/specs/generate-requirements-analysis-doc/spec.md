# 生成需求分析实验报告Word文档 Spec

## Why
根据已确认的编写方案（`temp/report/需求分析编写方案.md`），需要生成完整的「需求分析」实验报告Word文档，覆盖7个章节（封面+背景+环境+ER图+类图+用例图+用例描述+时序图），38个用例全部从Controller端点反推。

## What Changes
- 在 `temp/report/` 目录下生成 `需求分析_黄泊凯_面向对象软件设计与建模.docx`
- 文档包含：封面页、7个正文章节、38个用例描述表格、38张时序图、ER图、类图、4张用例图
- 所有UML图以Mermaid代码块形式嵌入文档（等宽字体），并标注图编号和图题

## Impact
- Affected code: 无代码变更
- Affected specs: 依赖 generate-report-plans 的输出

---

## ADDED Requirements

### Requirement: 封面页
文档 SHALL 包含封面页，格式参照`temp\面向对象软件设计与建模实验报告-需求分析封面.docx`，包含以下信息：
- 课程名称：面向对象软件设计与建模
- 题目：需求分析
- 指导教师：欧毓毅
- 系别：计算机学院
- 专业：软件工程
- 学生姓名：黄泊凯
- 班级/学号：软工3/3123004394
- 实验日期：2026-06-01

#### Scenario: 封面页生成
- **WHEN** 生成Word文档
- **THEN** 文档第一页为封面页，包含上述所有信息，格式与模板一致

### Requirement: 第一章 背景及意义
文档 SHALL 包含"在线学习系统的背景及意义"章节，约500-800字，涵盖：
- 在线教育时代背景
- 系统定位：面向英语学科的在线学习与考试平台
- 核心价值：题库管理→智能组卷→在线考试→成绩分析的完整闭环
- 三种角色的核心需求概述

### Requirement: 第二章 实验环境
文档 SHALL 包含"实验环境"章节，含三个子节：
- 2.1 硬件环境（用户提供的具体配置）
- 2.2 软件环境（OS/JDK/Maven/SQLite/Trae CN等）
- 2.3 开发工具（IDE/AI辅助/Git/Postman/Mermaid等）

### Requirement: 第三章 系统ER图
文档 SHALL 包含ER图章节：
- 用Mermaid erDiagram语法输出4张表（user/question/exam/score）及其关系
- 标注所有字段及类型
- 文字说明表间关系（user→score 1:N、exam→score 1:N、exam→question M:N快照、user→question 隐式1:N）
- 数据来源：02-Data-Dictionary.md §2/§4、Entity代码、DDL脚本

### Requirement: 第四章 实体类图
文档 SHALL 包含实体类图章节：
- 4个Entity类（User/Question/Exam/Score）+ 3个Enum（UserType/QuestionType/ExamStatus）
- 包含类名、属性（含类型）、主要操作
- 标注类之间的关系（4表独立设计，无JPA关联）
- 用Mermaid classDiagram语法输出
- 数据来源：Entity代码、Enum代码

### Requirement: 第五章 子系统用例图
文档 SHALL 包含4个子系统用例图：
- 5.1 M01 用户认证与管理（11个用例，参与者：student/teacher/admin）
- 5.2 M02 题库管理（8个用例，参与者：teacher/admin）
- 5.3 M03 组卷与考试管理（10个用例，参与者：teacher/admin/student）
- 5.4 M04 成绩统计（9个用例，参与者：student/teacher/admin）
- 每张用例图用Mermaid语法输出
- 所有用例从Controller端点反推
- 数据来源：4个Controller代码

### Requirement: 第六章 用例描述
文档 SHALL 包含所有38个用例的完整描述表格，按模块分组：
- 6.1 M01模块（11个用例）
- 6.2 M02模块（8个用例）
- 6.3 M03模块（10个用例）
- 6.4 M04模块（9个用例）
- 每个用例含：用例名称、用例编号(UC-M0x-xx)、参与者、前置条件、基本事件流、替代事件流、后置条件
- 数据来源：Controller端点 + Service业务逻辑 + Wiki模块文档

### Requirement: 第七章 时序图
文档 SHALL 包含38个用例的时序图，按模块分组：
- 7.1 M01模块（11张时序图）
- 7.2 M02模块（8张时序图）
- 7.3 M03模块（10张时序图）
- 7.4 M04模块（9张时序图）
- 每张时序图用Mermaid sequenceDiagram语法输出
- 参与者→Controller→Service→Repository→Database调用链
- 方法名与代码中实际方法名一致
- 数据来源：Controller/Service/Repository代码

### Requirement: Word文档格式
文档 SHALL 遵循以下格式规范：
- 标题：一级标题小二号加粗楷体，二级标题小三号加粗楷体，三级标题四号加粗楷体
- 正文：仿宋小四号，1.5倍行距
- 图表：居中，图题在图下方，表题在表上方
- 页码：底部居中
- UML图：Mermaid代码以等宽字体(Courier New)嵌入，标注图编号和图题
- 文件名：`需求分析_黄泊凯_面向对象软件设计与建模.docx`
