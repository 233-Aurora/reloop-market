<script setup lang="ts">
defineProps<{
  title: string
  description?: string
}>()
</script>

<template>
  <div class="empty-state" role="status">
    <h3>{{ title }}</h3>
    <p v-if="description">{{ description }}</p>
    <div v-if="$slots.default" class="empty-actions">
      <slot />
    </div>
  </div>
</template>

<style scoped>
.empty-state {
  padding: 48px 24px;
  text-align: center;
  background: var(--ce-color-surface);
  border: 1px dashed var(--ce-color-border);
  border-radius: var(--ce-radius);
}

h3 {
  margin: 0 0 8px;
  font-size: 16px;
  font-weight: 600;
}

p {
  margin: 0;
  color: var(--ce-color-muted);
  line-height: 1.6;
  font-size: 14px;
}

.empty-actions {
  margin-top: 16px;
  display: flex;
  justify-content: center;
  gap: 12px;
  flex-wrap: wrap;
}
</style>
