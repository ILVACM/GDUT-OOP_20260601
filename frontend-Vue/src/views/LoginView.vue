<template>
  <el-card style="width: 420px" shadow="always">
    <template #header>
      <div style="text-align: center">
        <h2 style="margin: 0">在线考试系统</h2>
        <p style="color: #909399; margin-top: 8px">用户登录</p>
      </div>
    </template>
    <el-form ref="formRef" :model="form" :rules="rules" label-width="0" size="large" @submit.prevent="handleLogin">
      <el-form-item prop="name">
        <el-input v-model="form.name" prefix-icon="User" placeholder="用户名" />
      </el-form-item>
      <el-form-item prop="password">
        <el-input v-model="form.password" prefix-icon="Lock" type="password" placeholder="密码" show-password />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" style="width: 100%" :loading="loading" native-type="submit">登 录</el-button>
      </el-form-item>
      <div style="text-align: center">
        <router-link to="/register" style="color: #409eff">没有账号？立即注册</router-link>
      </div>
    </el-form>
  </el-card>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { useUserStore } from '@/stores/user'
import type { LoginReq } from '@/types'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const formRef = ref<FormInstance>()
const loading = ref(false)

const form = reactive<LoginReq>({
  name: '',
  password: '',
})

const rules: FormRules = {
  name: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

const handleLogin = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await userStore.login(form)
    ElMessage.success('登录成功')
    const redirect = (route.query.redirect as string) || '/'
    router.push(redirect)
  } catch {
    // 错误已由拦截器处理
  } finally {
    loading.value = false
  }
}
</script>
