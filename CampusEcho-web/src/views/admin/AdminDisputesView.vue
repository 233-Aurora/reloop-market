<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { RefreshRight } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  fetchAdminDispute,
  fetchAdminDisputes,
  rejectAdminDispute,
  resolveAdminDispute,
} from '../../api/disputes'
import type {
  DisputeReason,
  DisputeResolution,
  DisputeResponse,
  DisputeStatus,
} from '../../api/types'
import { useAuthStore } from '../../stores/auth'
import {
  DISPUTE_REASONS,
  DISPUTE_RESOLUTIONS,
  disputeReasonLabel,
  disputeResolutionLabel,
  disputeStatusLabel,
} from '../../utils/dispute'
import { getErrorMessage } from '../../utils/error'
import { formatDateTime, formatRelativeTime } from '../../utils/format'
import EmptyState from '../../components/EmptyState.vue'
import ProblemAlert from '../../components/ProblemAlert.vue'

const PAGE_SIZE = 20
const auth = useAuthStore()
const router = useRouter()

const status = ref<'ALL' | DisputeStatus>('OPEN')
const reason = ref<'ALL' | DisputeReason>('ALL')
const page = ref(1)

const items = ref<DisputeResponse[]>([])
const totalElements = ref(0)
const totalPages = ref(0)
const loading = ref(true)
const errorMessage = ref('')
const actingId = ref<number | null>(null)

const detailVisible = ref(false)
const detailLoading = ref(false)
const detailError = ref('')
const detail = ref<DisputeResponse | null>(null)

const resolveNote = ref('')
const resolveAction = ref<Exclude<DisputeResolution, 'NONE'>>('CANCEL_TRADE')
const rejectNote = ref('')

const summary = computed(() => {
  if (loading.value) return '加载中…'
  if (errorMessage.value) return '加载失败'
  return `共 ${totalElements.value} 条争议`
})

async function loadDisputes() {
  if (!auth.isAdmin) {
    loading.value = false
    return
  }
  loading.value = true
  errorMessage.value = ''
  try {
    const result = await fetchAdminDisputes({
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
    errorMessage.value = getErrorMessage(error, '无法加载争议列表')
  } finally {
    loading.value = false
  }
}

function replaceDispute(updated: DisputeResponse) {
  items.value = items.value.map((row) => (row.id === updated.id ? updated : row))
  if (detail.value?.id === updated.id) {
    detail.value = updated
  }
}

async function openDetail(disputeId: number) {
  detailVisible.value = true
  detailLoading.value = true
  detailError.value = ''
  detail.value = null
  resolveNote.value = ''
  rejectNote.value = ''
  resolveAction.value = 'CANCEL_TRADE'
  try {
    detail.value = await fetchAdminDispute(disputeId)
  } catch (error) {
    detailError.value = getErrorMessage(error, '无法加载争议详情')
  } finally {
    detailLoading.value = false
  }
}

async function handleResolve() {
  if (!detail.value || actingId.value !== null) return
  const note = resolveNote.value.trim()
  if (note.length < 2) {
    ElMessage.warning('仲裁说明至少 2 个字符')
    return
  }

  const actionLabel = disputeResolutionLabel(resolveAction.value)
  try {
    await ElMessageBox.confirm(
      `确认以「${actionLabel}」处理该争议？交易状态将立即变更。`,
      '仲裁争议',
      { type: 'warning', confirmButtonText: '确认裁定', cancelButtonText: '取消' },
    )
  } catch {
    return
  }

  actingId.value = detail.value.id
  try {
    const updated = await resolveAdminDispute(detail.value.id, {
      resolution: resolveAction.value,
      resolutionNote: note,
    })
    replaceDispute(updated)
    ElMessage.success('争议已裁定')
    await loadDisputes()
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '裁定失败'))
  } finally {
    actingId.value = null
  }
}

async function handleReject() {
  if (!detail.value || actingId.value !== null) return
  const note = rejectNote.value.trim()
  if (note.length < 2) {
    ElMessage.warning('驳回说明至少 2 个字符')
    return
  }

  try {
    await ElMessageBox.confirm(
      '驳回后交易将恢复为「已接受」，双方可继续确认完成。确认驳回？',
      '驳回争议',
      { type: 'warning', confirmButtonText: '驳回', cancelButtonText: '取消' },
    )
  } catch {
    return
  }

  actingId.value = detail.value.id
  try {
    const updated = await rejectAdminDispute(detail.value.id, { resolutionNote: note })
    replaceDispute(updated)
    ElMessage.success('已驳回争议')
    await loadDisputes()
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '驳回失败'))
  } finally {
    actingId.value = null
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
        <p class="ce-eyebrow">管理后台</p>
        <h1>争议仲裁</h1>
        <p class="ce-page-desc">
          处理已接受交易的履约争议。可裁定完成、取消交易或驳回；内容违规请走举报处理，勿与争议混用。
        </p>
      </div>
      <el-button @click="router.push({ name: 'admin-dashboard' })">管理概览</el-button>
    </header>

    <EmptyState
      v-if="!auth.isAuthenticated"
      title="请先登录"
      description="争议仲裁需要管理员账户。"
    >
      <el-button
        type="primary"
        @click="router.push({ name: 'login', query: { redirect: '/admin/disputes' } })"
      >
        去登录
      </el-button>
    </EmptyState>

    <EmptyState
      v-else-if="!auth.isAdmin"
      title="权限不足"
      description="当前账户不是管理员。"
    >
      <el-button type="primary" @click="router.push({ name: 'home' })">返回市场</el-button>
    </EmptyState>

    <template v-else>
      <div class="filters">
        <el-select v-model="status" style="width: 140px">
          <el-option label="全部状态" value="ALL" />
          <el-option label="待仲裁" value="OPEN" />
          <el-option label="已裁定" value="RESOLVED" />
          <el-option label="已驳回" value="REJECTED" />
        </el-select>
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
        <el-button text :icon="RefreshRight" :loading="loading" @click="loadDisputes">
          刷新
        </el-button>
      </div>

      <ProblemAlert
        v-if="errorMessage"
        title="加载失败"
        :message="errorMessage"
        @close="errorMessage = ''"
      />

      <div v-if="loading" class="ce-panel loading-box">加载争议列表…</div>

      <EmptyState
        v-else-if="items.length === 0 && !errorMessage"
        title="没有匹配的争议"
        description="试试调整筛选条件。"
      />

      <div v-else class="table-wrap">
        <table class="dispute-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>物品 / 交易</th>
              <th>原因</th>
              <th>发起人</th>
              <th>状态</th>
              <th>时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="dispute in items" :key="dispute.id">
              <td>#{{ dispute.id }}</td>
              <td>
                <strong>{{ dispute.itemTitle || `物品 #${dispute.itemId}` }}</strong>
                <p class="sub">交易 #{{ dispute.tradeRequestId }}</p>
              </td>
              <td>{{ disputeReasonLabel(dispute.reason) }}</td>
              <td>{{ dispute.openerUsername || dispute.openerId }}</td>
              <td>
                <span class="status" :class="`status-${dispute.status.toLowerCase()}`">
                  {{ disputeStatusLabel(dispute.status) }}
                </span>
              </td>
              <td :title="formatDateTime(dispute.createdAt)">
                {{ formatRelativeTime(dispute.createdAt) }}
              </td>
              <td>
                <el-button size="small" type="primary" plain @click="openDetail(dispute.id)">
                  处理
                </el-button>
              </td>
            </tr>
          </tbody>
        </table>
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
    </template>

    <el-drawer
      v-model="detailVisible"
      title="争议仲裁"
      size="min(480px, 96vw)"
      destroy-on-close
    >
      <div v-if="detailLoading" class="loading-box">加载详情…</div>
      <ProblemAlert
        v-else-if="detailError"
        title="加载失败"
        :message="detailError"
        @close="detailError = ''"
      />
      <template v-else-if="detail">
        <dl class="detail-meta">
          <div>
            <dt>物品</dt>
            <dd>{{ detail.itemTitle || `#${detail.itemId}` }}</dd>
          </div>
          <div>
            <dt>交易</dt>
            <dd>
              <RouterLink :to="{ name: 'trade-detail', params: { id: detail.tradeRequestId } }">
                #{{ detail.tradeRequestId }}
              </RouterLink>
            </dd>
          </div>
          <div>
            <dt>原因</dt>
            <dd>{{ disputeReasonLabel(detail.reason) }}</dd>
          </div>
          <div>
            <dt>发起人</dt>
            <dd>{{ detail.openerUsername || detail.openerId }}</dd>
          </div>
          <div>
            <dt>被申请方</dt>
            <dd>{{ detail.respondentUsername || detail.respondentId }}</dd>
          </div>
          <div>
            <dt>状态</dt>
            <dd>{{ disputeStatusLabel(detail.status) }}</dd>
          </div>
          <div>
            <dt>说明</dt>
            <dd class="pre">{{ detail.description }}</dd>
          </div>
        </dl>

        <template v-if="detail.status === 'OPEN'">
          <section class="action-block">
            <h3>裁定结果</h3>
            <el-radio-group v-model="resolveAction" class="resolution-group">
              <el-radio
                v-for="item in DISPUTE_RESOLUTIONS"
                :key="item.value"
                :value="item.value"
                border
              >
                <span class="radio-title">{{ item.label }}</span>
                <span class="radio-desc">{{ item.description }}</span>
              </el-radio>
            </el-radio-group>
            <el-input
              v-model="resolveNote"
              type="textarea"
              :rows="3"
              maxlength="1000"
              show-word-limit
              placeholder="仲裁说明（2-1000 字）"
              style="margin-top: 10px"
            />
            <el-button
              type="primary"
              style="margin-top: 10px"
              :loading="actingId === detail.id"
              @click="handleResolve"
            >
              确认裁定
            </el-button>
          </section>

          <section class="action-block">
            <h3>驳回争议</h3>
            <p class="block-hint">驳回后交易恢复为已接受，双方可继续确认完成。</p>
            <el-input
              v-model="rejectNote"
              type="textarea"
              :rows="3"
              maxlength="1000"
              show-word-limit
              placeholder="驳回说明（2-1000 字）"
            />
            <el-button
              style="margin-top: 10px"
              :loading="actingId === detail.id"
              @click="handleReject"
            >
              驳回争议
            </el-button>
          </section>
        </template>

        <section v-else class="action-block">
          <h3>已处理</h3>
          <p>结果：{{ disputeResolutionLabel(detail.resolution) }}</p>
          <p v-if="detail.handlerUsername">处理人：{{ detail.handlerUsername }}</p>
          <p v-if="detail.handledAt">时间：{{ formatDateTime(detail.handledAt) }}</p>
          <p v-if="detail.resolutionNote" class="pre">说明：{{ detail.resolutionNote }}</p>
        </section>
      </template>
    </el-drawer>
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

.table-wrap {
  overflow-x: auto;
  background: var(--ce-color-surface);
  border: 1px solid var(--ce-color-border);
  border-radius: var(--ce-radius);
}

.dispute-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

.dispute-table th,
.dispute-table td {
  padding: 12px 14px;
  text-align: left;
  border-bottom: 1px solid var(--ce-color-border);
  vertical-align: top;
}

.dispute-table th {
  background: #f8fafc;
  color: var(--ce-color-muted);
  font-weight: 600;
  white-space: nowrap;
}

.dispute-table tbody tr:last-child td {
  border-bottom: 0;
}

.sub {
  margin: 4px 0 0;
  color: var(--ce-color-muted);
  font-size: 12px;
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

.detail-meta {
  margin: 0 0 18px;
  display: grid;
  gap: 10px;
}

.detail-meta > div {
  display: grid;
  grid-template-columns: 72px 1fr;
  gap: 8px;
  font-size: 13px;
  line-height: 1.5;
}

.detail-meta dt {
  color: var(--ce-color-muted);
}

.detail-meta dd {
  margin: 0;
  word-break: break-word;
}

.detail-meta a {
  color: var(--ce-color-primary);
  font-weight: 600;
}

.pre {
  white-space: pre-wrap;
}

.action-block {
  margin-bottom: 18px;
  padding: 14px;
  border: 1px solid var(--ce-color-border);
  border-radius: var(--ce-radius-md);
  background: #fafbfc;
}

.action-block h3 {
  margin: 0 0 10px;
  font-size: 14px;
}

.action-block p {
  margin: 0 0 6px;
  font-size: 13px;
  line-height: 1.5;
}

.block-hint {
  margin: 0 0 10px !important;
  color: var(--ce-color-muted);
}

.resolution-group {
  display: grid;
  gap: 8px;
  width: 100%;
}

.resolution-group :deep(.el-radio) {
  margin: 0;
  height: auto;
  width: 100%;
  padding: 10px 12px;
  align-items: flex-start;
  white-space: normal;
}

.radio-title {
  display: block;
  font-weight: 600;
  font-size: 13px;
}

.radio-desc {
  display: block;
  margin-top: 2px;
  color: var(--ce-color-muted);
  font-size: 12px;
  line-height: 1.4;
  font-weight: 400;
}

@media (max-width: 720px) {
  .filters {
    flex-direction: column;
  }

  .filters :deep(.el-select) {
    width: 100% !important;
  }
}
</style>
