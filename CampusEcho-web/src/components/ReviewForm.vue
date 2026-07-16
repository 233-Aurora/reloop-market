<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { createReview } from '../api/reviews'
import type { ReviewResponse } from '../api/types'
import { getErrorMessage, getFieldErrors } from '../utils/error'
import ProblemAlert from './ProblemAlert.vue'

const props = defineProps<{
  tradeRequestId: number
  revieweeLabel?: string
}>()

const emit = defineEmits<{
  submitted: [review: ReviewResponse]
}>()

const submitting = ref(false)
const errorMessage = ref('')
const fieldErrors = ref<Record<string, string>>({})

const form = reactive({
  rating: 5,
  comment: '',
})

function clearErrors() {
  errorMessage.value = ''
  fieldErrors.value = {}
}

async function handleSubmit() {
  if (submitting.value) return
  clearErrors()

  if (!form.rating || form.rating < 1 || form.rating > 5) {
    fieldErrors.value = { rating: '请选择 1-5 星评分' }
    return
  }

  submitting.value = true
  try {
    const review = await createReview(props.tradeRequestId, {
      rating: form.rating,
      comment: form.comment.trim() || null,
    })
    ElMessage.success('评价已提交')
    emit('submitted', review)
    form.comment = ''
    form.rating = 5
  } catch (error) {
    fieldErrors.value = getFieldErrors(error)
    errorMessage.value = getErrorMessage(error, '提交评价失败')
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <form class="review-form" @submit.prevent="handleSubmit">
    <h3>评价{{ revieweeLabel || '对方' }}</h3>
    <p class="hint">交易完成后可评价一次，评分将影响对方信誉分。</p>

    <ProblemAlert
      v-if="errorMessage"
      title="提交失败"
      :message="errorMessage"
      @close="errorMessage = ''"
    />

    <div class="field">
      <label>评分</label>
      <el-rate
        v-model="form.rating"
        show-text
        :texts="['很差', '较差', '一般', '满意', '非常满意']"
        :colors="['#f59e0b', '#f59e0b', '#f59e0b']"
        @change="clearErrors"
      />
      <p v-if="fieldErrors.rating" class="field-error">{{ fieldErrors.rating }}</p>
    </div>

    <div class="field">
      <label for="review-comment">文字评价（可选）</label>
      <el-input
        id="review-comment"
        v-model="form.comment"
        type="textarea"
        :rows="3"
        maxlength="1000"
        show-word-limit
        placeholder="例如：沟通顺畅，当面交易顺利"
        @input="clearErrors"
      />
      <p v-if="fieldErrors.comment" class="field-error">{{ fieldErrors.comment }}</p>
    </div>

    <el-button type="primary" native-type="submit" :loading="submitting">
      提交评价
    </el-button>
  </form>
</template>

<style scoped>
.review-form {
  display: grid;
  gap: 14px;
  padding: 16px;
  background: var(--ce-color-primary-soft);
  border: 1px solid #c5e3dc;
  border-radius: var(--ce-radius);
}

h3 {
  margin: 0;
  font-size: 15px;
}

.hint {
  margin: 0;
  color: var(--ce-color-muted);
  font-size: 13px;
  line-height: 1.5;
}

.field {
  display: grid;
  gap: 8px;
}

.field label {
  font-size: 13px;
  font-weight: 600;
  color: var(--ce-color-ink);
}

.field-error {
  margin: 0;
  color: var(--ce-color-danger);
  font-size: 12px;
}
</style>
