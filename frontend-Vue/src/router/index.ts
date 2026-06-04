import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import { useUserStore } from '@/stores/user'
import type { UserType } from '@/types'

// Layouts
import BasicLayout from '@/layouts/BasicLayout.vue'
import BlankLayout from '@/layouts/BlankLayout.vue'
import ExamLayout from '@/layouts/ExamLayout.vue'

// Views - lazy loaded
const HomeView = () => import('@/views/HomeView.vue')
const LoginView = () => import('@/views/LoginView.vue')
const RegisterView = () => import('@/views/RegisterView.vue')
const QuestionView = () => import('@/views/QuestionView.vue')
const ExamView = () => import('@/views/ExamView.vue')
const StudentExamView = () => import('@/views/StudentExamView.vue')
const ExamTakingView = () => import('@/views/ExamTakingView.vue')
const ExamResultView = () => import('@/views/ExamResultView.vue')
const DataView = () => import('@/views/DataView.vue')
const StudentScoreView = () => import('@/views/StudentScoreView.vue')
const ProfileView = () => import('@/views/ProfileView.vue')
const UserManageView = () => import('@/views/UserManageView.vue')
const GradeView = () => import('@/views/GradeView.vue')

const routes: RouteRecordRaw[] = [
  // 空白 Layout（登录/注册）
  {
    path: '/login',
    component: BlankLayout,
    children: [
      { path: '', name: 'Login', component: LoginView, meta: { requiresAuth: false } },
    ],
  },
  {
    path: '/register',
    component: BlankLayout,
    children: [
      { path: '', name: 'Register', component: RegisterView, meta: { requiresAuth: false } },
    ],
  },
  // 沉浸式 Layout（答题）
  {
    path: '/exam-taking/:examId',
    component: ExamLayout,
    children: [
      { path: '', name: 'ExamTaking', component: ExamTakingView, meta: { requiresAuth: true, requiredRoles: ['student'] as UserType[] } },
    ],
  },
  {
    path: '/exam-result/:scoreId',
    component: ExamLayout,
    children: [
      { path: '', name: 'ExamResult', component: ExamResultView, meta: { requiresAuth: true } },
    ],
  },
  // 基础 Layout（主要业务页面）
  {
    path: '/',
    component: BasicLayout,
    meta: { requiresAuth: true },
    children: [
      { path: '', name: 'Home', component: HomeView },
      { path: 'questions', name: 'Questions', component: QuestionView, meta: { requiredRoles: ['teacher', 'admin'] as UserType[] } },
      { path: 'exams', name: 'Exams', component: ExamView, meta: { requiredRoles: ['teacher', 'admin'] as UserType[] } },
      { path: 'my-exams', name: 'MyExams', component: StudentExamView, meta: { requiredRoles: ['student'] as UserType[] } },
      { path: 'data', name: 'Data', component: DataView, meta: { requiredRoles: ['teacher', 'admin'] as UserType[] } },
      { path: 'my-scores', name: 'MyScores', component: StudentScoreView, meta: { requiredRoles: ['student'] as UserType[] } },
      { path: 'profile', name: 'Profile', component: ProfileView },
      { path: 'users', name: 'Users', component: UserManageView, meta: { requiredRoles: ['admin'] as UserType[] } },
      { path: 'grade', name: 'Grade', component: GradeView, meta: { requiredRoles: ['teacher', 'admin'] as UserType[] } },
    ],
  },
  // 404
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/NotFoundView.vue'),
    meta: { requiresAuth: false },
  },
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
})

// 路由守卫
router.beforeEach((to, _from, next) => {
  const userStore = useUserStore()
  const requiresAuth = to.meta.requiresAuth !== false
  const requiredRoles = to.meta.requiredRoles as UserType[] | undefined

  if (requiresAuth && !userStore.isLoggedIn) {
    next({ name: 'Login', query: { redirect: to.fullPath } })
    return
  }

  if (userStore.isLoggedIn && (to.name === 'Login' || to.name === 'Register')) {
    next({ name: 'Home' })
    return
  }

  if (requiredRoles && userStore.userType && !requiredRoles.includes(userStore.userType as UserType)) {
    next({ name: 'Home' })
    return
  }

  next()
})

export default router
