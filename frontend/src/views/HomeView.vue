<template>
  <div class="page">
    <section class="hero">
      <h1>Hello {{ userStore.displayName }}, ready to learn?</h1>
      <p>
        Pick up where you left off, build a study plan and check your understanding with a
        quick quiz. Everything is tracked so you always know where you stand.
      </p>
      <div class="hero-actions">
        <el-input v-model="keyword" size="large" placeholder="Search courses, topics or teachers"
                  class="hero-search" clearable @keyup.enter="search">
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-button size="large" type="primary" class="hero-btn" @click="search">Search</el-button>
        <el-button v-if="userStore.isAdmin" size="large" plain class="hero-ghost" @click="$router.push('/admin')">
          Admin console
        </el-button>
      </div>
    </section>

    <section class="stats">
      <StatCard label="Enrolled courses" :value="stats.enrolledCourses" icon="Collection" color="#4f46e5" />
      <StatCard label="Completed courses" :value="stats.completedCourses" icon="Trophy" color="#10b981" />
      <StatCard label="Lessons finished" :value="stats.completedLessons" icon="Reading" color="#0ea5e9" />
      <StatCard label="Study minutes" :value="stats.studyMinutes" icon="Timer" color="#f59e0b" />
      <StatCard label="Active study plans" :value="stats.activePlans" icon="Calendar" color="#8b5cf6" />
      <StatCard label="Avg. quiz score" :value="`${stats.averageQuizScore ?? 0}%`" icon="TrendCharts" color="#ef4444" />
    </section>

    <section v-if="recentCourses.length" class="block">
      <div class="section-head">
        <div>
          <h2 class="section-title">Continue learning</h2>
          <div class="muted tiny">Jump straight back into your courses</div>
        </div>
        <el-button link type="primary" @click="$router.push('/my/courses')">View all</el-button>
      </div>
      <div class="grid">
        <div v-for="item in recentCourses" :key="item.id" class="resume card">
          <div class="resume-cover" :style="{ background: coverGradient(item.courseId) }">
            {{ initials(item.courseTitle) }}
          </div>
          <div class="resume-body">
            <div class="resume-title">{{ item.courseTitle }}</div>
            <div class="muted tiny">{{ item.completedLessons }} / {{ item.totalLessons }} lessons</div>
            <el-progress :percentage="item.progressPercent" :stroke-width="7"
                         :color="progressColor(item.progressPercent)" :show-text="false" />
            <div class="resume-foot">
              <el-tag size="small" :type="enrollmentStatusType(item.status)" effect="light" round>
                {{ enrollmentStatusLabel(item.status) }}
              </el-tag>
              <el-button size="small" type="primary" plain @click="$router.push(`/my/courses/${item.courseId}`)">
                Continue
              </el-button>
            </div>
          </div>
        </div>
      </div>
    </section>

    <section class="block">
      <div class="section-head">
        <div>
          <h2 class="section-title">Recommended for you</h2>
          <div class="muted tiny">{{ recommendationHint }}</div>
        </div>
        <el-button link type="primary" @click="loadRecommendations">Refresh</el-button>
      </div>
      <div v-loading="loadingRecommendations" class="grid">
        <CourseCard v-for="item in recommendations" :key="item.course.id" :course="item.course"
                    :reason="item.reason" :match-score="item.matchScore" @open="openCourse" />
      </div>
      <EmptyState v-if="!loadingRecommendations && !recommendations.length"
                  icon="MagicStick" title="No suggestions yet"
                  description="Enrol in a course and the recommender will start suggesting related material." />
    </section>

    <section class="block">
      <div class="section-head">
        <div>
          <h2 class="section-title">Browse by category</h2>
          <div class="muted tiny">Find the topic you want to master</div>
        </div>
        <el-button link type="primary" @click="$router.push('/courses')">All courses</el-button>
      </div>
      <div class="cats">
        <button v-for="category in categories" :key="category.id" class="cat"
                @click="$router.push({ path: '/courses', query: { categoryId: category.id } })">
          <strong>{{ category.name }}</strong>
          <span class="muted tiny">{{ category.courseCount }} courses</span>
        </button>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { courseApi, dashboardApi } from '@/api'
import { useUserStore } from '@/stores/user'
import StatCard from '@/components/StatCard.vue'
import CourseCard from '@/components/CourseCard.vue'
import EmptyState from '@/components/EmptyState.vue'
import {
  coverGradient,
  enrollmentStatusLabel,
  enrollmentStatusType,
  initials,
  progressColor
} from '@/utils/format'

const router = useRouter()
const userStore = useUserStore()

const keyword = ref('')
const categories = ref([])
const recommendations = ref([])
const loadingRecommendations = ref(false)
const stats = reactive({
  enrolledCourses: 0,
  completedCourses: 0,
  completedLessons: 0,
  studyMinutes: 0,
  activePlans: 0,
  averageQuizScore: 0
})
const recentCourses = ref([])

const recommendationHint = computed(() =>
  recentCourses.value.length
    ? 'Based on the courses you are already studying'
    : 'Popular courses to get you started'
)

function search() {
  router.push({ path: '/courses', query: keyword.value ? { keyword: keyword.value } : {} })
}

function openCourse(courseId) {
  router.push(`/courses/${courseId}`)
}

async function loadDashboard() {
  try {
    const data = await dashboardApi.student()
    Object.assign(stats, {
      enrolledCourses: data.enrolledCourses,
      completedCourses: data.completedCourses,
      completedLessons: data.completedLessons,
      studyMinutes: data.studyMinutes,
      activePlans: data.activePlans,
      averageQuizScore: data.averageQuizScore
    })
    recentCourses.value = data.recentCourses || []
  } catch (error) {
    /* handled globally */
  }
}

async function loadRecommendations() {
  loadingRecommendations.value = true
  try {
    recommendations.value = await dashboardApi.recommendations(6)
  } catch (error) {
    recommendations.value = []
  } finally {
    loadingRecommendations.value = false
  }
}

onMounted(async () => {
  loadDashboard()
  loadRecommendations()
  try {
    categories.value = await courseApi.categories()
  } catch (error) {
    categories.value = []
  }
})
</script>

<style scoped>
.hero-actions {
  position: relative;
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  max-width: 720px;
}

.hero-search {
  flex: 1;
  min-width: 260px;
}

.hero-btn {
  background: #fff;
  border-color: #fff;
  color: var(--brand-1);
  font-weight: 650;
}

.hero-btn:hover {
  background: #f5f3ff;
  border-color: #f5f3ff;
  color: var(--brand-1);
}

.hero-ghost {
  background: rgba(255, 255, 255, 0.14);
  border-color: rgba(255, 255, 255, 0.45);
  color: #fff;
}

.hero-ghost:hover {
  background: rgba(255, 255, 255, 0.24);
  border-color: #fff;
  color: #fff;
}

.stats {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(178px, 1fr));
  gap: 14px;
  margin: 26px 0 8px;
}

.block {
  margin-top: 40px;
}

.grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(258px, 1fr));
  gap: 18px;
}

.resume {
  display: flex;
  gap: 14px;
  padding: 14px;
  align-items: center;
}

.resume-cover {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  display: grid;
  place-items: center;
  color: #fff;
  font-weight: 800;
  font-size: 15px;
  flex-shrink: 0;
}

.resume-body {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.resume-title {
  font-weight: 650;
  font-size: 14px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.resume-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.cats {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(190px, 1fr));
  gap: 12px;
}

.cat {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 2px;
  padding: 14px 16px;
  border-radius: 12px;
  border: 1px solid var(--line);
  background: #fff;
  cursor: pointer;
  transition: border-color 0.15s ease, transform 0.15s ease;
  text-align: left;
}

.cat:hover {
  border-color: var(--brand-1);
  transform: translateY(-2px);
}

.cat strong {
  font-size: 14px;
}
</style>
