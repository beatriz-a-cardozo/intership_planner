import { useState } from 'react'
import { cn } from '@/lib/utils'

export interface PairedBarPoint {
  key: string
  /** Rótulo curto do eixo (ex.: "nov"). */
  label: string
  /** Rótulo completo para o tooltip (ex.: "novembro de 2026"). */
  title: string
  a: number
  b: number
  aLabel: string
  bLabel: string
  /** Linha extra no tooltip (ex.: sobra do mês). */
  footer?: string
  highlight?: boolean
}

interface PairedBarChartProps {
  points: PairedBarPoint[]
  seriesA: string
  seriesB: string
  ariaLabel: string
  height?: number
}

/**
 * Duas séries por categoria (ex.: receitas × despesas por mês). Cores da paleta categórica validada
 * (tokens --series-income / --series-expense), barras finas com pontas arredondadas, legenda fixa,
 * grade recessiva e tooltip por coluna no hover/foco. Valores em texto no tooltip e na tabela acessível.
 */
export function PairedBarChart({ points, seriesA, seriesB, ariaLabel, height = 180 }: PairedBarChartProps) {
  const [active, setActive] = useState<number | null>(null)
  const max = Math.max(1, ...points.flatMap((p) => [p.a, p.b]))
  const ticks = [1, 0.5, 0]

  return (
    <figure>
      <div className="mb-3 flex items-center gap-4 text-xs text-muted" aria-hidden>
        <span className="flex items-center gap-1.5">
          <span className="h-2.5 w-3 rounded-[3px] bg-series-income" /> {seriesA}
        </span>
        <span className="flex items-center gap-1.5">
          <span className="h-2.5 w-3 rounded-[3px] bg-series-expense" /> {seriesB}
        </span>
      </div>
      <div className="relative" style={{ height }}>
        {ticks.map((t) => (
          <div
            key={t}
            aria-hidden
            className={cn(
              'absolute inset-x-0 border-t',
              t === 0 ? 'border-border' : 'border-dashed border-border/60',
            )}
            style={{ bottom: `${t * 100}%` }}
          />
        ))}
        <div className="absolute inset-0 flex items-end gap-1 sm:gap-2" role="list" aria-label={ariaLabel}>
          {points.map((p, i) => (
            <div
              key={p.key}
              role="listitem"
              tabIndex={0}
              aria-label={`${p.title}: ${seriesA} ${p.aLabel}, ${seriesB} ${p.bLabel}${p.footer ? `, ${p.footer}` : ''}`}
              className={cn(
                'group relative flex h-full flex-1 items-end justify-center gap-[2px] rounded-md outline-none focus-visible:ring-2 focus-visible:ring-ring/40',
                p.highlight && 'bg-surface-2/70',
              )}
              onMouseEnter={() => setActive(i)}
              onMouseLeave={() => setActive(null)}
              onFocus={() => setActive(i)}
              onBlur={() => setActive(null)}
            >
              <span
                className="w-full max-w-3 rounded-t-[4px] bg-series-income"
                style={{ height: `${(p.a / max) * 100}%`, minHeight: p.a > 0 ? 2 : 0 }}
              />
              <span
                className="w-full max-w-3 rounded-t-[4px] bg-series-expense"
                style={{ height: `${(p.b / max) * 100}%`, minHeight: p.b > 0 ? 2 : 0 }}
              />
              {active === i && (
                <div
                  role="tooltip"
                  className={cn(
                    'pointer-events-none absolute bottom-full z-10 mb-2 whitespace-nowrap rounded-xl border border-border bg-surface px-3 py-2 text-xs shadow-card',
                    i < points.length / 2 ? 'left-0' : 'right-0',
                  )}
                >
                  <p className="font-medium text-foreground">{p.title}</p>
                  <p className="mt-1 flex items-center gap-1.5">
                    <span className="size-2 rounded-full bg-series-income" aria-hidden />
                    <span className="text-muted">{seriesA}</span>
                    <span className="ml-auto pl-3 font-medium tabular text-foreground">{p.aLabel}</span>
                  </p>
                  <p className="flex items-center gap-1.5">
                    <span className="size-2 rounded-full bg-series-expense" aria-hidden />
                    <span className="text-muted">{seriesB}</span>
                    <span className="ml-auto pl-3 font-medium tabular text-foreground">{p.bLabel}</span>
                  </p>
                  {p.footer && <p className="mt-1 border-t border-border pt-1 text-muted">{p.footer}</p>}
                </div>
              )}
            </div>
          ))}
        </div>
      </div>
      <div className="mt-1.5 flex gap-1 sm:gap-2" aria-hidden>
        {points.map((p) => (
          <span
            key={p.key}
            className={cn(
              'flex-1 text-center text-[11px] text-muted',
              p.highlight && 'font-semibold text-foreground',
            )}
          >
            {p.label}
          </span>
        ))}
      </div>
    </figure>
  )
}
