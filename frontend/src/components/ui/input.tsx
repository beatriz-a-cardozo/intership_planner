import type { ComponentProps } from 'react'
import { cn } from '@/lib/utils'

export function Input({ className, ...props }: ComponentProps<'input'>) {
  return (
    <input
      className={cn(
        'h-11 w-full rounded-xl border border-border bg-surface px-3.5 text-base text-foreground placeholder:text-subtle sm:text-sm',
        'transition-colors focus-visible:border-ring focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring/25',
        'aria-[invalid=true]:border-danger aria-[invalid=true]:ring-danger/20 disabled:opacity-60',
        className,
      )}
      {...props}
    />
  )
}
