import request from '@/utils/request'
import type { UserVO, RegisterReq, UserStatusReq, PageResult } from '@/types'

export function listUsers(params: { page?: number; size?: number; type?: string; status?: number }) {
  return request.get<PageResult<UserVO>>('/v1/users', { params })
}

export function createUser(data: RegisterReq) {
  return request.post<UserVO>('/v1/users', data)
}

export function updateUser(id: number, data: RegisterReq) {
  return request.put<UserVO>(`/v1/users/${id}`, data)
}

export function updateUserStatus(id: number, data: UserStatusReq) {
  return request.patch(`/v1/users/${id}/status`, data)
}

export function deleteUser(id: number) {
  return request.delete(`/v1/users/${id}`)
}

export function batchDeleteUsers(ids: number[]) {
  return request.delete('/v1/users/batch', { data: ids })
}
