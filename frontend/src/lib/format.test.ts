import { describe, expect, it } from 'vitest'
import { formatDate, formatMoney, formatPercent, formatRate } from './format'

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
})
