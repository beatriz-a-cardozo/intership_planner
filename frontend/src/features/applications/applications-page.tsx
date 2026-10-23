import { ClipboardCheck } from 'lucide-react'
import { useState } from 'react'
import { Link } from 'react-router'
import type { Application, ApplicationStatus } from '@/api/types'
import { EmptyState } from '@/components/empty-state'
import { ErrorState } from '@/components/error-state'
import { PageHeader } from '@/components/page-header'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card } from '@/components/ui/card'
import { Skeleton } from '@/components/ui/skeleton'
import { SwitchField } from '@/components/ui/switch'
import { useApplications } from '@/features/universities/api'
import { ResponseRange } from '@/features/universities/application-panel'
import { StatusSelect } from '@/features/universities/bits'
import { formatDate, pluralize } from '@/lib/format'
import { TERM_SHORT } from '@/lib/labels'
import { cn } from '@/lib/utils'

/** Os 10 status agrupados em 4 colunas do quadro, na ordem em que a candidatura avança. */
const COLUMNS: { key: string; title: string; hint: string; statuses: ApplicationStatus[] }[] = [
  { key: 'prep', title: 'Preparando', hint: 'Antes de enviar', statuses: ['AWAITING_OPENING'] },
  {
    key: 'action',
    title: 'Ação necessária',
    hint: 'A universidade pediu algo',
    statuses: ['ITEM_REQUESTED', 'EXAM_REQUESTED', 'INTERVIEW_REQUESTED'],
  },
  {
    key: 'review',
    title: 'Em análise',
    hint: 'Aguardando a universidade',
    statuses: ['DOCUMENTS_UNDER_REVIEW', 'AWAITING_RESULT', 'WAITLISTED'],
  },
  { key: 'done', title: 'Resultado', hint: 'Resposta final', statuses: ['ACCEPTED', 'ENROLLED', 'REJECTED'] },
]

export function ApplicationsPage() {
  const [includeArchived, setIncludeArchived] = useState(false)
  const applications = useApplications({ includeArchived: includeArchived || undefined })

  return (
    <>
      <PageHeader
        title="Candidaturas"
        description="Uma por programa escolhido. Mude o status direto no card; o histórico fica registrado."
        actions={
          <SwitchField
            id="include-archived"
            label="Mostrar arquivadas"
            checked={includeArchived}
            onCheckedChange={setIncludeArchived}
          />
        }
      />

      {applications.isPending ? (
        <div className="grid gap-4 @4xl:grid-cols-4">
          {COLUMNS.map((c) => (
            <Skeleton key={c.key} className="h-48 rounded-card" />
          ))}
        </div>
      ) : applications.isError ? (
        <Card>
          <ErrorState error={applications.error} onRetry={() => void applications.refetch()} />
        </Card>
      ) : applications.data.length === 0 ? (
        <Card>
          <EmptyState
            icon={ClipboardCheck}
            title="Nenhuma candidatura"
            description="Em Universidades, marque um programa como “Escolhida” para criar a candidatura."
            action={
              <Button asChild>
                <Link to="/universities">Ver programas</Link>
              </Button>
            }
          />
        </Card>
      ) : (
        <div className="grid gap-6 @4xl:grid-cols-4 @4xl:gap-4">
          {COLUMNS.map((col) => {
            const items = applications.data.filter((a) => col.statuses.includes(a.status))
            return (
              <section
                key={col.key}
                aria-labelledby={`col-${col.key}`}
                className="min-w-0 @4xl:rounded-card @4xl:bg-surface-2/60 @4xl:p-3"
              >
                <header className="mb-3 flex items-baseline justify-between gap-2 px-1">
                  <h2 id={`col-${col.key}`} className="text-sm font-semibold">
                    {col.title}
                  </h2>
                  <span className="text-xs text-muted">
                    {items.length > 0 ? pluralize(items.length, 'candidatura', 'candidaturas') : col.hint}
                  </span>
                </header>
                {items.length > 0 && (
                  <ul className="grid gap-3">
                    {items.map((a) => (
                      <li key={a.id}>
                        <ApplicationCard application={a} />
                      </li>
                    ))}
                  </ul>
                )}
              </section>
            )
          })}
        </div>
      )}
    </>
  )
}

function ApplicationCard({ application: a }: { application: Application }) {
  const deadline = a.deadline ?? a.window?.closesOn ?? null
  return (
    <Card className={cn('grid gap-3 p-4', a.archived && 'opacity-60')}>
      <div className="min-w-0">
        <Link to={`/programs/${a.program.id}?tab=application`} className="font-medium hover:text-primary">
          {a.program.name}
        </Link>
        <p className="truncate text-xs text-muted">
          {a.program.universityName} · {TERM_SHORT[a.term]} {a.intakeYear}
        </p>
      </div>
      <StatusSelect id={`board-status-${a.id}`} application={a} />
      <div className="flex flex-wrap gap-1.5">
        {a.archived && <Badge>Arquivada</Badge>}
        {a.pendingRequests > 0 && (
          <Badge tone="accent">
            {pluralize(a.pendingRequests, 'pendência', 'pendências')}
            {a.nextRequestDue && ` · até ${formatDate(a.nextRequestDue)}`}
          </Badge>
        )}
        {!a.submittedAt && deadline && <Badge>Prazo {formatDate(deadline)}</Badge>}
      </div>
      {a.submittedAt && a.result === 'PENDING' && <ResponseRange application={a} />}
    </Card>
  )
}
