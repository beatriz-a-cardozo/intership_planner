import { APP_NAME } from '@/lib/config'
import { cn } from '@/lib/utils'

export function BrandMark({ className }: { className?: string }) {
  return (
    <svg viewBox="0 0 32 32" className={cn('size-8', className)} aria-hidden>
      <rect width="32" height="32" rx="9" className="fill-primary" />
      <path d="M8 21.5c3.2-6.4 6.6-9.6 10.2-9.6 2.4 0 4 1.3 5.8 3.6" fill="none" className="stroke-primary-foreground" strokeWidth="2.4" strokeLinecap="round" />
      <circle cx="23.6" cy="15.6" r="2.6" className="fill-accent" />
    </svg>
  )
}

export function Brand({ className }: { className?: string }) {
  return (
    <span className={cn('flex items-center gap-2.5', className)}>
      <BrandMark />
      <span className="font-display text-lg font-semibold tracking-tight">{APP_NAME}</span>
    </span>
  )
}
