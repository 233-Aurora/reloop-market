<script setup lang="ts">
withDefaults(
  defineProps<{
    cards?: number
  }>(),
  { cards: 8 },
)
</script>

<template>
  <div class="skeleton-grid" aria-hidden="true">
    <div v-for="n in cards" :key="n" class="skeleton-card">
      <div class="thumb shimmer" />
      <div class="lines">
        <div class="line shimmer" />
        <div class="line short shimmer" />
        <div class="line tiny shimmer" />
      </div>
    </div>
  </div>
</template>

<style scoped>
.skeleton-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.skeleton-card {
  background: var(--ce-color-surface);
  border: 1px solid var(--ce-color-border);
  border-radius: var(--ce-radius);
  overflow: hidden;
}

.thumb {
  aspect-ratio: 4 / 3;
  background: #eef1f4;
}

.lines {
  display: grid;
  gap: 10px;
  padding: 14px 12px 16px;
}

.line {
  height: 14px;
  border-radius: 4px;
  background: #eef1f4;
}

.line.short {
  width: 45%;
}

.line.tiny {
  width: 30%;
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

@media (max-width: 960px) {
  .skeleton-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .skeleton-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 12px;
  }
}

@media (max-width: 420px) {
  .skeleton-grid {
    grid-template-columns: 1fr;
  }
}

@media (prefers-reduced-motion: reduce) {
  .shimmer {
    animation: none;
  }
}
</style>
