import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '@/api/client'
import { invalidateMoney, qk } from '@/api/query-keys'
import type {
  CatalogItem,
  CatalogRequest,
  Expense,
  ExpenseDetail,
  ExpenseGroupBy,
  ExpenseRequest,
  ExpenseStatus,
  ExpenseSummary,
  PaymentRequest,
  PreviewTotal,
  PreviewTotalRequest,
} from '@/api/types'

export interface ExpenseFilters {
  objectiveId?: string
  categoryId?: string
  cycleId?: string
  status?: ExpenseStatus
  required?: boolean
  q?: string
}

function toQuery(params: object) {
  const search = new URLSearchParams()
  Object.entries(params).forEach(([k, v]) => {
    if (v !== undefined && v !== null && v !== '') search.set(k, String(v))
  })
  const s = search.toString()
  return s ? `?${s}` : ''
}

export function useExpenses(filters: ExpenseFilters) {
  return useQuery({
    queryKey: qk.expenses(filters),
    queryFn: () => api.get<Expense[]>(`/api/expenses${toQuery(filters)}`),
    placeholderData: (previous) => previous,
  })
}

export function useExpense(id: string | null) {
  return useQuery({
    queryKey: qk.expense(id ?? ''),
    queryFn: () => api.get<ExpenseDetail>(`/api/expenses/${id}`),
    enabled: !!id,
  })
}

export function useExpenseSummary(objectiveId: string | undefined, groupBy: ExpenseGroupBy) {
  return useQuery({
    queryKey: qk.expenseSummary(objectiveId, groupBy),
    queryFn: () => api.get<ExpenseSummary>(`/api/expenses/summary${toQuery({ objectiveId, groupBy })}`),
    placeholderData: (previous) => previous,
  })
}

export function previewTotal(body: PreviewTotalRequest) {
  return api.post<PreviewTotal>('/api/expenses/preview-total', body)
}

function useMoneyMutation<TVars, TResult>(fn: (vars: TVars) => Promise<TResult>) {
  const client = useQueryClient()
  return useMutation({ mutationFn: fn, onSuccess: () => invalidateMoney(client) })
}

export function useSaveExpense() {
  return useMoneyMutation(({ id, body }: { id?: string; body: ExpenseRequest }) =>
    id ? api.put<ExpenseDetail>(`/api/expenses/${id}`, body) : api.post<ExpenseDetail>('/api/expenses', body),
  )
}

export function useDeleteExpense() {
  return useMoneyMutation((id: string) => api.delete<void>(`/api/expenses/${id}`))
}

export function useCancelExpense() {
  return useMoneyMutation(({ id, cancel }: { id: string; cancel: boolean }) =>
    api.post<ExpenseDetail>(`/api/expenses/${id}/${cancel ? 'cancel' : 'restore'}`),
  )
}

export function useAddPayment() {
  return useMoneyMutation(({ expenseId, body }: { expenseId: string; body: PaymentRequest }) =>
    api.post<ExpenseDetail>(`/api/expenses/${expenseId}/payments`, body),
  )
}

export function useDeletePayment() {
  return useMoneyMutation(({ expenseId, paymentId }: { expenseId: string; paymentId: string }) =>
    api.delete<ExpenseDetail>(`/api/expenses/${expenseId}/payments/${paymentId}`),
  )
}

// ------------------------------------------------------------------ catálogo

export type CatalogKind = 'categories' | 'cycles'
const catalogPath = (kind: CatalogKind) =>
  kind === 'categories' ? '/api/expense-categories' : '/api/expense-cycles'

export function useCatalog(kind: CatalogKind) {
  return useQuery({
    queryKey: kind === 'categories' ? qk.categories : qk.cycles,
    queryFn: () => api.get<CatalogItem[]>(catalogPath(kind)),
    staleTime: 5 * 60_000,
  })
}

export function useSaveCatalogItem(kind: CatalogKind) {
  const client = useQueryClient()
  return useMutation({
    mutationFn: ({ id, body }: { id?: string; body: CatalogRequest }) =>
      id
        ? api.put<CatalogItem>(`${catalogPath(kind)}/${id}`, body)
        : api.post<CatalogItem>(catalogPath(kind), body),
    onSuccess: () => {
      void client.invalidateQueries({ queryKey: ['catalog'] })
      invalidateMoney(client)
    },
  })
}

export function useDeleteCatalogItem(kind: CatalogKind) {
  const client = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => api.delete<void>(`${catalogPath(kind)}/${id}`),
    onSuccess: () => void client.invalidateQueries({ queryKey: ['catalog'] }),
  })
}
