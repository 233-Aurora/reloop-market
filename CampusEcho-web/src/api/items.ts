import { http, requestData } from './http'
import type {
  ItemCreateRequest,
  ItemImageResponse,
  ItemResponse,
  ItemUpdateRequest,
  PageResponse,
} from './types'

export interface ItemListQuery {
  categoryId?: number
  keyword?: string
  page?: number
  size?: number
}

export function fetchItems(query: ItemListQuery = {}): Promise<PageResponse<ItemResponse>> {
  return requestData(
    http.get<PageResponse<ItemResponse>>('/items', {
      params: {
        categoryId: query.categoryId,
        keyword: query.keyword || undefined,
        page: query.page ?? 1,
        size: query.size ?? 20,
      },
    }),
  )
}

export function fetchItem(id: number): Promise<ItemResponse> {
  return requestData(http.get<ItemResponse>(`/items/${id}`))
}

export function fetchMyItems(page = 1, size = 20): Promise<PageResponse<ItemResponse>> {
  return requestData(
    http.get<PageResponse<ItemResponse>>('/items/mine', {
      params: { page, size },
    }),
  )
}

export function createItem(payload: ItemCreateRequest): Promise<ItemResponse> {
  return requestData(http.post<ItemResponse>('/items', payload))
}

export function updateItem(id: number, payload: ItemUpdateRequest): Promise<ItemResponse> {
  return requestData(http.put<ItemResponse>(`/items/${id}`, payload))
}

export function offShelfItem(id: number): Promise<ItemResponse> {
  return requestData(http.post<ItemResponse>(`/items/${id}/off-shelf`))
}

export function publishItem(id: number): Promise<ItemResponse> {
  return requestData(http.post<ItemResponse>(`/items/${id}/publish`))
}

export function fetchItemImages(id: number): Promise<ItemImageResponse[]> {
  return requestData(http.get<ItemImageResponse[]>(`/items/${id}/images`))
}

export function uploadItemImage(
  itemId: number,
  file: File,
  primary = false,
): Promise<ItemImageResponse> {
  const form = new FormData()
  form.append('file', file)
  return requestData(
    http.post<ItemImageResponse>(`/items/${itemId}/images`, form, {
      params: { primary },
    }),
  )
}

export function setPrimaryImage(itemId: number, imageId: number): Promise<ItemImageResponse> {
  return requestData(http.post<ItemImageResponse>(`/items/${itemId}/images/${imageId}/primary`))
}

export function deleteItemImage(itemId: number, imageId: number): Promise<void> {
  return requestData(http.delete<void>(`/items/${itemId}/images/${imageId}`))
}
