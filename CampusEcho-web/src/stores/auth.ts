import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { fetchCurrentUser, login as loginApi, register as registerApi } from '../api/auth'
import { getStoredToken, setStoredToken } from '../api/http'
import type {
  CurrentUserResponse,
  LoginRequest,
  RegisterRequest,
  UserProfileResponse,
} from '../api/types'

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(getStoredToken())
  const user = ref<CurrentUserResponse | null>(null)
  const initialized = ref(false)
  const loading = ref(false)

  const isAuthenticated = computed(() => Boolean(token.value && user.value))
  const isAdmin = computed(() => user.value?.role === 'ADMIN')

  function setSession(accessToken: string, currentUser: CurrentUserResponse): void {
    token.value = accessToken
    user.value = currentUser
    setStoredToken(accessToken)
  }

  function clearSession(): void {
    token.value = null
    user.value = null
    setStoredToken(null)
  }

  async function initialize(): Promise<void> {
    if (initialized.value) {
      return
    }

    loading.value = true
    try {
      if (!token.value) {
        user.value = null
        return
      }

      user.value = await fetchCurrentUser()
    } catch {
      clearSession()
    } finally {
      loading.value = false
      initialized.value = true
    }
  }

  async function login(payload: LoginRequest): Promise<CurrentUserResponse> {
    const result = await loginApi(payload)
    setSession(result.accessToken, result.user)
    initialized.value = true
    return result.user
  }

  async function register(payload: RegisterRequest): Promise<UserProfileResponse> {
    return registerApi(payload)
  }

  function logout(): void {
    clearSession()
  }

  function setUser(next: CurrentUserResponse): void {
    user.value = next
  }

  return {
    token,
    user,
    initialized,
    loading,
    isAuthenticated,
    isAdmin,
    initialize,
    login,
    register,
    logout,
    setUser,
    clearSession,
  }
})
