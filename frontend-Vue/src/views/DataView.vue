<template>
  <div>
    <el-tabs v-model="activeTab">
      <el-tab-pane label="考试统计" name="exam">
        <div style="margin-bottom: 16px">
          <el-select v-model="selectedExamId" placeholder="选择考试" style="width: 300px" @change="loadExamStats">
            <el-option v-for="e in examList" :key="e.id" :label="e.exam" :value="e.id" />
          </el-select>
        </div>
        <el-descriptions v-if="examStats" :column="3" border>
          <el-descriptions-item label="考试名称">{{ examStats.examName }}</el-descriptions-item>
          <el-descriptions-item label="参加人数">{{ examStats.participantCount }}</el-descriptions-item>
          <el-descriptions-item label="提交人数">{{ examStats.submitCount }}</el-descriptions-item>
          <el-descriptions-item label="通过率">{{ (examStats.passRate * 100).toFixed(1) }}%</el-descriptions-item>
          <el-descriptions-item label="平均分">{{ examStats.averageScore?.toFixed(1) }}</el-descriptions-item>
          <el-descriptions-item label="最高分">{{ examStats.maxScore }}</el-descriptions-item>
          <el-descriptions-item label="最低分">{{ examStats.minScore }}</el-descriptions-item>
          <el-descriptions-item label="中位数">{{ examStats.medianScore }}</el-descriptions-item>
        </el-descriptions>
        <div v-if="examStats?.scoreDistribution" style="margin-top: 16px">
          <h4>成绩分布</h4>
          <div v-for="(count, range) in examStats.scoreDistribution" :key="range" style="display: flex; align-items: center; margin-bottom: 4px">
            <span style="width: 80px">{{ range }}分</span>
            <el-progress :percentage="examStats.submitCount ? (count / examStats.submitCount * 100) : 0" :stroke-width="18" :text-inside="true" style="flex: 1" />
            <span style="margin-left: 8px; width: 40px">{{ count }}人</span>
          </div>
        </div>
      </el-tab-pane>
      <el-tab-pane label="题目统计" name="question">
        <el-table :data="questionStats" border stripe>
          <el-table-column prop="questionId" label="题目ID" width="90" />
          <el-table-column prop="type" label="题型" width="120">
            <template #default="{ row }">
              <el-tag size="small">{{ typeLabels[row.type] || row.type }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="use" label="使用次数" width="100" />
          <el-table-column prop="correct" label="正确次数" width="100" />
          <el-table-column prop="accuracyRate" label="正确率" width="100">
            <template #default="{ row }">
              {{ row.accuracyRate != null ? (row.accuracyRate * 100).toFixed(1) + '%' : '-' }}
            </template>
          </el-table-column>
        </el-table>
        <div style="display: flex; justify-content: flex-end; margin-top: 16px">
          <el-pagination
            v-model:current-page="qPage"
            v-model:page-size="qSize"
            :total="qTotal"
            layout="total, prev, pager, next"
            @current-change="loadQuestionStats"
          />
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { listExams } from '@/api/exam'
import { getExamStatistics, getQuestionStatistics } from '@/api/score'
import type { ExamVO, ExamStatisticsVO, QuestionStatisticsVO } from '@/types'

const typeLabels: Record<string, string> = {
  SingleChoice: '单选题', MultipleChoice: '多选题', Judge: '判断题', Fill: '填空题', Essay: '简答题',
}

const activeTab = ref('exam')
const examList = ref<ExamVO[]>([])
const selectedExamId = ref<number | null>(null)
const examStats = ref<ExamStatisticsVO | null>(null)

const questionStats = ref<QuestionStatisticsVO[]>([])
const qPage = ref(1)
const qSize = ref(20)
const qTotal = ref(0)

const loadExamStats = async () => {
  if (!selectedExamId.value) return
  try {
    examStats.value = await getExamStatistics(selectedExamId.value)
  } catch { /* handled */ }
}

const loadQuestionStats = async () => {
  try {
    const res = await getQuestionStatistics({ page: qPage.value - 1, size: qSize.value })
    questionStats.value = res.content
    qTotal.value = res.totalElements
  } catch { /* handled */ }
}

onMounted(async () => {
  try {
    const res = await listExams({ page: 0, size: 100 })
    examList.value = res.content
  } catch { /* handled */ }
  loadQuestionStats()
})
</script>
