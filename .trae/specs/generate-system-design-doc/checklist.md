# Checklist

## 封面页
- [x] 封面页包含课程名称（面向对象软件设计与建模）
- [x] 封面页包含题目（系统设计）
- [x] 封面页包含指导教师（欧毓毅）
- [x] 封面页包含学生姓名（黄泊凯）
- [x] 封面页包含班级/学号（软工3/3123004394）
- [x] 封面页包含实验日期（2026-06-01）

## 第一章 架构设计
- [x] 1.1 整体架构图用Mermaid graph语法输出
- [x] 1.1 架构图包含前端层(标注设计规划中)+后端三层+数据层
- [x] 1.1 架构图包含系统运行环境说明(300字以上)
- [x] 1.2 类包图用Mermaid语法输出
- [x] 1.2 类包图包含common(4子包:api/exception/security/config)+modules(4模块包)

## 第二章 类文件结构
- [x] 2.1 Controller层表格含4个Controller类及端点数
- [x] 2.2 Service层表格含4个Service类及方法数
- [x] 2.3 Repository层表格含4个Repository接口及自定义查询方法
- [x] 2.4 Entity层表格含4个Entity+3个Enum
- [x] 2.5 DTO层表格含43个DTO类（按模块分组，已补充UserExamHistoryVO）
- [x] 2.6 Common层表格含8个类
- [x] 每层有200字以上文字说明

## 第三章 VOPC时序图
- [x] M01模块11张时序图
- [x] M02模块8张时序图
- [x] M03模块10张时序图
- [x] M04模块9张时序图
- [x] 每张图下方附100字说明
- [x] 方法名与代码完全一致

## 第四章 核心类详细定义
- [x] UserService类图含全部属性和方法签名
- [x] QuestionService类图含全部属性和方法签名
- [x] ExamService类图含全部属性和方法签名
- [x] ScoreService类图含全部属性和方法签名
- [x] JwtUtil类图含全部属性和方法签名
- [x] Result<T>类图含全部属性和方法签名
- [x] GlobalExceptionHandler类图含全部方法签名
- [x] 每个类图下方附设计要点说明

## 第五章 数据库表结构
- [x] user表字段定义完整（字段名/Java类型/SQLite类型/约束/默认值/业务含义）
- [x] question表字段定义完整+answer JSON 5种结构说明
- [x] exam表字段定义完整+question_sum JSON结构+状态机说明
- [x] score表字段定义完整+detail JSON结构说明
- [x] 表间关系说明（物理外键+逻辑引用+删除策略）

## 文档格式
- [x] 一级标题：楷体小二号加粗
- [x] 二级标题：楷体小三号加粗
- [x] 三级标题：楷体四号加粗
- [x] 正文：仿宋小四号，1.5倍行距
- [x] UML图：等宽字体(Courier New)嵌入
- [x] 文件名：系统设计_黄泊凯_面向对象软件设计与建模.docx
- [x] 保存路径：temp/report/
