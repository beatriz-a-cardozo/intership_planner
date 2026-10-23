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
  financeCategories: ['finance', 'categories'] as const,
  financeSummary: (month: string) => ['finance', 'summary', month] as const,
  financeOverview: ['finance', 'overview'] as const,
  commitments: ['finance', 'commitments'] as const,
  installmentPlans: ['finance', 'installments'] as const,
  recurring: ['finance', 'recurring'] as const,
  projection: (objectiveId: string) => ['progress', 'projection', objectiveId] as const,
  universities: (params: object) => ['universities', 'list', params] as const,
  university: (id: string) => ['universities', 'detail', id] as const,
  programs: (params: object) => ['programs', 'list', params] as const,
  program: (id: string) => ['programs', 'detail', id] as const,
  compare: (ids: string[]) => ['programs', 'compare', ...ids] as const,
  applications: (params: object) => ['applications', 'list', params] as const,
  application: (id: string) => ['applications', 'detail', id] as const,
  applicationHistory: (id: string) => ['applications', 'history', id] as const,
  applicationRequests: (id: string) => ['applications', 'requests', id] as const,
  universityImages: (id: string) => ['universities', 'images', id] as const,
  universityLocations: ['universities', 'locations'] as const,
  imageContent: (universityId: string, imageId: string) => ['image-content', universityId, imageId] as const,
  tasks: (params: object) => ['tasks', params] as const,
  timeline: (params: object) => ['timeline', params] as const,
  luggage: ['luggage'] as const,
  notifications: (params: object) => ['notifications', 'list', params] as const,
  unreadCount: ['notifications', 'unread'] as const,
  notificationPreferences: ['notifications', 'preferences'] as const,
  exchangeAlerts: ['notifications', 'alerts'] as const,
  importSession: (id: string) => ['imports', id] as const,
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
    'finance',
    'timeline',
    'notifications',
  ]) {
    void client.invalidateQueries({ queryKey: [prefix] })
  }
}

/** Universidades, programas e candidaturas se afetam mutuamente (escolhida ↔ candidatura ↔ dashboard). */
export function invalidateUniversities(client: QueryClient) {
  for (const prefix of ['universities', 'programs', 'applications', 'dashboard', 'timeline', 'tasks']) {
    void client.invalidateQueries({ queryKey: [prefix] })
  }
}

/** Tarefas e eventos aparecem na timeline e nos próximos itens do dashboard. */
export function invalidatePlanning(client: QueryClient) {
  for (const prefix of ['tasks', 'timeline', 'dashboard']) {
    void client.invalidateQueries({ queryKey: [prefix] })
  }
}
