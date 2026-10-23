import { useId, useMemo, useState, type PointerEvent } from 'react'

export interface TrendPoint {
  key: string
  label: string
  title: string
  value: number
  valueLabel: string
}

interface TrendLineProps {
  points: TrendPoint[]
  ariaLabel: string
  /** Linha de referência tracejada (ex.: meta do objetivo). */
  reference?: { value: number; label: string }
  /** Ponto destacado (ex.: mês em que a meta é alcançada). */
  markKey?: string | null
  formatAxis: (value: number) => string
  height?: number
}

const W = 600

/**
 * Série única ao longo do tempo: traço de 2px, área suave, eixo com 3 marcas recessivas, linha de
 * referência tracejada com rótulo em texto, crosshair + tooltip no hover/toque.
 */
export function TrendLine({
  points,
  ariaLabel,
  reference,
  markKey,
  formatAxis,
  height = 160,
}: TrendLineProps) {
  const gradientId = useId()
  const [active, setActive] = useState<number | null>(null)

  const g = useMemo(() => {
    const values = points.map((p) => p.value).concat(reference ? [reference.value] : [])
    const min = Math.min(0, ...values)
    const max = Math.max(...values, 1)
    const span = max - min || 1
    const y = (v: number) => height - ((v - min) / span) * height
    const xy = points.map((p, i) => ({
      x: points.length === 1 ? W / 2 : (i / (points.length - 1)) * W,
      y: y(p.value),
    }))
    const line = xy.map((p, i) => `${i ? 'L' : 'M'}${p.x.toFixed(1)},${p.y.toFixed(1)}`).join(' ')
    const zeroY = y(0)
    return {
      xy,
      line,
      area: `${line} L${W},${zeroY} L0,${zeroY} Z`,
      ticks: [max, (max + min) / 2, min],
      y,
      refY: reference ? y(reference.value) : null,
      zeroY,
    }
  }, [points, reference, height])

  if (points.length === 0) return null

  const onMove = (e: PointerEvent<SVGSVGElement>) => {
    const rect = e.currentTarget.getBoundingClientRect()
    const ratio = (e.clientX - rect.left) / rect.width
    setActive(Math.round(Math.min(1, Math.max(0, ratio)) * (points.length - 1)))
  }

  const current = active != null ? points[active] : null
  const currentXY = active != null ? g.xy[active] : null
  const markIndex = markKey ? points.findIndex((p) => p.key === markKey) : -1

  return (
    <figure className="flex gap-2">
      <div
        className="flex shrink-0 flex-col justify-between py-0 text-right text-[11px] tabular text-muted"
        style={{ height }}
        aria-hidden
      >
        {g.ticks.map((t, i) => (
          <span key={i} className="-translate-y-1/2 first:translate-y-0 last:translate-y-0">
            {formatAxis(t)}
          </span>
        ))}
      </div>
      <div className="relative min-w-0 flex-1">
        <svg
          viewBox={`0 0 ${W} ${height}`}
          preserveAspectRatio="none"
          className="w-full touch-none overflow-visible"
          style={{ height }}
          role="img"
          aria-label={ariaLabel}
          onPointerMove={onMove}
          onPointerDown={onMove}
          onPointerLeave={() => setActive(null)}
        >
          <defs>
            <linearGradient id={gradientId} x1="0" x2="0" y1="0" y2="1">
              <stop offset="0%" stopColor="var(--primary)" stopOpacity="0.16" />
              <stop offset="100%" stopColor="var(--primary)" stopOpacity="0" />
            </linearGradient>
          </defs>
          {g.ticks.map((t, i) => (
            <line
              key={i}
              x1={0}
              x2={W}
              y1={g.y(t)}
              y2={g.y(t)}
              stroke="var(--border)"
              strokeDasharray="3 4"
              vectorEffect="non-scaling-stroke"
            />
          ))}
          <line
            x1={0}
            x2={W}
            y1={g.zeroY}
            y2={g.zeroY}
            stroke="var(--border)"
            vectorEffect="non-scaling-stroke"
          />
          {g.refY != null && (
            <line
              x1={0}
              x2={W}
              y1={g.refY}
              y2={g.refY}
              stroke="var(--accent)"
              strokeWidth={1.5}
              strokeDasharray="6 4"
              vectorEffect="non-scaling-stroke"
            />
          )}
          <path d={g.area} fill={`url(#${gradientId})`} />
          <path
            d={g.line}
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
              stroke="var(--muted)"
              strokeOpacity={0.4}
              vectorEffect="non-scaling-stroke"
            />
          )}
        </svg>
        {/* marcadores como HTML para não distorcer com preserveAspectRatio="none" */}
        {markIndex >= 0 && (
          <span
            aria-hidden
            className="absolute size-3 -translate-x-1/2 -translate-y-1/2 rounded-full border-2 border-surface bg-primary"
            style={{ left: `${(g.xy[markIndex]!.x / W) * 100}%`, top: g.xy[markIndex]!.y }}
          />
        )}
        {currentXY && (
          <span
            aria-hidden
            className="absolute size-2.5 -translate-x-1/2 -translate-y-1/2 rounded-full border-2 border-surface bg-primary"
            style={{ left: `${(currentXY.x / W) * 100}%`, top: currentXY.y }}
          />
        )}
        {reference && g.refY != null && (
          <span
            className="absolute right-0 -translate-y-full pb-0.5 text-[11px] font-medium text-accent"
            style={{ top: g.refY }}
          >
            {reference.label}
          </span>
        )}
        {current && currentXY && (
          <div
            role="tooltip"
            className="pointer-events-none absolute z-10 -translate-x-1/2 -translate-y-full whitespace-nowrap rounded-lg border border-border bg-surface px-2.5 py-1.5 text-xs shadow-card"
            style={{
              left: `${Math.min(88, Math.max(12, (currentXY.x / W) * 100))}%`,
              top: Math.max(0, currentXY.y - 10),
            }}
          >
            <span className="text-muted">{current.title}</span>{' '}
            <span className="font-medium tabular text-foreground">{current.valueLabel}</span>
          </div>
        )}
        <div className="mt-1.5 flex justify-between text-[11px] text-muted" aria-hidden>
          <span>{points[0]!.label}</span>
          {points.length > 2 && <span>{points[Math.floor(points.length / 2)]!.label}</span>}
          <span>{points[points.length - 1]!.label}</span>
        </div>
      </div>
    </figure>
  )
}
