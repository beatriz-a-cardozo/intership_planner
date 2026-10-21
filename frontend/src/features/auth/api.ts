import { api } from '@/api/client'
import type { AuthResponse, CurrentUser, LoginRequest, RegisterRequest } from '@/api/types'

export const authApi = {
  login: (body: LoginRequest) => api.post<AuthResponse>('/api/auth/login', body, { skipAuthRefresh: true }),
  register: (body: RegisterRequest) => api.post<AuthResponse>('/api/auth/register', body, { skipAuthRefresh: true }),
  logout: () => api.post<void>('/api/auth/logout', undefined, { skipAuthRefresh: true }),
  me: () => api.get<CurrentUser>('/api/me'),
}
