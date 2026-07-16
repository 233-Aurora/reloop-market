import { http, requestData } from './http'
import type {
  CategoryAdminResponse,
  CategoryTreeResponse,
  CategoryWriteRequest,
} from './types'

export function fetchCategoryTree(): Promise<CategoryTreeResponse[]> {
  return requestData(http.get<CategoryTreeResponse[]>('/categories/tree'))
}

export function createCategory(payload: CategoryWriteRequest): Promise<CategoryAdminResponse> {
  return requestData(http.post<CategoryAdminResponse>('/categories', payload))
}

export function updateCategory(
  id: number,
  payload: CategoryWriteRequest,
): Promise<CategoryAdminResponse> {
  return requestData(http.put<CategoryAdminResponse>(`/categories/${id}`, payload))
}

export function deleteCategory(id: number): Promise<void> {
  return requestData(http.delete<void>(`/categories/${id}`))
}
