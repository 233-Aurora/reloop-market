<script setup lang="ts">
import { onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import AppHeader from '../components/AppHeader.vue'
import AppSidebar from '../components/AppSidebar.vue'
import MobileBottomNav from '../components/MobileBottomNav.vue'

const SIDEBAR_KEY = 'campusecho_sidebar_collapsed'
const DESKTOP_BP = 960

const route = useRoute()
const sidebarCollapsed = ref(false)
const mobileDrawerOpen = ref(false)

function readStoredCollapsed(): boolean {
  try {
    return localStorage.getItem(SIDEBAR_KEY) === '1'
  } catch {
    return false
  }
}

function persistCollapsed(value: boolean) {
  try {
    localStorage.setItem(SIDEBAR_KEY, value ? '1' : '0')
  } catch {
    /* ignore */
  }
}

function updateViewport() {
  if (window.innerWidth > DESKTOP_BP) {
    mobileDrawerOpen.value = false
  }
}

function toggleSidebar() {
  sidebarCollapsed.value = !sidebarCollapsed.value
  persistCollapsed(sidebarCollapsed.value)
}

function openMobileMenu() {
  mobileDrawerOpen.value = true
}

function closeMobileMenu() {
  mobileDrawerOpen.value = false
}

watch(
  () => route.fullPath,
  () => {
    mobileDrawerOpen.value = false
  },
)

onMounted(() => {
  sidebarCollapsed.value = readStoredCollapsed()
  updateViewport()
  window.addEventListener('resize', updateViewport)
})

onUnmounted(() => {
  window.removeEventListener('resize', updateViewport)
})
</script>

<template>
  <div class="app-shell" :class="{ 'sidebar-collapsed': sidebarCollapsed }">
    <div class="desktop-sidebar">
      <AppSidebar :collapsed="sidebarCollapsed" />
    </div>

    <div
      v-if="mobileDrawerOpen"
      class="mobile-overlay"
      aria-hidden="true"
      @click="closeMobileMenu"
    />

    <div class="mobile-drawer" :class="{ open: mobileDrawerOpen }">
      <AppSidebar :collapsed="false" @navigate="closeMobileMenu" />
    </div>

    <div class="main-column">
      <AppHeader
        :sidebar-collapsed="sidebarCollapsed"
        @toggle-sidebar="toggleSidebar"
        @open-mobile-menu="openMobileMenu"
      />
      <main class="app-main">
        <div class="content-frame">
          <RouterView />
        </div>
      </main>
    </div>

    <MobileBottomNav />
  </div>
</template>

<style scoped>
.app-shell {
  min-height: 100vh;
  display: grid;
  grid-template-columns: var(--ce-sidebar-width) minmax(0, 1fr);
  background: var(--ce-color-bg);
  transition: grid-template-columns 0.2s ease;
}

.app-shell.sidebar-collapsed {
  grid-template-columns: var(--ce-sidebar-collapsed-width) minmax(0, 1fr);
}

.desktop-sidebar {
  position: sticky;
  top: 0;
  height: 100vh;
  z-index: 40;
}

.main-column {
  min-width: 0;
  display: flex;
  flex-direction: column;
  min-height: 100vh;
}

.app-main {
  flex: 1;
  padding: 20px 24px 32px;
}

.content-frame {
  width: min(var(--ce-content-width), 100%);
  margin: 0 auto;
}

.mobile-overlay,
.mobile-drawer {
  display: none;
}

@media (max-width: 960px) {
  .app-shell,
  .app-shell.sidebar-collapsed {
    grid-template-columns: 1fr;
  }

  .desktop-sidebar {
    display: none;
  }

  .mobile-overlay {
    display: block;
    position: fixed;
    inset: 0;
    z-index: 200;
    background: rgba(16, 24, 40, 0.45);
  }

  .mobile-drawer {
    display: block;
    position: fixed;
    top: 0;
    left: 0;
    bottom: 0;
    z-index: 210;
    width: min(var(--ce-sidebar-width), 86vw);
    transform: translateX(-105%);
    transition: transform 0.2s ease;
    box-shadow: var(--ce-shadow-md);
  }

  .mobile-drawer.open {
    transform: translateX(0);
  }

  .mobile-drawer :deep(.sidebar) {
    width: 100%;
    height: 100%;
  }

  .app-main {
    padding: 16px 16px calc(var(--ce-bottom-nav-height) + env(safe-area-inset-bottom, 0px) + 16px);
  }
}

@media (prefers-reduced-motion: reduce) {
  .app-shell,
  .mobile-drawer {
    transition: none;
  }
}
</style>
