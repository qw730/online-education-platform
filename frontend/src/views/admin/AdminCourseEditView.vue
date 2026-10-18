<template>
  <div class="page narrow">
    <el-button link class="back" @click="$router.push('/admin/courses')">
      <el-icon><ArrowLeft /></el-icon> Back to courses
    </el-button>

    <h1 class="page-title">{{ isEdit ? 'Edit course' : 'New course' }}</h1>
    <p class="page-subtitle">
      {{ isEdit ? 'Update the course information and publish it when ready.' : 'Fill in the course information, then add chapters and a quiz.' }}
    </p>

    <div v-loading="loading" class="card card-pad">
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <div class="two-col">
          <el-form-item label="Title" prop="title" class="span-2">
            <el-input v-model="form.title" placeholder="e.g. Java Programming Fundamentals" />
          </el-form-item>

          <el-form-item label="Subtitle" class="span-2">
            <el-input v-model="form.subtitle" placeholder="One line that sells the course" />
          </el-form-item>

          <el-form-item label="Category" prop="categoryId">
            <el-select v-model="form.categoryId" placeholder="Select a category" style="width: 100%">
              <el-option v-for="category in categories" :key="category.id"
                         :label="category.name" :value="category.id" />
            </el-select>
          </el-form-item>

          <el-form-item label="Teacher">
            <el-input v-model="form.teacherName" placeholder="Instructor name" />
          </el-form-item>

          <el-form-item label="Level">
            <el-select v-model="form.level" style="width: 100%">
              <el-option v-for="item in LEVEL_OPTIONS" :key="item.value"
                         :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>

          <el-form-item label="Status">
            <el-select v-model="form.status" style="width: 100%">
              <el-option label="Published" :value="1" />
              <el-option label="Draft (hidden from students)" :value="0" />
            </el-select>
          </el-form-item>

          <el-form-item label="Price (¥)">
            <el-input-number v-model="form.price" :min="0" :precision="2" :step="10"
                             controls-position="right" style="width: 100%" />
          </el-form-item>

          <el-form-item label="Total duration (hours)">
            <el-input-number v-model="form.durationHours" :min="0" :max="999"
                             controls-position="right" style="width: 100%" />
          </el-form-item>

          <el-form-item label="Keywords (comma separated, used by the recommender)" class="span-2">
            <el-input v-model="form.tags" placeholder="java, programming, backend" />
            <div class="muted tiny hint">
              Tip: reuse the keywords of similar courses so students get better suggestions.
            </div>
          </el-form-item>

          <el-form-item label="Description" class="span-2">
            <el-input v-model="form.description" type="textarea" :rows="6"
                      placeholder="What will the student learn?" />
          </el-form-item>
        </div>

        <div class="actions">
          <el-button @click="$router.push('/admin/courses')">Cancel</el-button>
          <el-button v-if="isEdit" @click="$router.push(`/admin/courses/${courseId}/lessons`)">
            Manage chapters
          </el-button>
          <el-button v-if="isEdit" @click="$router.push(`/admin/courses/${courseId}/quizzes`)">
            Manage quizzes
          </el-button>
          <el-button type="primary" :loading="saving" @click="save">
            {{ isEdit ? 'Save changes' : 'Create course' }}
          </el-button>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { adminApi, courseApi } from '@/api'
import { LEVEL_OPTIONS } from '@/utils/format'

const route = useRoute()
const router = useRouter()

const formRef = ref()
const loading = ref(false)
const saving = ref(false)
const categories = ref([])

const courseId = computed(() => route.params.id)
const isEdit = computed(() => Boolean(courseId.value))

const form = reactive({
  title: '',
  subtitle: '',
  description: '',
  coverUrl: '',
  categoryId: null,
  teacherName: '',
  level: 'BEGINNER',
  price: 0,
  durationHours: 0,
  tags: '',
  status: 1
})

const rules = {
  title: [{ required: true, message: 'Please enter a title', trigger: 'blur' }],
  categoryId: [{ required: true, message: 'Please select a category', trigger: 'change' }]
}

async function load() {
  if (!isEdit.value) return
  loading.value = true
  try {
    const detail = await adminApi.courseDetail(courseId.value)
    const course = detail.course
    Object.assign(form, {
      title: course.title,
      subtitle: course.subtitle || '',
      description: course.description || '',
      coverUrl: course.coverUrl || '',
      categoryId: course.categoryId,
      teacherName: course.teacherName || '',
      level: course.level || 'BEGINNER',
      price: Number(course.price ?? 0),
      durationHours: course.durationHours ?? 0,
      tags: course.tags || '',
      status: course.status ?? 1
    })
  } catch (error) {
    /* handled globally */
  } finally {
    loading.value = false
  }
}

async function save() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    if (isEdit.value) {
      await adminApi.updateCourse(courseId.value, { ...form })
      ElMessage.success('Course updated')
    } else {
      const created = await adminApi.createCourse({ ...form })
      ElMessage.success('Course created, now add its chapters')
      router.push(`/admin/courses/${created.id}/lessons`)
      return
    }
  } catch (error) {
    /* handled globally */
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  try {
    categories.value = await courseApi.categories()
  } catch (error) {
    categories.value = []
  }
  load()
})
</script>

<style scoped>
.narrow {
  max-width: 900px;
}

.back {
  margin-bottom: 12px;
}

.two-col {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0 18px;
}

.span-2 {
  grid-column: span 2;
}

.hint {
  margin-top: 4px;
  line-height: 1.5;
}

.actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  flex-wrap: wrap;
  border-top: 1px solid var(--line);
  padding-top: 18px;
  margin-top: 6px;
}

@media (max-width: 700px) {
  .two-col {
    grid-template-columns: 1fr;
  }

  .span-2 {
    grid-column: span 1;
  }
}
</style>
