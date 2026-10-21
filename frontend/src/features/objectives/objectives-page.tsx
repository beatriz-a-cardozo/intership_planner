import { CalendarDays, MapPin, Plus, Star, Target } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router'
import { EmptyState } from '@/components/empty-state'
import { ErrorState } from '@/components/error-state'
import { PageHeader } from '@/components/page-header'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card } from '@/components/ui/card'
import { Skeleton } from '@/components/ui/skeleton'
import { formatDate, formatMoney } from '@/lib/format'
import { countryName, TARGET_MODE_LABELS } from '@/lib/labels'
import { useObjectives } from './api'
import { ObjectiveFormDialog } from './objective-form-dialog'

const STATUS: Record<string, string> = { ACTIVE: 'Ativo', COMPLETED: 'Concluído', ARCHIVED: 'Arquivado' }

export function ObjectivesPage() {
  const objectives = useObjectives()
  const [creating, setCreating] = useState(false)
  const navigate = useNavigate()

  return (
    <>
      <PageHeader
        title="Objetivos"
        description="Cada objetivo tem sua meta, prazo e gastos. O principal aparece no dashboard."
        actions={
          <Button onClick={() => setCreating(true)}>
            <Plus aria-hidden /> Novo objetivo
          </Button>
        }
      />

      {objectives.isPending ? (
        <div className="grid gap-4 sm:grid-cols-2">
          <Skeleton className="h-40 rounded-card" />
          <Skeleton className="h-40 rounded-card" />
        </div>
      ) : objectives.isError ? (
        <Card>
          <ErrorState error={objectives.error} onRetry={() => void objectives.refetch()} />
        </Card>
      ) : objectives.data.length === 0 ? (
        <Card>
          <EmptyState
            icon={Target}
            title="Nenhum objetivo"
            description="Crie seu primeiro objetivo para começar o planejamento."
            action={<Button onClick={() => setCreating(true)}>Criar objetivo</Button>}
          />
        </Card>
      ) : (
        <ul className="grid gap-4 sm:grid-cols-2">
          {objectives.data.map((o) => {
            const place = [o.city, countryName(o.countryCode)].filter(Boolean).join(', ')
            return (
              <li key={o.id}>
                <Link
                  to={`/objectives/${o.id}`}
                  className="block h-full rounded-card focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring/40"
                >
                  <Card className="h-full p-5 transition-colors hover:border-primary/40">
                    <div className="flex items-start justify-between gap-3">
                      <h2 className="font-semibold">{o.name}</h2>
                      <div className="flex shrink-0 gap-1.5">
                        {o.primary && (
                          <Badge tone="primary">
                            <Star className="size-3" aria-hidden /> Principal
                          </Badge>
                        )}
                        {o.status !== 'ACTIVE' && <Badge>{STATUS[o.status]}</Badge>}
                      </div>
                    </div>
                    <div className="mt-3 grid gap-1.5 text-sm text-muted">
                      {place && (
                        <span className="flex items-center gap-2">
                          <MapPin className="size-4" aria-hidden /> {place}
                        </span>
                      )}
                      <span className="flex items-center gap-2">
                        <CalendarDays className="size-4" aria-hidden /> {formatDate(o.targetDate, 'long')}
                      </span>
                    </div>
                    <p className="mt-4 text-sm">
                      <span className="text-muted">Meta: </span>
                      {o.targetMode === 'MANUAL' && o.targetAmount != null
                        ? formatMoney({ amount: o.targetAmount, currency: o.currency })
                        : TARGET_MODE_LABELS.DERIVED.toLowerCase()}
                    </p>
                  </Card>
                </Link>
              </li>
            )
          })}
        </ul>
      )}

      {creating && (
        <ObjectiveFormDialog
          open
          onOpenChange={setCreating}
          onSaved={(o) => navigate(`/objectives/${o.id}`)}
        />
      )}
    </>
  )
}
