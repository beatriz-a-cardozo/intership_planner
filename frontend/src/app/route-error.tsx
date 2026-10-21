import { AlertTriangle } from 'lucide-react'
import { useRouteError } from 'react-router'
import { Button } from '@/components/ui/button'

/** Erro inesperado ao renderizar uma rota: mensagem amigável em vez da tela padrão do React Router. */
export function RouteError() {
  const error = useRouteError()
  if (import.meta.env.DEV) console.error(error)
  return (
    <div role="alert" className="grid min-h-dvh place-items-center bg-background px-6">
      <div className="flex max-w-sm flex-col items-center text-center">
        <div className="mb-4 grid size-14 place-items-center rounded-2xl bg-danger-soft text-danger">
          <AlertTriangle className="size-6" aria-hidden />
        </div>
        <h1 className="font-display text-2xl font-semibold">Algo deu errado nesta tela</h1>
        <p className="mt-2 text-sm text-muted">
          Recarregue a página. Se continuar, volte ao início e tente de novo.
        </p>
        <div className="mt-6 flex gap-2">
          <Button variant="outline" onClick={() => window.location.reload()}>
            Recarregar
          </Button>
          <Button onClick={() => window.location.assign('/')}>Ir para o início</Button>
        </div>
      </div>
    </div>
  )
}
