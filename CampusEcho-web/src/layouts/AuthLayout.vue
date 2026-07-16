<script setup lang="ts">
</script>

<template>
  <div class="auth-shell">
    <header class="auth-header">
      <RouterLink class="brand" to="/">CampusEcho</RouterLink>
    </header>
    <main class="auth-main">
      <RouterView />
    </main>
  </div>
</template>

<style scoped>
.auth-shell {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background:
    radial-gradient(circle at top right, rgba(21, 108, 94, 0.08), transparent 40%),
    var(--ce-color-bg);
}

.auth-header {
  height: var(--ce-header-height);
  display: flex;
  align-items: center;
  padding: 0 max(24px, calc((100% - var(--ce-content-width)) / 2));
  background: var(--ce-color-surface);
  border-bottom: 1px solid var(--ce-color-border);
  box-shadow: var(--ce-shadow-sm);
}

.brand {
  font-size: 20px;
  font-weight: 700;
  color: var(--ce-color-primary);
}

.auth-main {
  flex: 1;
  display: flex;
  align-items: flex-start;
  justify-content: center;
  padding: 48px 16px 64px;
}

@media (max-width: 720px) {
  .auth-header {
    padding: 0 16px;
  }

  .auth-main {
    padding: 28px 16px 40px;
  }
}
</style>
