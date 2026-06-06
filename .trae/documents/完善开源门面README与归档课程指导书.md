# 计划：完善开源门面 README.md 与归档课程指导书至 Wiki

## 当前状态分析

- **项目**：在线学习系统（English Online Learning System），Spring Boot 4 + Vue 3 + SQLite
- **后端**：已完成全链路（71 Java 文件、41 端点、43 DTO、73 测试通过）
- **前端**：尚未创建（frontend/ 目录不存在）
- **Logo**：`Data/Vue/logo.png` 已确认存在
- **指导书**：`面向对象软件设计与建模实验指导书2026.doc` 已确认存在
- **README.md**：已存在，需要覆盖重写
- **Wiki**：包含 00-INDEX.md、01-Global-Standards.md、02-Data-Dictionary.md 及 modules/references 子目录

##  Proposed Changes

### 任务一：生成符合国际开源标准的 README.md

**文件**：`d:/GDUT-OOP_20260601/README.md`（覆盖现有文件）

**内容结构**：

1. **Header**
   - Logo: `![Online Learning System Logo](Data/Vue/logo.png)`
   - 居中大标题：`# Online Learning System / 在线学习系统`
   - Slogan: `A modern English online learning platform built with Spring Boot 4 and Vue 3`
   - Shields.io 徽章：Build, License MIT, Spring Boot 4.x, Vue 3.x, JDK 21, SQLite

2. **✨ 项目简介 (Introduction)**
   - 2-3 段话描述系统背景、解决问题、目标用户（学生/教师/管理员）
   - 引用《面向对象软件设计与建模》课程项目背景

3. **🚀 核心特性 (Features)**
   - 按角色分列：学生端（在线答题、成绩查询、错题分析）、教师端（题库管理、手动/自动组卷、考试管理、成绩统计）、管理端（用户管理、全局数据看板）

4. **🛠️ 技术栈 (Tech Stack)**
   - 后端：Spring Boot 4.0.6、JDK 21、Spring Data JPA、Lombok、JWT
   - 前端：Vue 3、Vite、Vue Router 4、Pinia、Element Plus、Axios
   - 数据库：SQLite（xerial jdbc）
   - AI 辅助：Trae CN / AI Agent 辅助开发

5. **🏗️ 系统架构 (Architecture)**
   - 前后端分离架构、RESTful API 规范（`/api/v1/` 前缀）
   - 统一 `Result<T>` 响应契约
   - 单表+JSON 多态题库设计（answer/question_sum/detail 三个 JSON 字段）
   - 4 张核心表独立设计（user/question/exam/score），零 N+1 风险

6. **⚡ 快速开始 (Quick Start)**
   - 环境要求：JDK 21、Node.js 24.15.0、Maven 3.9+
   - 后端启动：`cd backend && mvn spring-boot:run`
   - 前端启动：`cd frontend && npm install && npm run dev`（标注：前端待创建）
   - 测试运行：`cd backend && mvn test`（73 个测试全部通过）

7. **📂 项目结构 (Project Structure)**
   - 使用 tree 风格代码块展示后端 modules/、common/、resources/
   - 展示 wiki/ 目录结构

8. **📖 详细文档 (Documentation)**
   - 引导前往 wiki/ 查看 API 规范、数据字典、模块设计文档
   - 列出关键文档链接

9. **🤝 贡献与致谢 (Contributing & Acknowledgements)**
   - 说明为《面向对象软件设计与建模》期末项目
   - 致谢 AI Agent 在开发中的贡献
   - 欢迎 Issue 和 PR

10. **📄 许可证 (License)**
    - MIT License

### 任务二：将指导书归档至 Wiki 知识库

**文件**：`d:/GDUT-OOP_20260601/wiki/00-Course-Guidelines.md`

**步骤**：
1. 读取 `面向对象软件设计与建模实验指导书2026.doc` 内容
   - 注意：.doc 格式为旧版 Word 二进制格式，可能需要通过 Python 的 `docx` 库或 pandoc 转换
2. 转换为结构清晰的 Markdown 格式
   - 保留层级标题（一、二、三 → #、##、###）
   - 保留核心要求、附录说明、评分标准
   - 去除无意义排版空白
3. 文件顶部添加元数据说明
4. 写入 wiki/ 目录

**注意**：由于 .doc 是旧版 Word 格式（非 .docx），可能需要借助外部工具（如 pandoc 或 LibreOffice）进行转换。如果无法直接读取，将在实施时尝试使用可用的转换方法。

## Assumptions & Decisions

1. Logo 路径使用 `Data/Vue/logo.png` 相对路径，确保 GitHub 正确渲染
2. README 使用中英双语标题，但正文以中文为主（符合课程项目定位）
3. 指导书转换将尽可能保留原始结构和关键信息
4. 前端尚未创建，README 中会明确标注此状态

## Verification

1. README.md 生成后检查：
   - Markdown 语法正确性
   - 图片路径有效性
   - 徽章链接有效性
   - 结构完整性（10 个必需章节全部包含）
2. 00-Course-Guidelines.md 生成后检查：
   - Markdown 格式正确
   - 标题层级清晰
   - 内容完整无遗漏
