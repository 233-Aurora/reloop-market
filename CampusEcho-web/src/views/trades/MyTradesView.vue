<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { RefreshRight } from '@element-plus/icons-vue'
import { fetchTradeRequests } from '../../api/trades'
import type { TradeRequestResponse, TradeStatus } from '../../api/types'
import { useAuthStore } from '../../stores/auth'
import { getErrorMessage } from '../../utils/error'
import {
  matchRoleFilter,
  matchStatusFilter,
  type TradeRoleFilter,
} from '../../utils/trade'
import EmptyState from '../../components/EmptyState.vue'
import PageSkeleton from '../../components/PageSkeleton.vue'
import ProblemAlert from '../../components/ProblemAlert.vue'
import TradeRequestList from '../../components/TradeRequestList.vue'

const PAGE_SIZE = 12
const auth = useAuthStore()

const roleFilter = ref<TradeRoleFilter>('ALL')
const statusFilter = ref<'ALL' | TradeStatus>('ALL')
const page = ref(1)
const items = ref<TradeRequestResponse[]>([])
const totalElements = ref(0)
const totalPages = ref(0)
const loading = ref(true)
const errorMessage = ref('')

const roleOptions = [
  { label: '全部角色', value: 'ALL' },
  { label: '我是买家', value: 'BUYER' },
  { label: '我是卖家', value: 'SELLER' },
]

const statusOptions = [
  { label: '全部状态', value: 'ALL' },
  { label: '待处理', value: 'PENDING' },
  { label: '已接受', value: 'ACCEPTED' },
  { label: '争议中', value: 'DISPUTED' },
  { label: '已完成', value: 'COMPLETED' },
  { label: '已拒绝', value: 'REJECTED' },
  { label: '已取消', value: 'CANCELLED' },
]

const userId = computed(() => auth.user?.id ?? 0)

const filteredItems = computed(() => {
  if (!userId.value) return []
  return items.value.filter(
    (trade) =>
      matchRoleFilter(trade, userId.value, roleFilter.value) &&
      matchStatusFilter(trade, statusFilter.value),
  )
})

const summary = computed(() => {
  if (loading.value) return '加载中…'
  if (errorMessage.value) return '加载失败'
  if (roleFilter.value === 'ALL' && statusFilter.value === 'ALL') {
    return `共 ${totalElements.value} 条交易`
  }
  return `当前筛选 ${filteredItems.value.length} 条（本页 ${items.value.length} 条）`
})

async function loadTrades() {
  loading.value = true
  errorMessage.value = ''
  try {
    const result = await fetchTradeRequests(page.value, PAGE_SIZE)
    items.value = result.content
    totalElements.value = result.totalElements
    totalPages.value = result.totalPages
    if (result.totalPages > 0 && page.value > result.totalPages) {
      page.value = result.totalPages
    }
  } catch (error) {
    items.value = []
    totalElements.value = 0
    totalPages.value = 0
    errorMessage.value = getErrorMessage(error, '无法加载交易列表')
  } finally {
    loading.value = false
  }
}

function handlePageChange(next: number) {
  page.value = next
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

watch(page, () => {
  void loadTrades()
})

onMounted(() => {
  void loadTrades()
})
</script>

<template>
  <section class="ce-page">
    <header class="ce-page-header">
      <div>
        <h1>我的交易</h1>
        <p class="ce-page-desc">查看作为买家或卖家参与的交易请求，并处理待办操作。</p>
      </div>
    </header>

    <div class="filters">
      <el-segmented v-model="roleFilter" :options="roleOptions" />
      <el-segmented v-model="statusFilter" :options="statusOptions" />
    </div>

    <div class="ce-result-bar">
      <p>{{ summary }}</p>
      <el-button text :icon="RefreshRight" :loading="loading" @click="loadTrades">
        刷新
      </el-button>
    </div>

    <PageSkeleton v-if="loading" :cards="4" />

    <template v-else-if="errorMessage">
      <ProblemAlert title="加载失败" :message="errorMessage" @close="errorMessage = ''" />
      <EmptyState title="无法加载交易" :description="errorMessage">
        <el-button type="primary" @click="loadTrades">重试</el-button>
      </EmptyState>
    </template>

    <EmptyState
      v-else-if="items.length === 0"
      title="还没有交易记录"
      description="在物品详情页发起交易请求后，会显示在这里。"
    >
      <RouterLink class="ce-cta" :to="{ name: 'home' }">去逛逛市场</RouterLink>
    </EmptyState>

    <EmptyState
      v-else-if="filteredItems.length === 0"
      title="没有符合筛选的交易"
      description="试试切换角色或状态筛选。"
    >
      <el-button
        @click="
          roleFilter = 'ALL';
          statusFilter = 'ALL'
        "
      >
        清除筛选
      </el-button>
    </EmptyState>

    <template v-else>
      <TradeRequestList :items="filteredItems" :user-id="userId" />

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
    </template>
  </section>
</template>

<style scoped>
.filters {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

@media (max-width: 960px) {
  .filters {
    flex-direction: column;
  }

  .filters :deep(.el-segmented) {
    width: 100%;
  }
}
</style>
