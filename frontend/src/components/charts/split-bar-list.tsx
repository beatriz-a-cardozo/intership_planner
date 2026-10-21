import { cn } from '@/lib/utils'

export interface SplitBarRow {
  key: string
  label: string
  /** Cor de identidade da categoria/ciclo (só no marcador ao lado do rótulo, nunca no texto). */
  dotColor?: string | null
  total: number
  paid: number
  totalLabel: string
  paidLabel: string
  remainingLabel: string
  count: number
}

interface SplitBarListProps {
  rows: SplitBarRow[]
  ariaLabel: string
  className?: string
}

/**
 * Barras horizontais de série única (magnitude): comprimento = total do grupo; dentro dela,
 * o trecho pago (cheio) e o restante (claro), separados por 2px. Legenda fixa no topo; cada linha
 * é focável e mostra os valores em tooltip no hover/foco. Os números ficam em texto, não só na cor.
 */
export function SplitBarList({ rows, ariaLabel, className }: SplitBarListProps) {
  const max = Math.max(...rows.map((r) => r.total), 0)

  return (
    <div className={className}>
      <div className="mb-3 flex items-center gap-4 text-xs text-muted" aria-hidden>
        <span className="flex items-center gap-1.5">
          <span className="h-2.5 w-4 rounded-[3px] bg-primary" /> Pago
        </span>
        <span className="flex items-center gap-1.5">
          <span className="h-2.5 w-4 rounded-[3px] bg-primary/30" /> A pagar
        </span>
      </div>
      <ul className="grid gap-3.5" aria-label={ariaLabel}>
        {rows.map((r) => {
          const width = max > 0 ? (r.total / max) * 100 : 0
          const paidShare = r.total > 0 ? Math.min(1, r.paid / r.total) : 0
          return (
            <li
              key={r.key}
              tabIndex={0}
              className="group relative rounded-lg outline-none focus-visible:ring-2 focus-visible:ring-ring/40"
              aria-label={`${r.label}: total ${r.totalLabel}, pago ${r.paidLabel}, a pagar ${r.remainingLabel}`}
            >
              <div className="mb-1.5 flex items-baseline justify-between gap-3 text-sm">
                <span className="flex min-w-0 items-center gap-2">
                  {r.dotColor && (
                    <span
                      aria-hidden
                      className="size-2.5 shrink-0 rounded-full"
                      style={{ backgroundColor: r.dotColor }}
                    />
                  )}
                  <span className="truncate">{r.label}</span>
                  <span className="shrink-0 text-xs text-subtle">{r.count}</span>
                </span>
                <span className="shrink-0 font-medium tabular">{r.totalLabel}</span>
              </div>
              {/* área de hover maior que a barra */}
              <div className="flex h-2.5 items-stretch" style={{ width: `${Math.max(width, 1.5)}%` }}>
                {paidShare > 0 && (
                  <span
                    className={cn('bg-primary', paidShare < 1 ? 'rounded-l-[4px]' : 'rounded-[4px]')}
                    style={{ width: `${paidShare * 100}%` }}
                  />
                )}
                {paidShare < 1 && (
                  <span
                    className={cn(
                      'flex-1 bg-primary/30',
                      paidShare > 0 ? 'ml-[2px] rounded-r-[4px]' : 'rounded-[4px]',
                    )}
                  />
                )}
              </div>
              <div
                role="tooltip"
                className="pointer-events-none absolute -top-2 right-0 z-10 hidden -translate-y-full rounded-xl border border-border bg-surface px-3 py-2 text-xs shadow-card group-hover:block group-focus-visible:block"
              >
                <p className="font-medium text-foreground">{r.label}</p>
                <dl className="mt-1 grid grid-cols-[auto_auto] gap-x-3 gap-y-0.5 tabular">
                  <dt className="text-muted">Total</dt>
                  <dd className="text-right text-foreground">{r.totalLabel}</dd>
                  <dt className="text-muted">Pago</dt>
                  <dd className="text-right text-foreground">{r.paidLabel}</dd>
                  <dt className="text-muted">A pagar</dt>
                  <dd className="text-right text-foreground">{r.remainingLabel}</dd>
                </dl>
              </div>
            </li>
          )
        })}
      </ul>
    </div>
  )
}
