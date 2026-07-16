<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft } from '@element-plus/icons-vue'
import { fetchMyReport } from '../../api/reports'
import { ApiError, type ReportResponse } from '../../api/types'
import { getErrorMessage } from '../../utils/error'
import { formatDateTime } from '../../utils/format'
import {
  reportActionLabel,
  reportReasonLabel,
  reportStatusLabel,
  reportTargetLabel,
} from '../../utils/report'
import EmptyState from '../../components/EmptyState.vue'
import ProblemAlert from '../../components/ProblemAlert.vue'

const route = useRoute()
const router = useRouter()

const report = ref<ReportResponse | null>(null)
const loading = ref(true)
const errorMessage = ref('')
const notFound = ref(false)

const reportId = computed(() => {
  const raw = route.params.id
  const value = Array.isArray(raw) ? raw[0] : raw
  if (typeof value !== 'string' || !/^\d+$/.test(value)) return null
  return Number(value)
})

async function loadDetail() {
  if (reportId.value === null) {
    loading.value = false
    notFound.value = true
    return
  }

  loading.value = true
  errorMessage.value = ''
  notFound.value = false
  try {
    report.value = await fetchMyReport(reportId.value)
  } catch (error) {
    report.value = null
    if (error instanceof ApiError && error.status === 404) {
      notFound.value = true
    } else {
      errorMessage.value = getErrorMessage(error, '无法加载举报详情')
    }
  } finally {
    loading.value = false
  }
}

watch(
  () => route.params.id,
  () => {
    void loadDetail()
  },
  { immediate: true },
)
</script>

<template>
  <section class="ce-page ce-page-narrow">
    <button type="button" class="ce-back-btn" @click="router.push({ name: 'my-reports' })">
      <el-icon><ArrowLeft /></el-icon>
      <span>返回列表</span>
    </button>

    <div v-if="loading" class="ce-panel loading-box">加载中…</div>

    <EmptyState
      v-else-if="notFound"
      title="举报不存在"
      description="只能查看自己提交的举报。"
    >
      <el-button type="primary" @click="router.push({ name: 'my-reports' })">我的举报</el-button>
    </EmptyState>

    <template v-else-if="errorMessage">
      <ProblemAlert title="加载失败" :message="errorMessage" @close="errorMessage = ''" />
      <EmptyState title="无法加载详情" :description="errorMessage">
        <el-button type="primary" @click="loadDetail">重试</el-button>
      </EmptyState>
    </template>

    <article v-else-if="report" class="ce-panel detail">
      <header class="detail-header">
        <div>
          <p class="ce-eyebrow">举报 #{{ report.id }}</p>
          <h1>
            {{ reportTargetLabel(report.targetType) }} ·
            {{ report.targetLabel || `#${report.targetId}` }}
          </h1>
        </div>
        <span class="status" :class="`status-${report.status.toLowerCase()}`">
          {{ reportStatusLabel(report.status) }}
        </span>
      </header>

      <dl class="meta">
        <div>
          <dt>原因</dt>
          <dd>{{ reportReasonLabel(report.reason) }}</dd>
        </div>
        <div>
          <dt>提交时间</dt>
          <dd>{{ formatDateTime(report.createdAt) }}</dd>
        </div>
        <div>
          <dt>更新时间</dt>
          <dd>{{ formatDateTime(report.updatedAt) }}</dd>
        </div>
      </dl>

      <section class="block">
        <h2>举报说明</h2>
        <p class="body-text">{{ report.description }}</p>
      </section>

      <section v-if="report.status !== 'PENDING'" class="block">
        <h2>处理结果</h2>
        <dl class="meta">
          <div>
            <dt>处理动作</dt>
            <dd>{{ reportActionLabel(report.action) }}</dd>
          </div>
          <div v-if="report.handlerUsername">
            <dt>处理人</dt>
            <dd>{{ report.handlerUsername }}</dd>
          </div>
          <div v-if="report.handledAt">
            <dt>处理时间</dt>
            <dd>{{ formatDateTime(report.handledAt) }}</dd>
          </div>
        </dl>
        <p v-if="report.resolutionNote" class="body-text note">
          {{ report.resolutionNote }}
        </p>
      </section>
      <p v-else class="pending-hint">管理员尚未处理，请耐心等待。</p>
    </article>
  </section>
</template>

<style scoped>
.loading-box {
  text-align: center;
  color: var(--ce-color-muted);
  padding: 28px;
}

.detail {
  display: grid;
  gap: 18px;
}

.detail-header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
}

.detail-header h1 {
  margin-bottom: 0;
  font-size: 22px;
}

.meta {
  margin: 0;
  display: grid;
  gap: 10px;
}

.meta > div {
  display: grid;
  grid-template-columns: 80px 1fr;
  gap: 8px;
  font-size: 14px;
}

.meta dt {
  color: var(--ce-color-muted);
}

.meta dd {
  margin: 0;
  word-break: break-word;
}

.block h2 {
  margin: 0 0 10px;
  font-size: 15px;
}

.body-text {
  margin: 0;
  white-space: pre-wrap;
  line-height: 1.65;
  font-size: 14px;
}

.note {
  margin-top: 10px;
  padding: 10px 12px;
  background: #f8fafc;
  border-radius: var(--ce-radius-sm);
  color: var(--ce-color-muted);
}

.pending-hint {
  margin: 0;
  color: var(--ce-color-muted);
  font-size: 13px;
}

.status {
  display: inline-flex;
  min-height: 24px;
  padding: 0 10px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 600;
  align-items: center;
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
</style>
