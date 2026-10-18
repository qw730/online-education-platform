<template>
  <div class="page">
    <el-button link class="back" @click="$router.push('/admin/courses')">
      <el-icon><ArrowLeft /></el-icon> Back to courses
    </el-button>

    <div class="section-head">
      <div>
        <h1 class="page-title">Quizzes</h1>
        <p class="page-subtitle" style="margin: 0">{{ courseTitle || 'Loading course ...' }}</p>
      </div>
      <el-button type="primary" @click="openCreate">
        <el-icon><Plus /></el-icon>&nbsp;New quiz
      </el-button>
    </div>

    <div v-loading="loading" class="list">
      <div v-for="quiz in quizzes" :key="quiz.id" class="card quiz">
        <div class="quiz-main">
          <div class="row-gap" style="gap: 8px">
            <el-tag size="small" :type="quiz.status === 1 ? 'success' : 'info'" effect="light" round>
              {{ quiz.status === 1 ? 'Published' : 'Draft' }}
            </el-tag>
            <span class="muted tiny">{{ quiz.questionCount }} questions · {{ quiz.totalScore }} points</span>
          </div>
          <div class="quiz-title">{{ quiz.title }}</div>
          <div class="muted tiny">{{ quiz.description }}</div>
          <div class="muted tiny">
            Pass mark {{ quiz.passScore }}% ·
            {{ quiz.maxAttempts > 0 ? `${quiz.maxAttempts} attempts allowed` : 'unlimited attempts' }} ·
            {{ quiz.durationMinutes > 0 ? `${quiz.durationMinutes} min limit` : 'no time limit' }} ·
            {{ quiz.attemptCount }} submissions
          </div>
        </div>
        <div class="quiz-actions">
          <el-button size="small" @click="$router.push(`/admin/quizzes/${quiz.id}/questions`)">
            Questions
          </el-button>
          <el-button size="small" @click="openEdit(quiz)">Edit</el-button>
          <el-button size="small" type="danger" plain @click="remove(quiz)">Delete</el-button>
        </div>
      </div>
    </div>

    <EmptyState v-if="!loading && !quizzes.length" icon="Notebook"
                title="No quiz for this course yet"
                description="Create a quiz, then add single choice, multiple choice or true/false questions.">
      <template #action>
        <el-button type="primary" @click="openCreate">Create the first quiz</el-button>
      </template>
    </EmptyState>

    <el-dialog v-model="dialog" :title="editing ? 'Edit quiz' : 'New quiz'" width="560px">
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="Title" prop="title">
          <el-input v-model="form.title" placeholder="e.g. Java Basics Check-up" />
        </el-form-item>
        <el-form-item label="Description">
          <el-input v-model="form.description" type="textarea" :rows="2" />
        </el-form-item>
        <div class="two-col">
          <el-form-item label="Time limit (minutes, 0 = none)">
            <el-input-number v-model="form.durationMinutes" :min="0" :max="240" :step="5"
                             controls-position="right" style="width: 100%" />
          </el-form-item>
          <el-form-item label="Pass mark (%)">
            <el-input-number v-model="form.passScore" :min="0" :max="100" :step="5"
                             controls-position="right" style="width: 100%" />
          </el-form-item>
          <el-form-item label="Max attempts (0 = unlimited)">
            <el-input-number v-model="form.maxAttempts" :min="0" :max="50"
                             controls-position="right" style="width: 100%" />
          </el-form-item>
          <el-form-item label="Status">
            <el-select v-model="form.status" style="width: 100%">
              <el-option label="Published" :value="1" />
              <el-option label="Draft" :value="0" />
            </el-select>
          </el-form-item>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">Cancel</el-button>
        <el-button type="primary" :loading="saving" @click="save">
          {{ editing ? 'Save changes' : 'Create quiz' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminApi } from '@/api'
import EmptyState from '@/components/EmptyState.vue'

const route = useRoute()
const router = useRouter()
const courseId = route.params.id

const loading = ref(false)
const saving = ref(false)
const quizzes = ref([])
const courseTitle = ref('')

const dialog = ref(false)
const editing = ref(false)
const editingId = ref(null)
const formRef = ref()

const form = reactive({
  courseId: Number(courseId),
  title: '',
  description: '',
  durationMinutes: 15,
  passScore: 60,
  maxAttempts: 0,
  status: 1
})

const rules = {
  title: [{ required: true, message: 'Please enter a quiz title', trigger: 'blur' }]
}

async function load() {
  loading.value = true
  try {
    quizzes.value = await adminApi.quizzes(courseId)
    if (!courseTitle.value) {
      const detail = await adminApi.courseDetail(courseId)
      courseTitle.value = detail.course.title
    }
  } catch (error) {
    quizzes.value = []
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editing.value = false
  editingId.value = null
  Object.assign(form, {
    courseId: Number(courseId),
    title: '',
    description: '',
    durationMinutes: 15,
    passScore: 60,
    maxAttempts: 0,
    status: 1
  })
  dialog.value = true
}

function openEdit(quiz) {
  editing.value = true
  editingId.value = quiz.id
  Object.assign(form, {
    courseId: Number(courseId),
    title: quiz.title,
    description: quiz.description || '',
    durationMinutes: quiz.durationMinutes ?? 0,
    passScore: quiz.passScore ?? 60,
    maxAttempts: quiz.maxAttempts ?? 0,
    status: quiz.status ?? 1
  })
  dialog.value = true
}

async function save() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    if (editing.value) {
      await adminApi.updateQuiz(editingId.value, { ...form })
      ElMessage.success('Quiz updated')
      dialog.value = false
      await load()
    } else {
      const created = await adminApi.createQuiz({ ...form })
      ElMessage.success('Quiz created, now add its questions')
      dialog.value = false
      await load()
      openQuestions(created.id)
    }
  } catch (error) {
    /* handled globally */
  } finally {
    saving.value = false
  }
}

function openQuestions(quizId) {
  router.push(`/admin/quizzes/${quizId}/questions`)
}

async function remove(quiz) {
  try {
    await ElMessageBox.confirm(
      `Delete "${quiz.title}" together with its questions and every submission?`,
      'Delete quiz',
      { confirmButtonText: 'Delete', cancelButtonText: 'Cancel', type: 'warning' }
    )
    await adminApi.deleteQuiz(quiz.id)
    ElMessage.success('Quiz deleted')
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

.quiz {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  padding: 18px;
  flex-wrap: wrap;
}

.quiz-main {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 260px;
  flex: 1;
}

.quiz-title {
  font-size: 16px;
  font-weight: 700;
}

.quiz-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.two-col {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0 18px;
}

@media (max-width: 700px) {
  .two-col {
    grid-template-columns: 1fr;
  }
}
</style>
