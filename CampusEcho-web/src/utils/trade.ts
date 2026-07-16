import type { TradeRequestResponse, TradeStatus } from '../api/types'

export type TradeRoleFilter = 'ALL' | 'BUYER' | 'SELLER'

export function isBuyer(trade: TradeRequestResponse, userId: number): boolean {
  return trade.requesterId === userId
}

export function isSeller(trade: TradeRequestResponse, userId: number): boolean {
  return trade.sellerId === userId
}

export function canAccept(trade: TradeRequestResponse, userId: number): boolean {
  return isSeller(trade, userId) && trade.status === 'PENDING'
}

export function canReject(trade: TradeRequestResponse, userId: number): boolean {
  return isSeller(trade, userId) && trade.status === 'PENDING'
}

export function canCancel(trade: TradeRequestResponse, userId: number): boolean {
  return isBuyer(trade, userId) && trade.status === 'PENDING'
}

export function canConfirmCompletion(trade: TradeRequestResponse, userId: number): boolean {
  if (trade.status !== 'ACCEPTED') return false
  if (isBuyer(trade, userId)) return !trade.requesterConfirmed
  if (isSeller(trade, userId)) return !trade.sellerConfirmed
  return false
}

export function canOpenDispute(trade: TradeRequestResponse, userId: number): boolean {
  if (trade.status !== 'ACCEPTED') return false
  return isBuyer(trade, userId) || isSeller(trade, userId)
}

export function canReview(
  trade: TradeRequestResponse,
  userId: number,
  alreadyReviewed: boolean,
): boolean {
  if (trade.status !== 'COMPLETED') return false
  if (!isBuyer(trade, userId) && !isSeller(trade, userId)) return false
  return !alreadyReviewed
}

export function roleLabel(trade: TradeRequestResponse, userId: number): string {
  if (isBuyer(trade, userId)) return '我是买家'
  if (isSeller(trade, userId)) return '我是卖家'
  return '参与者'
}

export function matchRoleFilter(
  trade: TradeRequestResponse,
  userId: number,
  filter: TradeRoleFilter,
): boolean {
  if (filter === 'ALL') return true
  if (filter === 'BUYER') return isBuyer(trade, userId)
  return isSeller(trade, userId)
}

export function matchStatusFilter(
  trade: TradeRequestResponse,
  status: 'ALL' | TradeStatus,
): boolean {
  if (status === 'ALL') return true
  return trade.status === status
}
