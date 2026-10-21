import { LogOut, Menu } from 'lucide-react'
import { useState } from 'react'
import { Link, NavLink, Outlet } from 'react-router'
import { useAuth } from '@/app/providers/auth-provider'
import { Brand } from '@/components/brand'
import { Badge } from '@/components/ui/badge'
import { Sheet, SheetContent, SheetTrigger } from '@/components/ui/sheet'
import { cn } from '@/lib/utils'
import { isAvailable, NAV_ITEMS, type NavItem } from './navigation'

function initials(name: string) {
  return name
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((p) => p[0]?.toUpperCase())
    .join('')
}

function SidebarLink({ item, onNavigate }: { item: NavItem; onNavigate?: () => void }) {
  const Icon = item.icon
  return (
    <NavLink
      to={item.to}
      end={item.to === '/'}
      onClick={onNavigate}
      className={({ isActive }) =>
        cn(
          'group flex h-11 items-center gap-3 rounded-xl px-3 text-sm transition-colors',
          isActive ? 'bg-primary-soft font-medium text-primary' : 'text-muted hover:bg-surface-2 hover:text-foreground',
        )
      }
    >
      <Icon className="size-[18px] shrink-0" aria-hidden />
      <span className="flex-1">{item.label}</span>
      {!isAvailable(item) && <Badge>Fase {item.phase}</Badge>}
    </NavLink>
  )
}

function UserFooter() {
  const { user, logout } = useAuth()
  if (!user) return null
  return (
    <div className="flex items-center gap-3 rounded-2xl bg-surface-2 p-3">
      <div className="grid size-9 place-items-center rounded-full bg-primary text-xs font-semibold text-primary-foreground" aria-hidden>
        {initials(user.displayName)}
      </div>
      <div className="min-w-0 flex-1">
        <p className="truncate text-sm font-medium">{user.displayName}</p>
        <p className="truncate text-xs text-muted">{user.email}</p>
      </div>
      <button
        type="button"
        onClick={() => void logout()}
        className="grid size-9 place-items-center rounded-lg text-muted hover:bg-surface hover:text-foreground"
        aria-label="Sair"
        title="Sair"
      >
        <LogOut className="size-4" />
      </button>
    </div>
  )
}

function DesktopSidebar() {
  const main = NAV_ITEMS.filter((i) => i.to !== '/settings')
  const settings = NAV_ITEMS.find((i) => i.to === '/settings')!
  return (
    <aside className="sticky top-0 hidden h-dvh w-68 shrink-0 flex-col border-r border-border bg-surface px-4 py-6 lg:flex">
      <Link to="/" className="mb-8 px-2" aria-label="Ir para o dashboard">
        <Brand />
      </Link>
      <nav aria-label="Principal" className="flex flex-1 flex-col gap-1 overflow-y-auto">
        {main.map((item) => (
          <SidebarLink key={item.to} item={item} />
        ))}
      </nav>
      <div className="mt-4 flex flex-col gap-3">
        <SidebarLink item={settings} />
        <UserFooter />
      </div>
    </aside>
  )
}

function MobileBottomNav() {
  const [open, setOpen] = useState(false)
  const primary = NAV_ITEMS.filter((i) => i.mobilePrimary)
  const others = NAV_ITEMS.filter((i) => !i.mobilePrimary)

  return (
    <nav
      aria-label="Principal"
      className="fixed inset-x-0 bottom-0 z-40 border-t border-border bg-surface/95 pb-[env(safe-area-inset-bottom)] backdrop-blur lg:hidden"
    >
      <ul className="mx-auto grid max-w-lg grid-cols-6">
        {primary.map((item) => {
          const Icon = item.icon
          return (
            <li key={item.to} className="col-span-1">
              <NavLink
                to={item.to}
                end={item.to === '/'}
                className={({ isActive }) =>
                  cn(
                    'flex h-16 flex-col items-center justify-center gap-1 text-[11px]',
                    isActive ? 'font-semibold text-primary' : 'text-muted',
                  )
                }
              >
                <Icon className="size-5" aria-hidden />
                {item.shortLabel ?? item.label}
              </NavLink>
            </li>
          )
        })}
        <li className="col-span-1">
          <Sheet open={open} onOpenChange={setOpen}>
            <SheetTrigger className="flex h-16 w-full flex-col items-center justify-center gap-1 text-[11px] text-muted">
              <Menu className="size-5" aria-hidden />
              Mais
            </SheetTrigger>
            <SheetContent title="Mais opções">
              <div className="flex flex-col gap-1">
                {others.map((item) => (
                  <SidebarLink key={item.to} item={item} onNavigate={() => setOpen(false)} />
                ))}
              </div>
              <div className="mt-4">
                <UserFooter />
              </div>
            </SheetContent>
          </Sheet>
        </li>
      </ul>
    </nav>
  )
}

export function AppShell() {
  return (
    <div className="flex min-h-dvh">
      <a
        href="#conteudo"
        className="sr-only z-50 rounded-lg bg-primary px-4 py-2 text-primary-foreground focus:not-sr-only focus:fixed focus:left-4 focus:top-4"
      >
        Pular para o conteúdo
      </a>
      <DesktopSidebar />
      <div className="flex min-w-0 flex-1 flex-col">
        <header className="sticky top-0 z-30 flex h-14 items-center border-b border-border bg-background/90 px-4 backdrop-blur lg:hidden">
          <Link to="/" aria-label="Ir para o dashboard">
            <Brand />
          </Link>
        </header>
        <main id="conteudo" className="mx-auto w-full max-w-6xl flex-1 px-4 pb-28 pt-6 sm:px-6 lg:px-10 lg:pb-12 lg:pt-10">
          <Outlet />
        </main>
      </div>
      <MobileBottomNav />
    </div>
  )
}
