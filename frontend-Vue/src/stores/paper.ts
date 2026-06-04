import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { PaperItem } from '@/types'

export const usePaperStore = defineStore('paper', () => {
  const examName = ref('')
  const starttime = ref('')
  const endtime = ref('')
  const items = ref<PaperItem[]>([])

  const totalScore = computed(() => items.value.reduce((sum, item) => sum + item.score, 0))
  const totalQuestions = computed(() => items.value.length)

  function addItem(item: PaperItem) {
    if (!items.value.find(i => i.questionId === item.questionId)) {
      items.value.push(item)
    }
  }

  function removeItem(questionId: number) {
    items.value = items.value.filter(i => i.questionId !== questionId)
  }

  function reorderItems(newItems: PaperItem[]) {
    items.value = newItems
  }

  function updateItemScore(questionId: number, score: number) {
    const item = items.value.find(i => i.questionId === questionId)
    if (item) item.score = score
  }

  function clearAll() {
    examName.value = ''
    starttime.value = ''
    endtime.value = ''
    items.value = []
  }

  return { examName, starttime, endtime, items, totalScore, totalQuestions, addItem, removeItem, reorderItems, updateItemScore, clearAll }
}, {
  persist: true,
})
