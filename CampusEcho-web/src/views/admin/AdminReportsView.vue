<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { RefreshRight } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  fetchAdminReport,
  fetchAdminReports,
  rejectAdminReport,
  resolveAdminReport,
  restoreBlockedItem,
} from '../../api/reports'
import type {
  ReportAction,
  ReportReason,
  ReportResponse,
  ReportStatus,
  ReportTargetType,
} from '../../api/types'
import { useAuthStore } from '../../stores/auth'
import { getErrorMessage } from '../../utils/error'
import { formatDateTime, formatRelativeTime } from '../../utils/format'
import {
  actionsForTarget,
  reportActionLabel,
  reportReasonLabel,
  reportStatusLabel,
  reportTargetLabel,
} from '../../utils/report'
import EmptyState from '../../components/EmptyState.vue'
import ProblemAlert from '../../components/ProblemAlert.vue'

const PAGE_SIZE = 20
const auth = useAuthStore()
const router = useRouter()

const targetType = ref<'ALL' | ReportTargetType>('ALL')
const reason = ref<'ALL' | ReportReason>('ALL')
const status = ref<'ALL' | ReportStatus>('PENDING')
const page = ref(1)

const items = ref<ReportResponse[]>([])
const totalElements = ref(0)
const totalPages = ref(0)
const loading = ref(true)
const errorMessage = ref('')
const actingId = ref<number | null>(null)

const detailVisible = ref(false)
const detailLoading = ref(false)
const detailError = ref('')
const detail = ref<ReportResponse | null>(null)

const resolveNote = ref('')
const resolveAction = ref<ReportAction>('NONE')
const rejectNote = ref('')
const restoreItemId = ref<number | null>(null)
const restoreReason = ref('')

const summary = computed(() => {
  if (loading.value) return '加载中…'
  if (errorMessage.value) return '加载失败'
  return `共 ${totalElements.value} 条举报`
})

const actionOptions = computed(() =>
  detail.value ? actionsForTarget(detail.value.targetType) : [],
)

async function loadReports() {
  if (!auth.isAdmin) {
    loading.value = false
    return
  }
  loading.value = true
  errorMessage.value = ''
  try {
    const result = await fetchAdminReports({
      targetType: targetType.value === 'ALL' ? undefined : targetType.value,
      reason: reason.value === 'ALL' ? undefined : reason.value,
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
    errorMessage.value = getErrorMessage(error, '无法加载举报列表')
  } finally {
    loading.value = false
  }
}

function replaceReport(updated: ReportResponse) {
  items.value = items.value.map((row) => (row.id === updated.id ? updated : row))
  if (detail.value?.id === updated.id) {
    detail.value = updated
  }
}

async function openDetail(reportId: number) {
  detailVisible.value = true
  detailLoading.value = true
  detailError.value = ''
  detail.value = null
  resolveNote.value = ''
  rejectNote.value = ''
  restoreReason.value = ''
  restoreItemId.value = null
  try {
    detail.value = await fetchAdminReport(reportId)
    resolveAction.value =
      detail.value.targetType === 'ITEM' ? 'BLOCK_ITEM' : 'SUSPEND_USER'
    if (detail.value.targetType === 'ITEM') {
      restoreItemId.value = detail.value.targetId
    }
  } catch (error) {
    detailError.value = getErrorMessage(error, '无法加载举报详情')
  } finally {
    detailLoading.value = false
  }
}

async function handleResolve() {
  if (!detail.value || actingId.value !== null) return
  const note = resolveNote.value.trim()
  if (note.length < 2) {
    ElMessage.warning('处理说明至少 2 个字符')
    return
  }

  try {
    await ElMessageBox.confirm(
      `确认以「${reportActionLabel(resolveAction.value)}」处理该举报？相关处罚将立即生效。`,
      '处理举报',
      { type: 'warning', confirmButtonText: '确认处理', cancelButtonText: '取消' },
    )
  } catch {
    return
  }

  actingId.value = detail.value.id
  try {
    const updated = await resolveAdminReport(detail.value.id, {
      action: resolveAction.value,
      resolutionNote: note,
    })
    replaceReport(updated)
    ElMessage.success('举报已处理')
    await loadReports()
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '处理失败'))
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
    await ElMessageBox.confirm('确认驳回该举报？', '驳回举报', {
      type: 'warning',
      confirmButtonText: '驳回',
      cancelButtonText: '取消',
    })
  } catch {
    return
  }

  actingId.value = detail.value.id
  try {
    const updated = await rejectAdminReport(detail.value.id, { resolutionNote: note })
    replaceReport(updated)
    ElMessage.success('已驳回')
    await loadReports()
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '驳回失败'))
  } finally {
    actingId.value = null
  }
}

async function handleRestore() {
  if (!restoreItemId.value || actingId.value !== null) return
  const reasonText = restoreReason.value.trim()
  if (reasonText.length < 2) {
    ElMessage.warning('恢复说明至少 2 个字符')
    return
  }

  try {
    await ElMessageBox.confirm(
      '解除屏蔽后物品保持下架，需卖家自行重新上架。确认继续？',
      '恢复物品',
      { type: 'warning', confirmButtonText: '恢复', cancelButtonText: '取消' },
    )
  } catch {
    return
  }

  actingId.value = restoreItemId.value
  try {
    await restoreBlockedItem(restoreItemId.value, { reason: reasonText })
    ElMessage.success('物品已解除屏蔽（保持下架）')
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '恢复失败'))
  } finally {
    actingId.value = null
  }
}

function handlePageChange(next: number) {
  page.value = next
  void loadReports()
}

watch([targetType, reason, status], () => {
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
        <p class="ce-eyebrow">管理后台</p>
        <h1>举报处理</h1>
        <p class="ce-page-desc">
          筛选并处理物品/用户举报。可驳回、屏蔽物品、停用用户或组合处罚；也可单独恢复被屏蔽物品。
        </p>
      </div>
      <el-button @click="router.push({ name: 'admin-dashboard' })">管理概览</el-button>
    </header>

    <EmptyState
      v-if="!auth.isAuthenticated"
      title="请先登录"
      description="举报处理需要管理员账户。"
    >
      <el-button
        type="primary"
        @click="router.push({ name: 'login', query: { redirect: '/admin/reports' } })"
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
        <el-select v-model="targetType" style="width: 140px">
          <el-option label="全部目标" value="ALL" />
          <el-option label="物品" value="ITEM" />
          <el-option label="用户" value="USER" />
        </el-select>
        <el-select v-model="status" style="width: 140px">
          <el-option label="全部状态" value="ALL" />
          <el-option label="待处理" value="PENDING" />
          <el-option label="已处理" value="RESOLVED" />
          <el-option label="已驳回" value="REJECTED" />
        </el-select>
        <el-select v-model="reason" style="width: 180px">
          <el-option label="全部原因" value="ALL" />
          <el-option label="疑似诈骗" value="FRAUD" />
          <el-option label="违禁物品" value="PROHIBITED_ITEM" />
          <el-option label="描述失实" value="MISLEADING_INFORMATION" />
          <el-option label="骚扰辱骂" value="HARASSMENT" />
          <el-option label="冒充他人" value="IMPERSONATION" />
          <el-option label="其他" value="OTHER" />
        </el-select>
      </div>

      <div class="ce-result-bar">
        <p>{{ summary }}</p>
        <el-button text :icon="RefreshRight" :loading="loading" @click="loadReports">
          刷新
        </el-button>
      </div>

      <ProblemAlert
        v-if="errorMessage"
        title="加载失败"
        :message="errorMessage"
        @close="errorMessage = ''"
      />

      <div v-if="loading" class="ce-panel loading-box">加载举报列表…</div>

      <EmptyState
        v-else-if="items.length === 0 && !errorMessage"
        title="没有匹配的举报"
        description="试试调整筛选条件。"
      />

      <div v-else class="table-wrap">
        <table class="report-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>目标</th>
              <th>原因</th>
              <th>举报人</th>
              <th>状态</th>
              <th>时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="report in items" :key="report.id">
              <td>#{{ report.id }}</td>
              <td>
                <strong>
                  {{ reportTargetLabel(report.targetType) }} ·
                  {{ report.targetLabel || `#${report.targetId}` }}
                </strong>
              </td>
              <td>{{ reportReasonLabel(report.reason) }}</td>
              <td>{{ report.reporterUsername || report.reporterId }}</td>
              <td>
                <span class="status" :class="`status-${report.status.toLowerCase()}`">
                  {{ reportStatusLabel(report.status) }}
                </span>
              </td>
              <td :title="formatDateTime(report.createdAt)">
                {{ formatRelativeTime(report.createdAt) }}
              </td>
              <td>
                <el-button size="small" type="primary" plain @click="openDetail(report.id)">
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
      title="举报处理"
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
            <dt>目标</dt>
            <dd>
              {{ reportTargetLabel(detail.targetType) }} ·
              {{ detail.targetLabel || `#${detail.targetId}` }}
            </dd>
          </div>
          <div>
            <dt>原因</dt>
            <dd>{{ reportReasonLabel(detail.reason) }}</dd>
          </div>
          <div>
            <dt>举报人</dt>
            <dd>{{ detail.reporterUsername || detail.reporterId }}</dd>
          </div>
          <div>
            <dt>状态</dt>
            <dd>{{ reportStatusLabel(detail.status) }}</dd>
          </div>
          <div>
            <dt>说明</dt>
            <dd class="pre">{{ detail.description }}</dd>
          </div>
        </dl>

        <template v-if="detail.status === 'PENDING'">
          <section class="action-block">
            <h3>处理并处罚</h3>
            <el-select v-model="resolveAction" style="width: 100%">
              <el-option
                v-for="item in actionOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
            <el-input
              v-model="resolveNote"
              type="textarea"
              :rows="3"
              maxlength="1000"
              show-word-limit
              placeholder="处理说明（2-1000 字）"
              style="margin-top: 10px"
            />
            <el-button
              type="primary"
              style="margin-top: 10px"
              :loading="actingId === detail.id"
              @click="handleResolve"
            >
              确认处理
            </el-button>
          </section>

          <section class="action-block">
            <h3>驳回举报</h3>
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
              驳回
            </el-button>
          </section>
        </template>

        <section v-else class="action-block">
          <h3>已处理信息</h3>
          <p>动作：{{ reportActionLabel(detail.action) }}</p>
          <p v-if="detail.handlerUsername">处理人：{{ detail.handlerUsername }}</p>
          <p v-if="detail.handledAt">时间：{{ formatDateTime(detail.handledAt) }}</p>
          <p v-if="detail.resolutionNote" class="pre">{{ detail.resolutionNote }}</p>
        </section>

        <section v-if="detail.targetType === 'ITEM'" class="action-block">
          <h3>恢复被屏蔽物品</h3>
          <p class="muted">物品 ID：{{ restoreItemId ?? detail.targetId }}（恢复后保持下架）</p>
          <el-input
            v-model="restoreReason"
            type="textarea"
            :rows="2"
            maxlength="1000"
            show-word-limit
            placeholder="恢复说明（2-1000 字）"
          />
          <el-button
            type="success"
            plain
            style="margin-top: 10px"
            :loading="actingId === (restoreItemId ?? detail.targetId)"
            @click="handleRestore"
          >
            解除屏蔽
          </el-button>
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
  box-shadow: var(--ce-shadow-sm);
}

.report-table {
  width: 100%;
  border-collapse: collapse;
  min-width: 860px;
}

.report-table th,
.report-table td {
  padding: 12px 14px;
  border-bottom: 1px solid var(--ce-color-border);
  text-align: left;
  font-size: 13px;
  vertical-align: middle;
}

.report-table th {
  background: #f9fafb;
  color: var(--ce-color-muted);
  font-size: 12px;
}

.report-table tr:last-child td {
  border-bottom: 0;
}

.status {
  display: inline-flex;
  min-height: 22px;
  padding: 0 8px;
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

.detail-meta {
  margin: 0 0 16px;
  display: grid;
  gap: 10px;
}

.detail-meta > div {
  display: grid;
  grid-template-columns: 72px 1fr;
  gap: 8px;
  font-size: 14px;
}

.detail-meta dt {
  color: var(--ce-color-muted);
}

.detail-meta dd {
  margin: 0;
  word-break: break-word;
}

.pre {
  white-space: pre-wrap;
  line-height: 1.55;
}

.action-block {
  padding: 14px 0;
  border-top: 1px solid var(--ce-color-border);
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

.muted {
  color: var(--ce-color-muted);
}

@media (max-width: 960px) {
  .filters {
    flex-direction: column;
  }

  .filters .el-select {
    width: 100% !important;
  }
}
</style>
