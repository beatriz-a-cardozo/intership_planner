import { AlertTriangle } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { errorMessage } from '@/api/errors'

export function ErrorState({ error, onRetry }: { error: unknown; onRetry?: () => void }) {
  return (
    <div role="alert" className="flex flex-col items-center px-6 py-12 text-center">
      <div className="mb-4 grid size-14 place-items-center rounded-2xl bg-danger-soft text-danger">
        <AlertTriangle className="size-6" aria-hidden />
      </div>
      <h3 className="text-base font-semibold">Não foi possível carregar</h3>
      <p className="mt-1.5 max-w-sm text-sm text-muted">{errorMessage(error)}</p>
      {onRetry && (
        <Button variant="outline" className="mt-5" onClick={onRetry}>
          Tentar novamente
        </Button>
      )}
    </div>
  )
}
