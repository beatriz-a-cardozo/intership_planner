import { CalendarRange, Check, Plus } from 'lucide-react'
import { useState } from 'react'
import { Link } from 'react-router'
import type { TimelineItem, TimelineSource } from '@/api/types'
import { EmptyState } from '@/components/empty-state'
import { ErrorState } from '@/components/error-state'
import { PageHeader } from '@/components/page-header'
import { Button } from '@/components/ui/button'
import { Card } from '@/components/ui/card'
import { SegmentedControl } from '@/components/ui/segmented'
import { Skeleton } from '@/components/ui/skeleton'
import { formatDate, formatMoney, formatMonth, todayIso } from '@/lib/format'
import { PRIORITY_LABELS, TIMELINE_SOURCE_LABELS } from '@/lib/labels'
import { cn } from '@/lib/utils'
import { useTimeline } from './api'
import { EventDialog } from './event-dialog'
import { TimelineIcon } from './timeline-icon'
import { itemLink } from './timeline-item'

type Range = 'ahead' | 'past'

const SOURCES = Object.keys(TIMELINE_SOURCE_LABELS) as TimelineSource[]

/** Intervalo da consulta (só para pedir os dados; as datas dos itens vêm prontas da API). */
function rangeFor(range: Range) {
  const today = todayIso()
  const [y, m] = today.split('-').map(Number)
  const iso = (year: number, month: number, day: number) =>
    new Date(year, month - 1, day).toLocaleDateString('sv-SE') // yyyy-MM-dd local
  return range === 'ahead'
    ? { from: iso(y!, m! - 1, 1), to: iso(y!, m! + 12, 0) }
    : { from: iso(y!, m! - 12, 1), to: today }
}

export function TimelinePage() {
  const [range, setRange] = useState<Range>('ahead')
  const [hidden, setHidden] = useState<TimelineSource[]>([])
  const [editing, setEditing] = useState<TimelineItem | 'new' | null>(null)
  const sources = hidden.length ? SOURCES.filter((s) => !hidden.includes(s)) : undefined
  const timeline = useTimeline({ ...rangeFor(range), sources })

  const items = range === 'past' ? [...(timeline.data?.items ?? [])].reverse() : (timeline.data?.items ?? [])
  const months = groupByMonth(items)
  const toggle = (s: TimelineSource) =>
    setHidden((h) =>
      h.includes(s) ? h.filter((x) => x !== s) : h.length === SOURCES.length - 1 ? h : [...h, s],
    )

  return (
    <>
      <PageHeader
        title="Timeline"
        description="Gastos com data, períodos das universidades (inscrições, prazos e respostas) e tarefas numa linha só, mais os seus eventos."
        actions={
          <Button onClick={() => setEditing('new')}>
            <Plus aria-hidden /> Evento
          </Button>
        }
      />

      <div className="mb-6 grid gap-3">
        <SegmentedControl
          ariaLabel="Período"
          value={range}
          onValueChange={setRange}
          options={[
            { value: 'ahead', label: 'Próximos 12 meses' },
            { value: 'past', label: 'Últimos 12 meses' },
          ]}
        />
        <div className="flex flex-wrap gap-2" role="group" aria-label="Mostrar na timeline">
          {SOURCES.map((s) => {
            const on = !hidden.includes(s)
            return (
              <button
                key={s}
                type="button"
                aria-pressed={on}
                onClick={() => toggle(s)}
                className={cn(
                  'inline-flex min-h-9 items-center gap-1.5 rounded-full border px-3 text-sm transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring/40',
                  on
                    ? 'border-primary/40 bg-primary-soft text-foreground'
                    : 'border-border text-muted line-through decoration-muted/60',
                )}
              >
                {on && <Check className="size-3.5 text-primary" aria-hidden />}
                {TIMELINE_SOURCE_LABELS[s]}
              </button>
            )
          })}
        </div>
      </div>

      {timeline.isPending ? (
        <Skeleton className="h-96 rounded-card" />
      ) : timeline.isError ? (
        <Card>
          <ErrorState error={timeline.error} onRetry={() => void timeline.refetch()} />
        </Card>
      ) : months.length === 0 ? (
        <Card>
          <EmptyState
            icon={CalendarRange}
            title="Nada neste período"
            description="Prazos de gastos, parcelas, candidaturas e tarefas aparecem aqui sozinhos. Você também pode criar eventos, como o voo ou uma prova."
            action={<Button onClick={() => setEditing('new')}>Criar evento</Button>}
          />
        </Card>
      ) : (
        <div className="grid gap-10">
          {months.map(([month, list]) => (
            <section key={month} aria-labelledby={`m-${month}`}>
              <h2
                id={`m-${month}`}
                className="mb-4 font-display text-xl font-semibold first-letter:uppercase sm:text-2xl"
              >
                {formatMonth(month)}
              </h2>
              <ol className="relative grid gap-4 before:absolute before:top-2 before:bottom-2 before:left-[1.6rem] before:w-0.5 before:rounded-full before:bg-primary/25 lg:before:left-1/2 lg:before:-translate-x-1/2">
                {list.map((item, i) => (
                  <TimelineRow
                    key={item.id}
                    item={item}
                    side={i % 2 === 0 ? 'left' : 'right'}
                    today={timeline.data!.today}
                    onEdit={() => setEditing(item)}
                  />
                ))}
              </ol>
            </section>
          ))}
        </div>
      )}

      {editing !== null && (
        <EventDialog item={editing === 'new' ? undefined : editing} onClose={() => setEditing(null)} />
      )}
    </>
  )
}

function groupByMonth(items: TimelineItem[]) {
  const map = new Map<string, TimelineItem[]>()
  for (const item of items) {
    const key = item.date.slice(0, 7)
    map.set(key, [...(map.get(key) ?? []), item])
  }
  return [...map.entries()]
}

const WEEKDAY = new Intl.DateTimeFormat('pt-BR', { weekday: 'short' })

function TimelineRow({
  item,
  side,
  today,
  onEdit,
}: {
  item: TimelineItem
  side: 'left' | 'right'
  today: string
  onEdit: () => void
}) {
  const link = itemLink(item)
  const [y, m, d] = item.date.split('-').map(Number)
  const isToday = item.date === today
  const done = item.status === 'DONE'

  const body = (
    <>
      <div className="flex items-start gap-3">
        <span
          className={cn(
            'grid size-9 shrink-0 place-items-center rounded-xl',
            item.status === 'OVERDUE'
              ? 'bg-danger-soft text-danger'
              : done
                ? 'bg-surface-2 text-muted'
                : 'bg-primary-soft text-primary',
          )}
          aria-hidden
        >
          <TimelineIcon item={item} className="size-4.5" />
        </span>
        <div className="min-w-0 flex-1">
          <p className={cn('font-medium', done && 'text-muted')}>
            {item.title}
            {done && <span className="sr-only"> (concluído)</span>}
          </p>
          <p className="text-xs text-muted">
            {[
              TIMELINE_SOURCE_LABELS[item.source],
              item.subtitle,
              item.endDate && `até ${formatDate(item.endDate)}`,
              item.priority &&
                item.priority !== 'MEDIUM' &&
                `prioridade ${PRIORITY_LABELS[item.priority].toLowerCase()}`,
            ]
              .filter(Boolean)
              .join(' · ')}
          </p>
        </div>
        <div className="shrink-0 text-right">
          {item.amount && <p className="text-sm font-medium tabular">{formatMoney(item.amount)}</p>}
          {item.status === 'OVERDUE' && <p className="text-xs font-medium text-danger">Atrasado</p>}
          {done && <Check className="ml-auto size-4 text-primary" aria-hidden />}
        </div>
      </div>
    </>
  )

  const cardClass = cn(
    'block rounded-card border border-border bg-surface p-4 text-left shadow-card transition-colors hover:border-primary/40 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring/40',
    done && 'opacity-75',
  )

  return (
    <li
      className={cn(
        'relative grid grid-cols-[3.25rem_1fr] items-start gap-3',
        'lg:grid-cols-[1fr_4rem_1fr] lg:gap-6',
      )}
    >
      {/* data em numeral grande, na espinha */}
      <div
        className={cn(
          'relative z-10 grid place-items-center rounded-2xl border-2 bg-background py-1.5 text-center',
          isToday ? 'border-primary' : 'border-transparent',
          'lg:col-start-2 lg:row-start-1',
        )}
      >
        <span className="font-display text-2xl leading-none font-semibold tabular">{d}</span>
        <span className="text-[11px] uppercase text-muted">
          {WEEKDAY.format(new Date(y!, m! - 1, d!)).replace('.', '')}
        </span>
      </div>
      <div className={cn('min-w-0 lg:row-start-1', side === 'left' ? 'lg:col-start-1' : 'lg:col-start-3')}>
        {item.source === 'EVENT' ? (
          <button type="button" className={cn(cardClass, 'w-full')} onClick={onEdit}>
            {body}
          </button>
        ) : link ? (
          <Link to={link} className={cardClass}>
            {body}
          </Link>
        ) : (
          <div className={cardClass}>{body}</div>
        )}
      </div>
    </li>
  )
}
