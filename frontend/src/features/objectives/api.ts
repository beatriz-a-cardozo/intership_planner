import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '@/api/client'
import { invalidateMoney, qk } from '@/api/query-keys'
import type {
  Adjustment,
  AdjustmentRequest,
  Dashboard,
  Objective,
  ObjectiveProgress,
  ObjectiveRequest,
} from '@/api/types'

export function useDashboard(objectiveId?: string) {
  return useQuery({
    queryKey: qk.dashboard(objectiveId),
    queryFn: () => api.get<Dashboard>(`/api/dashboard${objectiveId ? `?objectiveId=${objectiveId}` : ''}`),
  })
}

export function useObjectives() {
  return useQuery({ queryKey: qk.objectives, queryFn: () => api.get<Objective[]>('/api/objectives') })
}

export function useObjective(id: string) {
  return useQuery({ queryKey: qk.objective(id), queryFn: () => api.get<Objective>(`/api/objectives/${id}`) })
}

export function useObjectiveProgress(id: string) {
  return useQuery({
    queryKey: qk.progress(id),
    queryFn: () => api.get<ObjectiveProgress>(`/api/objectives/${id}/progress`),
  })
}

function useMoneyMutation<TVars, TResult>(fn: (vars: TVars) => Promise<TResult>) {
  const client = useQueryClient()
  return useMutation({ mutationFn: fn, onSuccess: () => invalidateMoney(client) })
}

export function useSaveObjective() {
  return useMoneyMutation(({ id, body }: { id?: string; body: ObjectiveRequest }) =>
    id ? api.put<Objective>(`/api/objectives/${id}`, body) : api.post<Objective>('/api/objectives', body),
  )
}

export function useDeleteObjective() {
  return useMoneyMutation((id: string) => api.delete<void>(`/api/objectives/${id}`))
}

export function useMakePrimary() {
  return useMoneyMutation((id: string) => api.put<Objective>(`/api/objectives/${id}/primary`))
}

export function useAdjustments(objectiveId: string) {
  return useQuery({
    queryKey: qk.adjustments(objectiveId),
    queryFn: () => api.get<Adjustment[]>(`/api/objectives/${objectiveId}/adjustments`),
  })
}

export function useSaveAdjustment(objectiveId: string) {
  return useMoneyMutation(({ id, body }: { id?: string; body: AdjustmentRequest }) =>
    id
      ? api.put<Adjustment>(`/api/objectives/${objectiveId}/adjustments/${id}`, body)
      : api.post<Adjustment>(`/api/objectives/${objectiveId}/adjustments`, body),
  )
}

export function useDeleteAdjustment(objectiveId: string) {
  return useMoneyMutation((id: string) =>
    api.delete<void>(`/api/objectives/${objectiveId}/adjustments/${id}`),
  )
}
