<template>
  <div class="page">
    <el-button link class="back" @click="$router.back()">
      <el-icon><ArrowLeft /></el-icon> Back
    </el-button>

    <div class="section-head">
      <div>
        <h1 class="page-title">Question bank</h1>
        <p class="page-subtitle" style="margin: 0">{{ quizTitle || 'Loading quiz ...' }}</p>
      </div>
      <el-button type="primary" @click="openCreate">
        <el-icon><Plus /></el-icon>&nbsp;Add question
      </el-button>
    </div>

    <div v-loading="loading" class="list">
      <div v-for="(question, index) in questions" :key="question.id" class="card q">
        <div class="q-head">
          <span class="q-index">{{ index + 1 }}</span>
          <div class="q-content">{{ question.content }}</div>
          <div class="q-tags">
            <el-tag size="small" type="info" effect="plain" round>{{ questionTypeLabel(question.type) }}</el-tag>
            <el-tag size="small" effect="plain" round>{{ question.score }} pts</el-tag>
          </div>
        </div>

        <div class="options">
          <div v-for="option in question.options" :key="option.id" class="opt"
               :class="{ correct: isCorrect(question, option.optionKey) }">
            <span class="opt-key">{{ option.optionKey }}</span>
            <span class="opt-text">{{ option.content }}</span>
            <el-icon v-if="isCorrect(question, option.optionKey)" class="mark"><CircleCheckFilled /></el-icon>
          </div>
        </div>

        <div v-if="question.analysis" class="analysis muted tiny">
          Explanation: {{ question.analysis }}
        </div>

        <div class="q-actions">
          <el-button size="small" @click="openEdit(question)">Edit</el-button>
          <el-button size="small" type="danger" plain @click="remove(question)">Delete</el-button>
        </div>
      </div>
    </div>

    <EmptyState v-if="!loading && !questions.length" icon="EditPen"
                title="No question in this quiz"
                description="Add single choice, multiple choice or true/false questions. The backend grades them automatically." />

    <el-dialog v-model="dialog" :title="editing ? 'Edit question' : 'New question'" width="680px"
               top="6vh">
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="Question" prop="content">
          <el-input v-model="form.content" type="textarea" :rows="2"
                    placeholder="What is the size of an int in Java?" />
        </el-form-item>

        <div class="two-col">
          <el-form-item label="Type">
            <el-select v-model="form.type" style="width: 100%" @change="onTypeChange">
              <el-option v-for="item in QUESTION_TYPES" :key="item.value"
                         :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="Score">
            <el-input-number v-model="form.score" :min="1" :max="100" :step="5"
                             controls-position="right" style="width: 100%" />
          </el-form-item>
        </div>

        <el-form-item label="Options">
          <div class="opt-editor">
            <div v-for="(option, index) in form.options" :key="index" class="opt-row">
              <el-checkbox v-if="form.type === 'MULTIPLE'" :model-value="isMarked(option.optionKey)"
                           @change="(value) => markOption(option.optionKey, value)" />
              <el-radio v-else :model-value="form.correctAnswer" :value="option.optionKey"
                        @change="form.correctAnswer = option.optionKey">&nbsp;</el-radio>
              <span class="opt-key">{{ option.optionKey }}</span>
              <el-input v-model="option.content" placeholder="Option text" />
              <el-button text type="danger" :disabled="form.options.length <= 2"
                         @click="removeOption(index)">
                <el-icon><Delete /></el-icon>
              </el-button>
            </div>
          </div>
          <el-button size="small" plain class="add-opt" @click="addOption">
            <el-icon><Plus /></el-icon>&nbsp;Add option
          </el-button>
        </el-form-item>

        <el-form-item label="Correct answer">
          <el-input v-model="form.correctAnswer"
                    :placeholder="form.type === 'TRUE_FALSE' ? 'T or F' : form.type === 'MULTIPLE' ? 'A,C' : 'A'" />
          <div class="muted tiny hint">
            Use option keys. Separate multiple answers with commas, e.g. <code>A,C</code>.
          </div>
        </el-form-item>

        <el-form-item label="Explanation (shown after submitting)">
          <el-input v-model="form.analysis" type="textarea" :rows="2" />
        </el-form-item>

        <el-form-item label="Order">
          <el-input-number v-model="form.sortOrder" :min="1" :max="200"
                           controls-position="right" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialog = false">Cancel</el-button>
        <el-button type="primary" :loading="saving" @click="save">
          {{ editing ? 'Save changes' : 'Add question' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminApi, quizApi } from '@/api'
import EmptyState from '@/components/EmptyState.vue'
import { QUESTION_TYPES, questionTypeLabel } from '@/utils/format'

const route = useRoute()
const quizId = route.params.id

const loading = ref(false)
const saving = ref(false)
const questions = ref([])
const quizTitle = ref('')

const dialog = ref(false)
const editing = ref(false)
const editingId = ref(null)
const formRef = ref()

const form = reactive({
  content: '',
  type: 'SINGLE',
  correctAnswer: '',
  score: 10,
  analysis: '',
  sortOrder: 1,
  options: []
})

const rules = {
  content: [{ required: true, message: 'Please write the question', trigger: 'blur' }],
  correctAnswer: [{ required: true, message: 'Please provide the correct answer', trigger: 'blur' }]
}

function isCorrect(question, key) {
  return String(question.correctAnswer || '')
    .split(',')
    .map((part) => part.trim())
    .includes(key)
}

function isMarked(key) {
  return String(form.correctAnswer || '')
    .split(',')
    .map((part) => part.trim())
    .includes(key)
}

function markOption(key, checked) {
  const keys = String(form.correctAnswer || '')
    .split(',')
    .map((part) => part.trim())
    .filter(Boolean)
  const next = checked ? [...new Set([...keys, key])] : keys.filter((item) => item !== key)
  form.correctAnswer = next.sort().join(',')
}

function letter(index) {
  return String.fromCharCode(65 + index)
}

function addOption() {
  form.options.push({ optionKey: letter(form.options.length), content: '' })
}

function removeOption(index) {
  form.options.splice(index, 1)
  form.options.forEach((option, i) => {
    option.optionKey = letter(i)
  })
}

function onTypeChange(type) {
  if (type === 'TRUE_FALSE') {
    form.options = [
      { optionKey: 'T', content: 'True' },
      { optionKey: 'F', content: 'False' }
    ]
    form.correctAnswer = 'T'
  } else if (form.options.length < 2) {
    form.options = [
      { optionKey: 'A', content: '' },
      { optionKey: 'B', content: '' },
      { optionKey: 'C', content: '' },
      { optionKey: 'D', content: '' }
    ]
  }
}

async function load() {
  loading.value = true
  try {
    questions.value = await adminApi.questions(quizId)
    if (!quizTitle.value) {
      const detail = await quizApi.detail(quizId)
      quizTitle.value = detail.quiz.title
    }
  } catch (error) {
    questions.value = []
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editing.value = false
  editingId.value = null
  Object.assign(form, {
    content: '',
    type: 'SINGLE',
    correctAnswer: 'A',
    score: 10,
    analysis: '',
    sortOrder: questions.value.length + 1,
    options: [
      { optionKey: 'A', content: '' },
      { optionKey: 'B', content: '' },
      { optionKey: 'C', content: '' },
      { optionKey: 'D', content: '' }
    ]
  })
  dialog.value = true
}

function openEdit(question) {
  editing.value = true
  editingId.value = question.id
  Object.assign(form, {
    content: question.content,
    type: question.type,
    correctAnswer: question.correctAnswer,
    score: question.score,
    analysis: question.analysis || '',
    sortOrder: question.sortOrder ?? 1,
    options: question.options.map((option) => ({
      optionKey: option.optionKey,
      content: option.content
    }))
  })
  dialog.value = true
}

async function save() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  const options = form.options
    .filter((option) => option.content.trim())
    .map((option, index) => ({ optionKey: option.optionKey, content: option.content, sortOrder: index + 1 }))
  if (options.length < 2) {
    ElMessage.warning('Please provide at least two options')
    return
  }

  saving.value = true
  try {
    const payload = {
      content: form.content,
      type: form.type,
      correctAnswer: form.correctAnswer,
      score: form.score,
      analysis: form.analysis,
      sortOrder: form.sortOrder,
      options
    }
    if (editing.value) {
      await adminApi.updateQuestion(editingId.value, payload)
      ElMessage.success('Question updated')
    } else {
      await adminApi.createQuestion(quizId, payload)
      ElMessage.success('Question added')
    }
    dialog.value = false
    await load()
  } catch (error) {
    /* handled globally */
  } finally {
    saving.value = false
  }
}

async function remove(question) {
  try {
    await ElMessageBox.confirm('Delete this question?', 'Delete question', {
      confirmButtonText: 'Delete',
      cancelButtonText: 'Cancel',
      type: 'warning'
    })
    await adminApi.deleteQuestion(question.id)
    ElMessage.success('Question deleted')
    await load()
  } catch (error) {
    /* cancelled or handled globally */
  }
}

onMounted(load)
</script>

<style scoped>
.back {
  margin-bottom: 12px;
}

.list {
  display: flex;
  flex-direction: column;
  gap: 14px;
  min-height: 80px;
}

.q {
  padding: 18px;
}

.q-head {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 13px;
}

.q-index {
  width: 27px;
  height: 27px;
  border-radius: 9px;
  background: var(--brand-soft);
  color: var(--brand-1);
  display: grid;
  place-items: center;
  font-weight: 700;
  font-size: 12px;
  flex-shrink: 0;
}

.q-content {
  flex: 1;
  font-weight: 650;
  font-size: 14.5px;
  line-height: 1.55;
}

.q-tags {
  display: flex;
  gap: 6px;
}

.options {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.opt {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  border: 1px solid var(--line);
  border-radius: 9px;
  font-size: 13.5px;
}

.opt.correct {
  border-color: #86efac;
  background: #f0fdf4;
}

.opt-key {
  width: 22px;
  height: 22px;
  border-radius: 7px;
  background: #f1f5f9;
  display: grid;
  place-items: center;
  font-size: 11px;
  font-weight: 700;
  flex-shrink: 0;
}

.opt-text {
  flex: 1;
}

.mark {
  color: #10b981;
  font-size: 16px;
}

.analysis {
  margin-top: 11px;
  line-height: 1.6;
}

.q-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 13px;
}

.opt-editor {
  display: flex;
  flex-direction: column;
  gap: 8px;
  width: 100%;
}

.opt-row {
  display: flex;
  align-items: center;
  gap: 9px;
}

.add-opt {
  margin-top: 9px;
}

.two-col {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0 18px;
}

.hint {
  margin-top: 4px;
}

.hint code {
  background: #f1f5f9;
  padding: 1px 5px;
  border-radius: 5px;
}

@media (max-width: 700px) {
  .two-col {
    grid-template-columns: 1fr;
  }
}
</style>
