<template>
  <div v-if="examInfo" style="max-width: 900px; margin: 0 auto">
    <div style="margin-bottom: 16px; display: flex; justify-content: space-between; align-items: center">
      <h3>{{ examInfo.exam }}</h3>
      <div style="color: #909399">
        共 {{ examInfo.questionItems.length }} 题 | 总分 {{ totalMaxScore }}
      </div>
    </div>

    <div v-for="(item, index) in examInfo.questionItems" :key="item.questionId" style="margin-bottom: 24px; padding: 16px; background: #fff; border-radius: 8px; border: 1px solid #ebeef5">
      <div style="display: flex; justify-content: space-between; margin-bottom: 12px">
        <div>
          <el-tag size="small" style="margin-right: 8px">{{ typeLabels[item.type] }}</el-tag>
          <strong>第 {{ index + 1 }} 题</strong>（{{ item.score }}分）
        </div>
        <el-tag v-if="answers[item.questionId] !== undefined" type="success" size="small">已作答</el-tag>
      </div>
      <div style="margin-bottom: 12px; line-height: 1.6">{{ item.context }}</div>

      <!-- 题目图片 -->
      <div v-if="item.imageUrl" style="margin-bottom: 12px">
        <el-image
          :src="item.imageUrl"
          :preview-src-list="[item.imageUrl]"
          fit="contain"
          style="max-width: 100%; max-height: 400px"
          alt="题目图片"
          lazy
        >
          <template #error>
            <div style="color: #909399; font-size: 12px">图片加载失败</div>
          </template>
        </el-image>
      </div>

      <!-- 单选题 -->
      <el-radio-group v-if="item.type === 'SingleChoice'" :model-value="answers[item.questionId]" @update:model-value="val => updateAnswer(item.questionId, val)">
        <div v-for="opt in item.options || []" :key="opt" style="margin-bottom: 8px">
          <el-radio :value="opt">{{ opt }}</el-radio>
        </div>
      </el-radio-group>

      <!-- 多选题 -->
      <el-checkbox-group v-if="item.type === 'MultipleChoice'" :model-value="answers[item.questionId] || []" @update:model-value="val => updateAnswer(item.questionId, val)">
        <div v-for="opt in item.options || []" :key="opt" style="margin-bottom: 8px">
          <el-checkbox :value="opt">{{ opt }}</el-checkbox>
        </div>
      </el-checkbox-group>

      <!-- 判断题 -->
      <el-radio-group v-if="item.type === 'Judge'" :model-value="answers[item.questionId]" @update:model-value="val => updateAnswer(item.questionId, val)">
        <el-radio :value="true">正确</el-radio>
        <el-radio :value="false">错误</el-radio>
      </el-radio-group>

      <!-- 填空题 -->
      <div v-if="item.type === 'Fill'">
        <div v-for="(_, i) in getBlankCount(item.questionId)" :key="i" style="margin-bottom: 8px">
          <el-input
            :model-value="(answers[item.questionId] || [])[i]"
            @update:model-value="val => updateFillAnswer(item.questionId, i, val)"
            :placeholder="'第 ' + (i + 1) + ' 空'"
            style="max-width: 400px"
          />
        </div>
        <el-button size="small" @click="addBlank(item.questionId)" style="margin-top: 4px">+ 增加空</el-button>
      </div>

      <!-- 简答题 -->
      <el-input
        v-if="item.type === 'Essay'"
        :model-value="answers[item.questionId]"
        @update:model-value="val => updateAnswer(item.questionId, val)"
        type="textarea"
        :rows="4"
        placeholder="请输入答案"
      />
    </div>

    <div style="text-align: center; margin: 24px 0">
      <el-button type="primary" size="large" @click="handleSubmit" :loading="submitting">
        提交答卷
      </el-button>
    </div>
  </div>
  <div v-else style="text-align: center; padding: 40px">
    <el-icon class="is-loading" :size="32"><Loading /></el-icon>
    <p>加载中...</p>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Loading } from '@element-plus/icons-vue'
import { useExamStore } from '@/stores/exam'
import { getExamPreview } from '@/api/exam'
import type { ExamForStudentVO } from '@/types'

const route = useRoute()
const router = useRouter()
const examStore = useExamStore()

const examInfo = ref<ExamForStudentVO | null>(null)
const answers = computed(() => examStore.answers)
const submitting = ref(false)

// 填空题空数追踪（questionId → 空数）
const blankCounts = ref<Record<number, number>>({})

let autoSaveTimer: ReturnType<typeof setInterval> | null = null

const typeLabels: Record<string, string> = {
  SingleChoice: '单选题', MultipleChoice: '多选题', Judge: '判断题', Fill: '填空题', Essay: '简答题',
}

const totalMaxScore = computed(() => {
  if (!examInfo.value) return 0
  return examInfo.value.questionItems.reduce((sum, item) => sum + item.score, 0)
})

const updateAnswer = (questionId: number, answer: any) => {
  examStore.updateAnswer(questionId, answer)
}

const updateFillAnswer = (questionId: number, blankIndex: number, value: string) => {
  const current = (examStore.answers[questionId] || []) as string[]
  const newAnswers = [...current]
  newAnswers[blankIndex] = value
  examStore.updateAnswer(questionId, newAnswers)
}

// 填空题：获取当前空数，默认1空
const getBlankCount = (questionId: number): number[] => {
  const count = blankCounts.value[questionId] || 1
  return Array.from({ length: count })
}

const addBlank = (questionId: number) => {
  blankCounts.value[questionId] = (blankCounts.value[questionId] || 1) + 1
}

const handleSubmit = async () => {
  try {
    await ElMessageBox.confirm('确定要提交答卷吗？提交后不可修改。', '确认提交', {
      confirmButtonText: '确定提交',
      cancelButtonText: '再检查一下',
      type: 'warning',
    })
  } catch {
    return
  }

  submitting.value = true
  try {
    const result = await examStore.submitExam()
    ElMessage.success('提交成功！')
    // 跳转到成绩详情页
    router.push(`/exam-result/${result.id}`)
  } catch {
    ElMessage.error('提交失败，请重试')
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  const examId = Number(route.params.examId)
  if (!examId) {
    ElMessage.error('无效的考试ID')
    router.push('/my-exams')
    return
  }

  try {
    // 如果 ExamStore 中已有此考试数据，说明是断点续答
    if (examStore.examId === examId && examStore.examInfo) {
      examInfo.value = examStore.examInfo
      // 尝试加载草稿
      await examStore.loadDraftAnswers()
      return
    }

    // 全新进入答题
    const data = await getExamPreview(examId)
    examInfo.value = data
    examStore.initExam(data)

    // 尝试加载草稿
    await examStore.loadDraftAnswers()
  } catch {
    ElMessage.error('加载考试失败')
    router.push('/my-exams')
  }

  // 每 30 秒自动保存草稿
  autoSaveTimer = setInterval(async () => {
    try {
      await examStore.saveDraft()
    } catch { /* 静默 */ }
  }, 30000)
})

onUnmounted(() => {
  if (autoSaveTimer) {
    clearInterval(autoSaveTimer)
    autoSaveTimer = null
  }
})
</script>
