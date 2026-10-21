import { cn } from '@/lib/utils'

interface SplitMeterProps {
  /** 0..1 */
  share: number
  filledLabel: string
  filledValue: string
  restLabel: string
  restValue: string
  className?: string
}

/** Medidor de duas partes (ex.: pago × a pagar) com legenda e valores em texto. */
export function SplitMeter({
  share,
  filledLabel,
  filledValue,
  restLabel,
  restValue,
  className,
}: SplitMeterProps) {
  const pct = Math.min(1, Math.max(0, share)) * 100
  return (
    <div className={className}>
      <div
        className="flex h-3 w-full"
        role="meter"
        aria-valuemin={0}
        aria-valuemax={100}
        aria-valuenow={Math.round(pct)}
        aria-label={`${filledLabel}: ${filledValue}; ${restLabel}: ${restValue}`}
      >
        {pct > 0 && (
          <span
            className={cn('bg-primary', pct < 100 ? 'rounded-l-[4px]' : 'rounded-[4px]')}
            style={{ width: `${pct}%` }}
          />
        )}
        {pct < 100 && (
          <span
            className={cn('flex-1 bg-surface-2', pct > 0 ? 'ml-[2px] rounded-r-[4px]' : 'rounded-[4px]')}
          />
        )}
      </div>
      <div className="mt-2.5 flex flex-wrap justify-between gap-x-4 gap-y-1 text-sm">
        <span className="flex items-center gap-2">
          <span aria-hidden className="h-2.5 w-4 rounded-[3px] bg-primary" />
          <span className="text-muted">{filledLabel}</span>
          <span className="font-medium tabular">{filledValue}</span>
        </span>
        <span className="flex items-center gap-2">
          <span aria-hidden className="h-2.5 w-4 rounded-[3px] bg-surface-2 ring-1 ring-border" />
          <span className="text-muted">{restLabel}</span>
          <span className="font-medium tabular">{restValue}</span>
        </span>
      </div>
    </div>
  )
}
