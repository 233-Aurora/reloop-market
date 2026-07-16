import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { fetchUnreadCount, markAllMessagesRead, markMessageRead } from '../api/messages'
import { useAuthStore } from './auth'

export const useMessageStore = defineStore('messages', () => {
  const unreadCount = ref(0)
  const loading = ref(false)
  let pollTimer: ReturnType<typeof setInterval> | null = null

  const hasUnread = computed(() => unreadCount.value > 0)
  const badgeText = computed(() => {
    if (unreadCount.value <= 0) return ''
    if (unreadCount.value > 99) return '99+'
    return String(unreadCount.value)
  })

  async function refreshUnread(): Promise<void> {
    const auth = useAuthStore()
    if (!auth.isAuthenticated) {
      unreadCount.value = 0
      return
    }

    loading.value = true
    try {
      const result = await fetchUnreadCount()
      unreadCount.value = result.unreadCount
    } catch {
      // 静默失败，避免干扰主流程
    } finally {
      loading.value = false
    }
  }

  function setUnreadCount(count: number): void {
    unreadCount.value = Math.max(0, count)
  }

  function decreaseUnread(by = 1): void {
    unreadCount.value = Math.max(0, unreadCount.value - by)
  }

  async function markRead(id: number): Promise<void> {
    const result = await markMessageRead(id)
    if (result.read) {
      // 重新拉取保证准确
      await refreshUnread()
    }
  }

  async function markAllRead(): Promise<void> {
    const result = await markAllMessagesRead()
    unreadCount.value = result.unreadCount
  }

  function startPolling(intervalMs = 60_000): void {
    stopPolling()
    void refreshUnread()
    pollTimer = setInterval(() => {
      void refreshUnread()
    }, intervalMs)
  }

  function stopPolling(): void {
    if (pollTimer) {
      clearInterval(pollTimer)
      pollTimer = null
    }
  }

  function reset(): void {
    stopPolling()
    unreadCount.value = 0
  }

  return {
    unreadCount,
    loading,
    hasUnread,
    badgeText,
    refreshUnread,
    setUnreadCount,
    decreaseUnread,
    markRead,
    markAllRead,
    startPolling,
    stopPolling,
    reset,
  }
})
