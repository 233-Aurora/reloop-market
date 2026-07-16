import { http, requestData } from './http'
import type {
  AdminDashboardResponse,
  AdminUserListQuery,
  AdminUserResponse,
  PageResponse,
  UserRole,
  UserStatus,
} from './types'

export function fetchAdminDashboard(): Promise<AdminDashboardResponse> {
  return requestData(http.get<AdminDashboardResponse>('/admin/dashboard'))
}

export function fetchAdminUsers(
  query: AdminUserListQuery = {},
): Promise<PageResponse<AdminUserResponse>> {
  return requestData(
    http.get<PageResponse<AdminUserResponse>>('/admin/users', {
      params: {
        keyword: query.keyword || undefined,
        role: query.role,
        status: query.status,
        page: query.page ?? 1,
        size: query.size ?? 20,
      },
    }),
  )
}

export function fetchAdminUser(userId: number): Promise<AdminUserResponse> {
  return requestData(http.get<AdminUserResponse>(`/admin/users/${userId}`))
}

export function updateAdminUserStatus(
  userId: number,
  status: UserStatus,
): Promise<AdminUserResponse> {
  return requestData(
    http.patch<AdminUserResponse>(`/admin/users/${userId}/status`, { status }),
  )
}

export function updateAdminUserRole(
  userId: number,
  role: UserRole,
): Promise<AdminUserResponse> {
  return requestData(http.patch<AdminUserResponse>(`/admin/users/${userId}/role`, { role }))
}
