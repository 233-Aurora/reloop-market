import { http, requestData } from './http'
import type {
  MessageListQuery,
  MessageResponse,
  PageResponse,
  UnreadCountResponse,
} from './types'

export function fetchMessages(
  query: MessageListQuery = {},
): Promise<PageResponse<MessageResponse>> {
  return requestData(
    http.get<PageResponse<MessageResponse>>('/messages', {
      params: {
        unreadOnly: query.unreadOnly,
        messageType: query.messageType,
        page: query.page ?? 1,
        size: query.size ?? 20,
      },
    }),
  )
}

export function fetchUnreadCount(): Promise<UnreadCountResponse> {
  return requestData(http.get<UnreadCountResponse>('/messages/unread-count'))
}

export function fetchMessage(id: number): Promise<MessageResponse> {
  return requestData(http.get<MessageResponse>(`/messages/${id}`))
}

export function markMessageRead(id: number): Promise<MessageResponse> {
  return requestData(http.post<MessageResponse>(`/messages/${id}/read`))
}

export function markAllMessagesRead(): Promise<UnreadCountResponse> {
  return requestData(http.post<UnreadCountResponse>('/messages/read-all'))
}
