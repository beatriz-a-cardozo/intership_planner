import { Compass } from 'lucide-react'
import { Link } from 'react-router'
import { EmptyState } from '@/components/empty-state'
import { Button } from '@/components/ui/button'

export function NotFoundPage() {
  return (
    <div className="grid min-h-[60dvh] place-items-center">
      <EmptyState
        icon={Compass}
        title="Página não encontrada"
        description="O endereço acessado não existe ou foi movido."
        action={
          <Button asChild>
            <Link to="/">Ir para o dashboard</Link>
          </Button>
        }
      />
    </div>
  )
}
