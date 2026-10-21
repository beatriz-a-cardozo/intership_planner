import { Switch as SwitchPrimitive } from 'radix-ui'
import type { ComponentProps } from 'react'
import { cn } from '@/lib/utils'

interface SwitchFieldProps extends Omit<ComponentProps<typeof SwitchPrimitive.Root>, 'children'> {
  id: string
  label: string
  hint?: string
}

/** Interruptor com rótulo clicável; a linha inteira tem 44px de altura (alvo de toque). */
export function SwitchField({ id, label, hint, className, ...props }: SwitchFieldProps) {
  return (
    <div className={cn('flex min-h-11 items-center justify-between gap-4', className)}>
      <label htmlFor={id} className="cursor-pointer">
        <span className="block text-sm font-medium">{label}</span>
        {hint && <span className="block text-xs text-muted">{hint}</span>}
      </label>
      <SwitchPrimitive.Root
        id={id}
        className="relative h-7 w-12 shrink-0 cursor-pointer rounded-full bg-border transition-colors data-[state=checked]:bg-primary focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-ring"
        {...props}
      >
        <SwitchPrimitive.Thumb className="block size-6 translate-x-0.5 rounded-full bg-surface shadow transition-transform data-[state=checked]:translate-x-[1.375rem]" />
      </SwitchPrimitive.Root>
    </div>
  )
}
