# 生成实验总结和感想Word文档 Spec

## Why
根据已确认的编写方案（`temp/report/实验总结编写方案.md`），需要生成完整的「实验总结和感想」实验报告Word文档，覆盖3个章节（封面+扩展性设计+AI工具与问题+提示词附录），重点从代码中提炼具体设计决策。

## What Changes
- 在 `temp/report/` 目录下生成 `实验总结和感想_黄泊凯_面向对象软件设计与建模.docx`
- 文档包含：封面页、3个正文章节、8个扩展性设计点、5个典型问题、AI提示词附录

## Impact
- Affected code: 无代码变更
- Affected specs: 依赖 generate-report-plans 的输出

---

## ADDED Requirements

### Requirement: 封面页
文档 SHALL 包含封面页，题目为"实验总结和感想"，其余信息同前两份文档。

### Requirement: 第一章 系统扩展性和灵活性设计
文档 SHALL 包含8个扩展性设计点（约2000-3000字），每个设计点必须引用具体代码文件：
1. JSON多态答案字段（Question.java + 5种Answer DTO）
2. 组卷快照设计（Exam.java questionSum + ExamService快照构建）
3. 考试状态机（ExamStatus枚举 + 4态流转 + Mermaid stateDiagram）
4. Entity-DTO完全隔离（43个DTO类 + toVO转换）
5. 统一响应契约与全局异常处理（Result<T> + GlobalExceptionHandler + 业务码体系）
6. 注解式权限控制（@RequireRole + JwtAuthenticationInterceptor）
7. 4表独立架构（消除N+1 + findAllById批量加载）
8. 题内统计自维护（use/correct字段 + incrementUse/incrementCorrect）

### Requirement: 第二章 AI工具使用与实验问题
文档 SHALL 包含4个子节（约2000-3000字）：
- 2.1 AI工具概述（Trae CN/Qwen/GLM的使用场景）
- 2.2 系统环境（OS/JDK/Maven/SQLite/IDE）
- 2.3 5个典型问题与解决方案（从wiki/02-Data-Dictionary.md §10.2提取）
- 2.4 实验感想（约800-1200字，真情实感）

### Requirement: 第三章 完整AI提示词附录
文档 SHALL 包含6个场景的提示词列表：
1. 数据模型设计阶段
2. 全局规范制定阶段
3. 后端模块开发阶段
4. 单元测试编写阶段
5. Wiki文档生成阶段
6. 问题排查阶段

### Requirement: Word文档格式
文档 SHALL 遵循与前两份文档相同的格式规范。文件名：`实验总结和感想_黄泊凯_面向对象软件设计与建模.docx`
