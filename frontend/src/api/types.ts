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

// ------------------------------------------------------------------ Fase 4: controle financeiro
export type EntryKind = Schemas['TransactionResponse']['kind']
export type TransactionStatus = Schemas['TransactionResponse']['status']
export type FinanceCategory = Schemas['FinanceCategoryResponse']
export type FinanceCategoryRequest = Schemas['FinanceCategoryRequest']
export type Transaction = Schemas['TransactionResponse']
export type TransactionRequest = Schemas['TransactionRequest']
export type InstallmentPlan = Schemas['InstallmentPlanResponse']
export type InstallmentPlanRequest = Schemas['InstallmentPlanRequest']
export type PaymentMethod = InstallmentPlan['paymentMethod']
export type RecurringEntry = Schemas['RecurringEntryResponse']
export type RecurringEntryRequest = Schemas['RecurringEntryRequest']
export type Frequency = RecurringEntry['frequency']
export type SavingGoal = Schemas['SavingGoalResponse']
export type MonthlySummary = Schemas['MonthlySummaryResponse']
export type FinanceOverview = Schemas['OverviewResponse']
export type MonthPoint = Schemas['MonthPoint']
export type Commitments = Schemas['CommitmentsResponse']
export type Projection = Schemas['ProjectionResponse']

// ------------------------------------------------------------------ Fase 5: universidades e candidaturas
export type University = Schemas['UniversityResponse']
export type UniversityRequest = Schemas['UniversityRequest']
export type UniversityDetail = Schemas['UniversityDetailResponse']
export type Program = Schemas['ProgramResponse']
export type ProgramRequest = Schemas['ProgramRequest']
export type LanguageRequirement = Schemas['LanguageRequirementDto']
export type IntakeRequest = Schemas['IntakeDto']
export type Intake = Schemas['IntakeResponse']
export type CompareResult = Schemas['CompareResponse']
export type Rating = NonNullable<Program['admissionChance']>
export type Employability = NonNullable<University['employability']>
export type Requirement = Program['gre']
export type Nature = NonNullable<University['nature']>
export type InstitutionType = NonNullable<University['institutionType']>
export type ApplicationChannel = NonNullable<Program['applicationChannel']>
export type Term = Intake['term']
export type WindowState = Intake['nextWindow']['state']
export type Application = Schemas['ApplicationResponse']
export type ApplicationStatus = Application['status']
export type ApplicationResult = Application['result']
export type ApplicationCreateRequest = Schemas['ApplicationCreateRequest']
export type ApplicationUpdateRequest = Schemas['ApplicationUpdateRequest']
export type StatusHistory = Schemas['StatusHistoryResponse']
export type UniversityRequestItem = Schemas['RequestResponse']
export type UniversityRequestRequest = Schemas['RequestRequest']
export type ApplicationsOverview = Dashboard['applications']
export type UniversityImage = Schemas['UniversityImageResponse']

// ------------------------------------------------------------------ ajustes: duas metas
export type ScopeGoal = Schemas['ScopeGoal']

// ------------------------------------------------------------------ Fase 6: tarefas e timeline
export type Task = Schemas['TaskResponse']
export type TaskRequest = Schemas['TaskRequest']
export type TaskStatus = Task['status']
export type Priority = Task['priority']
export type TimelineItem = Schemas['TimelineItem']
export type TimelineResponse = Schemas['TimelineResponse']
export type TimelineSource = TimelineItem['source']
export type TimelineKind = TimelineItem['kind']
export type TimelineItemStatus = TimelineItem['status']
export type EventRequest = Schemas['EventRequest']
export type EventType = NonNullable<TimelineItem['eventType']>

// ------------------------------------------------------------------ Fase 7: mala
export type LuggageBag = Schemas['BagResponse']
export type BagRequest = Schemas['BagRequest']
export type BagType = LuggageBag['type']
export type WeightState = LuggageBag['state']
export type LuggageItem = Schemas['ItemResponse']
export type ItemRequest = Schemas['ItemRequest']
export type ItemCategory = LuggageItem['category']
export type LuggageSummary = Schemas['LuggageSummary']

// ------------------------------------------------------------------ Fase 8: notificações
export type AppNotification = Schemas['NotificationResponse']
export type NotificationPage = Schemas['NotificationPage']
export type NotificationCategory = AppNotification['category']
export type NotificationSeverity = AppNotification['severity']
export type NotificationPreference = Schemas['PreferenceResponse']
export type NotificationPreferenceRequest = Schemas['PreferenceRequest']
export type ExchangeAlert = Schemas['AlertResponse']
export type ExchangeAlertRequest = Schemas['AlertRequest']

// ------------------------------------------------------------------ Fase 9: importação
export type ImportPreview = Schemas['ImportPreview']
export type SheetPreview = Schemas['SheetPreview']
export type ImportRow = Schemas['PreviewRow']
export type ImportRowStatus = ImportRow['status']
export type ImportSheet = SheetPreview['sheet']
export type CommitRequest = Schemas['CommitRequest']
export type CommitSummary = Schemas['CommitSummary']

// ------------------------------------------------------------------ Fase 10: busca global
export type SearchResponse = Schemas['SearchResponse']
export type SearchResult = Schemas['SearchResult']
export type SearchType = SearchResult['type']

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
