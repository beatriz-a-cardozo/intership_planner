import { Bell } from 'lucide-react'
import { useEffect, useRef } from 'react'
import { Link } from 'react-router'
import { cn } from '@/lib/utils'
import { useRefreshNotifications, useUnreadCount } from './api'

/** Sino com o número de não lidas. Na primeira montagem, gera as notificações do dia (idempotente). */
export function NotificationBell({ className }: { className?: string }) {
  const unread = useUnreadCount()
  const refresh = useRefreshNotifications()
  const started = useRef(false)

  useEffect(() => {
    if (started.current) return
    started.current = true
    refresh.mutate()
  }, [refresh])

  const count = unread.data?.count ?? 0
  return (
    <Link
      to="/notifications"
      className={cn(
        'relative grid size-10 place-items-center rounded-xl text-muted transition-colors hover:bg-surface-2 hover:text-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring/40',
        className,
      )}
      aria-label={count > 0 ? `Notificações: ${count} não lida${count > 1 ? 's' : ''}` : 'Notificações'}
    >
      <Bell className="size-5" aria-hidden />
      {count > 0 && (
        <span
          className="absolute top-1 right-1 grid min-w-4.5 place-items-center rounded-full bg-danger px-1 text-[10px] leading-4.5 font-semibold text-white"
          aria-hidden
        >
          {count > 99 ? '99+' : count}
        </span>
      )}
    </Link>
  )
}
