import { http, requestData } from './http'
import type { PageResponse, ReviewCreateRequest, ReviewResponse } from './types'

export function createReview(
  tradeRequestId: number,
  payload: ReviewCreateRequest,
): Promise<ReviewResponse> {
  return requestData(
    http.post<ReviewResponse>(`/trade-requests/${tradeRequestId}/reviews`, payload),
  )
}

export function fetchTradeReviews(tradeRequestId: number): Promise<ReviewResponse[]> {
  return requestData(http.get<ReviewResponse[]>(`/trade-requests/${tradeRequestId}/reviews`))
}

export function fetchUserReviews(
  userId: number,
  page = 1,
  size = 20,
): Promise<PageResponse<ReviewResponse>> {
  return requestData(
    http.get<PageResponse<ReviewResponse>>(`/users/${userId}/reviews`, {
      params: { page, size },
    }),
  )
}
