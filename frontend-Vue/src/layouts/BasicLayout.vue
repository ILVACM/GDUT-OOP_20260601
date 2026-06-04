<template>
  <el-container style="height: 100vh">
    <el-aside width="220px" style="background-color: #304156">
      <div style="height: 60px; display: flex; align-items: center; justify-content: center; color: #fff; font-size: 18px; font-weight: bold">
        在线考试系统
      </div>
      <el-menu
        :default-active="route.path"
        router
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#409eff"
      >
        <template v-for="item in menuItems" :key="item.path">
          <el-menu-item :index="item.path">
            <el-icon><component :is="item.icon" /></el-icon>
            <span>{{ item.title }}</span>
          </el-menu-item>
        </template>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header style="display: flex; align-items: center; justify-content: flex-end; border-bottom: 1px solid #e6e6e6; padding: 0 20px">
        <span style="margin-right: 16px; color: #606266">{{ userStore.userName }} ({{ roleLabel }})</span>
        <el-button type="danger" text @click="handleLogout">退出登录</el-button>
      </el-header>
      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { House, Document, EditPen, DataAnalysis, User, Management } from '@element-plus/icons-vue'
import type { UserType } from '@/types'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const roleLabel = computed(() => {
  const labels: Record<string, string> = { student: '学生', teacher: '教师', admin: '管理员' }
  return labels[userStore.userType as string] || ''
})

interface MenuItem {
  path: string
  title: string
  icon: any
  roles: UserType[]
}

const allMenuItems: MenuItem[] = [
  { path: '/', title: '首页', icon: House, roles: ['student', 'teacher', 'admin'] },
  { path: '/questions', title: '题库管理', icon: Document, roles: ['teacher', 'admin'] },
  { path: '/exams', title: '考试管理', icon: EditPen, roles: ['teacher', 'admin'] },
  { path: '/my-exams', title: '我的考试', icon: EditPen, roles: ['student'] },
  { path: '/data', title: '数据统计', icon: DataAnalysis, roles: ['teacher', 'admin'] },
  { path: '/my-scores', title: '我的成绩', icon: DataAnalysis, roles: ['student'] },
  { path: '/users', title: '用户管理', icon: Management, roles: ['admin'] },
  { path: '/profile', title: '个人中心', icon: User, roles: ['student', 'teacher', 'admin'] },
]

const menuItems = computed(() => {
  return allMenuItems.filter(item => item.roles.includes(userStore.userType as UserType))
})

const handleLogout = () => {
  userStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.el-aside {
  overflow-y: auto;
}
.el-menu {
  border-right: none;
}
</style>
