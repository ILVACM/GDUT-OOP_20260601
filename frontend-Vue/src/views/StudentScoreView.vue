<template>
  <div>
    <el-tabs v-model="activeTab">
      <el-tab-pane label="我的成绩" name="scores">
        <el-table :data="scores" border stripe>
          <el-table-column prop="examName" label="考试名称" />
          <el-table-column prop="totalScore" label="得分" width="100" />
          <el-table-column prop="maxScore" label="满分" width="100" />
          <el-table-column prop="accuracy" label="正确率" width="100">
            <template #default="{ row }">
              {{ row.accuracy != null ? (row.accuracy * 100).toFixed(1) + '%' : '-' }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="100">
            <template #default="{ row }">
              <el-button size="small" @click="viewDetail(row.id)">查看详情</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div style="display: flex; justify-content: flex-end; margin-top: 16px">
          <el-pagination
            v-model:current-page="page"
            v-model:page-size="size"
            :total="total"
            layout="total, prev, pager, next"
            @current-change="loadScores"
          />
        </div>
      </el-tab-pane>
      <el-tab-pane label="我的错题" name="mistakes">
        <el-table :data="mistakes" border stripe>
          <el-table-column prop="type" label="题型" width="100">
            <template #default="{ row }">
              <el-tag size="small">{{ typeLabels[row.type] || row.type }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="context" label="题干" show-overflow-tooltip />
          <el-table-column label="我的答案" width="150">
            <template #default="{ row }">{{ JSON.stringify(row.userAnswer) }}</template>
          </el-table-column>
          <el-table-column label="正确答案" width="150">
            <template #default="{ row }">{{ JSON.stringify(row.correctAnswer) }}</template>
          </el-table-column>
          <el-table-column prop="examName" label="来源考试" width="180" />
        </el-table>
        <div style="display: flex; justify-content: flex-end; margin-top: 16px">
          <el-pagination
            v-model:current-page="mPage"
            v-model:page-size="mSize"
            :total="mTotal"
            layout="total, prev, pager, next"
            @current-change="loadMistakes"
          />
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getMyScores, getMyMistakes } from '@/api/score'
import type { ScoreListVO, MistakeItemVO } from '@/types'

const router = useRouter()
const typeLabels: Record<string, string> = {
  SingleChoice: '单选题', MultipleChoice: '多选题', Judge: '判断题', Fill: '填空题', Essay: '简答题',
}

const activeTab = ref('scores')
const scores = ref<ScoreListVO[]>([])
const page = ref(1)
const size = ref(10)
const total = ref(0)

const mistakes = ref<MistakeItemVO[]>([])
const mPage = ref(1)
const mSize = ref(10)
const mTotal = ref(0)

const loadScores = async () => {
  try {
    const res = await getMyScores({ page: page.value - 1, size: size.value })
    scores.value = res.content
    total.value = res.totalElements
  } catch { /* handled */ }
}

const loadMistakes = async () => {
  try {
    const res = await getMyMistakes({ page: mPage.value - 1, size: mSize.value })
    mistakes.value = res.content
    mTotal.value = res.totalElements
  } catch { /* handled */ }
}

const viewDetail = (scoreId: number) => {
  router.push(`/exam-result/${scoreId}`)
}

onMounted(() => {
  loadScores()
  loadMistakes()
})
</script>
