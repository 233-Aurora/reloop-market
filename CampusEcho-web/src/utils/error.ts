import { ApiError } from '../api/types'

export function getErrorMessage(error: unknown, fallback = '操作失败，请稍后重试'): string {
  if (error instanceof ApiError) {
    return error.detail || fallback
  }
  if (error instanceof Error && error.message) {
    return error.message
  }
  return fallback
}

export function getFieldErrors(error: unknown): Record<string, string> {
  if (error instanceof ApiError && error.violations) {
    return { ...error.violations }
  }
  return {}
}
