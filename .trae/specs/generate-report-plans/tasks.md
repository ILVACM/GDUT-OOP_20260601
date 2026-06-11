# Tasks

- [x] Task 1: 生成系统理解总结文档
  - [x] SubTask 1.1: 在 `temp/report/` 目录下创建 `系统理解总结.md`
  - [x] SubTask 1.2: 撰写约500字的系统整体理解，涵盖系统定位、角色划分（学生/教师/管理员）、核心业务流程（出题→组卷→考试→判分→统计）、技术架构特点（三层架构+Common基础设施、单表+JSON设计、JWT无状态认证、4表独立设计）

- [x] Task 2: 生成需求分析编写方案文档
  - [x] SubTask 2.1: 在 `temp/report/` 目录下创建 `需求分析编写方案.md`
  - [x] SubTask 2.2: 编写第1章「背景及意义」大纲（数据来源：README.md，预估800字）
  - [x] SubTask 2.3: 编写第2章「开发环境」大纲（数据来源：README.md技术栈、application.yaml，预估500字）
  - [x] SubTask 2.4: 编写第3章「ER图+实体类图」大纲，含Mermaid erDiagram和classDiagram草案（数据来源：02-Data-Dictionary.md §2/§4、Entity代码）
  - [x] SubTask 2.5: 编写第4章「子系统用例图」大纲，含4个模块的Mermaid用例图草案（M01:11个用例、M02:8个用例、M03:10个用例、M04:9个用例）
  - [x] SubTask 2.6: 编写第5章「所有用例描述表格」大纲，覆盖38个用例的完整描述（表格字段：编号/名称/参与者/前置条件/主事件流/后置条件）
  - [x] SubTask 2.7: 编写第6章「时序图」大纲，含38个用例的Mermaid sequenceDiagram草案

- [x] Task 3: 生成系统设计编写方案文档
  - [x] SubTask 3.1: 在 `temp/report/` 目录下创建 `系统设计编写方案.md`
  - [x] SubTask 3.2: 编写第1章「架构图+类包图」大纲，含Mermaid架构图和包图草案（数据来源：项目目录结构、01-Global-Standards.md §6）
  - [x] SubTask 3.3: 编写第2章「每层类文件结构」大纲，列出Controller/Service/Repository/Entity/DTO/Common层所有类
  - [x] SubTask 3.4: 编写第3章「VOPC类图」大纲，含38个用例的参与类类图草案
  - [x] SubTask 3.5: 编写第4章「核心类详细定义」大纲，含UserService/QuestionService/ExamService/ScoreService/JwtUtil/Result的Mermaid classDiagram
  - [x] SubTask 3.6: 编写第5章「数据库表结构」大纲，含4张表完整字段定义表（数据来源：02-Data-Dictionary.md §4、scripts/table_*.sql）

- [x] Task 4: 生成实验总结编写方案文档
  - [x] SubTask 4.1: 在 `temp/report/` 目录下创建 `实验总结编写方案.md`
  - [x] SubTask 4.2: 编写第1章「扩展性和灵活性设计」大纲，提炼9个设计模式/扩展性措施（数据来源：代码实现、Wiki文档）
  - [x] SubTask 4.3: 编写第2章「AI工具/环境/问题/感想」大纲（数据来源：02-Data-Dictionary.md §10.2）
  - [x] SubTask 4.4: 编写第3章「AI提示词附录」大纲（数据来源：Wiki AI协作规范）

- [x] Task 5: 输出风险提示与建议
  - [x] SubTask 5.1: 在方案文档中列出风险提示（前端未实现、DraftController不存在、UML图转换、封面模板等）

# Task Dependencies
- Task 2, 3, 4 依赖 Task 1（系统理解总结为后续方案提供基础）
- Task 2, 3, 4 之间可并行执行
- Task 5 可与 Task 2-4 并行执行
