import { http, requestData } from './http'
import type {
  DisputeCreateRequest,
  DisputeListQuery,
  DisputeRejectionRequest,
  DisputeResolutionRequest,
  DisputeResponse,
  PageResponse,
} from './types'

export function createDispute(payload: DisputeCreateRequest): Promise<DisputeResponse> {
  return requestData(http.post<DisputeResponse>('/disputes', payload))
}

export function fetchMyDisputes(
  query: DisputeListQuery = {},
): Promise<PageResponse<DisputeResponse>> {
  return requestData(
    http.get<PageResponse<DisputeResponse>>('/disputes/mine', {
      params: {
        status: query.status,
        reason: query.reason,
        page: query.page ?? 1,
        size: query.size ?? 20,
      },
    }),
  )
}

export function fetchMyDispute(disputeId: number): Promise<DisputeResponse> {
  return requestData(http.get<DisputeResponse>(`/disputes/${disputeId}`))
}

export function fetchAdminDisputes(
  query: DisputeListQuery = {},
): Promise<PageResponse<DisputeResponse>> {
  return requestData(
    http.get<PageResponse<DisputeResponse>>('/admin/disputes', {
      params: {
        status: query.status,
        reason: query.reason,
        page: query.page ?? 1,
        size: query.size ?? 20,
      },
    }),
  )
}

export function fetchAdminDispute(disputeId: number): Promise<DisputeResponse> {
  return requestData(http.get<DisputeResponse>(`/admin/disputes/${disputeId}`))
}

export function resolveAdminDispute(
  disputeId: number,
  payload: DisputeResolutionRequest,
): Promise<DisputeResponse> {
  return requestData(
    http.post<DisputeResponse>(`/admin/disputes/${disputeId}/resolve`, payload),
  )
}

export function rejectAdminDispute(
  disputeId: number,
  payload: DisputeRejectionRequest,
): Promise<DisputeResponse> {
  return requestData(
    http.post<DisputeResponse>(`/admin/disputes/${disputeId}/reject`, payload),
  )
}
