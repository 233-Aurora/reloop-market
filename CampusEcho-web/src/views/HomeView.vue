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
    <header class="market-hero">
      <div class="hero-copy">
        <p class="hero-kicker"><span></span> ReLoop Market <i>·</i> CAMPUS REUSE</p>
        <h1>好物不止一站，<br /><em>继续在校园流转。</em></h1>
        <p class="hero-description">从课本到数码，从宿舍到下一位同学。发现值得被再次使用的好物。</p>
        <div class="hero-actions">
          <RouterLink v-if="auth.isAuthenticated" class="hero-primary" :to="{ name: 'item-create' }">
            发布一件闲置 <span aria-hidden="true">↗</span>
          </RouterLink>
          <RouterLink v-else class="hero-primary" :to="{ name: 'register' }">
            加入校园市集 <span aria-hidden="true">↗</span>
          </RouterLink>
          <span class="hero-footnote">面交更安心 · 让闲置重新有用</span>
        </div>
      </div>
      <div class="hero-art" aria-hidden="true">
        <div class="art-ring ring-outer"></div>
        <div class="art-ring ring-inner"></div>
        <div class="art-disc">
          <span class="disc-small">GIVE IT</span>
          <strong>another<br />round</strong>
          <span class="disc-arrow">↗</span>
        </div>
        <div class="art-note"><span>01 / 03</span><strong>Reuse<br />with care.</strong></div>
        <span class="art-spark spark-one">✳</span>
        <span class="art-spark spark-two">✳</span>
      </div>
    </header>

    <section class="market-search-panel" aria-label="搜索和筛选物品">
      <div class="search-heading">
        <div><span class="section-kicker">THE MARKET</span><h2>逛逛校园好物</h2></div>
        <span class="search-aside">SEARCH · FILTER · DISCOVER</span>
      </div>
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
    </section>

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
.market-hero {
  position: relative;
  min-height: 300px;
  display: grid;
  grid-template-columns: minmax(0, 1.25fr) minmax(260px, 0.75fr);
  align-items: center;
  overflow: hidden;
  padding: 34px 48px;
  border: 1px solid #e5d6c2;
  border-radius: 22px;
  background: #eee3d2;
  box-shadow: var(--ce-shadow-sm);
}

.hero-copy { position: relative; z-index: 1; }
.hero-kicker,
.section-kicker {
  margin: 0 0 14px;
  color: var(--ce-color-primary);
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.15em;
}
.hero-kicker span { display: inline-block; width: 7px; height: 7px; margin-right: 7px; border-radius: 50%; background: #c37b42; vertical-align: 1px; }
.hero-kicker i { padding: 0 5px; font-style: normal; color: #a99a84; }
.market-hero h1 { margin: 0; color: #332b25; font-family: Georgia, 'Songti SC', serif; font-size: clamp(34px, 4vw, 54px); font-weight: 600; line-height: 1.18; letter-spacing: -0.045em; }
.market-hero h1 em { color: var(--ce-color-primary); font-style: normal; }
.hero-description { max-width: 500px; margin: 16px 0 0; color: #76695d; font-size: 15px; line-height: 1.8; }
.hero-actions { display: flex; align-items: center; gap: 18px; margin-top: 22px; }
.hero-primary { display: inline-flex; align-items: center; gap: 18px; min-height: 46px; padding: 0 18px; border-radius: 6px; background: var(--ce-color-primary); color: #fffaf4; font-size: 14px; font-weight: 700; transition: transform .15s ease, background .15s ease; }
.hero-primary:hover { transform: translateY(-2px); background: var(--ce-color-primary-hover); }
.hero-primary span { font-size: 18px; }
.hero-footnote { color: #8b7c6c; font-size: 12px; }
.hero-art { position: relative; min-height: 230px; display: grid; place-items: center; }
.art-ring { position: absolute; border: 1px solid rgba(164, 96, 66, .24); border-radius: 50%; }
.ring-outer { width: 238px; height: 238px; }
.ring-inner { width: 188px; height: 188px; border-style: dashed; }
.art-disc { position: relative; width: 148px; height: 148px; display: flex; flex-direction: column; align-items: center; justify-content: center; border-radius: 50%; background: #bd5037; color: #fff5e9; text-align: center; transform: rotate(-9deg); box-shadow: 0 18px 30px rgba(123, 61, 42, .2); }
.disc-small { font-size: 9px; font-weight: 700; letter-spacing: .18em; }
.art-disc strong { margin-top: 5px; font-family: Georgia, serif; font-size: 22px; line-height: .95; }
.disc-arrow { position: absolute; right: 17px; bottom: 15px; font-size: 20px; }
.art-note { position: absolute; right: 2%; bottom: 4px; padding: 10px 13px; border: 1px solid #e3d3c0; background: #fbf6ec; color: #786858; transform: rotate(4deg); box-shadow: 0 6px 14px rgba(66, 47, 32, .08); }
.art-note span { display: block; margin-bottom: 4px; color: #b56247; font-size: 9px; letter-spacing: .13em; }
.art-note strong { font-family: Georgia, serif; font-size: 13px; line-height: 1.1; }
.art-spark { position: absolute; color: #c17a43; }
.spark-one { top: 7%; right: 17%; font-size: 21px; }
.spark-two { bottom: 11%; left: 12%; color: #66836c; font-size: 15px; }

.market-search-panel { display: grid; gap: 16px; padding: 22px 24px; border: 1px solid var(--ce-color-border); border-radius: 16px; background: var(--ce-color-surface); box-shadow: var(--ce-shadow-sm); }
.search-heading { display: flex; align-items: flex-end; justify-content: space-between; gap: 12px; }
.search-heading h2 { margin: 0; color: var(--ce-color-ink); font-family: Georgia, 'Songti SC', serif; font-size: 23px; }
.section-kicker { display: block; margin-bottom: 5px; font-size: 9px; }
.search-aside { color: #a49a8d; font-size: 9px; font-weight: 700; letter-spacing: .14em; }
.desktop-filters { padding-top: 2px; border-top: 1px solid #eee8de; }
.toolbar :deep(.el-input__wrapper) { min-height: 46px; background: #faf7f0; box-shadow: none; }
.toolbar :deep(.el-input-group__append) { background: var(--ce-color-primary); color: white; box-shadow: none; }
.toolbar :deep(.el-input-group__append .el-button) { color: white; }
.ce-result-bar { padding: 0 2px; }

@media (max-width: 800px) {
  .market-hero { grid-template-columns: 1fr; min-height: auto; padding: 28px 24px; }
  .hero-art { position: absolute; opacity: .16; right: 0; width: 240px; pointer-events: none; }
  .hero-copy { max-width: 560px; }
  .hero-description { max-width: 420px; }
}

@media (max-width: 520px) {
  .market-hero { padding: 25px 20px; }
  .market-hero h1 { font-size: 34px; }
  .hero-actions { align-items: flex-start; flex-direction: column; gap: 10px; }
  .hero-art { right: -72px; }
  .market-search-panel { padding: 18px 16px; }
  .search-aside { display: none; }
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
