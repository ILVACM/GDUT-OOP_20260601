# Checklist

## 封面页
- [x] 封面页包含课程名称（面向对象软件设计与建模）
- [x] 封面页包含题目（需求分析）
- [x] 封面页包含指导教师（欧毓毅）
- [x] 封面页包含系别（计算机学院）
- [x] 封面页包含专业（软件工程）
- [x] 封面页包含学生姓名（黄泊凯）
- [x] 封面页包含班级/学号（软工3/3123004394）
- [x] 封面页包含实验日期（2026-06-01）

## 第一章 背景及意义
- [x] 包含在线教育时代背景
- [x] 包含系统定位（英语在线学习与考试平台）
- [x] 包含核心价值（题库→组卷→考试→分析闭环）
- [x] 包含三种角色需求概述
- [x] 篇幅约500-800字（实际约583字）

## 第二章 实验环境
- [x] 2.1 硬件环境包含用户提供的具体配置（i5-14600KF/32GB/RTX3090等）
- [x] 2.2 软件环境包含OS/JDK/Maven/SQLite/Trae CN
- [x] 2.3 开发工具包含IDE/AI辅助/Git/Postman/Mermaid

## 第三章 系统ER图
- [x] ER图包含4张表（user/question/exam/score）
- [x] ER图标注所有字段及类型
- [x] 文字说明4种表间关系
- [x] Mermaid erDiagram语法输出（Courier New字体段落，erDiagram关键词存在）

## 第四章 实体类图
- [x] 类图包含4个Entity类（User/Question/Exam/Score）
- [x] 类图包含3个Enum（UserType/QuestionType/ExamStatus）
- [x] 类图包含属性（含类型）和操作
- [x] 标注4表独立设计（无JPA关联）
- [x] Mermaid classDiagram语法输出（Courier New字体段落，classDiagram关键词存在）

## 第五章 子系统用例图
- [x] M01用例图覆盖11个用例（从UserController反推）
- [x] M02用例图覆盖8个用例（从QuestionController反推）
- [x] M03用例图覆盖10个用例（从ExamController反推）
- [x] M04用例图覆盖9个用例（从ScoreController反推）
- [x] 每张用例图标注参与者（student/teacher/admin）
- [x] Mermaid语法输出（使用graph TD语法，含subgraph分组）

## 第六章 用例描述
- [x] M01模块11个用例描述表格完整
- [x] M02模块8个用例描述表格完整
- [x] M03模块10个用例描述表格完整
- [x] M04模块9个用例描述表格完整
- [x] 每个用例含：名称/编号/参与者/前置条件/基本事件流/替代事件流/后置条件
- [x] 用例编号格式：UC-M0x-xx（38个用例编号全部存在）

## 第七章 时序图
- [x] M01模块11张时序图
- [x] M02模块8张时序图
- [x] M03模块10张时序图
- [x] M04模块9张时序图
- [x] 时序图方法名与代码一致
- [x] Mermaid sequenceDiagram语法输出（38个sequenceDiagram关键词，Courier New字体段落）

## 文档格式
- [x] 一级标题：楷体小二号加粗（KaiTi 18pt 加粗）
- [x] 二级标题：楷体小三号加粗（KaiTi 15pt 加粗）
- [x] 三级标题：楷体四号加粗（KaiTi 14pt 加粗，第5/6/7章子节已使用add_heading3）
- [x] 正文：仿宋小四号，1.5倍行距（仿宋12pt已使用，图题1.5倍行距，代码块1.0倍行距，表格1.0倍行距）
- [x] UML图：等宽字体(Courier New)嵌入
- [x] 图编号和图题完整（52处图编号）
- [x] 文件名：需求分析_黄泊凯_面向对象软件设计与建模.docx
- [x] 保存路径：temp/report/
