<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { createReport } from '../api/reports'
import type { ReportReason, ReportTargetType } from '../api/types'
import { getErrorMessage, getFieldErrors } from '../utils/error'
import { reasonsForTarget, reportTargetLabel } from '../utils/report'
import ProblemAlert from './ProblemAlert.vue'

const props = defineProps<{
  modelValue: boolean
  targetType: ReportTargetType
  targetId: number
  targetLabel?: string
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  submitted: []
}>()

const visible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value),
})

const submitting = ref(false)
const errorMessage = ref('')
const fieldErrors = ref<Record<string, string>>({})

const form = reactive({
  reason: '' as ReportReason | '',
  description: '',
})

const reasonOptions = computed(() => reasonsForTarget(props.targetType))

function resetForm() {
  form.reason = ''
  form.description = ''
  errorMessage.value = ''
  fieldErrors.value = {}
}

watch(
  () => props.modelValue,
  (open) => {
    if (open) resetForm()
  },
)

async function handleSubmit() {
  if (submitting.value) return
  errorMessage.value = ''
  fieldErrors.value = {}

  if (!form.reason) {
    fieldErrors.value = { reason: '请选择举报原因' }
    return
  }
  const description = form.description.trim()
  if (description.length < 10 || description.length > 1000) {
    fieldErrors.value = { description: '举报说明长度必须在 10 到 1000 个字符之间' }
    return
  }

  submitting.value = true
  try {
    await createReport({
      targetType: props.targetType,
      targetId: props.targetId,
      reason: form.reason,
      description,
    })
    ElMessage.success('举报已提交，管理员将尽快处理')
    visible.value = false
    emit('submitted')
  } catch (error) {
    fieldErrors.value = getFieldErrors(error)
    errorMessage.value = getErrorMessage(error, '提交举报失败')
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <el-dialog
    v-model="visible"
    :title="`举报${reportTargetLabel(targetType)}`"
    width="min(480px, 92vw)"
    destroy-on-close
  >
    <p class="lead">
      目标：
      <strong>{{ targetLabel || `${reportTargetLabel(targetType)} #${targetId}` }}</strong>
    </p>
    <p class="hint">请客观描述问题。同一目标在待处理期间不可重复举报。</p>

    <ProblemAlert
      v-if="errorMessage"
      title="提交失败"
      :message="errorMessage"
      @close="errorMessage = ''"
    />

    <el-form label-position="top" size="large" @submit.prevent="handleSubmit">
      <el-form-item label="举报原因" required :error="fieldErrors.reason">
        <el-select v-model="form.reason" placeholder="请选择原因" style="width: 100%">
          <el-option
            v-for="item in reasonOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
      </el-form-item>

      <el-form-item label="详细说明" required :error="fieldErrors.description">
        <el-input
          v-model="form.description"
          type="textarea"
          :rows="5"
          maxlength="1000"
          show-word-limit
          placeholder="请说明具体情况，至少 10 个字符"
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button :disabled="submitting" @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="handleSubmit">提交举报</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.lead {
  margin: 0 0 6px;
  font-size: 14px;
  line-height: 1.5;
}

.hint {
  margin: 0 0 14px;
  color: var(--ce-color-muted);
  font-size: 13px;
  line-height: 1.5;
}
</style>
