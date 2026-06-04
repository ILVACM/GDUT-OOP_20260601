<template>
  <div>
    <el-row :gutter="20">
      <!-- 学生首页 -->
      <template v-if="userStore.userType === 'student'">
        <el-col :span="12">
          <el-card shadow="hover">
            <template #header><span>待参加的考试</span></template>
            <el-empty v-if="availableExams.length === 0" description="暂无待参加考试" />
            <div v-else>
              <div v-for="exam in availableExams" :key="exam.id" style="margin-bottom: 12px; display: flex; justify-content: space-between; align-items: center">
                <div>
                  <div style="font-weight: bold">{{ exam.exam }}</div>
                  <div style="color: #909399; font-size: 13px">{{ exam.starttime }} ~ {{ exam.endtime }}</div>
                </div>
                <el-button v-if="isRunning(exam)" type="primary" size="small" @click="enterExam(exam.id)">进入答题</el-button>
                <el-tag v-else type="info" size="small">未开考</el-tag>
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="12">
          <el-card shadow="hover">
            <template #header><span>最近成绩</span></template>
            <el-empty v-if="recentScores.length === 0" description="暂无成绩" />
            <div v-else>
              <div v-for="score in recentScores" :key="score.id" style="margin-bottom: 8px; display: flex; justify-content: space-between">
                <span>{{ score.examName }}</span>
                <el-tag :type="score.totalScore >= score.maxScore * 0.6 ? 'success' : 'danger'">{{ score.totalScore }}/{{ score.maxScore }}</el-tag>
              </div>
            </div>
          </el-card>
        </el-col>
      </template>

      <!-- 教师/管理员首页 -->
      <template v-else>
        <el-col :span="8">
          <el-card shadow="hover">
            <el-statistic title="考试总数" :value="examStats.total" />
          </el-card>
        </el-col>
        <el-col :span="8">
          <el-card shadow="hover">
            <el-statistic title="进行中的考试" :value="examStats.running" />
          </el-card>
        </el-col>
        <el-col :span="8">
          <el-card shadow="hover">
            <el-statistic title="题库题目数" :value="examStats.questionCount" />
          </el-card>
        </el-col>
      </template>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { listAvailableExams } from '@/api/exam'
import { getMyScores } from '@/api/score'
import { listExams } from '@/api/exam'
import { listQuestions } from '@/api/question'
import type { ExamForStudentVO, ScoreListVO } from '@/types'

const router = useRouter()
const userStore = useUserStore()

const availableExams = ref<ExamForStudentVO[]>([])
const recentScores = ref<ScoreListVO[]>([])
const examStats = ref({ total: 0, running: 0, questionCount: 0 })

const isRunning = (exam: ExamForStudentVO) => {
  const now = new Date().getTime()
  const start = new Date(exam.starttime).getTime()
  const end = new Date(exam.endtime).getTime()
  return now >= start && now < end
}

const enterExam = (examId: number) => {
  router.push(`/exam-taking/${examId}`)
}

onMounted(async () => {
  try {
    if (userStore.userType === 'student') {
      const [exams, scores] = await Promise.all([
        listAvailableExams(),
        getMyScores({ page: 0, size: 5 }),
      ])
      availableExams.value = exams
      recentScores.value = scores.content
    } else {
      const [exams, questions] = await Promise.all([
        listExams({ page: 0, size: 1 }),
        listQuestions({ page: 0, size: 1 }),
      ])
      examStats.value.total = exams.totalElements
      examStats.value.questionCount = questions.totalElements
    }
  } catch {
    // 静默处理
  }
})
</script>
