<script setup lang="ts">
import { ref } from 'vue'
import type { ReviewResponse } from '../api/types'
import { formatDateTime, formatRelativeTime, resolveAssetUrl } from '../utils/format'

defineProps<{
  reviews: ReviewResponse[]
  emptyText?: string
}>()

const failedIds = ref<Set<number>>(new Set())

function avatarSrc(review: ReviewResponse) {
  if (failedIds.value.has(review.id)) return null
  return resolveAssetUrl(review.reviewerAvatarUrl)
}

function markFailed(id: number) {
  const next = new Set(failedIds.value)
  next.add(id)
  failedIds.value = next
}
</script>

<template>
  <div v-if="reviews.length" class="review-list">
    <article v-for="review in reviews" :key="review.id" class="review-card">
      <div class="review-head">
        <div class="avatar" aria-hidden="true">
          <img
            v-if="avatarSrc(review)"
            :src="avatarSrc(review)!"
            :alt="review.reviewerDisplayName || '用户'"
            @error="markFailed(review.id)"
          />
          <span v-else>{{ (review.reviewerDisplayName || '用').slice(0, 1) }}</span>
        </div>
        <div class="meta">
          <strong>{{ review.reviewerDisplayName || `用户 #${review.reviewerId}` }}</strong>
          <time :datetime="review.createdAt" :title="formatDateTime(review.createdAt)">
            {{ formatRelativeTime(review.createdAt) }}
          </time>
        </div>
        <el-rate
          :model-value="review.rating"
          disabled
          show-score
          score-template="{value}"
          :colors="['#f59e0b', '#f59e0b', '#f59e0b']"
        />
      </div>
      <p class="comment">{{ review.comment || '（未填写文字评价）' }}</p>
    </article>
  </div>
  <p v-else class="empty">{{ emptyText || '暂无评价' }}</p>
</template>

<style scoped>
.review-list {
  display: grid;
  gap: 12px;
}

.review-card {
  padding: 14px 16px;
  background: var(--ce-color-surface);
  border: 1px solid var(--ce-color-border);
  border-radius: var(--ce-radius);
  box-shadow: var(--ce-shadow-sm);
}

.review-head {
  display: grid;
  grid-template-columns: 40px 1fr auto;
  gap: 10px;
  align-items: center;
  margin-bottom: 10px;
}

.avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  overflow: hidden;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: var(--ce-color-primary-soft);
  color: var(--ce-color-primary);
  font-weight: 700;
}

.avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.meta {
  min-width: 0;
  display: grid;
  gap: 2px;
}

.meta strong {
  font-size: 14px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.meta time {
  color: var(--ce-color-muted);
  font-size: 12px;
}

.comment {
  margin: 0;
  font-size: 14px;
  line-height: 1.65;
  white-space: pre-wrap;
  word-break: break-word;
  color: var(--ce-color-ink);
}

.empty {
  margin: 0;
  padding: 20px;
  text-align: center;
  color: var(--ce-color-muted);
  font-size: 14px;
  background: var(--ce-color-surface);
  border: 1px dashed var(--ce-color-border);
  border-radius: var(--ce-radius);
}

@media (max-width: 560px) {
  .review-head {
    grid-template-columns: 40px 1fr;
  }

  .review-head :deep(.el-rate) {
    grid-column: 2;
  }
}
</style>
