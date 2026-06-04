# Tasks

## 阶段一：项目骨架与基础设施搭建

- [x] Task 1: 重构项目配置与依赖安装
  - [x] SubTask 1.1: 安装新增依赖（element-plus, axios, pinia-plugin-persistedstate, @types/node, typescript, vue-tsc 等）
  - [x] SubTask 1.2: 添加 TypeScript 配置文件（tsconfig.json, tsconfig.app.json, tsconfig.node.json）
  - [x] SubTask 1.3: 将 vite.config.js 迁移为 vite.config.ts，配置 Vite Proxy（`/api` → `http://localhost:8080`）和路径别名
  - [x] SubTask 1.4: 更新 index.html 和 package.json 配置

- [x] Task 2: 搭建目录结构与 TypeScript 类型定义
  - [x] SubTask 2.1: 创建 `src/types/` 目录，定义所有 TypeScript Interface（UserVO, LoginReq/Resp, QuestionVO, ExamVO, ScoreVO, PageResult, Result 等全部 API 数据结构）
  - [x] SubTask 2.2: 创建 `src/api/`、`src/views/`、`src/components/`、`src/stores/`、`src/router/`、`src/utils/`、`src/layouts/`、`src/directives/` 目录结构

- [x] Task 3: 封装 Axios 请求工具类
  - [x] SubTask 3.1: 创建 `src/utils/request.ts`，实现 Axios 实例（baseURL: `/api`，timeout: 10000）
  - [x] SubTask 3.2: 实现请求拦截器（自动注入 JWT Token）
  - [x] SubTask 3.3: 实现响应拦截器（统一解包 Result<T>，code=200 返回 data，否则 ElMessage.error 并 reject）
  - [x] SubTask 3.4: 处理 4101/4102 状态码（未登录/Token 过期，跳转登录页）

- [x] Task 4: 配置 Vue Router 基础路由表及多套 Layout 组件
  - [x] SubTask 4.1: 创建 `src/layouts/BasicLayout.vue`（左侧导航 + 顶部用户信息 + 主内容区）
  - [x] SubTask 4.2: 创建 `src/layouts/BlankLayout.vue`（空白居中卡片式）
  - [x] SubTask 4.3: 创建 `src/layouts/ExamLayout.vue`（沉浸式，隐藏导航，全屏倒计时 + 交卷按钮）
  - [x] SubTask 4.4: 创建 `src/router/index.ts`，定义嵌套路由表（3 套 Layout 对应路由分组）
  - [x] SubTask 4.5: 创建各页面占位组件（HomeView, LoginView, RegisterView, QuestionView, ExamView, ExamTakingView, DataView, ProfileView 等）

- [x] Task 5: 配置 main.ts 入口文件
  - [x] SubTask 5.1: 注册 Element Plus（全量导入）、Pinia（含持久化插件）、Vue Router
  - [x] SubTask 5.2: 清理脚手架默认组件（HelloWorld, TheWelcome 等）

## 阶段二：权限系统与状态管理 (Stores)

- [x] Task 6: 实现 UserStore
  - [x] SubTask 6.1: 创建 `src/stores/user.ts`，定义 state（token, user: UserVO | null）、getters（isLoggedIn, userType, userName）、actions（login, logout, fetchCurrentUser, changePassword）
  - [x] SubTask 6.2: 配置 pinia-plugin-persistedstate，持久化 token 和 user 到 LocalStorage

- [x] Task 7: 实现路由守卫
  - [x] SubTask 7.1: 在 `router/index.ts` 中实现 `beforeEach` 守卫：未登录重定向到 /login；已登录访问 /login 重定向到首页
  - [x] SubTask 7.2: 实现角色权限校验：路由 meta 中声明 requiredRoles，守卫中校验当前用户角色

- [x] Task 8: 实现 PaperStore（组卷缓存）
  - [x] SubTask 8.1: 创建 `src/stores/paper.ts`，定义 state（examName, starttime, endtime, items: PaperItem[]）、actions（addItem, removeItem, reorderItems, updateItem, clearAll, submit）
  - [x] SubTask 8.2: 配置持久化

- [x] Task 9: 实现 ExamStore（答题缓存）
  - [x] SubTask 9.1: 创建 `src/stores/exam.ts`，定义 state（examId, examInfo, answers: Map<number, any>, remainingTime）、actions（initExam, updateAnswer, submitExam, clearExam）
  - [x] SubTask 9.2: 配置持久化，支持断点续答

- [x] Task 10: 封装 v-permission 按钮级权限指令
  - [x] SubTask 10.1: 创建 `src/directives/permission.ts`，实现 v-permission 自定义指令
  - [x] SubTask 10.2: 在 main.ts 中全局注册指令

- [x] Task 11: 封装 API 模块
  - [x] SubTask 11.1: 创建 `src/api/auth.ts`（login, register, logout, me, changePassword）
  - [x] SubTask 11.2: 创建 `src/api/user.ts`（listUsers, createUser, updateUser, updateUserStatus, deleteUser, batchDeleteUsers）
  - [x] SubTask 11.3: 创建 `src/api/question.ts`（create, batchCreate, getById, list, update, delete, batchDelete, getRandom）
  - [x] SubTask 11.4: 创建 `src/api/exam.ts`（createManual, createAuto, getById, getPreview, update, publish, withdraw, delete, list, listAvailable, saveDraft, loadDraft）
  - [x] SubTask 11.5: 创建 `src/api/score.ts`（submitExam, gradeEssay, getMyScores, getMyMistakes, getScoreById, getExamScores, getExamStatistics, getQuestionStatistics, getQuestionStatisticById）

## 阶段三：核心业务页面实现

- [x] Task 12: 实现基础 Layout 及左侧动态菜单
  - [x] SubTask 12.1: 实现 BasicLayout.vue 完整布局（el-container + el-aside + el-header + el-main）
  - [x] SubTask 12.2: 实现动态侧边栏菜单（根据用户角色渲染不同菜单项）
  - [x] SubTask 12.3: 实现顶部用户信息栏（用户名、角色、退出登录）

- [x] Task 13: 实现登录页和注册页
  - [x] SubTask 13.1: 实现 LoginView.vue（BlankLayout 内，居中卡片，用户名+密码表单，调用 auth.login）
  - [x] SubTask 13.2: 实现 RegisterView.vue（BlankLayout 内，居中卡片，用户名+密码+角色选择，调用 auth.register）

- [x] Task 14: 实现首页
  - [x] SubTask 14.1: 实现 HomeView.vue，根据角色显示不同概览面板（学生：待考考试/最近成绩；教师：考试统计/待评卷数；管理员：系统概览）

- [x] Task 15: 实现题库管理页
  - [x] SubTask 15.1: 实现 QuestionView.vue，题目分页列表（el-table），支持按类型/关键字筛选
  - [x] SubTask 15.2: 实现题目创建/编辑对话框（el-dialog），支持 5 种题型的答案 JSON 动态表单
  - [x] SubTask 15.3: 实现题目删除确认（单个 + 批量），使用 v-permission 控制按钮可见性

- [x] Task 16: 实现考试管理页（教师/管理员）
  - [x] SubTask 16.1: 实现 ExamView.vue，考试分页列表（el-table），支持按状态/时间筛选
  - [x] SubTask 16.2: 实现考试状态操作按钮（发布/撤回/删除），根据 exam.status 动态显示
  - [x] SubTask 16.3: 实现手动组卷对话框/页面（从题库选题，PaperStore 缓存，调整分值和顺序）
  - [x] SubTask 16.4: 实现自动组卷对话框（填写抽题规则，调用 exam.createAuto）

- [x] Task 17: 实现学生考试列表页
  - [x] SubTask 17.1: 实现 StudentExamView.vue，调用 exams/available 接口，显示可参加考试列表
  - [x] SubTask 17.2: 对 running 状态考试显示"进入答题"按钮

- [x] Task 18: 实现数据统计页
  - [x] SubTask 18.1: 实现 DataView.vue（教师/管理员），包含考试统计和题目统计两个 Tab
  - [x] SubTask 18.2: 实现学生成绩页，显示个人成绩列表和错题集

- [x] Task 19: 实现个人中心页
  - [x] SubTask 19.1: 实现 ProfileView.vue，显示用户信息 + 修改密码表单
  - [x] SubTask 19.2: 实现管理员用户管理功能（用户列表 CRUD + 状态切换 + 批量删除）

## 阶段四：复杂交互页面与收尾

- [x] Task 20: 实现答题页（沉浸式）
  - [x] SubTask 20.1: 实现 ExamTakingView.vue（ExamLayout 内），加载考试题目，渲染 5 种题型的答题组件
  - [x] SubTask 20.2: 实现全屏倒计时组件（基于 exam.starttime/endtime 计算，倒计时结束自动交卷）
  - [x] SubTask 20.3: 实现答题进度实时保存（ExamStore + 草稿 API 每 30 秒自动保存）
  - [x] SubTask 20.4: 实现交卷逻辑（调用 submitExam API，成功后清空 ExamStore 并跳转结果页）
  - [x] SubTask 20.5: 实现断点续答（页面加载时从 ExamStore/草稿 API 恢复进度）

- [x] Task 21: 实现答题结果页
  - [x] SubTask 21.1: 实现 ExamResultView.vue，显示总分、各题得分明细、正确/错误标记

- [x] Task 22: 实现教师评卷页
  - [x] SubTask 22.1: 实现 GradeView.vue，显示待评卷列表，点击进入评卷详情
  - [x] SubTask 22.2: 实现简答题评卷表单（查看学生答案 + 参考答案 + 打分输入）

- [x] Task 23: 全局收尾与优化
  - [x] SubTask 23.1: 全局样式统一（Element Plus 主题定制、响应式布局适配）
  - [x] SubTask 23.2: 表单提交防抖（所有提交按钮添加 debounce）
  - [x] SubTask 23.3: 错误边界处理（404 页面、网络异常提示、Token 过期自动跳转登录）
  - [x] SubTask 23.4: 清理无用脚手架代码和文件

# Task Dependencies
- Task 1 → Task 2, Task 3, Task 4, Task 5（基础设施先行）
- Task 3 → Task 11（Axios 封装完成后才能写 API 模块）
- Task 2 → Task 11（TypeScript 类型定义完成后才能写 API 模块）
- Task 4 → Task 7（路由表完成后才能加守卫）
- Task 6 → Task 7, Task 10（UserStore 完成后才能做权限校验）
- Task 5 → Task 6, Task 8, Task 9（main.ts 注册完成后才能使用 Store）
- Task 11 → Task 13~22（API 模块完成后才能实现业务页面）
- Task 8 → Task 16.3（PaperStore 完成后才能做手动组卷）
- Task 9 → Task 20（ExamStore 完成后才能做答题页）
- 阶段一全部 → 阶段二 → 阶段三 → 阶段四（严格顺序）

## 阶段五：检查清单验证后的修复任务

- [ ] Task 24: 补全 TypeScript 类型定义
  - [ ] SubTask 24.1: 在 `src/types/index.ts` 中补充缺失的类型定义：LoginReq、LoginResp、RegisterReq、UserVO、ChangePasswordReq、UserStatusReq
  - [ ] SubTask 24.2: 确认所有 API 模块和 Store 中引用的类型均能正确解析

- [ ] Task 25: 修复 TypeScript 构建错误
  - [ ] SubTask 25.1: 在 `tsconfig.app.json` 中添加 `"ignoreDeprecations": "6.0"` 以解决 TypeScript 6 中 `baseUrl` 弃用警告
  - [ ] SubTask 25.2: 验证 `npm run build` 编译通过无错误

- [ ] Task 26: 完善 API 模块端点覆盖
  - [ ] SubTask 26.1: 确认 `src/api/user.ts` 中 updateUser 函数被正确使用（函数已定义但前端页面未调用编辑用户功能）
  - [ ] SubTask 26.2: 修复 `src/api/score.ts` 中 gradeEssay 参数签名，与后端 `POST /api/v1/scores/{scoreId}/grade-essay` 对齐
  - [ ] SubTask 26.3: 核对全部 API 端点与后端 40 个端点一一对应

- [ ] Task 27: 完善管理员用户管理页面
  - [ ] SubTask 27.1: 将 UserManageView.vue 从占位符实现为完整的管理页面（用户列表 CRUD + 状态切换 + 批量删除 + 编辑用户）
  - [ ] SubTask 27.2: 考虑将 ProfileView.vue 中的管理员用户管理功能迁移至 UserManageView.vue，避免功能分散

- [ ] Task 28: 为所有表单提交按钮添加防抖
  - [ ] SubTask 28.1: 在 LoginView、RegisterView、QuestionView、ExamView、ProfileView、UserManageView 等所有含提交按钮的页面中引入并使用 useDebounce
  - [ ] SubTask 28.2: 验证防抖效果（连续点击提交按钮不会重复发送请求）
