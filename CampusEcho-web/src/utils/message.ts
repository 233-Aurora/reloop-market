import type { RouteLocationRaw } from 'vue-router'
import type { MessageResponse, MessageType } from '../api/types'

const typeLabels: Record<MessageType, string> = {
  TRADE_REQUEST_CREATED: '新的交易请求',
  TRADE_REQUEST_ACCEPTED: '交易已接受',
  TRADE_REQUEST_REJECTED: '交易已拒绝',
  TRADE_REQUEST_CANCELLED: '交易已取消',
  TRADE_CONFIRMATION_RECEIVED: '对方已确认完成',
  TRADE_COMPLETED: '交易已完成',
  TRADE_CANCELLED_BY_MODERATION: '交易因治理取消',
  REPORT_RESOLVED: '举报已处理',
  REPORT_REJECTED: '举报已驳回',
  ITEM_BLOCKED: '物品被屏蔽',
  ITEM_RESTORED: '物品已恢复',
  ACCOUNT_SUSPENDED: '账户已停用',
  DISPUTE_OPENED: '交易争议已发起',
  DISPUTE_RESOLVED: '争议已裁定',
  DISPUTE_REJECTED: '争议已驳回',
}

export type MessageFilterGroup = 'ALL' | 'TRADE' | 'REPORT' | 'MODERATION' | 'DISPUTE'

const tradeTypes: MessageType[] = [
  'TRADE_REQUEST_CREATED',
  'TRADE_REQUEST_ACCEPTED',
  'TRADE_REQUEST_REJECTED',
  'TRADE_REQUEST_CANCELLED',
  'TRADE_CONFIRMATION_RECEIVED',
  'TRADE_COMPLETED',
  'TRADE_CANCELLED_BY_MODERATION',
]

const reportTypes: MessageType[] = ['REPORT_RESOLVED', 'REPORT_REJECTED']

const moderationTypes: MessageType[] = ['ITEM_BLOCKED', 'ITEM_RESTORED', 'ACCOUNT_SUSPENDED']

const disputeTypes: MessageType[] = ['DISPUTE_OPENED', 'DISPUTE_RESOLVED', 'DISPUTE_REJECTED']

export function messageTypeLabel(type: MessageType): string {
  return typeLabels[type] ?? type
}

export function messageGroupOptions(): { label: string; value: MessageFilterGroup }[] {
  return [
    { label: '全部类型', value: 'ALL' },
    { label: '交易相关', value: 'TRADE' },
    { label: '举报相关', value: 'REPORT' },
    { label: '治理相关', value: 'MODERATION' },
    { label: '争议相关', value: 'DISPUTE' },
  ]
}

export function typesInGroup(group: MessageFilterGroup): MessageType[] | null {
  if (group === 'ALL') return null
  if (group === 'TRADE') return tradeTypes
  if (group === 'REPORT') return reportTypes
  if (group === 'MODERATION') return moderationTypes
  return disputeTypes
}

export function matchesMessageGroup(
  message: MessageResponse,
  group: MessageFilterGroup,
): boolean {
  const types = typesInGroup(group)
  if (!types) return true
  return types.includes(message.messageType)
}

export function messageLink(message: MessageResponse): RouteLocationRaw | null {
  if (message.disputeId) {
    return { name: 'my-dispute-detail', params: { id: message.disputeId } }
  }
  if (message.tradeRequestId) {
    return { name: 'trade-detail', params: { id: message.tradeRequestId } }
  }
  if (message.reportId) {
    return { name: 'my-report-detail', params: { id: message.reportId } }
  }
  if (message.itemId) {
    return { name: 'item-detail', params: { id: message.itemId } }
  }
  return null
}

export function messageLinkLabel(message: MessageResponse): string | null {
  if (message.disputeId) return '查看争议'
  if (message.tradeRequestId) return '查看交易'
  if (message.reportId) return '查看举报'
  if (message.itemId) return '查看物品'
  return null
}
