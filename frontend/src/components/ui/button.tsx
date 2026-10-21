import { cva, type VariantProps } from 'class-variance-authority'
import { Slot } from 'radix-ui'
import { Loader2 } from 'lucide-react'
import type { ComponentProps } from 'react'
import { cn } from '@/lib/utils'

export const buttonVariants = cva(
  'inline-flex items-center justify-center gap-2 whitespace-nowrap rounded-xl text-sm font-medium transition-colors disabled:pointer-events-none disabled:opacity-50 [&_svg]:size-4 [&_svg]:shrink-0 cursor-pointer',
  {
    variants: {
      variant: {
        primary: 'bg-primary text-primary-foreground hover:bg-primary-hover',
        secondary: 'bg-surface-2 text-foreground hover:bg-border',
        outline: 'border border-border bg-surface text-foreground hover:bg-surface-2',
        ghost: 'text-foreground hover:bg-surface-2',
        danger: 'bg-danger text-white hover:opacity-90',
        link: 'text-primary underline-offset-4 hover:underline px-0',
      },
      size: {
        sm: 'h-9 px-3',
        md: 'h-11 px-4', // 44px: alvo de toque mínimo
        lg: 'h-12 px-6 text-base',
        icon: 'size-11',
      },
    },
    defaultVariants: { variant: 'primary', size: 'md' },
  },
)

type ButtonProps = ComponentProps<'button'> &
  VariantProps<typeof buttonVariants> & { asChild?: boolean; loading?: boolean }

export function Button({ className, variant, size, asChild, loading, disabled, children, ...props }: ButtonProps) {
  const Comp = asChild ? Slot.Root : 'button'
  return (
    <Comp
      className={cn(buttonVariants({ variant, size }), className)}
      disabled={disabled || loading}
      aria-busy={loading || undefined}
      {...props}
    >
      {loading && <Loader2 className="animate-spin" aria-hidden />}
      {children}
    </Comp>
  )
}
