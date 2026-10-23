import { describe, expect, it } from 'vitest'
import { formatDate, formatMoney, formatMonth, formatPercent, formatRate, shiftMonth } from './format'

// Intl usa espaço não separável entre símbolo e valor
const NBSP = String.fromCharCode(160)
const normalize = (s: string) => s.split(NBSP).join(' ')

describe('format', () => {
  it('formata dinheiro em BRL e EUR no padrão brasileiro', () => {
    expect(normalize(formatMoney({ amount: 1234.56, currency: 'BRL' }))).toBe('R$ 1.234,56')
    expect(normalize(formatMoney({ amount: 855.58, currency: 'EUR' }))).toBe('€ 855,58')
    expect(formatMoney(null)).toBe('—')
  })

  it('formata datas ISO sem deslocar o dia pelo fuso', () => {
    expect(formatDate('2027-05-15')).toBe('15/05/2027')
    expect(formatDate(null)).toBe('—')
  })

  it('formata percentuais e cotações', () => {
    expect(formatPercent(25.4, 1)).toBe('25,4%')
    expect(formatRate(5.6432)).toBe('5,6432')
    expect(formatRate(6)).toBe('6,00')
  })

  it('navega entre meses atravessando o ano', () => {
    expect(shiftMonth('2026-11', 2)).toBe('2027-01')
    expect(shiftMonth('2027-01', -1)).toBe('2026-12')
    expect(shiftMonth('2026-05', 0)).toBe('2026-05')
    expect(formatMonth('2026-11')).toBe('novembro de 2026')
    expect(formatMonth(null)).toBe('—')
  })
})
