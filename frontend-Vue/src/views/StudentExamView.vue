<template>
  <div>
    <h3 style="margin-bottom: 16px">我的考试</h3>
    <el-empty v-if="exams.length === 0" description="暂无可参加的考试" />
    <el-row :gutter="16">
      <el-col v-for="exam in exams" :key="exam.id" :span="8" style="margin-bottom: 16px">
        <el-card shadow="hover">
          <div style="font-weight: bold; font-size: 16px; margin-bottom: 8px">{{ exam.exam }}</div>
          <div style="color: #909399; font-size: 13px; margin-bottom: 4px">开始: {{ exam.starttime }}</div>
          <div style="color: #909399; font-size: 13px; margin-bottom: 12px">结束: {{ exam.endtime }}</div>
          <div style="display: flex; justify-content: flex-end">
            <el-button v-if="isRunning(exam)" type="primary" size="small" @click="enterExam(exam.id)">进入答题</el-button>
            <el-tag v-else type="info" size="small">未开考</el-tag>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { listAvailableExams } from '@/api/exam'
import type { ExamForStudentVO } from '@/types'

const router = useRouter()
const exams = ref<ExamForStudentVO[]>([])

const isRunning = (exam: ExamForStudentVO) => {
  const now = Date.now()
  return now >= new Date(exam.starttime).getTime() && now < new Date(exam.endtime).getTime()
}

const enterExam = (examId: number) => {
  router.push(`/exam-taking/${examId}`)
}

onMounted(async () => {
  try {
    exams.value = await listAvailableExams()
  } catch { /* handled */ }
})
</script>
