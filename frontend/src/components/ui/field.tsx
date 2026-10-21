import type { ReactNode } from 'react'
import { cn } from '@/lib/utils'
import { Label } from './label'

interface FieldProps {
  id: string
  label: string
  error?: string
  hint?: string
  className?: string
  children: ReactNode
}

/** Agrupa label + controle + dica + erro. O controle deve usar `aria-describedby={describedBy(id, …)}`. */
export function Field({ id, label, error, hint, className, children }: FieldProps) {
  return (
    <div className={cn('grid gap-1.5', className)}>
      <Label htmlFor={id}>{label}</Label>
      {children}
      {hint && !error && (
        <p id={`${id}-hint`} className="text-xs text-muted">
          {hint}
        </p>
      )}
      {error && (
        <p id={`${id}-error`} role="alert" className="text-xs font-medium text-danger">
          {error}
        </p>
      )}
    </div>
  )
}

export function describedBy(id: string, error?: string, hint?: string) {
  return error ? `${id}-error` : hint ? `${id}-hint` : undefined
}
