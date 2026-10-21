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

export type Money = Schemas['MoneyResponse']
export type ExchangeRate = Schemas['ExchangeRateResponse']
export type RatePoint = Schemas['RatePointResponse']
export type ExchangeRateOverrideRequest = Schemas['ExchangeRateOverrideRequest']

export type Objective = Schemas['ObjectiveResponse']
export type ObjectiveRequest = Schemas['ObjectiveRequest']
export type ObjectiveProgress = Schemas['ObjectiveProgressResponse']
export type TargetMode = Objective['targetMode']
export type ExpenseScope = Objective['expenseScope']
export type Adjustment = Schemas['AdjustmentResponse']
export type AdjustmentRequest = Schemas['AdjustmentRequest']

export type Account = Schemas['AccountResponse']
export type AccountRequest = Schemas['AccountRequest']
export type AccountType = Account['type']
export type AccountsSummary = Schemas['AccountsSummaryResponse']
export type BalanceRequest = Schemas['BalanceRequest']
export type BalanceSnapshot = Schemas['BalanceSnapshotResponse']

export type OnboardingRequest = Schemas['OnboardingRequest']
export type OnboardingResponse = Schemas['OnboardingResponse']
export type Dashboard = Schemas['DashboardResponse']

export type CatalogItem = Schemas['CatalogResponse']
export type CatalogRequest = Schemas['CatalogRequest']
export type Expense = Schemas['ExpenseResponse']
export type ExpenseRequest = Schemas['ExpenseRequest']
export type ExpenseDetail = Schemas['ExpenseDetailResponse']
export type ExpenseStatus = Expense['status']
export type Recurrence = Expense['recurrence']
export type Payment = Schemas['PaymentResponse']
export type PaymentRequest = Schemas['PaymentRequest']
export type PreviewTotalRequest = Schemas['PreviewTotalRequest']
export type PreviewTotal = Schemas['PreviewTotalResponse']
export type ExpenseSummary = Schemas['ExpenseSummaryResponse']
export type ExpenseGroupBy = ExpenseSummary['groupBy']
export type ExpenseGroup = Schemas['Group']

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
