<script setup lang="ts">
import { computed, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  acceptTradeRequest,
  cancelTradeRequest,
  confirmTradeCompletion,
  rejectTradeRequest,
} from '../api/trades'
import type { DisputeResponse, TradeRequestResponse } from '../api/types'
import {
  canAccept,
  canCancel,
  canConfirmCompletion,
  canOpenDispute,
  canReject,
} from '../utils/trade'
import { getErrorMessage } from '../utils/error'
import DisputeDialog from './DisputeDialog.vue'

const props = defineProps<{
  trade: TradeRequestResponse
  userId: number
}>()

const emit = defineEmits<{
  updated: [trade: TradeRequestResponse]
  conflict: [message: string]
  disputeOpened: [dispute: DisputeResponse]
}>()

const acting = ref(false)
const disputeVisible = ref(false)

const showAccept = computed(() => canAccept(props.trade, props.userId))
const showReject = computed(() => canReject(props.trade, props.userId))
const showCancel = computed(() => canCancel(props.trade, props.userId))
const showConfirm = computed(() => canConfirmCompletion(props.trade, props.userId))
const showDispute = computed(() => canOpenDispute(props.trade, props.userId))
const hasActions = computed(
  () =>
    showAccept.value ||
    showReject.value ||
    showCancel.value ||
    showConfirm.value ||
    showDispute.value,
)

function onDisputeSubmitted(dispute: DisputeResponse) {
  emit('disputeOpened', dispute)
}

async function runAction(
  confirmText: string,
  title: string,
  action: () => Promise<TradeRequestResponse>,
  successText: string,
) {
  try {
    await ElMessageBox.confirm(confirmText, title, {
      type: 'warning',
      confirmButtonText: '确认',
      cancelButtonText: '取消',
    })
  } catch {
    return
  }

  acting.value = true
  try {
    const updated = await action()
    emit('updated', updated)
    ElMessage.success(successText)
  } catch (error) {
    const message = getErrorMessage(error, '操作失败')
    if (message.includes('状态') || message.includes('不允许')) {
      emit('conflict', message)
    } else {
      ElMessage.error(message)
    }
  } finally {
    acting.value = false
  }
}

function handleAccept() {
  return runAction(
    '接受后物品将变为「已预订」，其他待处理请求会被拒绝。确认接受吗？',
    '接受交易请求',
    () => acceptTradeRequest(props.trade.id),
    '已接受交易请求',
  )
}

function handleReject() {
  return runAction(
    '拒绝后买家将无法继续此请求。确认拒绝吗？',
    '拒绝交易请求',
    () => rejectTradeRequest(props.trade.id),
    '已拒绝交易请求',
  )
}

function handleCancel() {
  return runAction(
    '取消后需要重新发起交易请求。确认取消吗？',
    '取消交易请求',
    () => cancelTradeRequest(props.trade.id),
    '已取消交易请求',
  )
}

function handleConfirm() {
  return runAction(
    '请确认已完成当面交易。双方都确认后，物品将标记为已售出。',
    '确认交易完成',
    () => confirmTradeCompletion(props.trade.id),
    '已确认完成',
  )
}
</script>

<template>
  <div v-if="hasActions" class="action-bar">
    <el-button
      v-if="showAccept"
      type="primary"
      :loading="acting"
      @click="handleAccept"
    >
      接受请求
    </el-button>
    <el-button
      v-if="showReject"
      type="danger"
      plain
      :loading="acting"
      @click="handleReject"
    >
      拒绝
    </el-button>
    <el-button
      v-if="showCancel"
      :loading="acting"
      @click="handleCancel"
    >
      取消请求
    </el-button>
    <el-button
      v-if="showConfirm"
      type="primary"
      :loading="acting"
      @click="handleConfirm"
    >
      确认完成
    </el-button>
    <el-button
      v-if="showDispute"
      type="warning"
      plain
      :disabled="acting"
      @click="disputeVisible = true"
    >
      发起争议
    </el-button>
  </div>
  <p v-else-if="trade.status === 'DISPUTED'" class="no-action">
    交易处于争议中，确认完成已冻结，请等待管理员仲裁。
  </p>
  <p v-else class="no-action">当前状态没有可执行操作。</p>

  <DisputeDialog
    v-if="showDispute"
    v-model="disputeVisible"
    :trade="trade"
    @submitted="onDisputeSubmitted"
  />
</template>

<style scoped>
.action-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.no-action {
  margin: 0;
  color: var(--ce-color-muted);
  font-size: 13px;
}

@media (max-width: 720px) {
  .action-bar .el-button {
    flex: 1;
  }
}
</style>
