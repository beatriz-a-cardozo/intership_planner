import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '@/api/client'
import { invalidateMoney, qk } from '@/api/query-keys'
import type {
  Account,
  AccountRequest,
  AccountsSummary,
  BalanceRequest,
  BalanceSnapshot,
  ExchangeRate,
  ExchangeRateOverrideRequest,
  RatePoint,
} from '@/api/types'

export function useAccounts() {
  return useQuery({ queryKey: qk.accounts, queryFn: () => api.get<Account[]>('/api/accounts') })
}

export function useAccountsSummary() {
  return useQuery({
    queryKey: qk.accountsSummary,
    queryFn: () => api.get<AccountsSummary>('/api/accounts/summary'),
  })
}

export function useBalanceHistory(accountId: string | null) {
  return useQuery({
    queryKey: qk.balances(accountId ?? ''),
    queryFn: () => api.get<BalanceSnapshot[]>(`/api/accounts/${accountId}/balances`),
    enabled: !!accountId,
  })
}

function useMoneyMutation<TVars, TResult>(fn: (vars: TVars) => Promise<TResult>) {
  const client = useQueryClient()
  return useMutation({ mutationFn: fn, onSuccess: () => invalidateMoney(client) })
}

export function useSaveAccount() {
  return useMoneyMutation(({ id, body }: { id?: string; body: AccountRequest }) =>
    id ? api.put<Account>(`/api/accounts/${id}`, body) : api.post<Account>('/api/accounts', body),
  )
}

export function useDeleteAccount() {
  return useMoneyMutation((id: string) => api.delete<void>(`/api/accounts/${id}`))
}

export function useRecordBalance() {
  return useMoneyMutation(({ accountId, body }: { accountId: string; body: BalanceRequest }) =>
    api.post<BalanceSnapshot>(`/api/accounts/${accountId}/balances`, body),
  )
}

// ------------------------------------------------------------------ câmbio

export function useExchangeRate(base = 'EUR', quote = 'BRL') {
  return useQuery({
    queryKey: qk.exchangeCurrent(base, quote),
    queryFn: () => api.get<ExchangeRate>(`/api/exchange-rates/current?base=${base}&quote=${quote}`),
  })
}

export function useExchangeHistory(base = 'EUR', quote = 'BRL') {
  return useQuery({
    queryKey: qk.exchangeHistory(base, quote),
    queryFn: () => api.get<RatePoint[]>(`/api/exchange-rates/history?base=${base}&quote=${quote}`),
    staleTime: 60 * 60_000,
  })
}

export function useSetRateOverride() {
  return useMoneyMutation((body: ExchangeRateOverrideRequest) =>
    api.put<ExchangeRate>('/api/exchange-rates/override', body),
  )
}

export function useClearRateOverride() {
  return useMoneyMutation(({ base, quote }: { base: string; quote: string }) =>
    api.delete<void>(`/api/exchange-rates/override?base=${base}&quote=${quote}`),
  )
}
