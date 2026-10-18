<template>
  <div class="course-card" @click="$emit('open', course.id)">
    <div class="course-cover" :style="{ background: coverGradient(course.categoryId ?? course.id) }">
      {{ initials(course.title) }}
    </div>
    <div class="course-body">
      <div class="row-gap" style="gap: 6px">
        <el-tag size="small" effect="plain" round>{{ course.categoryName || 'General' }}</el-tag>
        <el-tag size="small" :type="levelTagType(course.level)" effect="light" round>
          {{ levelLabel(course.level) }}
        </el-tag>
        <el-tag v-if="course.status === 0" size="small" type="info" effect="dark" round>Draft</el-tag>
      </div>

      <div class="course-title">{{ course.title }}</div>
      <div class="muted tiny clamp-2">{{ course.subtitle || 'No description provided yet.' }}</div>

      <div v-if="reason" class="reason">
        <el-icon><MagicStick /></el-icon>
        <span>{{ reason }}</span>
      </div>

      <div class="spacer" />

      <div class="course-meta">
        <span><el-icon><User /></el-icon> {{ course.studentCount ?? 0 }}</span>
        <span><el-icon><Reading /></el-icon> {{ course.lessonCount ?? 0 }} lessons</span>
        <span><el-icon><Star /></el-icon> {{ Number(course.rating ?? 0).toFixed(1) }}</span>
      </div>

      <div class="card-foot">
        <span class="price">{{ formatPrice(course.price) }}</span>
        <el-tag v-if="matchScore" size="small" type="success" effect="light" round>
          Match {{ matchScore }}
        </el-tag>
      </div>
    </div>
  </div>
</template>

<script setup>
import { coverGradient, formatPrice, initials, levelLabel, levelTagType } from '@/utils/format'

defineProps({
  course: { type: Object, required: true },
  reason: { type: String, default: '' },
  matchScore: { type: Number, default: 0 }
})

defineEmits(['open'])
</script>

<style scoped>
.reason {
  display: flex;
  align-items: flex-start;
  gap: 6px;
  font-size: 12px;
  line-height: 1.45;
  color: #6d28d9;
  background: #f5f3ff;
  border: 1px solid #ede9fe;
  border-radius: 8px;
  padding: 6px 9px;
}

.clamp-2 {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  line-height: 1.5;
}

.card-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-top: 1px solid var(--line);
  padding-top: 10px;
  margin-top: 2px;
}

.price {
  font-weight: 700;
  color: var(--brand-1);
}
</style>
