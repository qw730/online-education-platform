<template>
  <div class="page">
    <h1 class="page-title">Course catalogue</h1>
    <p class="page-subtitle">Browse every published course on the platform.</p>

    <div class="filters card card-pad">
      <el-input v-model="filters.keyword" placeholder="Search by title, topic or teacher" clearable
                class="filter-search" @keyup.enter="applyFilters">
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>

      <el-select v-model="filters.categoryId" placeholder="All categories" clearable class="filter-select">
        <el-option v-for="category in categories" :key="category.id" :label="category.name" :value="category.id" />
      </el-select>

      <el-select v-model="filters.level" placeholder="All levels" clearable class="filter-select">
        <el-option v-for="item in LEVEL_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
      </el-select>

      <el-select v-model="filters.sortBy" placeholder="Sort by" class="filter-select">
        <el-option label="Newest first" value="newest" />
        <el-option label="Most popular" value="popular" />
        <el-option label="Highest rated" value="rating" />
        <el-option label="Price: low to high" value="priceAsc" />
        <el-option label="Price: high to low" value="priceDesc" />
      </el-select>

      <el-button type="primary" @click="applyFilters">Apply</el-button>
      <el-button @click="resetFilters">Reset</el-button>
    </div>

    <div v-loading="loading" class="grid">
      <CourseCard v-for="course in courses" :key="course.id" :course="course" @open="openCourse" />
    </div>

    <EmptyState v-if="!loading && !courses.length" icon="Search" title="No course matches your filters"
                description="Try a different keyword, or clear the filters to see the whole catalogue.">
      <template #action>
        <el-button type="primary" @click="resetFilters">Clear filters</el-button>
      </template>
    </EmptyState>

    <div v-if="total > 0" class="pager">
      <el-pagination background layout="prev, pager, next, total" :total="total"
                     :current-page="page" :page-size="size" @current-change="onPageChange" />
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { courseApi } from '@/api'
import CourseCard from '@/components/CourseCard.vue'
import EmptyState from '@/components/EmptyState.vue'
import { LEVEL_OPTIONS } from '@/utils/format'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const courses = ref([])
const categories = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(9)

const filters = reactive({
  keyword: route.query.keyword || '',
  categoryId: route.query.categoryId ? Number(route.query.categoryId) : null,
  level: route.query.level || null,
  sortBy: 'newest'
})

function applyFilters() {
  page.value = 1
  load()
}

function resetFilters() {
  filters.keyword = ''
  filters.categoryId = null
  filters.level = null
  filters.sortBy = 'newest'
  applyFilters()
}

function openCourse(courseId) {
  router.push(`/courses/${courseId}`)
}

function onPageChange(next) {
  page.value = next
  load()
}

async function load() {
  loading.value = true
  try {
    const data = await courseApi.list({
      keyword: filters.keyword || undefined,
      categoryId: filters.categoryId || undefined,
      level: filters.level || undefined,
      sortBy: filters.sortBy || undefined,
      page: page.value,
      size: size.value
    })
    courses.value = data.records || []
    total.value = data.total || 0
  } catch (error) {
    courses.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

// React to filters coming from the home page category tiles.
watch(
  () => route.query,
  (query) => {
    filters.keyword = query.keyword || ''
    filters.categoryId = query.categoryId ? Number(query.categoryId) : null
    filters.level = query.level || null
    page.value = 1
    load()
  }
)

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
  margin-bottom: 22px;
}

.filter-search {
  flex: 1;
  min-width: 220px;
}

.filter-select {
  width: 178px;
}

.grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(258px, 1fr));
  gap: 18px;
  min-height: 120px;
}

.pager {
  display: flex;
  justify-content: center;
  margin-top: 30px;
}
</style>
