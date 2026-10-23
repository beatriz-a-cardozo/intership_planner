import { ArrowLeft, MapPin, Pencil, Star, Trash2 } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router'
import { toast } from 'sonner'
import { errorMessage } from '@/api/errors'
import type { ObjectiveProgress } from '@/api/types'
import { ConfirmDialog } from '@/components/confirm-dialog'
import { ErrorState } from '@/components/error-state'
import { PageHeader } from '@/components/page-header'
import { ProgressRing } from '@/components/progress-ring'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Skeleton } from '@/components/ui/skeleton'
import { formatDate, formatMoney, pluralize } from '@/lib/format'
import { countryName, EXPENSE_SCOPE_LABELS } from '@/lib/labels'
import { AdjustmentsCard } from './adjustments-card'
import { ProjectionCard } from './projection-card'
import { ScopeGoalsCard } from './scope-goals-card'
import { useDeleteObjective, useMakePrimary, useObjective, useObjectiveProgress } from './api'
import { ObjectiveFormDialog } from './objective-form-dialog'

export function ObjectiveDetailPage() {
  const { id = '' } = useParams()
  const objective = useObjective(id)
  const progress = useObjectiveProgress(id)
  const makePrimary = useMakePrimary()
  const remove = useDeleteObjective()
  const navigate = useNavigate()
  const [editing, setEditing] = useState(false)
  const [deleting, setDeleting] = useState(false)

  if (objective.isPending) return <Skeleton className="h-96 rounded-card" />
  if (objective.isError) {
    return (
      <Card>
        <ErrorState error={objective.error} onRetry={() => void objective.refetch()} />
      </Card>
    )
  }

  const o = objective.data
  const place = [o.city, countryName(o.countryCode)].filter(Boolean).join(', ')

  return (
    <>
      <Link
        to="/objectives"
        className="mb-4 inline-flex h-10 items-center gap-1.5 text-sm text-muted hover:text-foreground"
      >
        <ArrowLeft className="size-4" aria-hidden /> Objetivos
      </Link>
      <PageHeader
        title={o.name}
        description={o.description ?? undefined}
        actions={
          <>
            {!o.primary && (
              <Button
                variant="outline"
                loading={makePrimary.isPending}
                onClick={() =>
                  makePrimary.mutate(o.id, {
                    onSuccess: () => toast.success('Agora este é o objetivo principal.'),
                    onError: (e) => toast.error(errorMessage(e)),
                  })
                }
              >
                <Star aria-hidden /> Tornar principal
              </Button>
            )}
            <Button
              variant="outline"
              size="icon"
              aria-label="Editar objetivo"
              onClick={() => setEditing(true)}
            >
              <Pencil />
            </Button>
            <Button
              variant="outline"
              size="icon"
              aria-label="Excluir objetivo"
              onClick={() => setDeleting(true)}
            >
              <Trash2 />
            </Button>
          </>
        }
      />
      <div className="-mt-3 mb-6 flex flex-wrap items-center gap-2 text-sm text-muted">
        {o.primary && (
          <Badge tone="primary">
            <Star className="size-3" aria-hidden /> Principal
          </Badge>
        )}
        {place && (
          <span className="flex items-center gap-1">
            <MapPin className="size-3.5" aria-hidden /> {place}
          </span>
        )}
        <span>· até {formatDate(o.targetDate, 'long')}</span>
        <span>· {pluralize(o.horizonMonths, 'mês', 'meses')} de duração</span>
      </div>

      <div className="grid gap-5 lg:grid-cols-[1.2fr_1fr]">
        <Card>
          <CardHeader className="flex-col">
            <CardTitle>Progresso</CardTitle>
            <CardDescription>
              {o.targetMode === 'MANUAL'
                ? 'Meta de valor fixo.'
                : `Meta calculada: ${EXPENSE_SCOPE_LABELS[o.expenseScope].toLowerCase()} + ajustes.`}
            </CardDescription>
          </CardHeader>
          <CardContent>
            {progress.isPending ? (
              <Skeleton className="h-56" />
            ) : progress.isError ? (
              <ErrorState error={progress.error} onRetry={() => void progress.refetch()} />
            ) : (
              <ProgressBreakdown p={progress.data} />
            )}
          </CardContent>
        </Card>
        <div className="grid content-start gap-5">
          <ProjectionCard objectiveId={o.id} />
          <AdjustmentsCard objectiveId={o.id} currency={o.currency} />
        </div>
      </div>
      {progress.data && (
        <div className="mt-5">
          <ScopeGoalsCard progress={progress.data} />
        </div>
      )}

      {editing && <ObjectiveFormDialog open onOpenChange={setEditing} objective={o} />}
      <ConfirmDialog
        open={deleting}
        onOpenChange={setDeleting}
        title="Excluir objetivo?"
        description="Os gastos, pagamentos e ajustes deste objetivo serão excluídos. As contas continuam, apenas desvinculadas."
        confirmLabel="Excluir objetivo"
        destructive
        loading={remove.isPending}
        onConfirm={() =>
          remove.mutate(o.id, {
            onSuccess: () => {
              toast.success('Objetivo excluído.')
              navigate('/objectives', { replace: true })
            },
            onError: (e) => toast.error(errorMessage(e)),
          })
        }
      />
    </>
  )
}

function Row({
  label,
  value,
  strong,
  sign,
}: {
  label: string
  value: string
  strong?: boolean
  sign?: string
}) {
  return (
    <div className={`flex items-center justify-between gap-3 py-2 text-sm ${strong ? 'font-semibold' : ''}`}>
      <dt className={strong ? '' : 'text-muted'}>{label}</dt>
      <dd className="tabular">
        {sign && <span className="mr-1 text-muted">{sign}</span>}
        {value}
      </dd>
    </div>
  )
}

function ProgressBreakdown({ p }: { p: ObjectiveProgress }) {
  return (
    <div className="grid gap-6 sm:grid-cols-[auto_1fr] sm:items-start">
      <ProgressRing percent={p.percentComplete} size={140} className="mx-auto" />
      <div>
        <dl className="divide-y divide-border">
          {p.targetMode === 'DERIVED' && (
            <>
              <Row label="Gastos no escopo" value={formatMoney(p.plannedExpenses)} />
              <Row
                label="Ajustes"
                value={formatMoney(p.adjustments)}
                sign={p.adjustments.amount > 0 ? '+' : undefined}
              />
            </>
          )}
          <Row label="Meta" value={formatMoney(p.target)} strong />
          <Row label="Já pago" value={formatMoney(p.paid)} />
          <Row label="Saldo em contas vinculadas" value={formatMoney(p.accountsBalance)} />
          <Row label="Mobilizado" value={formatMoney(p.mobilized)} strong />
          <Row label="Faltam" value={formatMoney(p.remaining)} strong />
        </dl>
        <div className="mt-4 rounded-xl bg-surface-2 p-4 text-sm">
          {p.overdue ? (
            <p className="text-danger">A data alvo já passou: faltam {formatMoney(p.remaining)}.</p>
          ) : (
            <p>
              Guarde <span className="font-semibold tabular">{formatMoney(p.monthlySavingNeeded)}</span> por
              mês nos próximos {pluralize(p.monthsRemaining, 'mês', 'meses')}.
            </p>
          )}
          {p.plannedMonthlySaving && p.onTrack != null && (
            <p className="mt-1 text-muted">
              Seu plano: {formatMoney(p.plannedMonthlySaving)}/mês —{' '}
              <span className={p.onTrack ? 'text-primary' : 'text-accent'}>
                {p.onTrack ? 'no ritmo' : 'abaixo do necessário'}
              </span>
              .
            </p>
          )}
        </div>
        {p.ratio > 1 && (
          <p className="mt-3 text-sm text-primary">
            Você já mobilizou {Math.round(p.ratio * 100)}% da meta. 🎉
          </p>
        )}
      </div>
    </div>
  )
}
