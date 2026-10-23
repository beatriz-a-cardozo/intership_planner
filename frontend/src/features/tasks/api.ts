import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '@/api/client'
import { invalidatePlanning, qk } from '@/api/query-keys'
import type { Priority, Task, TaskRequest, TaskStatus } from '@/api/types'

export interface TaskFilters {
  status?: TaskStatus
  priority?: Priority
}

function query(params: object) {
  const s = new URLSearchParams()
  Object.entries(params).forEach(([k, v]) => {
    if (v !== undefined && v !== null && v !== '') s.set(k, String(v))
  })
  const str = s.toString()
  return str ? `?${str}` : ''
}

export function useTasks(filters: TaskFilters = {}) {
  return useQuery({
    queryKey: qk.tasks(filters),
    queryFn: () => api.get<Task[]>(`/api/tasks${query(filters)}`),
    placeholderData: (previous) => previous,
  })
}

function usePlanningMutation<TVars, TResult>(fn: (vars: TVars) => Promise<TResult>) {
  const client = useQueryClient()
  return useMutation({ mutationFn: fn, onSuccess: () => invalidatePlanning(client) })
}

export function useSaveTask() {
  return usePlanningMutation(({ id, body }: { id?: string; body: TaskRequest }) =>
    id ? api.put<Task>(`/api/tasks/${id}`, body) : api.post<Task>('/api/tasks', body),
  )
}

export function useTaskStatus() {
  return usePlanningMutation(({ id, status }: { id: string; status: TaskStatus }) =>
    api.patch<Task>(`/api/tasks/${id}/status`, { status }),
  )
}

export function useDeleteTask() {
  return usePlanningMutation((id: string) => api.delete<void>(`/api/tasks/${id}`))
}
