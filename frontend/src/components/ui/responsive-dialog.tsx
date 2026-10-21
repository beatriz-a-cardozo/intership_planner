import { Dialog } from 'radix-ui'
import { X } from 'lucide-react'
import type { ReactNode } from 'react'
import { cn } from '@/lib/utils'

interface ResponsiveDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  title: string
  description?: string
  children?: ReactNode
  /** Rodapé fixo (botões de ação). */
  footer?: ReactNode
  className?: string
}

/**
 * Diálogo único para o app: gaveta de baixo para cima no celular (alcance do polegar) e
 * modal centralizado a partir de `sm`. Formulários longos rolam por dentro, com o rodapé sempre visível.
 */
export function ResponsiveDialog({
  open,
  onOpenChange,
  title,
  description,
  children,
  footer,
  className,
}: ResponsiveDialogProps) {
  return (
    <Dialog.Root open={open} onOpenChange={onOpenChange}>
      <Dialog.Portal>
        <Dialog.Overlay className="fixed inset-0 z-50 bg-black/40 backdrop-blur-[2px]" />
        <Dialog.Content
          className={cn(
            'fixed z-50 flex max-h-[92dvh] flex-col bg-surface shadow-xl outline-none',
            'inset-x-0 bottom-0 rounded-t-[1.75rem] pb-[env(safe-area-inset-bottom)]',
            'sm:inset-x-auto sm:bottom-auto sm:left-1/2 sm:top-1/2 sm:w-[min(36rem,calc(100vw-2rem))] sm:-translate-x-1/2 sm:-translate-y-1/2 sm:rounded-card sm:pb-0',
            className,
          )}
          {...(description ? {} : { 'aria-describedby': undefined })}
        >
          <div aria-hidden className="mx-auto mt-3 h-1.5 w-10 rounded-full bg-border sm:hidden" />
          <div className="flex items-start justify-between gap-3 px-5 pb-2 pt-4 sm:px-6 sm:pt-6">
            <div>
              <Dialog.Title className="font-display text-xl font-semibold tracking-tight">
                {title}
              </Dialog.Title>
              {description && (
                <Dialog.Description className="mt-1 text-sm text-muted">{description}</Dialog.Description>
              )}
            </div>
            <Dialog.Close
              className="grid size-10 shrink-0 place-items-center rounded-full text-muted hover:bg-surface-2"
              aria-label="Fechar"
            >
              <X className="size-5" />
            </Dialog.Close>
          </div>
          {children ? (
            <div className="min-h-0 flex-1 overflow-y-auto px-5 pb-5 pt-2 sm:px-6">{children}</div>
          ) : (
            <div className="pb-2" />
          )}
          {footer && (
            <div className="flex flex-col-reverse gap-2 border-t border-border px-5 py-4 sm:flex-row sm:justify-end sm:px-6">
              {footer}
            </div>
          )}
        </Dialog.Content>
      </Dialog.Portal>
    </Dialog.Root>
  )
}
