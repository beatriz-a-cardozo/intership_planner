import type { AccountType, ExpenseScope, ExpenseStatus, Recurrence, TargetMode } from '@/api/types'

export const RECURRENCE_LABELS: Record<Recurrence, string> = {
  ONE_TIME: 'Único',
  MONTHLY: 'Mensal',
  SEMIANNUAL: 'Semestral',
  ANNUAL: 'Anual',
  PER_UNIT: 'Por unidade',
  CUSTOM: 'Personalizado',
}

export const RECURRENCE_HINTS: Record<Recurrence, string> = {
  ONE_TIME: 'Pago uma vez.',
  MONTHLY: 'Repete todo mês durante o objetivo.',
  SEMIANNUAL: 'Repete a cada semestre (ex.: taxa da universidade).',
  ANNUAL: 'Repete uma vez por ano.',
  PER_UNIT: 'Preço por unidade × quantidade (ex.: 5 inscrições).',
  CUSTOM: 'Você define quantas vezes ocorre.',
}

export const STATUS_LABELS: Record<ExpenseStatus, string> = {
  PENDING: 'Pendente',
  PARTIALLY_PAID: 'Parcial',
  PAID: 'Pago',
  CANCELLED: 'Cancelado',
}

export const STATUS_TONES: Record<ExpenseStatus, 'neutral' | 'primary' | 'accent' | 'danger'> = {
  PENDING: 'neutral',
  PARTIALLY_PAID: 'accent',
  PAID: 'primary',
  CANCELLED: 'neutral',
}

export const ACCOUNT_TYPE_LABELS: Record<AccountType, string> = {
  CHECKING: 'Conta corrente',
  SAVINGS: 'Poupança',
  INVESTMENT: 'Investimento',
  CASH: 'Espécie',
  BLOCKED: 'Conta bloqueada (Sperrkonto)',
  OTHER: 'Outro',
}

export const TARGET_MODE_LABELS: Record<TargetMode, string> = {
  DERIVED: 'Calculada pelos gastos',
  MANUAL: 'Valor fixo',
}

export const EXPENSE_SCOPE_LABELS: Record<ExpenseScope, string> = {
  REQUIRED_ONLY: 'Só gastos obrigatórios',
  ALL: 'Todos os gastos',
}

/** Países mais comuns para intercâmbio; o backend aceita qualquer código ISO de 2 letras. */
export const COUNTRIES: { code: string; name: string }[] = [
  { code: 'DE', name: 'Alemanha' },
  { code: 'AT', name: 'Áustria' },
  { code: 'BE', name: 'Bélgica' },
  { code: 'CA', name: 'Canadá' },
  { code: 'ES', name: 'Espanha' },
  { code: 'US', name: 'Estados Unidos' },
  { code: 'FI', name: 'Finlândia' },
  { code: 'FR', name: 'França' },
  { code: 'NL', name: 'Holanda' },
  { code: 'IE', name: 'Irlanda' },
  { code: 'IT', name: 'Itália' },
  { code: 'NO', name: 'Noruega' },
  { code: 'PT', name: 'Portugal' },
  { code: 'GB', name: 'Reino Unido' },
  { code: 'SE', name: 'Suécia' },
  { code: 'CH', name: 'Suíça' },
]

export function countryName(code: string | null | undefined) {
  if (!code) return null
  return COUNTRIES.find((c) => c.code === code)?.name ?? code
}

export function rateSourceLabel(source: string) {
  switch (source) {
    case 'FRANKFURTER':
      return 'BCE (Frankfurter)'
    case 'AWESOMEAPI':
      return 'AwesomeAPI'
    case 'MANUAL':
      return 'Definida por você'
    case 'FALLBACK':
      return 'Reserva (sem conexão com provedores)'
    default:
      return source
  }
}
