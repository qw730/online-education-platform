<template>
  <div class="page">
    <div class="section-head">
      <div>
        <h1 class="page-title">Administrator overview</h1>
        <p class="page-subtitle" style="margin: 0">Platform wide statistics and quick actions.</p>
      </div>
      <div class="row-gap">
        <el-button @click="$router.push('/admin/categories')">Categories</el-button>
        <el-button type="primary" @click="$router.push('/admin/courses/new')">
          <el-icon><Plus /></el-icon>&nbsp;New course
        </el-button>
      </div>
    </div>

    <div v-loading="loading" class="stats">
      <StatCard label="Total users" :value="stats.totalUsers" icon="UserFilled" color="#4f46e5" />
      <StatCard label="Students" :value="stats.totalStudents" icon="User" color="#0ea5e9" />
      <StatCard label="Courses" :value="stats.totalCourses" icon="Collection" color="#8b5cf6" />
      <StatCard label="Published" :value="stats.publishedCourses" icon="Promotion" color="#10b981" />
      <StatCard label="Drafts" :value="stats.draftCourses" icon="EditPen" color="#f59e0b" />
      <StatCard label="Enrolments" :value="stats.totalEnrollments" icon="Tickets" color="#ef4444" />
      <StatCard label="Quizzes" :value="stats.totalQuizzes" icon="Notebook" color="#14b8a6" />
      <StatCard label="Questions" :value="stats.totalQuestions" icon="List" color="#6366f1" />
      <StatCard label="Quiz submissions" :value="stats.totalAttempts" icon="Finished" color="#f97316" />
      <StatCard label="Categories" :value="stats.totalCategories" icon="Grid" color="#a855f7" />
      <StatCard label="Administrators" :value="stats.totalAdmins" icon="Setting" color="#64748b" />
    </div>

    <section class="block">
      <div class="section-head">
        <div>
          <h2 class="section-title">Most popular courses</h2>
          <div class="muted tiny">Ranked by enrolment count</div>
        </div>
        <el-button link type="primary" @click="$router.push('/admin/courses')">Manage courses</el-button>
      </div>
      <div class="grid">
        <CourseCard v-for="course in topCourses" :key="course.id" :course="course"
                    @open="(id) => $router.push(`/admin/courses/${id}/edit`)" />
      </div>
      <EmptyState v-if="!loading && !topCourses.length" icon="Collection"
                  title="No published course yet"
                  description="Create your first course to get started.">
        <template #action>
          <el-button type="primary" @click="$router.push('/admin/courses/new')">Create a course</el-button>
        </template>
      </EmptyState>
    </section>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { dashboardApi } from '@/api'
import StatCard from '@/components/StatCard.vue'
import CourseCard from '@/components/CourseCard.vue'
import EmptyState from '@/components/EmptyState.vue'

const loading = ref(false)
const topCourses = ref([])
const stats = reactive({
  totalUsers: 0,
  totalStudents: 0,
  totalAdmins: 0,
  totalCourses: 0,
  publishedCourses: 0,
  draftCourses: 0,
  totalCategories: 0,
  totalEnrollments: 0,
  totalQuizzes: 0,
  totalQuestions: 0,
  totalAttempts: 0
})

async function load() {
  loading.value = true
  try {
    const data = await dashboardApi.admin()
    Object.assign(stats, data)
    topCourses.value = data.topCourses || []
  } catch (error) {
    /* handled globally */
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.stats {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(178px, 1fr));
  gap: 14px;
  min-height: 90px;
}

.block {
  margin-top: 40px;
}

.grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(258px, 1fr));
  gap: 18px;
}
</style>
