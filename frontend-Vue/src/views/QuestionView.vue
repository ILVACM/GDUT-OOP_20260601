<template>
  <div>
    <div style="display: flex; justify-content: space-between; margin-bottom: 16px">
      <div style="display: flex; gap: 12px">
        <el-select v-model="queryType" placeholder="题型筛选" clearable style="width: 150px" @change="loadData">
          <el-option label="单选题" value="SingleChoice" />
          <el-option label="多选题" value="MultipleChoice" />
          <el-option label="判断题" value="Judge" />
          <el-option label="填空题" value="Fill" />
          <el-option label="简答题" value="Essay" />
        </el-select>
        <el-input v-model="queryKeyword" placeholder="搜索题干关键字" clearable style="width: 200px" @keyup.enter="loadData" />
        <el-button type="primary" @click="loadData">搜索</el-button>
      </div>
      <div style="display: flex; gap: 8px">
        <el-button v-permission="['teacher', 'admin']" type="primary" @click="openCreateDialog">创建题目</el-button>
        <el-button v-permission="['teacher', 'admin']" type="danger" :disabled="selectedIds.length === 0" @click="handleBatchDelete">批量删除</el-button>
      </div>
    </div>

    <el-table :data="questions" border stripe @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="50" />
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="type" label="题型" width="120">
        <template #default="{ row }">
          <el-tag>{{ typeLabels[row.type] || row.type }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="context" label="题干" show-overflow-tooltip />
      <el-table-column prop="img" label="图片" width="70">
        <template #default="{ row }">
          <el-tag :type="row.img === 1 ? 'success' : 'info'" size="small">{{ row.img === 1 ? '有' : '无' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="use" label="使用次数" width="90" />
      <el-table-column prop="accuracy" label="正确率" width="90">
        <template #default="{ row }">
          {{ row.accuracy != null ? (row.accuracy * 100).toFixed(1) + '%' : '-' }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160" v-permission="['teacher', 'admin']">
        <template #default="{ row }">
          <el-button size="small" @click="openEditDialog(row)">编辑</el-button>
          <el-button size="small" type="danger" @click="handleDelete(row.id)">删除</el-button>
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

    <!-- 创建/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑题目' : '创建题目'" width="600px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="80px">
        <el-form-item label="题型" prop="type">
          <el-select v-model="form.type" placeholder="选择题型" :disabled="isEdit" style="width: 100%">
            <el-option label="单选题" value="SingleChoice" />
            <el-option label="多选题" value="MultipleChoice" />
            <el-option label="判断题" value="Judge" />
            <el-option label="填空题" value="Fill" />
            <el-option label="简答题" value="Essay" />
          </el-select>
        </el-form-item>
        <el-form-item label="题干" prop="context">
          <el-input v-model="form.context" type="textarea" :rows="3" placeholder="输入题干内容" />
        </el-form-item>
        <el-form-item label="是否有图">
          <el-switch v-model="form.imgBool" active-text="有图" inactive-text="无图" />
        </el-form-item>

        <!-- 单选题答案 -->
        <template v-if="form.type === 'SingleChoice'">
          <el-form-item label="选项">
            <div v-for="(_, i) in form.options" :key="i" style="display: flex; gap: 8px; margin-bottom: 8px">
              <el-input v-model="form.options[i]" :placeholder="'选项 ' + String.fromCharCode(65 + i)" />
              <el-button v-if="form.options.length > 2" type="danger" circle size="small" @click="form.options.splice(i, 1)">-</el-button>
            </div>
            <el-button v-if="form.options.length < 6" size="small" @click="form.options.push('')">添加选项</el-button>
          </el-form-item>
          <el-form-item label="正确答案" prop="correctOption">
            <el-select v-model="form.correctOption" placeholder="选择正确选项">
              <el-option v-for="(_, i) in form.options" :key="i" :label="String.fromCharCode(65 + i)" :value="String.fromCharCode(65 + i)" />
            </el-select>
          </el-form-item>
        </template>

        <!-- 多选题答案 -->
        <template v-if="form.type === 'MultipleChoice'">
          <el-form-item label="选项">
            <div v-for="(_, i) in form.options" :key="i" style="display: flex; gap: 8px; margin-bottom: 8px">
              <el-input v-model="form.options[i]" :placeholder="'选项 ' + String.fromCharCode(65 + i)" />
              <el-button v-if="form.options.length > 2" type="danger" circle size="small" @click="form.options.splice(i, 1)">-</el-button>
            </div>
            <el-button v-if="form.options.length < 6" size="small" @click="form.options.push('')">添加选项</el-button>
          </el-form-item>
          <el-form-item label="正确答案" prop="correctOptions">
            <el-select v-model="form.correctOptions" multiple placeholder="选择正确选项">
              <el-option v-for="(_, i) in form.options" :key="i" :label="String.fromCharCode(65 + i)" :value="String.fromCharCode(65 + i)" />
            </el-select>
          </el-form-item>
        </template>

        <!-- 判断题答案 -->
        <template v-if="form.type === 'Judge'">
          <el-form-item label="正确答案" prop="judgeCorrect">
            <el-radio-group v-model="form.judgeCorrect">
              <el-radio :value="true">正确</el-radio>
              <el-radio :value="false">错误</el-radio>
            </el-radio-group>
          </el-form-item>
        </template>

        <!-- 填空题答案 -->
        <template v-if="form.type === 'Fill'">
          <el-form-item label="填空答案">
            <div v-for="(_, i) in form.blanks" :key="i" style="display: flex; gap: 8px; margin-bottom: 8px">
              <el-input v-model="form.blanks[i]" :placeholder="'第 ' + (i + 1) + ' 空'" />
              <el-button v-if="form.blanks.length > 1" type="danger" circle size="small" @click="form.blanks.splice(i, 1)">-</el-button>
            </div>
            <el-button size="small" @click="form.blanks.push('')">添加空</el-button>
          </el-form-item>
        </template>

        <!-- 简答题答案 -->
        <template v-if="form.type === 'Essay'">
          <el-form-item label="参考答案">
            <el-input v-model="form.reference" type="textarea" :rows="3" placeholder="参考答案" />
          </el-form-item>
          <el-form-item label="关键词">
            <el-select v-model="form.keywords" multiple filterable allow-create default-first-option placeholder="输入关键词回车添加" style="width: 100%" />
          </el-form-item>
        </template>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { listQuestions, createQuestion, updateQuestion, deleteQuestion, batchDeleteQuestions } from '@/api/question'
import type { QuestionVO, QuestionType, QuestionCreateReq } from '@/types'

const typeLabels: Record<string, string> = {
  SingleChoice: '单选题', MultipleChoice: '多选题', Judge: '判断题', Fill: '填空题', Essay: '简答题',
}

const questions = ref<QuestionVO[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const queryType = ref<QuestionType | ''>('')
const queryKeyword = ref('')
const selectedIds = ref<number[]>([])

const dialogVisible = ref(false)
const isEdit = ref(false)
const editId = ref<number | null>(null)
const submitting = ref(false)
const formRef = ref<FormInstance>()

const form = reactive({
  type: 'SingleChoice' as QuestionType,
  context: '',
  imgBool: false,
  options: ['A', 'B', 'C', 'D'] as string[],
  correctOption: '',
  correctOptions: [] as string[],
  judgeCorrect: true,
  blanks: [''] as string[],
  reference: '',
  keywords: [] as string[],
})

const formRules: FormRules = {
  type: [{ required: true, message: '请选择题型', trigger: 'change' }],
  context: [{ required: true, message: '请输入题干', trigger: 'blur' }],
}

const buildAnswer = (): any => {
  switch (form.type) {
    case 'SingleChoice':
      return { version: 1, type: 'SingleChoice', correctOption: form.correctOption, options: form.options }
    case 'MultipleChoice':
      return { version: 1, type: 'MultipleChoice', correctOptions: form.correctOptions, options: form.options }
    case 'Judge':
      return { version: 1, type: 'Judge', correct: form.judgeCorrect }
    case 'Fill':
      return { version: 1, type: 'Fill', blanks: form.blanks }
    case 'Essay':
      return { version: 1, type: 'Essay', reference: form.reference, keywords: form.keywords, scoreRule: '' }
    default:
      return {}
  }
}

const resetForm = () => {
  form.type = 'SingleChoice'
  form.context = ''
  form.imgBool = false
  form.options = ['A', 'B', 'C', 'D']
  form.correctOption = ''
  form.correctOptions = []
  form.judgeCorrect = true
  form.blanks = ['']
  form.reference = ''
  form.keywords = []
}

const loadData = async () => {
  try {
    const res = await listQuestions({
      type: queryType.value || undefined,
      keyword: queryKeyword.value || undefined,
      page: currentPage.value - 1,
      size: pageSize.value,
    })
    questions.value = res.content
    total.value = res.totalElements
  } catch { /* handled by interceptor */ }
}

const handleSelectionChange = (rows: QuestionVO[]) => {
  selectedIds.value = rows.map(r => r.id)
}

const openCreateDialog = () => {
  isEdit.value = false
  editId.value = null
  resetForm()
  dialogVisible.value = true
}

const openEditDialog = (row: QuestionVO) => {
  isEdit.value = true
  editId.value = row.id
  form.type = row.type
  form.context = row.context
  form.imgBool = row.img === 1

  const ans = row.answer as any
  if (row.type === 'SingleChoice') {
    form.options = ans.options || ['A', 'B', 'C', 'D']
    form.correctOption = ans.correctOption || ''
  } else if (row.type === 'MultipleChoice') {
    form.options = ans.options || ['A', 'B', 'C', 'D']
    form.correctOptions = ans.correctOptions || []
  } else if (row.type === 'Judge') {
    form.judgeCorrect = ans.correct ?? true
  } else if (row.type === 'Fill') {
    form.blanks = ans.blanks || ['']
  } else if (row.type === 'Essay') {
    form.reference = ans.reference || ''
    form.keywords = ans.keywords || []
  }

  dialogVisible.value = true
}

const handleSubmit = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    const data: QuestionCreateReq = {
      type: form.type,
      context: form.context,
      img: form.imgBool ? 1 : 0,
      answer: JSON.stringify(buildAnswer()),
    }
    if (isEdit.value && editId.value) {
      await updateQuestion(editId.value, data)
      ElMessage.success('更新成功')
    } else {
      await createQuestion(data)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    loadData()
  } catch { /* handled */ } finally {
    submitting.value = false
  }
}

const handleDelete = async (id: number) => {
  await ElMessageBox.confirm('确定删除该题目吗？', '确认删除', { type: 'warning' })
  await deleteQuestion(id)
  ElMessage.success('删除成功')
  loadData()
}

const handleBatchDelete = async () => {
  await ElMessageBox.confirm(`确定删除选中的 ${selectedIds.value.length} 道题目吗？`, '确认删除', { type: 'warning' })
  await batchDeleteQuestions(selectedIds.value)
  ElMessage.success('批量删除成功')
  loadData()
}

onMounted(loadData)
</script>
