import { ResponsiveDialog } from '@/components/ui/responsive-dialog'
import { Skeleton } from '@/components/ui/skeleton'

/**
 * Diálogo de formulário enquanto as listas das opções carregam. O formulário só deve montar depois:
 * um `<select>` não controlado perde o valor inicial se a opção ainda não existir na montagem
 * (editar uma conta vinculada ao objetivo salvaria "nenhum").
 */
export function DialogLoading({
  title,
  onClose,
  className,
}: {
  title: string
  onClose: () => void
  className?: string
}) {
  return (
    <ResponsiveDialog open onOpenChange={(o) => !o && onClose()} title={title} className={className}>
      <div className="grid gap-4" aria-busy="true" aria-label="Carregando">
        <Skeleton className="h-11" />
        <Skeleton className="h-11" />
        <Skeleton className="h-11" />
      </div>
    </ResponsiveDialog>
  )
}
