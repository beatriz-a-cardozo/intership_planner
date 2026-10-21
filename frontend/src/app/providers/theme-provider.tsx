import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import type { ThemePreference } from '@/api/types'
import { readStoredTheme, resolveTheme, storeTheme } from '@/lib/theme'

interface ThemeContextValue {
  preference: ThemePreference
  resolved: 'light' | 'dark'
  setPreference: (preference: ThemePreference) => void
}

const ThemeContext = createContext<ThemeContextValue | null>(null)

const darkQuery = () => window.matchMedia?.('(prefers-color-scheme: dark)')

export function ThemeProvider({ children }: { children: ReactNode }) {
  const [preference, setPreferenceState] = useState<ThemePreference>(readStoredTheme)
  const [systemDark, setSystemDark] = useState(() => darkQuery()?.matches ?? false)

  useEffect(() => {
    const mq = darkQuery()
    if (!mq) return
    const listener = (e: MediaQueryListEvent) => setSystemDark(e.matches)
    mq.addEventListener('change', listener)
    return () => mq.removeEventListener('change', listener)
  }, [])

  const resolved = resolveTheme(preference, systemDark)

  useEffect(() => {
    document.documentElement.classList.toggle('dark', resolved === 'dark')
  }, [resolved])

  const value = useMemo<ThemeContextValue>(
    () => ({
      preference,
      resolved,
      setPreference: (p) => {
        storeTheme(p)
        setPreferenceState(p)
      },
    }),
    [preference, resolved],
  )

  return <ThemeContext.Provider value={value}>{children}</ThemeContext.Provider>
}

export function useTheme() {
  const ctx = useContext(ThemeContext)
  if (!ctx) throw new Error('useTheme deve ser usado dentro de ThemeProvider')
  return ctx
}
