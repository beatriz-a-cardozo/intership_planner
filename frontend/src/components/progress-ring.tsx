import { cn } from '@/lib/utils'
import { formatPercent } from '@/lib/format'

interface ProgressRingProps {
  /** 0–100 (já limitado pelo backend). */
  percent: number
  size?: number
  label?: string
  className?: string
}

/** Anel de progresso do objetivo. Traço fino com ponta arredondada; trilho neutro. */
export function ProgressRing({ percent, size = 168, label = 'do objetivo', className }: ProgressRingProps) {
  const stroke = Math.max(8, size / 14)
  const radius = (size - stroke) / 2
  const circumference = 2 * Math.PI * radius
  const clamped = Math.min(100, Math.max(0, percent))
  const offset = circumference * (1 - clamped / 100)

  return (
    <div
      className={cn('relative grid shrink-0 place-items-center', className)}
      style={{ width: size, height: size }}
      role="img"
      aria-label={`${formatPercent(clamped, 1)} ${label}`}
    >
      <svg width={size} height={size} viewBox={`0 0 ${size} ${size}`} className="-rotate-90">
        <circle
          cx={size / 2}
          cy={size / 2}
          r={radius}
          fill="none"
          strokeWidth={stroke}
          className="stroke-surface-2"
        />
        <circle
          cx={size / 2}
          cy={size / 2}
          r={radius}
          fill="none"
          strokeWidth={stroke}
          strokeLinecap="round"
          strokeDasharray={circumference}
          strokeDashoffset={offset}
          className="stroke-primary transition-[stroke-dashoffset] duration-700 ease-out"
        />
      </svg>
      <div className="absolute inset-0 grid place-items-center text-center">
        <div>
          <p
            className="font-display text-4xl font-semibold tabular tracking-tight"
            style={{ fontSize: size / 4.6 }}
          >
            {formatPercent(clamped, clamped > 0 && clamped < 10 ? 1 : 0)}
          </p>
          <p className="text-xs text-muted">{label}</p>
        </div>
      </div>
    </div>
  )
}
