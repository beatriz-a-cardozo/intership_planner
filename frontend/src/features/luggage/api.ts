import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '@/api/client'
import { qk } from '@/api/query-keys'
import type { BagRequest, ItemRequest, LuggageBag, LuggageItem, LuggageSummary } from '@/api/types'

export function useLuggageSummary() {
  return useQuery({
    queryKey: [...qk.luggage, 'summary'],
    queryFn: () => api.get<LuggageSummary>('/api/luggage/summary'),
  })
}

export function useLuggageItems() {
  return useQuery({
    queryKey: [...qk.luggage, 'items'],
    queryFn: () => api.get<LuggageItem[]>('/api/luggage/items'),
  })
}

function useLuggageMutation<TVars, TResult>(fn: (vars: TVars) => Promise<TResult>) {
  const client = useQueryClient()
  return useMutation({ mutationFn: fn, onSuccess: () => client.invalidateQueries({ queryKey: qk.luggage }) })
}

export function useSaveBag() {
  return useLuggageMutation(({ id, body }: { id?: string; body: BagRequest }) =>
    id
      ? api.put<LuggageBag>(`/api/luggage/bags/${id}`, body)
      : api.post<LuggageBag>('/api/luggage/bags', body),
  )
}

export function useDeleteBag() {
  return useLuggageMutation((id: string) => api.delete<void>(`/api/luggage/bags/${id}`))
}

export function useSaveItem() {
  return useLuggageMutation(({ id, body }: { id?: string; body: ItemRequest }) =>
    id
      ? api.put<LuggageItem>(`/api/luggage/items/${id}`, body)
      : api.post<LuggageItem>('/api/luggage/items', body),
  )
}

export function usePackItem() {
  return useLuggageMutation(({ id, packed }: { id: string; packed: boolean }) =>
    api.patch<LuggageItem>(`/api/luggage/items/${id}/packed`, { packed }),
  )
}

export function useDeleteItem() {
  return useLuggageMutation((id: string) => api.delete<void>(`/api/luggage/items/${id}`))
}
