<script setup lang="ts">
import { computed, ref } from 'vue'
import { Delete, Upload } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { deleteAvatar, uploadAvatar } from '../api/auth'
import type { CurrentUserResponse } from '../api/types'
import { getErrorMessage } from '../utils/error'
import { resolveAssetUrl } from '../utils/format'
import ProblemAlert from './ProblemAlert.vue'

const props = defineProps<{
  avatarUrl: string | null
  displayName: string
}>()

const emit = defineEmits<{
  updated: [user: CurrentUserResponse]
}>()

const AVATAR_ACCEPT = 'image/jpeg,image/png,image/webp'
const AVATAR_MAX_BYTES = 2 * 1024 * 1024

const fileInputRef = ref<HTMLInputElement | null>(null)
const uploading = ref(false)
const deleting = ref(false)
const errorMessage = ref('')
const imageFailed = ref(false)

const avatarSrc = computed(() => {
  if (imageFailed.value) return null
  return resolveAssetUrl(props.avatarUrl)
})

const initials = computed(() => (props.displayName || '用').slice(0, 1))

function validateAvatar(file: File): string | null {
  const allowed = ['image/jpeg', 'image/png', 'image/webp']
  if (!allowed.includes(file.type)) {
    return '仅支持 JPEG、PNG、WebP 格式'
  }
  if (file.size > AVATAR_MAX_BYTES) {
    return '头像不能超过 2 MiB'
  }
  return null
}

function openPicker() {
  if (uploading.value || deleting.value) return
  fileInputRef.value?.click()
}

async function onFileChange(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return

  const invalid = validateAvatar(file)
  if (invalid) {
    ElMessage.warning(invalid)
    input.value = ''
    return
  }

  errorMessage.value = ''
  uploading.value = true
  try {
    const user = await uploadAvatar(file)
    imageFailed.value = false
    emit('updated', user)
    ElMessage.success(props.avatarUrl ? '头像已替换' : '头像已上传')
  } catch (error) {
    errorMessage.value = getErrorMessage(error, '头像上传失败')
  } finally {
    uploading.value = false
    input.value = ''
  }
}

async function handleDelete() {
  if (!props.avatarUrl || uploading.value || deleting.value) return

  try {
    await ElMessageBox.confirm('确定删除当前头像吗？', '删除头像', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
    })
  } catch {
    return
  }

  errorMessage.value = ''
  deleting.value = true
  try {
    const user = await deleteAvatar()
    imageFailed.value = false
    emit('updated', user)
    ElMessage.success('头像已删除')
  } catch (error) {
    errorMessage.value = getErrorMessage(error, '删除头像失败')
  } finally {
    deleting.value = false
  }
}
</script>

<template>
  <section class="avatar-uploader">
    <div class="preview" aria-hidden="true">
      <img
        v-if="avatarSrc"
        :src="avatarSrc"
        :alt="displayName"
        @error="imageFailed = true"
      />
      <span v-else>{{ initials }}</span>
    </div>

    <div class="content">
      <h2>头像</h2>
      <p>支持 JPEG / PNG / WebP，不超过 2 MiB。与资料保存相互独立。</p>

      <ProblemAlert
        v-if="errorMessage"
        title="头像操作失败"
        :message="errorMessage"
        @close="errorMessage = ''"
      />

      <div class="actions">
        <el-button
          type="primary"
          :icon="Upload"
          :loading="uploading"
          :disabled="deleting"
          @click="openPicker"
        >
          {{ avatarUrl ? '替换头像' : '上传头像' }}
        </el-button>
        <el-button
          v-if="avatarUrl"
          type="danger"
          plain
          :icon="Delete"
          :loading="deleting"
          :disabled="uploading"
          @click="handleDelete"
        >
          删除头像
        </el-button>
      </div>

      <input
        ref="fileInputRef"
        class="sr-only"
        type="file"
        :accept="AVATAR_ACCEPT"
        @change="onFileChange"
      />
    </div>
  </section>
</template>

<style scoped>
.avatar-uploader {
  display: flex;
  gap: 20px;
  align-items: flex-start;
  padding: 20px;
  background: var(--ce-color-surface);
  border: 1px solid var(--ce-color-border);
  border-radius: var(--ce-radius);
}

.preview {
  width: 88px;
  height: 88px;
  border-radius: 50%;
  overflow: hidden;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: var(--ce-color-primary-soft);
  color: var(--ce-color-primary);
  font-size: 28px;
  font-weight: 700;
  flex-shrink: 0;
  border: 1px solid var(--ce-color-border);
}

.preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.content {
  flex: 1;
  min-width: 0;
  display: grid;
  gap: 10px;
}

h2 {
  margin: 0;
  font-size: 16px;
}

p {
  margin: 0;
  color: var(--ce-color-muted);
  font-size: 13px;
  line-height: 1.5;
}

.actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

@media (max-width: 560px) {
  .avatar-uploader {
    flex-direction: column;
    align-items: center;
    text-align: center;
  }

  .actions {
    justify-content: center;
  }

  .actions .el-button {
    flex: 1;
  }
}
</style>
