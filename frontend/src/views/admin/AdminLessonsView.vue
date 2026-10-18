<template>
  <div class="page">
    <el-button link class="back" @click="$router.push('/admin/courses')">
      <el-icon><ArrowLeft /></el-icon> Back to courses
    </el-button>

    <div class="section-head">
      <div>
        <h1 class="page-title">Chapters</h1>
        <p class="page-subtitle" style="margin: 0">{{ courseTitle || 'Loading course ...' }}</p>
      </div>
      <el-button type="primary" @click="openCreate">
        <el-icon><Plus /></el-icon>&nbsp;Add chapter
      </el-button>
    </div>

    <div class="card card-pad table-card">
      <el-table v-loading="loading" :data="lessons" style="width: 100%"
                empty-text="This course has no chapter yet">
        <el-table-column label="#" width="70">
          <template #default="{ row }">
            <span class="mono">{{ row.sortOrder }}</span>
          </template>
        </el-table-column>

        <el-table-column label="Chapter" min-width="240">
          <template #default="{ row }">
            <div class="cell-title">{{ row.title }}</div>
            <div class="muted tiny">{{ row.summary }}</div>
          </template>
        </el-table-column>

        <el-table-column label="Duration" width="110">
          <template #default="{ row }">
            <span class="mono">{{ formatMinutes(row.durationMinutes) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="Actions" width="170" align="right">
          <template #default="{ row }">
            <el-button size="small" @click="openEdit(row)">Edit</el-button>
            <el-button size="small" type="danger" plain @click="remove(row)">Delete</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dialog" :title="editing ? 'Edit chapter' : 'New chapter'" width="600px">
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="Title" prop="title">
          <el-input v-model="form.title" placeholder="e.g. Variables, types and operators" />
        </el-form-item>
        <el-form-item label="Short summary">
          <el-input v-model="form.summary" placeholder="One line describing the chapter" />
        </el-form-item>
        <el-form-item label="Content">
          <el-input v-model="form.content" type="textarea" :rows="5"
                    placeholder="Teaching notes shown to the student" />
        </el-form-item>
        <div class="two-col">
          <el-form-item label="Duration (minutes)">
            <el-input-number v-model="form.durationMinutes" :min="0" :max="600" :step="5"
                             controls-position="right" style="width: 100%" />
          </el-form-item>
          <el-form-item label="Order">
            <el-input-number v-model="form.sortOrder" :min="1" :max="999"
                             controls-position="right" style="width: 100%" />
          </el-form-item>
        </div>
        <el-form-item label="Video URL (optional)">
          <el-input v-model="form.videoUrl" placeholder="https://..." />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">Cancel</el-button>
        <el-button type="primary" :loading="saving" @click="save">
          {{ editing ? 'Save changes' : 'Add chapter' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminApi } from '@/api'
import { formatMinutes } from '@/utils/format'

const route = useRoute()
const courseId = route.params.id

const loading = ref(false)
const saving = ref(false)
const lessons = ref([])
const courseTitle = ref('')

const dialog = ref(false)
const editing = ref(false)
const editingId = ref(null)
const formRef = ref()

const form = reactive({
  title: '',
  summary: '',
  content: '',
  videoUrl: '',
  durationMinutes: 30,
  sortOrder: 1
})

const rules = {
  title: [{ required: true, message: 'Please enter a chapter title', trigger: 'blur' }]
}

async function load() {
  loading.value = true
  try {
    lessons.value = await adminApi.lessons(courseId)
    if (!courseTitle.value) {
      const detail = await adminApi.courseDetail(courseId)
      courseTitle.value = detail.course.title
    }
  } catch (error) {
    lessons.value = []
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editing.value = false
  editingId.value = null
  Object.assign(form, {
    title: '',
    summary: '',
    content: '',
    videoUrl: '',
    durationMinutes: 30,
    sortOrder: lessons.value.length + 1
  })
  dialog.value = true
}

function openEdit(row) {
  editing.value = true
  editingId.value = row.id
  Object.assign(form, {
    title: row.title,
    summary: row.summary || '',
    content: row.content || '',
    videoUrl: row.videoUrl || '',
    durationMinutes: row.durationMinutes ?? 0,
    sortOrder: row.sortOrder ?? 1
  })
  dialog.value = true
}

async function save() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    if (editing.value) {
      await adminApi.updateLesson(editingId.value, { ...form })
      ElMessage.success('Chapter updated')
    } else {
      await adminApi.createLesson(courseId, { ...form })
      ElMessage.success('Chapter added')
    }
    dialog.value = false
    await load()
  } catch (error) {
    /* handled globally */
  } finally {
    saving.value = false
  }
}

async function remove(row) {
  try {
    await ElMessageBox.confirm(`Delete the chapter "${row.title}"?`, 'Delete chapter', {
      confirmButtonText: 'Delete',
      cancelButtonText: 'Cancel',
      type: 'warning'
    })
    await adminApi.deleteLesson(row.id)
    ElMessage.success('Chapter deleted')
    await load()
  } catch (error) {
    /* cancelled or handled globally */
  }
}

onMounted(load)
</script>

<style scoped>
.back {
  margin-bottom: 12px;
}

.table-card {
  padding: 8px 8px 4px;
}

.cell-title {
  font-weight: 600;
  font-size: 13.5px;
}

.two-col {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0 18px;
}

@media (max-width: 700px) {
  .two-col {
    grid-template-columns: 1fr;
  }
}
</style>
