import { describe, expect, it } from 'vitest'
import { resolveTheme } from './theme'

describe('resolveTheme', () => {
  it('respeita a escolha explícita do usuário', () => {
    expect(resolveTheme('DARK', false)).toBe('dark')
    expect(resolveTheme('LIGHT', true)).toBe('light')
  })

  it('segue o sistema quando a preferência é SYSTEM', () => {
    expect(resolveTheme('SYSTEM', true)).toBe('dark')
    expect(resolveTheme('SYSTEM', false)).toBe('light')
  })
})
