# Tasks

- [x] Task 1: 读取所有源代码和Wiki文档，收集生成文档所需的数据
  - [x] SubTask 1.1: 读取4个Controller代码，提取完整端点列表
  - [x] SubTask 1.2: 读取4个Service代码，提取方法签名和调用链
  - [x] SubTask 1.3: 读取4个Entity + 3个Enum代码，提取字段和类型
  - [x] SubTask 1.4: 读取4个Repository代码，提取查询方法
  - [x] SubTask 1.5: 读取Wiki文档和DDL脚本，提取ER关系和表结构
  - [x] SubTask 1.6: 读取已有的编写方案 `temp/report/需求分析编写方案.md`

- [x] Task 2: 生成需求分析Word文档
  - [x] SubTask 2.1: 安装python-docx依赖
  - [x] SubTask 2.2: 编写Python脚本生成Word文档
  - [x] SubTask 2.3: 运行脚本生成 `temp/report/需求分析_黄泊凯_面向对象软件设计与建模.docx`
  - [x] SubTask 2.4: 修复格式问题（三级标题+行距一致性）

- [x] Task 3: 验证生成的Word文档
  - [x] SubTask 3.1: 检查文档结构完整性（7个章节+封面）
  - [x] SubTask 3.2: 检查用例数量（38个用例全部覆盖）
  - [x] SubTask 3.3: 检查UML图数量（1张ER图+1张类图+4张用例图+38张时序图）
  - [x] SubTask 3.4: 检查格式规范（字体/字号/行距/页码）

# Task Dependencies
- Task 2 依赖 Task 1（需要先收集数据）
- Task 3 依赖 Task 2（需要先生成文档）
