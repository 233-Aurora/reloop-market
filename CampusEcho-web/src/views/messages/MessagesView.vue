<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { Bell, RefreshRight } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { fetchMessages } from '../../api/messages'
import type { MessageResponse } from '../../api/types'
import { useMessageStore } from '../../stores/messages'
import { getErrorMessage } from '../../utils/error'
import { formatDateTime, formatRelativeTime } from '../../utils/format'
import {
  matchesMessageGroup,
  messageGroupOptions,
  messageLink,
  messageLinkLabel,
  messageTypeLabel,
  type MessageFilterGroup,
} from '../../utils/message'
import EmptyState from '../../components/EmptyState.vue'
import ProblemAlert from '../../components/ProblemAlert.vue'

const PAGE_SIZE = 20
const router = useRouter()
const messageStore = useMessageStore()

const unreadOnly = ref(false)
const group = ref<MessageFilterGroup>('ALL')
const page = ref(1)
const items = ref<MessageResponse[]>([])
const totalElements = ref(0)
const totalPages = ref(0)
const loading = ref(true)
const errorMessage = ref('')
const actingId = ref<number | null>(null)
const markingAll = ref(false)

const groupOptions = messageGroupOptions()

const filteredItems = computed(() =>
  items.value.filter((item) => matchesMessageGroup(item, group.value)),
)

const summary = computed(() => {
  if (loading.value) return '加载中…'
  if (errorMessage.value) return '加载失败'
  const unreadHint =
    messageStore.unreadCount > 0 ? ` · 未读 ${messageStore.unreadCount}` : ''
  if (group.value === 'ALL') {
    return `共 ${totalElements.value} 条消息${unreadHint}`
  }
  return `当前筛选 ${filteredItems.value.length} 条（本页 ${items.value.length} 条）${unreadHint}`
})

async function loadMessages() {
  loading.value = true
  errorMessage.value = ''
  try {
    const result = await fetchMessages({
      unreadOnly: unreadOnly.value || undefined,
      page: page.value,
      size: PAGE_SIZE,
    })
    items.value = result.content
    totalElements.value = result.totalElements
    totalPages.value = result.totalPages
    if (result.totalPages > 0 && page.value > result.totalPages) {
      page.value = result.totalPages
    }
    await messageStore.refreshUnread()
  } catch (error) {
    items.value = []
    totalElements.value = 0
    totalPages.value = 0
    errorMessage.value = getErrorMessage(error, '无法加载消息')
  } finally {
    loading.value = false
  }
}

async function openMessage(message: MessageResponse) {
  if (!message.read) {
    actingId.value = message.id
    try {
      await messageStore.markRead(message.id)
      items.value = items.value.map((row) =>
        row.id === message.id ? { ...row, read: true } : row,
      )
    } catch (error) {
      ElMessage.error(getErrorMessage(error, '标记已读失败'))
    } finally {
      actingId.value = null
    }
  }

  const link = messageLink(message)
  if (link) {
    await router.push(link)
  }
}

async function handleMarkAll() {
  if (markingAll.value || messageStore.unreadCount === 0) return
  markingAll.value = true
  try {
    await messageStore.markAllRead()
    items.value = items.value.map((row) => ({ ...row, read: true }))
    ElMessage.success('已全部标为已读')
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '操作失败'))
  } finally {
    markingAll.value = false
  }
}

function handlePageChange(next: number) {
  page.value = next
  void loadMessages()
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

watch(unreadOnly, () => {
  page.value = 1
  void loadMessages()
})

onMounted(() => {
  void loadMessages()
})
</script>

<template>
  <section class="ce-page">
    <header class="ce-page-header">
      <div>
        <h1>消息中心</h1>
        <p class="ce-page-desc">
          接收交易状态变更、举报处理结果与内容治理相关的系统通知。
        </p>
      </div>
      <div class="header-actions">
        <el-button
          :disabled="messageStore.unreadCount === 0"
          :loading="markingAll"
          @click="handleMarkAll"
        >
          全部已读
        </el-button>
        <el-button :icon="RefreshRight" :loading="loading" @click="loadMessages">
          刷新
        </el-button>
      </div>
    </header>

    <div class="filters">
      <el-segmented
        v-model="unreadOnly"
        :options="[
          { label: '全部', value: false },
          { label: '仅未读', value: true },
        ]"
      />
      <el-segmented v-model="group" :options="groupOptions" />
    </div>

    <div class="ce-result-bar">
      <p>{{ summary }}</p>
    </div>

    <ProblemAlert
      v-if="errorMessage"
      title="加载失败"
      :message="errorMessage"
      @close="errorMessage = ''"
    />

    <div v-if="loading" class="ce-panel loading-box">加载消息…</div>

    <EmptyState
      v-else-if="items.length === 0 && !errorMessage"
      :title="unreadOnly ? '没有未读消息' : '暂无消息'"
      description="交易推进、举报处理或物品治理变化时，会在这里收到通知。"
    >
      <el-button type="primary" @click="router.push({ name: 'home' })">去逛逛市场</el-button>
    </EmptyState>

    <EmptyState
      v-else-if="filteredItems.length === 0"
      title="当前筛选下没有消息"
      description="试试切换类型或查看全部。"
    >
      <el-button @click="group = 'ALL'">查看全部类型</el-button>
    </EmptyState>

    <div v-else class="message-list">
      <article
        v-for="message in filteredItems"
        :key="message.id"
        class="message-card"
        :class="{ unread: !message.read }"
      >
        <button
          type="button"
          class="message-main"
          :disabled="actingId === message.id"
          @click="openMessage(message)"
        >
          <span class="icon-wrap" aria-hidden="true">
            <el-icon :size="18"><Bell /></el-icon>
            <span v-if="!message.read" class="dot" />
          </span>
          <div class="body">
            <div class="top">
              <strong>{{ messageTypeLabel(message.messageType) }}</strong>
              <time :datetime="message.createdAt" :title="formatDateTime(message.createdAt)">
                {{ formatRelativeTime(message.createdAt) }}
              </time>
            </div>
            <p class="content">{{ message.content }}</p>
            <p class="meta">
              <span v-if="!message.read" class="unread-tag">未读</span>
              <span v-else class="read-tag">已读</span>
              <span v-if="messageLinkLabel(message)"> · {{ messageLinkLabel(message) }}</span>
            </p>
          </div>
        </button>
      </article>
    </div>

    <div v-if="totalPages > 1" class="ce-pagination">
      <el-pagination
        background
        layout="prev, pager, next"
        :current-page="page"
        :page-size="PAGE_SIZE"
        :total="totalElements"
        @current-change="handlePageChange"
      />
    </div>
  </section>
</template>

<style scoped>
.header-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.filters {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.loading-box {
  text-align: center;
  color: var(--ce-color-muted);
  padding: 28px;
}

.message-list {
  display: grid;
  gap: 10px;
}

.message-card {
  background: var(--ce-color-surface);
  border: 1px solid var(--ce-color-border);
  border-radius: var(--ce-radius);
  box-shadow: var(--ce-shadow-sm);
  overflow: hidden;
}

.message-card.unread {
  border-color: #b7dad4;
  background: linear-gradient(90deg, #f3faf8 0%, #ffffff 48px);
}

.message-main {
  width: 100%;
  display: flex;
  gap: 12px;
  align-items: flex-start;
  padding: 14px 16px;
  border: 0;
  background: transparent;
  text-align: left;
  cursor: pointer;
  color: inherit;
}

.message-main:hover {
  background: #fafbfc;
}

.message-main:disabled {
  opacity: 0.7;
  cursor: wait;
}

.icon-wrap {
  position: relative;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: var(--ce-color-primary-soft);
  color: var(--ce-color-primary);
  flex-shrink: 0;
}

.dot {
  position: absolute;
  top: 2px;
  right: 2px;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #e11d48;
  border: 1px solid #fff;
}

.body {
  flex: 1;
  min-width: 0;
  display: grid;
  gap: 6px;
}

.top {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: baseline;
}

.top strong {
  font-size: 14px;
}

.top time {
  color: var(--ce-color-muted);
  font-size: 12px;
  white-space: nowrap;
}

.content {
  margin: 0;
  font-size: 14px;
  line-height: 1.55;
  color: var(--ce-color-ink);
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.meta {
  margin: 0;
  color: var(--ce-color-muted);
  font-size: 12px;
}

.unread-tag {
  color: #be123c;
  font-weight: 600;
}

.read-tag {
  color: var(--ce-color-muted);
}

@media (max-width: 960px) {
  .filters {
    flex-direction: column;
  }

  .filters :deep(.el-segmented) {
    width: 100%;
  }

  .header-actions {
    width: 100%;
  }

  .header-actions .el-button {
    flex: 1;
  }
}
</style>
