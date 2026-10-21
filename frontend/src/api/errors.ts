import type { ProblemDetails } from './types'

export class ApiError extends Error {
  readonly status: number
  readonly problem: ProblemDetails

  constructor(problem: ProblemDetails) {
    super(problem.detail || problem.title || `Erro ${problem.status}`)
    this.name = 'ApiError'
    this.status = problem.status
    this.problem = problem
  }

  /** Erros de validação por campo, no formato { campo: mensagem }. */
  get fieldErrors(): Record<string, string> {
    return Object.fromEntries((this.problem.errors ?? []).map((e) => [e.field, e.message]))
  }
}

export class NetworkError extends Error {
  constructor() {
    super('Não foi possível conectar ao servidor. Verifique sua conexão e tente novamente.')
    this.name = 'NetworkError'
  }
}

export function errorMessage(error: unknown): string {
  if (error instanceof ApiError || error instanceof NetworkError) return error.message
  return 'Ocorreu um erro inesperado.'
}
