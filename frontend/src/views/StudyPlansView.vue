<template>
  <div class="page">
    <div class="section-head">
      <div>
        <h1 class="page-title">Study plans</h1>
        <p class="page-subtitle" style="margin: 0">
          Break a course into dated tasks and tick them off one by one.
        </p>
      </div>
      <el-button type="primary" @click="openCreate">
        <el-icon><Plus /></el-icon>&nbsp;New study plan
      </el-button>
    </div>

    <div v-loading="loading" class="plans">
      <div v-for="plan in plans" :key="plan.id" class="card plan">
        <div class="plan-head">
          <div class="plan-head-main">
            <div class="row-gap" style="gap: 8px">
              <el-tag size="small" :type="planStatusType(plan.status)" effect="light" round>
                {{ planStatusLabel(plan.status) }}
              </el-tag>
              <span class="muted tiny">{{ plan.courseTitle }}</span>
            </div>
            <div class="plan-title">{{ plan.title }}</div>
            <div class="muted tiny">{{ plan.goal }}</div>
          </div>
          <el-dropdown trigger="click" @command="(command) => onCommand(command, plan)">
            <el-button text><el-icon><MoreFilled /></el-icon></el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="edit">Edit plan</el-dropdown-item>
                <el-dropdown-item command="course">Open course</el-dropdown-item>
                <el-dropdown-item command="delete" divided>Delete plan</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>

        <div class="plan-stats">
          <span><el-icon><Calendar /></el-icon> {{ plan.startDate }} → {{ plan.endDate }}</span>
          <span><el-icon><Timer /></el-icon> {{ plan.dailyMinutes }} min / day</span>
          <span><el-icon><List /></el-icon> {{ plan.doneItems }} / {{ plan.totalItems }} tasks</span>
        </div>

        <el-progress :percentage="plan.progressPercent" :stroke-width="8"
                     :color="progressColor(plan.progressPercent)" :show-text="false" />

        <div class="items">
          <div v-for="item in plan.items" :key="item.id" class="item" :class="{ done: item.status === 'DONE' }">
            <el-checkbox :model-value="item.status === 'DONE'" @change="() => toggleItem(plan, item)" />
            <div class="item-main">
              <div class="item-title">{{ item.title }}</div>
              <div class="muted tiny">{{ item.plannedDate }}</div>
            </div>
            <el-button text type="danger" size="small" @click="removeItem(plan, item)">
              <el-icon><Delete /></el-icon>
            </el-button>
          </div>
          <div v-if="!plan.items.length" class="muted tiny empty-items">
            No task yet - add one below or edit the plan.
          </div>
        </div>

        <el-button size="small" plain class="add-item" @click="openAddItem(plan)">
          <el-icon><Plus /></el-icon>&nbsp;Add task
        </el-button>
      </div>
    </div>

    <EmptyState v-if="!loading && !plans.length" icon="Calendar"
                title="You have no study plan yet"
                description="Create one from a course you are enrolled in, or generate the tasks automatically from its chapters.">
      <template #action>
        <el-button type="primary" @click="openCreate">Create my first plan</el-button>
      </template>
    </EmptyState>

    <!-- create / edit -->
    <el-dialog v-model="dialog" :title="editing ? 'Edit study plan' : 'New study plan'" width="540px">
      <el-form :model="form" label-position="top">
        <el-form-item label="Course">
          <el-select v-model="form.courseId" placeholder="Select an enrolled course"
                     :disabled="editing" style="width: 100%">
            <el-option v-for="course in courses" :key="course.courseId" :label="course.courseTitle"
                       :value="course.courseId" />
          </el-select>
        </el-form-item>
        <el-form-item label="Plan title">
          <el-input v-model="form.title" placeholder="e.g. Finish Java in four weeks" />
        </el-form-item>
        <el-form-item label="Goal">
          <el-input v-model="form.goal" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="Period">
          <el-date-picker v-model="form.range" type="daterange" value-format="YYYY-MM-DD"
                          start-placeholder="Start" end-placeholder="End" style="width: 100%" />
        </el-form-item>
        <el-form-item label="Daily study time (minutes)">
          <el-input-number v-model="form.dailyMinutes" :min="10" :max="480" :step="10" />
        </el-form-item>
        <el-form-item v-if="editing" label="Status">
          <el-select v-model="form.status" style="width: 100%">
            <el-option label="Planned" value="PLANNED" />
            <el-option label="In progress" value="IN_PROGRESS" />
            <el-option label="Completed" value="COMPLETED" />
            <el-option label="Abandoned" value="ABANDONED" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="!editing">
          <el-checkbox v-model="form.autoGenerateItems">
            Generate one task per chapter automatically
          </el-checkbox>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">Cancel</el-button>
        <el-button type="primary" :loading="saving" @click="save">
          {{ editing ? 'Save changes' : 'Create plan' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- add task -->
    <el-dialog v-model="itemDialog" title="Add a task" width="460px">
      <el-form :model="itemForm" label-position="top">
        <el-form-item label="Task title">
          <el-input v-model="itemForm.title" placeholder="e.g. Revisit exceptions chapter" />
        </el-form-item>
        <el-form-item label="Planned date">
          <el-date-picker v-model="itemForm.plannedDate" type="date" value-format="YYYY-MM-DD"
                          style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="itemDialog = false">Cancel</el-button>
        <el-button type="primary" :loading="saving" @click="addItem">Add task</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { learningApi, planApi } from '@/api'
import EmptyState from '@/components/EmptyState.vue'
import { planStatusLabel, planStatusType, progressColor } from '@/utils/format'

const router = useRouter()

const loading = ref(false)
const saving = ref(false)
const plans = ref([])
const courses = ref([])

const dialog = ref(false)
const editing = ref(false)
const editingId = ref(null)
const form = reactive({
  courseId: null,
  title: '',
  goal: '',
  range: [],
  dailyMinutes: 30,
  status: 'PLANNED',
  autoGenerateItems: true
})

const itemDialog = ref(false)
const itemForm = reactive({ planId: null, title: '', plannedDate: '' })

async function load() {
  loading.value = true
  try {
    plans.value = await planApi.list()
  } catch (error) {
    plans.value = []
  } finally {
    loading.value = false
  }
}

async function loadCourses() {
  try {
    courses.value = await learningApi.myActiveCourses()
  } catch (error) {
    courses.value = []
  }
}

function openCreate() {
  if (!courses.value.length) {
    ElMessage.warning('Enrol in a course first, then you can plan your study.')
    router.push('/courses')
    return
  }
  const today = new Date()
  const end = new Date()
  end.setDate(end.getDate() + 21)
  editing.value = false
  editingId.value = null
  Object.assign(form, {
    courseId: courses.value[0].courseId,
    title: '',
    goal: '',
    range: [today.toISOString().slice(0, 10), end.toISOString().slice(0, 10)],
    dailyMinutes: 30,
    status: 'PLANNED',
    autoGenerateItems: true
  })
  dialog.value = true
}

function openEdit(plan) {
  editing.value = true
  editingId.value = plan.id
  Object.assign(form, {
    courseId: plan.courseId,
    title: plan.title,
    goal: plan.goal || '',
    range: [plan.startDate, plan.endDate],
    dailyMinutes: plan.dailyMinutes,
    status: plan.status,
    autoGenerateItems: false
  })
  dialog.value = true
}

function openAddItem(plan) {
  itemForm.planId = plan.id
  itemForm.title = ''
  itemForm.plannedDate = new Date().toISOString().slice(0, 10)
  itemDialog.value = true
}

async function save() {
  if (!form.courseId || !form.title || form.range.length !== 2) {
    ElMessage.warning('Please fill in the course, the title and the date range')
    return
  }
  saving.value = true
  const payload = {
    courseId: form.courseId,
    title: form.title,
    goal: form.goal,
    startDate: form.range[0],
    endDate: form.range[1],
    dailyMinutes: form.dailyMinutes,
    status: form.status,
    autoGenerateItems: form.autoGenerateItems
  }
  try {
    if (editing.value) {
      await planApi.update(editingId.value, payload)
      ElMessage.success('Study plan updated')
    } else {
      await planApi.create(payload)
      ElMessage.success('Study plan created')
    }
    dialog.value = false
    await load()
  } catch (error) {
    /* handled globally */
  } finally {
    saving.value = false
  }
}

async function addItem() {
  if (!itemForm.title) {
    ElMessage.warning('Please give the task a title')
    return
  }
  saving.value = true
  try {
    await planApi.addItem(itemForm.planId, {
      title: itemForm.title,
      plannedDate: itemForm.plannedDate
    })
    ElMessage.success('Task added')
    itemDialog.value = false
    await load()
  } catch (error) {
    /* handled globally */
  } finally {
    saving.value = false
  }
}

async function toggleItem(plan, item) {
  try {
    await planApi.toggleItem(plan.id, item.id)
    await load()
  } catch (error) {
    /* handled globally */
  }
}

async function removeItem(plan, item) {
  try {
    await planApi.removeItem(plan.id, item.id)
    await load()
  } catch (error) {
    /* handled globally */
  }
}

async function removePlan(plan) {
  try {
    await ElMessageBox.confirm(`Delete the plan "${plan.title}"?`, 'Delete plan', {
      confirmButtonText: 'Delete',
      cancelButtonText: 'Cancel',
      type: 'warning'
    })
    await planApi.remove(plan.id)
    ElMessage.success('Study plan deleted')
    await load()
  } catch (error) {
    /* cancelled or handled globally */
  }
}

function onCommand(command, plan) {
  if (command === 'edit') openEdit(plan)
  if (command === 'delete') removePlan(plan)
  if (command === 'course') router.push(`/my/courses/${plan.courseId}`)
}

onMounted(() => {
  load()
  loadCourses()
})
</script>

<style scoped>
.plans {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(400px, 1fr));
  gap: 18px;
  min-height: 80px;
}

.plan {
  padding: 18px;
  display: flex;
  flex-direction: column;
  gap: 13px;
}

.plan-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.plan-head-main {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
}

.plan-title {
  font-size: 16px;
  font-weight: 700;
  letter-spacing: -0.01em;
}

.plan-stats {
  display: flex;
  flex-wrap: wrap;
  gap: 15px;
  color: var(--ink-500);
  font-size: 12px;
}

.plan-stats span {
  display: inline-flex;
  align-items: center;
  gap: 5px;
}

.items {
  display: flex;
  flex-direction: column;
  gap: 6px;
  max-height: 260px;
  overflow-y: auto;
}

.item {
  display: flex;
  align-items: center;
  gap: 9px;
  padding: 7px 10px;
  border-radius: 9px;
  background: #fafafa;
}

.item.done .item-title {
  text-decoration: line-through;
  color: var(--ink-500);
}

.item-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.item-title {
  font-size: 13px;
  font-weight: 600;
}

.empty-items {
  padding: 10px 4px;
}

.add-item {
  align-self: flex-start;
}

@media (max-width: 700px) {
  .plans {
    grid-template-columns: 1fr;
  }
}
</style>
