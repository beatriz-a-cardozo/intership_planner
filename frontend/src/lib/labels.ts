import type { AccountType, ExpenseScope, ExpenseStatus, Recurrence, TargetMode } from '@/api/types'
import type {
  Application,
  ApplicationChannel,
  ApplicationResult,
  ApplicationStatus,
  Employability,
  Frequency,
  InstitutionType,
  Nature,
  PaymentMethod,
  Rating,
  Requirement,
  Term,
  WindowState,
} from '@/api/types'
import type {
  BagType,
  EventType,
  ImportRowStatus,
  ItemCategory,
  NotificationCategory,
  Priority,
  TaskStatus,
  TimelineSource,
  WeightState,
} from '@/api/types'

export const RECURRENCE_LABELS: Record<Recurrence, string> = {
  ONE_TIME: 'Único',
  MONTHLY: 'Mensal',
  SEMIANNUAL: 'Semestral',
  ANNUAL: 'Anual',
  PER_UNIT: 'Por unidade',
  CUSTOM: 'Personalizado',
}

export const RECURRENCE_HINTS: Record<Recurrence, string> = {
  ONE_TIME: 'Pago uma vez.',
  MONTHLY: 'Repete todo mês durante o objetivo.',
  SEMIANNUAL: 'Repete a cada semestre (ex.: taxa da universidade).',
  ANNUAL: 'Repete uma vez por ano.',
  PER_UNIT: 'Preço por unidade × quantidade (ex.: 5 inscrições).',
  CUSTOM: 'Você define quantas vezes ocorre.',
}

export const STATUS_LABELS: Record<ExpenseStatus, string> = {
  PENDING: 'Pendente',
  PARTIALLY_PAID: 'Parcial',
  PAID: 'Pago',
  CANCELLED: 'Cancelado',
}

export const STATUS_TONES: Record<ExpenseStatus, 'neutral' | 'primary' | 'accent' | 'danger'> = {
  PENDING: 'neutral',
  PARTIALLY_PAID: 'accent',
  PAID: 'primary',
  CANCELLED: 'neutral',
}

export const ACCOUNT_TYPE_LABELS: Record<AccountType, string> = {
  CHECKING: 'Conta corrente',
  SAVINGS: 'Poupança',
  INVESTMENT: 'Investimento',
  CASH: 'Espécie',
  BLOCKED: 'Conta bloqueada (Sperrkonto)',
  OTHER: 'Outro',
}

export const TARGET_MODE_LABELS: Record<TargetMode, string> = {
  DERIVED: 'Calculada pelos gastos',
  MANUAL: 'Valor fixo',
}

export const EXPENSE_SCOPE_LABELS: Record<ExpenseScope, string> = {
  REQUIRED_ONLY: 'Só gastos obrigatórios',
  ALL: 'Todos os gastos',
}

/** Países mais comuns para intercâmbio; o backend aceita qualquer código ISO de 2 letras. */
export const COUNTRIES: { code: string; name: string }[] = [
  { code: 'DE', name: 'Alemanha' },
  { code: 'AT', name: 'Áustria' },
  { code: 'BE', name: 'Bélgica' },
  { code: 'CA', name: 'Canadá' },
  { code: 'ES', name: 'Espanha' },
  { code: 'US', name: 'Estados Unidos' },
  { code: 'FI', name: 'Finlândia' },
  { code: 'FR', name: 'França' },
  { code: 'NL', name: 'Holanda' },
  { code: 'IE', name: 'Irlanda' },
  { code: 'IT', name: 'Itália' },
  { code: 'NO', name: 'Noruega' },
  { code: 'PT', name: 'Portugal' },
  { code: 'GB', name: 'Reino Unido' },
  { code: 'SE', name: 'Suécia' },
  { code: 'CH', name: 'Suíça' },
]

export function countryName(code: string | null | undefined) {
  if (!code) return null
  return COUNTRIES.find((c) => c.code === code)?.name ?? code
}

export function rateSourceLabel(source: string) {
  switch (source) {
    case 'FRANKFURTER':
      return 'BCE (Frankfurter)'
    case 'AWESOMEAPI':
      return 'AwesomeAPI'
    case 'MANUAL':
      return 'Definida por você'
    case 'FALLBACK':
      return 'Reserva (sem conexão com provedores)'
    default:
      return source
  }
}

// ------------------------------------------------------------------ Fase 4

export const FREQUENCY_LABELS: Record<Frequency, string> = {
  WEEKLY: 'Semanal',
  MONTHLY: 'Mensal',
  YEARLY: 'Anual',
  EVERY_N_MONTHS: 'A cada N meses',
}

export const PAYMENT_METHOD_LABELS: Record<PaymentMethod, string> = {
  CREDIT_CARD: 'Cartão de crédito',
  BOLETO: 'Boleto',
  PIX: 'Pix',
  DEBIT: 'Débito',
  BANK_TRANSFER: 'Transferência',
  OTHER: 'Outro',
}

// ------------------------------------------------------------------ Fase 5
export const RATING_LABELS: Record<Rating, string> = {
  LOW: 'Baixa',
  LOW_MEDIUM: 'Baixa-média',
  MEDIUM: 'Média',
  MEDIUM_HIGH: 'Média-alta',
  HIGH: 'Alta',
}

export const EMPLOYABILITY_LABELS: Record<Employability, string> = {
  ...RATING_LABELS,
  EXCELLENT: 'Excelente',
}

export const REQUIREMENT_LABELS: Record<Requirement, string> = {
  REQUIRED: 'Exige',
  NOT_REQUIRED: 'Não exige',
  CONDITIONAL: 'Se necessário',
  UNKNOWN: '—',
}

export const NATURE_LABELS: Record<Nature, string> = {
  UNIVERSITY: 'Universität',
  TU: 'Technische Universität',
  HOCHSCHULE: 'Hochschule',
  OTHER: 'Outra',
}

export const INSTITUTION_TYPE_LABELS: Record<InstitutionType, string> = {
  PUBLIC: 'Pública',
  PRIVATE: 'Privada',
}

export const CHANNEL_LABELS: Record<ApplicationChannel, string> = {
  DIRECT: 'Direta',
  UNI_ASSIST: 'uni-assist',
  VPD: 'VPD',
  PORTAL: 'Portal da universidade',
}

export const TERM_LABELS: Record<Term, string> = {
  WINTER: 'Inverno (Wintersemester)',
  SUMMER: 'Verão (Sommersemester)',
  OTHER: 'Outro',
}

export const TERM_SHORT: Record<Term, string> = { WINTER: 'WiSe', SUMMER: 'SoSe', OTHER: 'Outro' }

export const WINDOW_STATE_LABELS: Record<WindowState, string> = {
  NOT_OPEN_YET: 'Ainda não abriu',
  OPEN: 'Inscrições abertas',
  CLOSED: 'Encerrada',
}

/** Na ordem do processo (o backend usa a mesma ordem). */
export const APPLICATION_STATUSES: ApplicationStatus[] = [
  'AWAITING_OPENING',
  'DOCUMENTS_UNDER_REVIEW',
  'EXAM_REQUESTED',
  'INTERVIEW_REQUESTED',
  'ITEM_REQUESTED',
  'WAITLISTED',
  'AWAITING_RESULT',
  'ACCEPTED',
  'REJECTED',
  'ENROLLED',
]

export const APPLICATION_STATUS_LABELS: Record<ApplicationStatus, string> = {
  AWAITING_OPENING: 'Aguardando abertura das inscrições',
  DOCUMENTS_UNDER_REVIEW: 'Documentos em análise',
  EXAM_REQUESTED: 'Solicitado exame',
  INTERVIEW_REQUESTED: 'Solicitado entrevista',
  ITEM_REQUESTED: 'Solicitado item',
  WAITLISTED: 'Lista de espera',
  AWAITING_RESULT: 'Aguardando resultado',
  ACCEPTED: 'Aprovado',
  REJECTED: 'Não aceito',
  ENROLLED: 'Matriculada',
}

export const RESULT_TONES: Record<ApplicationResult, 'neutral' | 'primary' | 'accent' | 'danger'> = {
  PENDING: 'neutral',
  ACCEPTED: 'primary',
  WAITLISTED: 'accent',
  REJECTED: 'danger',
}

export const CONFIDENCE_LABELS: Record<NonNullable<Application['responseConfidence']>, string> = {
  LOW: 'Baixa',
  MEDIUM: 'Média',
  HIGH: 'Alta',
}

export const REQUEST_TYPE_LABELS: Record<string, string> = {
  DOCUMENT: 'Documento',
  EXAM: 'Exame',
  INTERVIEW: 'Entrevista',
  LETTER: 'Carta de motivação',
  RECOMMENDATION: 'Recomendação',
  TEST: 'Teste',
  PAYMENT: 'Pagamento',
  OTHER: 'Outro',
}

export const MONTH_NAMES = [
  'Janeiro',
  'Fevereiro',
  'Março',
  'Abril',
  'Maio',
  'Junho',
  'Julho',
  'Agosto',
  'Setembro',
  'Outubro',
  'Novembro',
  'Dezembro',
]

// ------------------------------------------------------------------ Fase 6 e 7

export const PRIORITY_LABELS: Record<Priority, string> = {
  LOW: 'Baixa',
  MEDIUM: 'Média',
  HIGH: 'Alta',
  URGENT: 'Urgente',
}

export const PRIORITY_TONES: Record<Priority, 'neutral' | 'primary' | 'accent' | 'danger'> = {
  LOW: 'neutral',
  MEDIUM: 'neutral',
  HIGH: 'accent',
  URGENT: 'danger',
}

export const TASK_STATUS_LABELS: Record<TaskStatus, string> = {
  TODO: 'A fazer',
  IN_PROGRESS: 'Em andamento',
  DONE: 'Concluída',
}

export const EVENT_TYPE_LABELS: Record<EventType, string> = {
  MILESTONE: 'Marco',
  TRAVEL: 'Viagem',
  EXAM: 'Prova',
  DOCUMENT: 'Documento',
  APPOINTMENT: 'Compromisso',
  PAYMENT: 'Pagamento',
  OTHER: 'Outro',
}

export const TIMELINE_SOURCE_LABELS: Record<TimelineSource, string> = {
  EVENT: 'Eventos',
  EXPENSE: 'Gastos',
  APPLICATION: 'Candidaturas',
  REQUEST: 'Solicitações',
  TASK: 'Tarefas',
}

export const BAG_TYPE_LABELS: Record<BagType, string> = {
  CARRY_ON: 'Mala de mão',
  CHECKED: 'Despachada',
  PERSONAL_ITEM: 'Item pessoal',
  OTHER: 'Outra',
}

export const ITEM_CATEGORY_LABELS: Record<ItemCategory, string> = {
  DOCUMENTS: 'Documentos',
  CLOTHING: 'Roupas',
  ELECTRONICS: 'Eletrônicos',
  HYGIENE: 'Higiene',
  HEALTH: 'Saúde',
  ACCESSORIES: 'Acessórios',
  FOOD: 'Comida',
  OTHER: 'Outros',
}

export const WEIGHT_STATE_LABELS: Record<WeightState, string> = {
  NO_LIMIT: 'Sem limite',
  OK: 'Dentro do limite',
  WARNING: 'Quase no limite',
  OVER: 'Acima do limite',
}

// ------------------------------------------------------------------ Fase 8 e 9

export const NOTIFICATION_CATEGORY_LABELS: Record<NotificationCategory, string> = {
  DEADLINES: 'Prazos de inscrição e solicitações',
  PAYMENTS: 'Vencimentos de gastos e parcelas',
  RESPONSES: 'Respostas esperadas',
  TASKS: 'Tarefas',
  EXCHANGE: 'Alertas de câmbio',
  MOTIVATIONAL: 'Marcos do objetivo',
}

export const IMPORT_STATUS_LABELS: Record<ImportRowStatus, string> = {
  VALID: 'Ok',
  WARNING: 'Com aviso',
  ERROR: 'Erro',
  DUPLICATE: 'Já existe',
}

export const IMPORT_STATUS_TONES: Record<ImportRowStatus, 'neutral' | 'primary' | 'accent' | 'danger'> = {
  VALID: 'primary',
  WARNING: 'accent',
  ERROR: 'danger',
  DUPLICATE: 'neutral',
}
