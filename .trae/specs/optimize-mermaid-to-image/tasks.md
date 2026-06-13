# Tasks
- [x] Task 1: 分析刚才操作的失败原因并记录
  - [x] SubTask 1.1: 记录 mermaid-cli (mmdc) 失败原因：Chromium 下载问题
  - [x] SubTask 1.2: 记录 mermaid.ink API 失败原因：接口变更返回 404
  - [x] SubTask 1.3: 记录 Playwright 方案成功的关键因素
- [x] Task 2: 创建可复用的 Mermaid 转图片脚本
  - [x] SubTask 2.1: 创建 `scripts/mermaid-to-image.mjs` 脚本
  - [x] SubTask 2.2: 支持命令行参数：输入 .mmd 文件路径、输出路径、格式
  - [x] SubTask 2.3: 自动处理 HTML 模板生成和清理
  - [x] SubTask 2.4: 添加错误处理和友好提示
- [x] Task 3: 更新项目文档
  - [x] SubTask 3.1: 标准流程已记录在 spec.md 中，作为后续操作的参考依据

# Task Dependencies
- [Task 2] depends on [Task 1]
- [Task 3] depends on [Task 2]
