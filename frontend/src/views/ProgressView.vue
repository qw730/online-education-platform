<template>
  <div class="page">
    <h1 class="page-title">Learning progress</h1>
    <p class="page-subtitle">A single view of everything you have studied so far.</p>

    <div class="summary">
      <StatCard label="Courses" :value="rows.length" icon="Collection" color="#4f46e5" />
      <StatCard label="Average progress" :value="`${averageProgress}%`" icon="TrendCharts" color="#0ea5e9" />
      <StatCard label="Chapters finished" :value="finishedLessons" icon="Reading" color="#10b981" />
      <StatCard label="Courses completed" :value="completedCourses" icon="Trophy" color="#f59e0b" />
    </div>

    <div class="card card-pad table-card">
      <el-table v-loading="loading" :data="rows" style="width: 100%" empty-text="No enrolment yet">
        <el-table-column label="Course" min-width="240">
          <template #default="{ row }">
            <div class="cell-course">
              <div class="cell-cover" :style="{ background: coverGradient(row.courseId) }">
                {{ initials(row.courseTitle) }}
              </div>
              <div>
                <div class="cell-title">{{ row.courseTitle }}</div>
                <div class="muted tiny">{{ row.categoryName || 'General' }}</div>
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="Status" width="130">
          <template #default="{ row }">
            <el-tag size="small" :type="enrollmentStatusType(row.status)" effect="light" round>
              {{ enrollmentStatusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="Progress" min-width="200">
          <template #default="{ row }">
            <div class="progress-cell">
              <el-progress :percentage="row.progressPercent" :stroke-width="8"
                           :color="progressColor(row.progressPercent)" :show-text="false" />
              <span class="muted tiny">{{ row.progressPercent }}%</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="Chapters" width="110">
          <template #default="{ row }">
            <span class="mono">{{ row.completedLessons }}/{{ row.totalLessons }}</span>
          </template>
        </el-table-column>

        <el-table-column label="Last studied" width="130">
          <template #default="{ row }">
            <span class="muted tiny">{{ formatDate(row.lastStudyAt) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="" width="150" align="right">
          <template #default="{ row }">
            <el-button size="small" @click="openDetail(row)">Details</el-button>
            <el-button size="small" type="primary" plain
                       @click="$router.push(`/my/courses/${row.courseId}`)">Learn</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-drawer v-model="drawer" :title="active?.courseTitle || 'Progress detail'" size="480px">
      <div v-loading="detailLoading" class="drawer-body">
        <template v-if="detail">
          <div class="drawer-stats">
            <div><strong>{{ detail.progressPercent }}%</strong><span class="muted tiny">completed</span></div>
            <div><strong>{{ detail.completedLessons }}/{{ detail.totalLessons }}</strong><span class="muted tiny">chapters</span></div>
            <div><strong>{{ formatMinutes(detail.studiedMinutes) }}</strong><span class="muted tiny">studied</span></div>
          </div>

          <el-timeline>
            <el-timeline-item v-for="lesson in detail.lessons" :key="lesson.id"
                              :type="lesson.completed ? 'success' : 'info'"
                              :hollow="!lesson.completed"
                              :timestamp="formatMinutes(lesson.durationMinutes)">
              {{ lesson.title }}
            </el-timeline-item>
          </el-timeline>
        </template>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { learningApi } from '@/api'
import StatCard from '@/components/StatCard.vue'
import {
  coverGradient,
  enrollmentStatusLabel,
  enrollmentStatusType,
  formatDate,
  formatMinutes,
  initials,
  progressColor
} from '@/utils/format'

const loading = ref(false)
const rows = ref([])

const drawer = ref(false)
const detailLoading = ref(false)
const detail = ref(null)
const active = ref(null)

const averageProgress = computed(() => {
  if (!rows.value.length) return 0
  const total = rows.value.reduce((sum, row) => sum + (row.progressPercent || 0), 0)
  return Math.round(total / rows.value.length)
})

const finishedLessons = computed(() =>
  rows.value.reduce((sum, row) => sum + (row.completedLessons || 0), 0)
)

const completedCourses = computed(
  () => rows.value.filter((row) => row.status === 'COMPLETED').length
)

async function load() {
  loading.value = true
  try {
    rows.value = (await learningApi.myActiveCourses()) || []
  } catch (error) {
    rows.value = []
  } finally {
    loading.value = false
  }
}

async function openDetail(row) {
  active.value = row
  drawer.value = true
  detailLoading.value = true
  detail.value = null
  try {
    detail.value = await learningApi.courseProgress(row.courseId)
  } catch (error) {
    detail.value = null
  } finally {
    detailLoading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.summary {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(190px, 1fr));
  gap: 14px;
  margin-bottom: 22px;
}

.table-card {
  padding: 8px 8px 4px;
}

.cell-course {
  display: flex;
  align-items: center;
  gap: 11px;
}

.cell-cover {
  width: 38px;
  height: 38px;
  border-radius: 10px;
  display: grid;
  place-items: center;
  color: #fff;
  font-weight: 700;
  font-size: 12px;
  flex-shrink: 0;
}

.cell-title {
  font-weight: 600;
  font-size: 13.5px;
}

.progress-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.drawer-stats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
  margin-bottom: 22px;
}

.drawer-stats > div {
  display: flex;
  flex-direction: column;
  background: #fafafa;
  border-radius: 11px;
  padding: 12px;
}

.drawer-stats strong {
  font-size: 17px;
}
</style>
