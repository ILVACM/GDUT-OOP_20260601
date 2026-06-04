import request from '@/utils/request'
import type { LoginReq, LoginResp, RegisterReq, UserVO, ChangePasswordReq } from '@/types'

export function login(data: LoginReq) {
  return request.post<LoginResp>('/v1/auth/login', data)
}

export function register(data: RegisterReq) {
  return request.post<UserVO>('/v1/auth/register', data)
}

export function logout() {
  return request.post('/v1/auth/logout')
}

export function me() {
  return request.get<UserVO>('/v1/auth/me')
}

export function changePassword(data: ChangePasswordReq) {
  return request.post('/v1/auth/password', data)
}
