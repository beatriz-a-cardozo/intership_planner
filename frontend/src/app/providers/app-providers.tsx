import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { useState, type ReactNode } from 'react'
import { Toaster } from 'sonner'
import { ApiError } from '@/api/errors'
import { AuthProvider } from './auth-provider'
import { ThemeProvider, useTheme } from './theme-provider'

function makeQueryClient() {
  return new QueryClient({
    defaultOptions: {
      queries: {
        staleTime: 30_000,
        refetchOnWindowFocus: false,
        // Não insiste em erros do cliente (4xx): só em falhas de rede/servidor.
        retry: (count, error) => !(error instanceof ApiError && error.status < 500) && count < 2,
      },
    },
  })
}

function ThemedToaster() {
  const { resolved } = useTheme()
  return <Toaster theme={resolved} position="top-center" richColors closeButton />
}

export function AppProviders({ children }: { children: ReactNode }) {
  const [queryClient] = useState(makeQueryClient)
  return (
    <ThemeProvider>
      <QueryClientProvider client={queryClient}>
        <AuthProvider>{children}</AuthProvider>
        <ThemedToaster />
      </QueryClientProvider>
    </ThemeProvider>
  )
}
