import request from '@/utils/request'
import type { QuestionVO, QuestionCreateReq, QuestionUpdateReq, QuestionQueryReq, BatchImportResult, PageResult } from '@/types'

export function createQuestion(data: QuestionCreateReq) {
  return request.post<QuestionVO>('/v1/questions', data)
}

export function batchCreateQuestions(data: QuestionCreateReq[]) {
  return request.post<BatchImportResult>('/v1/questions/batch', data)
}

export function getQuestionById(id: number) {
  return request.get<QuestionVO>(`/v1/questions/${id}`)
}

export function listQuestions(params: QuestionQueryReq) {
  return request.get<PageResult<QuestionVO>>('/v1/questions', { params })
}

export function updateQuestion(id: number, data: QuestionUpdateReq) {
  return request.put<QuestionVO>(`/v1/questions/${id}`, data)
}

export function deleteQuestion(id: number) {
  return request.delete(`/v1/questions/${id}`)
}

export function batchDeleteQuestions(ids: number[]) {
  return request.delete('/v1/questions/batch', { data: ids })
}

export function getRandomQuestion(params?: { type?: string; excludedIds?: string }) {
  return request.get<QuestionVO>('/v1/questions/random', { params })
}
