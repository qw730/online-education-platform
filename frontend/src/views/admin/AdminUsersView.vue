<template>
  <div class="page">
    <div class="section-head">
      <div>
        <h1 class="page-title">Users</h1>
        <p class="page-subtitle" style="margin: 0">
          Every account on the platform. Disable an account to block its sign in.
        </p>
      </div>
    </div>

    <div class="filters card card-pad">
      <el-input v-model="keyword" placeholder="Search by username or name" clearable
                class="filter-search" @keyup.enter="applySearch">
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      <el-button type="primary" @click="applySearch">Search</el-button>
      <el-button @click="reset">Reset</el-button>
    </div>

    <div class="card card-pad table-card">
      <el-table v-loading="loading" :data="users" style="width: 100%" empty-text="No user found">
        <el-table-column label="User" min-width="220">
          <template #default="{ row }">
            <div class="cell-user">
              <div class="cell-avatar">{{ initials(row.realName || row.username) }}</div>
              <div>
                <div class="cell-title">{{ row.realName || row.username }}</div>
                <div class="muted tiny">@{{ row.username }}</div>
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="Role" width="130">
          <template #default="{ row }">
            <el-tag size="small" :type="row.role === 'ADMIN' ? 'warning' : 'primary'" effect="light" round>
              {{ row.role === 'ADMIN' ? 'Administrator' : 'Student' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="Email" min-width="200">
          <template #default="{ row }">
            <span class="muted">{{ row.email || '-' }}</span>
          </template>
        </el-table-column>

        <el-table-column label="Status" width="130">
          <template #default="{ row }">
            <el-tag size="small" :type="row.status === 1 ? 'success' : 'danger'" effect="light" round>
              {{ row.status === 1 ? 'Active' : 'Disabled' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="Registered" width="170">
          <template #default="{ row }">
            <span class="muted tiny">{{ formatDateTime(row.createdAt) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="" width="150" align="right">
          <template #default="{ row }">
            <el-button v-if="row.role !== 'ADMIN'" size="small"
                       :type="row.status === 1 ? 'danger' : 'success'" plain
                       @click="toggle(row)">
              {{ row.status === 1 ? 'Disable' : 'Enable' }}
            </el-button>
            <span v-else class="muted tiny">protected</span>
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
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminApi } from '@/api'
import { formatDateTime, initials } from '@/utils/format'

const loading = ref(false)
const users = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const keyword = ref('')

async function load() {
  loading.value = true
  try {
    const data = await adminApi.users({
      keyword: keyword.value || undefined,
      page: page.value,
      size: size.value
    })
    users.value = data.records || []
    total.value = data.total || 0
  } catch (error) {
    users.value = []
  } finally {
    loading.value = false
  }
}

function applySearch() {
  page.value = 1
  load()
}

function reset() {
  keyword.value = ''
  applySearch()
}

function onPageChange(next) {
  page.value = next
  load()
}

async function toggle(row) {
  try {
    await ElMessageBox.confirm(
      row.status === 1
        ? `Disable "${row.username}"? They will no longer be able to sign in.`
        : `Enable "${row.username}" again?`,
      'Change account status',
      { confirmButtonText: 'Confirm', cancelButtonText: 'Cancel', type: 'warning' }
    )
    await adminApi.toggleUserStatus(row.id)
    ElMessage.success('Account status updated')
    await load()
  } catch (error) {
    /* cancelled or handled globally */
  }
}

onMounted(load)
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
  min-width: 240px;
}

.table-card {
  padding: 8px 8px 4px;
}

.cell-user {
  display: flex;
  align-items: center;
  gap: 11px;
}

.cell-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  color: #fff;
  font-size: 12px;
  font-weight: 700;
  background: linear-gradient(135deg, var(--brand-1), var(--brand-2));
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
