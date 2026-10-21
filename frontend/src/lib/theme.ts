import type { ThemePreference } from '@/api/types'

export const THEME_STORAGE_KEY = 'ip.theme'

export function resolveTheme(preference: ThemePreference, systemPrefersDark: boolean): 'light' | 'dark' {
  if (preference === 'SYSTEM') return systemPrefersDark ? 'dark' : 'light'
  return preference === 'DARK' ? 'dark' : 'light'
}

export function readStoredTheme(): ThemePreference {
  try {
    const value = localStorage.getItem(THEME_STORAGE_KEY)
    return value === 'LIGHT' || value === 'DARK' || value === 'SYSTEM' ? value : 'SYSTEM'
  } catch {
    return 'SYSTEM'
  }
}

export function storeTheme(preference: ThemePreference) {
  try {
    localStorage.setItem(THEME_STORAGE_KEY, preference)
  } catch {
    /* armazenamento indisponível (modo privado): apenas não persiste localmente */
  }
}
