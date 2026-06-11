# Tasks

- [x] Task 1: 读取所有源代码，收集生成文档所需的精确数据
  - [x] SubTask 1.1: 读取4个Controller代码，提取端点和方法签名
  - [x] SubTask 1.2: 读取4个Service代码，提取全部属性和方法签名
  - [x] SubTask 1.3: 读取4个Entity + 3个Enum + 所有DTO代码
  - [x] SubTask 1.4: 读取4个Repository代码，提取查询方法
  - [x] SubTask 1.5: 读取Common层8个代码文件
  - [x] SubTask 1.6: 读取DDL脚本和application.yaml

- [x] Task 2: 生成系统设计Word文档
  - [x] SubTask 2.1: 编写Python脚本生成Word文档（复用需求分析脚本的格式函数）
  - [x] SubTask 2.2: 运行脚本生成 `temp/report/系统设计_黄泊凯_面向对象软件设计与建模.docx`
  - [x] SubTask 2.3: 修复缺失的UserExamHistoryVO DTO类

- [x] Task 3: 验证生成的Word文档
  - [x] SubTask 3.1: 检查文档结构完整性（5个章节+封面）
  - [x] SubTask 3.2: 检查38个VOPC时序图和7个核心类类图
  - [x] SubTask 3.3: 检查43个DTO类全部列出
  - [x] SubTask 3.4: 检查方法名与代码一致

# Task Dependencies
- Task 2 依赖 Task 1
- Task 3 依赖 Task 2
