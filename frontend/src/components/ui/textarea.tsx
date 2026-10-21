import type { ComponentProps } from 'react'
import { cn } from '@/lib/utils'

export function Textarea({ className, ...props }: ComponentProps<'textarea'>) {
  return (
    <textarea
      className={cn(
        'min-h-24 w-full rounded-xl border border-border bg-surface px-3.5 py-2.5 text-base text-foreground placeholder:text-subtle sm:text-sm',
        'transition-colors focus-visible:border-ring focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring/25',
        'aria-[invalid=true]:border-danger',
        className,
      )}
      {...props}
    />
  )
}
