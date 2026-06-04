<template>
  <div>
    <el-row :gutter="20">
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header><span>个人信息</span></template>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="用户ID">{{ userStore.user?.id }}</el-descriptions-item>
            <el-descriptions-item label="用户名">{{ userStore.user?.name }}</el-descriptions-item>
            <el-descriptions-item label="角色">{{ roleLabel }}</el-descriptions-item>
            <el-descriptions-item label="状态">{{ userStore.user?.status === 1 ? '启用' : '禁用' }}</el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header><span>修改密码</span></template>
          <el-form ref="pwdFormRef" :model="pwdForm" :rules="pwdRules" label-width="80px">
            <el-form-item label="旧密码" prop="oldPassword">
              <el-input v-model="pwdForm.oldPassword" type="password" show-password />
            </el-form-item>
            <el-form-item label="新密码" prop="newPassword">
              <el-input v-model="pwdForm.newPassword" type="password" show-password />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="pwdLoading" @click="handleChangePassword">确认修改</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>
    </el-row>

    <!-- 管理员用户管理 -->
    <el-card v-if="userStore.userType === 'admin'" shadow="hover" style="margin-top: 20px">
      <template #header>
        <div style="display: flex; justify-content: space-between">
          <span>用户管理</span>
          <el-button type="primary" size="small" @click="openCreateUserDialog">创建用户</el-button>
        </div>
      </template>
      <el-table :data="users" border stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="name" label="用户名" />
        <el-table-column prop="type" label="角色" width="100">
          <template #default="{ row }">
            <el-tag>{{ roleLabels[row.type] || row.type }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-switch :model-value="row.status === 1" @change="(val: boolean) => handleToggleStatus(row.id, val ? 1 : 0)" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button size="small" type="danger" @click="handleDeleteUser(row.id)" :disabled="row.id === userStore.user?.id">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 创建用户对话框 -->
    <el-dialog v-model="createUserVisible" title="创建用户" width="400px" destroy-on-close>
      <el-form ref="createUserFormRef" :model="createUserForm" :rules="createUserRules" label-width="80px">
        <el-form-item label="用户名" prop="name">
          <el-input v-model="createUserForm.name" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="createUserForm.password" type="password" show-password />
        </el-form-item>
        <el-form-item label="角色" prop="type">
          <el-select v-model="createUserForm.type" style="width: 100%">
            <el-option label="学生" value="student" />
            <el-option label="教师" value="teacher" />
            <el-option label="管理员" value="admin" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createUserVisible = false">取消</el-button>
        <el-button type="primary" :loading="createUserLoading" @click="handleCreateUser">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { changePassword } from '@/api/auth'
import { listUsers, createUser, updateUserStatus, deleteUser } from '@/api/user'
import type { UserVO, UserType } from '@/types'

const userStore = useUserStore()
const roleLabel = computed(() => {
  const labels: Record<string, string> = { student: '学生', teacher: '教师', admin: '管理员' }
  return labels[userStore.userType as string] || ''
})
const roleLabels: Record<string, string> = { student: '学生', teacher: '教师', admin: '管理员' }

// 修改密码
const pwdFormRef = ref<FormInstance>()
const pwdLoading = ref(false)
const pwdForm = reactive({ oldPassword: '', newPassword: '' })
const pwdRules: FormRules = {
  oldPassword: [{ required: true, message: '请输入旧密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 32, message: '密码长度6-32位', trigger: 'blur' },
  ],
}

const handleChangePassword = async () => {
  const valid = await pwdFormRef.value?.validate().catch(() => false)
  if (!valid) return
  pwdLoading.value = true
  try {
    await changePassword({ oldPassword: pwdForm.oldPassword, newPassword: pwdForm.newPassword })
    ElMessage.success('密码修改成功')
    pwdForm.oldPassword = ''
    pwdForm.newPassword = ''
  } catch { /* handled */ } finally {
    pwdLoading.value = false
  }
}

// 用户管理（管理员）
const users = ref<UserVO[]>([])
const createUserVisible = ref(false)
const createUserLoading = ref(false)
const createUserFormRef = ref<FormInstance>()
const createUserForm = reactive({ name: '', password: '', type: 'student' as UserType })
const createUserRules: FormRules = {
  name: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 32, message: '密码长度6-32位', trigger: 'blur' },
  ],
  type: [{ required: true, message: '请选择角色', trigger: 'change' }],
}

const loadUsers = async () => {
  if (userStore.userType !== 'admin') return
  try {
    const res = await listUsers({ page: 0, size: 100 })
    users.value = res.content
  } catch { /* handled */ }
}

const openCreateUserDialog = () => {
  createUserForm.name = ''
  createUserForm.password = ''
  createUserForm.type = 'student'
  createUserVisible.value = true
}

const handleCreateUser = async () => {
  const valid = await createUserFormRef.value?.validate().catch(() => false)
  if (!valid) return
  createUserLoading.value = true
  try {
    await createUser({ name: createUserForm.name, password: createUserForm.password, type: createUserForm.type })
    ElMessage.success('创建成功')
    createUserVisible.value = false
    loadUsers()
  } catch { /* handled */ } finally {
    createUserLoading.value = false
  }
}

const handleToggleStatus = async (id: number, status: number) => {
  try {
    await updateUserStatus(id, { status })
    ElMessage.success('状态更新成功')
    loadUsers()
  } catch { /* handled */ }
}

const handleDeleteUser = async (id: number) => {
  await ElMessageBox.confirm('确定删除该用户吗？', '确认删除', { type: 'warning' })
  try {
    await deleteUser(id)
    ElMessage.success('删除成功')
    loadUsers()
  } catch { /* handled */ }
}

onMounted(loadUsers)
</script>
