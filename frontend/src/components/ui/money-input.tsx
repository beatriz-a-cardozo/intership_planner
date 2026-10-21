import type { ComponentProps } from 'react'
import { Input } from './input'
import { cn } from '@/lib/utils'

const SYMBOLS: Record<string, string> = { BRL: 'R$', EUR: '€', USD: 'US$' }

/** Campo de valor com o símbolo da moeda à esquerda; teclado numérico no celular. */
export function MoneyInput({
  currency,
  className,
  ...props
}: ComponentProps<'input'> & { currency: string }) {
  return (
    <div className="relative">
      <span
        className="pointer-events-none absolute left-3.5 top-1/2 -translate-y-1/2 text-sm text-muted"
        aria-hidden
      >
        {SYMBOLS[currency] ?? currency}
      </span>
      <Input inputMode="decimal" autoComplete="off" className={cn('pl-11 tabular', className)} {...props} />
    </div>
  )
}
