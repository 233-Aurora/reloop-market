<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { createItem } from '../../api/items'
import type { ItemCreateRequest, ItemImageResponse, ItemResponse } from '../../api/types'
import { getErrorMessage, getFieldErrors } from '../../utils/error'
import ItemForm from '../../components/ItemForm.vue'
import ItemImageManager from '../../components/ItemImageManager.vue'

const router = useRouter()

const step = ref<'form' | 'images'>('form')
const submitting = ref(false)
const formError = ref('')
const fieldErrors = ref<Record<string, string>>({})
const createdItem = ref<ItemResponse | null>(null)
const images = ref<ItemImageResponse[]>([])

function clearErrors() {
  formError.value = ''
  fieldErrors.value = {}
}

async function handleCreate(payload: ItemCreateRequest) {
  if (submitting.value) return
  submitting.value = true
  clearErrors()
  try {
    const item = await createItem(payload)
    createdItem.value = item
    images.value = item.images ?? []
    step.value = 'images'
    ElMessage.success('物品已发布，请继续上传图片')
  } catch (error) {
    fieldErrors.value = getFieldErrors(error)
    formError.value = getErrorMessage(error, '发布失败，请检查填写信息')
  } finally {
    submitting.value = false
  }
}

function finish() {
  if (!createdItem.value) return
  void router.push({ name: 'item-detail', params: { id: createdItem.value.id } })
}
</script>

<template>
  <section class="page">
    <header class="page-header">
      <div>
        <p class="eyebrow">{{ step === 'form' ? '步骤 1 / 2' : '步骤 2 / 2' }}</p>
        <h1>{{ step === 'form' ? '发布物品' : '管理图片' }}</h1>
        <p class="desc">
          <template v-if="step === 'form'">
            填写物品信息后立即上架。创建成功后可继续上传图片。
          </template>
          <template v-else>
            图片可稍后补充。建议至少上传 1 张清晰主图。
          </template>
        </p>
      </div>
    </header>

    <div v-if="step === 'form'" class="panel">
      <ItemForm
        submit-label="发布物品"
        :loading="submitting"
        :form-error="formError"
        :field-errors="fieldErrors"
        @submit="handleCreate"
        @clear-error="clearErrors"
      >
        <template #actions>
          <el-button type="primary" native-type="submit" :loading="submitting">
            发布物品
          </el-button>
          <el-button :disabled="submitting" @click="router.back()">取消</el-button>
        </template>
      </ItemForm>
    </div>

    <template v-else-if="createdItem">
      <div class="success-banner">
        <strong>「{{ createdItem.title }}」已发布</strong>
        <span>当前状态：在售。可继续上传图片，或稍后在编辑页补充。</span>
      </div>

      <ItemImageManager
        :item-id="createdItem.id"
        :images="images"
        @change="images = $event"
      />

      <div class="finish-actions">
        <el-button type="primary" @click="finish">查看物品详情</el-button>
        <el-button @click="router.push({ name: 'item-create' })">继续发布</el-button>
      </div>
    </template>
  </section>
</template>

<style scoped>
.page {
  display: grid;
  gap: 20px;
  max-width: 720px;
}

.page-header h1 {
  margin-bottom: 8px;
}

.eyebrow {
  margin: 0 0 6px;
  color: var(--ce-color-primary);
  font-size: 13px;
  font-weight: 600;
}

.desc {
  margin: 0;
  color: var(--ce-color-muted);
  font-size: 14px;
  line-height: 1.6;
}

.panel {
  padding: 24px;
  background: var(--ce-color-surface);
  border: 1px solid var(--ce-color-border);
  border-radius: var(--ce-radius);
}

.success-banner {
  display: grid;
  gap: 4px;
  padding: 14px 16px;
  background: var(--ce-color-primary-soft);
  border-left: 4px solid var(--ce-color-primary);
  border-radius: 0 var(--ce-radius) var(--ce-radius) 0;
  font-size: 14px;
  line-height: 1.5;
}

.success-banner span {
  color: var(--ce-color-muted);
}

.finish-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}
</style>
