import { Check, Circle } from 'lucide-react'
import { useAuth } from '@/app/providers/auth-provider'
import { PageHeader } from '@/components/page-header'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { CURRENT_PHASE, NAV_ITEMS } from '@/layouts/navigation'
import { cn } from '@/lib/utils'

function greeting(date = new Date()) {
  const h = date.getHours()
  if (h < 12) return 'Bom dia'
  if (h < 18) return 'Boa tarde'
  return 'Boa noite'
}

export function DashboardPage() {
  const { user } = useAuth()
  const firstName = user?.displayName.split(' ')[0] ?? ''
  const modules = NAV_ITEMS.filter((i) => i.to !== '/' && i.to !== '/settings')

  return (
    <>
      <PageHeader
        eyebrow={new Intl.DateTimeFormat('pt-BR', { dateStyle: 'full' }).format(new Date())}
        title={`${greeting()}, ${firstName}`}
        description="Seu painel vai reunir objetivo, finanças, prazos e universidades. Os módulos são liberados fase a fase."
      />

      <Card>
        <CardHeader className="flex-col">
          <CardTitle>Roteiro do produto</CardTitle>
          <CardDescription>O que já está disponível e o que vem a seguir.</CardDescription>
        </CardHeader>
        <CardContent>
          <ol className="relative grid gap-0 border-l-2 border-primary/30 pl-6">
            {modules.map((m) => {
              const done = m.phase <= CURRENT_PHASE
              return (
                <li key={m.to} className="relative pb-6 last:pb-0">
                  <span
                    className={cn(
                      'absolute -left-[33px] top-0.5 grid size-4 place-items-center rounded-full ring-4 ring-surface',
                      done ? 'bg-primary text-primary-foreground' : 'bg-surface-2 text-subtle',
                    )}
                    aria-hidden
                  >
                    {done ? <Check className="size-3" /> : <Circle className="size-2 fill-current" />}
                  </span>
                  <p className="text-xs font-medium uppercase tracking-[0.12em] text-muted">Fase {m.phase}</p>
                  <p className="font-medium">{m.label}</p>
                  <p className="text-sm text-muted">{m.description}</p>
                </li>
              )
            })}
          </ol>
        </CardContent>
      </Card>
    </>
  )
}
