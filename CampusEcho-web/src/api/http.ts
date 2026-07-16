import axios, { type AxiosError, type InternalAxiosRequestConfig } from 'axios'
import type { ApiResponse, ProblemDetail } from './types'
import { ApiError } from './types'

const TOKEN_KEY = 'campusecho_access_token'

export function getStoredToken(): string | null {
  return localStorage.getItem(TOKEN_KEY)
}

export function setStoredToken(token: string | null): void {
  if (token) {
    localStorage.setItem(TOKEN_KEY, token)
  } else {
    localStorage.removeItem(TOKEN_KEY)
  }
}

export const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? '/api/v1',
  timeout: 15_000,
})

type UnauthorizedHandler = (redirectPath?: string) => void

let onUnauthorized: UnauthorizedHandler | null = null

export function setUnauthorizedHandler(handler: UnauthorizedHandler | null): void {
  onUnauthorized = handler
}

http.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = getStoredToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

http.interceptors.response.use(
  (response) => {
    const payload = response.data as ApiResponse<unknown> | unknown
    if (
      payload &&
      typeof payload === 'object' &&
      'code' in payload &&
      'data' in payload &&
      (payload as ApiResponse<unknown>).code === 0
    ) {
      return {
        ...response,
        data: (payload as ApiResponse<unknown>).data,
      }
    }
    return response
  },
  (error: AxiosError<ProblemDetail>) => {
    const status = error.response?.status ?? 0
    const problem = error.response?.data

    if (status === 401) {
      setStoredToken(null)
      const current = window.location.pathname + window.location.search
      onUnauthorized?.(current.startsWith('/login') ? undefined : current)
    }

    if (problem && typeof problem === 'object' && ('detail' in problem || 'status' in problem)) {
      return Promise.reject(
        new ApiError({
          type: problem.type,
          title: problem.title,
          status: problem.status ?? status,
          detail: problem.detail || error.message || '请求失败',
          instance: problem.instance,
          code: problem.code,
          violations: problem.violations,
        }),
      )
    }

    return Promise.reject(
      new ApiError({
        status: status || 0,
        detail: error.message || '网络异常，请稍后重试',
        code: 'NETWORK_ERROR',
      }),
    )
  },
)

export async function requestData<T>(promise: Promise<{ data: T }>): Promise<T> {
  const response = await promise
  return response.data
}
