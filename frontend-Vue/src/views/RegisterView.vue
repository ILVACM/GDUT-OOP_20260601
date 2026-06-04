<template>
  <el-card style="width: 420px" shadow="always">
    <template #header>
      <div style="text-align: center">
        <h2 style="margin: 0">在线考试系统</h2>
        <p style="color: #909399; margin-top: 8px">学生注册</p>
      </div>
    </template>
    <el-form ref="formRef" :model="form" :rules="rules" label-width="0" size="large" @submit.prevent="handleRegister">
      <el-form-item prop="name">
        <el-input v-model="form.name" prefix-icon="User" placeholder="用户名（4-30位）" />
      </el-form-item>
      <el-form-item prop="password">
        <el-input v-model="form.password" prefix-icon="Lock" type="password" placeholder="密码（6-32位）" show-password />
      </el-form-item>
      <el-form-item prop="confirmPassword">
        <el-input v-model="form.confirmPassword" prefix-icon="Lock" type="password" placeholder="确认密码" show-password />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" style="width: 100%" :loading="loading" native-type="submit">注 册</el-button>
      </el-form-item>
      <div style="text-align: center">
        <router-link to="/login" style="color: #409eff">已有账号？返回登录</router-link>
      </div>
    </el-form>
  </el-card>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const formRef = ref<FormInstance>()
const loading = ref(false)

const form = reactive({
  name: '',
  password: '',
  confirmPassword: '',
})

const validateConfirmPassword = (_rule: any, value: string, callback: any) => {
  if (value !== form.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const rules: FormRules = {
  name: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 4, max: 30, message: '用户名长度为4-30位', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 32, message: '密码长度为6-32位', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' },
  ],
}

const handleRegister = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await userStore.register({
      name: form.name,
      password: form.password,
      type: 'student',
    })
    ElMessage.success('注册成功，请登录')
    router.push('/login')
  } catch {
    // 错误已由拦截器处理
  } finally {
    loading.value = false
  }
}
</script>
