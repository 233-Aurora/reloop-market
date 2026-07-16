import { http, requestData } from './http'
import type { HealthStatus } from './types'

export function fetchHealth(): Promise<HealthStatus> {
  return requestData(http.get<HealthStatus>('/health'))
}
