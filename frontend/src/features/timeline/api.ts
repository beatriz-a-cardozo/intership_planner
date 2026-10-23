import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '@/api/client'
import { invalidatePlanning, qk } from '@/api/query-keys'
import type { EventRequest, TimelineItem, TimelineResponse, TimelineSource } from '@/api/types'

export interface TimelineParams {
  from?: string
  to?: string
  sources?: TimelineSource[]
}

export function useTimeline(params: TimelineParams) {
  const s = new URLSearchParams()
  if (params.from) s.set('from', params.from)
  if (params.to) s.set('to', params.to)
  params.sources?.forEach((src) => s.append('sources', src))
  const qs = s.toString()
  return useQuery({
    queryKey: qk.timeline(params),
    queryFn: () => api.get<TimelineResponse>(`/api/timeline${qs ? `?${qs}` : ''}`),
    placeholderData: (previous) => previous,
  })
}

function usePlanningMutation<TVars, TResult>(fn: (vars: TVars) => Promise<TResult>) {
  const client = useQueryClient()
  return useMutation({ mutationFn: fn, onSuccess: () => invalidatePlanning(client) })
}

export function useSaveEvent() {
  return usePlanningMutation(({ id, body }: { id?: string; body: EventRequest }) =>
    id
      ? api.put<TimelineItem>(`/api/timeline/events/${id}`, body)
      : api.post<TimelineItem>('/api/timeline/events', body),
  )
}

export function useEventDone() {
  return usePlanningMutation(({ id, done }: { id: string; done: boolean }) =>
    api.patch<TimelineItem>(`/api/timeline/events/${id}/done`, { done }),
  )
}

export function useDeleteEvent() {
  return usePlanningMutation((id: string) => api.delete<void>(`/api/timeline/events/${id}`))
}
