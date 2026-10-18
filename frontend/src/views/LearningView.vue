<template>
  <div v-loading="loading" class="page">
    <el-button link class="back" @click="$router.push('/my/courses')">
      <el-icon><ArrowLeft /></el-icon> Back to my learning
    </el-button>

    <template v-if="progress">
      <section class="head card card-pad">
        <div>
          <h1 class="page-title">{{ progress.courseTitle }}</h1>
          <p class="page-subtitle" style="margin-bottom: 0">
            Tick each chapter as you finish it - your progress bar updates immediately.
          </p>
        </div>
        <div class="head-stats">
          <el-progress type="dashboard" :percentage="progress.progressPercent" :width="118"
                       :stroke-width="9" :color="progressColor(progress.progressPercent)">
            <template #default="{ percentage }">
              <div class="dial-value">{{ percentage }}%</div>
              <div class="muted tiny">completed</div>
            </template>
          </el-progress>
          <div class="head-numbers">
            <div><strong>{{ progress.completedLessons }}</strong><span class="muted tiny">of {{ progress.totalLessons }} lessons</span></div>
            <div><strong>{{ formatMinutes(progress.studiedMinutes) }}</strong><span class="muted tiny">studied</span></div>
            <div><strong>{{ formatMinutes(progress.totalMinutes) }}</strong><span class="muted tiny">total length</span></div>
          </div>
        </div>
      </section>

      <section class="columns">
        <div class="card card-pad">
          <div class="section-head">
            <h2 class="section-title">Chapters</h2>
            <el-button size="small" type="primary" plain :loading="saving" @click="openPlanDialog">
              <el-icon><Calendar /></el-icon>&nbsp;Add to study plan
            </el-button>
          </div>

          <div v-if="progress.lessons.length" class="lesson-list">
            <div v-for="(lesson, index) in progress.lessons" :key="lesson.id" class="lesson"
                 :class="{ done: lesson.completed }">
              <el-checkbox :model-value="lesson.completed" :disabled="saving"
                           @change="(value) => toggle(lesson, value)" />
              <div class="lesson-main">
                <div class="lesson-title">
                  <span class="muted tiny">{{ String(index + 1).padStart(2, '0') }}</span>
                  {{ lesson.title }}
                </div>
                <div class="muted tiny">{{ lesson.summary }}</div>
              </div>
              <div class="lesson-side">
                <span class="muted tiny">{{ formatMinutes(lesson.durationMinutes) }}</span>
                <el-tag v-if="lesson.completed" size="small" type="success" effect="light" round>Done</el-tag>
              </div>
            </div>
          </div>

          <EmptyState v-else title="This course has no chapter yet"
                      description="Ask the administrator to publish the chapters." />
        </div>

        <aside class="side">
          <div class="card card-pad">
            <div class="section-head">
              <h2 class="section-title">Quizzes</h2>
            </div>
            <div v-if="quizzes.length" class="quiz-list">
              <div v-for="quiz in quizzes" :key="quiz.id" class="quiz-item">
                <div class="quiz-title">{{ quiz.title }}</div>
                <div class="muted tiny">{{ quiz.questionCount }} questions · {{ quiz.durationMinutes }} min</div>
                <div class="quiz-foot">
                  <el-tag v-if="quiz.bestScore !== null" size="small"
                          :type="quiz.passed ? 'success' : 'warning'" effect="light" round>
                    Best {{ quiz.bestScore }}/{{ quiz.totalScore }}
                  </el-tag>
                  <span v-else class="muted tiny">Not attempted</span>
                  <el-button size="small" type="primary" @click="$router.push(`/quiz/${quiz.id}`)">
                    Take quiz
                  </el-button>
                </div>
              </div>
            </div>
            <EmptyState v-else icon="EditPen" title="No quiz available"
                        description="This course does not have a published quiz." />
          </div>

          <div class="card card-pad tips">
            <h2 class="section-title">Study tips</h2>
            <ul class="perks">
              <li><el-icon><Check /></el-icon> Study in short, regular sessions</li>
              <li><el-icon><Check /></el-icon> Mark a chapter done only after you practised it</li>
              <li><el-icon><Check /></el-icon> Retake the quiz until you pass comfortably</li>
            </ul>
          </div>
        </aside>
      </section>
    </template>

    <el-dialog v-model="planDialog" title="Create a study plan" width="520px">
      <el-form :model="planForm" label-position="top">
        <el-form-item label="Plan title">
          <el-input v-model="planForm.title" placeholder="e.g. Finish this course in three weeks" />
        </el-form-item>
        <el-form-item label="Goal">
          <el-input v-model="planForm.goal" type="textarea" :rows="2"
                    placeholder="What do you want to achieve?" />
        </el-form-item>
        <el-form-item label="Period">
          <el-date-picker v-model="planForm.range" type="daterange" value-format="YYYY-MM-DD"
                          start-placeholder="Start" end-placeholder="End" style="width: 100%" />
        </el-form-item>
        <el-form-item label="Daily study time (minutes)">
          <el-input-number v-model="planForm.dailyMinutes" :min="10" :max="480" :step="10" />
        </el-form-item>
        <el-form-item>
          <el-checkbox v-model="planForm.autoGenerateItems">
            Generate one task per chapter automatically
          </el-checkbox>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="planDialog = false">Cancel</el-button>
        <el-button type="primary" :loading="saving" @click="createPlan">Create plan</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { learningApi, planApi, quizApi } from '@/api'
import EmptyState from '@/components/EmptyState.vue'
import { formatMinutes, progressColor } from '@/utils/format'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const saving = ref(false)
const progress = ref(null)
const quizzes = ref([])

const planDialog = ref(false)
const planForm = reactive({
  title: '',
  goal: '',
  range: [],
  dailyMinutes: 30,
  autoGenerateItems: true
})

async function load() {
  loading.value = true
  try {
    progress.value = await learningApi.courseProgress(route.params.id)
    quizzes.value = await quizApi.byCourse(route.params.id)
  } catch (error) {
    progress.value = null
  } finally {
    loading.value = false
  }
}

async function toggle(lesson, value) {
  saving.value = true
  try {
    progress.value = await learningApi.toggleLesson(lesson.id, {
      completed: value,
      watchedMinutes: value ? lesson.durationMinutes : 0
    })
    ElMessage.success(value ? `"${lesson.title}" marked as finished` : `"${lesson.title}" reopened`)
    quizzes.value = await quizApi.byCourse(route.params.id)
  } catch (error) {
    /* handled globally */
  } finally {
    saving.value = false
  }
}

function openPlanDialog() {
  const today = new Date()
  const end = new Date()
  end.setDate(end.getDate() + 21)
  planForm.title = `${progress.value.courseTitle} study plan`
  planForm.goal = 'Finish all chapters and pass the quiz.'
  planForm.range = [today.toISOString().slice(0, 10), end.toISOString().slice(0, 10)]
  planForm.dailyMinutes = 30
  planForm.autoGenerateItems = true
  planDialog.value = true
}

async function createPlan() {
  if (!planForm.title || planForm.range.length !== 2) {
    ElMessage.warning('Please provide a title and a date range')
    return
  }
  saving.value = true
  try {
    await planApi.create({
      courseId: Number(route.params.id),
      title: planForm.title,
      goal: planForm.goal,
      startDate: planForm.range[0],
      endDate: planForm.range[1],
      dailyMinutes: planForm.dailyMinutes,
      autoGenerateItems: planForm.autoGenerateItems
    })
    ElMessage.success('Study plan created')
    planDialog.value = false
    router.push('/my/plans')
  } catch (error) {
    /* handled globally */
  } finally {
    saving.value = false
  }
}

onMounted(load)
watch(() => route.params.id, load)
</script>

<style scoped>
.back {
  margin-bottom: 12px;
}

.head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 26px;
  flex-wrap: wrap;
}

.head-stats {
  display: flex;
  align-items: center;
  gap: 26px;
}

.dial-value {
  font-size: 20px;
  font-weight: 750;
}

.head-numbers {
  display: flex;
  flex-direction: column;
  gap: 9px;
}

.head-numbers > div {
  display: flex;
  align-items: baseline;
  gap: 7px;
}

.head-numbers strong {
  font-size: 16px;
}

.columns {
  display: grid;
  grid-template-columns: 1fr 320px;
  gap: 20px;
  margin-top: 20px;
  align-items: start;
}

.side {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.lesson-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 6px;
}

.lesson {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 13px 15px;
  border: 1px solid var(--line);
  border-radius: 12px;
  transition: background 0.15s ease, border-color 0.15s ease;
}

.lesson:hover {
  border-color: #c7d2fe;
  background: #fafaff;
}

.lesson.done {
  background: #f0fdf4;
  border-color: #bbf7d0;
}

.lesson-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.lesson-title {
  font-weight: 600;
  font-size: 14px;
  display: flex;
  gap: 8px;
  align-items: center;
}

.lesson-side {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
}

.quiz-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 6px;
}

.quiz-item {
  border: 1px solid var(--line);
  border-radius: 12px;
  padding: 13px;
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.quiz-title {
  font-weight: 650;
  font-size: 13.5px;
}

.quiz-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 5px;
}

.perks {
  list-style: none;
  padding: 0;
  margin: 12px 0 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
  font-size: 13px;
  color: var(--ink-700);
}

.perks li {
  display: flex;
  align-items: center;
  gap: 8px;
}

.perks .el-icon {
  color: #10b981;
}

@media (max-width: 1000px) {
  .columns {
    grid-template-columns: 1fr;
  }
}
</style>
