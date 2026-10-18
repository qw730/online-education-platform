/* Small presentation helpers shared by the views. */

const GRADIENTS = [
  'linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%)',
  'linear-gradient(135deg, #0ea5e9 0%, #22d3ee 100%)',
  'linear-gradient(135deg, #f97316 0%, #fbbf24 100%)',
  'linear-gradient(135deg, #10b981 0%, #34d399 100%)',
  'linear-gradient(135deg, #ef4444 0%, #f472b6 100%)',
  'linear-gradient(135deg, #8b5cf6 0%, #ec4899 100%)',
  'linear-gradient(135deg, #0f766e 0%, #14b8a6 100%)',
  'linear-gradient(135deg, #4338ca 0%, #6366f1 100%)'
]

/** Deterministic gradient so the same course always looks the same. */
export function coverGradient(seed) {
  const key = String(seed ?? 0)
  let hash = 0
  for (let i = 0; i < key.length; i += 1) {
    hash = (hash * 31 + key.charCodeAt(i)) % 100000
  }
  return GRADIENTS[hash % GRADIENTS.length]
}

export function initials(text, max = 2) {
  if (!text) return '?'
  return text
    .split(/\s+/)
    .filter(Boolean)
    .map((word) => word[0])
    .slice(0, max)
    .join('')
    .toUpperCase()
}

export function formatDate(value) {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return String(value)
  return date.toLocaleDateString('en-GB', { year: 'numeric', month: 'short', day: '2-digit' })
}

export function formatDateTime(value) {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return String(value)
  return `${date.toLocaleDateString('en-GB', {
    year: 'numeric',
    month: 'short',
    day: '2-digit'
  })} ${date.toLocaleTimeString('en-GB', { hour: '2-digit', minute: '2-digit' })}`
}

export function formatMinutes(minutes) {
  const total = Number(minutes) || 0
  if (total < 60) return `${total}m`
  const hours = Math.floor(total / 60)
  const rest = total % 60
  return rest === 0 ? `${hours}h` : `${hours}h ${rest}m`
}

export function formatPrice(price) {
  const value = Number(price) || 0
  return value === 0 ? 'Free' : `¥${value.toFixed(2)}`
}

export const LEVEL_OPTIONS = [
  { label: 'Beginner', value: 'BEGINNER' },
  { label: 'Intermediate', value: 'INTERMEDIATE' },
  { label: 'Advanced', value: 'ADVANCED' }
]

export function levelLabel(level) {
  return LEVEL_OPTIONS.find((item) => item.value === level)?.label || level || '-'
}

export function levelTagType(level) {
  if (level === 'ADVANCED') return 'danger'
  if (level === 'INTERMEDIATE') return 'warning'
  return 'success'
}

export const ENROLLMENT_STATUS = {
  ENROLLED: { label: 'In progress', type: 'primary' },
  COMPLETED: { label: 'Completed', type: 'success' },
  DROPPED: { label: 'Dropped', type: 'info' }
}

export function enrollmentStatusLabel(status) {
  return ENROLLMENT_STATUS[status]?.label || status || '-'
}

export function enrollmentStatusType(status) {
  return ENROLLMENT_STATUS[status]?.type || 'info'
}

export const PLAN_STATUS = {
  PLANNED: { label: 'Planned', type: 'info' },
  IN_PROGRESS: { label: 'In progress', type: 'primary' },
  COMPLETED: { label: 'Completed', type: 'success' },
  ABANDONED: { label: 'Abandoned', type: 'warning' }
}

export function planStatusLabel(status) {
  return PLAN_STATUS[status]?.label || status || '-'
}

export function planStatusType(status) {
  return PLAN_STATUS[status]?.type || 'info'
}

export const QUESTION_TYPES = [
  { label: 'Single choice', value: 'SINGLE' },
  { label: 'Multiple choice', value: 'MULTIPLE' },
  { label: 'True / False', value: 'TRUE_FALSE' }
]

export function questionTypeLabel(type) {
  return QUESTION_TYPES.find((item) => item.value === type)?.label || type
}

/** Splits "a,b,c" into a clean array. */
export function splitTags(tags) {
  if (!tags) return []
  return String(tags)
    .split(',')
    .map((tag) => tag.trim())
    .filter(Boolean)
}

export function progressColor(percent) {
  const value = Number(percent) || 0
  if (value >= 100) return '#10b981'
  if (value >= 50) return '#4f46e5'
  if (value > 0) return '#f59e0b'
  return '#cbd5e1'
}
