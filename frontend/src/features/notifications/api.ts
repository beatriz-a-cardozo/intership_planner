import { useInfiniteQuery, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '@/api/client'
import { qk } from '@/api/query-keys'
import type {
  AppNotification,
  ExchangeAlert,
  ExchangeAlertRequest,
  NotificationCategory,
  NotificationPage,
  NotificationPreference,
  NotificationPreferenceRequest,
} from '@/api/types'

const PAGE_SIZE = 20

export function useNotifications(unreadOnly: boolean) {
  return useInfiniteQuery({
    queryKey: qk.notifications({ unreadOnly }),
    initialPageParam: 0,
    queryFn: ({ pageParam }) =>
      api.get<NotificationPage>(
        `/api/notifications?unread=${unreadOnly}&page=${pageParam}&size=${PAGE_SIZE}`,
      ),
    getNextPageParam: (last, pages) => (last.hasMore ? pages.length : undefined),
  })
}

/** Badge do sino: atualiza a cada minuto. */
export function useUnreadCount() {
  return useQuery({
    queryKey: qk.unreadCount,
    queryFn: () => api.get<{ count: number }>('/api/notifications/unread-count'),
    refetchInterval: 60_000,
  })
}

/** Roda os detectores para a pessoa (idempotente). Chamado uma vez ao abrir o app. */
export function useRefreshNotifications() {
  const client = useQueryClient()
  return useMutation({
    mutationFn: () => api.post<{ created: number }>('/api/notifications/refresh'),
    onSuccess: () => client.invalidateQueries({ queryKey: ['notifications'] }),
  })
}

function useNotificationMutation<TVars, TResult>(fn: (vars: TVars) => Promise<TResult>) {
  const client = useQueryClient()
  return useMutation({
    mutationFn: fn,
    onSuccess: () => client.invalidateQueries({ queryKey: ['notifications'] }),
  })
}

export function useMarkRead() {
  return useNotificationMutation((id: string) => api.patch<AppNotification>(`/api/notifications/${id}/read`))
}

export function useMarkAllRead() {
  return useNotificationMutation(() => api.post<{ count: number }>('/api/notifications/read-all'))
}

export function useDeleteNotification() {
  return useNotificationMutation((id: string) => api.delete<void>(`/api/notifications/${id}`))
}

export function useNotificationPreferences() {
  return useQuery({
    queryKey: qk.notificationPreferences,
    queryFn: () => api.get<NotificationPreference[]>('/api/notification-preferences'),
  })
}

export function useSavePreference() {
  return useNotificationMutation(
    ({ category, body }: { category: NotificationCategory; body: NotificationPreferenceRequest }) =>
      api.put<NotificationPreference>(`/api/notification-preferences/${category}`, body),
  )
}

export function useExchangeAlerts() {
  return useQuery({
    queryKey: qk.exchangeAlerts,
    queryFn: () => api.get<ExchangeAlert[]>('/api/exchange-rate-alerts'),
  })
}

export function useSaveExchangeAlert() {
  return useNotificationMutation(({ id, body }: { id?: string; body: ExchangeAlertRequest }) =>
    id
      ? api.put<ExchangeAlert>(`/api/exchange-rate-alerts/${id}`, body)
      : api.post<ExchangeAlert>('/api/exchange-rate-alerts', body),
  )
}

export function useDeleteExchangeAlert() {
  return useNotificationMutation((id: string) => api.delete<void>(`/api/exchange-rate-alerts/${id}`))
}
