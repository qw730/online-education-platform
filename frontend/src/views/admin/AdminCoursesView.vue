<template>
  <div class="page">
    <div class="section-head">
      <div>
        <h1 class="page-title">Manage courses</h1>
        <p class="page-subtitle" style="margin: 0">
          Create, edit, publish and delete courses. Drafts are only visible here.
        </p>
      </div>
      <el-button type="primary" @click="$router.push('/admin/courses/new')">
        <el-icon><Plus /></el-icon>&nbsp;New course
      </el-button>
    </div>

    <div class="filters card card-pad">
      <el-input v-model="filters.keyword" placeholder="Search by title or teacher" clearable
                class="filter-search" @keyup.enter="applyFilters">
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      <el-select v-model="filters.categoryId" placeholder="All categories" clearable class="filter-select">
        <el-option v-for="category in categories" :key="category.id" :label="category.name" :value="category.id" />
      </el-select>
      <el-select v-model="filters.level" placeholder="All levels" clearable class="filter-select">
        <el-option v-for="item in LEVEL_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
      </el-select>
      <el-button type="primary" @click="applyFilters">Search</el-button>
      <el-button @click="reset">Reset</el-button>
    </div>

    <div class="card card-pad table-card">
      <el-table v-loading="loading" :data="courses" style="width: 100%" empty-text="No course found">
        <el-table-column label="Course" min-width="260">
          <template #default="{ row }">
            <div class="cell-course">
              <div class="cell-cover" :style="{ background: coverGradient(row.categoryId) }">
                {{ initials(row.title) }}
              </div>
              <div>
                <div class="cell-title">{{ row.title }}</div>
                <div class="muted tiny">{{ row.categoryName }} · {{ row.teacherName || 'Staff' }}</div>
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="Level" width="120">
          <template #default="{ row }">
            <el-tag size="small" :type="levelTagType(row.level)" effect="light" round>
              {{ levelLabel(row.level) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="Status" width="120">
          <template #default="{ row }">
            <el-switch :model-value="row.status === 1" inline-prompt active-text="Live" inactive-text="Draft"
                       @change="toggleStatus(row)" />
          </template>
        </el-table-column>

        <el-table-column label="Lessons" width="95">
          <template #default="{ row }">
            <span class="mono">{{ row.lessonCount }}</span>
          </template>
        </el-table-column>

        <el-table-column label="Students" width="100">
          <template #default="{ row }">
            <span class="mono">{{ row.studentCount }}</span>
          </template>
        </el-table-column>

        <el-table-column label="Price" width="100">
          <template #default="{ row }">{{ formatPrice(row.price) }}</template>
        </el-table-column>

        <el-table-column label="Actions" width="280" align="right">
          <template #default="{ row }">
            <el-button size="small" @click="$router.push(`/admin/courses/${row.id}/edit`)">Edit</el-button>
            <el-button size="small" @click="$router.push(`/admin/courses/${row.id}/lessons`)">Lessons</el-button>
            <el-button size="small" @click="$router.push(`/admin/courses/${row.id}/quizzes`)">Quizzes</el-button>
            <el-button size="small" type="danger" plain @click="remove(row)">Delete</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="total > 0" class="pager">
        <el-pagination background layout="prev, pager, next, total" :total="total"
                       :current-page="page" :page-size="size" @current-change="onPageChange" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminApi, courseApi } from '@/api'
import {
  coverGradient,
  formatPrice,
  initials,
  LEVEL_OPTIONS,
  levelLabel,
  levelTagType
} from '@/utils/format'

const loading = ref(false)
const courses = ref([])
const categories = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)

const filters = reactive({ keyword: '', categoryId: null, level: null })

async function load() {
  loading.value = true
  try {
    const data = await adminApi.courses({
      keyword: filters.keyword || undefined,
      categoryId: filters.categoryId || undefined,
      level: filters.level || undefined,
      page: page.value,
      size: size.value
    })
    courses.value = data.records || []
    total.value = data.total || 0
  } catch (error) {
    courses.value = []
  } finally {
    loading.value = false
  }
}

function applyFilters() {
  page.value = 1
  load()
}

function reset() {
  filters.keyword = ''
  filters.categoryId = null
  filters.level = null
  applyFilters()
}

function onPageChange(next) {
  page.value = next
  load()
}

async function toggleStatus(row) {
  try {
    await adminApi.updateCourse(row.id, {
      title: row.title,
      subtitle: row.subtitle,
      description: row.description,
      coverUrl: row.coverUrl,
      categoryId: row.categoryId,
      teacherName: row.teacherName,
      level: row.level,
      price: row.price,
      durationHours: row.durationHours,
      tags: row.tags,
      status: row.status === 1 ? 0 : 1
    })
    ElMessage.success(row.status === 1 ? 'Course moved back to draft' : 'Course published')
    await load()
  } catch (error) {
    /* handled globally */
  }
}

async function remove(row) {
  try {
    await ElMessageBox.confirm(
      `Deleting "${row.title}" also removes its lessons, quizzes and every enrolment record. This cannot be undone.`,
      'Delete course',
      { confirmButtonText: 'Delete permanently', cancelButtonText: 'Cancel', type: 'warning' }
    )
    await adminApi.deleteCourse(row.id)
    ElMessage.success('Course deleted')
    await load()
  } catch (error) {
    /* cancelled or handled globally */
  }
}

onMounted(async () => {
  load()
  try {
    categories.value = await courseApi.categories()
  } catch (error) {
    categories.value = []
  }
})
</script>

<style scoped>
.filters {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  align-items: center;
  margin-bottom: 20px;
}

.filter-search {
  flex: 1;
  min-width: 220px;
}

.filter-select {
  width: 178px;
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

.pager {
  display: flex;
  justify-content: center;
  padding: 16px 0 8px;
}
</style>
