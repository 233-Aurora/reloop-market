<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { RefreshRight } from '@element-plus/icons-vue'
import { fetchMyDisputes } from '../../api/disputes'
import type { DisputeReason, DisputeResponse, DisputeStatus } from '../../api/types'
import { getErrorMessage } from '../../utils/error'
import { formatDateTime, formatRelativeTime } from '../../utils/format'
import {
  DISPUTE_REASONS,
  disputeReasonLabel,
  disputeResolutionLabel,
  disputeStatusLabel,
} from '../../utils/dispute'
import EmptyState from '../../components/EmptyState.vue'
import ProblemAlert from '../../components/ProblemAlert.vue'

const PAGE_SIZE = 12
const router = useRouter()

const status = ref<'ALL' | DisputeStatus>('ALL')
const reason = ref<'ALL' | DisputeReason>('ALL')
const page = ref(1)
const items = ref<DisputeResponse[]>([])
const totalElements = ref(0)
const totalPages = ref(0)
const loading = ref(true)
const errorMessage = ref('')

const summary = computed(() => {
  if (loading.value) return '加载中…'
  if (errorMessage.value) return '加载失败'
  return `共 ${totalElements.value} 条争议`
})

async function loadDisputes() {
  loading.value = true
  errorMessage.value = ''
  try {
    const result = await fetchMyDisputes({
      status: status.value === 'ALL' ? undefined : status.value,
      reason: reason.value === 'ALL' ? undefined : reason.value,
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
    errorMessage.value = getErrorMessage(error, '无法加载争议记录')
  } finally {
    loading.value = false
  }
}

function handlePageChange(next: number) {
  page.value = next
  void loadDisputes()
}

watch([status, reason], () => {
  page.value = 1
  void loadDisputes()
})

onMounted(() => {
  void loadDisputes()
})
</script>

<template>
  <section class="ce-page">
    <header class="ce-page-header">
      <div>
        <h1>我的争议</h1>
        <p class="ce-page-desc">
          查看你作为发起人或被申请方参与的交易争议与仲裁结果。
        </p>
      </div>
    </header>

    <div class="filters">
      <el-segmented
        v-model="status"
        :options="[
          { label: '全部状态', value: 'ALL' },
          { label: '待仲裁', value: 'OPEN' },
          { label: '已裁定', value: 'RESOLVED' },
          { label: '已驳回', value: 'REJECTED' },
        ]"
      />
      <el-select v-model="reason" style="width: 180px">
        <el-option label="全部原因" value="ALL" />
        <el-option
          v-for="item in DISPUTE_REASONS"
          :key="item.value"
          :label="item.label"
          :value="item.value"
        />
      </el-select>
    </div>

    <div class="ce-result-bar">
      <p>{{ summary }}</p>
      <el-button text :icon="RefreshRight" :loading="loading" @click="loadDisputes">刷新</el-button>
    </div>

    <ProblemAlert
      v-if="errorMessage"
      title="加载失败"
      :message="errorMessage"
      @close="errorMessage = ''"
    />

    <div v-if="loading" class="ce-panel loading-box">加载争议记录…</div>

    <EmptyState
      v-else-if="items.length === 0 && !errorMessage"
      title="暂无争议记录"
      description="在已接受的交易详情中可发起履约争议。"
    >
      <el-button type="primary" @click="router.push({ name: 'my-trades' })">我的交易</el-button>
    </EmptyState>

    <div v-else class="dispute-list">
      <article v-for="dispute in items" :key="dispute.id" class="dispute-card">
        <div class="top">
          <div>
            <h3>{{ dispute.itemTitle || `物品 #${dispute.itemId}` }}</h3>
            <p class="meta">
              {{ disputeReasonLabel(dispute.reason) }} · 交易 #{{ dispute.tradeRequestId }} ·
              <time :datetime="dispute.createdAt" :title="formatDateTime(dispute.createdAt)">
                {{ formatRelativeTime(dispute.createdAt) }}
              </time>
            </p>
          </div>
          <span class="status" :class="`status-${dispute.status.toLowerCase()}`">
            {{ disputeStatusLabel(dispute.status) }}
          </span>
        </div>
        <p class="description">{{ dispute.description }}</p>
        <div v-if="dispute.status !== 'OPEN'" class="resolution">
          <p>
            仲裁结果：{{ disputeResolutionLabel(dispute.resolution) }}
            <template v-if="dispute.handlerUsername"> · 处理人 {{ dispute.handlerUsername }}</template>
          </p>
          <p v-if="dispute.resolutionNote">说明：{{ dispute.resolutionNote }}</p>
        </div>
        <div class="footer">
          <span>
            发起人 {{ dispute.openerUsername || dispute.openerId }} · 对方
            {{ dispute.respondentUsername || dispute.respondentId }}
          </span>
          <el-button
            size="small"
            text
            type="primary"
            @click="router.push({ name: 'my-dispute-detail', params: { id: dispute.id } })"
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
  align-items: center;
}

.loading-box {
  text-align: center;
  color: var(--ce-color-muted);
  padding: 28px;
}

.dispute-list {
  display: grid;
  gap: 12px;
}

.dispute-card {
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
  gap: 8px;
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

.status-open {
  background: #fee4e2;
  color: #b42318;
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
    align-items: stretch;
  }

  .filters :deep(.el-segmented),
  .filters :deep(.el-select) {
    width: 100% !important;
  }
}
</style>
