import { API_BASE_URL } from '@/lib/config'
import { ApiError, NetworkError } from './errors'
import type { AuthResponse, ProblemDetails } from './types'

/**
 * Cliente HTTP da aplicação.
 *
 * - O access token (JWT curto) vive só em memória — nunca em localStorage.
 * - O refresh token é um cookie HttpOnly gerenciado pelo backend (path /api/auth).
 * - Em 401, faz UM refresh (single-flight, compartilhado entre requisições concorrentes) e repete a chamada.
 */

let accessToken: string | null = null
let refreshInFlight: Promise<AuthResponse | null> | null = null
const sessionListeners = new Set<(session: AuthResponse | null) => void>()

export function setAccessToken(token: string | null) {
  accessToken = token
}

export function getAccessToken() {
  return accessToken
}

/** Notificado quando a sessão é renovada ou perdida (ex.: refresh expirou). */
export function onSessionChange(listener: (session: AuthResponse | null) => void) {
  sessionListeners.add(listener)
  return () => {
    sessionListeners.delete(listener)
  }
}

function emitSession(session: AuthResponse | null) {
  sessionListeners.forEach((l) => l(session))
}

async function parseProblem(response: Response): Promise<ProblemDetails> {
  try {
    const body = await response.json()
    return { status: response.status, ...body }
  } catch {
    return { status: response.status, title: response.statusText }
  }
}

async function rawFetch(path: string, init: RequestInit): Promise<Response> {
  try {
    return await fetch(`${API_BASE_URL}${path}`, { credentials: 'include', ...init })
  } catch {
    throw new NetworkError()
  }
}

/** Renova a sessão usando o cookie de refresh. Retorna null se não houver sessão válida. */
export function refreshSession(): Promise<AuthResponse | null> {
  if (!refreshInFlight) {
    refreshInFlight = (async () => {
      try {
        const response = await rawFetch('/api/auth/refresh', { method: 'POST' })
        if (!response.ok) {
          setAccessToken(null)
          emitSession(null)
          return null
        }
        const session = (await response.json()) as AuthResponse
        setAccessToken(session.accessToken)
        emitSession(session)
        return session
      } finally {
        refreshInFlight = null
      }
    })()
  }
  return refreshInFlight
}

export interface RequestOptions extends Omit<RequestInit, 'body'> {
  body?: unknown
  /** Não tenta refresh em 401 (usado nos próprios endpoints de autenticação). */
  skipAuthRefresh?: boolean
}

export async function apiFetch<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const { body, skipAuthRefresh, headers, ...rest } = options

  const doRequest = () => {
    const h = new Headers(headers)
    h.set('Accept', 'application/json')
    if (body !== undefined) h.set('Content-Type', 'application/json')
    if (accessToken) h.set('Authorization', `Bearer ${accessToken}`)
    return rawFetch(path, {
      ...rest,
      headers: h,
      body: body === undefined ? undefined : JSON.stringify(body),
    })
  }

  let response = await doRequest()

  if (response.status === 401 && !skipAuthRefresh) {
    const session = await refreshSession()
    if (session) response = await doRequest()
  }

  if (!response.ok) throw new ApiError(await parseProblem(response))
  if (response.status === 204) return undefined as T
  return (await response.json()) as T
}

export const api = {
  get: <T>(path: string, options?: RequestOptions) => apiFetch<T>(path, { ...options, method: 'GET' }),
  post: <T>(path: string, body?: unknown, options?: RequestOptions) =>
    apiFetch<T>(path, { ...options, method: 'POST', body }),
  put: <T>(path: string, body?: unknown, options?: RequestOptions) =>
    apiFetch<T>(path, { ...options, method: 'PUT', body }),
  patch: <T>(path: string, body?: unknown, options?: RequestOptions) =>
    apiFetch<T>(path, { ...options, method: 'PATCH', body }),
  delete: <T>(path: string, options?: RequestOptions) => apiFetch<T>(path, { ...options, method: 'DELETE' }),
}
