export type UserType = 'student' | 'teacher' | 'admin'

// ==================== 通用 ====================

export interface Result<T> {
  code: number
  message: string
  data: T
}

export interface PageResult<T> {
  content: T[]
  totalElements: number
  totalPages: number
  number: number
  size: number
}

// ==================== M01 用户认证 ====================

export interface LoginReq {
  name: string
  password: string
}

export interface RegisterReq {
  name: string
  password: string
  type: UserType
}

export interface ChangePasswordReq {
  oldPassword: string
  newPassword: string
}

export interface UserVO {
  id: number
  name: string
  type: UserType
  status: number
}

export interface LoginResp {
  token: string
  user: UserVO
}

export interface UserStatusReq {
  status: number
}

// ==================== M02 题库 ====================

export type QuestionType = 'SingleChoice' | 'MultipleChoice' | 'Judge' | 'Fill' | 'Essay'

export interface QuestionVO {
  id: number
  type: QuestionType
  context: string
  img: number
  answer: any
  use: number
  accuracy: number | null
}

export interface QuestionCreateReq {
  type: QuestionType
  context: string
  img: number
  answer: any
}

export type QuestionUpdateReq = QuestionCreateReq

export interface QuestionQueryReq {
  type?: QuestionType
  keyword?: string
  page?: number
  size?: number
}

export interface BatchImportResult {
  success: number
  failed: number
  errors?: string[]
}

// ==================== 考试 ====================

export type ExamStatus = 'draft' | 'publish' | 'running' | 'done'

export interface ExamQuestionItem {
  questionId: number
  score: number
}

export interface ExamVO {
  id: number
  exam: string
  status: ExamStatus
  starttime: string
  endtime: string
  questionItems: ExamQuestionItem[]
}

export interface ExamQuestionForStudentVO {
  questionId: number
  type: QuestionType
  context: string
  img: number
  options: any  // 从 question.answer.options 提取（仅单选/多选）
  score: number
}

export interface ExamForStudentVO {
  id: number
  exam: string
  starttime: string
  endtime: string
  questionItems: ExamQuestionForStudentVO[]
}

export interface ExamCreateManualReq {
  exam: string
  starttime: string
  endtime: string
  items: ExamQuestionItem[]
}

export interface AutoRule {
  totalQuestions: number
  totalScore: number
  typeFilter?: QuestionType[]
  usePenalty?: boolean
}

export interface ExamCreateAutoReq {
  exam: string
  starttime: string
  endtime: string
  autoRule: AutoRule
}

// ==================== 答题/草稿 ====================

export interface AnswerItem {
  questionId: number
  userAnswer: any
}

export interface DraftSaveReq {
  answers: AnswerItem[]
}

// ==================== 组卷 Store ====================

export interface PaperItem {
  questionId: number
  score: number
  type: QuestionType
  context: string
}

// ==================== 成绩 ====================

export interface DetailItemVO {
  questionId: number
  type: QuestionType
  context: string
  userAnswer: any
  correctAnswer: any
  score: number
  maxScore: number
  isCorrect: boolean | null
}

export interface ScoreVO {
  id: number
  user: number
  userName: string
  exam: number
  examName: string
  all: number
  maxScore: number
  accuracy: number
  detail: DetailItemVO[]
}

export interface ScoreListVO {
  id: number
  examId: number
  examName: string
  userId: number
  username: string
  score: number
  totalScore: number
  submittedAt: string
}

export interface MistakeItemVO {
  questionId: number
  type: QuestionType
  context: string
  userAnswer: any
  correctAnswer: any
}

export interface ExamSubmitReq {
  examId: number
  answers: AnswerItem[]
}

export interface EssayGradeReq {
  scoreId: number
  questionId: number
  score: number
  comment?: string
}

// ==================== 统计 ====================

export interface ExamStatisticsVO {
  examName: string
  participantCount: number
  submitCount: number
  passRate: number
  averageScore: number | null
  maxScore: number | null
  minScore: number | null
  medianScore: number | null
  scoreDistribution: Record<string, number>
}

export interface QuestionStatisticsVO {
  questionId: number
  type: QuestionType
  use: number
  correct: number
  accuracyRate: number | null
}
