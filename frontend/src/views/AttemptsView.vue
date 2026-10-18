<template>
  <div class="page">
    <h1 class="page-title">Quiz history</h1>
    <p class="page-subtitle">Every quiz you have submitted, with the score the backend calculated.</p>

    <div class="card card-pad table-card">
      <el-table v-loading="loading" :data="attempts" style="width: 100%"
                empty-text="You have not taken any quiz yet">
        <el-table-column label="Quiz" min-width="220">
          <template #default="{ row }">
            <div class="cell-title">{{ row.quizTitle }}</div>
            <div class="muted tiny">{{ row.courseTitle }}</div>
          </template>
        </el-table-column>

        <el-table-column label="Score" width="150">
          <template #default="{ row }">
            <span class="score">{{ row.score }}</span>
            <span class="muted tiny"> / {{ row.totalScore }}</span>
            <span class="muted tiny"> ({{ percent(row) }}%)</span>
          </template>
        </el-table-column>

        <el-table-column label="Correct" width="110">
          <template #default="{ row }">
            <span class="mono">{{ row.correctCount }}/{{ row.totalCount }}</span>
          </template>
        </el-table-column>

        <el-table-column label="Result" width="120">
          <template #default="{ row }">
            <el-tag size="small" :type="row.passed ? 'success' : 'danger'" effect="light" round>
              {{ row.passed ? 'Passed' : 'Failed' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="Duration" width="110">
          <template #default="{ row }">
            <span class="muted tiny">{{ row.durationSeconds }}s</span>
          </template>
        </el-table-column>

        <el-table-column label="Submitted" width="170">
          <template #default="{ row }">
            <span class="muted tiny">{{ formatDateTime(row.createdAt) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="" width="120" align="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" plain
                       @click="$router.push(`/quiz/result/${row.id}`)">Review</el-button>
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
import { quizApi } from '@/api'
import { formatDateTime } from '@/utils/format'

const loading = ref(false)
const attempts = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)

function percent(row) {
  if (!row.totalScore) return 0
  return Math.round((row.score * 100) / row.totalScore)
}

async function load() {
  loading.value = true
  try {
    const data = await quizApi.myAttempts({ page: page.value, size: size.value })
    attempts.value = data.records || []
    total.value = data.total || 0
  } catch (error) {
    attempts.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function onPageChange(next) {
  page.value = next
  load()
}

onMounted(load)
</script>

<style scoped>
.table-card {
  padding: 8px 8px 4px;
}

.cell-title {
  font-weight: 600;
  font-size: 13.5px;
}

.score {
  font-weight: 750;
  font-size: 16px;
  color: var(--brand-1);
}

.pager {
  display: flex;
  justify-content: center;
  padding: 16px 0 8px;
}
</style>
