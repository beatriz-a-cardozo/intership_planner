import {
  BellOff,
  CalendarClock,
  CheckCheck,
  CreditCard,
  Euro,
  ListChecks,
  Mail,
  PartyPopper,
  Settings2,
  Trash2,
} from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router'
import { toast } from 'sonner'
import { errorMessage } from '@/api/errors'
import type { AppNotification, NotificationCategory } from '@/api/types'
import { EmptyState } from '@/components/empty-state'
import { ErrorState } from '@/components/error-state'
import { PageHeader } from '@/components/page-header'
import { Button } from '@/components/ui/button'
import { Card } from '@/components/ui/card'
import { SegmentedControl } from '@/components/ui/segmented'
import { Skeleton } from '@/components/ui/skeleton'
import { formatDateTime } from '@/lib/format'
import { NOTIFICATION_CATEGORY_LABELS } from '@/lib/labels'
import { cn } from '@/lib/utils'
import { useDeleteNotification, useMarkAllRead, useMarkRead, useNotifications } from './api'

const ICONS: Record<NotificationCategory, typeof Mail> = {
  DEADLINES: CalendarClock,
  PAYMENTS: CreditCard,
  RESPONSES: Mail,
  TASKS: ListChecks,
  EXCHANGE: Euro,
  MOTIVATIONAL: PartyPopper,
}

export function NotificationsPage() {
  const [view, setView] = useState<'all' | 'unread'>('all')
  const notifications = useNotifications(view === 'unread')
  const markAll = useMarkAllRead()
  const items = notifications.data?.pages.flatMap((p) => p.items) ?? []
  const unread = notifications.data?.pages[0]?.unreadCount ?? 0

  return (
    <>
      <PageHeader
        title="Notificações"
        description="Prazos, vencimentos, respostas esperadas, tarefas, câmbio e marcos do objetivo."
        actions={
          <>
            <Button asChild variant="outline">
              <Link to="/settings#notificacoes">
                <Settings2 aria-hidden /> Preferências
              </Link>
            </Button>
            <Button
              variant="outline"
              disabled={unread === 0}
              loading={markAll.isPending}
              onClick={() => markAll.mutate(undefined, { onError: (e) => toast.error(errorMessage(e)) })}
            >
              <CheckCheck aria-hidden /> Marcar todas como lidas
            </Button>
          </>
        }
      />
      <SegmentedControl
        className="mb-5"
        ariaLabel="Mostrar"
        value={view}
        onValueChange={setView}
        options={[
          { value: 'all', label: 'Todas' },
          { value: 'unread', label: unread > 0 ? `Não lidas (${unread})` : 'Não lidas' },
        ]}
      />

      {notifications.isPending ? (
        <Skeleton className="h-64 rounded-card" />
      ) : notifications.isError ? (
        <Card>
          <ErrorState error={notifications.error} onRetry={() => void notifications.refetch()} />
        </Card>
      ) : items.length === 0 ? (
        <Card>
          <EmptyState
            icon={BellOff}
            title={view === 'unread' ? 'Tudo lido' : 'Nenhuma notificação'}
            description="Avisamos com antecedência dos prazos de inscrição, vencimentos, respostas esperadas e tarefas."
          />
        </Card>
      ) : (
        <>
          <Card className="p-0">
            <ul className="divide-y divide-border">
              {items.map((n) => (
                <NotificationRow key={n.id} notification={n} />
              ))}
            </ul>
          </Card>
          {notifications.hasNextPage && (
            <div className="mt-4 flex justify-center">
              <Button
                variant="outline"
                loading={notifications.isFetchingNextPage}
                onClick={() => void notifications.fetchNextPage()}
              >
                Carregar mais
              </Button>
            </div>
          )}
        </>
      )}
    </>
  )
}

function NotificationRow({ notification: n }: { notification: AppNotification }) {
  const markRead = useMarkRead()
  const remove = useDeleteNotification()
  const navigate = useNavigate()
  const Icon = ICONS[n.category]

  const open = () => {
    if (!n.read) markRead.mutate(n.id)
    if (n.actionUrl) void navigate(n.actionUrl)
  }

  return (
    <li className={cn('flex items-start gap-3 px-4 py-3.5 sm:px-5', !n.read && 'bg-primary-soft/30')}>
      <span
        className={cn(
          'mt-0.5 grid size-9 shrink-0 place-items-center rounded-xl',
          n.severity === 'CRITICAL'
            ? 'bg-danger-soft text-danger'
            : n.severity === 'WARNING'
              ? 'bg-accent-soft text-accent'
              : 'bg-primary-soft text-primary',
        )}
        aria-hidden
      >
        <Icon className="size-4.5" />
      </span>
      <button
        type="button"
        onClick={open}
        className="min-w-0 flex-1 text-left focus-visible:outline-none focus-visible:underline"
      >
        <p className={cn('text-sm', n.read ? 'text-muted' : 'font-medium')}>
          {!n.read && <span className="sr-only">Não lida: </span>}
          {n.title}
        </p>
        {n.body && <p className="mt-0.5 text-sm text-muted">{n.body}</p>}
        <p className="mt-1 text-xs text-subtle">
          {NOTIFICATION_CATEGORY_LABELS[n.category]} · {formatDateTime(n.createdAt)}
        </p>
      </button>
      {!n.read && <span className="mt-2 size-2 shrink-0 rounded-full bg-primary" aria-hidden />}
      <Button
        variant="ghost"
        size="icon"
        aria-label={`Excluir "${n.title}"`}
        disabled={remove.isPending}
        onClick={() => remove.mutate(n.id, { onError: (e) => toast.error(errorMessage(e)) })}
      >
        <Trash2 />
      </Button>
    </li>
  )
}
