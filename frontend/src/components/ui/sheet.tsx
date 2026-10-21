import { Dialog } from 'radix-ui'
import { X } from 'lucide-react'
import type { ComponentProps, ReactNode } from 'react'
import { cn } from '@/lib/utils'

export const Sheet = Dialog.Root
export const SheetTrigger = Dialog.Trigger
export const SheetClose = Dialog.Close

interface SheetContentProps extends ComponentProps<typeof Dialog.Content> {
  side?: 'bottom' | 'left'
  title: string
  description?: string
  children: ReactNode
}

/** Painel deslizante: "bottom" para mobile (gaveta), "left" para menus laterais. */
export function SheetContent({ side = 'bottom', title, description, className, children, ...props }: SheetContentProps) {
  return (
    <Dialog.Portal>
      <Dialog.Overlay className="fixed inset-0 z-50 bg-black/40 backdrop-blur-[2px]" />
      <Dialog.Content
        className={cn(
          'fixed z-50 flex flex-col gap-4 bg-surface p-5 shadow-xl outline-none',
          side === 'bottom' &&
            'inset-x-0 bottom-0 max-h-[85dvh] rounded-t-[1.75rem] pb-[max(1.25rem,env(safe-area-inset-bottom))]',
          side === 'left' && 'inset-y-0 left-0 w-[min(20rem,85vw)]',
          className,
        )}
        {...(description ? {} : { 'aria-describedby': undefined })}
        {...props}
      >
        {side === 'bottom' && <div aria-hidden className="mx-auto h-1.5 w-10 rounded-full bg-border" />}
        <div className="flex items-center justify-between">
          <Dialog.Title className="text-base font-semibold">{title}</Dialog.Title>
          <Dialog.Close className="grid size-10 place-items-center rounded-full text-muted hover:bg-surface-2" aria-label="Fechar">
            <X className="size-5" />
          </Dialog.Close>
        </div>
        {description && <Dialog.Description className="text-sm text-muted">{description}</Dialog.Description>}
        <div className="overflow-y-auto">{children}</div>
      </Dialog.Content>
    </Dialog.Portal>
  )
}
