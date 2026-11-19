<script setup lang="ts">
import type { CategoryTreeResponse } from '../api/types'

defineProps<{
  categories: CategoryTreeResponse[]
  modelValue: number | null
  loading?: boolean
}>()

const emit = defineEmits<{
  'update:modelValue': [value: number | null]
}>()

function select(id: number | null) {
  emit('update:modelValue', id)
}
</script>

<template>
  <div class="category-filter" role="group" aria-label="分类筛选">
    <button
      type="button"
      class="chip"
      :class="{ active: modelValue === null }"
      :disabled="loading"
      @click="select(null)"
    >
      全部
    </button>
    <button
      v-for="category in categories"
      :key="category.id"
      type="button"
      class="chip"
      :class="{ active: modelValue === category.id }"
      :disabled="loading"
      @click="select(category.id)"
    >
      {{ category.name }}
    </button>
  </div>
</template>

<style scoped>
.category-filter {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.chip {
  min-height: 34px;
  padding: 0 14px;
  border: 1px solid var(--ce-color-border);
  border-radius: 999px;
  background: var(--ce-color-surface);
  color: var(--ce-color-muted);
  font-size: 13px;
  cursor: pointer;
  transition: background 0.12s ease, color 0.12s ease, border-color 0.12s ease;
}

.chip:hover:not(:disabled) {
  border-color: #d6a38d;
  color: var(--ce-color-primary);
}

.chip.active {
  border-color: var(--ce-color-primary);
  background: var(--ce-color-primary-soft);
  color: var(--ce-color-primary);
  font-weight: 600;
}

.chip:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.chip:focus-visible {
  outline: none;
  box-shadow: var(--ce-shadow-focus);
}
</style>
