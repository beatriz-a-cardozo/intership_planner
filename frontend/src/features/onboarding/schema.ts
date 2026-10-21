import { z } from 'zod'
import type { OnboardingRequest } from '@/api/types'
import { isValidDecimal, parseDecimal } from '@/lib/decimal'
import { todayIso } from '@/lib/format'

const optionalMoney = z
  .string()
  .refine((v) => isValidDecimal(v, { allowEmpty: true }), 'Informe um valor válido (ex.: 1.500,00).')

export const onboardingSchema = z
  .object({
    objectiveName: z
      .string()
      .trim()
      .min(2, 'Dê um nome ao seu objetivo.')
      .max(120, 'Máximo de 120 caracteres.'),
    countryCode: z.string().length(2, 'Escolha o país.'),
    city: z.string().max(120, 'Máximo de 120 caracteres.'),
    targetDate: z
      .string()
      .min(1, 'Informe a data alvo.')
      .refine((v) => v > todayIso(), 'A data alvo precisa estar no futuro.'),
    currentSavings: optionalMoney,
    currentSavingsCurrency: z.enum(['BRL', 'EUR']),
    plannedMonthlySaving: optionalMoney,
    currency: z.enum(['BRL', 'EUR']),
    targetMode: z.enum(['DERIVED', 'MANUAL']),
    targetAmount: optionalMoney,
  })
  .refine((v) => v.targetMode === 'DERIVED' || (parseDecimal(v.targetAmount) ?? 0) > 0, {
    path: ['targetAmount'],
    message: 'Informe o valor da meta.',
  })

export type OnboardingForm = z.infer<typeof onboardingSchema>

/** Campos validados em cada passo (o último também valida a regra da meta). */
export const STEP_FIELDS: (keyof OnboardingForm)[][] = [
  ['objectiveName'],
  ['countryCode', 'city'],
  ['targetDate'],
  ['currentSavings', 'currentSavingsCurrency'],
  ['plannedMonthlySaving'],
  ['currency', 'targetMode', 'targetAmount'],
]

export const onboardingDefaults: OnboardingForm = {
  objectiveName: '',
  countryCode: 'DE',
  city: '',
  targetDate: '',
  currentSavings: '',
  currentSavingsCurrency: 'BRL',
  plannedMonthlySaving: '',
  currency: 'BRL',
  targetMode: 'DERIVED',
  targetAmount: '',
}

export function toOnboardingRequest(v: OnboardingForm): OnboardingRequest {
  return {
    objectiveName: v.objectiveName.trim(),
    countryCode: v.countryCode,
    city: v.city.trim() || undefined,
    targetDate: v.targetDate,
    currency: v.currency,
    targetAmount: v.targetMode === 'MANUAL' ? (parseDecimal(v.targetAmount) ?? undefined) : undefined,
    currentSavings: parseDecimal(v.currentSavings) ?? undefined,
    currentSavingsCurrency: v.currentSavingsCurrency,
    plannedMonthlySaving: parseDecimal(v.plannedMonthlySaving) ?? undefined,
  }
}
