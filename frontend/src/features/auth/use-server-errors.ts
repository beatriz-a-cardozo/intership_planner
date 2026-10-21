import type { FieldValues, Path, UseFormSetError } from 'react-hook-form'
import { ApiError, errorMessage } from '@/api/errors'

/** Aplica erros de validação do backend nos campos; retorna a mensagem geral (se houver). */
export function applyServerErrors<T extends FieldValues>(error: unknown, setError: UseFormSetError<T>, fields: Path<T>[]): string {
  if (error instanceof ApiError) {
    const fieldErrors = error.fieldErrors
    let matched = false
    for (const field of fields) {
      const message = fieldErrors[field]
      if (message) {
        setError(field, { type: 'server', message })
        matched = true
      }
    }
    if (matched) return ''
  }
  return errorMessage(error)
}
