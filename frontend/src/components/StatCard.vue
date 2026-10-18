<template>
  <div class="stat card">
    <div class="stat-icon" :style="{ background: tint }">
      <el-icon :size="20"><component :is="icon" /></el-icon>
    </div>
    <div class="stat-body">
      <div class="stat-value">{{ value }}</div>
      <div class="stat-label">{{ label }}</div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  label: { type: String, required: true },
  value: { type: [String, Number], default: 0 },
  icon: { type: String, default: 'DataLine' },
  color: { type: String, default: '#4f46e5' }
})

const tint = computed(() => `${props.color}18`)
</script>

<style scoped>
.stat {
  display: flex;
  align-items: center;
  gap: 13px;
  padding: 16px 18px;
}

.stat-icon {
  width: 42px;
  height: 42px;
  border-radius: 11px;
  display: grid;
  place-items: center;
  color: v-bind(color);
  flex-shrink: 0;
}

.stat-value {
  font-size: 21px;
  font-weight: 750;
  line-height: 1.15;
  letter-spacing: -0.02em;
}

.stat-label {
  font-size: 12px;
  color: var(--ink-500);
  margin-top: 2px;
}
</style>
