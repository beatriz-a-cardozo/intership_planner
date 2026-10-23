import { describe, expect, it } from 'vitest'
import { formatWeight } from '@/lib/format'
import { fromGrams, isValidWeight, naturalUnit, toGrams } from './weight-input'

describe('peso da mala', () => {
  it('converte o digitado em gramas inteiros', () => {
    expect(toGrams('5,8', 'kg')).toBe(5800)
    expect(toGrams('23', 'kg')).toBe(23000)
    expect(toGrams('450', 'g')).toBe(450)
    expect(toGrams('0,125', 'kg')).toBe(125)
    expect(toGrams('', 'kg')).toBeUndefined()
  })

  it('volta para texto na unidade escolhida e escolhe a unidade natural', () => {
    expect(fromGrams(5800, 'kg')).toBe('5,8')
    expect(fromGrams(450, 'g')).toBe('450')
    expect(fromGrams(null, 'kg')).toBe('')
    expect(naturalUnit(23000, 'g')).toBe('kg')
    expect(naturalUnit(450, 'kg')).toBe('g')
    expect(naturalUnit(1250, 'kg')).toBe('g')
    expect(naturalUnit(undefined, 'kg')).toBe('kg')
  })

  it('valida e formata', () => {
    expect(isValidWeight('5,8')).toBe(true)
    expect(isValidWeight('')).toBe(true)
    expect(isValidWeight('abc')).toBe(false)
    expect(formatWeight(450)).toBe('450 g')
    expect(formatWeight(5800)).toBe('5,8 kg')
    expect(formatWeight(23000)).toBe('23,0 kg')
  })
})
