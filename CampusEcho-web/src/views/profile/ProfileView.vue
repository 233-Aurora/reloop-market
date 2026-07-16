<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import { fetchCurrentUser, updateCurrentUser } from '../../api/auth'
import type { CurrentUserResponse } from '../../api/types'
import { useAuthStore } from '../../stores/auth'
import { getErrorMessage, getFieldErrors } from '../../utils/error'
import { formatDateTime } from '../../utils/format'
import AvatarUploader from '../../components/AvatarUploader.vue'
import EmptyState from '../../components/EmptyState.vue'
import ProblemAlert from '../../components/ProblemAlert.vue'

const auth = useAuthStore()

const loading = ref(true)
const saving = ref(false)
const loadError = ref('')
const formError = ref('')
const fieldErrors = ref<Record<string, string>>({})
const formRef = ref<FormInstance>()

const profile = ref<CurrentUserResponse | null>(null)

const form = reactive({
  displayName: '',
  phone: '',
  campus: '',
})

const rules: FormRules = {
  displayName: [
    { required: true, message: '请输入昵称', trigger: 'blur' },
    { max: 64, message: '昵称最多 64 个字符', trigger: 'blur' },
  ],
  phone: [{ max: 32, message: '手机号最多 32 个字符', trigger: 'blur' }],
  campus: [{ max: 128, message: '校区最多 128 个字符', trigger: 'blur' }],
}

function applyProfile(user: CurrentUserResponse) {
  profile.value = user
  form.displayName = user.displayName
  form.phone = user.phone ?? ''
  form.campus = user.campus ?? ''
  auth.setUser(user)
}

function clearFormErrors() {
  formError.value = ''
  fieldErrors.value = {}
}

async function loadProfile() {
  loading.value = true
  loadError.value = ''
  try {
    const user = await fetchCurrentUser()
    applyProfile(user)
  } catch (error) {
    loadError.value = getErrorMessage(error, '无法加载个人资料')
  } finally {
    loading.value = false
  }
}

async function handleSave() {
  if (!formRef.value || saving.value) return

  clearFormErrors()
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  saving.value = true
  try {
    const user = await updateCurrentUser({
      displayName: form.displayName.trim(),
      phone: form.phone.trim() || null,
      campus: form.campus.trim() || null,
    })
    applyProfile(user)
    ElMessage.success('资料已保存')
  } catch (error) {
    fieldErrors.value = getFieldErrors(error)
    formError.value = getErrorMessage(error, '保存失败，请检查填写信息')
  } finally {
    saving.value = false
  }
}

function onAvatarUpdated(user: CurrentUserResponse) {
  applyProfile(user)
}

watch(
  () => auth.user,
  (user) => {
    if (user && !profile.value && !loading.value) {
      applyProfile(user)
    }
  },
)

onMounted(() => {
  void loadProfile()
})
</script>

<template>
  <section class="ce-page ce-page-narrow">
    <header class="ce-page-header">
      <div>
        <h1>个人资料</h1>
        <p class="ce-page-desc">管理头像与联系信息。用户名和邮箱不可修改。</p>
      </div>
    </header>

    <div v-if="loading" class="skeleton" aria-hidden="true">
      <div class="sk-avatar shimmer" />
      <div class="sk-panel shimmer" />
    </div>

    <template v-else-if="loadError">
      <ProblemAlert title="加载失败" :message="loadError" @close="loadError = ''" />
      <EmptyState title="无法加载个人资料" :description="loadError">
        <el-button type="primary" @click="loadProfile">重试</el-button>
      </EmptyState>
    </template>

    <template v-else-if="profile">
      <nav class="quick-links" aria-label="账户快捷入口">
        <RouterLink class="quick-link" :to="{ name: 'my-items' }">我的物品</RouterLink>
        <RouterLink class="quick-link" :to="{ name: 'my-trades' }">我的交易</RouterLink>
        <RouterLink class="quick-link" :to="{ name: 'messages' }">消息中心</RouterLink>
        <RouterLink class="quick-link" :to="{ name: 'my-disputes' }">我的争议</RouterLink>
        <RouterLink class="quick-link" :to="{ name: 'my-reports' }">我的举报</RouterLink>
      </nav>

      <AvatarUploader
        :avatar-url="profile.avatarUrl"
        :display-name="profile.displayName"
        @updated="onAvatarUpdated"
      />

      <section class="ce-panel" aria-label="只读账号信息">
        <h2>账号信息</h2>
        <dl class="meta-list">
          <div>
            <dt>用户名</dt>
            <dd>{{ profile.username }}</dd>
          </div>
          <div>
            <dt>邮箱</dt>
            <dd>{{ profile.email }}</dd>
          </div>
          <div>
            <dt>角色</dt>
            <dd>{{ profile.role === 'ADMIN' ? '管理员' : '普通用户' }}</dd>
          </div>
          <div>
            <dt>信誉分</dt>
            <dd>{{ profile.reputationScore }}</dd>
          </div>
          <div>
            <dt>注册时间</dt>
            <dd :title="formatDateTime(profile.createdAt)">
              {{ formatDateTime(profile.createdAt) }}
            </dd>
          </div>
        </dl>
      </section>

      <section class="ce-panel" aria-label="可编辑资料">
        <h2>可编辑资料</h2>
        <p class="panel-hint">昵称必填；手机号与校区可清空。保存与头像操作互不影响。</p>

        <el-form
          ref="formRef"
          class="profile-form"
          :model="form"
          :rules="rules"
          label-position="top"
          size="large"
          :disabled="saving"
          @submit.prevent="handleSave"
        >
          <ProblemAlert
            v-if="formError"
            title="保存失败"
            :message="formError"
            @close="clearFormErrors"
          />

          <el-form-item label="昵称" prop="displayName" :error="fieldErrors.displayName">
            <el-input
              v-model="form.displayName"
              maxlength="64"
              show-word-limit
              placeholder="展示给其他同学的名称"
              @input="clearFormErrors"
            />
          </el-form-item>

          <el-form-item label="手机号（可选）" prop="phone" :error="fieldErrors.phone">
            <el-input
              v-model="form.phone"
              maxlength="32"
              show-word-limit
              placeholder="仅自己可见，可留空"
              clearable
              @input="clearFormErrors"
            />
          </el-form-item>

          <el-form-item label="校区（可选）" prop="campus" :error="fieldErrors.campus">
            <el-input
              v-model="form.campus"
              maxlength="128"
              show-word-limit
              placeholder="例如：主校区"
              clearable
              @input="clearFormErrors"
            />
          </el-form-item>

          <div class="form-actions">
            <el-button type="primary" native-type="submit" :loading="saving">
              保存资料
            </el-button>
            <el-button :disabled="saving" @click="loadProfile">重置</el-button>
          </div>
        </el-form>
      </section>
    </template>
  </section>
</template>

<style scoped>
.quick-links {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.quick-link {
  display: inline-flex;
  align-items: center;
  min-height: 34px;
  padding: 0 12px;
  border-radius: var(--ce-radius-md);
  border: 1px solid var(--ce-color-border);
  background: var(--ce-color-surface);
  color: var(--ce-color-ink);
  font-size: 13px;
}

.quick-link:hover {
  border-color: var(--ce-color-hover-border);
  color: var(--ce-color-primary);
  background: var(--ce-color-primary-soft);
}

.ce-panel h2 {
  margin: 0 0 8px;
  font-size: 16px;
}

.panel-hint {
  margin: 0 0 16px;
  color: var(--ce-color-muted);
  font-size: 13px;
  line-height: 1.5;
}

.meta-list {
  margin: 12px 0 0;
  display: grid;
  gap: 12px;
}

.meta-list > div {
  display: grid;
  grid-template-columns: 80px 1fr;
  gap: 8px;
  font-size: 14px;
  line-height: 1.5;
}

.meta-list dt {
  color: var(--ce-color-muted);
}

.meta-list dd {
  margin: 0;
  word-break: break-word;
}

.profile-form {
  display: grid;
  gap: 2px;
}

.form-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  padding-top: 8px;
}

.skeleton {
  display: grid;
  gap: 16px;
}

.sk-avatar {
  height: 128px;
  border-radius: var(--ce-radius);
  background: #eef1f4;
}

.sk-panel {
  height: 280px;
  border-radius: var(--ce-radius);
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
