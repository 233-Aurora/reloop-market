<script setup lang="ts">
defineProps<{
  title: string
  description?: string
}>()
</script>

<template>
  <section class="placeholder">
    <h1>{{ title }}</h1>
    <p>{{ description || '该页面将在后续步骤实现。' }}</p>
  </section>
</template>

<style scoped>
.placeholder {
  padding: 32px 0;
}

h1 {
  margin-bottom: 8px;
}

p {
  margin: 0;
  color: var(--ce-color-muted);
  line-height: 1.6;
}
</style>
