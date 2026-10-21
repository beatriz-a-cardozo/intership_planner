import { cva, type VariantProps } from 'class-variance-authority'
import type { ComponentProps } from 'react'
import { cn } from '@/lib/utils'

const badgeVariants = cva('inline-flex items-center gap-1 rounded-full px-2 py-0.5 text-[11px] font-medium', {
  variants: {
    tone: {
      neutral: 'bg-surface-2 text-muted',
      primary: 'bg-primary-soft text-primary',
      accent: 'bg-accent-soft text-accent',
      danger: 'bg-danger-soft text-danger',
    },
  },
  defaultVariants: { tone: 'neutral' },
})

export function Badge({ className, tone, ...props }: ComponentProps<'span'> & VariantProps<typeof badgeVariants>) {
  return <span className={cn(badgeVariants({ tone }), className)} {...props} />
}
