import type {
  ItemCondition,
  ItemModerationStatus,
  ItemStatus,
  TradeStatus,
} from '../api/types'

const conditionLabels: Record<ItemCondition, string> = {
  NEW: '全新',
  LIKE_NEW: '几乎全新',
  GOOD: '良好',
  FAIR: '一般',
}

const itemStatusLabels: Record<ItemStatus, string> = {
  DRAFT: '草稿',
  ON_SALE: '在售',
  RESERVED: '已预订',
  SOLD: '已售出',
  OFF_SHELF: '已下架',
}

const tradeStatusLabels: Record<TradeStatus, string> = {
  PENDING: '待处理',
  ACCEPTED: '已接受',
  REJECTED: '已拒绝',
  CANCELLED: '已取消',
  COMPLETED: '已完成',
  DISPUTED: '争议中',
}

export function formatPrice(value: number | string | null | undefined): string {
  if (value === null || value === undefined || value === '') {
    return '¥0.00'
  }
  const num = typeof value === 'number' ? value : Number(value)
  if (Number.isNaN(num)) {
    return `¥${value}`
  }
  return `¥${num.toFixed(2)}`
}

export function formatDateTime(value: string | null | undefined): string {
  if (!value) {
    return '—'
  }
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value
  }
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  }).format(date)
}

export function formatRelativeTime(value: string | null | undefined): string {
  if (!value) {
    return '—'
  }
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value
  }
  const diffMs = Date.now() - date.getTime()
  const minutes = Math.floor(diffMs / 60_000)
  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes} 分钟前`
  const hours = Math.floor(minutes / 60)
  if (hours < 24) return `${hours} 小时前`
  const days = Math.floor(hours / 24)
  if (days < 30) return `${days} 天前`
  return formatDateTime(value)
}

export function conditionLabel(value: ItemCondition): string {
  return conditionLabels[value] ?? value
}

export function itemStatusLabel(value: ItemStatus): string {
  return itemStatusLabels[value] ?? value
}

export function moderationStatusLabel(value: ItemModerationStatus): string {
  return value === 'BLOCKED' ? '已屏蔽' : '正常'
}

export function tradeStatusLabel(value: TradeStatus): string {
  return tradeStatusLabels[value] ?? value
}

export function resolveAssetUrl(url: string | null | undefined): string | null {
  if (!url) {
    return null
  }
  if (url.startsWith('http://') || url.startsWith('https://') || url.startsWith('data:')) {
    return url
  }
  return url
}
