import request from '@/utils/request'
import type { ScoreVO, ScoreListVO, MistakeItemVO, ExamSubmitReq, EssayGradeReq, ExamStatisticsVO, QuestionStatisticsVO, PageResult } from '@/types'

export function submitExam(examId: number, data: ExamSubmitReq) {
  return request.post<ScoreVO>(`/v1/exams/${examId}/submit`, data)
}

export function gradeEssay(scoreId: number, data: EssayGradeReq) {
  return request.post<ScoreVO>(`/v1/scores/${scoreId}/grade-essay`, data)
}

export function getMyScores(params: { page?: number; size?: number }) {
  return request.get<PageResult<ScoreListVO>>('/v1/scores/me', { params })
}

export function getMyMistakes(params: { page?: number; size?: number }) {
  return request.get<PageResult<MistakeItemVO>>('/v1/scores/me/mistakes', { params })
}

export function getScoreById(id: number) {
  return request.get<ScoreVO>(`/v1/scores/${id}`)
}

export function getExamScores(examId: number, params: { page?: number; size?: number }) {
  return request.get<PageResult<ScoreListVO>>(`/v1/exams/${examId}/scores`, { params })
}

export function getExamStatistics(examId: number) {
  return request.get<ExamStatisticsVO>(`/v1/statistics/exams/${examId}`)
}

export function getQuestionStatistics(params: { page?: number; size?: number; sortBy?: string }) {
  return request.get<PageResult<QuestionStatisticsVO>>('/v1/statistics/questions', { params })
}

export function getQuestionStatisticById(id: number) {
  return request.get<QuestionStatisticsVO>(`/v1/statistics/questions/${id}`)
}
