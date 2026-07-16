import type {
  DisputeReason,
  DisputeResolution,
  DisputeStatus,
} from '../api/types'

export const DISPUTE_REASONS: { value: DisputeReason; label: string }[] = [
  { value: 'NON_DELIVERY', label: '未交付/未收到' },
  { value: 'ITEM_NOT_AS_DESCRIBED', label: '货不对板' },
  { value: 'PAYMENT_ISSUE', label: '付款/交接纠纷' },
  { value: 'NO_SHOW', label: '约定爽约' },
  { value: 'OTHER', label: '其他' },
]

export const DISPUTE_RESOLUTIONS: {
  value: Exclude<DisputeResolution, 'NONE'>
  label: string
  description: string
}[] = [
  {
    value: 'COMPLETE_TRADE',
    label: '裁定交易完成',
    description: '交易变为已完成，物品标记为已售出，双方可评价。',
  },
  {
    value: 'CANCEL_TRADE',
    label: '裁定取消交易',
    description: '交易取消；卖家活跃且物品正常时恢复在售。',
  },
]

const reasonLabels: Record<DisputeReason, string> = {
  NON_DELIVERY: '未交付/未收到',
  ITEM_NOT_AS_DESCRIBED: '货不对板',
  PAYMENT_ISSUE: '付款/交接纠纷',
  NO_SHOW: '约定爽约',
  OTHER: '其他',
}

const statusLabels: Record<DisputeStatus, string> = {
  OPEN: '待仲裁',
  RESOLVED: '已裁定',
  REJECTED: '已驳回',
}

const resolutionLabels: Record<DisputeResolution, string> = {
  COMPLETE_TRADE: '裁定交易完成',
  CANCEL_TRADE: '裁定取消交易',
  NONE: '无裁定（驳回）',
}

export function disputeReasonLabel(reason: DisputeReason): string {
  return reasonLabels[reason] ?? reason
}

export function disputeStatusLabel(status: DisputeStatus): string {
  return statusLabels[status] ?? status
}

export function disputeResolutionLabel(
  resolution: DisputeResolution | null | undefined,
): string {
  if (!resolution) return '—'
  return resolutionLabels[resolution] ?? resolution
}
