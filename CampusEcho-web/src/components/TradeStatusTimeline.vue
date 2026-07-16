<script setup lang="ts">
import { computed } from 'vue'
import type { TradeRequestResponse } from '../api/types'
import { formatDateTime, tradeStatusLabel } from '../utils/format'

const props = defineProps<{
  trade: TradeRequestResponse
}>()

interface Step {
  key: string
  title: string
  description: string
  state: 'done' | 'current' | 'todo' | 'failed'
}

const steps = computed<Step[]>(() => {
  const trade = props.trade
  const status = trade.status

  const created: Step = {
    key: 'created',
    title: '发起请求',
    description: formatDateTime(trade.createdAt),
    state: 'done',
  }

  if (status === 'PENDING') {
    return [
      created,
      {
        key: 'seller',
        title: '等待卖家处理',
        description: '卖家可接受或拒绝',
        state: 'current',
      },
      {
        key: 'confirm',
        title: '双方确认完成',
        description: '接受后由买卖双方确认',
        state: 'todo',
      },
      {
        key: 'done',
        title: '交易完成',
        description: '物品将标记为已售出',
        state: 'todo',
      },
    ]
  }

  if (status === 'REJECTED') {
    return [
      created,
      {
        key: 'seller',
        title: '卖家已拒绝',
        description: formatDateTime(trade.updatedAt),
        state: 'failed',
      },
    ]
  }

  if (status === 'CANCELLED') {
    return [
      created,
      {
        key: 'cancel',
        title: '买家已取消',
        description: formatDateTime(trade.updatedAt),
        state: 'failed',
      },
    ]
  }

  if (status === 'ACCEPTED') {
    return [
      created,
      {
        key: 'seller',
        title: '卖家已接受',
        description: '物品已预订',
        state: 'done',
      },
      {
        key: 'confirm',
        title: '双方确认完成',
        description: `买家 ${trade.requesterConfirmed ? '已确认' : '未确认'} · 卖家 ${trade.sellerConfirmed ? '已确认' : '未确认'}`,
        state: 'current',
      },
      {
        key: 'done',
        title: '交易完成',
        description: '双方确认后自动完成',
        state: 'todo',
      },
    ]
  }

  if (status === 'DISPUTED') {
    return [
      created,
      {
        key: 'seller',
        title: '卖家已接受',
        description: '物品已预订',
        state: 'done',
      },
      {
        key: 'dispute',
        title: '争议仲裁中',
        description: '确认完成已冻结，等待管理员裁定',
        state: 'current',
      },
      {
        key: 'done',
        title: '等待裁定结果',
        description: '可完成交易或取消交易',
        state: 'todo',
      },
    ]
  }

  return [
    created,
    {
      key: 'seller',
      title: '卖家已接受',
      description: '物品曾被预订',
      state: 'done',
    },
    {
      key: 'confirm',
      title: '双方已确认',
      description: '买家与卖家均已确认',
      state: 'done',
    },
    {
      key: 'done',
      title: tradeStatusLabel(status),
      description: formatDateTime(trade.updatedAt),
      state: status === 'COMPLETED' ? 'done' : 'failed',
    },
  ]
})
</script>

<template>
  <ol class="timeline" aria-label="交易状态时间线">
    <li v-for="step in steps" :key="step.key" class="step" :class="`is-${step.state}`">
      <span class="dot" aria-hidden="true" />
      <div class="content">
        <strong>{{ step.title }}</strong>
        <p>{{ step.description }}</p>
      </div>
    </li>
  </ol>
</template>

<style scoped>
.timeline {
  margin: 0;
  padding: 0;
  list-style: none;
  display: grid;
  gap: 0;
}

.step {
  position: relative;
  display: grid;
  grid-template-columns: 20px 1fr;
  gap: 12px;
  padding-bottom: 16px;
}

.step:not(:last-child)::before {
  content: '';
  position: absolute;
  left: 9px;
  top: 18px;
  bottom: 0;
  width: 2px;
  background: var(--ce-color-border);
}

.dot {
  width: 12px;
  height: 12px;
  margin-top: 4px;
  border-radius: 50%;
  background: #c5cdd5;
  border: 2px solid #fff;
  box-shadow: 0 0 0 1px #c5cdd5;
  justify-self: center;
}

.is-done .dot {
  background: var(--ce-color-primary);
  box-shadow: 0 0 0 1px var(--ce-color-primary);
}

.is-current .dot {
  background: #fff;
  box-shadow: 0 0 0 2px var(--ce-color-primary);
}

.is-failed .dot {
  background: var(--ce-color-danger);
  box-shadow: 0 0 0 1px var(--ce-color-danger);
}

.content strong {
  display: block;
  margin-bottom: 4px;
  font-size: 14px;
}

.content p {
  margin: 0;
  color: var(--ce-color-muted);
  font-size: 13px;
  line-height: 1.5;
}

.is-failed .content strong {
  color: var(--ce-color-danger);
}
</style>
