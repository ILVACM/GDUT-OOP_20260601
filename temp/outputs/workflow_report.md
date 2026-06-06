# 本地 Agent 全流程辅助开发工作流 — 结构化要点报告

> 项目名称：在线学习系统（English Online Learning System）
> 技术栈：Spring Boot 4.0.6 + JPA + SQLite + Vue 3 + TypeScript + Vite
> 报告日期：2026-06-05
> 生成路径：`temp/outputs/workflow_report.md`
> 
> **合规声明**：本报告引用的所有源文件路径均标注于"原始路径"列，生成过程未对任何源文件进行写入或修改操作。

---

## 1. 核心组件清单

### 1.1 知识库层（Agent 上下文的"单一真理源"）

| 组件名称 | 原始路径 | 功能 | Agent 使用时机 |
|----------|----------|------|----------------|
| 项目总索引 | `wiki/00-INDEX.md` | 技术栈矩阵、功能矩阵、模块索引、AI 协作边界 | 任何任务第一步 |
| 全局开发规范 | `wiki/01-Global-Standards.md` | API 契约、异常处理、JPA 规范、分层标准 | 任何编码任务前 |
| 数据字典 | `wiki/02-Data-Dictionary.md` | 表结构、字段映射、JSON 扩展、JPA 约束 | 涉及 Entity/Repository 时 |
| M01 模块文档 | `wiki/modules/M01-User-Auth.md` | 用户认证与权限管理 | 用户相关任务 |
| M02 模块文档 | `wiki/modules/M02-Question-Bank.md` | 题库管理 | 题目相关任务 |
| M03 模块文档 | `wiki/modules/M03-Exam-Assembly.md` | 考试与组卷 | 考试相关任务 |
| M04 模块文档 | `wiki/modules/M04-Score-Statistics.md` | 分数与统计 | 分数相关任务 |
| SQLite 优化参考 | `wiki/references/SQLite-Optimization.md` | PRAGMA 配置、并发陷阱 | 数据库调优任务 |

### 1.2 规则层（强制约束 Agent 行为边界）

| 组件名称 | 原始路径 | 功能 |
|----------|----------|------|
| 开发指导规则 | `.trae/rules/开发指导.md` | 渐进式上下文加载协议、绝对禁止事项 |
| 技术栈硬约束 | `wiki/01-Global-Standards.md §1` | 禁止 MyBatis、禁止 Vue2 等 |
| JPA 强制规范 | `wiki/01-Global-Standards.md §4.1` | Entity/DTO 隔离、N+1 防护 |

### 1.3 Spec 层（任务规格说明）

| 组件名称 | 原始路径 | 功能 |
|----------|----------|------|
| RESTful API 设计 | `.trae/specs/design-restful-api/spec.md` | API 契约设计规格 |
| 业务逻辑层实现 | `.trae/specs/implement-business-logic-layer/spec.md` | 14 个 Requirement + 46 个 Scenario |
| 业务逻辑缺口修复 | `.trae/specs/fix-business-logic-gaps/spec.md` | 补充业务逻辑实现 |
| Vue3 前端构建 | `.trae/specs/build-vue3-frontend/spec.md` | 前端构建规格 |

### 1.4 基础设施层（配置与脚本）

| 组件名称 | 原始路径 | 功能 |
|----------|----------|------|
| 生产配置 | `backend/src/main/resources/application.yaml` | SQLite PRAGMA/JPA/连接池 |
| 测试配置 | `backend/src/test/resources/application-test.yaml` | 测试环境独立配置 |
| DDL 脚本 | `scripts/table_*.sql` | 4 张核心表建表语句 |
| Maven 配置 | `backend/pom.xml` | 依赖与构建 |
| npm 配置 | `frontend-Vue/package.json` | 前端依赖 |

---

## 2. 流程拓扑描述

### 2.1 五阶段工作流

```
阶段一：顶层设计
  ├── 1. 数据结构设计 → 交付物：scripts/table_*.sql
  └── 2. API 契约设计 → 交付物：wiki/ 文档 + .trae/specs/
  
阶段二：基础设施搭建
  ├── 3. 数据库初始化 → PRAGMA 配置 + DDL 执行
  └── 4. 项目初始化 → pom.xml + package.json + application.yaml
  
阶段三：后端核心开发
  ├── 5.1 数据层 → Entity + Enum + Repository（垂直分割 M01-M04）
  ├── 5.2 基础设施 → Result<T> + GlobalExceptionHandler + JWT
  ├── 5.3 业务层 → Service + DTO（14 Requirements, 46 Scenarios）
  ├── 5.4 接口层 → Controller（40 端点 + 统一返回）
  └── 5.5 测试闭环 → 73 个单元测试全部通过
  
阶段四：前端开发与联调
  ├── 6.1 Vue3 项目初始化
  ├── 6.2 路由/状态/API 封装
  └── 6.3 页面组件开发（14 views）
  
阶段五：交付与部署
  └── Docker 构建 + Nginx 反向代理
```

### 2.2 Agent 协作拓扑

```
用户（需求提出者）
    │
    ├── 第一阶段：需求导入 + 方案设计（人机协同）
    │   ├── 用户确定技术栈、业务功能矩阵
    │   ├── Agent 辅助生成 wiki/ 文档体系
    │   └── 用户评审并确认文档
    │
    ├── 第二阶段：构建 Agent 引导语料库（人机协同）
    │   ├── 用户/Agent 共同编写 .trae/rules/开发指导.md
    │   ├── 用户/Agent 共同编写 .trae/specs/*.md（Requirement + Scenario）
    │   └── 形成渐进式上下文加载协议
    │
    ├── 第三阶段：Agent 自主执行开发（Agent 自治）
    │   ├── Agent 读取 wiki/ + rules/ + specs/
    │   ├── Agent 按模块独立开发 M01→M02→M03→M04
    │   ├── Agent 生成 Entity/Repository/Service/Controller/DTO
    │   ├── Agent 编写单元测试
    │   └── Agent 自动运行测试验证
    │
    └── 第四阶段：成果验收 + 迭代修复（人机协同）
        ├── 用户审查代码 + 测试报告
        ├── 用户反馈问题
        └── Agent 修复并重新验证
```

---

## 3. 人机协作接口分析

### 3.1 需求导入接口

| 接口点 | 人类角色 | Agent 角色 | 交互产物 |
|--------|----------|------------|----------|
| 顶层设计 | 确定业务功能、技术选型 | 辅助生成 ER 图、DDL、文档模板 | wiki/ 文档 |
| API 契约 | 确认接口清单、权限矩阵 | 生成 RESTful 规范、DTO 草案 | spec.md + 文档 |
| 规格说明 | 评审 Requirement/Scenario | 编写结构化测试场景 | .trae/specs/*.md |

### 3.2 进度同步接口

| 同步机制 | 实现方式 |
|----------|----------|
| 任务清单 | `.trae/specs/*/tasks.md` 定义 6 主任务 + 18 子任务 |
| 检查清单 | `.trae/specs/*/checklist.md` 定义 46 个验证点 |
| 状态追踪 | wiki/ 文档中的"实现状态"章节 |
| 版本记录 | wiki/ 文档中的"版本与变更记录" |

### 3.3 成果验收接口

| 验收维度 | 验收方式 |
|----------|----------|
| 代码编译 | `mvn compile` 退出码 = 0 |
| 单元测试 | `mvn test` 73 个测试全部通过 |
| API 契约 | Controller 端点与 spec.md 逐一对齐 |
| 文档一致性 | Entity 字段与 Data-Dictionary 对齐 |
| 规则遵守 | 无 Entity 裸奔、无 N+1、无硬编码 |

---

## 4. Agent 自治能力边界

### 4.1 自主决策范围（Agent 可独立完成）

| 开发环节 | 自治内容 |
|----------|----------|
| Entity 生成 | 根据 Data-Dictionary 自动生成字段、注解 |
| Repository 编写 | 根据查询需求生成 Spring Data 方法名/JPQL |
| DTO 转换 | Req→Entity、Entity→VO 的 Record 定义 |
| Service 逻辑 | 业务校验、事务控制、判分算法 |
| Controller 编写 | 统一 Result\<T\> 包装、@RequireRole 注解 |
| 单元测试 | Mock Repository、验证业务路径 |
| 异常处理 | 业务码映射、全局异常处理器适配 |

### 4.2 人机协同边界（必须人类介入）

| 环节 | 原因 |
|------|------|
| 技术栈选型 | 影响整个项目架构 |
| 数据库表结构设计 | 影响所有下游开发 |
| 业务功能矩阵定义 | 决定系统边界 |
| wiki 文档重大变更 | 影响 Agent 后续所有行为 |
| 第三方依赖引入 | 影响兼容性 |
| API 契约变更 | 影响前后端联调 |
| 生产部署方案 | 涉及运维环境 |

---

## 5. 质量控制机制

### 5.1 内置代码审查规则（来自 wiki + rules）

| 规则编号 | 规则内容 | 检查点 |
|----------|----------|--------|
| R1 | 禁止 Entity 裸奔 | Controller 返回值必须是 Result\<T\> |
| R2 | 禁止 N+1 查询 | 关联查询必须使用 findAllById |
| R3 | 禁止脱离文档造轮子 | 必须遵循 Result\<T\> + GlobalExceptionHandler |
| R4 | 禁止破坏数据字典 | Entity 字段必须与 02-Data-Dictionary 对齐 |
| R5 | 禁止上下文贪吃 | 必须使用渐进式加载协议 |
| R6 | 事务控制 | 写操作必须加 @Transactional |
| R7 | 前端规范 | baseURL 不能硬编码、必须配置拦截器 |

### 5.2 测试用例生成策略

| 测试层级 | 覆盖内容 | 工具 |
|----------|----------|------|
| Repository 测试 | CRUD、自定义查询、增量更新 | Spring Data JPA Test |
| Service 测试 | 正常路径 + 关键异常路径 | @MockBean + @SpringBootTest |
| 集成测试 | 启动验证、配置加载 | application-test.yaml |

### 5.3 反馈闭环

```
Agent 生成代码 → mvn compile → 失败 → 修复 → 重新编译
Agent 生成代码 → mvn test → 失败 → 分析失败 → 修复 → 重新测试
Agent 完成模块 → 检查 checklist.md → 逐项打勾 → 生成报告
```

---

## 6. 可配置性设计

### 6.1 技术栈适配参数

| 参数 | 配置位置 | 示例值 |
|------|----------|--------|
| JDK 版本 | `backend/pom.xml` | 21 |
| Spring Boot 版本 | `backend/pom.xml` | 4.0.6 |
| 数据库类型 | `application.yaml` | SQLite / MySQL / PostgreSQL |
| ORM 框架 | `backend/pom.xml` | JPA（禁止 MyBatis） |
| 前端框架 | `frontend-Vue/package.json` | Vue 3 |

### 6.2 项目类型兼容

| 维度 | 当前值 | 可替换方案 |
|------|--------|------------|
| 业务领域 | 在线学习系统 | 任意 CRUD+权限系统 |
| 数据库 | SQLite | MySQL/PostgreSQL（需改 DDL+方言） |
| 认证方式 | JWT | Session/OAuth2 |
| UI 组件库 | Element Plus | Ant Design Vue/Naive UI |

### 6.3 工作流参数化

| 参数 | 描述 | 配置位置 |
|------|------|----------|
| 模块数量 | 4 (M01-M04) | 可按需增减 |
| 测试用例数 | 73 | 按复杂度调整 |
| API 端点数 | 40 | 按功能调整 |
| 角色数量 | 3 (student/teacher/admin) | 按需求调整 |

---

## 7. 最佳实践建议

### 7.1 Prompt 设计原则

1. **明确约束**：在 prompt 中显式声明"必须遵循 wiki/ 文档"，禁止自由发挥
2. **结构化输入**：使用 Requirement + Scenario 格式定义任务
3. **渐进式加载**：要求 Agent 先读全局基座，再按需加载模块文档
4. **Think Aloud**：要求 Agent 外化思考过程，使用 `<context_loading>` 标签
5. **验收标准**：在 prompt 中明确编译通过、测试通过等量化指标

### 7.2 规则集版本管理

1. **文档即代码**：wiki/ 和 rules/ 文件纳入 Git 管理
2. **版本记录**：每次重大变更在文档末尾添加版本记录
3. **向后兼容**：新增规则不删除旧规则，标记为"已废弃"
4. **冲突检测**：新增规则必须与现有 Red Lines 无冲突

### 7.3 效能优化技巧

1. **一次配置，多次复用**：.trae/rules/开发指导.md 作为 workspace 规则自动生效
2. **模块化开发**：按 M01→M02→M03→M04 独立开发，降低上下文复杂度
3. **先文档后代码**：wiki 文档完善后再让 Agent 生成代码，减少返工
4. **测试驱动**：先写测试场景（Scenario），再让 Agent 实现
5. **自动化验证**：每次 Agent 完成模块后自动运行 mvn test

---

## 8. 风险点与补充数据建议

| 风险编号 | 风险描述 | 影响范围 | 建议措施 |
|----------|----------|----------|----------|
| RISK-1 | wiki 文档版本不同步 | Agent 可能生成过时代码 | 建立文档-代码同步审查机制 |
| RISK-2 | 前端工程尚未完整实现 | 联调阶段可能发现 API 契约偏差 | 前端开发完成后补充 API 联调测试 |
| RISK-3 | SQLite 并发写入瓶颈 | 多学生同时提交答卷可能 SQLITE_BUSY | 考虑异步处理或消息队列 |
| RISK-4 | 缺少 E2E 集成测试 | 仅单元测试覆盖，缺少端到端验证 | 补充 Cypress/Playwright E2E 测试 |
| RISK-5 | Docker 部署未实现 | 部署流程缺乏验证 | 补充 Dockerfile + docker-compose.yml |

---

## 9. 引用文件清单（可追溯性）

| 序号 | 原始路径 | 引用目的 |
|------|----------|----------|
| 1 | `wiki/00-INDEX.md` | 项目总索引 |
| 2 | `wiki/01-Global-Standards.md` | 全局规范 |
| 3 | `wiki/02-Data-Dictionary.md` | 数据字典 |
| 4 | `wiki/modules/M01-User-Auth.md` | M01 模块文档 |
| 5 | `wiki/modules/M02-Question-Bank.md` | M02 模块文档 |
| 6 | `wiki/modules/M03-Exam-Assembly.md` | M03 模块文档 |
| 7 | `wiki/modules/M04-Score-Statistics.md` | M04 模块文档 |
| 8 | `wiki/references/SQLite-Optimization.md` | SQLite 优化参考 |
| 9 | `.trae/rules/开发指导.md` | AI 协作规则 |
| 10 | `.trae/specs/implement-business-logic-layer/spec.md` | 业务逻辑规格书 |
| 11 | `.trae/specs/design-restful-api/spec.md` | API 设计规格书 |
| 12 | `.trae/specs/fix-business-logic-gaps/spec.md` | 业务缺口修复规格书 |
| 13 | `.trae/specs/build-vue3-frontend/spec.md` | 前端规格书 |
| 14 | `temp/在线学习系统-全栈开发工作流 (SOP).md` | 工作流 SOP |
| 15 | `backend/src/main/resources/application.yaml` | 生产配置 |
| 16 | `backend/src/test/resources/application-test.yaml` | 测试配置 |
| 17 | `backend/pom.xml` | Maven 配置 |
| 18 | `frontend-Vue/package.json` | npm 配置 |
| 19 | `frontend-Vue/src/utils/request.ts` | 前端 Axios 封装 |
| 20 | `scripts/table_*.sql` | DDL 脚本 |
