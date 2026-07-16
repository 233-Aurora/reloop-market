import type {
  ItemCondition,
  ItemModerationStatus,
  ItemResponse,
  ItemStatus,
} from '../api/types'
import { fetchItem, fetchMyItems } from '../api/items'
import { ApiError } from '../api/types'

export const ITEM_CONDITIONS: { value: ItemCondition; label: string }[] = [
  { value: 'NEW', label: '全新' },
  { value: 'LIKE_NEW', label: '几乎全新' },
  { value: 'GOOD', label: '良好' },
  { value: 'FAIR', label: '一般' },
]

export const IMAGE_ACCEPT = 'image/jpeg,image/png,image/webp'
export const IMAGE_MAX_BYTES = 5 * 1024 * 1024
export const IMAGE_MAX_COUNT = 5

export function canEditItemContent(
  status: ItemStatus,
  moderationStatus: ItemModerationStatus = 'NORMAL',
): boolean {
  if (moderationStatus === 'BLOCKED') return false
  return status !== 'SOLD' && status !== 'RESERVED'
}

export function canManageImages(
  status: ItemStatus,
  moderationStatus: ItemModerationStatus = 'NORMAL',
): boolean {
  return canEditItemContent(status, moderationStatus)
}

export function canOffShelf(
  status: ItemStatus,
  moderationStatus: ItemModerationStatus = 'NORMAL',
): boolean {
  return moderationStatus === 'NORMAL' && status === 'ON_SALE'
}

export function canPublish(
  status: ItemStatus,
  moderationStatus: ItemModerationStatus = 'NORMAL',
): boolean {
  return moderationStatus === 'NORMAL' && (status === 'OFF_SHELF' || status === 'DRAFT')
}

export function isItemBlocked(
  moderationStatus: ItemModerationStatus | null | undefined,
): boolean {
  return moderationStatus === 'BLOCKED'
}

export function validateImageFile(file: File): string | null {
  const allowed = ['image/jpeg', 'image/png', 'image/webp']
  if (!allowed.includes(file.type)) {
    return '仅支持 JPEG、PNG、WebP 格式'
  }
  if (file.size > IMAGE_MAX_BYTES) {
    return '单张图片不能超过 5 MiB'
  }
  return null
}

/** 物主加载物品：公开在售优先，否则从我的物品中查找（支持已下架等） */
export async function fetchOwnedItem(
  id: number,
  ownerId: number,
): Promise<ItemResponse> {
  try {
    const item = await fetchItem(id)
    if (item.sellerId !== ownerId) {
      throw new ApiError({
        status: 403,
        detail: '无权编辑该物品',
        code: 'FORBIDDEN',
      })
    }
    return item
  } catch (error) {
    if (!(error instanceof ApiError) || error.status !== 404) {
      throw error
    }
  }

  let page = 1
  let totalPages = 1
  while (page <= totalPages) {
    const result = await fetchMyItems(page, 50)
    totalPages = Math.max(result.totalPages, 1)
    const found = result.content.find((item) => item.id === id)
    if (found) {
      return found
    }
    page += 1
  }

  throw new ApiError({
    status: 404,
    detail: '物品不存在或无权操作',
    code: 'RESOURCE_NOT_FOUND',
  })
}
