<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Search, RefreshRight, Filter } from '@element-plus/icons-vue'
import { fetchCategoryTree } from '../api/categories'
import { fetchItems } from '../api/items'
import type { CategoryTreeResponse, ItemResponse } from '../api/types'
import { useAuthStore } from '../stores/auth'
import { getErrorMessage } from '../utils/error'
import CategoryFilter from '../components/CategoryFilter.vue'
import EmptyState from '../components/EmptyState.vue'
import ItemCard from '../components/ItemCard.vue'
import PageSkeleton from '../components/PageSkeleton.vue'
import ProblemAlert from '../components/ProblemAlert.vue'

const PAGE_SIZE = 12

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const keywordInput = ref('')
const keyword = ref('')
const categoryId = ref<number | null>(null)
const page = ref(1)

const categories = ref<CategoryTreeResponse[]>([])
const items = ref<ItemResponse[]>([])
const totalElements = ref(0)
const totalPages = ref(0)

const loading = ref(true)
const categoriesLoading = ref(true)
const errorMessage = ref('')
const filterDrawerVisible = ref(false)

const hasFilters = computed(() => Boolean(keyword.value || categoryId.value !== null))

const resultSummary = computed(() => {
  if (loading.value) return '加载中…'
  if (errorMessage.value) return '加载失败'
  const total = totalElements.value
  if (hasFilters.value) {
    return `找到 ${total} 件物品`
  }
  return `共 ${total} 件在售物品`
})

const selectedCategoryName = computed(() => {
  if (categoryId.value === null) return null
  return categories.value.find((c) => c.id === categoryId.value)?.name ?? null
})

function parseQuery() {
  const q = route.query
  keywordInput.value = typeof q.keyword === 'string' ? q.keyword : ''
  keyword.value = keywordInput.value.trim()

  if (typeof q.categoryId === 'string' && /^\d+$/.test(q.categoryId)) {
    categoryId.value = Number(q.categoryId)
  } else {
    categoryId.value = null
  }

  if (typeof q.page === 'string' && /^\d+$/.test(q.page)) {
    page.value = Math.max(1, Number(q.page))
  } else {
    page.value = 1
  }
}

async function syncQuery(next: {
  keyword?: string
  categoryId?: number | null
  page?: number
}) {
  const query: Record<string, string> = {}
  const nextKeyword = (next.keyword ?? keyword.value).trim()
  const nextCategoryId = next.categoryId !== undefined ? next.categoryId : categoryId.value
  const nextPage = next.page ?? page.value

  if (nextKeyword) query.keyword = nextKeyword
  if (nextCategoryId !== null && nextCategoryId !== undefined) {
    query.categoryId = String(nextCategoryId)
  }
  if (nextPage > 1) query.page = String(nextPage)

  await router.replace({ name: 'home', query })
}

async function loadCategories() {
  categoriesLoading.value = true
  try {
    categories.value = await fetchCategoryTree()
  } catch {
    categories.value = []
  } finally {
    categoriesLoading.value = false
  }
}

async function loadItems() {
  loading.value = true
  errorMessage.value = ''
  try {
    const result = await fetchItems({
      keyword: keyword.value || undefined,
      categoryId: categoryId.value ?? undefined,
      page: page.value,
      size: PAGE_SIZE,
    })
    items.value = result.content
    totalElements.value = result.totalElements
    totalPages.value = result.totalPages

    if (result.totalPages > 0 && page.value > result.totalPages) {
      page.value = result.totalPages
      await syncQuery({ page: result.totalPages })
      return
    }
  } catch (error) {
    items.value = []
    totalElements.value = 0
    totalPages.value = 0
    errorMessage.value = getErrorMessage(error, '无法加载物品列表，请稍后重试')
  } finally {
    loading.value = false
  }
}

async function handleSearch() {
  keyword.value = keywordInput.value.trim()
  page.value = 1
  await syncQuery({ keyword: keyword.value, page: 1 })
}

async function handleCategoryChange(id: number | null) {
  categoryId.value = id
  page.value = 1
  filterDrawerVisible.value = false
  await syncQuery({ categoryId: id, page: 1 })
}

async function handlePageChange(nextPage: number) {
  page.value = nextPage
  await syncQuery({ page: nextPage })
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

async function clearFilters() {
  keywordInput.value = ''
  keyword.value = ''
  categoryId.value = null
  page.value = 1
  await syncQuery({ keyword: '', categoryId: null, page: 1 })
}

async function retry() {
  await Promise.all([loadCategories(), loadItems()])
}

watch(
  () => route.query,
  async () => {
    parseQuery()
    await loadItems()
  },
  { immediate: true },
)

onMounted(() => {
  void loadCategories()
})
</script>

<template>
  <section class="ce-page home-page">
    <header class="ce-page-header home-hero">
      <div>
        <h1>物品市场</h1>
        <p class="ce-page-desc">浏览同学发布的在售物品，支持搜索与分类筛选。</p>
      </div>
    </header>

    <div class="toolbar">
      <div class="search-bar">
        <el-input
          v-model="keywordInput"
          clearable
          size="large"
          placeholder="搜索物品标题或描述"
          :prefix-icon="Search"
          aria-label="搜索物品"
          @clear="handleSearch"
          @keyup.enter="handleSearch"
        >
          <template #append>
            <el-button type="primary" @click="handleSearch">搜索</el-button>
          </template>
        </el-input>
      </div>

      <el-button class="mobile-filter-btn" :icon="Filter" @click="filterDrawerVisible = true">
        筛选
      </el-button>
    </div>

    <div class="desktop-filters">
      <CategoryFilter
        :categories="categories"
        :model-value="categoryId"
        :loading="categoriesLoading"
        @update:model-value="handleCategoryChange"
      />
    </div>

    <div class="ce-result-bar">
      <p>
        {{ resultSummary }}
        <template v-if="selectedCategoryName"> · {{ selectedCategoryName }}</template>
        <template v-if="keyword"> · “{{ keyword }}”</template>
      </p>
      <div class="result-actions">
        <el-button v-if="hasFilters" text type="primary" @click="clearFilters">
          清除筛选
        </el-button>
        <el-button
          text
          :icon="RefreshRight"
          :loading="loading"
          aria-label="刷新列表"
          @click="loadItems"
        >
          刷新
        </el-button>
      </div>
    </div>

    <PageSkeleton v-if="loading" :cards="PAGE_SIZE" />

    <template v-else-if="errorMessage">
      <ProblemAlert title="加载失败" :message="errorMessage" @close="errorMessage = ''" />
      <EmptyState title="无法加载物品" :description="errorMessage">
        <el-button type="primary" @click="retry">重试</el-button>
      </EmptyState>
    </template>

    <template v-else-if="items.length > 0">
      <div class="item-grid" aria-label="在售物品列表">
        <ItemCard v-for="item in items" :key="item.id" :item="item" />
      </div>

      <div v-if="totalPages > 1" class="ce-pagination">
        <el-pagination
          background
          layout="prev, pager, next"
          :current-page="page"
          :page-size="PAGE_SIZE"
          :total="totalElements"
          :hide-on-single-page="true"
          @current-change="handlePageChange"
        />
      </div>
    </template>

    <EmptyState
      v-else-if="hasFilters"
      title="没有符合条件的物品"
      description="试试更换关键词，或切换到其他分类。"
    >
      <el-button type="primary" @click="clearFilters">清除筛选</el-button>
    </EmptyState>

    <EmptyState
      v-else
      title="暂时还没有在售物品"
      description="成为第一个发布闲置物品的同学吧。"
    >
      <RouterLink v-if="auth.isAuthenticated" class="ce-cta" :to="{ name: 'item-create' }">
        发布物品
      </RouterLink>
      <template v-else>
        <RouterLink class="ce-cta" :to="{ name: 'login', query: { redirect: '/items/new' } }">
          登录后发布
        </RouterLink>
        <RouterLink class="ce-cta secondary" :to="{ name: 'register' }">注册账号</RouterLink>
      </template>
    </EmptyState>

    <el-drawer
      v-model="filterDrawerVisible"
      title="分类筛选"
      direction="btt"
      size="auto"
      class="filter-drawer"
    >
      <CategoryFilter
        :categories="categories"
        :model-value="categoryId"
        :loading="categoriesLoading"
        @update:model-value="handleCategoryChange"
      />
    </el-drawer>
  </section>
</template>

<style scoped>
.home-hero {
  padding: 18px 20px;
  background: linear-gradient(135deg, #ffffff 0%, #eef8f5 100%);
  border: 1px solid var(--ce-color-border);
  border-radius: var(--ce-radius);
  box-shadow: var(--ce-shadow-sm);
}

.toolbar {
  display: flex;
  gap: 12px;
  align-items: stretch;
}

.search-bar {
  flex: 1;
  min-width: 0;
  max-width: 640px;
}

.mobile-filter-btn {
  display: none;
}

.result-actions {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-shrink: 0;
}

.item-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

@media (max-width: 960px) {
  .item-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .mobile-filter-btn {
    display: inline-flex;
  }

  .desktop-filters {
    display: none;
  }
}

@media (max-width: 720px) {
  .item-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 12px;
  }
}

@media (max-width: 420px) {
  .item-grid {
    grid-template-columns: 1fr;
  }
}
</style>
