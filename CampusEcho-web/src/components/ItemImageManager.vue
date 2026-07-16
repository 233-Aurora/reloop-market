<script setup lang="ts">
import { computed, ref } from 'vue'
import { Delete, Picture, Star, Upload } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  deleteItemImage,
  setPrimaryImage,
  uploadItemImage,
} from '../api/items'
import type { ItemImageResponse } from '../api/types'
import {
  IMAGE_ACCEPT,
  IMAGE_MAX_COUNT,
  validateImageFile,
} from '../utils/item'
import { getErrorMessage } from '../utils/error'
import { resolveAssetUrl } from '../utils/format'
import ProblemAlert from './ProblemAlert.vue'

const props = defineProps<{
  itemId: number
  images: ItemImageResponse[]
  disabled?: boolean
}>()

const emit = defineEmits<{
  change: [images: ItemImageResponse[]]
}>()

const fileInputRef = ref<HTMLInputElement | null>(null)
const uploading = ref(false)
const actingId = ref<number | null>(null)
const errorMessage = ref('')
const failedIds = ref<Set<number>>(new Set())

const sortedImages = computed(() =>
  [...props.images].sort((a, b) => {
    if (a.primary !== b.primary) return a.primary ? -1 : 1
    return a.sortOrder - b.sortOrder
  }),
)

const canUploadMore = computed(
  () => !props.disabled && props.images.length < IMAGE_MAX_COUNT,
)

function imageSrc(image: ItemImageResponse) {
  if (failedIds.value.has(image.id)) return null
  return resolveAssetUrl(image.url)
}

function markFailed(id: number) {
  const next = new Set(failedIds.value)
  next.add(id)
  failedIds.value = next
}

function openPicker() {
  if (!canUploadMore.value || uploading.value) return
  fileInputRef.value?.click()
}

async function onFileChange(event: Event) {
  const input = event.target as HTMLInputElement
  const files = input.files
  if (!files?.length) return

  const remaining = IMAGE_MAX_COUNT - props.images.length
  const selected = Array.from(files).slice(0, remaining)

  errorMessage.value = ''
  uploading.value = true
  try {
    let nextImages = [...props.images]
    for (const file of selected) {
      const invalid = validateImageFile(file)
      if (invalid) {
        ElMessage.warning(`${file.name}：${invalid}`)
        continue
      }
      const makePrimary = nextImages.length === 0
      const uploaded = await uploadItemImage(props.itemId, file, makePrimary)
      if (uploaded.primary) {
        nextImages = nextImages.map((img) => ({ ...img, primary: false }))
      }
      nextImages = [...nextImages, uploaded]
      emit('change', nextImages)
    }
    if (selected.length) {
      ElMessage.success('图片上传成功')
    }
  } catch (error) {
    errorMessage.value = getErrorMessage(error, '图片上传失败')
  } finally {
    uploading.value = false
    input.value = ''
  }
}

async function handleSetPrimary(image: ItemImageResponse) {
  if (props.disabled || image.primary || actingId.value !== null) return
  actingId.value = image.id
  errorMessage.value = ''
  try {
    await setPrimaryImage(props.itemId, image.id)
    const next = props.images.map((img) => ({
      ...img,
      primary: img.id === image.id,
    }))
    emit('change', next)
    ElMessage.success('已设为主图')
  } catch (error) {
    errorMessage.value = getErrorMessage(error, '设置主图失败')
  } finally {
    actingId.value = null
  }
}

async function handleDelete(image: ItemImageResponse) {
  if (props.disabled || actingId.value !== null) return
  try {
    await ElMessageBox.confirm('确定删除这张图片吗？删除后不可恢复。', '删除图片', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
    })
  } catch {
    return
  }

  actingId.value = image.id
  errorMessage.value = ''
  try {
    await deleteItemImage(props.itemId, image.id)
    let next = props.images.filter((img) => img.id !== image.id)
    if (image.primary && next.length > 0 && !next.some((img) => img.primary)) {
      next = next.map((img, index) => ({ ...img, primary: index === 0 }))
    }
    emit('change', next)
    ElMessage.success('图片已删除')
  } catch (error) {
    errorMessage.value = getErrorMessage(error, '删除图片失败')
  } finally {
    actingId.value = null
  }
}
</script>

<template>
  <section class="image-manager">
    <header class="manager-header">
      <div>
        <h2>物品图片</h2>
        <p>
          支持 JPEG / PNG / WebP，单张不超过 5 MiB，最多 {{ IMAGE_MAX_COUNT }} 张。
          第一张默认主图。
        </p>
      </div>
      <el-button
        type="primary"
        :icon="Upload"
        :loading="uploading"
        :disabled="!canUploadMore"
        @click="openPicker"
      >
        上传图片
      </el-button>
      <input
        ref="fileInputRef"
        class="sr-only"
        type="file"
        :accept="IMAGE_ACCEPT"
        multiple
        @change="onFileChange"
      />
    </header>

    <ProblemAlert
      v-if="errorMessage"
      title="图片操作失败"
      :message="errorMessage"
      @close="errorMessage = ''"
    />

    <p v-if="disabled" class="disabled-hint">当前物品状态不允许修改图片。</p>

    <div v-if="sortedImages.length" class="image-grid">
      <article v-for="image in sortedImages" :key="image.id" class="image-card">
        <div class="preview">
          <img
            v-if="imageSrc(image)"
            :src="imageSrc(image)!"
            :alt="`物品图片 ${image.id}`"
            @error="markFailed(image.id)"
          />
          <div v-else class="preview-fallback">
            <el-icon :size="28"><Picture /></el-icon>
            <span>无法预览</span>
          </div>
          <span v-if="image.primary" class="primary-badge">主图</span>
        </div>
        <div class="image-actions">
          <el-button
            size="small"
            :icon="Star"
            :disabled="disabled || image.primary"
            :loading="actingId === image.id"
            @click="handleSetPrimary(image)"
          >
            设为主图
          </el-button>
          <el-button
            size="small"
            type="danger"
            plain
            :icon="Delete"
            :disabled="disabled"
            :loading="actingId === image.id"
            @click="handleDelete(image)"
          >
            删除
          </el-button>
        </div>
      </article>
    </div>

    <div v-else class="empty-images">
      <el-icon :size="32"><Picture /></el-icon>
      <p>还没有图片，上传后展示效果更好。</p>
      <el-button
        v-if="canUploadMore"
        type="primary"
        plain
        :loading="uploading"
        @click="openPicker"
      >
        选择图片
      </el-button>
    </div>
  </section>
</template>

<style scoped>
.image-manager {
  display: grid;
  gap: 16px;
  padding: 20px;
  background: var(--ce-color-surface);
  border: 1px solid var(--ce-color-border);
  border-radius: var(--ce-radius);
}

.manager-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.manager-header h2 {
  margin: 0 0 6px;
  font-size: 16px;
}

.manager-header p {
  margin: 0;
  color: var(--ce-color-muted);
  font-size: 13px;
  line-height: 1.5;
}

.disabled-hint {
  margin: 0;
  color: var(--ce-color-warning);
  font-size: 13px;
}

.image-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
  gap: 12px;
}

.image-card {
  display: grid;
  gap: 8px;
  min-width: 0;
}

.preview {
  position: relative;
  aspect-ratio: 1;
  border-radius: 6px;
  overflow: hidden;
  background: #eef1f4;
  border: 1px solid var(--ce-color-border);
}

.preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.preview-fallback {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  color: var(--ce-color-muted);
  font-size: 12px;
}

.primary-badge {
  position: absolute;
  top: 8px;
  left: 8px;
  min-height: 22px;
  padding: 0 8px;
  border-radius: 4px;
  background: var(--ce-color-primary);
  color: #fff;
  font-size: 12px;
  font-weight: 600;
  line-height: 22px;
}

.image-actions {
  display: grid;
  gap: 6px;
}

.image-actions .el-button {
  margin: 0;
  width: 100%;
}

.empty-images {
  display: grid;
  justify-items: center;
  gap: 10px;
  padding: 32px 16px;
  border: 1px dashed var(--ce-color-border);
  border-radius: var(--ce-radius);
  color: var(--ce-color-muted);
  text-align: center;
}

.empty-images p {
  margin: 0;
  font-size: 14px;
}

@media (max-width: 720px) {
  .manager-header {
    flex-direction: column;
  }

  .manager-header .el-button {
    width: 100%;
  }
}
</style>
