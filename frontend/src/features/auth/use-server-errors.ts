import type { FieldValues, Path, UseFormSetError } from 'react-hook-form'
import { ApiError, errorMessage } from '@/api/errors'

/**
 * Aplica erros de validação do backend nos campos; retorna a mensagem geral (se houver).
 * {@code aliases} mapeia o nome do campo no servidor para o do formulário quando diferem.
 */
export function applyServerErrors<T extends FieldValues>(
  error: unknown,
  setError: UseFormSetError<T>,
  fields: Path<T>[],
  aliases: Record<string, Path<T>> = {},
): string {
  if (error instanceof ApiError) {
    const fieldErrors = { ...error.fieldErrors }
    for (const [server, form] of Object.entries(aliases)) {
      if (fieldErrors[server] && !fieldErrors[form]) fieldErrors[form] = fieldErrors[server]
    }
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
