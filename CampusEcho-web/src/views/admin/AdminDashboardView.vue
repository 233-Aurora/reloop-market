<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  Goods,
  ScaleToOriginal,
  Setting,
  User,
  Warning,
} from '@element-plus/icons-vue'
import { fetchAdminDashboard } from '../../api/admin'
import type { AdminDashboardResponse } from '../../api/types'
import { useAuthStore } from '../../stores/auth'
import { getErrorMessage } from '../../utils/error'
import EmptyState from '../../components/EmptyState.vue'
import ProblemAlert from '../../components/ProblemAlert.vue'

const auth = useAuthStore()
const router = useRouter()

const loading = ref(true)
const errorMessage = ref('')
const dashboard = ref<AdminDashboardResponse | null>(null)

async function loadDashboard() {
  if (!auth.isAdmin) {
    loading.value = false
    return
  }
  loading.value = true
  errorMessage.value = ''
  try {
    dashboard.value = await fetchAdminDashboard()
  } catch (error) {
    dashboard.value = null
    errorMessage.value = getErrorMessage(error, '无法加载管理概览')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  void loadDashboard()
})
</script>

<template>
  <section class="ce-page">
    <header class="ce-page-header">
      <div>
        <p class="ce-eyebrow">管理后台</p>
        <h1>管理概览</h1>
        <p class="ce-page-desc">查看平台核心数据，并快速进入用户与分类管理。</p>
      </div>
      <el-button :loading="loading" @click="loadDashboard">刷新</el-button>
    </header>

    <EmptyState
      v-if="!auth.isAuthenticated"
      title="请先登录"
      description="管理概览需要管理员账户访问。"
    >
      <el-button
        type="primary"
        @click="router.push({ name: 'login', query: { redirect: '/admin' } })"
      >
        去登录
      </el-button>
    </EmptyState>

    <EmptyState
      v-else-if="!auth.isAdmin"
      title="权限不足"
      description="当前账户不是管理员，无法查看管理概览。"
    >
      <el-button type="primary" @click="router.push({ name: 'home' })">返回市场</el-button>
    </EmptyState>

    <template v-else>
      <ProblemAlert
        v-if="errorMessage"
        title="加载失败"
        :message="errorMessage"
        @close="errorMessage = ''"
      />

      <div v-if="loading && !dashboard" class="skeleton-grid" aria-hidden="true">
        <div v-for="n in 7" :key="n" class="stat-card ce-shimmer" />
      </div>

      <template v-else-if="dashboard">
        <div class="stat-grid">
          <article class="stat-card">
            <p class="stat-label">用户总数</p>
            <strong>{{ dashboard.totalUsers }}</strong>
            <p class="stat-meta">
              正常 {{ dashboard.activeUsers }} · 停用 {{ dashboard.suspendedUsers }}
            </p>
          </article>
          <article class="stat-card">
            <p class="stat-label">活跃管理员</p>
            <strong>{{ dashboard.activeAdmins }}</strong>
            <p class="stat-meta">系统至少保留 1 个</p>
          </article>
          <article class="stat-card">
            <p class="stat-label">物品总数</p>
            <strong>{{ dashboard.totalItems }}</strong>
            <p class="stat-meta">在售 {{ dashboard.onSaleItems }}</p>
          </article>
          <article class="stat-card">
            <p class="stat-label">交易请求</p>
            <strong>{{ dashboard.totalTradeRequests }}</strong>
            <p class="stat-meta">
              待处理 {{ dashboard.pendingTradeRequests }} · 已完成
              {{ dashboard.completedTradeRequests }}
            </p>
          </article>
          <article class="stat-card">
            <p class="stat-label">评价总数</p>
            <strong>{{ dashboard.totalReviews }}</strong>
            <p class="stat-meta">双方互评累计</p>
          </article>
          <article class="stat-card">
            <p class="stat-label">举报总数</p>
            <strong>{{ dashboard.totalReports }}</strong>
            <p class="stat-meta">待处理 {{ dashboard.pendingReports }}</p>
          </article>
          <article class="stat-card">
            <p class="stat-label">交易争议</p>
            <strong>{{ dashboard.totalDisputes }}</strong>
            <p class="stat-meta">待仲裁 {{ dashboard.openDisputes }}</p>
          </article>
        </div>

        <section class="ce-panel shortcuts">
          <h2>快捷入口</h2>
          <div class="shortcut-grid">
            <RouterLink class="shortcut" :to="{ name: 'admin-users' }">
              <el-icon :size="20"><User /></el-icon>
              <div>
                <strong>用户管理</strong>
                <p>检索、启停账户与调整角色</p>
              </div>
            </RouterLink>
            <RouterLink class="shortcut" :to="{ name: 'admin-categories' }">
              <el-icon :size="20"><Setting /></el-icon>
              <div>
                <strong>分类管理</strong>
                <p>维护物品分类树</p>
              </div>
            </RouterLink>
            <RouterLink class="shortcut" :to="{ name: 'admin-reports' }">
              <el-icon :size="20"><Warning /></el-icon>
              <div>
                <strong>举报处理</strong>
                <p>
                  待处理 {{ dashboard.pendingReports }} /
                  总计 {{ dashboard.totalReports }}
                </p>
              </div>
            </RouterLink>
            <RouterLink class="shortcut" :to="{ name: 'admin-disputes' }">
              <el-icon :size="20"><ScaleToOriginal /></el-icon>
              <div>
                <strong>争议仲裁</strong>
                <p>
                  待仲裁 {{ dashboard.openDisputes }} /
                  总计 {{ dashboard.totalDisputes }}
                </p>
              </div>
            </RouterLink>
            <RouterLink class="shortcut" :to="{ name: 'home' }">
              <el-icon :size="20"><Goods /></el-icon>
              <div>
                <strong>物品市场</strong>
                <p>返回前台浏览</p>
              </div>
            </RouterLink>
          </div>
          <p v-if="dashboard.pendingReports > 0 || dashboard.openDisputes > 0" class="report-hint">
            <el-icon><Warning /></el-icon>
            <span v-if="dashboard.pendingReports > 0">
              待处理举报 {{ dashboard.pendingReports }}
              <RouterLink :to="{ name: 'admin-reports' }">去处理</RouterLink>
            </span>
            <span v-if="dashboard.pendingReports > 0 && dashboard.openDisputes > 0"> · </span>
            <span v-if="dashboard.openDisputes > 0">
              待仲裁争议 {{ dashboard.openDisputes }}
              <RouterLink :to="{ name: 'admin-disputes' }">去仲裁</RouterLink>
            </span>
          </p>
        </section>
      </template>

      <EmptyState
        v-else-if="errorMessage"
        title="无法加载概览"
        :description="errorMessage"
      >
        <el-button type="primary" @click="loadDashboard">重试</el-button>
      </EmptyState>
    </template>
  </section>
</template>

<style scoped>
.stat-grid,
.skeleton-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.stat-card {
  padding: 18px 16px;
  background: var(--ce-color-surface);
  border: 1px solid var(--ce-color-border);
  border-radius: var(--ce-radius);
  box-shadow: var(--ce-shadow-sm);
  min-height: 110px;
}

.skeleton-grid .stat-card {
  background: var(--ce-color-skeleton);
  border: 0;
}

.stat-label {
  margin: 0 0 8px;
  color: var(--ce-color-muted);
  font-size: 13px;
}

.stat-card strong {
  display: block;
  font-size: 28px;
  line-height: 1.2;
  color: var(--ce-color-ink);
}

.stat-meta {
  margin: 8px 0 0;
  color: var(--ce-color-muted);
  font-size: 12px;
  line-height: 1.4;
}

.shortcuts h2 {
  margin: 0 0 14px;
  font-size: 16px;
}

.shortcut-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.shortcut {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 14px;
  border: 1px solid var(--ce-color-border);
  border-radius: var(--ce-radius-md);
  background: #fafbfc;
  color: inherit;
}

.shortcut:hover {
  border-color: var(--ce-color-hover-border);
  background: var(--ce-color-primary-soft);
}

.shortcut strong {
  display: block;
  margin-bottom: 4px;
  font-size: 14px;
}

.shortcut p {
  margin: 0;
  color: var(--ce-color-muted);
  font-size: 12px;
  line-height: 1.4;
}

.report-hint {
  display: flex;
  align-items: center;
  gap: 6px;
  margin: 14px 0 0;
  padding-top: 14px;
  border-top: 1px solid var(--ce-color-border);
  color: var(--ce-color-warning-text);
  font-size: 13px;
}

@media (max-width: 960px) {
  .stat-grid,
  .skeleton-grid,
  .shortcut-grid {
    grid-template-columns: 1fr 1fr;
  }
}

@media (max-width: 560px) {
  .stat-grid,
  .skeleton-grid,
  .shortcut-grid {
    grid-template-columns: 1fr;
  }
}
</style>
