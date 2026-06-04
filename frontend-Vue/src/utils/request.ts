// src/utils/request.ts
import axios, { type AxiosInstance, type AxiosResponse, type InternalAxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import router from '@/router'

// 模块级状态锁，防止并发请求导致重复弹窗
let isRedirecting = false

// 供登录成功后重置状态锁
export function resetRedirecting() {
  isRedirecting = false
}

const instance: AxiosInstance = axios.create({
  baseURL: '/api',  // F2: 严禁硬编码后端地址
  timeout: 10000,
})

// 请求拦截：注入 Token (F4)
instance.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const userStore = useUserStore()
  if (userStore.token) {
    config.headers.Authorization = `Bearer ${userStore.token}`
  }
  return config
})

// 响应拦截：统一解包 Result<T> (F3)
instance.interceptors.response.use(
  (response: AxiosResponse) => {
    const { code, message, data } = response.data
    if (code === 200) return data
    // 处理认证相关错误码
    if (code === 4101 || code === 4102) {
      // 登录/注册接口本身的认证失败，不执行 logout/跳转
      const isAuthApi = response.config.url?.includes('/auth/login') ||
                        response.config.url?.includes('/auth/register')
      if (!isAuthApi && !isRedirecting) {
        isRedirecting = true
        const userStore = useUserStore()
        userStore.logout()
        router.push('/login')
        ElMessage.error('登录已过期，请重新登录')
      } else if (isAuthApi) {
        ElMessage.error(message || '登录失败')
      }
      return Promise.reject(new Error(message))
    }
    ElMessage.error(message || '请求失败')
    return Promise.reject(new Error(message))
  },
  (error) => {
    ElMessage.error(error.message || '网络异常')
    return Promise.reject(error)
  }
)

export default instance
