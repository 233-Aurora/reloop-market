import { http, requestData } from './http'
import type { PageResponse, TradeRequestCreateRequest, TradeRequestResponse } from './types'

export function createTradeRequest(
  payload: TradeRequestCreateRequest,
): Promise<TradeRequestResponse> {
  return requestData(http.post<TradeRequestResponse>('/trade-requests', payload))
}

export function fetchTradeRequests(
  page = 1,
  size = 20,
): Promise<PageResponse<TradeRequestResponse>> {
  return requestData(
    http.get<PageResponse<TradeRequestResponse>>('/trade-requests', {
      params: { page, size },
    }),
  )
}

export function fetchTradeRequest(id: number): Promise<TradeRequestResponse> {
  return requestData(http.get<TradeRequestResponse>(`/trade-requests/${id}`))
}

export function acceptTradeRequest(id: number): Promise<TradeRequestResponse> {
  return requestData(http.post<TradeRequestResponse>(`/trade-requests/${id}/accept`))
}

export function rejectTradeRequest(id: number): Promise<TradeRequestResponse> {
  return requestData(http.post<TradeRequestResponse>(`/trade-requests/${id}/reject`))
}

export function cancelTradeRequest(id: number): Promise<TradeRequestResponse> {
  return requestData(http.post<TradeRequestResponse>(`/trade-requests/${id}/cancel`))
}

export function confirmTradeCompletion(id: number): Promise<TradeRequestResponse> {
  return requestData(http.post<TradeRequestResponse>(`/trade-requests/${id}/confirm-completion`))
}
