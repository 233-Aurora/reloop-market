<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { ChatDotRound, Goods, HomeFilled, User } from '@element-plus/icons-vue'

const route = useRoute()

/** 目的地导航：发布在顶栏 / 我的物品页，不占底栏 */
const items = [
  { name: 'home', label: '首页', icon: HomeFilled, to: { name: 'home' } },
  { name: 'my-items', label: '物品', icon: Goods, to: { name: 'my-items' } },
  { name: 'my-trades', label: '交易', icon: ChatDotRound, to: { name: 'my-trades' } },
  { name: 'profile', label: '我的', icon: User, to: { name: 'profile' } },
] as const

const activeName = computed(() => {
  const n = String(route.name ?? '')
  if (n === 'items' || n === 'item-detail' || n === 'item-create') return 'home'
  if (n === 'item-edit') return 'my-items'
  if (n === 'trade-detail') return 'my-trades'
  return n
})
</script>

<template>
  <nav class="bottom-nav" aria-label="移动端导航">
    <RouterLink
      v-for="item in items"
      :key="item.name"
      :to="item.to"
      class="bottom-link"
      :class="{ active: activeName === item.name }"
    >
      <el-icon :size="20"><component :is="item.icon" /></el-icon>
      <span>{{ item.label }}</span>
    </RouterLink>
  </nav>
</template>

<style scoped>
.bottom-nav {
  display: none;
}

@media (max-width: 960px) {
  .bottom-nav {
    position: fixed;
    left: 0;
    right: 0;
    bottom: 0;
    z-index: 100;
    height: calc(var(--ce-bottom-nav-height) + env(safe-area-inset-bottom, 0px));
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    background: var(--ce-color-surface);
    border-top: 1px solid var(--ce-color-border);
    padding-bottom: env(safe-area-inset-bottom, 0);
    box-shadow: 0 -2px 8px rgba(16, 24, 40, 0.04);
  }

  .bottom-link {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 2px;
    color: var(--ce-color-muted);
    font-size: 11px;
  }

  .bottom-link.active {
    color: var(--ce-color-primary);
    font-weight: 600;
  }
}
</style>
