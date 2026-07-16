<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { ArrowLeft, ArrowRight } from '@element-plus/icons-vue'
import type { ItemImageResponse } from '../api/types'
import { resolveAssetUrl } from '../utils/format'

const props = defineProps<{
  images: ItemImageResponse[]
  title: string
}>()

const activeIndex = ref(0)
const failedIds = ref<Set<number>>(new Set())

const sortedImages = computed(() => {
  return [...(props.images ?? [])].sort((a, b) => {
    if (a.primary !== b.primary) return a.primary ? -1 : 1
    return a.sortOrder - b.sortOrder
  })
})

const activeImage = computed(() => sortedImages.value[activeIndex.value] ?? null)

const activeSrc = computed(() => {
  if (!activeImage.value) return null
  if (failedIds.value.has(activeImage.value.id)) return null
  return resolveAssetUrl(activeImage.value.url)
})

const hasMultiple = computed(() => sortedImages.value.length > 1)

watch(
  () => props.images,
  () => {
    activeIndex.value = 0
    failedIds.value = new Set()
  },
)

function select(index: number) {
  if (index < 0 || index >= sortedImages.value.length) return
  activeIndex.value = index
}

function prev() {
  if (!hasMultiple.value) return
  const next = activeIndex.value - 1
  activeIndex.value = next < 0 ? sortedImages.value.length - 1 : next
}

function next() {
  if (!hasMultiple.value) return
  const nextIndex = activeIndex.value + 1
  activeIndex.value = nextIndex >= sortedImages.value.length ? 0 : nextIndex
}

function markFailed(id: number) {
  const next = new Set(failedIds.value)
  next.add(id)
  failedIds.value = next
}

function thumbSrc(image: ItemImageResponse) {
  if (failedIds.value.has(image.id)) return null
  return resolveAssetUrl(image.url)
}
</script>

<template>
  <div class="gallery">
    <div class="main" role="img" :aria-label="`${title} 图片`">
      <img
        v-if="activeSrc"
        :src="activeSrc"
        :alt="`${title} 图片 ${activeIndex + 1}`"
        @error="activeImage && markFailed(activeImage.id)"
      />
      <div v-else class="placeholder">
        <span>暂无图片</span>
      </div>

      <template v-if="hasMultiple">
        <button type="button" class="nav prev" aria-label="上一张图片" @click="prev">
          <el-icon :size="18"><ArrowLeft /></el-icon>
        </button>
        <button type="button" class="nav next" aria-label="下一张图片" @click="next">
          <el-icon :size="18"><ArrowRight /></el-icon>
        </button>
        <div class="counter" aria-live="polite">
          {{ activeIndex + 1 }} / {{ sortedImages.length }}
        </div>
      </template>
    </div>

    <div v-if="hasMultiple" class="thumbs" role="list" aria-label="图片缩略图">
      <button
        v-for="(image, index) in sortedImages"
        :key="image.id"
        type="button"
        class="thumb"
        :class="{ active: index === activeIndex }"
        role="listitem"
        :aria-label="`查看第 ${index + 1} 张图片`"
        :aria-current="index === activeIndex ? 'true' : undefined"
        @click="select(index)"
      >
        <img
          v-if="thumbSrc(image)"
          :src="thumbSrc(image)!"
          :alt="`${title} 缩略图 ${index + 1}`"
          loading="lazy"
          @error="markFailed(image.id)"
        />
        <span v-else class="thumb-fallback">无图</span>
      </button>
    </div>
  </div>
</template>

<style scoped>
.gallery {
  display: grid;
  gap: 12px;
}

.main {
  position: relative;
  aspect-ratio: 4 / 3;
  background: #eef1f4;
  border: 1px solid var(--ce-color-border);
  border-radius: var(--ce-radius);
  overflow: hidden;
}

.main img {
  width: 100%;
  height: 100%;
  object-fit: contain;
  background: #f4f6f8;
}

.placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--ce-color-muted);
  font-size: 14px;
  background:
    linear-gradient(135deg, #f4f6f8 25%, transparent 25%) -8px 0 / 16px 16px,
    linear-gradient(225deg, #f4f6f8 25%, transparent 25%) -8px 0 / 16px 16px,
    linear-gradient(315deg, #f4f6f8 25%, transparent 25%) 0 0 / 16px 16px,
    linear-gradient(45deg, #f4f6f8 25%, #e8ecf0 25%) 0 0 / 16px 16px;
}

.nav {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  width: 36px;
  height: 36px;
  border: 0;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.92);
  color: var(--ce-color-ink);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.12);
}

.nav:hover {
  background: #fff;
  color: var(--ce-color-primary);
}

.nav:focus-visible {
  outline: none;
  box-shadow: var(--ce-shadow-focus);
}

.nav.prev {
  left: 10px;
}

.nav.next {
  right: 10px;
}

.counter {
  position: absolute;
  right: 10px;
  bottom: 10px;
  min-height: 24px;
  padding: 0 8px;
  border-radius: 4px;
  background: rgba(32, 37, 45, 0.72);
  color: #fff;
  font-size: 12px;
  line-height: 24px;
}

.thumbs {
  display: flex;
  gap: 8px;
  overflow-x: auto;
  padding-bottom: 2px;
}

.thumb {
  flex: 0 0 72px;
  width: 72px;
  height: 72px;
  padding: 0;
  border: 2px solid transparent;
  border-radius: 6px;
  overflow: hidden;
  background: #eef1f4;
  cursor: pointer;
}

.thumb.active {
  border-color: var(--ce-color-primary);
}

.thumb:focus-visible {
  outline: none;
  box-shadow: var(--ce-shadow-focus);
}

.thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.thumb-fallback {
  display: flex;
  width: 100%;
  height: 100%;
  align-items: center;
  justify-content: center;
  color: var(--ce-color-muted);
  font-size: 12px;
}

@media (max-width: 720px) {
  .main {
    aspect-ratio: 1 / 1;
  }

  .thumb {
    flex-basis: 64px;
    width: 64px;
    height: 64px;
  }
}
</style>
