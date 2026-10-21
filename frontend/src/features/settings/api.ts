import { useMutation, useQuery } from '@tanstack/react-query'
import { api } from '@/api/client'
import type { Currency, UpdateSettingsRequest, UserSettings } from '@/api/types'

export const settingsKeys = {
  currencies: ['currencies'] as const,
}

export function useCurrencies() {
  return useQuery({
    queryKey: settingsKeys.currencies,
    queryFn: () => api.get<Currency[]>('/api/currencies'),
    staleTime: Infinity,
  })
}

export function useUpdateSettings() {
  return useMutation({
    mutationFn: (body: UpdateSettingsRequest) => api.put<UserSettings>('/api/me/settings', body),
  })
}
