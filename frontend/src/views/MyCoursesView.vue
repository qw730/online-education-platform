<template>
  <div class="page">
    <h1 class="page-title">My learning</h1>
    <p class="page-subtitle">Every course you are enrolled in, with live progress.</p>

    <div class="tabs card card-pad">
      <el-radio-group v-model="statusFilter" @change="applyFilter">
        <el-radio-button value="ALL">All</el-radio-button>
        <el-radio-button value="ENROLLED">In progress</el-radio-button>
        <el-radio-button value="COMPLETED">Completed</el-radio-button>
        <el-radio-button value="DROPPED">Dropped</el-radio-button>
      </el-radio-group>
      <span class="spacer" />
      <el-input v-model="keyword" placeholder="Filter by title" clearable style="width: 220px" />
    </div>

    <div v-loading="loading" class="grid">
      <div v-for="item in filtered" :key="item.id" class="enrol card">
        <div class="enrol-cover" :style="{ background: coverGradient(item.courseId) }">
          {{ initials(item.courseTitle) }}
        </div>
        <div class="enrol-body">
          <div class="row-gap" style="gap: 6px">
            <el-tag size="small" effect="plain" round>{{ item.categoryName || 'General' }}</el-tag>
            <el-tag size="small" :type="enrollmentStatusType(item.status)" effect="light" round>
              {{ enrollmentStatusLabel(item.status) }}
            </el-tag>
          </div>
          <div class="enrol-title">{{ item.courseTitle }}</div>
          <div class="muted tiny">{{ item.teacherName || 'Staff' }}</div>

          <el-progress :percentage="item.progressPercent" :stroke-width="8"
                       :color="progressColor(item.progressPercent)" />
          <div class="muted tiny">
            {{ item.completedLessons }} / {{ item.totalLessons }} lessons ·
            enrolled {{ formatDate(item.enrolledAt) }}
          </div>

          <div class="enrol-foot">
            <el-button size="small" @click="$router.push(`/courses/${item.courseId}`)">Details</el-button>
            <el-button size="small" type="primary" :disabled="item.status === 'DROPPED'"
                       @click="$router.push(`/my/courses/${item.courseId}`)">
              Continue
            </el-button>
          </div>
        </div>
      </div>
    </div>

    <EmptyState v-if="!loading && !filtered.length" icon="Collection"
                title="No course in this list"
                description="Head over to the catalogue and enrol in your first course.">
      <template #action>
        <el-button type="primary" @click="$router.push('/courses')">Browse courses</el-button>
      </template>
    </EmptyState>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { learningApi } from '@/api'
import EmptyState from '@/components/EmptyState.vue'
import {
  coverGradient,
  enrollmentStatusLabel,
  enrollmentStatusType,
  formatDate,
  initials,
  progressColor
} from '@/utils/format'

const loading = ref(false)
const enrollments = ref([])
const statusFilter = ref('ALL')
const keyword = ref('')

const filtered = computed(() =>
  enrollments.value.filter((item) => {
    const matchStatus = statusFilter.value === 'ALL' || item.status === statusFilter.value
    const matchKeyword =
      !keyword.value || item.courseTitle.toLowerCase().includes(keyword.value.trim().toLowerCase())
    return matchStatus && matchKeyword
  })
)

function applyFilter() {
  /* computed already handles it, kept for the v-model change event */
}

async function load() {
  loading.value = true
  try {
    const data = await learningApi.myActiveCourses()
    enrollments.value = data || []
  } catch (error) {
    enrollments.value = []
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.tabs {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 22px;
}

.grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 18px;
  min-height: 100px;
}

.enrol {
  display: flex;
  gap: 15px;
  padding: 15px;
}

.enrol-cover {
  width: 74px;
  height: 74px;
  border-radius: 13px;
  display: grid;
  place-items: center;
  color: #fff;
  font-weight: 800;
  font-size: 18px;
  flex-shrink: 0;
}

.enrol-body {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 7px;
}

.enrol-title {
  font-weight: 700;
  font-size: 15px;
  line-height: 1.35;
}

.enrol-foot {
  display: flex;
  gap: 8px;
  justify-content: flex-end;
  margin-top: 3px;
}
</style>
