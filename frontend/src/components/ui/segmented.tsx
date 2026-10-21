import { ToggleGroup } from 'radix-ui'
import type { ReactNode } from 'react'
import { cn } from '@/lib/utils'

interface SegmentedOption<T extends string> {
  value: T
  label: string
  icon?: ReactNode
}

interface SegmentedControlProps<T extends string> {
  value: T
  onValueChange: (value: T) => void
  options: SegmentedOption<T>[]
  ariaLabel: string
  className?: string
}

export function SegmentedControl<T extends string>({ value, onValueChange, options, ariaLabel, className }: SegmentedControlProps<T>) {
  return (
    <ToggleGroup.Root
      type="single"
      value={value}
      onValueChange={(v) => v && onValueChange(v as T)}
      aria-label={ariaLabel}
      className={cn('inline-flex w-full rounded-xl bg-surface-2 p-1 sm:w-auto', className)}
    >
      {options.map((o) => (
        <ToggleGroup.Item
          key={o.value}
          value={o.value}
          className="flex h-10 flex-1 items-center justify-center gap-2 rounded-lg px-3 text-sm text-muted transition-colors data-[state=on]:bg-surface data-[state=on]:text-foreground data-[state=on]:shadow-card sm:flex-none [&_svg]:size-4"
        >
          {o.icon}
          {o.label}
        </ToggleGroup.Item>
      ))}
    </ToggleGroup.Root>
  )
}
