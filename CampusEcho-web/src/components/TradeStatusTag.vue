<script setup lang="ts">
import { computed } from 'vue'
import type { TradeStatus } from '../api/types'
import { tradeStatusLabel } from '../utils/format'

const props = defineProps<{
  status: TradeStatus
}>()

const label = computed(() => tradeStatusLabel(props.status))
const tone = computed(() => {
  switch (props.status) {
    case 'PENDING':
      return 'warning'
    case 'ACCEPTED':
      return 'info'
    case 'DISPUTED':
      return 'danger'
    case 'COMPLETED':
      return 'success'
    case 'REJECTED':
    case 'CANCELLED':
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

.tone-info {
  color: #175cd3;
  background: #eff8ff;
}

.tone-danger {
  color: #b42318;
  background: #fee4e2;
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
