<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { RefreshRight } from '@element-plus/icons-vue'
import { fetchMyReports } from '../../api/reports'
import type { ReportResponse, ReportStatus, ReportTargetType } from '../../api/types'
import { getErrorMessage } from '../../utils/error'
import { formatDateTime, formatRelativeTime } from '../../utils/format'
import {
  reportActionLabel,
  reportReasonLabel,
  reportStatusLabel,
  reportTargetLabel,
} from '../../utils/report'
import EmptyState from '../../components/EmptyState.vue'
import ProblemAlert from '../../components/ProblemAlert.vue'

const PAGE_SIZE = 12
const router = useRouter()

const targetType = ref<'ALL' | ReportTargetType>('ALL')
const status = ref<'ALL' | ReportStatus>('ALL')
const page = ref(1)
const items = ref<ReportResponse[]>([])
const totalElements = ref(0)
const totalPages = ref(0)
const loading = ref(true)
const errorMessage = ref('')

const summary = computed(() => {
  if (loading.value) return '加载中…'
  if (errorMessage.value) return '加载失败'
  return `共 ${totalElements.value} 条举报`
})

async function loadReports() {
  loading.value = true
  errorMessage.value = ''
  try {
    const result = await fetchMyReports({
      targetType: targetType.value === 'ALL' ? undefined : targetType.value,
      status: status.value === 'ALL' ? undefined : status.value,
      page: page.value,
      size: PAGE_SIZE,
    })
    items.value = result.content
    totalElements.value = result.totalElements
    totalPages.value = result.totalPages
    if (result.totalPages > 0 && page.value > result.totalPages) {
      page.value = result.totalPages
    }
  } catch (error) {
    items.value = []
    totalElements.value = 0
    totalPages.value = 0
    errorMessage.value = getErrorMessage(error, '无法加载举报记录')
  } finally {
    loading.value = false
  }
}

function handlePageChange(next: number) {
  page.value = next
  void loadReports()
}

watch([targetType, status], () => {
  page.value = 1
  void loadReports()
})

onMounted(() => {
  void loadReports()
})
</script>

<template>
  <section class="ce-page">
    <header class="ce-page-header">
      <div>
        <h1>我的举报</h1>
        <p class="ce-page-desc">查看你提交的物品/用户举报及处理结果。</p>
      </div>
    </header>

    <div class="filters">
      <el-segmented
        v-model="targetType"
        :options="[
          { label: '全部目标', value: 'ALL' },
          { label: '物品', value: 'ITEM' },
          { label: '用户', value: 'USER' },
        ]"
      />
      <el-segmented
        v-model="status"
        :options="[
          { label: '全部状态', value: 'ALL' },
          { label: '待处理', value: 'PENDING' },
          { label: '已处理', value: 'RESOLVED' },
          { label: '已驳回', value: 'REJECTED' },
        ]"
      />
    </div>

    <div class="ce-result-bar">
      <p>{{ summary }}</p>
      <el-button text :icon="RefreshRight" :loading="loading" @click="loadReports">刷新</el-button>
    </div>

    <ProblemAlert
      v-if="errorMessage"
      title="加载失败"
      :message="errorMessage"
      @close="errorMessage = ''"
    />

    <div v-if="loading" class="ce-panel loading-box">加载举报记录…</div>

    <EmptyState
      v-else-if="items.length === 0 && !errorMessage"
      title="暂无举报记录"
      description="在物品详情或用户资料页可提交举报。"
    >
      <el-button type="primary" @click="router.push({ name: 'home' })">去逛逛市场</el-button>
    </EmptyState>

    <div v-else class="report-list">
      <article v-for="report in items" :key="report.id" class="report-card">
        <div class="top">
          <div>
            <h3>
              {{ reportTargetLabel(report.targetType) }} ·
              {{ report.targetLabel || `#${report.targetId}` }}
            </h3>
            <p class="meta">
              {{ reportReasonLabel(report.reason) }} ·
              <time :datetime="report.createdAt" :title="formatDateTime(report.createdAt)">
                {{ formatRelativeTime(report.createdAt) }}
              </time>
            </p>
          </div>
          <span class="status" :class="`status-${report.status.toLowerCase()}`">
            {{ reportStatusLabel(report.status) }}
          </span>
        </div>
        <p class="description">{{ report.description }}</p>
        <div v-if="report.status !== 'PENDING'" class="resolution">
          <p>
            处理结果：{{ reportActionLabel(report.action) }}
            <template v-if="report.handlerUsername"> · 处理人 {{ report.handlerUsername }}</template>
          </p>
          <p v-if="report.resolutionNote">说明：{{ report.resolutionNote }}</p>
        </div>
        <div class="footer">
          <span>举报 #{{ report.id }}</span>
          <el-button
            size="small"
            text
            type="primary"
            @click="router.push({ name: 'my-report-detail', params: { id: report.id } })"
          >
            查看详情
          </el-button>
        </div>
      </article>
    </div>

    <div v-if="totalPages > 1" class="ce-pagination">
      <el-pagination
        background
        layout="prev, pager, next"
        :current-page="page"
        :page-size="PAGE_SIZE"
        :total="totalElements"
        @current-change="handlePageChange"
      />
    </div>
  </section>
</template>

<style scoped>
.filters {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.loading-box {
  text-align: center;
  color: var(--ce-color-muted);
  padding: 28px;
}

.report-list {
  display: grid;
  gap: 12px;
}

.report-card {
  padding: 16px;
  background: var(--ce-color-surface);
  border: 1px solid var(--ce-color-border);
  border-radius: var(--ce-radius);
  box-shadow: var(--ce-shadow-sm);
  display: grid;
  gap: 10px;
}

.top {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
}

.top h3 {
  margin: 0 0 4px;
  font-size: 15px;
}

.meta,
.footer {
  margin: 0;
  color: var(--ce-color-muted);
  font-size: 12px;
}

.description {
  margin: 0;
  font-size: 14px;
  line-height: 1.6;
  white-space: pre-wrap;
}

.resolution {
  padding: 10px 12px;
  background: #f8fafc;
  border-radius: var(--ce-radius-sm);
  font-size: 13px;
  line-height: 1.5;
  color: var(--ce-color-muted);
}

.resolution p {
  margin: 0 0 4px;
}

.resolution p:last-child {
  margin-bottom: 0;
}

.footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-top: 1px solid #eef1f4;
  padding-top: 8px;
}

.status {
  display: inline-flex;
  min-height: 22px;
  padding: 0 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 600;
  align-items: center;
  white-space: nowrap;
}

.status-pending {
  background: #fef0c7;
  color: #93370d;
}

.status-resolved {
  background: #e6f2ef;
  color: #0f574c;
}

.status-rejected {
  background: #f2f4f7;
  color: #4b5563;
}

@media (max-width: 960px) {
  .filters {
    flex-direction: column;
  }

  .filters :deep(.el-segmented) {
    width: 100%;
  }
}
</style>
