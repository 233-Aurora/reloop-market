<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, RefreshRight } from '@element-plus/icons-vue'
import { fetchMyDispute } from '../../api/disputes'
import { ApiError, type DisputeResponse } from '../../api/types'
import { useAuthStore } from '../../stores/auth'
import {
  disputeReasonLabel,
  disputeResolutionLabel,
  disputeStatusLabel,
} from '../../utils/dispute'
import { getErrorMessage } from '../../utils/error'
import { formatDateTime } from '../../utils/format'
import EmptyState from '../../components/EmptyState.vue'
import ProblemAlert from '../../components/ProblemAlert.vue'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const dispute = ref<DisputeResponse | null>(null)
const loading = ref(true)
const errorMessage = ref('')
const notFound = ref(false)

const disputeId = computed(() => {
  const raw = route.params.id
  const value = Array.isArray(raw) ? raw[0] : raw
  if (typeof value !== 'string' || !/^\d+$/.test(value)) return null
  return Number(value)
})

const myRole = computed(() => {
  if (!dispute.value || !auth.user) return ''
  if (dispute.value.openerId === auth.user.id) return '你是发起人'
  if (dispute.value.respondentId === auth.user.id) return '你是被申请方'
  return '参与者'
})

async function loadDetail() {
  if (disputeId.value === null) {
    loading.value = false
    notFound.value = true
    return
  }
  loading.value = true
  errorMessage.value = ''
  notFound.value = false
  try {
    dispute.value = await fetchMyDispute(disputeId.value)
    document.title = `争议 #${dispute.value.id} · ReLoop Market`
  } catch (error) {
    dispute.value = null
    if (error instanceof ApiError && (error.status === 404 || error.status === 403)) {
      notFound.value = true
    } else {
      errorMessage.value = getErrorMessage(error, '无法加载争议详情')
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
  <section class="ce-page">
    <button type="button" class="ce-back-btn" @click="router.push({ name: 'my-disputes' })">
      <el-icon><ArrowLeft /></el-icon>
      <span>返回争议列表</span>
    </button>

    <div v-if="loading" class="ce-panel loading-box">加载争议详情…</div>

    <EmptyState
      v-else-if="notFound"
      title="争议不存在或无权查看"
      description="只能查看自己参与的交易争议。"
    >
      <el-button type="primary" @click="router.push({ name: 'my-disputes' })">我的争议</el-button>
    </EmptyState>

    <template v-else-if="errorMessage">
      <ProblemAlert title="加载失败" :message="errorMessage" @close="errorMessage = ''" />
      <EmptyState title="无法加载争议详情" :description="errorMessage">
        <el-button type="primary" @click="loadDetail">重试</el-button>
      </EmptyState>
    </template>

    <template v-else-if="dispute">
      <header class="ce-page-header">
        <div>
          <p class="ce-eyebrow">争议 #{{ dispute.id }}</p>
          <h1>{{ dispute.itemTitle || `物品 #${dispute.itemId}` }}</h1>
          <p class="ce-page-desc">{{ myRole }} · {{ disputeReasonLabel(dispute.reason) }}</p>
        </div>
        <div class="header-actions">
          <span class="status" :class="`status-${dispute.status.toLowerCase()}`">
            {{ disputeStatusLabel(dispute.status) }}
          </span>
          <el-button text :icon="RefreshRight" @click="loadDetail">刷新</el-button>
        </div>
      </header>

      <div class="layout">
        <section class="ce-panel">
          <h2>争议内容</h2>
          <dl class="meta-list">
            <div>
              <dt>关联交易</dt>
              <dd>
                <RouterLink :to="{ name: 'trade-detail', params: { id: dispute.tradeRequestId } }">
                  交易 #{{ dispute.tradeRequestId }}
                </RouterLink>
              </dd>
            </div>
            <div>
              <dt>关联物品</dt>
              <dd>
                <RouterLink :to="{ name: 'item-detail', params: { id: dispute.itemId } }">
                  {{ dispute.itemTitle || `物品 #${dispute.itemId}` }}
                </RouterLink>
              </dd>
            </div>
            <div>
              <dt>发起人</dt>
              <dd>{{ dispute.openerUsername || dispute.openerId }}</dd>
            </div>
            <div>
              <dt>被申请方</dt>
              <dd>{{ dispute.respondentUsername || dispute.respondentId }}</dd>
            </div>
            <div>
              <dt>创建时间</dt>
              <dd>{{ formatDateTime(dispute.createdAt) }}</dd>
            </div>
          </dl>
          <div class="description-box">
            <h3>说明</h3>
            <p>{{ dispute.description }}</p>
          </div>
        </section>

        <section class="ce-panel">
          <h2>仲裁结果</h2>
          <template v-if="dispute.status === 'OPEN'">
            <p class="hint">管理员尚未处理。交易确认完成已冻结，请耐心等待。</p>
          </template>
          <template v-else>
            <dl class="meta-list">
              <div>
                <dt>状态</dt>
                <dd>{{ disputeStatusLabel(dispute.status) }}</dd>
              </div>
              <div>
                <dt>结果</dt>
                <dd>{{ disputeResolutionLabel(dispute.resolution) }}</dd>
              </div>
              <div>
                <dt>处理人</dt>
                <dd>{{ dispute.handlerUsername || dispute.handledBy || '—' }}</dd>
              </div>
              <div>
                <dt>处理时间</dt>
                <dd>{{ formatDateTime(dispute.handledAt) }}</dd>
              </div>
            </dl>
            <div v-if="dispute.resolutionNote" class="description-box">
              <h3>处理说明</h3>
              <p>{{ dispute.resolutionNote }}</p>
            </div>
            <p v-if="dispute.status === 'REJECTED'" class="hint">
              争议已驳回，交易恢复为「已接受」，双方可继续确认完成。
            </p>
          </template>
          <el-button
            style="margin-top: 12px"
            @click="router.push({ name: 'trade-detail', params: { id: dispute.tradeRequestId } })"
          >
            查看交易
          </el-button>
        </section>
      </div>
    </template>
  </section>
</template>

<style scoped>
.loading-box {
  text-align: center;
  color: var(--ce-color-muted);
  padding: 28px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.layout {
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) minmax(0, 0.8fr);
  gap: 16px;
  align-items: start;
}

.ce-panel h2 {
  margin: 0 0 14px;
  font-size: 16px;
}

.meta-list {
  margin: 0;
  display: grid;
  gap: 10px;
}

.meta-list > div {
  display: grid;
  grid-template-columns: 88px 1fr;
  gap: 8px;
  font-size: 14px;
  line-height: 1.5;
}

.meta-list dt {
  color: var(--ce-color-muted);
}

.meta-list dd {
  margin: 0;
  min-width: 0;
  word-break: break-word;
}

.meta-list a {
  color: var(--ce-color-primary);
  font-weight: 600;
}

.description-box {
  margin-top: 14px;
  padding-top: 14px;
  border-top: 1px solid var(--ce-color-border);
}

.description-box h3 {
  margin: 0 0 8px;
  font-size: 14px;
}

.description-box p {
  margin: 0;
  white-space: pre-wrap;
  line-height: 1.65;
  font-size: 14px;
}

.hint {
  margin: 0;
  color: var(--ce-color-muted);
  font-size: 13px;
  line-height: 1.5;
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

@media (max-width: 860px) {
  .layout {
    grid-template-columns: 1fr;
  }
}
</style>
