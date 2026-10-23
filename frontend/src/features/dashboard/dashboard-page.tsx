import {
  ArrowRight,
  CalendarClock,
  GraduationCap,
  MapPin,
  PiggyBank,
  Receipt,
  Target,
  TrendingUp,
  Wallet,
} from 'lucide-react'
import { useState } from 'react'
import { Link } from 'react-router'
import type { ApplicationsOverview, Dashboard, ExchangeRate, TimelineItem } from '@/api/types'
import { useAuth } from '@/app/providers/auth-provider'
import { RateSparkline } from '@/components/charts/rate-sparkline'
import { SplitBarList } from '@/components/charts/split-bar-list'
import { SplitMeter } from '@/components/charts/split-meter'
import { EmptyState } from '@/components/empty-state'
import { ErrorState } from '@/components/error-state'
import { PageHeader } from '@/components/page-header'
import { ProgressRing } from '@/components/progress-ring'
import { RateBadge } from '@/components/rate-badge'
import { StatCard } from '@/components/stat-card'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Skeleton } from '@/components/ui/skeleton'
import { useExchangeHistory } from '@/features/finance/api'
import { SavingsDialog } from '@/features/finance/savings-dialog'
import { useProjection } from '@/features/finance/ledger-api'
import { useDashboard } from '@/features/objectives/api'
import { ScopeGoalsCard } from '@/features/objectives/scope-goals-card'
import { TimelineIcon } from '@/features/timeline/timeline-icon'
import { itemLink } from '@/features/timeline/timeline-item'
import { DestinationsCard } from './destinations-card'
import { formatDate, formatMoney, formatMonth, formatRate, pluralize } from '@/lib/format'
import { countryName } from '@/lib/labels'

function greeting(date = new Date()) {
  const h = date.getHours()
  if (h < 12) return 'Bom dia'
  if (h < 18) return 'Boa tarde'
  return 'Boa noite'
}

export function DashboardPage() {
  const { user } = useAuth()
  const dashboard = useDashboard()
  const firstName = user?.displayName.split(' ')[0] ?? ''

  return (
    <>
      <PageHeader
        eyebrow={new Intl.DateTimeFormat('pt-BR', { dateStyle: 'full' }).format(new Date())}
        title={`${greeting()}, ${firstName}`}
      />
      {dashboard.isPending ? (
        <DashboardSkeleton />
      ) : dashboard.isError ? (
        <Card>
          <ErrorState error={dashboard.error} onRetry={() => void dashboard.refetch()} />
        </Card>
      ) : (
        <DashboardContent data={dashboard.data} />
      )}
    </>
  )
}

function DashboardContent({ data }: { data: Dashboard }) {
  const { objective, progress, expenses, exchangeRate } = data
  const [saving, setSaving] = useState(false)

  if (!objective || !progress) {
    return (
      <div className="grid gap-5">
        <Card>
          <EmptyState
            icon={Target}
            title="Nenhum objetivo principal"
            description="Crie um objetivo para acompanhar meta, progresso e quanto guardar por mês."
            action={
              <Button asChild>
                <Link to="/objectives">Criar objetivo</Link>
              </Button>
            }
          />
        </Card>
        <div className="grid gap-5 @4xl:grid-cols-2">
          <BalancesCard data={data} />
          <ExchangeCard rate={exchangeRate} />
        </div>
      </div>
    )
  }

  const place = [objective.city, countryName(objective.countryCode)].filter(Boolean).join(', ')
  // Meta derivada ainda sem gastos: "100%, faltam R$ 0" seria enganoso
  const emptyTarget = progress.targetMode === 'DERIVED' && progress.target.amount === 0
  const paidShare =
    expenses && expenses.totals.total.amount > 0
      ? expenses.totals.paid.amount / expenses.totals.total.amount
      : 0

  return (
    <div className="grid gap-5">
      {saving && (
        <SavingsDialog
          objectiveId={objective.id}
          objectiveCurrency={progress.currency}
          onClose={() => setSaving(false)}
        />
      )}
      {/* Hero do objetivo */}
      <Card className="overflow-hidden">
        <div className="grid gap-6 p-5 sm:p-7 @2xl:grid-cols-[auto_1fr] @2xl:items-center @2xl:gap-10">
          <ProgressRing percent={emptyTarget ? 0 : progress.percentComplete} className="mx-auto @2xl:mx-0" />
          <div className="min-w-0">
            <div className="flex flex-wrap items-center gap-2">
              <Link to={`/objectives/${objective.id}`} className="font-semibold hover:underline">
                {objective.name}
              </Link>
              {place && (
                <span className="flex items-center gap-1 text-sm text-muted">
                  <MapPin className="size-3.5" aria-hidden /> {place}
                </span>
              )}
            </div>
            {emptyTarget ? (
              <>
                <p className="mt-4 font-display text-3xl font-semibold tracking-tight">
                  Sua meta ainda está vazia
                </p>
                <p className="mt-2 text-sm text-muted">
                  A meta é a soma dos gastos dos ciclos Preparação e Imigração (ajustável em Configurações).
                  Cadastre os principais (passagem, visto, seguro) para ver quanto falta.
                </p>
                <Button asChild size="sm" className="mt-4">
                  <Link to="/expenses">Cadastrar gastos</Link>
                </Button>
              </>
            ) : (
              <>
                <p className="mt-4 text-sm text-muted">Faltam</p>
                <p className="font-display text-4xl font-semibold tabular tracking-tight sm:text-5xl">
                  {formatMoney(progress.remaining)}
                </p>
                <p className="mt-2 text-sm text-muted">
                  {progress.overdue ? (
                    <span className="text-danger">A data alvo já passou.</span>
                  ) : (
                    <>
                      Guardando{' '}
                      <span className="font-medium text-foreground tabular">
                        {formatMoney(progress.monthlySavingNeeded)}
                      </span>{' '}
                      por mês até {formatDate(progress.targetDate, 'long')} (
                      {pluralize(progress.monthsRemaining, 'mês', 'meses')})
                    </>
                  )}
                </p>
                {progress.onTrack != null && progress.plannedMonthlySaving && (
                  <Badge tone={progress.onTrack ? 'primary' : 'accent'} className="mt-3">
                    {progress.onTrack ? 'No ritmo' : 'Abaixo do ritmo'}: você planejou{' '}
                    {formatMoney(progress.plannedMonthlySaving)}/mês
                  </Badge>
                )}
                <ProjectionLine objectiveId={objective.id} />
              </>
            )}
            <Button size="sm" variant="outline" className="mt-4" onClick={() => setSaving(true)}>
              <PiggyBank aria-hidden /> Guardar dinheiro
            </Button>
          </div>
        </div>
        <div className="grid grid-cols-2 border-t border-border sm:grid-cols-4">
          <HeroStat
            label="Meta"
            value={formatMoney(progress.target)}
            hint={progress.targetMode === 'MANUAL' ? 'valor fixo' : 'pelos ciclos na meta'}
          />
          <HeroStat label="Mobilizado" value={formatMoney(progress.mobilized)} hint="pago + contas" />
          <HeroStat label="Já pago" value={formatMoney(progress.paid)} />
          <HeroStat label="Em contas" value={formatMoney(progress.accountsBalance)} />
        </div>
      </Card>

      {expenses && expenses.totals.count > 0 && <ScopeGoalsCard progress={progress} />}

      <DestinationsCard />

      <div className="grid gap-5 @4xl:grid-cols-[1.4fr_1fr]">
        {/* Gastos */}
        <Card>
          <CardHeader>
            <div>
              <CardTitle>Gastos planejados</CardTitle>
              <CardDescription>Por ciclo, na moeda do objetivo.</CardDescription>
            </div>
            <Button asChild variant="ghost" size="sm">
              <Link to="/expenses">
                Ver gastos <ArrowRight aria-hidden />
              </Link>
            </Button>
          </CardHeader>
          <CardContent>
            {!expenses || expenses.totals.count === 0 ? (
              <EmptyState
                icon={Receipt}
                title="Nenhum gasto ainda"
                description="Cadastre passagem, visto, aluguel... e veja a meta se formar sozinha."
                className="py-6"
                action={
                  <Button asChild size="sm">
                    <Link to="/expenses">Adicionar gasto</Link>
                  </Button>
                }
              />
            ) : (
              <div className="grid gap-6">
                <div>
                  <p className="text-sm text-muted">
                    Total{' '}
                    <span className="font-medium text-foreground tabular">
                      {formatMoney(expenses.totals.total)}
                    </span>
                    {expenses.totals.adjustments.amount !== 0 && (
                      <>
                        {' '}
                        · com ajustes{' '}
                        <span className="font-medium text-foreground tabular">
                          {formatMoney(expenses.totals.totalAfterAdjustments)}
                        </span>
                      </>
                    )}
                  </p>
                  <SplitMeter
                    className="mt-3"
                    share={paidShare}
                    filledLabel="Pago"
                    filledValue={formatMoney(expenses.totals.paid)}
                    restLabel="A pagar"
                    restValue={formatMoney(expenses.totals.remaining)}
                  />
                </div>
                <SplitBarList
                  ariaLabel="Gastos por ciclo"
                  rows={expenses.groups.map((g) => ({
                    key: g.key,
                    label: g.label,
                    dotColor: g.color,
                    total: g.total.amount,
                    paid: g.paid.amount,
                    totalLabel: formatMoney(g.total),
                    paidLabel: formatMoney(g.paid),
                    remainingLabel: formatMoney(g.remaining),
                    count: g.count,
                  }))}
                />
              </div>
            )}
          </CardContent>
        </Card>

        <div className="grid content-start gap-5">
          <UpcomingCard items={data.upcoming} />
          <BalancesCard data={data} />
          <ApplicationsCard overview={data.applications} />
          <ExchangeCard rate={exchangeRate} />
        </div>
      </div>

      {expenses && expenses.totals.monthlyRecurring.amount > 0 && (
        <div className="grid gap-4 sm:grid-cols-3">
          <StatCard
            label="Custo mensal recorrente"
            value={formatMoney(expenses.totals.monthlyRecurring)}
            icon={CalendarClock}
            hint="Aluguel, alimentação e outros gastos mensais"
          />
        </div>
      )}
    </div>
  )
}

/** Próximos itens da timeline: o que está atrasado primeiro, depois o que vence nos próximos 60 dias. */
function UpcomingCard({ items }: { items: TimelineItem[] }) {
  return (
    <Card>
      <CardHeader>
        <div>
          <CardTitle>Próximos</CardTitle>
          <CardDescription>
            Gastos com data, prazos das universidades e tarefas dos próximos 60 dias.
          </CardDescription>
        </div>
        <Button asChild variant="ghost" size="sm">
          <Link to="/timeline">
            Timeline <ArrowRight aria-hidden />
          </Link>
        </Button>
      </CardHeader>
      <CardContent>
        {items.length === 0 ? (
          <p className="text-sm text-muted">Nada pendente nos próximos 60 dias.</p>
        ) : (
          <ul className="grid gap-3">
            {items.map((item) => {
              const overdue = item.status === 'OVERDUE'
              const content = (
                <>
                  <span
                    className={
                      overdue
                        ? 'grid size-8 shrink-0 place-items-center rounded-lg bg-danger-soft text-danger'
                        : 'grid size-8 shrink-0 place-items-center rounded-lg bg-primary-soft text-primary'
                    }
                    aria-hidden
                  >
                    <TimelineIcon item={item} className="size-4" />
                  </span>
                  <span className="min-w-0 flex-1">
                    <span className="block truncate text-sm font-medium">{item.title}</span>
                    <span className={overdue ? 'text-xs font-medium text-danger' : 'text-xs text-muted'}>
                      {overdue ? 'Atrasado · ' : ''}
                      {formatDate(item.date)}
                      {item.subtitle && ` · ${item.subtitle}`}
                    </span>
                  </span>
                  {item.amount && (
                    <span className="shrink-0 text-sm tabular">{formatMoney(item.amount)}</span>
                  )}
                </>
              )
              const link = itemLink(item) ?? '/timeline'
              return (
                <li key={item.id}>
                  <Link to={link} className="flex items-center gap-3 rounded-lg hover:bg-surface-2/60">
                    {content}
                  </Link>
                </li>
              )
            })}
          </ul>
        )}
      </CardContent>
    </Card>
  )
}

function HeroStat({ label, value, hint }: { label: string; value: string; hint?: string }) {
  return (
    <div className="border-border p-4 odd:border-r sm:border-r sm:last:border-r-0 sm:p-5">
      <p className="text-xs uppercase tracking-[0.1em] text-muted">{label}</p>
      <p className="mt-1 font-semibold tabular">{value}</p>
      {hint && <p className="text-xs text-subtle">{hint}</p>}
    </div>
  )
}

function BalancesCard({ data }: { data: Dashboard }) {
  const { accounts } = data
  return (
    <Card>
      <CardHeader>
        <div>
          <CardTitle>Saldos</CardTitle>
          <CardDescription>
            {pluralize(accounts.accountCount, 'conta ativa', 'contas ativas')}
          </CardDescription>
        </div>
        <Button asChild variant="ghost" size="sm">
          <Link to="/finance">
            Contas <ArrowRight aria-hidden />
          </Link>
        </Button>
      </CardHeader>
      <CardContent>
        {accounts.accountCount === 0 ? (
          <div className="flex items-center gap-3 text-sm text-muted">
            <Wallet className="size-5" aria-hidden /> Cadastre suas contas em BRL e EUR para somar ao
            progresso.
          </div>
        ) : (
          <>
            <ul className="grid gap-2">
              {accounts.byCurrency.map((m) => (
                <li key={m.currency} className="flex items-center justify-between text-sm">
                  <span className="text-muted">{m.currency}</span>
                  <span className="font-medium tabular">{formatMoney(m)}</span>
                </li>
              ))}
            </ul>
            <div className="mt-3 flex items-center justify-between border-t border-border pt-3">
              <span className="flex items-center gap-2 text-sm text-muted">
                <PiggyBank className="size-4" aria-hidden /> Total convertido
              </span>
              <span className="font-display text-xl font-semibold tabular">
                ≈ {formatMoney(accounts.total)}
              </span>
            </div>
          </>
        )}
      </CardContent>
    </Card>
  )
}

function ExchangeCard({ rate }: { rate: ExchangeRate }) {
  const history = useExchangeHistory(rate.base, rate.quote)
  return (
    <Card>
      <CardHeader>
        <div>
          <CardTitle>Câmbio</CardTitle>
          <p className="mt-1 font-display text-2xl font-semibold tabular">€ 1 = R$ {formatRate(rate.rate)}</p>
        </div>
        <RateBadge rate={rate} />
      </CardHeader>
      <CardContent>
        {rate.manual && rate.marketRate != null && (
          <p className="mb-3 text-xs text-muted">Mercado agora: R$ {formatRate(rate.marketRate)}</p>
        )}
        {history.isPending ? (
          <Skeleton className="h-[90px]" />
        ) : history.data && history.data.length > 1 ? (
          <RateSparkline points={history.data} label="Euro em reais, últimos 90 dias" />
        ) : (
          <p className="text-sm text-muted">O histórico aparece depois da primeira atualização.</p>
        )}
      </CardContent>
    </Card>
  )
}

function DashboardSkeleton() {
  return (
    <div className="grid gap-5" aria-busy>
      <Skeleton className="h-72 rounded-card" />
      <div className="grid gap-5 @4xl:grid-cols-[1.4fr_1fr]">
        <Skeleton className="h-80 rounded-card" />
        <Skeleton className="h-80 rounded-card" />
      </div>
    </div>
  )
}

/** Uma linha com a previsão de atingimento (Fase 4); some se não houver dados suficientes. */
function ProjectionLine({ objectiveId }: { objectiveId: string }) {
  const projection = useProjection(objectiveId)
  const p = projection.data
  if (!p) return null
  return (
    <p className="mt-3 flex items-center gap-1.5 text-sm text-muted">
      <TrendingUp className="size-4 shrink-0" aria-hidden />
      {p.reachesByTarget ? (
        <span>
          Previsão: meta alcançada em{' '}
          <span className="font-medium text-foreground">{formatMonth(p.reachMonth)}</span>
        </span>
      ) : (
        <span>
          Previsão: faltariam{' '}
          <span className="font-medium tabular text-foreground">{formatMoney(p.deficitAtTarget)}</span> na
          data alvo
        </span>
      )}
    </p>
  )
}

const UPCOMING_LABEL: Record<ApplicationsOverview['upcoming'][number]['kind'], string> = {
  DEADLINE: 'Prazo de inscrição',
  RESPONSE: 'Resposta provável',
  REQUEST: 'Prazo de solicitação',
}

/** Candidaturas ativas (Fase 5): contagem por resultado e próximos marcos. */
function ApplicationsCard({ overview }: { overview: ApplicationsOverview }) {
  return (
    <Card>
      <CardHeader>
        <div>
          <CardTitle>Candidaturas</CardTitle>
          <CardDescription>{pluralize(overview.active, 'ativa', 'ativas')}</CardDescription>
        </div>
        <Button asChild variant="ghost" size="sm">
          <Link to="/applications">
            Ver <ArrowRight aria-hidden />
          </Link>
        </Button>
      </CardHeader>
      <CardContent>
        {overview.active === 0 ? (
          <div className="flex items-center gap-3 text-sm text-muted">
            <GraduationCap className="size-5" aria-hidden /> Marque programas como &quot;Escolhida&quot; em
            Universidades.
          </div>
        ) : (
          <>
            <div className="flex flex-wrap gap-2">
              {overview.pending > 0 && <Badge>{overview.pending} em andamento</Badge>}
              {overview.accepted > 0 && <Badge tone="primary">{overview.accepted} aprovada(s)</Badge>}
              {overview.waitlisted > 0 && <Badge tone="accent">{overview.waitlisted} em espera</Badge>}
              {overview.rejected > 0 && <Badge tone="danger">{overview.rejected} não aceita(s)</Badge>}
            </div>
            {overview.upcoming.length > 0 && (
              <ul className="mt-4 grid gap-2.5">
                {overview.upcoming.map((u) => (
                  <li
                    key={`${u.applicationId}-${u.kind}`}
                    className="flex items-start justify-between gap-3 text-sm"
                  >
                    <span className="min-w-0">
                      <span className="block truncate">{u.programName}</span>
                      <span className="block truncate text-xs text-muted">
                        {UPCOMING_LABEL[u.kind]} · {u.universityName}
                      </span>
                    </span>
                    <span className="shrink-0 tabular">{formatDate(u.date)}</span>
                  </li>
                ))}
              </ul>
            )}
          </>
        )}
      </CardContent>
    </Card>
  )
}
