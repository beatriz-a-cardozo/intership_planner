import { isValidDecimal, parseDecimal } from '@/lib/decimal'

export type WeightUnit = 'g' | 'kg'

/** Valida o peso digitado (vazio permitido) no formato pt-BR. */
export function isValidWeight(text: string) {
  return isValidDecimal(text, { allowEmpty: true, thousands: false, maxFraction: 3 })
}

/** Texto digitado → gramas inteiros (a API guarda gramas). Vazio → undefined. "5,8" kg → 5800. */
export function toGrams(text: string, unit: WeightUnit): number | undefined {
  const value = parseDecimal(text, { thousands: false })
  if (value == null) return undefined
  return Math.round(unit === 'kg' ? value * 1000 : value)
}

/** Gramas → texto para editar na unidade escolhida. 5800 em kg → "5,8". */
export function fromGrams(grams: number | null | undefined, unit: WeightUnit): string {
  if (grams == null) return ''
  const value = unit === 'kg' ? grams / 1000 : grams
  return String(value).replace('.', ',')
}

/** Unidade mais natural para exibir um valor já salvo. */
export function naturalUnit(grams: number | null | undefined, fallback: WeightUnit): WeightUnit {
  if (grams == null) return fallback
  return grams >= 1000 && grams % 100 === 0 ? 'kg' : 'g'
}
