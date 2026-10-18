<template>
  <div class="page">
    <div class="section-head">
      <div>
        <h1 class="page-title">Categories</h1>
        <p class="page-subtitle" style="margin: 0">
          Categories group courses and drive the recommendation engine.
        </p>
      </div>
      <el-button type="primary" @click="openCreate">
        <el-icon><Plus /></el-icon>&nbsp;New category
      </el-button>
    </div>

    <div class="card card-pad table-card">
      <el-table v-loading="loading" :data="categories" style="width: 100%"
                empty-text="No category yet">
        <el-table-column label="Order" width="80">
          <template #default="{ row }"><span class="mono">{{ row.sortOrder }}</span></template>
        </el-table-column>
        <el-table-column label="Name" min-width="180" prop="name" />
        <el-table-column label="Code" width="130">
          <template #default="{ row }"><span class="mono">{{ row.code }}</span></template>
        </el-table-column>
        <el-table-column label="Description" min-width="240" prop="description" />
        <el-table-column label="Courses" width="110">
          <template #default="{ row }"><span class="mono">{{ row.courseCount }}</span></template>
        </el-table-column>
        <el-table-column label="" width="110" align="right">
          <template #default="{ row }">
            <el-button size="small" @click="openEdit(row)">Edit</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dialog" :title="editing ? 'Edit category' : 'New category'" width="480px">
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="Name" prop="name">
          <el-input v-model="form.name" placeholder="e.g. Data Science" />
        </el-form-item>
        <el-form-item label="Code" prop="code">
          <el-input v-model="form.code" placeholder="e.g. DATA" />
        </el-form-item>
        <el-form-item label="Description">
          <el-input v-model="form.description" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="Display order">
          <el-input-number v-model="form.sortOrder" :min="0" :max="99" controls-position="right" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">Cancel</el-button>
        <el-button type="primary" :loading="saving" @click="save">
          {{ editing ? 'Save changes' : 'Create category' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { adminApi, courseApi } from '@/api'

const loading = ref(false)
const saving = ref(false)
const categories = ref([])

const dialog = ref(false)
const editing = ref(false)
const editingId = ref(null)
const formRef = ref()

const form = reactive({ name: '', code: '', description: '', sortOrder: 0 })

const rules = {
  name: [{ required: true, message: 'Please enter a name', trigger: 'blur' }],
  code: [{ required: true, message: 'Please enter a code', trigger: 'blur' }]
}

async function load() {
  loading.value = true
  try {
    categories.value = await courseApi.categories()
  } catch (error) {
    categories.value = []
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editing.value = false
  editingId.value = null
  Object.assign(form, { name: '', code: '', description: '', sortOrder: categories.value.length + 1 })
  dialog.value = true
}

function openEdit(row) {
  editing.value = true
  editingId.value = row.id
  Object.assign(form, {
    name: row.name,
    code: row.code,
    description: row.description || '',
    sortOrder: row.sortOrder ?? 0
  })
  dialog.value = true
}

async function save() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    if (editing.value) {
      await adminApi.updateCategory(editingId.value, { ...form })
      ElMessage.success('Category updated')
    } else {
      await adminApi.createCategory({ ...form })
      ElMessage.success('Category created')
    }
    dialog.value = false
    await load()
  } catch (error) {
    /* handled globally */
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.table-card {
  padding: 8px 8px 4px;
}
</style>
