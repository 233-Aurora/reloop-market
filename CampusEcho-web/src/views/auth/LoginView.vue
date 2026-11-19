<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import type { FormInstance, FormRules } from 'element-plus'
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
  login: typeof route.query.login === 'string' ? route.query.login : '',
  password: '',
})

function isAdminLogin(login: string): boolean {
  return login.trim().toLowerCase() === 'admin'
}

function validatePassword(_rule: unknown, value: string, callback: (error?: Error) => void) {
  if (!value) {
    callback(new Error('请输入密码'))
    return
  }
  if (value.length > 72) {
    callback(new Error('密码长度不能超过 72 个字符'))
    return
  }
  // 平台管理员账户允许 5 位密码；其他账户仍要求 8-72 位
  const minLen = isAdminLogin(form.login) ? 5 : 8
  if (value.length < minLen) {
    callback(
      new Error(
        isAdminLogin(form.login)
          ? '管理员密码长度应在 5-72 个字符'
          : '密码长度应在 8-72 个字符',
      ),
    )
    return
  }
  callback()
}

const rules: FormRules = {
  login: [
    { required: true, message: '请输入用户名或邮箱', trigger: 'blur' },
    { min: 3, max: 254, message: '长度应在 3-254 个字符', trigger: 'blur' },
  ],
  password: [{ validator: validatePassword, trigger: 'blur' }],
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
    await auth.login({
      login: form.login.trim(),
      password: form.password,
    })

    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/'
    await router.replace(redirect || '/')
  } catch (error) {
    fieldErrors.value = getFieldErrors(error)
    errorMessage.value = getErrorMessage(error, '登录失败，请检查账号或密码')
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <section class="auth-card" aria-labelledby="login-title">
    <header class="auth-card-header">
      <h1 id="login-title">登录</h1>
      <p>使用用户名或邮箱登录 ReLoop Market</p>
    </header>

    <ProblemAlert
      v-if="errorMessage"
      title="无法登录"
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
      <el-form-item label="用户名或邮箱" prop="login" :error="fieldErrors.login">
        <el-input
          v-model="form.login"
          name="login"
          autocomplete="username"
          placeholder="student_01 或 student@example.com"
          clearable
          @input="
            clearErrors();
            formRef?.validateField('password').catch(() => undefined)
          "
        />
      </el-form-item>

      <el-form-item label="密码" prop="password" :error="fieldErrors.password">
        <el-input
          v-model="form.password"
          name="password"
          type="password"
          autocomplete="current-password"
          :placeholder="isAdminLogin(form.login) ? '管理员密码（至少 5 位）' : '请输入密码（至少 8 位）'"
          show-password
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
        登录
      </el-button>
    </el-form>

    <p class="auth-footer">
      还没有账号？
      <RouterLink :to="{ name: 'register', query: route.query }">立即注册</RouterLink>
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
  gap: 4px;
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
