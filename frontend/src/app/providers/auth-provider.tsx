import { useQueryClient } from '@tanstack/react-query'
import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import { onSessionChange, refreshSession, setAccessToken } from '@/api/client'
import type { AuthResponse, CurrentUser, LoginRequest, RegisterRequest } from '@/api/types'
import { authApi } from '@/features/auth/api'
import { useTheme } from './theme-provider'

type AuthStatus = 'loading' | 'authenticated' | 'anonymous'

interface AuthContextValue {
  status: AuthStatus
  user: CurrentUser | null
  login: (body: LoginRequest) => Promise<void>
  register: (body: RegisterRequest) => Promise<void>
  logout: () => Promise<void>
  setUser: (user: CurrentUser) => void
}

const AuthContext = createContext<AuthContextValue | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [status, setStatus] = useState<AuthStatus>('loading')
  const [user, setUserState] = useState<CurrentUser | null>(null)
  const queryClient = useQueryClient()
  const { setPreference } = useTheme()

  const applyUser = useCallback(
    (u: CurrentUser | null) => {
      setUserState(u)
      setStatus(u ? 'authenticated' : 'anonymous')
      // A preferência de tema salva no servidor acompanha o usuário entre dispositivos.
      if (u) setPreference(u.settings.theme)
    },
    [setPreference],
  )

  const applySession = useCallback(
    (session: AuthResponse) => {
      setAccessToken(session.accessToken)
      applyUser(session.user)
    },
    [applyUser],
  )

  // Restaura a sessão ao abrir o app (cookie de refresh HttpOnly).
  useEffect(() => {
    let active = true
    refreshSession()
      .then((session) => active && applyUser(session?.user ?? null))
      .catch(() => active && applyUser(null))
    return () => {
      active = false
    }
  }, [applyUser])

  // Se o refresh falhar no meio do uso (sessão expirada), volta para anônimo.
  useEffect(
    () =>
      onSessionChange((session) => {
        if (!session) {
          setUserState(null)
          setStatus('anonymous')
          queryClient.clear()
        }
      }),
    [queryClient],
  )

  const value = useMemo<AuthContextValue>(
    () => ({
      status,
      user,
      login: async (body) => applySession(await authApi.login(body)),
      register: async (body) => applySession(await authApi.register(body)),
      logout: async () => {
        try {
          await authApi.logout()
        } finally {
          setAccessToken(null)
          queryClient.clear()
          applyUser(null)
        }
      },
      setUser: (u) => applyUser(u),
    }),
    [status, user, applySession, applyUser, queryClient],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth deve ser usado dentro de AuthProvider')
  return ctx
}
