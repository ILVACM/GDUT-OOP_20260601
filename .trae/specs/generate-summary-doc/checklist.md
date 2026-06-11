# Checklist

## 封面页
- [x] 封面页包含课程名称（面向对象软件设计与建模）
- [x] 封面页包含题目（实验总结和感想）
- [x] 封面页包含指导教师（欧毓毅）
- [x] 封面页包含学生姓名（黄泊凯）
- [x] 封面页包含班级/学号（软工3/3123004394）
- [x] 封面页包含实验日期（2026-06-01）

## 第一章 扩展性和灵活性设计
- [x] 1.1 JSON多态答案字段（引用Question.java + 5种Answer DTO）
- [x] 1.2 组卷快照设计（引用Exam.java questionSum + ExamService）
- [x] 1.3 考试状态机（ExamStatus枚举 + Mermaid stateDiagram）
- [x] 1.4 Entity-DTO完全隔离（42个DTO类 + toVO转换）
- [x] 1.5 统一响应契约与全局异常处理（Result<T> + GlobalExceptionHandler + 业务码）
- [x] 1.6 注解式权限控制（@RequireRole + JwtAuthenticationInterceptor）
- [x] 1.7 4表独立架构（消除N+1 + findAllById批量加载）
- [x] 1.8 题内统计自维护（use/correct + incrementUse/incrementCorrect）
- [x] 每个设计点有具体代码引用（文件名+类名+方法名）
- [x] 第一章总篇幅约2000-3000字

## 第二章 AI工具与实验问题
- [x] 2.1 AI工具概述（Trae CN/Qwen/GLM使用场景）
- [x] 2.2 系统环境（OS/JDK/Maven/SQLite/IDE）
- [x] 2.3 5个典型问题与wiki §10.2一致
  - [x] 问题1：Boolean vs Integer类型不匹配
  - [x] 问题2：LocalDateTime vs String时间字段
  - [x] 问题3：Score外键映射方式
  - [x] 问题4：主键类型选择
  - [x] 问题5：SQLite JDBC自动配置冲突
- [x] 2.4 实验感想约800-1200字（真情实感）
- [x] 第二章总篇幅约2000-3000字

## 第三章 AI提示词附录
- [x] 6个场景的提示词列表完整
- [x] 每个提示词含场景说明+核心指令

## 文档格式
- [x] 一级标题：楷体小二号加粗
- [x] 二级标题：楷体小三号加粗
- [x] 三级标题：楷体四号加粗
- [x] 正文：仿宋小四号，1.5倍行距
- [x] 文件名：实验总结和感想_黄泊凯_面向对象软件设计与建模.docx
- [x] 保存路径：temp/report/
