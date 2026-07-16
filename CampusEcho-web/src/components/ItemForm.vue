<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { fetchCategoryTree } from '../api/categories'
import type { CategoryTreeResponse, ItemCondition, ItemCreateRequest } from '../api/types'
import { ITEM_CONDITIONS } from '../utils/item'
import { toCategoryOptions, type CategoryOption } from '../utils/category'
import { getErrorMessage } from '../utils/error'
import ProblemAlert from './ProblemAlert.vue'

const props = withDefaults(
  defineProps<{
    modelValue?: Partial<ItemCreateRequest>
    submitLabel?: string
    loading?: boolean
    disabled?: boolean
    fieldErrors?: Record<string, string>
    formError?: string
  }>(),
  {
    submitLabel: '保存',
    loading: false,
    disabled: false,
    fieldErrors: () => ({}),
    formError: '',
  },
)

const emit = defineEmits<{
  'update:modelValue': [value: ItemCreateRequest]
  submit: [value: ItemCreateRequest]
  'clear-error': []
}>()

const formRef = ref<FormInstance>()
const categories = ref<CategoryTreeResponse[]>([])
const categoryOptions = ref<CategoryOption[]>([])
const categoriesLoading = ref(false)
const categoriesError = ref('')

const form = reactive({
  categoryId: undefined as number | undefined,
  title: '',
  description: '',
  price: undefined as number | undefined,
  condition: 'GOOD' as ItemCondition,
  locationText: '',
})

const rules: FormRules = {
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }],
  title: [
    { required: true, message: '请输入标题', trigger: 'blur' },
    { max: 120, message: '标题最多 120 个字符', trigger: 'blur' },
  ],
  description: [
    { required: true, message: '请输入描述', trigger: 'blur' },
    { max: 10000, message: '描述最多 10000 个字符', trigger: 'blur' },
  ],
  price: [
    { required: true, message: '请输入价格', trigger: 'blur' },
    {
      type: 'number',
      min: 0,
      message: '价格不能小于 0',
      trigger: 'blur',
    },
  ],
  condition: [{ required: true, message: '请选择成色', trigger: 'change' }],
  locationText: [{ max: 255, message: '地点最多 255 个字符', trigger: 'blur' }],
}

function applyModel(value?: Partial<ItemCreateRequest>) {
  if (!value) return
  form.categoryId = value.categoryId
  form.title = value.title ?? ''
  form.description = value.description ?? ''
  form.price = value.price
  form.condition = value.condition ?? 'GOOD'
  form.locationText = value.locationText ?? ''
}

function buildPayload(): ItemCreateRequest | null {
  if (form.categoryId === undefined || form.price === undefined) return null
  return {
    categoryId: form.categoryId,
    title: form.title.trim(),
    description: form.description.trim(),
    price: Number(form.price),
    condition: form.condition,
    locationText: form.locationText.trim() || null,
  }
}

async function loadCategories() {
  categoriesLoading.value = true
  categoriesError.value = ''
  try {
    categories.value = await fetchCategoryTree()
    categoryOptions.value = toCategoryOptions(categories.value)
  } catch (error) {
    categoriesError.value = getErrorMessage(error, '无法加载分类')
    categoryOptions.value = []
  } finally {
    categoriesLoading.value = false
  }
}

async function handleSubmit() {
  if (!formRef.value || props.loading || props.disabled) return
  emit('clear-error')
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  const payload = buildPayload()
  if (!payload) return
  emit('update:modelValue', payload)
  emit('submit', payload)
}

watch(
  () => props.modelValue,
  (value) => applyModel(value),
  { immediate: true, deep: true },
)

onMounted(() => {
  void loadCategories()
})

defineExpose({
  validate: () => formRef.value?.validate(),
  resetFields: () => formRef.value?.resetFields(),
})
</script>

<template>
  <el-form
    ref="formRef"
    class="item-form"
    :model="form"
    :rules="rules"
    label-position="top"
    size="large"
    :disabled="disabled || loading"
    @submit.prevent="handleSubmit"
  >
    <ProblemAlert
      v-if="formError"
      title="提交失败"
      :message="formError"
      @close="emit('clear-error')"
    />

    <ProblemAlert
      v-if="categoriesError"
      title="分类加载失败"
      :message="categoriesError"
      @close="categoriesError = ''"
    />

    <el-form-item label="分类" prop="categoryId" :error="fieldErrors.categoryId">
      <el-tree-select
        v-model="form.categoryId"
        :data="categoryOptions"
        :loading="categoriesLoading"
        check-strictly
        filterable
        clearable
        placeholder="选择物品分类"
        style="width: 100%"
        :props="{ label: 'label', value: 'value', children: 'children' }"
        @change="emit('clear-error')"
      />
    </el-form-item>

    <el-form-item label="标题" prop="title" :error="fieldErrors.title">
      <el-input
        v-model="form.title"
        maxlength="120"
        show-word-limit
        placeholder="简洁描述物品，例如：九成新高等数学教材"
        @input="emit('clear-error')"
      />
    </el-form-item>

    <el-form-item label="价格（元）" prop="price" :error="fieldErrors.price">
      <el-input-number
        v-model="form.price"
        :min="0"
        :max="9999999999.99"
        :precision="2"
        :step="1"
        controls-position="right"
        style="width: 100%"
        @change="emit('clear-error')"
      />
    </el-form-item>

    <el-form-item label="成色" prop="condition" :error="fieldErrors.condition">
      <el-select
        v-model="form.condition"
        placeholder="选择成色"
        style="width: 100%"
        @change="emit('clear-error')"
      >
        <el-option
          v-for="item in ITEM_CONDITIONS"
          :key="item.value"
          :label="item.label"
          :value="item.value"
        />
      </el-select>
    </el-form-item>

    <el-form-item label="交易地点（可选）" prop="locationText" :error="fieldErrors.locationText">
      <el-input
        v-model="form.locationText"
        maxlength="255"
        show-word-limit
        placeholder="例如：主校区图书馆门口"
        @input="emit('clear-error')"
      />
    </el-form-item>

    <el-form-item label="描述" prop="description" :error="fieldErrors.description">
      <el-input
        v-model="form.description"
        type="textarea"
        :rows="6"
        maxlength="10000"
        show-word-limit
        placeholder="补充使用情况、入手渠道、可议价信息等"
        @input="emit('clear-error')"
      />
    </el-form-item>

    <div class="form-actions">
      <slot name="actions">
        <el-button type="primary" native-type="submit" :loading="loading" :disabled="disabled">
          {{ submitLabel }}
        </el-button>
      </slot>
    </div>
  </el-form>
</template>

<style scoped>
.item-form {
  display: grid;
  gap: 4px;
}

.form-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  padding-top: 8px;
}
</style>
