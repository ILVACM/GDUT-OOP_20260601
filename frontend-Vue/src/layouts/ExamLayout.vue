<template>
  <div style="height: 100vh; display: flex; flex-direction: column">
    <div style="display: flex; align-items: center; justify-content: space-between; padding: 8px 20px; background-color: #409eff; color: #fff">
      <span style="font-size: 16px; font-weight: bold">{{ examStore.examName }}</span>
      <div style="display: flex; align-items: center; gap: 16px">
        <span style="font-size: 18px; font-weight: bold">剩余时间: {{ formattedTime }}</span>
        <el-button type="danger" @click="handleSubmit">交卷</el-button>
      </div>
    </div>
    <div style="flex: 1; overflow-y: auto; padding: 20px; background-color: #f5f5f5">
      <router-view />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { useExamStore } from '@/stores/exam'

const router = useRouter()
const examStore = useExamStore()

const remainingSeconds = ref(0)
let timer: ReturnType<typeof setInterval> | null = null

const formattedTime = computed(() => {
  const h = Math.floor(remainingSeconds.value / 3600)
  const m = Math.floor((remainingSeconds.value % 3600) / 60)
  const s = remainingSeconds.value % 60
  return `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
})

const startTimer = () => {
  const endtime = examStore.endtime
  if (!endtime) return
  const endTimeMs = new Date(endtime).getTime()

  const update = () => {
    const now = Date.now()
    remainingSeconds.value = Math.max(0, Math.floor((endTimeMs - now) / 1000))
    if (remainingSeconds.value <= 0 && timer) {
      clearInterval(timer)
      timer = null
      // 时间到自动交卷
      examStore.submitExam()
    }
  }
  update()
  timer = setInterval(update, 1000)
}

const handleSubmit = async () => {
  try {
    await ElMessageBox.confirm('确定要交卷吗？交卷后不可修改。', '交卷确认', {
      confirmButtonText: '确定交卷',
      cancelButtonText: '继续答题',
      type: 'warning',
    })
    await examStore.submitExam()
  } catch {
    // 用户取消
  }
}

onMounted(() => {
  startTimer()
})

onUnmounted(() => {
  if (timer) {
    clearInterval(timer)
    timer = null
  }
})
</script>
