<template>
  <div>
    <h3 style="margin-bottom: 16px">评卷管理</h3>
    <el-empty v-if="pendingScores.length === 0" description="暂无待评卷" />
    <el-table v-else :data="pendingScores" border stripe>
      <el-table-column prop="id" label="分数ID" width="80" />
      <el-table-column prop="name" label="学生" width="120" />
      <el-table-column prop="examName" label="考试" />
      <el-table-column prop="all" label="当前得分" width="100" />
      <el-table-column label="操作" width="100">
        <template #default="{ row }">
          <el-button size="small" type="primary" @click="openGradeDialog(row)">评卷</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 评卷对话框 -->
    <el-dialog v-model="gradeDialogVisible" title="评卷" width="700px" destroy-on-close>
      <div v-for="item in currentDetail" :key="item.questionId" style="margin-bottom: 16px; padding: 12px; border: 1px solid #ebeef5; border-radius: 4px">
        <div style="font-weight: bold; margin-bottom: 8px">题目 #{{ item.questionId }} ({{ typeLabels[item.type] || item.type }})</div>
        <div style="margin-bottom: 4px"><strong>学生答案：</strong>{{ JSON.stringify(item.userAnswer) }}</div>
        <div style="margin-bottom: 4px"><strong>正确答案：</strong>{{ JSON.stringify(item.correctAnswer) }}</div>
        <div v-if="item.isCorrect === null" style="margin-top: 8px; display: flex; align-items: center; gap: 12px">
          <span>评分：</span>
          <el-input-number v-model="item.gradeScore" :min="0" :max="item.maxScore" size="small" />
          <span>/ {{ item.maxScore }}分</span>
          <el-button size="small" type="primary" @click="handleGrade(item)">提交评分</el-button>
        </div>
        <div v-else>
          <el-tag :type="item.isCorrect ? 'success' : 'danger'">{{ item.score }}/{{ item.maxScore }}分</el-tag>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getExamScores, getScoreById, gradeEssay } from '@/api/score'
import { listExams } from '@/api/exam'
import type { ScoreListVO, DetailItemVO } from '@/types'

const typeLabels: Record<string, string> = {
  SingleChoice: '单选题', MultipleChoice: '多选题', Judge: '判断题', Fill: '填空题', Essay: '简答题',
}

const pendingScores = ref<ScoreListVO[]>([])
const gradeDialogVisible = ref(false)
const currentDetail = ref<(DetailItemVO & { gradeScore?: number })[]>([])
const currentScoreId = ref<number | null>(null)

const loadPendingScores = async () => {
  try {
    const examRes = await listExams({ page: 0, size: 100 })
    const allScores: ScoreListVO[] = []
    for (const exam of examRes.content) {
      try {
        const scoreRes = await getExamScores(exam.id, { page: 0, size: 100 })
        allScores.push(...scoreRes.content)
      } catch { continue }
    }
    // 筛选含有未评卷简答题的分数记录
    const pending: ScoreListVO[] = []
    for (const s of allScores) {
      try {
        const detail = await getScoreById(s.id)
        if (detail.detail?.some(d => d.isCorrect === null)) {
          pending.push(s)
        }
      } catch { continue }
    }
    pendingScores.value = pending
  } catch { /* handled */ }
}

const openGradeDialog = async (score: ScoreListVO) => {
  try {
    const detail = await getScoreById(score.id)
    currentDetail.value = detail.detail.map(d => ({ ...d, gradeScore: 0 }))
    currentScoreId.value = score.id
    gradeDialogVisible.value = true
  } catch { /* handled */ }
}

const handleGrade = async (item: DetailItemVO & { gradeScore?: number }) => {
  if (!currentScoreId.value) return
  try {
    await gradeEssay(currentScoreId.value, {
      scoreId: currentScoreId.value,
      questionId: item.questionId,
      score: item.gradeScore ?? 0,
    })
    ElMessage.success('评分成功')
    item.isCorrect = item.gradeScore === item.maxScore
    item.score = item.gradeScore ?? 0
  } catch { /* handled */ }
}

onMounted(loadPendingScores)
</script>
