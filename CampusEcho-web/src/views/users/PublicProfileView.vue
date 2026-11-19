<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, WarnTriangleFilled } from '@element-plus/icons-vue'
import { fetchUserProfile } from '../../api/auth'
import { fetchUserReviews } from '../../api/reviews'
import { ApiError, type ReviewResponse, type UserProfileResponse } from '../../api/types'
import { useAuthStore } from '../../stores/auth'
import { getErrorMessage } from '../../utils/error'
import { formatDateTime, resolveAssetUrl } from '../../utils/format'
import EmptyState from '../../components/EmptyState.vue'
import ProblemAlert from '../../components/ProblemAlert.vue'
import ReportDialog from '../../components/ReportDialog.vue'
import ReviewList from '../../components/ReviewList.vue'

const PAGE_SIZE = 10
const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const profile = ref<UserProfileResponse | null>(null)
const reviews = ref<ReviewResponse[]>([])
const reviewPage = ref(1)
const reviewTotal = ref(0)
const reviewTotalPages = ref(0)
const loading = ref(true)
const reviewsLoading = ref(false)
const errorMessage = ref('')
const notFound = ref(false)
const reportDialogVisible = ref(false)

const userId = computed(() => {
  const raw = route.params.id
  const value = Array.isArray(raw) ? raw[0] : raw
  if (typeof value !== 'string' || !/^\d+$/.test(value)) return null
  return Number(value)
})

const avatarSrc = computed(() => resolveAssetUrl(profile.value?.avatarUrl))

const canReportUser = computed(() => {
  if (!auth.isAuthenticated || !profile.value || !auth.user) return false
  return auth.user.id !== profile.value.id
})

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

async function loadReviews(id: number, page = 1) {
  reviewsLoading.value = true
  try {
    const result = await fetchUserReviews(id, page, PAGE_SIZE)
    reviews.value = result.content
    reviewPage.value = result.page
    reviewTotal.value = result.totalElements
    reviewTotalPages.value = result.totalPages
  } catch {
    reviews.value = []
    reviewTotal.value = 0
    reviewTotalPages.value = 0
  } finally {
    reviewsLoading.value = false
  }
}

async function loadProfile() {
  if (userId.value === null) {
    loading.value = false
    notFound.value = true
    return
  }

  loading.value = true
  errorMessage.value = ''
  notFound.value = false
  profile.value = null
  reviews.value = []

  try {
    profile.value = await fetchUserProfile(userId.value)
    document.title = `${profile.value.displayName} · ReLoop Market`
    await loadReviews(userId.value, 1)
  } catch (error) {
    if (error instanceof ApiError && error.status === 404) {
      notFound.value = true
      errorMessage.value = ''
    } else {
      errorMessage.value = getErrorMessage(error, '无法加载用户资料')
    }
  } finally {
    loading.value = false
  }
}

function handleReviewPage(page: number) {
  if (!userId.value) return
  void loadReviews(userId.value, page)
}

watch(
  () => route.params.id,
  () => {
    void loadProfile()
  },
  { immediate: true },
)
</script>

<template>
  <section class="profile-page">
    <button type="button" class="back-btn" @click="router.back()">
      <el-icon><ArrowLeft /></el-icon>
      <span>返回</span>
    </button>

    <div v-if="loading" class="card skeleton" aria-hidden="true">
      <div class="avatar shimmer" />
      <div class="lines">
        <div class="line wide shimmer" />
        <div class="line mid shimmer" />
        <div class="line mid shimmer" />
      </div>
    </div>

    <EmptyState
      v-else-if="notFound"
      title="用户不存在"
      description="该用户可能已注销，或链接不正确。"
    >
      <RouterLink class="cta" :to="{ name: 'home' }">回到物品市场</RouterLink>
    </EmptyState>

    <template v-else-if="errorMessage">
      <ProblemAlert title="加载失败" :message="errorMessage" @close="errorMessage = ''" />
      <EmptyState title="无法加载用户资料" :description="errorMessage">
        <el-button type="primary" @click="loadProfile">重试</el-button>
      </EmptyState>
    </template>

    <template v-else-if="profile">
      <article class="card profile-card">
        <div class="header">
          <div class="avatar" aria-hidden="true">
            <img v-if="avatarSrc" :src="avatarSrc" :alt="profile.displayName" />
            <span v-else>{{ profile.displayName.slice(0, 1) }}</span>
          </div>
          <div>
            <h1>{{ profile.displayName }}</h1>
            <p class="username">@{{ profile.username }}</p>
          </div>
          <div class="score-badge" title="信誉分">
            <span class="score-label">信誉</span>
            <strong>{{ profile.reputationScore }}</strong>
          </div>
        </div>

        <dl class="meta">
          <div>
            <dt>校区</dt>
            <dd>{{ profile.campus || '未填写' }}</dd>
          </div>
          <div>
            <dt>注册时间</dt>
            <dd :title="formatDateTime(profile.createdAt)">
              {{ formatDateTime(profile.createdAt) }}
            </dd>
          </div>
        </dl>

        <p class="privacy-note">公开资料不显示邮箱和手机号。信誉分由历史评价均值计算。</p>

        <div v-if="canReportUser" class="profile-actions">
          <el-button :icon="WarnTriangleFilled" @click="openReportDialog">举报用户</el-button>
        </div>
      </article>

      <ReportDialog
        v-model="reportDialogVisible"
        target-type="USER"
        :target-id="profile.id"
        :target-label="profile.displayName"
      />

      <section class="card reviews-card">
        <header class="reviews-header">
          <h2>收到的评价</h2>
          <span class="count">共 {{ reviewTotal }} 条</span>
        </header>

        <p v-if="reviewsLoading" class="loading-text">加载评价中…</p>
        <template v-else>
          <ReviewList :reviews="reviews" empty-text="该用户还没有收到评价。" />
          <div v-if="reviewTotalPages > 1" class="pagination-wrap">
            <el-pagination
              background
              layout="prev, pager, next"
              :current-page="reviewPage"
              :page-size="PAGE_SIZE"
              :total="reviewTotal"
              @current-change="handleReviewPage"
            />
          </div>
        </template>
      </section>
    </template>
  </section>
</template>

<style scoped>
.profile-page {
  display: grid;
  gap: 16px;
  max-width: 720px;
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

.card {
  padding: 24px;
  background: var(--ce-color-surface);
  border: 1px solid var(--ce-color-border);
  border-radius: var(--ce-radius);
  box-shadow: var(--ce-shadow-sm);
}

.header {
  display: grid;
  grid-template-columns: 64px 1fr auto;
  align-items: center;
  gap: 16px;
  margin-bottom: 20px;
}

.avatar {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  overflow: hidden;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: var(--ce-color-primary-soft);
  color: var(--ce-color-primary);
  font-size: 22px;
  font-weight: 700;
  flex-shrink: 0;
}

.avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

h1 {
  margin: 0 0 4px;
  font-size: 24px;
}

.username {
  margin: 0;
  color: var(--ce-color-muted);
  font-size: 14px;
}

.score-badge {
  min-width: 72px;
  padding: 10px 12px;
  border-radius: 10px;
  background: linear-gradient(135deg, #fff7ed, #ffedd5);
  border: 1px solid #fed7aa;
  text-align: center;
}

.score-label {
  display: block;
  font-size: 11px;
  color: #9a3412;
  margin-bottom: 2px;
}

.score-badge strong {
  font-size: 20px;
  color: #c2410c;
}

.meta {
  margin: 0;
  display: grid;
  gap: 12px;
}

.meta > div {
  display: grid;
  grid-template-columns: 80px 1fr;
  gap: 8px;
  font-size: 14px;
  line-height: 1.5;
}

.meta dt {
  color: var(--ce-color-muted);
}

.meta dd {
  margin: 0;
}

.privacy-note {
  margin: 16px 0 0;
  padding-top: 16px;
  border-top: 1px solid var(--ce-color-border);
  color: var(--ce-color-muted);
  font-size: 13px;
}

.profile-actions {
  margin-top: 14px;
}

.reviews-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

.reviews-header h2 {
  margin: 0;
  font-size: 16px;
}

.count {
  color: var(--ce-color-muted);
  font-size: 13px;
}

.loading-text {
  margin: 0;
  color: var(--ce-color-muted);
  font-size: 14px;
}

.pagination-wrap {
  display: flex;
  justify-content: center;
  margin-top: 16px;
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
  display: flex;
  gap: 16px;
  align-items: center;
}

.skeleton .avatar {
  width: 64px;
  height: 64px;
}

.lines {
  flex: 1;
  display: grid;
  gap: 10px;
}

.line {
  height: 14px;
  border-radius: 4px;
  background: #eef1f4;
}

.line.wide {
  width: 50%;
  height: 22px;
}

.line.mid {
  width: 70%;
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

@media (max-width: 560px) {
  .header {
    grid-template-columns: 56px 1fr;
  }

  .score-badge {
    grid-column: 1 / -1;
    justify-self: start;
  }
}

@media (prefers-reduced-motion: reduce) {
  .shimmer {
    animation: none;
  }
}
</style>
