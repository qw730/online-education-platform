<template>
  <div v-loading="loading" class="page">
    <template v-if="quiz">
      <div class="quiz-head card card-pad">
        <div>
          <h1 class="page-title">{{ quiz.quiz.title }}</h1>
          <p class="page-subtitle" style="margin: 0">{{ quiz.quiz.courseTitle }}</p>
        </div>
        <div class="quiz-head-side">
          <div class="boxes">
            <div class="box">
              <span class="mono big">{{ answeredCount }}</span>
              <span class="muted tiny">of {{ questions.length }} answered</span>
            </div>
            <div class="box">
              <span class="mono big">{{ timerLabel }}</span>
              <span class="muted tiny">{{ quiz.quiz.durationMinutes ? 'time limit' : 'elapsed' }}</span>
            </div>
            <div class="box">
              <span class="mono big">{{ totalScore }}</span>
              <span class="muted tiny">total points</span>
            </div>
          </div>
          <el-button type="primary" size="large" :loading="submitting" @click="confirmSubmit">
            Submit answers
          </el-button>
        </div>
      </div>

      <el-alert v-if="quiz.quiz.description" :title="quiz.quiz.description" type="info"
                :closable="false" show-icon class="desc-alert" />

      <div v-if="questions.length" class="questions">
        <div v-for="(question, index) in questions" :key="question.id" class="card q">
          <div class="q-head">
            <span class="q-index">{{ index + 1 }}</span>
            <div class="q-content">{{ question.content }}</div>
            <div class="q-tags">
              <el-tag size="small" type="info" effect="plain" round>
                {{ questionTypeLabel(question.type) }}
              </el-tag>
              <el-tag size="small" effect="plain" round>{{ question.score }} pts</el-tag>
            </div>
          </div>

          <el-radio-group v-if="question.type !== 'MULTIPLE'" v-model="answers[question.id]" class="options">
            <el-radio v-for="option in question.options" :key="option.optionKey"
                      :value="option.optionKey" class="option">
              <span class="opt-key">{{ option.optionKey }}</span>
              <span>{{ option.content }}</span>
            </el-radio>
          </el-radio-group>

          <el-checkbox-group v-else v-model="answers[question.id]" class="options">
            <el-checkbox v-for="option in question.options" :key="option.optionKey"
                         :value="option.optionKey" class="option">
              <span class="opt-key">{{ option.optionKey }}</span>
              <span>{{ option.content }}</span>
            </el-checkbox>
          </el-checkbox-group>
        </div>

        <div class="submit-bar card card-pad">
          <div>
            <strong>{{ answeredCount }} / {{ questions.length }}</strong>
            <span class="muted"> questions answered. You can submit at any time.</span>
          </div>
          <div class="row-gap">
            <el-button @click="$router.back()">Cancel</el-button>
            <el-button type="primary" :loading="submitting" @click="confirmSubmit">Submit and see my score</el-button>
          </div>
        </div>
      </div>

      <EmptyState v-else icon="EditPen" title="This quiz has no question yet"
                  description="The administrator has not added questions to this quiz." />
    </template>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { quizApi } from '@/api'
import EmptyState from '@/components/EmptyState.vue'
import { questionTypeLabel } from '@/utils/format'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const submitting = ref(false)
const quiz = ref(null)
const answers = reactive({})
const elapsed = ref(0)
let timerId = null

const questions = computed(() => quiz.value?.questions || [])

const totalScore = computed(() =>
  questions.value.reduce((sum, question) => sum + (question.score || 0), 0)
)

const answeredCount = computed(
  () =>
    questions.value.filter((question) => {
      const value = answers[question.id]
      return Array.isArray(value) ? value.length > 0 : Boolean(value)
    }).length
)

const timerLabel = computed(() => {
  const remaining = quiz.value?.quiz?.durationMinutes
    ? Math.max(0, quiz.value.quiz.durationMinutes * 60 - elapsed.value)
    : elapsed.value
  const minutes = String(Math.floor(remaining / 60)).padStart(2, '0')
  const seconds = String(remaining % 60).padStart(2, '0')
  return `${minutes}:${seconds}`
})

async function load() {
  loading.value = true
  try {
    const data = await quizApi.detail(route.params.id)
    quiz.value = data
    data.questions.forEach((question) => {
      answers[question.id] = question.type === 'MULTIPLE' ? [] : ''
    })
    elapsed.value = 0
    if (timerId) clearInterval(timerId)
    timerId = setInterval(() => {
      elapsed.value += 1
      if (quiz.value?.quiz?.durationMinutes) {
        const limit = quiz.value.quiz.durationMinutes * 60
        if (elapsed.value >= limit) {
          clearInterval(timerId)
          ElMessage.warning('Time is up - your answers are being submitted')
          submit()
        }
      }
    }, 1000)
  } catch (error) {
    quiz.value = null
  } finally {
    loading.value = false
  }
}

function buildPayload() {
  return {
    durationSeconds: elapsed.value,
    answers: questions.value
      .filter((question) => {
        const value = answers[question.id]
        return Array.isArray(value) ? value.length > 0 : Boolean(value)
      })
      .map((question) => {
        const value = answers[question.id]
        const answer = Array.isArray(value) ? [...value].sort().join(',') : value
        return { questionId: question.id, answer }
      })
  }
}

async function confirmSubmit() {
  if (answeredCount.value < questions.value.length) {
    try {
      await ElMessageBox.confirm(
        `You have answered ${answeredCount.value} of ${questions.value.length} questions. Submit anyway?`,
        'Submit quiz',
        { confirmButtonText: 'Submit', cancelButtonText: 'Keep answering', type: 'warning' }
      )
    } catch (error) {
      return
    }
  }
  submit()
}

async function submit() {
  if (submitting.value) return
  submitting.value = true
  try {
    const result = await quizApi.submit(route.params.id, buildPayload())
    if (timerId) clearInterval(timerId)
    ElMessage.success(`You scored ${result.score} / ${result.totalScore}`)
    router.push(`/quiz/result/${result.attemptId}`)
  } catch (error) {
    /* handled globally */
  } finally {
    submitting.value = false
  }
}

onMounted(load)
onBeforeUnmount(() => {
  if (timerId) clearInterval(timerId)
})
</script>

<style scoped>
.quiz-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  flex-wrap: wrap;
}

.quiz-head-side {
  display: flex;
  align-items: center;
  gap: 20px;
  flex-wrap: wrap;
}

.boxes {
  display: flex;
  gap: 10px;
}

.box {
  display: flex;
  flex-direction: column;
  align-items: center;
  min-width: 82px;
  padding: 9px 12px;
  border-radius: 11px;
  background: #fafafa;
  border: 1px solid var(--line);
}

.big {
  font-size: 18px;
  font-weight: 750;
  color: var(--brand-1);
}

.desc-alert {
  margin-top: 18px;
}

.questions {
  display: flex;
  flex-direction: column;
  gap: 16px;
  margin-top: 18px;
}

.q {
  padding: 20px;
}

.q-head {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 14px;
}

.q-index {
  width: 28px;
  height: 28px;
  border-radius: 9px;
  background: var(--brand-soft);
  color: var(--brand-1);
  display: grid;
  place-items: center;
  font-weight: 700;
  font-size: 13px;
  flex-shrink: 0;
}

.q-content {
  flex: 1;
  font-size: 15px;
  font-weight: 650;
  line-height: 1.55;
}

.q-tags {
  display: flex;
  gap: 6px;
  flex-shrink: 0;
}

.options {
  display: flex;
  flex-direction: column;
  gap: 4px;
  width: 100%;
}

.option {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  margin: 0 !important;
  padding: 10px 13px;
  border-radius: 10px;
  border: 1px solid transparent;
  transition: background 0.15s ease, border-color 0.15s ease;
  height: auto;
}

.option:hover {
  background: #fafaff;
  border-color: #e0e7ff;
}

.opt-key {
  display: inline-grid;
  place-items: center;
  width: 22px;
  height: 22px;
  border-radius: 7px;
  background: #f1f5f9;
  font-size: 11px;
  font-weight: 700;
  color: var(--ink-700);
}

:deep(.option .el-radio__label),
:deep(.option .el-checkbox__label) {
  white-space: normal;
  line-height: 1.5;
  display: inline-flex;
  align-items: center;
  gap: 10px;
}

.submit-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
}
</style>
