<template>
  <div>
    <div style="display: flex; justify-content: space-between; margin-bottom: 16px">
      <div style="display: flex; gap: 12px">
        <el-select v-model="filterType" placeholder="角色筛选" clearable style="width: 150px" @change="loadData">
          <el-option label="学生" value="student" />
          <el-option label="教师" value="teacher" />
          <el-option label="管理员" value="admin" />
        </el-select>
        <el-select v-model="filterStatus" placeholder="状态筛选" clearable style="width: 150px" @change="loadData">
          <el-option label="启用" :value="1" />
          <el-option label="禁用" :value="0" />
        </el-select>
      </div>
      <div style="display: flex; gap: 8px">
        <el-button type="primary" @click="openCreateDialog">创建用户</el-button>
        <el-button type="danger" :disabled="selectedIds.length === 0" @click="handleBatchDelete">批量删除</el-button>
      </div>
    </div>

    <el-table :data="users" border stripe @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="50" />
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
          <el-button size="small" type="danger" @click="handleDelete(row.id)" :disabled="row.id === currentUserId">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div style="display: flex; justify-content: flex-end; margin-top: 16px">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        @size-change="loadData"
        @current-change="loadData"
      />
    </div>

    <!-- 创建用户对话框 -->
    <el-dialog v-model="createDialogVisible" title="创建用户" width="400px" destroy-on-close>
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-width="80px">
        <el-form-item label="用户名" prop="name">
          <el-input v-model="createForm.name" placeholder="4-30位" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="createForm.password" type="password" show-password placeholder="6-32位" />
        </el-form-item>
        <el-form-item label="角色" prop="type">
          <el-select v-model="createForm.type" style="width: 100%">
            <el-option label="学生" value="student" />
            <el-option label="教师" value="teacher" />
            <el-option label="管理员" value="admin" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="createLoading" @click="handleCreate">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { listUsers, createUser, updateUserStatus, deleteUser, batchDeleteUsers } from '@/api/user'
import { useUserStore } from '@/stores/user'
import type { UserVO, UserType } from '@/types'

const userStore = useUserStore()
const currentUserId = computed(() => userStore.user?.id)

const roleLabels: Record<string, string> = { student: '学生', teacher: '教师', admin: '管理员' }

const users = ref<UserVO[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const filterType = ref<UserType | ''>('')
const filterStatus = ref<number | ''>('')
const selectedIds = ref<number[]>([])

const createDialogVisible = ref(false)
const createLoading = ref(false)
const createFormRef = ref<FormInstance>()
const createForm = reactive({ name: '', password: '', type: 'student' as UserType })
const createRules: FormRules = {
  name: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 4, max: 30, message: '用户名长度4-30位', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 32, message: '密码长度6-32位', trigger: 'blur' },
  ],
  type: [{ required: true, message: '请选择角色', trigger: 'change' }],
}

const loadData = async () => {
  try {
    const res = await listUsers({
      page: currentPage.value - 1,
      size: pageSize.value,
      type: filterType.value || undefined,
      status: filterStatus.value !== '' ? filterStatus.value as number : undefined,
    })
    users.value = res.content
    total.value = res.totalElements
  } catch { /* handled */ }
}

const handleSelectionChange = (rows: UserVO[]) => {
  selectedIds.value = rows.map(r => r.id)
}

const openCreateDialog = () => {
  createForm.name = ''
  createForm.password = ''
  createForm.type = 'student'
  createDialogVisible.value = true
}

const handleCreate = async () => {
  const valid = await createFormRef.value?.validate().catch(() => false)
  if (!valid) return
  createLoading.value = true
  try {
    await createUser({ name: createForm.name, password: createForm.password, type: createForm.type })
    ElMessage.success('创建成功')
    createDialogVisible.value = false
    loadData()
  } catch { /* handled */ } finally {
    createLoading.value = false
  }
}

const handleToggleStatus = async (id: number, status: number) => {
  try {
    await updateUserStatus(id, { status })
    ElMessage.success('状态更新成功')
    loadData()
  } catch { /* handled */ }
}

const handleDelete = async (id: number) => {
  await ElMessageBox.confirm('确定删除该用户吗？', '确认删除', { type: 'warning' })
  try {
    await deleteUser(id)
    ElMessage.success('删除成功')
    loadData()
  } catch { /* handled */ }
}

const handleBatchDelete = async () => {
  await ElMessageBox.confirm(`确定删除选中的 ${selectedIds.value.length} 个用户吗？`, '确认删除', { type: 'warning' })
  try {
    await batchDeleteUsers(selectedIds.value)
    ElMessage.success('批量删除成功')
    loadData()
  } catch { /* handled */ }
}

onMounted(loadData)
</script>
