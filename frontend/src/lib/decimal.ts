/**
 * Converte o que a pessoa digita ("1.234,56", "1234,56", "1234.56", "75.000", "R$ 75") em número.
 * Retorna null para vazio e NaN para texto inválido. O backend revalida tudo.
 * Use {@code thousands: false} em cotações ("5.643" é 5,643, não 5.643).
 */
export function parseDecimal(
  input: string | null | undefined,
  opts: { thousands?: boolean } = {},
): number | null {
  if (input == null) return null
  let s = input.trim().replace(/[R$€\s]/g, '')
  if (s === '') return null
  if (s.includes(',')) {
    // padrão brasileiro: ponto = milhar, vírgula = decimal
    s = s.replace(/\./g, '').replace(',', '.')
  } else if (opts.thousands !== false && /^-?\d{1,3}(\.\d{3})+$/.test(s)) {
    // "75.000" / "1.500.000": pontos em grupos de 3 são milhar (pt-BR)
    s = s.replace(/\./g, '')
  }
  if (!/^-?\d+(\.\d+)?$/.test(s)) return Number.NaN
  return Number(s)
}

/** Número → texto editável no padrão brasileiro, sem separador de milhar ("1234,5"). */
export function decimalToInput(value: number | null | undefined): string {
  if (value == null) return ''
  return String(value).replace('.', ',')
}

export const isValidDecimal = (
  v: string,
  opts: {
    allowEmpty?: boolean
    min?: number
    allowNegative?: boolean
    thousands?: boolean
    maxFraction?: number
  } = {},
) => {
  const n = parseDecimal(v, { thousands: opts.thousands })
  if (n === null) return !!opts.allowEmpty
  if (Number.isNaN(n)) return false
  if (!opts.allowNegative && n < 0) return false
  if (opts.min !== undefined && n < opts.min) return false
  const factor = 10 ** (opts.maxFraction ?? 2)
  return Math.round(n * factor) / factor === n
}
