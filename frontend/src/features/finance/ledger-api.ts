import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '@/api/client'
import { invalidateMoney, qk } from '@/api/query-keys'
import type {
  Commitments,
  FinanceCategory,
  FinanceCategoryRequest,
  FinanceOverview,
  InstallmentPlan,
  InstallmentPlanRequest,
  MonthlySummary,
  Projection,
  RecurringEntry,
  RecurringEntryRequest,
  SavingGoal,
  Transaction,
  TransactionRequest,
  TransactionStatus,
} from '@/api/types'

/** Hooks do controle financeiro (Fase 4). Toda mutação invalida dinheiro em bloco (ver invalidateMoney). */

function useMoneyMutation<TVars, TResult>(fn: (vars: TVars) => Promise<TResult>) {
  const client = useQueryClient()
  return useMutation({ mutationFn: fn, onSuccess: () => invalidateMoney(client) })
}

export function useFinanceCategories() {
  return useQuery({
    queryKey: qk.financeCategories,
    queryFn: () => api.get<FinanceCategory[]>('/api/finance/categories'),
    staleTime: 5 * 60_000,
  })
}

export function useSaveFinanceCategory() {
  return useMoneyMutation(({ id, body }: { id?: string; body: FinanceCategoryRequest }) =>
    id
      ? api.put<FinanceCategory>(`/api/finance/categories/${id}`, body)
      : api.post<FinanceCategory>('/api/finance/categories', body),
  )
}

export function useDeleteFinanceCategory() {
  return useMoneyMutation((id: string) => api.delete<void>(`/api/finance/categories/${id}`))
}

export function useMonthlySummary(month: string) {
  return useQuery({
    queryKey: qk.financeSummary(month),
    queryFn: () => api.get<MonthlySummary>(`/api/finance/summary?month=${month}`),
    placeholderData: (previous) => previous,
  })
}

export function useFinanceOverview() {
  return useQuery({
    queryKey: qk.financeOverview,
    queryFn: () => api.get<FinanceOverview>('/api/finance/overview'),
  })
}

export function useCommitments() {
  return useQuery({
    queryKey: qk.commitments,
    queryFn: () => api.get<Commitments>('/api/finance/commitments?months=12'),
  })
}

export function useSaveTransaction() {
  return useMoneyMutation(({ id, body }: { id?: string; body: TransactionRequest }) =>
    id
      ? api.put<Transaction>(`/api/finance/transactions/${id}`, body)
      : api.post<Transaction>('/api/finance/transactions', body),
  )
}

export function useSetTransactionStatus() {
  return useMoneyMutation(({ id, status }: { id: string; status: TransactionStatus }) =>
    api.patch<Transaction>(`/api/finance/transactions/${id}/status`, { status }),
  )
}

export function useDeleteTransaction() {
  return useMoneyMutation((id: string) => api.delete<void>(`/api/finance/transactions/${id}`))
}

export function useInstallmentPlans() {
  return useQuery({
    queryKey: qk.installmentPlans,
    queryFn: () => api.get<InstallmentPlan[]>('/api/finance/installment-plans'),
  })
}

export function useSaveInstallmentPlan() {
  return useMoneyMutation(({ id, body }: { id?: string; body: InstallmentPlanRequest }) =>
    id
      ? api.put<InstallmentPlan>(`/api/finance/installment-plans/${id}`, body)
      : api.post<InstallmentPlan>('/api/finance/installment-plans', body),
  )
}

export function useDeleteInstallmentPlan() {
  return useMoneyMutation((id: string) => api.delete<void>(`/api/finance/installment-plans/${id}`))
}

export function useRecurringEntries() {
  return useQuery({
    queryKey: qk.recurring,
    queryFn: () => api.get<RecurringEntry[]>('/api/finance/recurring'),
  })
}

export function useSaveRecurring() {
  return useMoneyMutation(({ id, body }: { id?: string; body: RecurringEntryRequest }) =>
    id
      ? api.put<RecurringEntry>(`/api/finance/recurring/${id}`, body)
      : api.post<RecurringEntry>('/api/finance/recurring', body),
  )
}

export function useSetRecurringActive() {
  return useMoneyMutation(({ id, active }: { id: string; active: boolean }) =>
    api.patch<RecurringEntry>(`/api/finance/recurring/${id}/active`, { active }),
  )
}

export function useDeleteRecurring() {
  return useMoneyMutation((id: string) => api.delete<void>(`/api/finance/recurring/${id}`))
}

export function useSetSavingGoal() {
  return useMoneyMutation(
    ({ month, amount, currency }: { month: string; amount: number; currency: string }) =>
      api.put<SavingGoal>(`/api/finance/saving-goals/${month}`, { amount, currency }),
  )
}

export function useDeleteSavingGoal() {
  return useMoneyMutation((month: string) => api.delete<void>(`/api/finance/saving-goals/${month}`))
}

export function useProjection(objectiveId: string | undefined) {
  return useQuery({
    queryKey: qk.projection(objectiveId ?? ''),
    queryFn: () => api.get<Projection>(`/api/objectives/${objectiveId}/projection`),
    enabled: !!objectiveId,
  })
}
