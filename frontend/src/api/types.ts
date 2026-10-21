/**
 * Contratos da API usados pelo frontend.
 * Os tipos são derivados de `schema.d.ts`, gerado a partir do OpenAPI do backend (`npm run gen:api`),
 * para que frontend e backend nunca divirjam.
 */
import type { components } from './schema'

type Schemas = components['schemas']

export type ThemePreference = Schemas['UserSettingsResponse']['theme']
export type AuthResponse = Schemas['AuthResponse']
export type CurrentUser = Schemas['MeResponse']
export type UserSettings = Schemas['UserSettingsResponse']
export type UpdateSettingsRequest = Schemas['UpdateSettingsRequest']
export type LoginRequest = Schemas['LoginRequest']
export type RegisterRequest = Schemas['RegisterRequest']
export type Currency = Schemas['CurrencyResponse']

/** RFC 9457 Problem Details, formato padrão de erro do backend. */
export interface ProblemDetails {
  type?: string
  title?: string
  status: number
  detail?: string
  instance?: string
  traceId?: string
  errors?: { field: string; message: string }[]
}
