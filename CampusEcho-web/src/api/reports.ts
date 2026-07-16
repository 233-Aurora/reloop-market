import { http, requestData } from './http'
import type {
  ItemModerationResponse,
  ItemRestoreRequest,
  PageResponse,
  ReportCreateRequest,
  ReportReason,
  ReportRejectionRequest,
  ReportResolutionRequest,
  ReportResponse,
  ReportStatus,
  ReportTargetType,
} from './types'

export interface MyReportListQuery {
  targetType?: ReportTargetType
  status?: ReportStatus
  page?: number
  size?: number
}

export interface AdminReportListQuery {
  targetType?: ReportTargetType
  reason?: ReportReason
  status?: ReportStatus
  page?: number
  size?: number
}

export function createReport(payload: ReportCreateRequest): Promise<ReportResponse> {
  return requestData(http.post<ReportResponse>('/reports', payload))
}

export function fetchMyReports(
  query: MyReportListQuery = {},
): Promise<PageResponse<ReportResponse>> {
  return requestData(
    http.get<PageResponse<ReportResponse>>('/reports/mine', {
      params: {
        targetType: query.targetType,
        status: query.status,
        page: query.page ?? 1,
        size: query.size ?? 20,
      },
    }),
  )
}

export function fetchMyReport(reportId: number): Promise<ReportResponse> {
  return requestData(http.get<ReportResponse>(`/reports/${reportId}`))
}

export function fetchAdminReports(
  query: AdminReportListQuery = {},
): Promise<PageResponse<ReportResponse>> {
  return requestData(
    http.get<PageResponse<ReportResponse>>('/admin/reports', {
      params: {
        targetType: query.targetType,
        reason: query.reason,
        status: query.status,
        page: query.page ?? 1,
        size: query.size ?? 20,
      },
    }),
  )
}

export function fetchAdminReport(reportId: number): Promise<ReportResponse> {
  return requestData(http.get<ReportResponse>(`/admin/reports/${reportId}`))
}

export function resolveAdminReport(
  reportId: number,
  payload: ReportResolutionRequest,
): Promise<ReportResponse> {
  return requestData(
    http.post<ReportResponse>(`/admin/reports/${reportId}/resolve`, payload),
  )
}

export function rejectAdminReport(
  reportId: number,
  payload: ReportRejectionRequest,
): Promise<ReportResponse> {
  return requestData(
    http.post<ReportResponse>(`/admin/reports/${reportId}/reject`, payload),
  )
}

export function restoreBlockedItem(
  itemId: number,
  payload: ItemRestoreRequest,
): Promise<ItemModerationResponse> {
  return requestData(
    http.post<ItemModerationResponse>(`/admin/items/${itemId}/restore`, payload),
  )
}
