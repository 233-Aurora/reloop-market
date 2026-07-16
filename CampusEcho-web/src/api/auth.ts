import { http, requestData } from './http'
import type {
  CurrentUserResponse,
  LoginRequest,
  LoginResponse,
  RegisterRequest,
  UserProfileResponse,
  UserProfileUpdateRequest,
} from './types'

export function login(payload: LoginRequest): Promise<LoginResponse> {
  return requestData(http.post<LoginResponse>('/auth/login', payload))
}

export function register(payload: RegisterRequest): Promise<UserProfileResponse> {
  return requestData(http.post<UserProfileResponse>('/auth/register', payload))
}

export function fetchCurrentUser(): Promise<CurrentUserResponse> {
  return requestData(http.get<CurrentUserResponse>('/users/me'))
}

export function updateCurrentUser(payload: UserProfileUpdateRequest): Promise<CurrentUserResponse> {
  return requestData(http.put<CurrentUserResponse>('/users/me', payload))
}

export function uploadAvatar(file: File): Promise<CurrentUserResponse> {
  const form = new FormData()
  form.append('file', file)
  return requestData(http.post<CurrentUserResponse>('/users/me/avatar', form))
}

export function deleteAvatar(): Promise<CurrentUserResponse> {
  return requestData(http.delete<CurrentUserResponse>('/users/me/avatar'))
}

export function fetchUserProfile(id: number): Promise<UserProfileResponse> {
  return requestData(http.get<UserProfileResponse>(`/users/${id}`))
}
