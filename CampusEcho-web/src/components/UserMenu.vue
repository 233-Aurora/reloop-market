<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import {
  ArrowDown,
  Bell,
  ChatDotRound,
  DataBoard,
  Goods,
  ScaleToOriginal,
  SwitchButton,
  User,
  Warning,
} from '@element-plus/icons-vue'
import { useAuthStore } from '../stores/auth'
import { useMessageStore } from '../stores/messages'
import { resolveAssetUrl } from '../utils/format'

const auth = useAuthStore()
const messageStore = useMessageStore()
const router = useRouter()

const avatarSrc = computed(() => resolveAssetUrl(auth.user?.avatarUrl))
const displayName = computed(() => auth.user?.displayName || auth.user?.username || '用户')

async function handleCommand(command: string) {
  if (command === 'logout') {
    messageStore.reset()
    auth.logout()
    await router.push({ name: 'login' })
    return
  }
  await router.push({ name: command })
}
</script>

<template>
  <el-dropdown v-if="auth.isAuthenticated" trigger="click" @command="handleCommand">
    <button type="button" class="user-trigger" aria-label="用户菜单">
      <span class="avatar" aria-hidden="true">
        <img v-if="avatarSrc" :src="avatarSrc" :alt="displayName" />
        <span v-else>{{ displayName.slice(0, 1) }}</span>
      </span>
      <span class="name">{{ displayName }}</span>
      <el-icon><ArrowDown /></el-icon>
    </button>
    <template #dropdown>
      <el-dropdown-menu>
        <el-dropdown-item command="profile" :icon="User">个人资料</el-dropdown-item>
        <el-dropdown-item command="my-items" :icon="Goods">我的物品</el-dropdown-item>
        <el-dropdown-item command="my-trades" :icon="ChatDotRound">我的交易</el-dropdown-item>
        <el-dropdown-item command="messages" :icon="Bell">
          消息中心
          <template v-if="messageStore.hasUnread">（{{ messageStore.badgeText }}）</template>
        </el-dropdown-item>
        <el-dropdown-item command="my-disputes" :icon="ScaleToOriginal">我的争议</el-dropdown-item>
        <el-dropdown-item command="my-reports" :icon="Warning">我的举报</el-dropdown-item>
        <el-dropdown-item v-if="auth.isAdmin" divided command="admin-dashboard" :icon="DataBoard">
          管理后台
        </el-dropdown-item>
        <el-dropdown-item divided command="logout" :icon="SwitchButton">退出登录</el-dropdown-item>
      </el-dropdown-menu>
    </template>
  </el-dropdown>

  <div v-else class="auth-links">
    <RouterLink class="text-link" :to="{ name: 'login' }">登录</RouterLink>
    <RouterLink class="primary-link" :to="{ name: 'register' }">注册</RouterLink>
  </div>
</template>

<style scoped>
.user-trigger {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-height: 36px;
  padding: 0 8px;
  border: 0;
  border-radius: var(--ce-radius-sm);
  background: transparent;
  color: var(--ce-color-ink);
  cursor: pointer;
}

.user-trigger:hover {
  background: var(--ce-color-primary-soft);
}

.avatar {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  overflow: hidden;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: var(--ce-color-primary-soft);
  color: var(--ce-color-primary);
  font-size: 13px;
  font-weight: 600;
  flex-shrink: 0;
}

.avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.name {
  max-width: 96px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 14px;
}

.auth-links {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.text-link,
.primary-link {
  display: inline-flex;
  align-items: center;
  min-height: 36px;
  padding: 0 12px;
  border-radius: var(--ce-radius-sm);
  font-size: 14px;
}

.text-link {
  color: var(--ce-color-muted);
}

.text-link:hover {
  color: var(--ce-color-primary);
  background: var(--ce-color-primary-soft);
}

.primary-link {
  background: var(--ce-color-primary);
  color: #fff;
  font-weight: 600;
}

.primary-link:hover {
  background: var(--ce-color-primary-hover);
}

@media (max-width: 960px) {
  .name {
    display: none;
  }
}
</style>
