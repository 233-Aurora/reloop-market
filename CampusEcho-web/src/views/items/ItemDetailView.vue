<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, Location, ChatDotRound, EditPen, WarnTriangleFilled } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { fetchItem } from '../../api/items'
import { fetchUserProfile } from '../../api/auth'
import { createTradeRequest } from '../../api/trades'
import { ApiError, type ItemResponse, type UserProfileResponse } from '../../api/types'
import { useAuthStore } from '../../stores/auth'
import { getErrorMessage } from '../../utils/error'
import {
  conditionLabel,
  formatDateTime,
  formatPrice,
  formatRelativeTime,
  resolveAssetUrl,
} from '../../utils/format'
import EmptyState from '../../components/EmptyState.vue'
import ItemImageGallery from '../../components/ItemImageGallery.vue'
import ItemStatusTag from '../../components/ItemStatusTag.vue'
import ProblemAlert from '../../components/ProblemAlert.vue'
import ReportDialog from '../../components/ReportDialog.vue'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const item = ref<ItemResponse | null>(null)
const seller = ref<UserProfileResponse | null>(null)
const loading = ref(true)
const sellerLoading = ref(false)
const errorMessage = ref('')
const notFound = ref(false)

const tradeDialogVisible = ref(false)
const tradeMessage = ref('')
const tradeSubmitting = ref(false)
const tradeError = ref('')
const reportDialogVisible = ref(false)

const itemId = computed(() => {
  const raw = route.params.id
  const value = Array.isArray(raw) ? raw[0] : raw
  if (typeof value !== 'string' || !/^\d+$/.test(value)) return null
  return Number(value)
})

const isOwner = computed(() => {
  if (!auth.user || !item.value) return false
  return auth.user.id === item.value.sellerId
})

const canRequestTrade = computed(() => {
  return Boolean(
    item.value &&
      item.value.status === 'ON_SALE' &&
      item.value.moderationStatus !== 'BLOCKED' &&
      !isOwner.value,
  )
})

const canReportItem = computed(() => {
  return Boolean(auth.isAuthenticated && item.value && !isOwner.value)
})

const sellerAvatar = computed(() => resolveAssetUrl(seller.value?.avatarUrl))
const sellerName = computed(
  () => seller.value?.displayName || seller.value?.username || `用户 #${item.value?.sellerId ?? ''}`,
)

async function loadSeller(sellerId: number) {
  sellerLoading.value = true
  seller.value = null
  try {
    seller.value = await fetchUserProfile(sellerId)
  } catch {
    seller.value = null
  } finally {
    sellerLoading.value = false
  }
}

async function loadDetail() {
  if (itemId.value === null) {
    loading.value = false
    notFound.value = true
    errorMessage.value = ''
    item.value = null
    return
  }

  loading.value = true
  errorMessage.value = ''
  notFound.value = false
  item.value = null
  seller.value = null

  try {
    const detail = await fetchItem(itemId.value)
    item.value = detail
    document.title = `${detail.title} · ReLoop Market`
    await loadSeller(detail.sellerId)
  } catch (error) {
    if (error instanceof ApiError && error.status === 404) {
      notFound.value = true
      errorMessage.value = ''
    } else {
      errorMessage.value = getErrorMessage(error, '无法加载物品详情')
    }
  } finally {
    loading.value = false
  }
}

function openTradeDialog() {
  if (!auth.isAuthenticated) {
    void router.push({
      name: 'login',
      query: { redirect: route.fullPath },
    })
    return
  }
  tradeMessage.value = ''
  tradeError.value = ''
  tradeDialogVisible.value = true
}

function openReportDialog() {
  if (!auth.isAuthenticated) {
    void router.push({
      name: 'login',
      query: { redirect: route.fullPath },
    })
    return
  }
  reportDialogVisible.value = true
}

async function submitTradeRequest() {
  if (!item.value || tradeSubmitting.value) return

  tradeSubmitting.value = true
  tradeError.value = ''
  try {
    const result = await createTradeRequest({
      itemId: item.value.id,
      message: tradeMessage.value.trim() || null,
    })
    tradeDialogVisible.value = false
    ElMessage.success('交易请求已发送')
    await router.push({ name: 'trade-detail', params: { id: result.id } })
  } catch (error) {
    tradeError.value = getErrorMessage(error, '发起交易请求失败')
  } finally {
    tradeSubmitting.value = false
  }
}

watch(
  () => route.params.id,
  () => {
    void loadDetail()
  },
  { immediate: true },
)
</script>

<template>
  <section class="detail-page">
    <div class="top-bar">
      <button type="button" class="ce-back-btn" @click="router.back()">
        <el-icon><ArrowLeft /></el-icon>
        <span>返回</span>
      </button>
    </div>

    <div v-if="loading" class="detail-skeleton" aria-hidden="true">
      <div class="sk-gallery shimmer" />
      <div class="sk-info">
        <div class="sk-line wide shimmer" />
        <div class="sk-line price shimmer" />
        <div class="sk-line mid shimmer" />
        <div class="sk-line mid shimmer" />
        <div class="sk-block shimmer" />
      </div>
    </div>

    <EmptyState
      v-else-if="notFound"
      title="物品不存在或已不可见"
      description="可能已下架、售出，或链接不正确。"
    >
      <RouterLink class="ce-cta" :to="{ name: 'home' }">回到物品市场</RouterLink>
    </EmptyState>

    <template v-else-if="errorMessage">
      <ProblemAlert title="加载失败" :message="errorMessage" @close="errorMessage = ''" />
      <EmptyState title="无法加载物品详情" :description="errorMessage">
        <el-button type="primary" @click="loadDetail">重试</el-button>
      </EmptyState>
    </template>

    <template v-else-if="item">
      <div class="detail-grid">
        <ItemImageGallery :images="item.images" :title="item.title" />

        <aside class="info-panel">
          <div class="title-row">
            <h1>{{ item.title }}</h1>
            <ItemStatusTag :status="item.status" />
          </div>

          <p class="price">{{ formatPrice(item.price) }}</p>

          <dl class="meta-list">
            <div>
              <dt>成色</dt>
              <dd>{{ conditionLabel(item.condition) }}</dd>
            </div>
            <div>
              <dt>分类</dt>
              <dd>{{ item.categoryName }}</dd>
            </div>
            <div v-if="item.locationText">
              <dt>地点</dt>
              <dd>
                <el-icon class="loc-icon" aria-hidden="true"><Location /></el-icon>
                {{ item.locationText }}
              </dd>
            </div>
            <div>
              <dt>发布时间</dt>
              <dd :title="formatDateTime(item.createdAt)">
                {{ formatRelativeTime(item.createdAt) }}
              </dd>
            </div>
            <div>
              <dt>更新时间</dt>
              <dd :title="formatDateTime(item.updatedAt)">
                {{ formatDateTime(item.updatedAt) }}
              </dd>
            </div>
          </dl>

          <section class="seller-card" aria-label="卖家资料">
            <div class="seller-main">
              <div class="avatar" aria-hidden="true">
                <img v-if="sellerAvatar" :src="sellerAvatar" :alt="sellerName" />
                <span v-else>{{ sellerName.slice(0, 1) }}</span>
              </div>
              <div class="seller-text">
                <RouterLink
                  v-if="seller"
                  class="seller-name"
                  :to="{ name: 'user-profile', params: { id: seller.id } }"
                >
                  {{ sellerName }}
                </RouterLink>
                <strong v-else class="seller-name plain">
                  {{ sellerLoading ? '加载卖家…' : sellerName }}
                </strong>
                <p class="seller-meta">
                  <template v-if="seller">
                    <span v-if="seller.campus">{{ seller.campus }}</span>
                    <span v-if="seller.campus"> · </span>
                    信誉 {{ seller.reputationScore }}
                  </template>
                  <template v-else-if="!sellerLoading">卖家资料暂不可用</template>
                </p>
              </div>
            </div>
          </section>

          <div class="actions">
            <template v-if="isOwner">
              <el-button
                type="primary"
                :icon="EditPen"
                :disabled="item.moderationStatus === 'BLOCKED'"
                @click="router.push({ name: 'item-edit', params: { id: item.id } })"
              >
                编辑物品
              </el-button>
            </template>
            <template v-else-if="canRequestTrade">
              <el-button type="primary" :icon="ChatDotRound" @click="openTradeDialog">
                发起交易请求
              </el-button>
            </template>
            <template v-else>
              <el-button type="primary" disabled>
                {{ item.status === 'ON_SALE' ? '无法发起交易' : '当前不可交易' }}
              </el-button>
            </template>
            <el-button
              v-if="canReportItem"
              :icon="WarnTriangleFilled"
              @click="openReportDialog"
            >
              举报物品
            </el-button>
          </div>

          <p v-if="item.moderationStatus === 'BLOCKED'" class="hint warn">
            该物品已被管理员屏蔽{{ item.moderationReason ? `：${item.moderationReason}` : '' }}。屏蔽期间不可编辑、上架或交易。
          </p>
          <p v-else-if="!isOwner && item.status !== 'ON_SALE'" class="hint">
            该物品当前状态为「{{ item.status === 'RESERVED' ? '已预订' : item.status === 'SOLD' ? '已售出' : item.status === 'OFF_SHELF' ? '已下架' : item.status }}」，暂不可发起新的交易请求。
          </p>
          <p v-else-if="isOwner" class="hint">这是你发布的物品，可前往编辑或查看相关交易。</p>
        </aside>
      </div>

      <section class="description-panel" aria-labelledby="desc-title">
        <h2 id="desc-title">物品描述</h2>
        <p class="description">{{ item.description || '卖家未填写描述。' }}</p>
      </section>
    </template>

    <ReportDialog
      v-if="item"
      v-model="reportDialogVisible"
      target-type="ITEM"
      :target-id="item.id"
      :target-label="item.title"
    />

    <el-dialog
      v-model="tradeDialogVisible"
      title="发起交易请求"
      width="min(440px, 92vw)"
      destroy-on-close
      @closed="tradeError = ''"
    >
      <p class="dialog-lead">
        向卖家发送交易意向。留言可选，建议说明可交易时间或地点。
      </p>
      <ProblemAlert
        v-if="tradeError"
        title="提交失败"
        :message="tradeError"
        @close="tradeError = ''"
      />
      <el-input
        v-model="tradeMessage"
        type="textarea"
        :rows="4"
        maxlength="500"
        show-word-limit
        placeholder="例如：周五下午可以在图书馆门口当面交易"
      />
      <template #footer>
        <el-button @click="tradeDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="tradeSubmitting" @click="submitTradeRequest">
          发送请求
        </el-button>
      </template>
    </el-dialog>
  </section>
</template>

<style scoped>
.detail-page {
  display: grid;
  gap: 20px;
}

.top-bar {
  display: flex;
  align-items: center;
}

.back-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  min-height: 36px;
  padding: 0 8px 0 4px;
  border: 0;
  border-radius: 6px;
  background: transparent;
  color: var(--ce-color-muted);
  font-size: 14px;
  cursor: pointer;
}

.back-btn:hover {
  color: var(--ce-color-primary);
  background: var(--ce-color-primary-soft);
}

.detail-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.15fr) minmax(280px, 0.85fr);
  gap: 28px;
  align-items: start;
}

.info-panel {
  display: grid;
  gap: 16px;
  padding: 4px 0;
}

.title-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.title-row h1 {
  margin: 0;
  font-size: 26px;
  line-height: 1.3;
  flex: 1;
  min-width: 0;
}

.price {
  margin: 0;
  color: var(--ce-color-price);
  font-size: 28px;
  font-weight: 700;
  line-height: 1.2;
}

.meta-list {
  margin: 0;
  display: grid;
  gap: 10px;
  padding: 14px 0;
  border-top: 1px solid var(--ce-color-border);
  border-bottom: 1px solid var(--ce-color-border);
}

.meta-list > div {
  display: grid;
  grid-template-columns: 72px 1fr;
  gap: 8px;
  font-size: 14px;
  line-height: 1.5;
}

.meta-list dt {
  color: var(--ce-color-muted);
}

.meta-list dd {
  margin: 0;
  display: flex;
  align-items: center;
  gap: 4px;
  min-width: 0;
  word-break: break-word;
}

.loc-icon {
  flex-shrink: 0;
  color: var(--ce-color-muted);
}

.seller-card {
  padding: 14px;
  background: var(--ce-color-surface);
  border: 1px solid var(--ce-color-border);
  border-radius: var(--ce-radius);
}

.seller-main {
  display: flex;
  align-items: center;
  gap: 12px;
}

.avatar {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  overflow: hidden;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: var(--ce-color-primary-soft);
  color: var(--ce-color-primary);
  font-weight: 700;
  flex-shrink: 0;
}

.avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.seller-text {
  min-width: 0;
}

.seller-name {
  display: inline-block;
  font-size: 15px;
  font-weight: 600;
  color: var(--ce-color-ink);
}

.seller-name:not(.plain):hover {
  color: var(--ce-color-primary);
}

.seller-meta {
  margin: 4px 0 0;
  color: var(--ce-color-muted);
  font-size: 13px;
}

.actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.actions .el-button {
  min-width: 140px;
}

.hint {
  margin: 0;
  color: var(--ce-color-muted);
  font-size: 13px;
  line-height: 1.5;
}

.hint.warn {
  color: var(--ce-color-warning-text);
  background: var(--ce-color-warning-bg);
  padding: 10px 12px;
  border-radius: var(--ce-radius-sm);
}

.description-panel {
  padding: 20px 0 8px;
  border-top: 1px solid var(--ce-color-border);
}

.description-panel h2 {
  margin-bottom: 12px;
  font-size: 18px;
}

.description {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
  color: var(--ce-color-ink);
  font-size: 14px;
  line-height: 1.7;
}

.dialog-lead {
  margin: 0 0 12px;
  color: var(--ce-color-muted);
  font-size: 14px;
  line-height: 1.5;
}

.cta {
  display: inline-flex;
  align-items: center;
  min-height: 36px;
  padding: 0 14px;
  border-radius: 6px;
  background: var(--ce-color-primary);
  color: #fff;
  font-size: 14px;
}

.detail-skeleton {
  display: grid;
  grid-template-columns: minmax(0, 1.15fr) minmax(280px, 0.85fr);
  gap: 28px;
}

.sk-gallery {
  aspect-ratio: 4 / 3;
  border-radius: var(--ce-radius);
  background: #eef1f4;
}

.sk-info {
  display: grid;
  gap: 14px;
  align-content: start;
}

.sk-line {
  height: 16px;
  border-radius: 4px;
  background: #eef1f4;
}

.sk-line.wide {
  height: 28px;
  width: 80%;
}

.sk-line.price {
  height: 32px;
  width: 40%;
}

.sk-line.mid {
  width: 60%;
}

.sk-block {
  height: 120px;
  border-radius: var(--ce-radius);
  background: #eef1f4;
}

.shimmer {
  background: linear-gradient(90deg, #eef1f4 25%, #f7f8fa 50%, #eef1f4 75%);
  background-size: 200% 100%;
  animation: shimmer 1.2s ease-in-out infinite;
}

@keyframes shimmer {
  0% {
    background-position: 100% 0;
  }
  100% {
    background-position: -100% 0;
  }
}

@media (max-width: 860px) {
  .detail-grid,
  .detail-skeleton {
    grid-template-columns: 1fr;
    gap: 20px;
  }

  .title-row h1 {
    font-size: 22px;
  }

  .price {
    font-size: 24px;
  }

  .actions .el-button {
    flex: 1;
    min-width: 0;
  }
}

@media (prefers-reduced-motion: reduce) {
  .shimmer {
    animation: none;
  }
}
</style>
