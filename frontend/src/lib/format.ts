/**
 * Formatação apenas (regra do projeto: o frontend formata, não calcula). Valores chegam prontos da API.
 */
import type { Money } from '@/api/types'

const LOCALE = 'pt-BR'
const moneyFormatters = new Map<string, Intl.NumberFormat>()

function moneyFormatter(currency: string, compact: boolean) {
  const key = `${currency}:${compact}`
  let f = moneyFormatters.get(key)
  if (!f) {
    f = new Intl.NumberFormat(LOCALE, {
      style: 'currency',
      currency,
      ...(compact ? { notation: 'compact', maximumFractionDigits: 1 } : {}),
    })
    moneyFormatters.set(key, f)
  }
  return f
}

/** "R$ 1.234,56" / "€ 855,58". `compact` = "R$ 95,5 mil". */
export function formatMoney(value: Money | null | undefined, opts: { compact?: boolean } = {}) {
  if (!value) return '—'
  return moneyFormatter(value.currency, !!opts.compact).format(value.amount)
}

export function formatAmount(amount: number, currency: string, opts: { compact?: boolean } = {}) {
  return moneyFormatter(currency, !!opts.compact).format(amount)
}

/** Data ISO (yyyy-MM-dd) sem deslocamento de fuso: "15/05/2027". */
export function formatDate(iso: string | null | undefined, style: 'short' | 'medium' | 'long' = 'short') {
  if (!iso) return '—'
  const [y, m, d] = iso.slice(0, 10).split('-').map(Number)
  const date = new Date(y!, m! - 1, d!)
  const options: Intl.DateTimeFormatOptions =
    style === 'short'
      ? { day: '2-digit', month: '2-digit', year: 'numeric' }
      : style === 'medium'
        ? { day: '2-digit', month: 'short', year: 'numeric' }
        : { day: 'numeric', month: 'long', year: 'numeric' }
  return new Intl.DateTimeFormat(LOCALE, options).format(date)
}

/** Instante ISO → "21/10 08:00" no fuso do navegador. */
export function formatDateTime(iso: string | null | undefined) {
  if (!iso) return '—'
  return new Intl.DateTimeFormat(LOCALE, {
    day: '2-digit',
    month: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(iso))
}

/** 25.4 → "25,4%". */
export function formatPercent(value: number, fractionDigits = 0) {
  return `${value.toLocaleString(LOCALE, { minimumFractionDigits: fractionDigits, maximumFractionDigits: fractionDigits })}%`
}

/** Cotação: "6,1234". */
export function formatRate(rate: number) {
  return rate.toLocaleString(LOCALE, { minimumFractionDigits: 2, maximumFractionDigits: 4 })
}

/** Data de hoje em ISO local (yyyy-MM-dd), para valores padrão de formulários. */
export function todayIso() {
  const d = new Date()
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

export function pluralize(count: number, singular: string, plural: string) {
  return `${count} ${count === 1 ? singular : plural}`
}
