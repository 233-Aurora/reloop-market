import type {
  ReportAction,
  ReportReason,
  ReportStatus,
  ReportTargetType,
} from '../api/types'

export const ITEM_REPORT_REASONS: { value: ReportReason; label: string }[] = [
  { value: 'FRAUD', label: '疑似诈骗' },
  { value: 'PROHIBITED_ITEM', label: '违禁物品' },
  { value: 'MISLEADING_INFORMATION', label: '描述失实' },
  { value: 'OTHER', label: '其他' },
]

export const USER_REPORT_REASONS: { value: ReportReason; label: string }[] = [
  { value: 'FRAUD', label: '疑似诈骗' },
  { value: 'HARASSMENT', label: '骚扰辱骂' },
  { value: 'IMPERSONATION', label: '冒充他人' },
  { value: 'OTHER', label: '其他' },
]

const reasonLabels: Record<ReportReason, string> = {
  FRAUD: '疑似诈骗',
  PROHIBITED_ITEM: '违禁物品',
  MISLEADING_INFORMATION: '描述失实',
  HARASSMENT: '骚扰辱骂',
  IMPERSONATION: '冒充他人',
  OTHER: '其他',
}

const statusLabels: Record<ReportStatus, string> = {
  PENDING: '待处理',
  RESOLVED: '已处理',
  REJECTED: '已驳回',
}

const actionLabels: Record<ReportAction, string> = {
  NONE: '仅记录，不处罚',
  BLOCK_ITEM: '屏蔽物品',
  SUSPEND_USER: '停用用户',
  BLOCK_ITEM_AND_SUSPEND_USER: '屏蔽物品并停用用户',
}

const targetLabels: Record<ReportTargetType, string> = {
  ITEM: '物品',
  USER: '用户',
}

export function reportReasonLabel(reason: ReportReason): string {
  return reasonLabels[reason] ?? reason
}

export function reportStatusLabel(status: ReportStatus): string {
  return statusLabels[status] ?? status
}

export function reportActionLabel(action: ReportAction | null | undefined): string {
  if (!action) return '—'
  return actionLabels[action] ?? action
}

export function reportTargetLabel(type: ReportTargetType): string {
  return targetLabels[type] ?? type
}

export function reasonsForTarget(type: ReportTargetType) {
  return type === 'ITEM' ? ITEM_REPORT_REASONS : USER_REPORT_REASONS
}

export function actionsForTarget(type: ReportTargetType): { value: ReportAction; label: string }[] {
  if (type === 'USER') {
    return [
      { value: 'NONE', label: actionLabels.NONE },
      { value: 'SUSPEND_USER', label: actionLabels.SUSPEND_USER },
    ]
  }
  return [
    { value: 'NONE', label: actionLabels.NONE },
    { value: 'BLOCK_ITEM', label: actionLabels.BLOCK_ITEM },
    { value: 'SUSPEND_USER', label: actionLabels.SUSPEND_USER },
    { value: 'BLOCK_ITEM_AND_SUSPEND_USER', label: actionLabels.BLOCK_ITEM_AND_SUSPEND_USER },
  ]
}
