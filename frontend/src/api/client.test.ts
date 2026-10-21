import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { api, getAccessToken, onSessionChange, setAccessToken } from './client'
import { ApiError } from './errors'

const session = (token: string) => ({
  accessToken: token,
  tokenType: 'Bearer',
  expiresIn: 900,
  user: { id: '1', email: 'a@b.com', displayName: 'Bia', onboardingCompleted: false, settings: {} },
})

const json = (status: number, body: unknown) =>
  new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } })

describe('apiFetch', () => {
  const fetchMock = vi.fn<typeof fetch>()

  beforeEach(() => {
    vi.stubGlobal('fetch', fetchMock)
    setAccessToken('expired')
  })

  afterEach(() => {
    fetchMock.mockReset()
    vi.unstubAllGlobals()
  })

  it('envia o access token no header Authorization', async () => {
    fetchMock.mockResolvedValueOnce(json(200, { ok: true }))
    await api.get('/api/me')
    const headers = fetchMock.mock.calls[0]![1]!.headers as Headers
    expect(headers.get('Authorization')).toBe('Bearer expired')
  })

  it('renova a sessão em 401 e repete a requisição', async () => {
    fetchMock
      .mockResolvedValueOnce(json(401, { status: 401 }))
      .mockResolvedValueOnce(json(200, session('fresh')))
      .mockResolvedValueOnce(json(200, { value: 42 }))

    await expect(api.get<{ value: number }>('/api/me')).resolves.toEqual({ value: 42 })
    expect(fetchMock.mock.calls[1]![0]).toBe('/api/auth/refresh')
    expect(getAccessToken()).toBe('fresh')
    const retryHeaders = fetchMock.mock.calls[2]![1]!.headers as Headers
    expect(retryHeaders.get('Authorization')).toBe('Bearer fresh')
  })

  it('faz um único refresh para requisições concorrentes', async () => {
    fetchMock.mockImplementation(async (input, init) => {
      if (input === '/api/auth/refresh') return json(200, session('fresh'))
      const auth = (init?.headers as Headers).get('Authorization')
      return auth === 'Bearer fresh' ? json(200, { ok: true }) : json(401, { status: 401 })
    })

    await Promise.all([api.get('/api/a'), api.get('/api/b'), api.get('/api/c')])
    const refreshCalls = fetchMock.mock.calls.filter(([url]) => url === '/api/auth/refresh')
    expect(refreshCalls).toHaveLength(1)
  })

  it('encerra a sessão quando o refresh falha', async () => {
    const listener = vi.fn()
    const unsubscribe = onSessionChange(listener)
    fetchMock.mockResolvedValueOnce(json(401, { status: 401 })).mockResolvedValueOnce(json(401, { status: 401 }))

    await expect(api.get('/api/me')).rejects.toBeInstanceOf(ApiError)
    expect(listener).toHaveBeenCalledWith(null)
    expect(getAccessToken()).toBeNull()
    unsubscribe()
  })

  it('converte Problem Details em ApiError com erros por campo', async () => {
    fetchMock.mockResolvedValueOnce(
      json(400, {
        title: 'Dados inválidos',
        status: 400,
        detail: 'Verifique os campos.',
        errors: [{ field: 'email', message: 'E-mail já cadastrado.' }],
      }),
    )
    const error = await api.post('/api/auth/register', {}, { skipAuthRefresh: true }).catch((e: unknown) => e)
    expect(error).toBeInstanceOf(ApiError)
    expect((error as ApiError).status).toBe(400)
    expect((error as ApiError).fieldErrors).toEqual({ email: 'E-mail já cadastrado.' })
  })
})
