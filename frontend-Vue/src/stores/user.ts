import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { UserType, UserVO, LoginReq, RegisterReq, ChangePasswordReq } from '@/types'
import { login as apiLogin, register as apiRegister, logout as apiLogout, me as apiMe, changePassword as apiChangePassword } from '@/api/auth'

export const useUserStore = defineStore('user', () => {
  const token = ref<string>('')
  const user = ref<UserVO | null>(null)

  const isLoggedIn = computed(() => !!token.value)
  const userType = computed<UserType | ''>(() => user.value?.type ?? '')
  const userName = computed(() => user.value?.name ?? '')

  async function login(req: LoginReq) {
    const resp = await apiLogin(req)
    token.value = resp.token
    user.value = resp.user
  }

  async function register(req: RegisterReq) {
    await apiRegister(req)
  }

  async function logout() {
    try {
      await apiLogout()
    } finally {
      token.value = ''
      user.value = null
    }
  }

  async function fetchCurrentUser() {
    const resp = await apiMe()
    user.value = resp
  }

  async function changePassword(req: ChangePasswordReq) {
    await apiChangePassword(req)
  }

  return { token, user, isLoggedIn, userType, userName, login, register, logout, fetchCurrentUser, changePassword }
}, {
  persist: true,
})
