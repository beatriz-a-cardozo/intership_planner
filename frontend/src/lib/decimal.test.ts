import { describe, expect, it } from 'vitest'
import { decimalToInput, isValidDecimal, parseDecimal } from './decimal'

describe('parseDecimal', () => {
  it('aceita formatos brasileiro e internacional', () => {
    expect(parseDecimal('1.234,56')).toBe(1234.56)
    expect(parseDecimal('1234,5')).toBe(1234.5)
    expect(parseDecimal('1234.56')).toBe(1234.56)
    expect(parseDecimal('R$ 75')).toBe(75)
    expect(parseDecimal('-75.000')).toBe(-75000)
    expect(parseDecimal('6.15')).toBe(6.15)
    expect(parseDecimal('5.643', { thousands: false })).toBe(5.643)
    expect(parseDecimal('-75.000,00')).toBe(-75000)
  })

  it('distingue vazio de inválido', () => {
    expect(parseDecimal('')).toBeNull()
    expect(parseDecimal('abc')).toBeNaN()
  })

  it('valida casas decimais e sinal', () => {
    expect(isValidDecimal('10,555')).toBe(false)
    expect(isValidDecimal('-1')).toBe(false)
    expect(isValidDecimal('-1', { allowNegative: true })).toBe(true)
    expect(isValidDecimal('', { allowEmpty: true })).toBe(true)
    expect(decimalToInput(855.58)).toBe('855,58')
  })
})
