<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { Plus, RefreshRight } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { fetchMyItems, offShelfItem, publishItem } from '../../api/items'
import type { ItemResponse, ItemStatus } from '../../api/types'
import { getErrorMessage } from '../../utils/error'
import { canOffShelf, canPublish } from '../../utils/item'
import EmptyState from '../../components/EmptyState.vue'
import ItemCard from '../../components/ItemCard.vue'
import PageSkeleton from '../../components/PageSkeleton.vue'
import ProblemAlert from '../../components/ProblemAlert.vue'

const PAGE_SIZE = 12
const router = useRouter()

type StatusFilter = 'ALL' | ItemStatus

const statusFilter = ref<StatusFilter>('ALL')
const page = ref(1)
const items = ref<ItemResponse[]>([])
const totalElements = ref(0)
const totalPages = ref(0)
const loading = ref(true)
const errorMessage = ref('')
const actingId = ref<number | null>(null)

const tabs: { label: string; value: StatusFilter }[] = [
  { label: '全部', value: 'ALL' },
  { label: '在售', value: 'ON_SALE' },
  { label: '已预订', value: 'RESERVED' },
  { label: '已售出', value: 'SOLD' },
  { label: '已下架', value: 'OFF_SHELF' },
  { label: '草稿', value: 'DRAFT' },
]

const filteredItems = computed(() => {
  if (statusFilter.value === 'ALL') return items.value
  return items.value.filter((item) => item.status === statusFilter.value)
})

const summary = computed(() => {
  if (loading.value) return '加载中…'
  if (errorMessage.value) return '加载失败'
  if (statusFilter.value === 'ALL') return `共 ${totalElements.value} 件物品`
  return `当前筛选 ${filteredItems.value.length} 件（本页共 ${items.value.length} 件）`
})

async function loadItems() {
  loading.value = true
  errorMessage.value = ''
  try {
    const result = await fetchMyItems(page.value, PAGE_SIZE)
    items.value = result.content
    totalElements.value = result.totalElements
    totalPages.value = result.totalPages
    if (result.totalPages > 0 && page.value > result.totalPages) {
      page.value = result.totalPages
      return
    }
  } catch (error) {
    items.value = []
    totalElements.value = 0
    totalPages.value = 0
    errorMessage.value = getErrorMessage(error, '无法加载我的物品')
  } finally {
    loading.value = false
  }
}

async function handleOffShelf(item: ItemResponse) {
  try {
    await ElMessageBox.confirm(
      `确定下架「${item.title}」吗？下架后其他同学将无法在市场中看到。`,
      '下架物品',
      { type: 'warning', confirmButtonText: '下架', cancelButtonText: '取消' },
    )
  } catch {
    return
  }

  actingId.value = item.id
  try {
    const updated = await offShelfItem(item.id)
    items.value = items.value.map((row) => (row.id === item.id ? updated : row))
    ElMessage.success('已下架')
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '下架失败'))
  } finally {
    actingId.value = null
  }
}

async function handlePublish(item: ItemResponse) {
  actingId.value = item.id
  try {
    const updated = await publishItem(item.id)
    items.value = items.value.map((row) => (row.id === item.id ? updated : row))
    ElMessage.success('已重新上架')
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '上架失败'))
  } finally {
    actingId.value = null
  }
}

function handlePageChange(next: number) {
  page.value = next
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

watch(page, () => {
  void loadItems()
})

onMounted(() => {
  void loadItems()
})
</script>

<template>
  <section class="ce-page">
    <header class="ce-page-header">
      <div>
        <h1>我的物品</h1>
        <p class="ce-page-desc">管理已发布物品，支持编辑、上架与下架。</p>
      </div>
      <el-button type="primary" :icon="Plus" @click="router.push({ name: 'item-create' })">
        发布物品
      </el-button>
    </header>

    <el-segmented v-model="statusFilter" :options="tabs.map((t) => ({ label: t.label, value: t.value }))" />

    <div class="ce-result-bar">
      <p>{{ summary }}</p>
      <el-button text :icon="RefreshRight" :loading="loading" @click="loadItems">刷新</el-button>
    </div>

    <PageSkeleton v-if="loading" :cards="6" />

    <template v-else-if="errorMessage">
      <ProblemAlert title="加载失败" :message="errorMessage" @close="errorMessage = ''" />
      <EmptyState title="无法加载我的物品" :description="errorMessage">
        <el-button type="primary" @click="loadItems">重试</el-button>
      </EmptyState>
    </template>

    <EmptyState
      v-else-if="items.length === 0"
      title="还没有发布过物品"
      description="发布闲置物品后，可以在这里管理状态与图片。"
    >
      <el-button type="primary" @click="router.push({ name: 'item-create' })">去发布</el-button>
    </EmptyState>

    <EmptyState
      v-else-if="filteredItems.length === 0"
      title="该状态下暂无物品"
      description="试试切换其他状态，或发布新的物品。"
    >
      <el-button @click="statusFilter = 'ALL'">查看全部</el-button>
    </EmptyState>

    <template v-else>
      <div class="item-list">
        <article v-for="item in filteredItems" :key="item.id" class="item-row">
          <div class="card-wrap">
            <ItemCard :item="item" show-status />
          </div>
          <div class="row-actions">
            <el-button
              size="small"
              @click="router.push({ name: 'item-detail', params: { id: item.id } })"
            >
              详情
            </el-button>
              <el-button
                size="small"
                type="primary"
                plain
                :disabled="
                  item.moderationStatus === 'BLOCKED' ||
                  item.status === 'SOLD' ||
                  item.status === 'RESERVED'
                "
                @click="router.push({ name: 'item-edit', params: { id: item.id } })"
              >
                编辑
              </el-button>
              <el-button
                v-if="canOffShelf(item.status, item.moderationStatus)"
                size="small"
                :loading="actingId === item.id"
                @click="handleOffShelf(item)"
              >
                下架
              </el-button>
              <el-button
                v-if="canPublish(item.status, item.moderationStatus)"
                size="small"
                type="success"
                plain
                :loading="actingId === item.id"
                @click="handlePublish(item)"
              >
                上架
              </el-button>
              <span v-if="item.moderationStatus === 'BLOCKED'" class="blocked-tag">
                已屏蔽
              </span>
          </div>
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
    </template>
  </section>
</template>

<style scoped>
.item-list {
  display: grid;
  gap: 16px;
}

.item-row {
  display: grid;
  grid-template-columns: minmax(0, 220px) 1fr;
  gap: 16px;
  align-items: stretch;
  padding: 12px;
  background: var(--ce-color-surface);
  border: 1px solid var(--ce-color-border);
  border-radius: var(--ce-radius);
}

.card-wrap {
  min-width: 0;
}

.card-wrap :deep(.item-card) {
  border: 0;
  box-shadow: none;
}

.card-wrap :deep(.item-card:hover) {
  box-shadow: none;
}

.row-actions {
  display: flex;
  flex-wrap: wrap;
  align-content: center;
  gap: 8px;
  padding: 4px 0;
}

.blocked-tag {
  display: inline-flex;
  align-items: center;
  min-height: 24px;
  padding: 0 8px;
  border-radius: 4px;
  background: var(--ce-color-warning-bg);
  color: var(--ce-color-warning-text);
  font-size: 12px;
  font-weight: 600;
}

@media (max-width: 960px) {
  .item-row {
    grid-template-columns: 1fr;
  }

  .row-actions .el-button {
    flex: 1;
  }
}
</style>
