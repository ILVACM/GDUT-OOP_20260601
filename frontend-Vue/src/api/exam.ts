import request from '@/utils/request'
import type { ExamVO, ExamForStudentVO, ExamCreateManualReq, ExamCreateAutoReq, PageResult, DraftSaveReq, AnswerItem } from '@/types'

export function createManualExam(data: ExamCreateManualReq) {
  return request.post<ExamVO>('/v1/exams/manual', data)
}

export function createAutoExam(data: ExamCreateAutoReq) {
  return request.post<ExamVO>('/v1/exams/auto', data)
}

export function getExamById(id: number) {
  return request.get<ExamVO>(`/v1/exams/${id}`)
}

export function getExamPreview(id: number) {
  return request.get<ExamForStudentVO>(`/v1/exams/${id}/preview`)
}

export function updateExam(id: number, data: ExamCreateManualReq) {
  return request.put<ExamVO>(`/v1/exams/${id}`, data)
}

export function publishExam(id: number) {
  return request.post(`/v1/exams/${id}/publish`)
}

export function withdrawExam(id: number) {
  return request.post(`/v1/exams/${id}/withdraw`)
}

export function deleteExam(id: number) {
  return request.delete(`/v1/exams/${id}`)
}

export function listExams(params: { page?: number; size?: number; status?: string }) {
  return request.get<PageResult<ExamVO>>('/v1/exams', { params })
}

export function listAvailableExams() {
  return request.get<ExamForStudentVO[]>('/v1/exams/available')
}

export function saveDraft(examId: number, data: DraftSaveReq) {
  return request.put(`/v1/exams/${examId}/draft`, data)
}

export function loadDraft(examId: number) {
  return request.get<AnswerItem[]>(`/v1/exams/${examId}/draft`)
}
