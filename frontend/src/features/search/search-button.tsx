import { Search } from 'lucide-react'
import { useEffect, useState } from 'react'
import { cn } from '@/lib/utils'
import { SearchDialog } from './search-dialog'

/**
 * Botão da busca global. O AppShell monta dois (barra lateral e cabeçalho móvel, um deles oculto por CSS):
 * só o que recebe `shortcut` escuta Ctrl/Cmd+K, para não abrir dois diálogos.
 */
export function SearchButton({ shortcut = false, className }: { shortcut?: boolean; className?: string }) {
  const [open, setOpen] = useState(false)

  useEffect(() => {
    if (!shortcut) return
    const onKey = (e: KeyboardEvent) => {
      if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'k') {
        e.preventDefault()
        setOpen(true)
      }
    }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [shortcut])

  return (
    <>
      <button
        type="button"
        onClick={() => setOpen(true)}
        className={cn(
          'grid size-10 place-items-center rounded-xl text-muted transition-colors hover:bg-surface-2 hover:text-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring/40',
          className,
        )}
        aria-label="Buscar (Ctrl+K)"
        title="Buscar (Ctrl+K)"
      >
        <Search className="size-5" aria-hidden />
      </button>
      {open && <SearchDialog onClose={() => setOpen(false)} />}
    </>
  )
}
