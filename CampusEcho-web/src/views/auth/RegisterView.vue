<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '../../stores/auth'
import { getErrorMessage, getFieldErrors } from '../../utils/error'
import ProblemAlert from '../../components/ProblemAlert.vue'

const auth = useAuthStore()
const router = useRouter()
const route = useRoute()

const formRef = ref<FormInstance>()
const submitting = ref(false)
const errorMessage = ref('')
const fieldErrors = ref<Record<string, string>>({})

const form = reactive({
  username: '',
  email: '',
  password: '',
  displayName: '',
  phone: '',
  campus: '',
})

const rules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    {
      pattern: /^[A-Za-z0-9_]{3,32}$/,
      message: '用户名需为 3-32 位字母、数字或下划线',
      trigger: 'blur',
    },
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入有效邮箱', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 8, max: 72, message: '密码长度应在 8-72 个字符', trigger: 'blur' },
  ],
  displayName: [
    { required: true, message: '请输入昵称', trigger: 'blur' },
    { max: 64, message: '昵称最多 64 个字符', trigger: 'blur' },
  ],
  phone: [{ max: 32, message: '手机号最多 32 个字符', trigger: 'blur' }],
  campus: [{ max: 128, message: '校区最多 128 个字符', trigger: 'blur' }],
}

function clearErrors() {
  errorMessage.value = ''
  fieldErrors.value = {}
}

async function handleSubmit() {
  if (!formRef.value || submitting.value) return

  clearErrors()
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    await auth.register({
      username: form.username.trim(),
      email: form.email.trim(),
      password: form.password,
      displayName: form.displayName.trim(),
      phone: form.phone.trim() || null,
      campus: form.campus.trim() || null,
    })

    ElMessage.success('注册成功，请登录')
    await router.replace({
      name: 'login',
      query: {
        ...route.query,
        login: form.username.trim(),
      },
    })
  } catch (error) {
    fieldErrors.value = getFieldErrors(error)
    errorMessage.value = getErrorMessage(error, '注册失败，请检查填写信息')
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <section class="auth-card" aria-labelledby="register-title">
    <header class="auth-card-header">
      <h1 id="register-title">注册</h1>
      <p>创建账号后即可发布物品与发起交易</p>
    </header>

    <ProblemAlert
      v-if="errorMessage"
      title="无法注册"
      :message="errorMessage"
      @close="errorMessage = ''"
    />

    <el-form
      ref="formRef"
      class="auth-form"
      :model="form"
      :rules="rules"
      label-position="top"
      size="large"
      @submit.prevent="handleSubmit"
    >
      <el-form-item label="用户名" prop="username" :error="fieldErrors.username">
        <el-input
          v-model="form.username"
          name="username"
          autocomplete="username"
          placeholder="3-32 位字母、数字或下划线"
          clearable
          @input="clearErrors"
        />
      </el-form-item>

      <el-form-item label="邮箱" prop="email" :error="fieldErrors.email">
        <el-input
          v-model="form.email"
          name="email"
          type="email"
          autocomplete="email"
          placeholder="student@example.com"
          clearable
          @input="clearErrors"
        />
      </el-form-item>

      <el-form-item label="密码" prop="password" :error="fieldErrors.password">
        <el-input
          v-model="form.password"
          name="password"
          type="password"
          autocomplete="new-password"
          placeholder="8-72 个字符"
          show-password
          @input="clearErrors"
        />
      </el-form-item>

      <el-form-item label="昵称" prop="displayName" :error="fieldErrors.displayName">
        <el-input
          v-model="form.displayName"
          name="displayName"
          autocomplete="nickname"
          placeholder="展示给其他同学的名称"
          clearable
          @input="clearErrors"
        />
      </el-form-item>

      <el-form-item label="手机号（可选）" prop="phone" :error="fieldErrors.phone">
        <el-input
          v-model="form.phone"
          name="phone"
          autocomplete="tel"
          placeholder="仅自己可见"
          clearable
          @input="clearErrors"
        />
      </el-form-item>

      <el-form-item label="校区（可选）" prop="campus" :error="fieldErrors.campus">
        <el-input
          v-model="form.campus"
          name="campus"
          placeholder="例如：主校区"
          clearable
          @input="clearErrors"
        />
      </el-form-item>

      <el-button
        class="submit-btn"
        type="primary"
        native-type="submit"
        :loading="submitting"
        :disabled="submitting"
      >
        注册
      </el-button>
    </el-form>

    <p class="auth-footer">
      已有账号？
      <RouterLink :to="{ name: 'login', query: route.query }">去登录</RouterLink>
    </p>
  </section>
</template>

<style scoped>
.auth-card {
  width: min(440px, 100%);
  padding: 32px 28px;
  background: var(--ce-color-surface);
  border: 1px solid var(--ce-color-border);
  border-radius: var(--ce-radius);
  display: grid;
  gap: 20px;
}

.auth-card-header h1 {
  margin-bottom: 8px;
  font-size: 28px;
}

.auth-card-header p {
  margin: 0;
  color: var(--ce-color-muted);
  font-size: 14px;
  line-height: 1.6;
}

.auth-form {
  display: grid;
  gap: 2px;
}

.submit-btn {
  width: 100%;
  margin-top: 8px;
}

.auth-footer {
  margin: 0;
  text-align: center;
  color: var(--ce-color-muted);
  font-size: 14px;
}

.auth-footer a {
  color: var(--ce-color-primary);
  font-weight: 600;
}

.auth-footer a:hover {
  text-decoration: underline;
}

@media (max-width: 480px) {
  .auth-card {
    padding: 24px 18px;
  }

  .auth-card-header h1 {
    font-size: 24px;
  }
}
</style>
