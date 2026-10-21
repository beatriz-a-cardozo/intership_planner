import type { QueryClient } from '@tanstack/react-query'

/** Chaves de cache por domínio. Prefixos permitem invalidar um domínio inteiro. */
export const qk = {
  dashboard: (objectiveId?: string) => ['dashboard', objectiveId ?? 'primary'] as const,
  objectives: ['objectives'] as const,
  objective: (id: string) => ['objectives', id] as const,
  progress: (id: string) => ['progress', id] as const,
  adjustments: (objectiveId: string) => ['adjustments', objectiveId] as const,
  accounts: ['accounts'] as const,
  accountsSummary: ['accounts', 'summary'] as const,
  balances: (accountId: string) => ['accounts', accountId, 'balances'] as const,
  exchangeCurrent: (base: string, quote: string) => ['exchange', 'current', base, quote] as const,
  exchangeHistory: (base: string, quote: string) => ['exchange', 'history', base, quote] as const,
  expenses: (params: object) => ['expenses', 'list', params] as const,
  expense: (id: string) => ['expenses', 'detail', id] as const,
  expenseSummary: (objectiveId: string | undefined, groupBy: string) =>
    ['expenses', 'summary', objectiveId ?? 'primary', groupBy] as const,
  categories: ['catalog', 'categories'] as const,
  cycles: ['catalog', 'cycles'] as const,
}

/**
 * Tudo que envolve dinheiro afeta dashboard, progresso e resumos: invalida em bloco
 * (as listas são pequenas; simplicidade > granularidade).
 */
export function invalidateMoney(client: QueryClient) {
  for (const prefix of [
    'dashboard',
    'objectives',
    'progress',
    'adjustments',
    'accounts',
    'expenses',
    'exchange',
  ]) {
    void client.invalidateQueries({ queryKey: [prefix] })
  }
}
