# Tasks

- [x] Task 1: 读取Wiki文档和关键代码，收集扩展性设计点和问题记录
  - [x] SubTask 1.1: 读取wiki/02-Data-Dictionary.md §10.2（5个典型问题）
  - [x] SubTask 1.2: 读取关键代码文件（Result/GlobalExceptionHandler/RequireRole/Question/Exam/Score等）
  - [x] SubTask 1.3: 读取已有的编写方案 `temp/report/实验总结编写方案.md`

- [x] Task 2: 生成实验总结和感想Word文档
  - [x] SubTask 2.1: 编写Python脚本（复用前两份文档的格式函数）
  - [x] SubTask 2.2: 运行脚本生成 `temp/report/实验总结和感想_黄泊凯_面向对象软件设计与建模.docx`

- [x] Task 3: 验证生成的Word文档
  - [x] SubTask 3.1: 检查文档结构完整性（3个章节+封面）
  - [x] SubTask 3.2: 检查8个扩展性设计点均有代码引用
  - [x] SubTask 3.3: 检查5个典型问题与wiki §10.2一致
  - [x] SubTask 3.4: 检查格式规范
  - [x] SubTask 3.5: 修正事实错误（DTO数量43→42、GlobalExceptionHandler异常类型）

# Task Dependencies
- Task 2 依赖 Task 1
- Task 3 依赖 Task 2
