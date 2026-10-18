<template>
  <div v-loading="loading" class="page">
    <template v-if="result">
      <section class="head card card-pad">
        <el-progress type="dashboard" :percentage="percent" :width="150" :stroke-width="12"
                     :color="result.passed ? '#10b981' : '#ef4444'">
          <template #default>
            <div class="dial-score">{{ result.score }}</div>
            <div class="muted tiny">of {{ result.totalScore }} points</div>
          </template>
        </el-progress>

        <div class="head-body">
          <el-tag :type="result.passed ? 'success' : 'danger'" effect="dark" round size="large">
            {{ result.passed ? 'Passed' : 'Not passed' }}
          </el-tag>
          <h1 class="page-title">{{ result.quizTitle }}</h1>
          <p class="page-subtitle" style="margin: 0">{{ result.courseTitle }}</p>

          <div class="facts">
            <div><strong>{{ result.correctCount }}/{{ result.totalCount }}</strong><span class="muted tiny">correct answers</span></div>
            <div><strong>{{ percent }}%</strong><span class="muted tiny">pass mark {{ result.passScore }}%</span></div>
            <div><strong>{{ result.durationSeconds }}s</strong><span class="muted tiny">time spent</span></div>
            <div><strong>{{ formatDateTime(result.submittedAt) }}</strong><span class="muted tiny">submitted</span></div>
          </div>
        </div>

        <div class="head-actions">
          <el-button type="primary" @click="$router.push(`/quiz/${result.quizId}`)">Try again</el-button>
          <el-button @click="$router.push('/my/courses')">Back to my learning</el-button>
        </div>
      </section>

      <h2 class="section-title" style="margin-top: 34px">Answer review</h2>
      <div class="reviews">
        <div v-for="(item, index) in result.details" :key="item.questionId" class="card review"
             :class="item.correct ? 'ok' : 'ko'">
          <div class="review-head">
            <span class="review-index">{{ index + 1 }}</span>
            <div class="review-content">{{ item.content }}</div>
            <el-tag size="small" :type="item.correct ? 'success' : 'danger'" effect="light" round>
              {{ item.correct ? `+${item.gainedScore}` : '0' }} / {{ item.score }}
            </el-tag>
          </div>

          <div class="options">
            <div v-for="option in item.options" :key="option.optionKey" class="opt" :class="{
              correct: isCorrectOption(item, option.optionKey),
              chosen: isChosen(item, option.optionKey),
              wrong: isChosen(item, option.optionKey) && !isCorrectOption(item, option.optionKey)
            }">
              <span class="opt-key">{{ option.optionKey }}</span>
              <span class="opt-text">{{ option.content }}</span>
              <el-icon v-if="isCorrectOption(item, option.optionKey)" class="mark ok"><CircleCheckFilled /></el-icon>
              <el-icon v-else-if="isChosen(item, option.optionKey)" class="mark ko"><CircleCloseFilled /></el-icon>
            </div>
          </div>

          <div class="answer-line">
            <span class="muted tiny">Your answer:</span>
            <strong :class="item.correct ? 'text-ok' : 'text-ko'">
              {{ formatAnswer(item.yourAnswer) || 'Not answered' }}
            </strong>
            <span class="muted tiny">· Correct answer:</span>
            <strong class="text-ok">{{ formatAnswer(item.correctAnswer) }}</strong>
          </div>

          <el-alert v-if="item.analysis" :title="item.analysis" type="info" :closable="false"
                    class="analysis" show-icon />
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { quizApi } from '@/api'
import { formatDateTime } from '@/utils/format'

const route = useRoute()

const loading = ref(false)
const result = ref(null)

const percent = computed(() => {
  if (!result.value?.totalScore) return 0
  return Math.round((result.value.score * 100) / result.value.totalScore)
})

function isChosen(item, key) {
  return String(item.yourAnswer || '')
    .split(',')
    .map((part) => part.trim())
    .includes(key)
}

function isCorrectOption(item, key) {
  return String(item.correctAnswer || '')
    .split(',')
    .map((part) => part.trim())
    .includes(key)
}

function formatAnswer(value) {
  if (!value) return ''
  return String(value)
    .split(',')
    .map((part) => part.trim())
    .filter(Boolean)
    .map((part) => (part === 'T' ? 'True' : part === 'F' ? 'False' : part))
    .join(', ')
}

async function load() {
  loading.value = true
  try {
    result.value = await quizApi.attemptDetail(route.params.attemptId)
  } catch (error) {
    result.value = null
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.head {
  display: flex;
  align-items: center;
  gap: 30px;
  flex-wrap: wrap;
}

.dial-score {
  font-size: 30px;
  font-weight: 800;
  letter-spacing: -0.02em;
}

.head-body {
  flex: 1;
  min-width: 260px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.facts {
  display: flex;
  flex-wrap: wrap;
  gap: 24px;
  margin-top: 8px;
}

.facts > div {
  display: flex;
  flex-direction: column;
}

.facts strong {
  font-size: 16px;
}

.head-actions {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.reviews {
  display: flex;
  flex-direction: column;
  gap: 16px;
  margin-top: 16px;
}

.review {
  padding: 18px;
  border-left: 4px solid var(--line);
}

.review.ok {
  border-left-color: #10b981;
}

.review.ko {
  border-left-color: #ef4444;
}

.review-head {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 14px;
}

.review-index {
  width: 26px;
  height: 26px;
  border-radius: 8px;
  background: #f1f5f9;
  display: grid;
  place-items: center;
  font-size: 12px;
  font-weight: 700;
  flex-shrink: 0;
}

.review-content {
  flex: 1;
  font-weight: 650;
  line-height: 1.55;
}

.options {
  display: flex;
  flex-direction: column;
  gap: 7px;
}

.opt {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 9px 12px;
  border-radius: 9px;
  border: 1px solid var(--line);
  background: #fff;
  font-size: 13.5px;
}

.opt.correct {
  border-color: #86efac;
  background: #f0fdf4;
}

.opt.wrong {
  border-color: #fca5a5;
  background: #fef2f2;
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
  font-size: 16px;
}

.mark.ok {
  color: #10b981;
}

.mark.ko {
  color: #ef4444;
}

.answer-line {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 7px;
  margin-top: 13px;
  font-size: 13px;
}

.text-ok {
  color: #047857;
}

.text-ko {
  color: #b91c1c;
}

.analysis {
  margin-top: 12px;
}
</style>
