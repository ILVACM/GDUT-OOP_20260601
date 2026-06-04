# Vue3 前端全栈搭建 Spec

## Why
后端 API 全链路已完成（39 个端点，73 个测试通过），但前端项目仅存在 Vue CLI 脚手架默认模板，无任何业务代码。需要从零构建工程化规范、权限严密、交互流畅的在线考试系统前端，对接后端全部 API。

## What Changes
- 重构 `frontend-Vue` 项目：引入 TypeScript、Element Plus、Axios、pinia-plugin-persistedstate
- 搭建 3 套 Layout 嵌套路由（基础 Layout / 空白 Layout / 沉浸式 Layout）
- 实现路由级 + 按钮级双重 RBAC 权限控制（admin / teacher / student）
- 封装 Axios 请求工具类（Token 注入 + Result<T> 统一解包 + 错误提示）
- 实现 Pinia 状态管理（UserStore / PaperStore / ExamStore）并配置持久化
- 实现全部业务页面（登录注册、首页、题库、考试、答题、数据统计、个人中心）
- 对接后端 39 个 API 端点

## Impact
- Affected specs: design-restful-api（前端需完全对齐该 spec 定义的 39 个端点）
- Affected code: `frontend-Vue/` 整个目录重构
- 依赖: 后端 API 已就绪，Vite proxy 配置 `/api` 代理到后端 8080 端口

---

## ADDED Requirements

### Requirement: 项目基础设施
系统 SHALL 在 `frontend-Vue` 目录下搭建基于 Vue 3 + TypeScript + Vite 的前端工程。

#### Scenario: 项目初始化
- **WHEN** 开发者执行 `npm install` 并启动开发服务器
- **THEN** 项目 SHALL 成功编译运行，且包含以下依赖：
  - Vue 3 (Composition API + `<script setup>`)
  - TypeScript（严格类型检查）
  - Element Plus（UI 组件库）
  - Pinia + pinia-plugin-persistedstate（状态管理 + 持久化）
  - Vue Router 4（路由）
  - Axios（HTTP 请求）

#### Scenario: 目录结构规范
- **WHEN** 项目初始化完成
- **THEN** `src/` 目录 SHALL 包含以下子目录：
  - `api/` - 按模块划分的 Axios 调用文件
  - `views/` - 页面级组件
  - `components/` - 通用组件
  - `stores/` - Pinia stores
  - `router/` - 路由配置
  - `utils/` - 工具函数
  - `types/` - TypeScript 接口定义
  - `layouts/` - Layout 组件
  - `directives/` - 自定义指令

### Requirement: Axios 封装
系统 SHALL 封装 Axios 请求工具类，满足 wiki 01-Global-Standards.md §7.3 规范。

#### Scenario: 请求拦截器自动注入 Token
- **WHEN** 用户已登录且发起 HTTP 请求
- **THEN** 请求拦截器 SHALL 自动在 `Authorization` 头注入 `Bearer <token>`

#### Scenario: 响应拦截器统一解包 Result<T>
- **WHEN** 后端返回 `{ code: 200, message: "success", data: {...} }`
- **THEN** 响应拦截器 SHALL 自动解包，仅返回 `data` 字段给调用方
- **WHEN** 后端返回 `{ code: 4xxx, message: "错误信息", data: null }`
- **THEN** 响应拦截器 SHALL 使用 `ElMessage.error` 显示错误信息并 reject

#### Scenario: baseURL 配置
- **WHEN** Axios 实例创建
- **THEN** `baseURL` SHALL 配置为 `/api`（严禁硬编码 `http://localhost:8080`）

#### Scenario: Vite Proxy 配置
- **WHEN** 开发环境启动
- **THEN** `vite.config.ts` SHALL 配置 `server.proxy['/api']` 代理到 `http://localhost:8080`

### Requirement: 多套 Layout 嵌套路由
系统 SHALL 实现 3 套 Layout 组件，通过 Vue Router 嵌套路由组织页面。

#### Scenario: 基础 Layout（带侧边导航）
- **WHEN** 用户访问首页、题库页、考试查看页、数据查看页、个人中心
- **THEN** 页面 SHALL 渲染在基础 Layout 内，包含左侧导航栏和顶部用户信息

#### Scenario: 空白 Layout
- **WHEN** 用户访问登录页、注册页
- **THEN** 页面 SHALL 渲染在空白 Layout 内，居中卡片式设计，无导航栏

#### Scenario: 沉浸式 Layout
- **WHEN** 学生进入答题页
- **THEN** 页面 SHALL 渲染在沉浸式 Layout 内，隐藏侧边栏和顶部导航，提供全屏倒计时和交卷按钮

### Requirement: RBAC 权限控制
系统 SHALL 实现路由级和按钮级双重权限控制。

#### Scenario: 路由守卫拦截
- **WHEN** 未登录用户访问需鉴权页面
- **THEN** 路由守卫 SHALL 重定向到登录页
- **WHEN** 学生访问仅教师/管理员可用的页面（如题库管理、用户管理）
- **THEN** 路由守卫 SHALL 拦截并提示无权限

#### Scenario: 按钮级权限控制
- **WHEN** 模板中使用 `v-permission="['teacher', 'admin']"` 指令
- **THEN** 若当前用户角色不在列表中，该按钮 SHALL 被隐藏

#### Scenario: 角色菜单差异
- **WHEN** 管理员登录
- **THEN** 左侧菜单 SHALL 显示：首页、题库管理、考试管理、数据统计、用户管理、个人中心
- **WHEN** 教师登录
- **THEN** 左侧菜单 SHALL 显示：首页、题库管理、考试管理、数据统计、个人中心
- **WHEN** 学生登录
- **THEN** 左侧菜单 SHALL 显示：首页、我的考试、我的成绩、个人中心

### Requirement: Pinia 状态管理与持久化
系统 SHALL 使用 Pinia 管理全局状态，关键 Store 配置 LocalStorage 持久化。

#### Scenario: UserStore
- **WHEN** 用户登录成功
- **THEN** UserStore SHALL 存储 token、用户信息（id/name/type/status）
- **THEN** 数据 SHALL 持久化到 LocalStorage，刷新页面不丢失
- **WHEN** 用户注销
- **THEN** UserStore SHALL 清空所有数据

#### Scenario: PaperStore（组卷缓存）
- **WHEN** 教师在组卷流程中选择题目
- **THEN** PaperStore SHALL 缓存已选题目列表和试卷基础信息
- **THEN** 支持 add / remove / reorder 操作
- **THEN** 数据 SHALL 持久化到 LocalStorage，防刷新丢失
- **WHEN** 组卷提交成功
- **THEN** PaperStore SHALL 清空缓存

#### Scenario: ExamStore（答题缓存）
- **WHEN** 学生进入答题页
- **THEN** ExamStore SHALL 初始化试卷结构（题目列表、分值）
- **WHEN** 学生选择/输入答案
- **THEN** ExamStore SHALL 实时更新 `{ questionId, answer }` 记录
- **THEN** 数据 SHALL 持久化到 LocalStorage，支持断点续答
- **WHEN** 学生交卷成功
- **THEN** ExamStore SHALL 清空缓存

### Requirement: API 模块化封装
系统 SHALL 按后端模块划分 API 调用文件，每个文件对应一个业务模块。

#### Scenario: API 文件划分
- **WHEN** 前端项目构建
- **THEN** `src/api/` 目录 SHALL 包含：
  - `auth.ts` - 用户认证（login/register/logout/me/password）
  - `user.ts` - 用户管理（CRUD/状态切换/批量删除）
  - `question.ts` - 题库管理（CRUD/批量导入/随机获取/批量删除）
  - `exam.ts` - 考试与组卷（手动/自动组卷/发布/撤回/预览/草稿）
  - `score.ts` - 分数与统计（提交答卷/评卷/成绩查询/错题集/统计报表）

#### Scenario: TypeScript 接口定义
- **WHEN** API 调用涉及请求/响应数据
- **THEN** 所有数据结构 SHALL 在 `src/types/` 中定义 TypeScript Interface
- **THEN** 接口定义 SHALL 与后端 DTO/VO 严格对齐

### Requirement: 核心业务页面
系统 SHALL 实现以下核心页面，对接后端 API。

#### Scenario: 登录页
- **WHEN** 用户提交登录表单
- **THEN** 调用 `POST /api/v1/auth/login`，成功后存储 token 并跳转首页
- **WHEN** 登录失败
- **THEN** 显示错误提示（用户名或密码错误）

#### Scenario: 注册页
- **WHEN** 学生提交注册表单
- **THEN** 调用 `POST /api/v1/auth/register`（仅 student 角色），成功后跳转登录页

#### Scenario: 首页
- **WHEN** 不同角色登录
- **THEN** 首页 SHALL 显示角色相关的概览信息（学生：待考考试/最近成绩；教师：考试统计/待评卷；管理员：系统概览）

#### Scenario: 题库管理页
- **WHEN** 教师/管理员访问题库页
- **THEN** 显示题目分页列表，支持按类型/关键字筛选
- **THEN** 显示创建/编辑/删除操作按钮
- **WHEN** 学生访问题库页
- **THEN** 无权限访问（路由守卫拦截）

#### Scenario: 考试管理页（教师/管理员）
- **WHEN** 教师/管理员访问考试列表
- **THEN** 显示考试分页列表，支持按状态/时间筛选
- **THEN** 显示创建考试（手动/自动组卷）、发布、撤回、删除操作

#### Scenario: 我的考试页（学生）
- **WHEN** 学生访问考试列表
- **THEN** 调用 `GET /api/v1/exams/available`，显示可参加的考试
- **THEN** 对 running 状态考试显示"进入答题"按钮

#### Scenario: 答题页（沉浸式）
- **WHEN** 学生进入答题页
- **THEN** 页面使用沉浸式 Layout，显示全屏倒计时
- **THEN** 从 ExamStore 读取/保存答题进度
- **THEN** 每次答案变更实时更新 ExamStore + 调用草稿保存 API
- **WHEN** 倒计时结束或学生点击交卷
- **THEN** 调用 `POST /api/v1/exams/{examId}/submit` 提交答卷
- **THEN** 提交成功后跳转结果页

#### Scenario: 组卷页/弹窗
- **WHEN** 教师创建手动组卷
- **THEN** 从题库选题（支持搜索/筛选），添加到 PaperStore
- **THEN** 可调整题目顺序和分值
- **WHEN** 教师创建自动组卷
- **THEN** 填写抽题规则（题目数/总分/题型过滤/频次降权）
- **THEN** 调用 `POST /api/v1/exams/auto` 创建考试

#### Scenario: 数据统计页
- **WHEN** 教师/管理员访问统计页
- **THEN** 显示考试统计（通过率/成绩分布）和题目统计（正确率排名）
- **WHEN** 学生访问成绩页
- **THEN** 显示个人成绩列表和错题集

#### Scenario: 个人中心页
- **WHEN** 用户访问个人中心
- **THEN** 显示当前用户信息，支持修改密码
- **WHEN** 管理员访问
- **THEN** 额外显示用户管理功能（CRUD/状态切换/批量删除）

### Requirement: 表单防抖
系统 SHALL 对所有表单提交按钮实现防抖处理。

#### Scenario: 防抖提交
- **WHEN** 用户连续点击提交按钮
- **THEN** 系统 SHALL 在 500ms 内仅执行一次提交请求
