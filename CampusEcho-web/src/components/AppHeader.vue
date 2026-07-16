<script setup lang="ts">
import { computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { Bell, Expand, Fold, Menu, Plus } from '@element-plus/icons-vue'
import { useAuthStore } from '../stores/auth'
import { useMessageStore } from '../stores/messages'
import UserMenu from './UserMenu.vue'

defineProps<{
  sidebarCollapsed: boolean
}>()

const emit = defineEmits<{
  'toggle-sidebar': []
  'open-mobile-menu': []
}>()

const route = useRoute()
const auth = useAuthStore()
const messageStore = useMessageStore()

const pageTitle = computed(() => {
  const title = route.meta.title
  return typeof title === 'string' && title ? title : 'CampusEcho'
})

const showPublish = computed(() => {
  const name = String(route.name ?? '')
  return name !== 'item-create' && name !== 'item-edit' && name !== 'login' && name !== 'register'
})

watch(
  () => auth.isAuthenticated,
  (ok) => {
    if (ok) {
      messageStore.startPolling()
    } else {
      messageStore.reset()
    }
  },
  { immediate: true },
)

onMounted(() => {
  if (auth.isAuthenticated) {
    messageStore.startPolling()
  }
})
</script>

<template>
  <header class="app-header">
    <div class="header-left">
      <button
        type="button"
        class="icon-btn mobile-menu-btn"
        aria-label="打开菜单"
        @click="emit('open-mobile-menu')"
      >
        <el-icon :size="20"><Menu /></el-icon>
      </button>

      <button
        type="button"
        class="icon-btn desktop-toggle-btn"
        :aria-label="sidebarCollapsed ? '展开侧边栏' : '收起侧边栏'"
        :title="sidebarCollapsed ? '展开侧边栏' : '收起侧边栏'"
        @click="emit('toggle-sidebar')"
      >
        <el-icon :size="18">
          <Expand v-if="sidebarCollapsed" />
          <Fold v-else />
        </el-icon>
      </button>

      <div class="title-block">
        <span class="page-title">{{ pageTitle }}</span>
        <span class="brand-sub">CampusEcho</span>
      </div>
    </div>

    <div class="header-right">
      <RouterLink
        v-if="auth.isAuthenticated"
        class="icon-link"
        :to="{ name: 'messages' }"
        aria-label="消息中心"
        title="消息中心"
      >
        <el-badge
          :value="messageStore.badgeText"
          :hidden="!messageStore.hasUnread"
          :max="99"
        >
          <el-icon :size="18"><Bell /></el-icon>
        </el-badge>
      </RouterLink>

      <RouterLink
        v-if="showPublish && auth.isAuthenticated"
        class="publish-btn"
        :to="{ name: 'item-create' }"
      >
        <el-icon :size="16"><Plus /></el-icon>
        <span>发布</span>
      </RouterLink>
      <UserMenu />
    </div>
  </header>
</template>

<style scoped>
.app-header {
  height: var(--ce-header-height);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 0 20px;
  background: var(--ce-color-surface);
  border-bottom: 1px solid var(--ce-color-border);
  box-shadow: var(--ce-shadow-sm);
}

.header-left,
.header-right {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.icon-btn,
.icon-link {
  width: 36px;
  height: 36px;
  border: 0;
  border-radius: var(--ce-radius-md);
  background: transparent;
  color: var(--ce-color-muted);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}

.icon-btn:hover,
.icon-link:hover {
  background: var(--ce-color-primary-soft);
  color: var(--ce-color-primary);
}

.mobile-menu-btn {
  display: none;
}

.title-block {
  display: flex;
  flex-direction: column;
  min-width: 0;
  line-height: 1.2;
}

.page-title {
  font-size: 15px;
  font-weight: 700;
  color: var(--ce-color-ink);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.brand-sub {
  font-size: 11px;
  color: var(--ce-color-muted);
}

.publish-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  min-height: 34px;
  padding: 0 12px;
  border-radius: var(--ce-radius-md);
  background: var(--ce-color-primary);
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  white-space: nowrap;
}

.publish-btn:hover {
  background: var(--ce-color-primary-hover);
}

@media (max-width: 960px) {
  .mobile-menu-btn {
    display: inline-flex;
  }

  .desktop-toggle-btn {
    display: none;
  }

  .brand-sub {
    display: none;
  }

  .publish-btn span {
    display: none;
  }

  .publish-btn {
    width: 36px;
    padding: 0;
    justify-content: center;
  }
}
</style>
