# 在线学习系统 - 全栈开发与部署标准工作流 (SOP)

> **文档说明**：本文档定义了基于“数据驱动设计”与“契约优先”理念的全栈开发标准流程。适用于 Spring Boot + Vue3 + SQLite 技术栈，并深度结合 AI 辅助编程（Vibe Coding）的最佳实践。

## 🛠️ 技术栈概览

*   **后端**：Spring Boot , Spring Data JPA, SQLite
*   **前端**：Vue 3 (Composition API), Vite, Vue Router, Pinia, Element Plus, Axios
*   **部署**：Docker, Docker Compose, Nginx
*   **工具**：IDEA, VS Code, DataGrip/Navicat, Apifox/Postman

---

## 阶段一：顶层设计（谋定而后动）

### 1. 数据结构设计（核心基石）

*   **核心理念**：数据驱动设计。在写任何代码前，优先确定业务实体及其关联关系。
*   **执行动作**：
    *   梳理业务需求，设计 ER 图（实体关系图）。
    *   确定表结构、字段类型、主外键约束、索引。
    *   **AI 辅助**：输入业务描述，让 AI 生成标准的 DDL（建表 SQL 语句）。
*   **交付物**：`schema.sql` 数据库初始化脚本。

### 2. 前后端接口规范设计（API 契约）

*   **核心理念**：契约优先（Contract-First）。接口规范是指导后端 Controller 编写和前端 Axios 请求的**唯一真理**。
*   **执行动作**：
    *   定义 RESTful 风格的 URL 命名规范（如 `/api/v1/courses`）。
    *   定义统一的 HTTP 状态码使用规范。
    *   **定义全局统一的数据响应格式**（极其重要）：
      ```json
      // 成功响应
      { "code": 200, "message": "success", "data": { ... } }
      // 失败响应
      { "code": 500, "message": "用户名或密码错误", "data": null }
      ```
*   **交付物**：API 接口文档（推荐使用 Apifox 或 Swagger/Knife4j 管理）。

---

## 阶段二：基础设施搭建（磨刀不误砍柴工）

### 3. 数据库安装配置及联通测试

*   **执行动作**：
    *   在本地项目根目录创建 `data/db/` 文件夹，初始化 SQLite 数据库文件。
    *   执行 `schema.sql` 创建表结构。
    *   使用 DataGrip 或 Navicat 连接数据库，验证表结构和字段类型是否与设计一致。

### 4. 项目创建与基础配置
*   **后端初始化**：
    *   使用 Spring Initializr 创建项目，引入 Web, Lombok, SQLite 驱动等依赖。
    *   配置 `application-dev.yml`（开发环境），配置数据库连接池。
    *   编写全局统一返回结果类 `Result<T>` 和全局异常处理器 `@RestControllerAdvice`。
*   **前端初始化**：
    *   使用 `npm create vite@latest` 创建 Vue3 项目。
    *   安装核心依赖：`vue-router`, `pinia`, `element-plus`, `axios`。
    *   配置 Vite 代理（Proxy）解决开发环境跨域问题。
    *   配置 Axios 全局响应拦截器，统一处理后端返回的 `Result<T>` 格式和 Token 注入。

---

## 阶段三：后端核心开发（自底向上，层层推进）

### 5. 后台分层开发（逐层实现与验证）
*   **执行动作**：严格按照 MVC 三层架构，自底向上开发。
    1.  **实体类 (Entity)**：与数据库表一一映射（可利用 AI 根据 SQL 直接生成）。
    2.  **数据层 (Mapper/Dao)**：编写 MyBatis-Plus 接口或自定义 SQL。
    3.  **服务层 (Service)**：编写核心业务逻辑（如：选课人数校验、密码加密、事务控制）。
    4.  **接口层 (Controller)**：**严格按照阶段一定义的 API 契约暴露接口**，返回值统一使用 `Result<T>`。
*   **测试闭环**：每完成一个业务模块（如“用户管理”），**必须立即**使用 Apifox/Postman 发送请求进行接口测试。确保后端逻辑 100% 健壮后，再进入下一个模块或前端开发。
*   **AI 辅助技巧**：完成第一个模块后，将其作为 Context（上下文）喂给 AI，要求 AI 按照相同的代码风格和分层规范生成后续模块的 CRUD 代码。

---

## 阶段四：前端开发与联调（所见即所得）

### 6. 前端页面与交互开发
*   **执行动作**：
    *   **布局搭建**：使用 Element Plus 搭建系统整体框架（侧边栏菜单、顶部导航、面包屑）。
    *   **静态页面**：开发各个业务视图（表格、表单、弹窗）。
    *   **数据绑定**：调用后端 API，将真实数据渲染到页面。
    *   **交互逻辑**：处理表单校验、按钮防抖、路由守卫（权限拦截）。

### 7. 系统整体联调测试
*   **执行动作**：前后端同时启动，在浏览器中进行端到端（E2E）的真实业务流程测试。
*   **测试重点**：
    *   核心主流程（如：注册 -> 登录 -> 创建课程 -> 学生选课 -> 查看成绩）。
    *   边界异常情况（如：上传超大文件、输入非法特殊字符、并发操作）。
    *   前端路由刷新 404 问题、跨域问题是否彻底解决。

---

## 阶段五：交付与部署（完美收官）

### 8. Docker 构建与一键打包
*   **执行动作**：
    *   编写后端 `Dockerfile`（多阶段构建，减小镜像体积）。
    *   编写前端 `Dockerfile`（使用 Nginx 托管静态资源并配置反向代理）。
    *   编写 `docker-compose.yml` 编排文件，配置网络互通与 SQLite 数据卷（Volume）挂载。
*   **交付验证**：在本地清理所有运行中的服务，仅通过执行 `docker-compose up -d --build`，验证系统能否实现“开箱即用”的完美运行。

---

## 💡 实操避坑锦囊（全栈单人开发必看）

1.  **统一返回结果类（Result.java）**
    所有的 Controller 方法，返回值**必须**是 `Result<T>`。这会让你的前端 Axios 拦截器写得极其优雅，避免到处写 `if/else` 判断 HTTP 状态码。
2.  **全局异常处理器（GlobalExceptionHandler.java）**
    利用 `@RestControllerAdvice` 捕获所有运行时异常（如参数校验失败、业务自定义异常），统一包装成 `Result.error()` 返回。**绝对不要**让后端直接抛出 500 错误页面或原生 Java 堆栈信息给前端。
3.  **SQLite 的外键陷阱**
    SQLite 默认是**关闭外键约束**的！如果设计了表关联，必须在 Spring Boot 连接池初始化时手动开启：
    ```yaml
    spring:
      datasource:
        hikari:
          connection-init-sql: "PRAGMA foreign_keys = ON"
    ```
4.  **前端 API 请求永远使用“相对路径”**
    前端 Axios 的 `baseURL` 永远只写 `/api`，**不要写死** `http://localhost:8080`。将代理工作交给开发环境的 Vite 和生产环境的 Nginx，实现业务代码的零环境侵入。
5.  **善用 `.gitignore` 和 `.dockerignore`**
    千万不要把 `node_modules`、`target`、`.idea` 以及 **`data/db/*.db` (数据库文件)** 提交到 Git 仓库。数据文件应通过 Docker Volume 或本地备份进行管理。