<template>
  <div>
    <div style="display: flex; justify-content: space-between; margin-bottom: 16px">
      <div style="display: flex; gap: 12px">
        <el-select v-model="queryStatus" placeholder="状态筛选" clearable style="width: 150px" @change="loadData">
          <el-option label="草稿" value="draft" />
          <el-option label="已发布" value="publish" />
          <el-option label="进行中" value="running" />
          <el-option label="已结束" value="done" />
        </el-select>
        <el-button type="primary" @click="loadData">搜索</el-button>
      </div>
      <div style="display: flex; gap: 8px">
        <el-button type="primary" @click="openManualDialog">手动组卷</el-button>
        <el-button type="success" @click="openAutoDialog">自动组卷</el-button>
      </div>
    </div>

    <el-table :data="exams" border stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="exam" label="考试名称" show-overflow-tooltip />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="statusTagType[row.status]">{{ statusLabels[row.status] }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="starttime" label="开始时间" width="170" />
      <el-table-column prop="endtime" label="结束时间" width="170" />
      <el-table-column label="题目数" width="80">
        <template #default="{ row }">{{ row.questionItems?.length || 0 }}</template>
      </el-table-column>
      <el-table-column label="操作" width="260">
        <template #default="{ row }">
          <el-button size="small" @click="viewDetail(row.id)">详情</el-button>
          <el-button v-if="row.status === 'draft'" size="small" type="primary" @click="handlePublish(row.id)">发布</el-button>
          <el-button v-if="row.status === 'publish'" size="small" type="warning" @click="handleWithdraw(row.id)">撤回</el-button>
          <el-button v-if="row.status === 'draft'" size="small" type="danger" @click="handleDelete(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div style="display: flex; justify-content: flex-end; margin-top: 16px">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        @size-change="loadData"
        @current-change="loadData"
      />
    </div>

    <!-- 手动组卷对话框 -->
    <el-dialog v-model="manualDialogVisible" title="手动组卷" width="700px" destroy-on-close>
      <el-form ref="manualFormRef" :model="manualForm" :rules="manualRules" label-width="80px">
        <el-form-item label="考试名称" prop="exam">
          <el-input v-model="manualForm.exam" placeholder="输入考试名称" />
        </el-form-item>
        <el-form-item label="开始时间" prop="starttime">
          <el-date-picker v-model="manualForm.starttime" type="datetime" placeholder="选择开始时间" value-format="YYYY-MM-DDTHH:mm:ss" style="width: 100%" />
        </el-form-item>
        <el-form-item label="结束时间" prop="endtime">
          <el-date-picker v-model="manualForm.endtime" type="datetime" placeholder="选择结束时间" value-format="YYYY-MM-DDTHH:mm:ss" style="width: 100%" />
        </el-form-item>
        <el-form-item label="选择题目">
          <el-button size="small" @click="showQuestionPicker = true">从题库选题</el-button>
          <span style="margin-left: 12px; color: #909399">已选 {{ paperStore.items.length }} 题，总分 {{ paperStore.totalScore }}</span>
        </el-form-item>
        <el-table v-if="paperStore.items.length > 0" :data="paperStore.items" border size="small" style="margin-top: 8px">
          <el-table-column prop="questionId" label="题目ID" width="80" />
          <el-table-column prop="type" label="题型" width="100">
            <template #default="{ row }">
              <el-tag size="small">{{ typeLabels[row.type] || row.type }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="context" label="题干" show-overflow-tooltip />
          <el-table-column label="分值" width="100">
            <template #default="{ row }">
              <el-input-number v-model="row.score" :min="1" :max="100" size="small" />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="70">
            <template #default="{ $index }">
              <el-button size="small" type="danger" circle @click="paperStore.removeItem(paperStore.items[$index].questionId)">-</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-form>
      <template #footer>
        <el-button @click="manualDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleManualSubmit">创建考试</el-button>
      </template>
    </el-dialog>

    <!-- 题目选择对话框 -->
    <el-dialog v-model="showQuestionPicker" title="从题库选择题目" width="600px">
      <el-table :data="availableQuestions" border size="small" @selection-change="handleQuestionSelect">
        <el-table-column type="selection" width="50" />
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="type" label="题型" width="100">
          <template #default="{ row }">
            <el-tag size="small">{{ typeLabels[row.type] || row.type }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="context" label="题干" show-overflow-tooltip />
      </el-table>
      <template #footer>
        <el-button @click="showQuestionPicker = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 自动组卷对话框 -->
    <el-dialog v-model="autoDialogVisible" title="自动组卷" width="500px" destroy-on-close>
      <el-form ref="autoFormRef" :model="autoForm" :rules="autoRules" label-width="100px">
        <el-form-item label="考试名称" prop="exam">
          <el-input v-model="autoForm.exam" placeholder="输入考试名称" />
        </el-form-item>
        <el-form-item label="开始时间" prop="starttime">
          <el-date-picker v-model="autoForm.starttime" type="datetime" placeholder="选择开始时间" value-format="YYYY-MM-DDTHH:mm:ss" style="width: 100%" />
        </el-form-item>
        <el-form-item label="结束时间" prop="endtime">
          <el-date-picker v-model="autoForm.endtime" type="datetime" placeholder="选择结束时间" value-format="YYYY-MM-DDTHH:mm:ss" style="width: 100%" />
        </el-form-item>
        <el-form-item label="题目数量" prop="totalQuestions">
          <el-input-number v-model="autoForm.totalQuestions" :min="1" :max="100" />
        </el-form-item>
        <el-form-item label="总分" prop="totalScore">
          <el-input-number v-model="autoForm.totalScore" :min="1" :max="1000" />
        </el-form-item>
        <el-form-item label="题型过滤">
          <el-select v-model="autoForm.typeFilter" multiple placeholder="不限" style="width: 100%">
            <el-option label="单选题" value="SingleChoice" />
            <el-option label="多选题" value="MultipleChoice" />
            <el-option label="判断题" value="Judge" />
            <el-option label="填空题" value="Fill" />
            <el-option label="简答题" value="Essay" />
          </el-select>
        </el-form-item>
        <el-form-item label="高频降权">
          <el-switch v-model="autoForm.usePenalty" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="autoDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleAutoSubmit">创建考试</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { listExams, createManualExam, createAutoExam, publishExam, withdrawExam, deleteExam } from '@/api/exam'
import { listQuestions } from '@/api/question'
import { usePaperStore } from '@/stores/paper'
import type { ExamVO, QuestionType, QuestionVO, ExamCreateAutoReq } from '@/types'

const router = useRouter()
const paperStore = usePaperStore()

const typeLabels: Record<string, string> = {
  SingleChoice: '单选题', MultipleChoice: '多选题', Judge: '判断题', Fill: '填空题', Essay: '简答题',
}
const statusLabels: Record<string, string> = { draft: '草稿', publish: '已发布', running: '进行中', done: '已结束' }
const statusTagType: Record<string, string> = { draft: 'info', publish: 'warning', running: 'success', done: '' }

const exams = ref<ExamVO[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const queryStatus = ref<string>('')

const manualDialogVisible = ref(false)
const autoDialogVisible = ref(false)
const showQuestionPicker = ref(false)
const submitting = ref(false)

const availableQuestions = ref<QuestionVO[]>([])

const manualFormRef = ref<FormInstance>()
const autoFormRef = ref<FormInstance>()

const manualForm = reactive({
  exam: '',
  starttime: '',
  endtime: '',
})

const autoForm = reactive({
  exam: '',
  starttime: '',
  endtime: '',
  totalQuestions: 20,
  totalScore: 100,
  typeFilter: [] as QuestionType[],
  usePenalty: false,
})

const manualRules: FormRules = {
  exam: [{ required: true, message: '请输入考试名称', trigger: 'blur' }],
  starttime: [{ required: true, message: '请选择开始时间', trigger: 'change' }],
  endtime: [{ required: true, message: '请选择结束时间', trigger: 'change' }],
}

const autoRules: FormRules = {
  exam: [{ required: true, message: '请输入考试名称', trigger: 'blur' }],
  starttime: [{ required: true, message: '请选择开始时间', trigger: 'change' }],
  endtime: [{ required: true, message: '请选择结束时间', trigger: 'change' }],
  totalQuestions: [{ required: true, message: '请输入题目数量', trigger: 'blur' }],
  totalScore: [{ required: true, message: '请输入总分', trigger: 'blur' }],
}

const loadData = async () => {
  try {
    const res = await listExams({
      page: currentPage.value - 1,
      size: pageSize.value,
      status: queryStatus.value || undefined,
    })
    exams.value = res.content
    total.value = res.totalElements
  } catch { /* handled */ }
}

const openManualDialog = async () => {
  paperStore.clearAll()
  manualForm.exam = ''
  manualForm.starttime = ''
  manualForm.endtime = ''
  manualDialogVisible.value = true
  // 加载题库供选择
  try {
    const res = await listQuestions({ page: 0, size: 200 })
    availableQuestions.value = res.content
  } catch { /* handled */ }
}

const openAutoDialog = () => {
  autoForm.exam = ''
  autoForm.starttime = ''
  autoForm.endtime = ''
  autoForm.totalQuestions = 20
  autoForm.totalScore = 100
  autoForm.typeFilter = []
  autoForm.usePenalty = false
  autoDialogVisible.value = true
}

const handleQuestionSelect = (rows: QuestionVO[]) => {
  for (const row of rows) {
    paperStore.addItem({ questionId: row.id, score: 5, type: row.type, context: row.context })
  }
}

const handleManualSubmit = async () => {
  const valid = await manualFormRef.value?.validate().catch(() => false)
  if (!valid) return
  if (paperStore.items.length === 0) {
    ElMessage.warning('请至少选择一道题目')
    return
  }

  submitting.value = true
  try {
    await createManualExam({
      exam: manualForm.exam,
      starttime: manualForm.starttime,
      endtime: manualForm.endtime,
      items: paperStore.items.map(i => ({ questionId: i.questionId, score: i.score })),
    })
    ElMessage.success('组卷成功')
    paperStore.clearAll()
    manualDialogVisible.value = false
    loadData()
  } catch { /* handled */ } finally {
    submitting.value = false
  }
}

const handleAutoSubmit = async () => {
  const valid = await autoFormRef.value?.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    const data: ExamCreateAutoReq = {
      exam: autoForm.exam,
      starttime: autoForm.starttime,
      endtime: autoForm.endtime,
      autoRule: {
        totalQuestions: autoForm.totalQuestions,
        totalScore: autoForm.totalScore,
        typeFilter: autoForm.typeFilter.length > 0 ? autoForm.typeFilter : undefined,
        usePenalty: autoForm.usePenalty || undefined,
      },
    }
    await createAutoExam(data)
    ElMessage.success('自动组卷成功')
    autoDialogVisible.value = false
    loadData()
  } catch { /* handled */ } finally {
    submitting.value = false
  }
}

const viewDetail = (id: number) => {
  router.push(`/exams/${id}`)
}

const handlePublish = async (id: number) => {
  await ElMessageBox.confirm('确定发布该考试吗？', '确认发布', { type: 'info' })
  await publishExam(id)
  ElMessage.success('发布成功')
  loadData()
}

const handleWithdraw = async (id: number) => {
  await ElMessageBox.confirm('确定撤回该考试吗？', '确认撤回', { type: 'warning' })
  await withdrawExam(id)
  ElMessage.success('撤回成功')
  loadData()
}

const handleDelete = async (id: number) => {
  await ElMessageBox.confirm('确定删除该考试吗？仅草稿状态可删除。', '确认删除', { type: 'warning' })
  await deleteExam(id)
  ElMessage.success('删除成功')
  loadData()
}

onMounted(loadData)
</script>
