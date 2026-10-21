import { z } from 'zod'
import type { Expense, ExpenseRequest, PreviewTotalRequest, Recurrence } from '@/api/types'
import { decimalToInput, isValidDecimal, parseDecimal } from '@/lib/decimal'

export const SINGLE_OCCURRENCE: Recurrence[] = ['ONE_TIME', 'PER_UNIT']

const intOrEmpty = (min: number, max: number) =>
  z
    .string()
    .refine(
      (v) => v.trim() === '' || (/^\d+$/.test(v.trim()) && +v >= min && +v <= max),
      `Use um número entre ${min} e ${max}.`,
    )

export const expenseSchema = z
  .object({
    name: z.string().trim().min(1, 'Informe o nome do gasto.').max(160),
    categoryId: z.string().min(1, 'Escolha a categoria.'),
    cycleId: z.string(),
    currency: z.enum(['BRL', 'EUR']),
    unitPrice: z.string().refine((v) => isValidDecimal(v), 'Informe um valor válido (ex.: 1.250,00).'),
    quantity: intOrEmpty(1, 10000),
    recurrence: z.enum(['ONE_TIME', 'MONTHLY', 'SEMIANNUAL', 'ANNUAL', 'PER_UNIT', 'CUSTOM']),
    occurrences: intOrEmpty(1, 1200),
    required: z.boolean(),
    refundable: z.boolean(),
    dueDate: z.string(),
    endDate: z.string(),
    notes: z.string().max(2000),
  })
  .refine((v) => v.recurrence !== 'CUSTOM' || v.occurrences.trim() !== '', {
    path: ['occurrences'],
    message: 'Informe quantas vezes o gasto ocorre.',
  })
  .refine((v) => !v.dueDate || !v.endDate || v.endDate >= v.dueDate, {
    path: ['endDate'],
    message: 'A data final deve ser depois da inicial.',
  })

export type ExpenseForm = z.infer<typeof expenseSchema>

export function toExpenseForm(
  e: Expense | undefined,
  defaults: { categoryId?: string; currency: string },
): ExpenseForm {
  return {
    name: e?.name ?? '',
    categoryId: e?.category.id ?? defaults.categoryId ?? '',
    cycleId: e?.cycle?.id ?? '',
    currency: (e?.currency ?? defaults.currency) as 'BRL' | 'EUR',
    unitPrice: decimalToInput(e?.unitPrice),
    quantity: e ? String(e.quantity) : '',
    recurrence: e?.recurrence ?? 'ONE_TIME',
    occurrences: e && !SINGLE_OCCURRENCE.includes(e.recurrence) ? String(e.occurrences) : '',
    required: e?.required ?? true,
    refundable: e?.refundable ?? false,
    dueDate: e?.dueDate ?? '',
    endDate: e?.endDate ?? '',
    notes: e?.notes ?? '',
  }
}

const intOrUndefined = (v: string) => (v.trim() === '' ? undefined : Number(v))

export function toExpenseRequest(v: ExpenseForm, objectiveId: string): ExpenseRequest {
  const single = SINGLE_OCCURRENCE.includes(v.recurrence)
  return {
    objectiveId,
    categoryId: v.categoryId,
    cycleId: v.cycleId || undefined,
    name: v.name.trim(),
    currency: v.currency,
    unitPrice: parseDecimal(v.unitPrice) ?? 0,
    quantity: intOrUndefined(v.quantity),
    recurrence: v.recurrence,
    // vazio = o backend sugere pelo horizonte do objetivo
    occurrences: single ? undefined : intOrUndefined(v.occurrences),
    required: v.required,
    refundable: v.refundable,
    dueDate: v.dueDate || undefined,
    endDate: !single && v.endDate ? v.endDate : undefined,
    notes: v.notes.trim() || undefined,
  }
}

/** Preview só quando os campos do cálculo estão válidos (evita chamadas com dados incompletos). */
export function toPreviewRequest(
  v: Pick<ExpenseForm, 'currency' | 'unitPrice' | 'quantity' | 'recurrence' | 'occurrences'>,
  objectiveId: string,
): PreviewTotalRequest | null {
  if (!isValidDecimal(v.unitPrice)) return null
  const quantity = intOrUndefined(v.quantity)
  const occurrences = SINGLE_OCCURRENCE.includes(v.recurrence) ? undefined : intOrUndefined(v.occurrences)
  if (
    (quantity !== undefined && (Number.isNaN(quantity) || quantity < 1)) ||
    (occurrences !== undefined && (Number.isNaN(occurrences) || occurrences < 1))
  ) {
    return null
  }
  if (v.recurrence === 'CUSTOM' && occurrences === undefined) return null
  return {
    objectiveId,
    currency: v.currency,
    unitPrice: parseDecimal(v.unitPrice) ?? 0,
    quantity,
    recurrence: v.recurrence,
    occurrences,
  }
}
