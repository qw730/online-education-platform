<template>
  <div class="empty">
    <el-icon :size="42" class="empty-icon"><component :is="icon" /></el-icon>
    <div class="empty-title">{{ title }}</div>
    <div class="empty-desc muted">{{ description }}</div>
    <div v-if="$slots.action" class="empty-action">
      <slot name="action" />
    </div>
  </div>
</template>

<script setup>
defineProps({
  title: { type: String, default: 'Nothing here yet' },
  description: { type: String, default: '' },
  icon: { type: String, default: 'FolderOpened' }
})
</script>

<style scoped>
.empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  padding: 52px 20px;
  gap: 6px;
}

.empty-icon {
  color: #cbd5e1;
  margin-bottom: 6px;
}

.empty-title {
  font-size: 15px;
  font-weight: 650;
}

.empty-desc {
  font-size: 13px;
  max-width: 420px;
  line-height: 1.6;
}

.empty-action {
  margin-top: 14px;
}
</style>
