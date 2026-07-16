<script setup lang="ts">
import { computed, ref } from 'vue'
import type { ItemResponse } from '../api/types'
import {
  conditionLabel,
  formatPrice,
  formatRelativeTime,
  formatDateTime,
  resolveAssetUrl,
} from '../utils/format'
import ItemStatusTag from './ItemStatusTag.vue'

const props = defineProps<{
  item: ItemResponse
  showStatus?: boolean
}>()

const imageFailed = ref(false)

const primaryImage = computed(() => {
  const images = props.item.images ?? []
  const primary = images.find((img) => img.primary) ?? images[0]
  return resolveAssetUrl(primary?.url)
})

const imageSrc = computed(() => {
  if (imageFailed.value) return null
  return primaryImage.value
})
</script>

<template>
  <RouterLink class="item-card" :to="{ name: 'item-detail', params: { id: item.id } }">
    <div class="thumb" aria-hidden="true">
      <img
        v-if="imageSrc"
        :src="imageSrc"
        :alt="item.title"
        loading="lazy"
        @error="imageFailed = true"
      />
      <div v-else class="thumb-placeholder">
        <span>暂无图片</span>
      </div>
      <ItemStatusTag v-if="showStatus" class="status-badge" :status="item.status" />
    </div>

    <div class="body">
      <h3 class="title" :title="item.title">{{ item.title }}</h3>
      <p class="price">{{ formatPrice(item.price) }}</p>
      <div class="meta">
        <span>{{ conditionLabel(item.condition) }}</span>
        <span v-if="item.locationText" class="dot" aria-hidden="true">·</span>
        <span v-if="item.locationText" class="location" :title="item.locationText">
          {{ item.locationText }}
        </span>
      </div>
      <div class="footer">
        <span class="category">{{ item.categoryName }}</span>
        <time :datetime="item.createdAt" :title="formatDateTime(item.createdAt)">
          {{ formatRelativeTime(item.createdAt) }}
        </time>
      </div>
    </div>
  </RouterLink>
</template>

<style scoped>
.item-card {
  display: flex;
  flex-direction: column;
  min-height: 100%;
  background: var(--ce-color-surface);
  border: 1px solid var(--ce-color-border);
  border-radius: var(--ce-radius);
  overflow: hidden;
  color: inherit;
  box-shadow: var(--ce-shadow-sm);
  transition: border-color 0.15s ease, box-shadow 0.15s ease, transform 0.15s ease;
}

.item-card:hover {
  border-color: var(--ce-color-hover-border);
  box-shadow: var(--ce-shadow-md);
  transform: translateY(-2px);
}

.item-card:focus-visible {
  outline: none;
  box-shadow: var(--ce-shadow-focus);
}

.thumb {
  position: relative;
  aspect-ratio: 4 / 3;
  background: #eef1f4;
  overflow: hidden;
}

.thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.thumb-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--ce-color-muted);
  font-size: 13px;
  background:
    linear-gradient(135deg, #f4f6f8 25%, transparent 25%) -8px 0 / 16px 16px,
    linear-gradient(225deg, #f4f6f8 25%, transparent 25%) -8px 0 / 16px 16px,
    linear-gradient(315deg, #f4f6f8 25%, transparent 25%) 0 0 / 16px 16px,
    linear-gradient(45deg, #f4f6f8 25%, #e8ecf0 25%) 0 0 / 16px 16px;
}

.status-badge {
  position: absolute;
  top: 8px;
  left: 8px;
}

.body {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 12px 12px 14px;
  flex: 1;
}

.title {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  min-height: 2.8em;
}

.price {
  margin: 0;
  color: var(--ce-color-price);
  font-size: 18px;
  font-weight: 700;
  line-height: 1.2;
}

.meta,
.footer {
  display: flex;
  align-items: center;
  gap: 4px;
  color: var(--ce-color-muted);
  font-size: 12px;
  line-height: 1.4;
  min-width: 0;
}

.location {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.footer {
  margin-top: auto;
  justify-content: space-between;
  gap: 8px;
  padding-top: 4px;
}

.category {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.dot {
  flex-shrink: 0;
}

time {
  flex-shrink: 0;
}

@media (prefers-reduced-motion: reduce) {
  .item-card {
    transition: none;
  }

  .item-card:hover {
    transform: none;
  }
}
</style>
