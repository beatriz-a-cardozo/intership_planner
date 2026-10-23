import { CalendarDays, Check, ListChecks, Pencil, Plus, Trash2 } from 'lucide-react'
import { useState } from 'react'
import { Link } from 'react-router'
import { toast } from 'sonner'
import { errorMessage } from '@/api/errors'
import type { Task, TaskStatus } from '@/api/types'
import { ConfirmDialog } from '@/components/confirm-dialog'
import { EmptyState } from '@/components/empty-state'
import { ErrorState } from '@/components/error-state'
import { PageHeader } from '@/components/page-header'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card } from '@/components/ui/card'
import { SegmentedControl } from '@/components/ui/segmented'
import { Skeleton } from '@/components/ui/skeleton'
import { formatDate } from '@/lib/format'
import { PRIORITY_LABELS, PRIORITY_TONES, TASK_STATUS_LABELS } from '@/lib/labels'
import { cn } from '@/lib/utils'
import { useDeleteTask, useTasks, useTaskStatus } from './api'
import { TaskDialog } from './task-dialog'

type View = 'open' | 'done' | 'all'

export function TasksPage() {
  const [view, setView] = useState<View>('open')
  const tasks = useTasks(view === 'done' ? { status: 'DONE' } : {})
  const [editing, setEditing] = useState<Task | 'new' | null>(null)
  const [deleting, setDeleting] = useState<Task | null>(null)
  const remove = useDeleteTask()

  const list = (tasks.data ?? []).filter((t) => view !== 'open' || t.status !== 'DONE')

  return (
    <>
      <PageHeader
        title="Tarefas"
        description="O que precisa ser feito, com prazo e prioridade. Tarefas com prazo também aparecem na timeline."
        actions={
          <Button onClick={() => setEditing('new')}>
            <Plus aria-hidden /> Nova tarefa
          </Button>
        }
      />

      <SegmentedControl
        className="mb-5"
        ariaLabel="Mostrar"
        value={view}
        onValueChange={setView}
        options={[
          { value: 'open', label: 'Abertas' },
          { value: 'done', label: 'Concluídas' },
          { value: 'all', label: 'Todas' },
        ]}
      />

      {tasks.isPending ? (
        <Skeleton className="h-64 rounded-card" />
      ) : tasks.isError ? (
        <Card>
          <ErrorState error={tasks.error} onRetry={() => void tasks.refetch()} />
        </Card>
      ) : list.length === 0 ? (
        <Card>
          <EmptyState
            icon={ListChecks}
            title={view === 'done' ? 'Nada concluído ainda' : 'Nenhuma tarefa aberta'}
            description="Ex.: traduzir o histórico, agendar o TOEFL, pedir carta de recomendação."
            action={view !== 'done' && <Button onClick={() => setEditing('new')}>Criar tarefa</Button>}
          />
        </Card>
      ) : (
        <Card className="p-0">
          <ul className="divide-y divide-border">
            {list.map((t) => (
              <TaskRow key={t.id} task={t} onEdit={() => setEditing(t)} onDelete={() => setDeleting(t)} />
            ))}
          </ul>
        </Card>
      )}

      {editing !== null && (
        <TaskDialog task={editing === 'new' ? undefined : editing} onClose={() => setEditing(null)} />
      )}
      <ConfirmDialog
        open={deleting !== null}
        onOpenChange={(o) => !o && setDeleting(null)}
        title="Excluir tarefa?"
        description={deleting ? `"${deleting.title}" será excluída.` : ''}
        confirmLabel="Excluir"
        destructive
        loading={remove.isPending}
        onConfirm={() =>
          deleting &&
          remove.mutate(deleting.id, {
            onSuccess: () => {
              toast.success('Tarefa excluída.')
              setDeleting(null)
            },
            onError: (e) => toast.error(errorMessage(e)),
          })
        }
      />
    </>
  )
}

function TaskRow({ task: t, onEdit, onDelete }: { task: Task; onEdit: () => void; onDelete: () => void }) {
  const status = useTaskStatus()
  const done = t.status === 'DONE'
  const toggle = (next: TaskStatus) =>
    status.mutate(
      { id: t.id, status: next },
      {
        onSuccess: () => next === 'DONE' && toast.success('Tarefa concluída.'),
        onError: (e) => toast.error(errorMessage(e)),
      },
    )

  const links = [
    t.objective && { to: `/objectives/${t.objective.id}`, label: t.objective.label },
    t.university && { to: `/universities/${t.university.id}`, label: t.university.label },
    t.application && {
      to: `/programs/${t.application.parentId}?tab=application`,
      label: t.application.label,
    },
    t.expense && { to: '/expenses', label: t.expense.label },
  ].filter(Boolean) as { to: string; label: string }[]

  return (
    <li className="flex items-start gap-3 px-4 py-3 sm:px-5">
      <button
        type="button"
        role="checkbox"
        aria-checked={done}
        aria-label={done ? `Reabrir "${t.title}"` : `Concluir "${t.title}"`}
        disabled={status.isPending}
        onClick={() => toggle(done ? 'TODO' : 'DONE')}
        className={cn(
          'mt-0.5 grid size-6 shrink-0 place-items-center rounded-full border-2 transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring/40',
          done ? 'border-primary bg-primary text-primary-foreground' : 'border-border hover:border-primary',
        )}
      >
        {done && <Check className="size-3.5" aria-hidden />}
      </button>
      <div className="min-w-0 flex-1">
        <p className={cn('font-medium', done && 'text-muted line-through')}>{t.title}</p>
        {t.description && <p className="mt-0.5 line-clamp-2 text-sm text-muted">{t.description}</p>}
        <div className="mt-1.5 flex flex-wrap items-center gap-1.5 text-xs">
          {t.dueDate && (
            <span
              className={cn(
                'inline-flex items-center gap-1',
                t.overdue ? 'font-medium text-danger' : 'text-muted',
              )}
            >
              <CalendarDays className="size-3.5" aria-hidden />
              {t.overdue ? 'Atrasada · ' : ''}
              {formatDate(t.dueDate)}
            </span>
          )}
          {t.priority !== 'MEDIUM' && (
            <Badge tone={PRIORITY_TONES[t.priority]}>{PRIORITY_LABELS[t.priority]}</Badge>
          )}
          {t.status === 'IN_PROGRESS' && <Badge tone="primary">{TASK_STATUS_LABELS.IN_PROGRESS}</Badge>}
          {t.category && <Badge>{t.category}</Badge>}
          {links.map((l) => (
            <Link key={l.to + l.label} to={l.to} className="text-primary hover:underline">
              {l.label}
            </Link>
          ))}
        </div>
      </div>
      <div className="flex shrink-0">
        <Button variant="ghost" size="icon" aria-label={`Editar "${t.title}"`} onClick={onEdit}>
          <Pencil />
        </Button>
        <Button variant="ghost" size="icon" aria-label={`Excluir "${t.title}"`} onClick={onDelete}>
          <Trash2 />
        </Button>
      </div>
    </li>
  )
}
