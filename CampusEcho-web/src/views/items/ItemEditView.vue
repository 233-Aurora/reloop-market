<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { updateItem } from '../../api/items'
import {
  ApiError,
  type ItemCreateRequest,
  type ItemImageResponse,
  type ItemResponse,
} from '../../api/types'
import { useAuthStore } from '../../stores/auth'
import { getErrorMessage, getFieldErrors } from '../../utils/error'
import {
  canEditItemContent,
  canManageImages,
  fetchOwnedItem,
} from '../../utils/item'
import { itemStatusLabel } from '../../utils/format'
import EmptyState from '../../components/EmptyState.vue'
import ItemForm from '../../components/ItemForm.vue'
import ItemImageManager from '../../components/ItemImageManager.vue'
import ItemStatusTag from '../../components/ItemStatusTag.vue'
import ProblemAlert from '../../components/ProblemAlert.vue'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const loading = ref(true)
const saving = ref(false)
const errorMessage = ref('')
const notFound = ref(false)
const forbidden = ref(false)
const formError = ref('')
const fieldErrors = ref<Record<string, string>>({})

const item = ref<ItemResponse | null>(null)
const images = ref<ItemImageResponse[]>([])
const formModel = ref<Partial<ItemCreateRequest>>({})

const itemId = computed(() => {
  const raw = route.params.id
  const value = Array.isArray(raw) ? raw[0] : raw
  if (typeof value !== 'string' || !/^\d+$/.test(value)) return null
  return Number(value)
})

const editable = computed(() =>
  item.value ? canEditItemContent(item.value.status, item.value.moderationStatus) : false,
)
const imagesEditable = computed(() =>
  item.value ? canManageImages(item.value.status, item.value.moderationStatus) : false,
)

function clearFormErrors() {
  formError.value = ''
  fieldErrors.value = {}
}

function applyItem(next: ItemResponse) {
  item.value = next
  images.value = next.images ?? []
  formModel.value = {
    categoryId: next.categoryId,
    title: next.title,
    description: next.description,
    price: Number(next.price),
    condition: next.condition,
    locationText: next.locationText,
  }
  document.title = `编辑 ${next.title} · CampusEcho`
}

async function loadItem() {
  if (itemId.value === null || !auth.user) {
    loading.value = false
    notFound.value = true
    return
  }

  loading.value = true
  errorMessage.value = ''
  notFound.value = false
  forbidden.value = false
  item.value = null

  try {
    const owned = await fetchOwnedItem(itemId.value, auth.user.id)
    applyItem(owned)
  } catch (error) {
    if (error instanceof ApiError && error.status === 404) {
      notFound.value = true
    } else if (error instanceof ApiError && error.status === 403) {
      forbidden.value = true
    } else {
      errorMessage.value = getErrorMessage(error, '无法加载物品')
    }
  } finally {
    loading.value = false
  }
}

async function handleSave(payload: ItemCreateRequest) {
  if (!item.value || saving.value || !editable.value) return
  saving.value = true
  clearFormErrors()
  try {
    const updated = await updateItem(item.value.id, payload)
    applyItem({ ...updated, images: images.value })
    ElMessage.success('物品信息已保存')
  } catch (error) {
    fieldErrors.value = getFieldErrors(error)
    formError.value = getErrorMessage(error, '保存失败')
  } finally {
    saving.value = false
  }
}

function onImagesChange(next: ItemImageResponse[]) {
  images.value = next
  if (item.value) {
    item.value = { ...item.value, images: next }
  }
}

watch(
  () => [route.params.id, auth.user?.id] as const,
  () => {
    void loadItem()
  },
  { immediate: true },
)
</script>

<template>
  <section class="page">
    <button type="button" class="back-btn" @click="router.back()">
      <el-icon><ArrowLeft /></el-icon>
      <span>返回</span>
    </button>

    <div v-if="loading" class="panel skeleton" aria-hidden="true">
      <div class="sk-line wide shimmer" />
      <div class="sk-line mid shimmer" />
      <div class="sk-block shimmer" />
    </div>

    <EmptyState
      v-else-if="notFound"
      title="物品不存在"
      description="可能已被删除，或你没有权限访问。"
    >
      <RouterLink class="ce-cta" :to="{ name: 'my-items' }">我的物品</RouterLink>
    </EmptyState>

    <EmptyState
      v-else-if="forbidden"
      title="无权编辑"
      description="只能编辑自己发布的物品。"
    >
      <RouterLink class="ce-cta" :to="{ name: 'home' }">回到市场</RouterLink>
    </EmptyState>

    <template v-else-if="errorMessage">
      <ProblemAlert title="加载失败" :message="errorMessage" @close="errorMessage = ''" />
      <EmptyState title="无法加载物品" :description="errorMessage">
        <el-button type="primary" @click="loadItem">重试</el-button>
      </EmptyState>
    </template>

    <template v-else-if="item">
      <header class="page-header">
        <div>
          <h1>编辑物品</h1>
          <p class="desc">修改资料与图片。预订中或已售出的物品不可编辑。</p>
        </div>
        <ItemStatusTag :status="item.status" />
      </header>

      <div v-if="item.moderationStatus === 'BLOCKED'" class="status-banner">
        该物品已被管理员屏蔽{{ item.moderationReason ? `：${item.moderationReason}` : '' }}，不可修改资料与图片。
      </div>
      <div v-else-if="!editable" class="status-banner">
        当前状态为「{{ itemStatusLabel(item.status) }}」，不可修改资料与图片。
      </div>

      <div class="panel">
        <ItemForm
          :model-value="formModel"
          submit-label="保存资料"
          :loading="saving"
          :disabled="!editable"
          :form-error="formError"
          :field-errors="fieldErrors"
          @submit="handleSave"
          @clear-error="clearFormErrors"
        >
          <template #actions>
            <el-button
              type="primary"
              native-type="submit"
              :loading="saving"
              :disabled="!editable"
            >
              保存资料
            </el-button>
            <el-button
              :disabled="saving"
              @click="router.push({ name: 'item-detail', params: { id: item.id } })"
            >
              查看详情
            </el-button>
          </template>
        </ItemForm>
      </div>

      <ItemImageManager
        :item-id="item.id"
        :images="images"
        :disabled="!imagesEditable"
        @change="onImagesChange"
      />
    </template>
  </section>
</template>

<style scoped>
.page {
  display: grid;
  gap: 20px;
  max-width: 720px;
}

.back-btn {
  justify-self: start;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  min-height: 36px;
  padding: 0 8px 0 4px;
  border: 0;
  border-radius: 6px;
  background: transparent;
  color: var(--ce-color-muted);
  font-size: 14px;
  cursor: pointer;
}

.back-btn:hover {
  color: var(--ce-color-primary);
  background: var(--ce-color-primary-soft);
}

.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.page-header h1 {
  margin-bottom: 8px;
}

.desc {
  margin: 0;
  color: var(--ce-color-muted);
  font-size: 14px;
  line-height: 1.6;
}

.status-banner {
  padding: 12px 14px;
  background: #fef0c7;
  border-left: 4px solid var(--ce-color-warning);
  border-radius: 0 var(--ce-radius) var(--ce-radius) 0;
  color: #93370d;
  font-size: 14px;
  line-height: 1.5;
}

.panel {
  padding: 24px;
  background: var(--ce-color-surface);
  border: 1px solid var(--ce-color-border);
  border-radius: var(--ce-radius);
}

.cta {
  display: inline-flex;
  align-items: center;
  min-height: 36px;
  padding: 0 14px;
  border-radius: 6px;
  background: var(--ce-color-primary);
  color: #fff;
  font-size: 14px;
}

.skeleton {
  display: grid;
  gap: 14px;
}

.sk-line {
  height: 16px;
  border-radius: 4px;
  background: #eef1f4;
}

.sk-line.wide {
  width: 50%;
  height: 28px;
}

.sk-line.mid {
  width: 70%;
}

.sk-block {
  height: 220px;
  border-radius: 6px;
  background: #eef1f4;
}

.shimmer {
  background: linear-gradient(90deg, #eef1f4 25%, #f7f8fa 50%, #eef1f4 75%);
  background-size: 200% 100%;
  animation: shimmer 1.2s ease-in-out infinite;
}

@keyframes shimmer {
  0% {
    background-position: 100% 0;
  }
  100% {
    background-position: -100% 0;
  }
}

@media (prefers-reduced-motion: reduce) {
  .shimmer {
    animation: none;
  }
}
</style>
