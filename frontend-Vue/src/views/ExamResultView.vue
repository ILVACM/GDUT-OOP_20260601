<template>
  <div v-if="scoreDetail" style="max-width: 900px; margin: 0 auto">
    <el-card shadow="hover" style="margin-bottom: 20px">
      <div style="text-align: center">
        <h2>{{ scoreDetail.examName }}</h2>
        <div style="font-size: 48px; font-weight: bold; margin: 16px 0" :style="{ color: scoreDetail.all >= scoreDetail.maxScore * 0.6 ? '#67c23a' : '#f56c6c' }">
          {{ scoreDetail.all }}
        </div>
        <div style="color: #909399">满分 {{ scoreDetail.maxScore }} | 正确率 {{ (scoreDetail.accuracy * 100).toFixed(1) }}%</div>
      </div>
    </el-card>

    <h3 style="margin-bottom: 12px">答题详情</h3>
    <div v-for="(item, index) in scoreDetail.detail" :key="item.questionId" style="margin-bottom: 16px; padding: 16px; background: #fff; border-radius: 8px; border: 1px solid #ebeef5">
      <div style="display: flex; justify-content: space-between; margin-bottom: 8px">
        <div>
          <el-tag size="small" style="margin-right: 8px">{{ typeLabels[item.type] }}</el-tag>
          <strong>第 {{ index + 1 }} 题</strong>
        </div>
        <div>
          <el-tag :type="item.isCorrect === true ? 'success' : item.isCorrect === false ? 'danger' : 'warning'" size="small">
            {{ item.score }}/{{ item.maxScore }}分
          </el-tag>
        </div>
      </div>
      <div style="margin-bottom: 8px; line-height: 1.6">{{ item.context }}</div>
      <div style="margin-bottom: 8px">
        <strong>你的答案：</strong>
        <span :style="{ color: item.isCorrect ? '#67c23a' : '#f56c6c' }">{{ formatAnswer(item.userAnswer) }}</span>
      </div>
      <div>
        <strong>正确答案：</strong>
        <span style="color: #67c23a">{{ formatAnswer(item.correctAnswer) }}</span>
      </div>
    </div>

    <div style="text-align: center; margin-top: 24px">
      <el-button type="primary" @click="router.push('/')">返回首页</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getScoreById } from '@/api/score'
import type { ScoreVO } from '@/types'

const route = useRoute()
const router = useRouter()

const scoreDetail = ref<ScoreVO | null>(null)

const typeLabels: Record<string, string> = {
  SingleChoice: '单选题', MultipleChoice: '多选题', Judge: '判断题', Fill: '填空题', Essay: '简答题',
}

const formatAnswer = (answer: any): string => {
  if (answer === null || answer === undefined) return '未作答'
  if (typeof answer === 'boolean') return answer ? '正确' : '错误'
  if (Array.isArray(answer)) return answer.join(', ')
  return String(answer)
}

onMounted(async () => {
  const scoreId = Number(route.params.scoreId)
  if (!scoreId) {
    ElMessage.error('无效的成绩ID')
    router.push('/')
    return
  }
  try {
    scoreDetail.value = await getScoreById(scoreId)
  } catch {
    ElMessage.error('加载成绩失败')
    router.push('/')
  }
})
</script>
