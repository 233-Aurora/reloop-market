<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { createDispute } from '../api/disputes'
import type { DisputeReason, DisputeResponse, TradeRequestResponse } from '../api/types'
import { DISPUTE_REASONS } from '../utils/dispute'
import { getErrorMessage } from '../utils/error'

const props = defineProps<{
  modelValue: boolean
  trade: TradeRequestResponse
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  submitted: [dispute: DisputeResponse]
}>()

const reason = ref<DisputeReason>('ITEM_NOT_AS_DESCRIBED')
const description = ref('')
const submitting = ref(false)
const errorMessage = ref('')

const visible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value),
})

const charCount = computed(() => description.value.trim().length)

watch(
  () => props.modelValue,
  (open) => {
    if (open) {
      reason.value = 'ITEM_NOT_AS_DESCRIBED'
      description.value = ''
      errorMessage.value = ''
    }
  },
)

async function handleSubmit() {
  const text = description.value.trim()
  if (text.length < 10) {
    ElMessage.warning('争议说明至少 10 个字符')
    return
  }
  if (text.length > 1000) {
    ElMessage.warning('争议说明不能超过 1000 个字符')
    return
  }

  submitting.value = true
  errorMessage.value = ''
  try {
    const dispute = await createDispute({
      tradeRequestId: props.trade.id,
      reason: reason.value,
      description: text,
    })
    ElMessage.success('争议已提交，等待管理员仲裁')
    emit('submitted', dispute)
    visible.value = false
  } catch (error) {
    errorMessage.value = getErrorMessage(error, '提交争议失败')
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <el-dialog
    v-model="visible"
    title="发起交易争议"
    width="min(480px, 96vw)"
    destroy-on-close
    :close-on-click-modal="!submitting"
  >
    <p class="hint">
      仅用于已接受交易的履约纠纷（未交付、货不对板、付款/爽约等）。内容违规请走「举报」，不要混用。
    </p>
    <p class="trade-line">
      交易 #{{ trade.id }} · {{ trade.itemTitle || `物品 #${trade.itemId}` }}
    </p>

    <div class="field">
      <label for="dispute-reason">争议原因</label>
      <el-select id="dispute-reason" v-model="reason" style="width: 100%">
        <el-option
          v-for="item in DISPUTE_REASONS"
          :key="item.value"
          :label="item.label"
          :value="item.value"
        />
      </el-select>
    </div>

    <div class="field">
      <label for="dispute-desc">详细说明（10–1000 字）</label>
      <el-input
        id="dispute-desc"
        v-model="description"
        type="textarea"
        :rows="5"
        maxlength="1000"
        show-word-limit
        placeholder="请客观描述发生了什么、约定是什么、当前卡在哪一步。"
      />
      <p class="count">已输入 {{ charCount }} 字</p>
    </div>

    <p v-if="errorMessage" class="error">{{ errorMessage }}</p>

    <template #footer>
      <el-button :disabled="submitting" @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="handleSubmit">提交争议</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.hint {
  margin: 0 0 12px;
  padding: 10px 12px;
  background: #fef0c7;
  border-radius: var(--ce-radius-sm);
  color: #93370d;
  font-size: 13px;
  line-height: 1.5;
}

.trade-line {
  margin: 0 0 14px;
  font-size: 14px;
  font-weight: 600;
}

.field {
  display: grid;
  gap: 6px;
  margin-bottom: 14px;
}

.field label {
  font-size: 13px;
  font-weight: 600;
  color: var(--ce-color-ink);
}

.count {
  margin: 0;
  color: var(--ce-color-muted);
  font-size: 12px;
}

.error {
  margin: 0;
  color: var(--ce-color-danger);
  font-size: 13px;
}
</style>
