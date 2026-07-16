<script setup lang="ts">
import type { TradeRequestResponse } from '../api/types'
import { formatDateTime, formatRelativeTime } from '../utils/format'
import { roleLabel } from '../utils/trade'
import TradeStatusTag from './TradeStatusTag.vue'

defineProps<{
  items: TradeRequestResponse[]
  userId: number
}>()
</script>

<template>
  <div class="trade-list" aria-label="交易请求列表">
    <RouterLink
      v-for="trade in items"
      :key="trade.id"
      class="trade-card"
      :to="{ name: 'trade-detail', params: { id: trade.id } }"
    >
      <div class="top">
        <h3 :title="trade.itemTitle || `物品 #${trade.itemId}`">
          {{ trade.itemTitle || `物品 #${trade.itemId}` }}
        </h3>
        <TradeStatusTag :status="trade.status" />
      </div>

      <div class="meta">
        <span>{{ roleLabel(trade, userId) }}</span>
        <span class="dot" aria-hidden="true">·</span>
        <time :datetime="trade.createdAt" :title="formatDateTime(trade.createdAt)">
          {{ formatRelativeTime(trade.createdAt) }}
        </time>
      </div>

      <p v-if="trade.message" class="message">{{ trade.message }}</p>
      <p v-else class="message muted">无留言</p>

      <div class="footer">
        <span>请求 #{{ trade.id }}</span>
        <span v-if="trade.status === 'ACCEPTED'">
          确认进度：买家 {{ trade.requesterConfirmed ? '✓' : '…' }} / 卖家
          {{ trade.sellerConfirmed ? '✓' : '…' }}
        </span>
      </div>
    </RouterLink>
  </div>
</template>

<style scoped>
.trade-list {
  display: grid;
  gap: 12px;
}

.trade-card {
  display: grid;
  gap: 8px;
  padding: 16px;
  background: var(--ce-color-surface);
  border: 1px solid var(--ce-color-border);
  border-radius: var(--ce-radius);
  color: inherit;
  transition: border-color 0.12s ease, box-shadow 0.12s ease;
}

.trade-card:hover {
  border-color: #b7dad4;
  box-shadow: 0 2px 8px rgba(21, 108, 94, 0.08);
}

.trade-card:focus-visible {
  outline: none;
  box-shadow: var(--ce-shadow-focus);
}

.top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  min-width: 0;
}

.meta,
.footer {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
  color: var(--ce-color-muted);
  font-size: 12px;
}

.message {
  margin: 0;
  font-size: 14px;
  line-height: 1.55;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.message.muted {
  color: var(--ce-color-muted);
}

.footer {
  justify-content: space-between;
  padding-top: 4px;
  border-top: 1px solid #eef1f4;
}

.dot {
  flex-shrink: 0;
}
</style>
