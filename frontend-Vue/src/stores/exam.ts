import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { ExamForStudentVO, AnswerItem } from '@/types'
import { submitExam as apiSubmitExam } from '@/api/score'
import { saveDraft as apiSaveDraft, loadDraft as apiLoadDraft } from '@/api/exam'

export const useExamStore = defineStore('exam', () => {
  const examId = ref<number | null>(null)
  const examName = ref('')
  const endtime = ref('')
  const examInfo = ref<ExamForStudentVO | null>(null)
  const answers = ref<Record<number, any>>({})

  const hasExam = computed(() => examId.value !== null)

  function initExam(exam: ExamForStudentVO) {
    examId.value = exam.id
    examName.value = exam.exam
    endtime.value = exam.endtime
    examInfo.value = exam
    answers.value = {}
  }

  function updateAnswer(questionId: number, answer: any) {
    answers.value[questionId] = answer
  }

  async function saveDraft() {
    if (!examId.value) return
    const answerItems: AnswerItem[] = Object.entries(answers.value).map(([qid, ans]) => ({
      questionId: Number(qid),
      userAnswer: ans,
    }))
    await apiSaveDraft(examId.value, { answers: answerItems })
  }

  async function loadDraftAnswers() {
    if (!examId.value) return
    const draftAnswers = await apiLoadDraft(examId.value)
    if (draftAnswers && Array.isArray(draftAnswers)) {
      for (const item of draftAnswers) {
        answers.value[item.questionId] = item.userAnswer
      }
    }
  }

  async function submitExam() {
    if (!examId.value) return
    const answerItems: AnswerItem[] = Object.entries(answers.value).map(([qid, ans]) => ({
      questionId: Number(qid),
      userAnswer: ans,
    }))
    const result = await apiSubmitExam(examId.value, { examId: examId.value, answers: answerItems })
    clearExam()
    return result
  }

  function clearExam() {
    examId.value = null
    examName.value = ''
    endtime.value = ''
    examInfo.value = null
    answers.value = {}
  }

  return { examId, examName, endtime, examInfo, answers, hasExam, initExam, updateAnswer, saveDraft, loadDraftAnswers, submitExam, clearExam }
}, {
  persist: true,
})
