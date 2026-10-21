import { ChevronDown } from 'lucide-react'
import type { ComponentProps } from 'react'
import { cn } from '@/lib/utils'

/** Select nativo estilizado: melhor experiência em mobile (picker do sistema) e acessível por padrão. */
export function NativeSelect({ className, children, ...props }: ComponentProps<'select'>) {
  return (
    <div className="relative">
      <select
        className={cn(
          'h-11 w-full appearance-none rounded-xl border border-border bg-surface pl-3.5 pr-10 text-base text-foreground sm:text-sm',
          'focus-visible:border-ring focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring/25',
          className,
        )}
        {...props}
      >
        {children}
      </select>
      <ChevronDown className="pointer-events-none absolute right-3 top-1/2 size-4 -translate-y-1/2 text-muted" aria-hidden />
    </div>
  )
}
