<script setup lang="ts">
import { computed } from 'vue'
import type { ItemStatus } from '../api/types'
import { itemStatusLabel } from '../utils/format'

const props = defineProps<{
  status: ItemStatus
}>()

const label = computed(() => itemStatusLabel(props.status))
const tone = computed(() => {
  switch (props.status) {
    case 'ON_SALE':
      return 'success'
    case 'RESERVED':
      return 'warning'
    case 'SOLD':
      return 'muted'
    case 'OFF_SHELF':
      return 'muted'
    default:
      return 'default'
  }
})
</script>

<template>
  <span class="status-tag" :class="`tone-${tone}`">{{ label }}</span>
</template>

<style scoped>
.status-tag {
  display: inline-flex;
  align-items: center;
  min-height: 22px;
  padding: 0 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 600;
  line-height: 1;
  white-space: nowrap;
}

.tone-success {
  color: #0f574c;
  background: #e6f2ef;
}

.tone-warning {
  color: #93370d;
  background: #fef0c7;
}

.tone-muted {
  color: #4b5563;
  background: #eef1f4;
}

.tone-default {
  color: #344054;
  background: #f2f4f7;
}
</style>
