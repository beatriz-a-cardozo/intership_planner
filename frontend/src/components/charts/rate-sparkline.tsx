import { useId, useMemo, useState, type PointerEvent } from 'react'
import type { RatePoint } from '@/api/types'
import { formatDate, formatRate } from '@/lib/format'

interface RateSparklineProps {
  points: RatePoint[]
  height?: number
  /** Rótulo acessível, ex.: "Euro em reais, últimos 90 dias". */
  label: string
}

const W = 320

/**
 * Linha da cotação (série única): traço de 2px, área suave, crosshair + tooltip no hover/toque.
 * Mínimo e máximo do período ficam em texto abaixo (não dependem do gráfico).
 */
export function RateSparkline({ points, height = 72, label }: RateSparklineProps) {
  const gradientId = useId()
  const [active, setActive] = useState<number | null>(null)

  const geometry = useMemo(() => {
    if (points.length < 2) return null
    const rates = points.map((p) => p.rate)
    const min = Math.min(...rates)
    const max = Math.max(...rates)
    const pad = (max - min) * 0.12 || max * 0.01
    const lo = min - pad
    const hi = max + pad
    const xy = points.map((p, i) => ({
      x: (i / (points.length - 1)) * W,
      y: height - ((p.rate - lo) / (hi - lo)) * height,
    }))
    const line = xy.map((p, i) => `${i ? 'L' : 'M'}${p.x.toFixed(1)},${p.y.toFixed(1)}`).join(' ')
    const area = `${line} L${W},${height} L0,${height} Z`
    return { xy, line, area, min, max }
  }, [points, height])

  if (!geometry) {
    return <p className="text-sm text-muted">Histórico ainda indisponível.</p>
  }

  const onMove = (e: PointerEvent<SVGSVGElement>) => {
    const rect = e.currentTarget.getBoundingClientRect()
    const ratio = (e.clientX - rect.left) / rect.width
    setActive(Math.round(Math.min(1, Math.max(0, ratio)) * (points.length - 1)))
  }

  const current = active != null ? points[active] : null
  const currentXY = active != null ? geometry.xy[active] : null

  return (
    <figure className="relative">
      <svg
        viewBox={`0 0 ${W} ${height}`}
        preserveAspectRatio="none"
        className="h-[72px] w-full touch-none overflow-visible"
        style={{ height }}
        role="img"
        aria-label={`${label}: mínima ${formatRate(geometry.min)}, máxima ${formatRate(geometry.max)}`}
        onPointerMove={onMove}
        onPointerDown={onMove}
        onPointerLeave={() => setActive(null)}
      >
        <defs>
          <linearGradient id={gradientId} x1="0" x2="0" y1="0" y2="1">
            <stop offset="0%" stopColor="var(--primary)" stopOpacity="0.18" />
            <stop offset="100%" stopColor="var(--primary)" stopOpacity="0" />
          </linearGradient>
        </defs>
        <path d={geometry.area} fill={`url(#${gradientId})`} />
        <path
          d={geometry.line}
          fill="none"
          stroke="var(--primary)"
          strokeWidth={2}
          vectorEffect="non-scaling-stroke"
          strokeLinejoin="round"
        />
        {currentXY && (
          <line
            x1={currentXY.x}
            x2={currentXY.x}
            y1={0}
            y2={height}
            stroke="var(--border)"
            strokeWidth={1}
            vectorEffect="non-scaling-stroke"
          />
        )}
      </svg>
      {current && currentXY && (
        <div
          role="tooltip"
          className="pointer-events-none absolute -top-2 z-10 -translate-x-1/2 -translate-y-full whitespace-nowrap rounded-lg border border-border bg-surface px-2.5 py-1.5 text-xs shadow-card"
          style={{ left: `${(currentXY.x / W) * 100}%` }}
        >
          <span className="text-muted">{formatDate(current.date)}</span>{' '}
          <span className="font-medium tabular text-foreground">{formatRate(current.rate)}</span>
        </div>
      )}
      <figcaption className="mt-1.5 flex justify-between text-xs text-muted tabular">
        <span>mín. {formatRate(geometry.min)}</span>
        <span>máx. {formatRate(geometry.max)}</span>
      </figcaption>
    </figure>
  )
}
