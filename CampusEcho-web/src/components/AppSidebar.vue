<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import {
  Bell,
  ChatDotRound,
  DataBoard,
  Goods,
  HomeFilled,
  ScaleToOriginal,
  Setting,
  UserFilled,
  Warning,
} from '@element-plus/icons-vue'
import { useAuthStore } from '../stores/auth'

defineProps<{
  collapsed: boolean
}>()

const emit = defineEmits<{
  navigate: []
}>()

const route = useRoute()
const auth = useAuthStore()

/** 目的地导航：发布/资料放在页内 CTA 与用户菜单 */
const mainNav = [
  { name: 'home', label: '物品市场', icon: HomeFilled, to: { name: 'home' } },
  { name: 'my-items', label: '我的物品', icon: Goods, to: { name: 'my-items' } },
  { name: 'my-trades', label: '我的交易', icon: ChatDotRound, to: { name: 'my-trades' } },
  { name: 'messages', label: '消息中心', icon: Bell, to: { name: 'messages' } },
  { name: 'my-disputes', label: '我的争议', icon: ScaleToOriginal, to: { name: 'my-disputes' } },
  { name: 'my-reports', label: '我的举报', icon: Warning, to: { name: 'my-reports' } },
] as const

const activeName = computed(() => {
  const n = String(route.name ?? '')
  if (n === 'items' || n === 'item-detail' || n === 'item-create') return 'home'
  if (n === 'item-edit') return 'my-items'
  if (n === 'trade-detail') return 'my-trades'
  if (n === 'my-report-detail') return 'my-reports'
  if (n === 'my-dispute-detail') return 'my-disputes'
  if (
    n === 'admin-dashboard' ||
    n === 'admin-users' ||
    n === 'admin-categories' ||
    n === 'admin-reports' ||
    n === 'admin-disputes'
  ) {
    return n
  }
  return n
})

const adminNav = [
  { name: 'admin-dashboard', label: '管理概览', icon: DataBoard, to: { name: 'admin-dashboard' } },
  { name: 'admin-users', label: '用户管理', icon: UserFilled, to: { name: 'admin-users' } },
  { name: 'admin-categories', label: '分类管理', icon: Setting, to: { name: 'admin-categories' } },
  { name: 'admin-reports', label: '举报处理', icon: Warning, to: { name: 'admin-reports' } },
  {
    name: 'admin-disputes',
    label: '争议仲裁',
    icon: ScaleToOriginal,
    to: { name: 'admin-disputes' },
  },
] as const

function onNavigate() {
  emit('navigate')
}
</script>

<template>
  <aside class="sidebar" :class="{ collapsed }" aria-label="侧边导航">
    <div class="sidebar-top">
      <RouterLink
        class="brand"
        to="/"
        :title="collapsed ? 'ReLoop Market' : undefined"
        @click="onNavigate"
      >
        <span class="brand-mark">R</span>
        <span v-if="!collapsed" class="brand-text">ReLoop Market</span>
      </RouterLink>
    </div>

    <nav class="nav-section">
      <p v-if="!collapsed" class="section-label">浏览</p>
      <RouterLink
        v-for="item in mainNav"
        :key="item.name"
        :to="item.to"
        class="nav-item"
        :class="{ active: activeName === item.name }"
        :title="collapsed ? item.label : undefined"
        @click="onNavigate"
      >
        <el-icon :size="18"><component :is="item.icon" /></el-icon>
        <span v-if="!collapsed" class="nav-label">{{ item.label }}</span>
      </RouterLink>
    </nav>

    <nav v-if="auth.isAdmin" class="nav-section">
      <p v-if="!collapsed" class="section-label">管理</p>
      <RouterLink
        v-for="item in adminNav"
        :key="item.name"
        class="nav-item"
        :class="{ active: activeName === item.name }"
        :to="item.to"
        :title="collapsed ? item.label : undefined"
        @click="onNavigate"
      >
        <el-icon :size="18"><component :is="item.icon" /></el-icon>
        <span v-if="!collapsed" class="nav-label">{{ item.label }}</span>
      </RouterLink>
    </nav>

    <div class="sidebar-footer">
      <p v-if="!collapsed" class="footer-hint">校园二手 · 可信交易</p>
    </div>
  </aside>
</template>

<style scoped>
.sidebar {
  width: var(--ce-sidebar-width);
  height: 100%;
  display: flex;
  flex-direction: column;
  background: linear-gradient(
    180deg,
    var(--ce-color-sidebar) 0%,
    var(--ce-color-sidebar-end) 100%
  );
  color: var(--ce-color-sidebar-text);
  transition: width 0.2s ease;
  overflow: hidden;
}

.sidebar.collapsed {
  width: var(--ce-sidebar-collapsed-width);
}

.sidebar-top {
  display: flex;
  align-items: center;
  min-height: var(--ce-header-height);
  padding: 0 14px;
  border-bottom: 1px solid rgba(97, 76, 56, 0.12);
}

.collapsed .sidebar-top {
  justify-content: center;
  padding: 14px 8px;
}

.brand {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
  color: var(--ce-color-ink);
  font-weight: 700;
}

.brand-mark {
  width: 32px;
  height: 32px;
  border-radius: var(--ce-radius-md);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: var(--ce-color-primary);
  color: #fffaf4;
  font-size: 15px;
  flex-shrink: 0;
}

.brand-text {
  font-size: 16px;
  letter-spacing: 0.02em;
  white-space: nowrap;
}

.nav-section {
  display: grid;
  gap: 4px;
  padding: 14px 10px;
}

.section-label {
  margin: 0 0 6px 10px;
  font-size: 11px;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  color: #968779;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 12px;
  min-height: 42px;
  padding: 0 12px;
  border-radius: var(--ce-radius-md);
  color: var(--ce-color-sidebar-text);
  font-size: 14px;
  transition: background 0.12s ease, color 0.12s ease;
}

.collapsed .nav-item {
  justify-content: center;
  padding: 0;
}

.nav-item:hover {
  background: var(--ce-color-sidebar-hover);
  color: var(--ce-color-primary-hover);
}

.nav-item.active {
  background: var(--ce-color-sidebar-active);
  color: var(--ce-color-sidebar-text-active);
  font-weight: 600;
}

.nav-label {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.sidebar-footer {
  margin-top: auto;
  padding: 16px 18px 20px;
  border-top: 1px solid rgba(97, 76, 56, 0.12);
}

.footer-hint {
  margin: 0;
  font-size: 12px;
  color: #968779;
  line-height: 1.5;
}

.collapsed .sidebar-footer {
  display: none;
}

@media (prefers-reduced-motion: reduce) {
  .sidebar {
    transition: none;
  }
}
</style>
