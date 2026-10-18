<template>
  <div v-loading="loading" class="page">
    <template v-if="detail">
      <el-button link class="back" @click="$router.back()">
        <el-icon><ArrowLeft /></el-icon> Back
      </el-button>

      <section class="banner card">
        <div class="banner-cover" :style="{ background: coverGradient(detail.course.categoryId) }">
          {{ initials(detail.course.title) }}
        </div>
        <div class="banner-body">
          <div class="row-gap">
            <el-tag effect="plain" round>{{ detail.course.categoryName }}</el-tag>
            <el-tag :type="levelTagType(detail.course.level)" effect="light" round>
              {{ levelLabel(detail.course.level) }}
            </el-tag>
            <el-tag v-if="detail.course.status === 0" type="info" effect="dark" round>Draft</el-tag>
          </div>

          <h1>{{ detail.course.title }}</h1>
          <p class="muted subtitle">{{ detail.course.subtitle }}</p>

          <div class="meta">
            <span><el-icon><User /></el-icon> {{ detail.course.teacherName || 'Staff' }}</span>
            <span><el-icon><Reading /></el-icon> {{ detail.totalLessons }} lessons</span>
            <span><el-icon><Timer /></el-icon> {{ detail.course.durationHours }} hours</span>
            <span><el-icon><UserFilled /></el-icon> {{ detail.course.studentCount }} enrolled</span>
            <span><el-icon><Star /></el-icon> {{ Number(detail.course.rating ?? 0).toFixed(1) }}</span>
          </div>

          <div class="tags">
            <el-tag v-for="tag in splitTags(detail.course.tags)" :key="tag" size="small" type="info"
                    effect="plain" round>#{{ tag }}</el-tag>
          </div>
        </div>

        <div class="banner-side">
          <div class="price">{{ formatPrice(detail.course.price) }}</div>

          <template v-if="!detail.enrolled">
            <el-button type="primary" size="large" class="enroll" :loading="enrolling" @click="enroll">
              Enrol in this course
            </el-button>
            <div class="muted tiny center">Free to learn, progress is saved automatically.</div>
          </template>

          <template v-else>
            <div class="progress-box">
              <div class="row-gap">
                <span class="muted tiny">Your progress</span>
                <span class="spacer" />
                <strong>{{ detail.progressPercent ?? 0 }}%</strong>
              </div>
              <el-progress :percentage="detail.progressPercent ?? 0" :stroke-width="9"
                           :color="progressColor(detail.progressPercent)" :show-text="false" />
              <div class="muted tiny">
                {{ detail.completedLessons ?? 0 }} of {{ detail.totalLessons }} lessons finished
              </div>
            </div>
            <el-button type="primary" size="large" class="enroll"
                       @click="$router.push(`/my/courses/${detail.course.id}`)">
              Go to my learning
            </el-button>
            <el-button link type="danger" @click="dropCourse">Drop this course</el-button>
          </template>
        </div>
      </section>

      <section class="columns">
        <div class="main-col">
          <div class="card card-pad">
            <h2 class="section-title">About this course</h2>
            <p class="desc">{{ detail.course.description || 'No description provided yet.' }}</p>
          </div>

          <div class="card card-pad">
            <div class="section-head">
              <h2 class="section-title">Curriculum</h2>
              <span class="muted tiny">{{ detail.lessons.length }} chapters</span>
            </div>
            <el-collapse v-if="detail.lessons.length" accordion>
              <el-collapse-item v-for="(lesson, index) in detail.lessons" :key="lesson.id" :name="lesson.id">
                <template #title>
                  <div class="lesson-head">
                    <span class="lesson-index">{{ String(index + 1).padStart(2, '0') }}</span>
                    <span class="lesson-title">{{ lesson.title }}</span>
                    <span class="spacer" />
                    <span class="muted tiny">{{ formatMinutes(lesson.durationMinutes) }}</span>
                  </div>
                </template>
                <div class="lesson-summary muted">{{ lesson.summary }}</div>
                <p v-if="lesson.content" class="lesson-content">{{ lesson.content }}</p>
              </el-collapse-item>
            </el-collapse>
            <EmptyState v-else title="No chapter published yet"
                        description="The teacher has not added any chapter to this course." />
          </div>
        </div>

        <aside class="side-col">
          <div class="card card-pad">
            <div class="section-head">
              <h2 class="section-title">Quizzes</h2>
              <span class="muted tiny">{{ detail.quizzes.length }}</span>
            </div>
            <div v-if="detail.quizzes.length" class="quiz-list">
              <div v-for="quiz in detail.quizzes" :key="quiz.id" class="quiz-item">
                <div class="quiz-title">{{ quiz.title }}</div>
                <div class="muted tiny">{{ quiz.description }}</div>
                <div class="muted tiny">
                  {{ quiz.questionCount }} questions · pass at {{ quiz.passScore }}%
                </div>
                <div class="quiz-foot">
                  <el-tag v-if="quiz.bestScore !== null" size="small"
                          :type="quiz.passed ? 'success' : 'warning'" effect="light" round>
                    Best {{ quiz.bestScore }}/{{ quiz.totalScore }}
                  </el-tag>
                  <span v-else class="muted tiny">Not attempted</span>
                  <el-button size="small" type="primary" plain :disabled="!detail.enrolled"
                             @click="$router.push(`/quiz/${quiz.id}`)">
                    {{ detail.enrolled ? 'Start' : 'Enrol first' }}
                  </el-button>
                </div>
              </div>
            </div>
            <EmptyState v-else icon="EditPen" title="No quiz yet"
                        description="This course does not have a quiz at the moment." />
          </div>

          <div class="card card-pad">
            <h2 class="section-title">What you will get</h2>
            <ul class="perks">
              <li><el-icon><Check /></el-icon> {{ detail.totalLessons }} structured chapters</li>
              <li><el-icon><Check /></el-icon> Progress saved per chapter</li>
              <li><el-icon><Check /></el-icon> Auto-graded quizzes with explanations</li>
              <li><el-icon><Check /></el-icon> Personal study plan generator</li>
            </ul>
          </div>
        </aside>
      </section>
    </template>
  </div>
</template>

<script setup>
import { onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { courseApi, learningApi } from '@/api'
import EmptyState from '@/components/EmptyState.vue'
import {
  coverGradient,
  formatMinutes,
  formatPrice,
  initials,
  levelLabel,
  levelTagType,
  progressColor,
  splitTags
} from '@/utils/format'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const enrolling = ref(false)
const detail = ref(null)

async function load() {
  loading.value = true
  try {
    detail.value = await courseApi.detail(route.params.id)
  } catch (error) {
    detail.value = null
  } finally {
    loading.value = false
  }
}

async function enroll() {
  enrolling.value = true
  try {
    await learningApi.enroll(detail.value.course.id)
    ElMessage.success('You are enrolled. Happy learning!')
    await load()
  } catch (error) {
    /* handled globally */
  } finally {
    enrolling.value = false
  }
}

async function dropCourse() {
  try {
    await ElMessageBox.confirm(
      'Dropping the course keeps your progress records but removes it from your learning list. Continue?',
      'Drop course',
      { confirmButtonText: 'Drop', cancelButtonText: 'Keep it', type: 'warning' }
    )
    await learningApi.drop(detail.value.course.id)
    ElMessage.success('Course dropped')
    await load()
  } catch (error) {
    /* cancelled or handled globally */
  }
}

onMounted(load)
watch(() => route.params.id, load)
</script>

<style scoped>
.back {
  margin-bottom: 12px;
}

.banner {
  display: grid;
  grid-template-columns: 210px 1fr 240px;
  gap: 24px;
  padding: 22px;
  align-items: stretch;
}

.banner-cover {
  border-radius: 14px;
  min-height: 168px;
  display: grid;
  place-items: center;
  color: #fff;
  font-size: 40px;
  font-weight: 800;
  letter-spacing: 0.05em;
}

.banner-body {
  display: flex;
  flex-direction: column;
  gap: 10px;
  min-width: 0;
}

.banner-body h1 {
  margin: 0;
  font-size: 26px;
  line-height: 1.25;
  letter-spacing: -0.025em;
}

.subtitle {
  margin: 0;
  line-height: 1.6;
}

.meta {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  color: var(--ink-500);
  font-size: 13px;
}

.meta span {
  display: inline-flex;
  align-items: center;
  gap: 5px;
}

.tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.banner-side {
  border-left: 1px solid var(--line);
  padding-left: 22px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  justify-content: center;
}

.price {
  font-size: 28px;
  font-weight: 800;
  color: var(--brand-1);
  letter-spacing: -0.02em;
}

.enroll {
  width: 100%;
}

.center {
  text-align: center;
}

.progress-box {
  display: flex;
  flex-direction: column;
  gap: 7px;
}

.columns {
  display: grid;
  grid-template-columns: 1fr 330px;
  gap: 20px;
  margin-top: 20px;
  align-items: start;
}

.main-col,
.side-col {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.desc {
  margin: 10px 0 0;
  line-height: 1.75;
  color: var(--ink-700);
  white-space: pre-line;
}

.lesson-head {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  padding-right: 10px;
}

.lesson-index {
  width: 26px;
  height: 26px;
  border-radius: 8px;
  background: var(--brand-soft);
  color: var(--brand-1);
  display: grid;
  place-items: center;
  font-size: 11px;
  font-weight: 700;
  flex-shrink: 0;
}

.lesson-title {
  font-weight: 600;
  font-size: 14px;
}

.lesson-summary {
  font-size: 13px;
  margin-bottom: 8px;
}

.lesson-content {
  margin: 0;
  font-size: 13px;
  line-height: 1.75;
  color: var(--ink-700);
  white-space: pre-line;
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
  margin-top: 6px;
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
  .banner {
    grid-template-columns: 1fr;
  }

  .banner-side {
    border-left: none;
    border-top: 1px solid var(--line);
    padding-left: 0;
    padding-top: 18px;
  }

  .columns {
    grid-template-columns: 1fr;
  }
}
</style>
