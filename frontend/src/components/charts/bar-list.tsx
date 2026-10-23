export interface BarListRow {
  key: string
  label: string
  value: number
  valueLabel: string
  /** Cor de identidade só no marcador ao lado do nome (a barra usa um único tom: magnitude). */
  dotColor?: string | null
  hint?: string
}

/** Barras horizontais de série única com o valor em texto (ex.: despesas por categoria). */
export function BarList({ rows, ariaLabel }: { rows: BarListRow[]; ariaLabel: string }) {
  const max = Math.max(...rows.map((r) => r.value), 0)
  return (
    <ul className="grid gap-3" aria-label={ariaLabel}>
      {rows.map((r) => (
        <li key={r.key} aria-label={`${r.label}: ${r.valueLabel}`}>
          <div className="mb-1 flex items-baseline justify-between gap-3 text-sm">
            <span className="flex min-w-0 items-center gap-2">
              {r.dotColor && (
                <span
                  aria-hidden
                  className="size-2.5 shrink-0 rounded-full"
                  style={{ backgroundColor: r.dotColor }}
                />
              )}
              <span className="truncate">{r.label}</span>
              {r.hint && <span className="shrink-0 text-xs text-subtle">{r.hint}</span>}
            </span>
            <span className="shrink-0 font-medium tabular">{r.valueLabel}</span>
          </div>
          <div className="h-2 rounded-[4px] bg-surface-2">
            <div
              className="h-full rounded-[4px] bg-primary"
              style={{ width: `${max > 0 ? Math.max(1.5, (r.value / max) * 100) : 0}%` }}
            />
          </div>
        </li>
      ))}
    </ul>
  )
}
