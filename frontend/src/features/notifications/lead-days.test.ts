import { describe, expect, it } from 'vitest'
import { parseLeadDays } from './lead-days'

describe('antecedências das notificações', () => {
  it('aceita números separados por vírgula ou espaço, sem repetir e do maior para o menor', () => {
    expect(parseLeadDays('30, 7, 1')).toEqual([30, 7, 1])
    expect(parseLeadDays('1 7 30 7')).toEqual([30, 7, 1])
    expect(parseLeadDays('0')).toEqual([0])
  })

  it('recusa vazio, texto, mais de 5 ou acima de 365', () => {
    expect(parseLeadDays('')).toBeNull()
    expect(parseLeadDays('uma semana')).toBeNull()
    expect(parseLeadDays('1,2,3,4,5,6')).toBeNull()
    expect(parseLeadDays('400')).toBeNull()
  })
})
