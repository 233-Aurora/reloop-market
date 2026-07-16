<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, RefreshRight } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { fetchTradeRequest } from '../../api/trades'
import { fetchTradeReviews } from '../../api/reviews'
import { fetchUserProfile } from '../../api/auth'
import {
  ApiError,
  type DisputeResponse,
  type ReviewResponse,
  type TradeRequestResponse,
  type UserProfileResponse,
} from '../../api/types'
import { useAuthStore } from '../../stores/auth'
import { getErrorMessage } from '../../utils/error'
import { formatDateTime, resolveAssetUrl } from '../../utils/format'
import { canReview, isBuyer, isSeller, roleLabel } from '../../utils/trade'
import EmptyState from '../../components/EmptyState.vue'
import ProblemAlert from '../../components/ProblemAlert.vue'
import ReviewForm from '../../components/ReviewForm.vue'
import ReviewList from '../../components/ReviewList.vue'
import TradeActionBar from '../../components/TradeActionBar.vue'
import TradeStatusTag from '../../components/TradeStatusTag.vue'
import TradeStatusTimeline from '../../components/TradeStatusTimeline.vue'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const trade = ref<TradeRequestResponse | null>(null)
const counterparty = ref<UserProfileResponse | null>(null)
const reviews = ref<ReviewResponse[]>([])
const reviewsLoading = ref(false)
const loading = ref(true)
const errorMessage = ref('')
const notFound = ref(false)
const conflictMessage = ref('')

const tradeId = computed(() => {
  const raw = route.params.id
  const value = Array.isArray(raw) ? raw[0] : raw
  if (typeof value !== 'string' || !/^\d+$/.test(value)) return null
  return Number(value)
})

const userId = computed(() => auth.user?.id ?? 0)

const counterpartyId = computed(() => {
  if (!trade.value || !userId.value) return null
  if (isBuyer(trade.value, userId.value)) return trade.value.sellerId
  if (isSeller(trade.value, userId.value)) return trade.value.requesterId
  return null
})

const counterpartyLabel = computed(() => {
  if (!trade.value || !userId.value) return '对方'
  return isBuyer(trade.value, userId.value) ? '卖家' : '买家'
})

const avatarSrc = computed(() => resolveAssetUrl(counterparty.value?.avatarUrl))

const myReview = computed(() =>
  reviews.value.find((review) => review.reviewerId === userId.value) ?? null,
)

const showReviewForm = computed(() => {
  if (!trade.value || !userId.value) return false
  return canReview(trade.value, userId.value, Boolean(myReview.value))
})

async function loadCounterparty(id: number) {
  counterparty.value = null
  try {
    counterparty.value = await fetchUserProfile(id)
  } catch {
    counterparty.value = null
  }
}

async function loadReviews(id: number) {
  reviewsLoading.value = true
  try {
    reviews.value = await fetchTradeReviews(id)
  } catch {
    reviews.value = []
  } finally {
    reviewsLoading.value = false
  }
}

async function loadDetail(options?: { silent?: boolean }) {
  if (tradeId.value === null || !userId.value) {
    loading.value = false
    notFound.value = true
    return
  }

  if (!options?.silent) {
    loading.value = true
  }
  errorMessage.value = ''
  notFound.value = false

  try {
    const detail = await fetchTradeRequest(tradeId.value)
    trade.value = detail
    document.title = `交易 #${detail.id} · CampusEcho`
    const tasks: Promise<void>[] = []
    if (counterpartyId.value) {
      tasks.push(loadCounterparty(counterpartyId.value))
    }
    if (detail.status === 'COMPLETED') {
      tasks.push(loadReviews(detail.id))
    } else {
      reviews.value = []
    }
    await Promise.all(tasks)
  } catch (error) {
    if (error instanceof ApiError && error.status === 404) {
      notFound.value = true
      trade.value = null
      reviews.value = []
    } else {
      errorMessage.value = getErrorMessage(error, '无法加载交易详情')
    }
  } finally {
    loading.value = false
  }
}

function onUpdated(next: TradeRequestResponse) {
  trade.value = next
  conflictMessage.value = ''
  if (next.status === 'COMPLETED') {
    void loadReviews(next.id)
  }
}

function onDisputeOpened(dispute: DisputeResponse) {
  if (trade.value) {
    trade.value = { ...trade.value, status: 'DISPUTED' }
  }
  conflictMessage.value = ''
  ElMessage.success('争议已提交')
  void router.push({ name: 'my-dispute-detail', params: { id: dispute.id } })
}

function onReviewSubmitted(review: ReviewResponse) {
  reviews.value = [review, ...reviews.value.filter((item) => item.id !== review.id)]
}

async function onConflict(message: string) {
  conflictMessage.value = message
  ElMessage.warning('状态可能已变化，正在刷新…')
  await loadDetail({ silent: true })
}

watch(
  () => [route.params.id, auth.user?.id] as const,
  () => {
    void loadDetail()
  },
  { immediate: true },
)
</script>

<template>
  <section class="page">
    <button type="button" class="back-btn" @click="router.push({ name: 'my-trades' })">
      <el-icon><ArrowLeft /></el-icon>
      <span>返回交易列表</span>
    </button>

    <div v-if="loading" class="skeleton" aria-hidden="true">
      <div class="sk-line wide shimmer" />
      <div class="sk-block shimmer" />
      <div class="sk-block short shimmer" />
    </div>

    <EmptyState
      v-else-if="notFound"
      title="交易不存在或无权查看"
      description="只能查看自己参与的交易请求。"
    >
      <RouterLink class="cta" :to="{ name: 'my-trades' }">我的交易</RouterLink>
    </EmptyState>

    <template v-else-if="errorMessage">
      <ProblemAlert title="加载失败" :message="errorMessage" @close="errorMessage = ''" />
      <EmptyState title="无法加载交易详情" :description="errorMessage">
        <el-button type="primary" @click="loadDetail()">重试</el-button>
      </EmptyState>
    </template>

    <template v-else-if="trade">
      <header class="page-header">
        <div>
          <p class="eyebrow">交易 #{{ trade.id }}</p>
          <h1>{{ trade.itemTitle || `物品 #${trade.itemId}` }}</h1>
          <p class="desc">{{ roleLabel(trade, userId) }}</p>
        </div>
        <div class="header-actions">
          <TradeStatusTag :status="trade.status" />
          <el-button text :icon="RefreshRight" @click="loadDetail()">刷新</el-button>
        </div>
      </header>

      <ProblemAlert
        v-if="conflictMessage"
        title="状态已变化"
        :message="conflictMessage"
        @close="conflictMessage = ''"
      />

      <div class="layout">
        <div class="main-col">
          <section class="panel">
            <h2>物品与留言</h2>
            <dl class="meta-list">
              <div>
                <dt>物品</dt>
                <dd>
                  <RouterLink :to="{ name: 'item-detail', params: { id: trade.itemId } }">
                    {{ trade.itemTitle || `物品 #${trade.itemId}` }}
                  </RouterLink>
                </dd>
              </div>
              <div>
                <dt>创建时间</dt>
                <dd :title="formatDateTime(trade.createdAt)">
                  {{ formatDateTime(trade.createdAt) }}
                </dd>
              </div>
              <div>
                <dt>更新时间</dt>
                <dd :title="formatDateTime(trade.updatedAt)">
                  {{ formatDateTime(trade.updatedAt) }}
                </dd>
              </div>
            </dl>
            <div class="message-box">
              <h3>买家留言</h3>
              <p>{{ trade.message || '（无留言）' }}</p>
            </div>
          </section>

          <section class="panel">
            <h2>状态时间线</h2>
            <TradeStatusTimeline :trade="trade" />
          </section>
        </div>

        <aside class="side-col">
          <section class="panel">
            <h2>{{ counterpartyLabel }}</h2>
            <div class="person">
              <div class="avatar" aria-hidden="true">
                <img
                  v-if="avatarSrc"
                  :src="avatarSrc"
                  :alt="counterparty?.displayName || '用户'"
                />
                <span v-else>
                  {{ (counterparty?.displayName || counterpartyLabel).slice(0, 1) }}
                </span>
              </div>
              <div>
                <RouterLink
                  v-if="counterparty"
                  class="name"
                  :to="{ name: 'user-profile', params: { id: counterparty.id } }"
                >
                  {{ counterparty.displayName }}
                </RouterLink>
                <strong v-else class="name">用户 #{{ counterpartyId }}</strong>
                <p class="sub">
                  <template v-if="counterparty">
                    <span v-if="counterparty.campus">{{ counterparty.campus }} · </span>
                    信誉 {{ counterparty.reputationScore }}
                  </template>
                  <template v-else>资料暂不可用</template>
                </p>
              </div>
            </div>
          </section>

          <section class="panel">
            <h2>可执行操作</h2>
            <p v-if="trade.status === 'ACCEPTED'" class="confirm-hint">
              买家确认：{{ trade.requesterConfirmed ? '已确认' : '未确认' }} · 卖家确认：{{
                trade.sellerConfirmed ? '已确认' : '未确认'
              }}
            </p>
            <p v-else-if="trade.status === 'DISPUTED'" class="confirm-hint dispute-hint">
              交易进入争议仲裁，双方不能确认完成。可在「我的争议」查看进度。
            </p>
            <TradeActionBar
              :trade="trade"
              :user-id="userId"
              @updated="onUpdated"
              @conflict="onConflict"
              @dispute-opened="onDisputeOpened"
            />
            <el-button
              v-if="trade.status === 'DISPUTED'"
              style="margin-top: 12px"
              @click="router.push({ name: 'my-disputes' })"
            >
              查看我的争议
            </el-button>
          </section>

          <section v-if="trade.status === 'COMPLETED'" class="panel">
            <h2>交易评价</h2>
            <p v-if="reviewsLoading" class="confirm-hint">加载评价中…</p>
            <template v-else>
              <ReviewForm
                v-if="showReviewForm"
                :trade-request-id="trade.id"
                :reviewee-label="counterpartyLabel"
                @submitted="onReviewSubmitted"
              />
              <p v-else-if="myReview" class="confirm-hint">你已评价过本次交易。</p>
              <div class="review-block">
                <h3 class="sub-title">双方评价</h3>
                <ReviewList :reviews="reviews" empty-text="双方尚未评价。" />
              </div>
            </template>
          </section>
        </aside>
      </div>
    </template>
  </section>
</template>

<style scoped>
.page {
  display: grid;
  gap: 18px;
}

.back-btn {
  justify-self: start;
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

.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.eyebrow {
  margin: 0 0 6px;
  color: var(--ce-color-muted);
  font-size: 13px;
}

.page-header h1 {
  margin-bottom: 6px;
  font-size: 24px;
}

.desc {
  margin: 0;
  color: var(--ce-color-muted);
  font-size: 14px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.layout {
  display: grid;
  grid-template-columns: minmax(0, 1.3fr) minmax(260px, 0.7fr);
  gap: 16px;
  align-items: start;
}

.main-col,
.side-col {
  display: grid;
  gap: 16px;
}

.panel {
  padding: 18px;
  background: var(--ce-color-surface);
  border: 1px solid var(--ce-color-border);
  border-radius: var(--ce-radius);
}

.panel h2 {
  margin: 0 0 14px;
  font-size: 16px;
}

.meta-list {
  margin: 0 0 16px;
  display: grid;
  gap: 10px;
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
  min-width: 0;
  word-break: break-word;
}

.meta-list a {
  color: var(--ce-color-primary);
  font-weight: 600;
}

.meta-list a:hover {
  text-decoration: underline;
}

.message-box {
  padding-top: 14px;
  border-top: 1px solid var(--ce-color-border);
}

.message-box h3 {
  margin: 0 0 8px;
  font-size: 14px;
  font-weight: 600;
}

.message-box p {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
  font-size: 14px;
  line-height: 1.65;
  color: var(--ce-color-ink);
}

.person {
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

.name {
  display: inline-block;
  font-size: 15px;
  font-weight: 600;
  color: var(--ce-color-ink);
}

a.name:hover {
  color: var(--ce-color-primary);
}

.sub {
  margin: 4px 0 0;
  color: var(--ce-color-muted);
  font-size: 13px;
}

.confirm-hint {
  margin: 0 0 12px;
  color: var(--ce-color-muted);
  font-size: 13px;
  line-height: 1.5;
}

.dispute-hint {
  color: #b42318;
}

.review-block {
  display: grid;
  gap: 10px;
  margin-top: 14px;
}

.sub-title {
  margin: 0;
  font-size: 13px;
  font-weight: 600;
  color: var(--ce-color-muted);
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

.skeleton {
  display: grid;
  gap: 14px;
}

.sk-line {
  height: 28px;
  border-radius: 4px;
  background: #eef1f4;
}

.sk-line.wide {
  width: 50%;
}

.sk-block {
  height: 180px;
  border-radius: var(--ce-radius);
  background: #eef1f4;
}

.sk-block.short {
  height: 100px;
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
  .layout {
    grid-template-columns: 1fr;
  }

  .page-header {
    flex-direction: column;
  }
}

@media (prefers-reduced-motion: reduce) {
  .shimmer {
    animation: none;
  }
}
</style>
