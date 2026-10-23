import { useQuery } from '@tanstack/react-query'
import {
  Building2,
  CalendarDays,
  CreditCard,
  GraduationCap,
  ListChecks,
  Luggage,
  Receipt,
  Search,
  Target,
  type LucideIcon,
} from 'lucide-react'
import { useId, useState } from 'react'
import { useNavigate } from 'react-router'
import { api } from '@/api/client'
import type { SearchResponse, SearchResult, SearchType } from '@/api/types'
import { Input } from '@/components/ui/input'
import { ResponsiveDialog } from '@/components/ui/responsive-dialog'
import { useDebouncedValue } from '@/hooks/use-debounced-value'
import { cn } from '@/lib/utils'

const TYPES: Record<SearchType, { label: string; icon: LucideIcon }> = {
  OBJECTIVE: { label: 'Objetivo', icon: Target },
  EXPENSE: { label: 'Gasto', icon: Receipt },
  UNIVERSITY: { label: 'Universidade', icon: Building2 },
  PROGRAM: { label: 'Programa', icon: GraduationCap },
  TASK: { label: 'Tarefa', icon: ListChecks },
  EVENT: { label: 'Evento', icon: CalendarDays },
  LUGGAGE: { label: 'Mala', icon: Luggage },
  INSTALLMENT: { label: 'Parcelamento', icon: CreditCard },
}

function ResultIcon({ type }: { type: SearchType }) {
  const Icon = TYPES[type].icon
  return <Icon className="size-4" aria-hidden />
}

/** Busca global (Ctrl+K). Montado só quando aberto. Setas ↑/↓ escolhem, Enter abre. */
export function SearchDialog({ onClose }: { onClose: () => void }) {
  const [text, setText] = useState('')
  const [active, setActive] = useState(0)
  const q = useDebouncedValue(text.trim(), 200)
  const navigate = useNavigate()
  const listId = useId()
  const search = useQuery({
    queryKey: ['search', q],
    queryFn: () => api.get<SearchResponse>(`/api/search?q=${encodeURIComponent(q)}`),
    enabled: q.length >= 2,
    placeholderData: (previous) => previous,
  })
  const results = q.length >= 2 ? (search.data?.results ?? []) : []
  const current = Math.min(active, Math.max(results.length - 1, 0))

  const open = (r: SearchResult) => {
    onClose()
    void navigate(r.url)
  }

  return (
    <ResponsiveDialog
      open
      onOpenChange={(o) => !o && onClose()}
      title="Buscar"
      className="sm:w-[min(36rem,calc(100vw-2rem))]"
    >
      <div className="relative">
        <Search
          className="pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2 text-muted"
          aria-hidden
        />
        <Input
          autoFocus
          role="combobox"
          aria-expanded={results.length > 0}
          aria-controls={listId}
          aria-activedescendant={results.length > 0 ? `${listId}-${current}` : undefined}
          aria-label="Buscar em gastos, universidades, tarefas e mais"
          placeholder="Gastos, universidades, tarefas, mala…"
          className="pl-9"
          value={text}
          onChange={(e) => {
            setText(e.target.value)
            setActive(0)
          }}
          onKeyDown={(e) => {
            if (e.key === 'ArrowDown') {
              e.preventDefault()
              setActive((i) => Math.min(i + 1, results.length - 1))
            } else if (e.key === 'ArrowUp') {
              e.preventDefault()
              setActive((i) => Math.max(i - 1, 0))
            } else if (e.key === 'Enter' && results[current]) {
              e.preventDefault()
              open(results[current])
            }
          }}
        />
      </div>
      <div className="mt-3 min-h-24" aria-live="polite">
        {q.length < 2 ? (
          <p className="px-1 py-6 text-center text-sm text-muted">
            Digite ao menos 2 letras. Acentos não importam.
          </p>
        ) : search.isPending ? (
          <p className="px-1 py-6 text-center text-sm text-muted">Buscando…</p>
        ) : results.length === 0 ? (
          <p className="px-1 py-6 text-center text-sm text-muted">Nada encontrado para “{q}”.</p>
        ) : (
          <ul id={listId} role="listbox" aria-label="Resultados" className="grid gap-1">
            {results.map((r, i) => (
              <li
                key={`${r.type}-${r.id}`}
                id={`${listId}-${i}`}
                role="option"
                aria-selected={i === current}
                onMouseEnter={() => setActive(i)}
                onClick={() => open(r)}
                className={cn(
                  'flex cursor-pointer items-center gap-3 rounded-xl px-3 py-2.5',
                  i === current ? 'bg-primary-soft' : 'hover:bg-surface-2',
                )}
              >
                <span className="grid size-8 shrink-0 place-items-center rounded-lg bg-surface-2 text-muted">
                  <ResultIcon type={r.type} />
                </span>
                <span className="min-w-0 flex-1">
                  <span className="block truncate text-sm font-medium">{r.title}</span>
                  {r.subtitle && <span className="block truncate text-xs text-muted">{r.subtitle}</span>}
                </span>
                <span className="shrink-0 text-xs text-muted">{TYPES[r.type].label}</span>
              </li>
            ))}
          </ul>
        )}
      </div>
    </ResponsiveDialog>
  )
}
