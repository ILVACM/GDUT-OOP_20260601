# 生成系统设计实验报告Word文档 Spec

## Why
根据已确认的编写方案（`temp/report/系统设计编写方案.md`），需要生成完整的「系统设计」实验报告Word文档，覆盖5个章节（封面+架构设计+类文件结构+VOPC时序图+核心类定义+数据库表结构），38个用例全部覆盖，方法名与代码完全一致。

## What Changes
- 在 `temp/report/` 目录下生成 `系统设计_黄泊凯_面向对象软件设计与建模.docx`
- 文档包含：封面页、5个正文章节、38个VOPC时序图、7个核心类详细类图、4张数据库表定义

## Impact
- Affected code: 无代码变更
- Affected specs: 依赖 generate-report-plans 的输出

---

## ADDED Requirements

### Requirement: 封面页
文档 SHALL 包含封面页，题目为"系统设计"，其余信息同需求分析文档（课程名称/指导教师/系别/专业/姓名/班级学号/日期）。

### Requirement: 第一章 子系统架构设计
文档 SHALL 包含两个子节：
- 1.1 整体架构图：Mermaid graph展示前端层(设计规划中)+后端三层+数据层+系统运行环境说明(300字以上)
- 1.2 类包图：Mermaid展示common(4子包)+modules(4模块包)的包结构

### Requirement: 第二章 各层类文件结构
文档 SHALL 包含6个子节，每层一个表格+200字以上说明：
- 2.1 Controller层（4个Controller类，含端点数）
- 2.2 Service层（4个Service类，含方法数）
- 2.3 Repository层（4个Repository接口，含自定义查询方法）
- 2.4 Entity层（4个Entity+3个Enum，含字段数）
- 2.5 DTO层（43个DTO/VO/Req类，按模块分组）
- 2.6 Common层（8个类，含职责说明）

### Requirement: 第三章 用例详细时序图（VOPC）
文档 SHALL 包含38个用例的详细时序图，按模块分组（M01:11, M02:8, M03:10, M04:9），每张图下方附100字说明。方法名必须与代码完全一致。

### Requirement: 第四章 核心类详细定义
文档 SHALL 包含7个核心类的Mermaid classDiagram（含全部属性和方法签名）：
- UserService、QuestionService、ExamService、ScoreService
- JwtUtil、Result<T>、GlobalExceptionHandler

### Requirement: 第五章 数据库表结构设计
文档 SHALL 包含4张表的完整字段定义表（字段名/Java类型/SQLite类型/约束/默认值/业务含义），以及JSON字段结构说明和表间关系说明。

### Requirement: Word文档格式
文档 SHALL 遵循与需求分析文档相同的格式规范：
- 一级标题楷体18pt加粗、二级标题楷体15pt加粗、三级标题楷体14pt加粗
- 正文仿宋12pt/1.5倍行距
- UML图Courier New等宽字体
- 文件名：`系统设计_黄泊凯_面向对象软件设计与建模.docx`
