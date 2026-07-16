<script setup lang="ts">
defineProps<{
  title?: string
  message: string
}>()

const emit = defineEmits<{
  close: []
}>()
</script>

<template>
  <div class="problem-alert" role="alert">
    <div class="problem-body">
      <strong v-if="title">{{ title }}</strong>
      <p>{{ message }}</p>
    </div>
    <button type="button" class="problem-close" aria-label="关闭错误提示" @click="emit('close')">
      ×
    </button>
  </div>
</template>

<style scoped>
.problem-alert {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 12px 14px;
  background: #fef3f2;
  border-left: 4px solid var(--ce-color-danger);
  border-radius: 0 var(--ce-radius) var(--ce-radius) 0;
  color: #7a271a;
}

.problem-body {
  flex: 1;
  min-width: 0;
}

strong {
  display: block;
  margin-bottom: 4px;
  font-size: 14px;
}

p {
  margin: 0;
  font-size: 14px;
  line-height: 1.5;
}

.problem-close {
  border: 0;
  background: transparent;
  color: inherit;
  font-size: 20px;
  line-height: 1;
  cursor: pointer;
  padding: 0 4px;
}
</style>
