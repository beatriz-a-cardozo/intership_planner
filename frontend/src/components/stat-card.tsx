import type { LucideIcon } from 'lucide-react'
import type { ReactNode } from 'react'
import { Card } from '@/components/ui/card'
import { cn } from '@/lib/utils'

interface StatCardProps {
  label: string
  value: ReactNode
  hint?: ReactNode
  icon?: LucideIcon
  tone?: 'default' | 'primary' | 'accent' | 'danger'
  className?: string
}

/** Indicador único: rótulo, número grande e uma linha de contexto. */
export function StatCard({ label, value, hint, icon: Icon, tone = 'default', className }: StatCardProps) {
  return (
    <Card className={cn('p-4 sm:p-5', className)}>
      <div className="flex items-center justify-between gap-2">
        <p className="text-xs font-medium uppercase tracking-[0.1em] text-muted">{label}</p>
        {Icon && (
          <span
            className={cn(
              'grid size-8 place-items-center rounded-lg',
              tone === 'primary' && 'bg-primary-soft text-primary',
              tone === 'accent' && 'bg-accent-soft text-accent',
              tone === 'danger' && 'bg-danger-soft text-danger',
              tone === 'default' && 'bg-surface-2 text-muted',
            )}
            aria-hidden
          >
            <Icon className="size-4" />
          </span>
        )}
      </div>
      <p className="mt-2 text-2xl font-semibold tabular tracking-tight">{value}</p>
      {hint && <div className="mt-1 text-xs text-muted">{hint}</div>}
    </Card>
  )
}
