import {
  AlertTriangle,
  ArrowDownLeft,
  ArrowUpRight,
  CalendarClock,
  ChevronLeft,
  ChevronRight,
  Pencil,
  Plus,
  Target,
  Trash2,
} from 'lucide-react'
import { useState } from 'react'
import { toast } from 'sonner'
import { errorMessage } from '@/api/errors'
import type { EntryKind, MonthlySummary, Transaction } from '@/api/types'
import { useAuth } from '@/app/providers/auth-provider'
import { BarList } from '@/components/charts/bar-list'
import { PairedBarChart } from '@/components/charts/paired-bar-chart'
import { TrendLine } from '@/components/charts/trend-line'
import { ConfirmDialog } from '@/components/confirm-dialog'
import { EmptyState } from '@/components/empty-state'
import { ErrorState } from '@/components/error-state'
import { StatCard } from '@/components/stat-card'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Field } from '@/components/ui/field'
import { MoneyInput } from '@/components/ui/money-input'
import { ResponsiveDialog } from '@/components/ui/responsive-dialog'
import { Skeleton } from '@/components/ui/skeleton'
import { decimalToInput, isValidDecimal, parseDecimal } from '@/lib/decimal'
import {
  currentMonth,
  formatAmount,
  formatDate,
  formatMoney,
  formatMonth,
  formatPercent,
  pluralize,
  shiftMonth,
  todayIso,
} from '@/lib/format'
import { cn } from '@/lib/utils'
import {
  useCommitments,
  useDeleteSavingGoal,
  useDeleteTransaction,
  useFinanceOverview,
  useMonthlySummary,
  useSetSavingGoal,
  useSetTransactionStatus,
} from './ledger-api'
import { InstallmentDialog, type InstallmentDraft } from './installments-tab'
import { TransactionDialog } from './transaction-dialog'

export function MonthTab() {
  const { user } = useAuth()
  const primary = user?.settings.primaryCurrency ?? 'BRL'
  const [month, setMonth] = useState(currentMonth)
  const summary = useMonthlySummary(month)
  const overview = useFinanceOverview()
  const commitments = useCommitments()
  const [editing, setEditing] = useState<Transaction | { kind: EntryKind } | null>(null)
  const [goalOpen, setGoalOpen] = useState(false)
  const [installmentDraft, setInstallmentDraft] = useState<InstallmentDraft | null>(null)

  const isCurrent = month === currentMonth()

  return (
    <div className="grid gap-5">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div className="flex items-center gap-1">
          <Button
            variant="ghost"
            size="icon"
            aria-label="Mês anterior"
            onClick={() => setMonth((m) => shiftMonth(m, -1))}
          >
            <ChevronLeft />
          </Button>
          <h2
            className="min-w-44 text-center font-display text-xl font-semibold first-letter:uppercase"
            aria-live="polite"
          >
            {formatMonth(month)}
          </h2>
          <Button
            variant="ghost"
            size="icon"
            aria-label="Próximo mês"
            onClick={() => setMonth((m) => shiftMonth(m, 1))}
          >
            <ChevronRight />
          </Button>
          {!isCurrent && (
            <Button variant="link" size="sm" onClick={() => setMonth(currentMonth())}>
              Mês atual
            </Button>
          )}
        </div>
        <div className="flex gap-2">
          <Button variant="outline" onClick={() => setEditing({ kind: 'INCOME' })}>
            <ArrowDownLeft aria-hidden /> Receita
          </Button>
          <Button onClick={() => setEditing({ kind: 'EXPENSE' })}>
            <Plus aria-hidden /> Despesa
          </Button>
        </div>
      </div>

      {summary.isPending ? (
        <div className="grid grid-cols-2 gap-4 @4xl:grid-cols-4">
          {[0, 1, 2, 3].map((i) => (
            <Skeleton key={i} className="h-28 rounded-card" />
          ))}
        </div>
      ) : summary.isError ? (
        <Card>
          <ErrorState error={summary.error} onRetry={() => void summary.refetch()} />
        </Card>
      ) : (
        <SummaryCards s={summary.data} onEditGoal={() => setGoalOpen(true)} />
      )}

      {summary.data && summary.data.pendingCount > 0 && (
        <div role="status" className="flex items-center gap-3 rounded-xl bg-accent-soft px-4 py-3 text-sm">
          <AlertTriangle className="size-4 shrink-0 text-accent" aria-hidden />
          <span>
            {pluralize(
              summary.data.pendingCount,
              'lançamento previsto já passou',
              'lançamentos previstos já passaram',
            )}{' '}
            da data: confirme se aconteceram ou exclua.
          </span>
        </div>
      )}

      <div className="grid gap-5 @4xl:grid-cols-[1.4fr_1fr]">
        <Card>
          <CardHeader className="flex-col">
            <CardTitle>Receitas × despesas</CardTitle>
            <CardDescription>5 meses para trás e 6 à frente (previstos incluídos).</CardDescription>
          </CardHeader>
          <CardContent className="grid gap-6">
            {overview.isPending ? (
              <Skeleton className="h-52" />
            ) : overview.data && !overview.data.months.some((m) => m.income.amount || m.expenses.amount) ? (
              <p className="py-10 text-center text-sm text-muted">
                Sem receitas nem despesas neste período. O gráfico aparece com o primeiro lançamento.
              </p>
            ) : overview.data ? (
              <>
                <PairedBarChart
                  ariaLabel="Receitas e despesas por mês"
                  seriesA="Receitas"
                  seriesB="Despesas"
                  points={overview.data.months.map((m) => ({
                    key: m.month,
                    label: formatMonth(m.month, 'short').replace('.', ''),
                    title: formatMonth(m.month),
                    a: m.income.amount,
                    b: m.expenses.amount,
                    aLabel: formatMoney(m.income),
                    bLabel: formatMoney(m.expenses),
                    footer: `Sobra ${formatMoney(m.surplus)}`,
                    highlight: m.month === month,
                  }))}
                />
                <div>
                  <p className="mb-2 text-sm font-medium">Sobra acumulada no período</p>
                  <TrendLine
                    ariaLabel="Sobra acumulada mês a mês"
                    formatAxis={(v) => formatAmount(v, overview.data.currency, { compact: true })}
                    points={overview.data.months.map((m) => ({
                      key: m.month,
                      label: formatMonth(m.month, 'medium'),
                      title: formatMonth(m.month),
                      value: m.cumulativeSurplus.amount,
                      valueLabel: formatMoney(m.cumulativeSurplus),
                    }))}
                    height={120}
                  />
                </div>
              </>
            ) : null}
          </CardContent>
        </Card>

        <div className="grid content-start gap-5">
          <Card>
            <CardHeader className="flex-col">
              <CardTitle>Despesas por categoria</CardTitle>
              <CardDescription className="first-letter:uppercase">{formatMonth(month)}</CardDescription>
            </CardHeader>
            <CardContent>
              {summary.data && summary.data.expensesByCategory.length > 0 ? (
                <BarList
                  ariaLabel="Despesas por categoria"
                  rows={summary.data.expensesByCategory.map((c) => ({
                    key: c.category.id,
                    label: c.category.name,
                    dotColor: c.category.color,
                    value: c.total.amount,
                    valueLabel: formatMoney(c.total),
                    hint: String(c.count),
                  }))}
                />
              ) : (
                <p className="text-sm text-muted">Sem despesas neste mês.</p>
              )}
            </CardContent>
          </Card>
          <CommitmentsCard data={commitments.data} />
        </div>
      </div>

      <Card>
        <CardHeader className="flex-col">
          <CardTitle>Lançamentos</CardTitle>
          <CardDescription>Marque o círculo para confirmar o que aconteceu.</CardDescription>
        </CardHeader>
        {summary.data && summary.data.transactions.length === 0 ? (
          <EmptyState
            icon={CalendarClock}
            title="Nenhum lançamento neste mês"
            description="Registre receitas e despesas, ou crie parcelamentos e assinaturas para preencher os meses sozinhos."
          />
        ) : (
          <TransactionList items={summary.data?.transactions ?? []} onEdit={setEditing} />
        )}
      </Card>

      {editing !== null && (
        <TransactionDialog
          transaction={'id' in editing ? editing : undefined}
          defaultKind={'id' in editing ? undefined : editing.kind}
          defaultDate={isCurrent ? todayIso() : `${month}-01`}
          defaultCurrency={primary}
          onOpenChange={(o) => !o && setEditing(null)}
          onInstallment={(draft) => {
            setEditing(null)
            setInstallmentDraft(draft)
          }}
        />
      )}
      {installmentDraft && (
        <InstallmentDialog draft={installmentDraft} onClose={() => setInstallmentDraft(null)} />
      )}
      {goalOpen && summary.data && (
        <GoalDialog
          month={month}
          current={summary.data.savingGoal?.amount ?? null}
          currency={summary.data.currency}
          onClose={() => setGoalOpen(false)}
        />
      )}
    </div>
  )
}

function SummaryCards({ s, onEditGoal }: { s: MonthlySummary; onEditGoal: () => void }) {
  return (
    <div className="grid grid-cols-2 gap-4 @4xl:grid-cols-4">
      <StatCard
        label="Receitas"
        value={formatMoney(s.income.total)}
        icon={ArrowDownLeft}
        hint={s.income.planned.amount > 0 ? `${formatMoney(s.income.planned)} previsto` : 'tudo confirmado'}
      />
      <StatCard
        label="Despesas"
        value={formatMoney(s.expenses.total)}
        icon={ArrowUpRight}
        hint={`parcelas ${formatMoney(s.expenses.installments)} · fixas ${formatMoney(s.expenses.recurring)}`}
      />
      <StatCard
        label="Sobra prevista"
        value={formatMoney(s.projectedSurplus)}
        tone={s.projectedSurplus.amount < 0 ? 'danger' : 'primary'}
        hint={`realizada ${formatMoney(s.realizedSurplus)}`}
      />
      <button
        type="button"
        onClick={onEditGoal}
        className="rounded-card text-left focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring/40"
        title="Editar meta de economia do mês"
      >
        {/* o nome acessível vem do texto visível do cartão (WCAG 2.5.3) + esta dica */}
        <span className="sr-only">Editar: </span>
        <StatCard
          label="Meta do mês"
          icon={Target}
          tone="accent"
          className="h-full transition-colors hover:border-primary/40"
          value={
            s.savingGoal ? formatMoney(s.savingGoal) : <span className="text-base text-muted">Definir</span>
          }
          hint={
            s.goalRatio != null ? (
              <span className={cn(s.goalRatio >= 1 ? 'text-primary' : 'text-accent')}>
                {formatPercent(Math.round(s.goalRatio * 100))} da meta pela sobra prevista
              </span>
            ) : (
              'quanto quer guardar'
            )
          }
        />
      </button>
    </div>
  )
}

function TransactionList({ items, onEdit }: { items: Transaction[]; onEdit: (t: Transaction) => void }) {
  const setStatus = useSetTransactionStatus()
  const remove = useDeleteTransaction()
  const [deleting, setDeleting] = useState<Transaction | null>(null)
  const today = todayIso()

  return (
    <>
      <ul className="divide-y divide-border">
        {items.map((t) => {
          const confirmed = t.status === 'CONFIRMED'
          const overdue = !confirmed && t.occurredOn < today
          return (
            <li key={t.id} className="flex items-center gap-3 px-4 py-3 sm:px-5">
              <button
                type="button"
                role="checkbox"
                aria-checked={confirmed}
                aria-label={
                  confirmed ? `Voltar ${t.description} para previsto` : `Confirmar ${t.description}`
                }
                onClick={() =>
                  setStatus.mutate(
                    { id: t.id, status: confirmed ? 'PLANNED' : 'CONFIRMED' },
                    { onError: (e) => toast.error(errorMessage(e)) },
                  )
                }
                className={cn(
                  'grid size-7 shrink-0 place-items-center rounded-full border-2 transition-colors',
                  confirmed
                    ? 'border-primary bg-primary text-primary-foreground'
                    : overdue
                      ? 'border-accent'
                      : 'border-border',
                )}
              >
                {confirmed && <span className="size-2.5 rounded-full bg-primary-foreground" aria-hidden />}
              </button>
              <button type="button" onClick={() => onEdit(t)} className="min-w-0 flex-1 text-left">
                <p className={cn('truncate font-medium', !confirmed && 'text-muted')}>{t.description}</p>
                <p className="flex flex-wrap items-center gap-x-2 text-xs text-muted">
                  <span className="flex items-center gap-1">
                    <span
                      aria-hidden
                      className="size-2 rounded-full"
                      style={{ backgroundColor: t.category.color }}
                    />
                    {t.category.name}
                  </span>
                  <span>{formatDate(t.occurredOn)}</span>
                  {t.sourceLabel && <Badge>{t.sourceLabel}</Badge>}
                  {overdue && <Badge tone="accent">pendente</Badge>}
                </p>
              </button>
              <span className="shrink-0 text-right font-semibold tabular">
                <span className="sr-only">{t.kind === 'INCOME' ? 'Receita' : 'Despesa'} </span>
                {t.kind === 'INCOME' ? '+' : '−'} {formatMoney(t.amount)}
              </span>
              <span className="hidden shrink-0 sm:flex">
                <Button
                  variant="ghost"
                  size="icon"
                  aria-label={`Editar ${t.description}`}
                  onClick={() => onEdit(t)}
                >
                  <Pencil />
                </Button>
                {t.source !== 'INSTALLMENT' && (
                  <Button
                    variant="ghost"
                    size="icon"
                    aria-label={`Excluir ${t.description}`}
                    onClick={() => setDeleting(t)}
                  >
                    <Trash2 />
                  </Button>
                )}
              </span>
            </li>
          )
        })}
      </ul>
      <ConfirmDialog
        open={deleting !== null}
        onOpenChange={(o) => !o && setDeleting(null)}
        title="Excluir lançamento?"
        description={
          deleting?.source === 'RECURRING'
            ? 'Só esta ocorrência será removida (a recorrência continua).'
            : `"${deleting?.description}" será excluído.`
        }
        confirmLabel="Excluir"
        destructive
        loading={remove.isPending}
        onConfirm={() =>
          deleting &&
          remove.mutate(deleting.id, {
            onSuccess: () => setDeleting(null),
            onError: (e) => toast.error(errorMessage(e)),
          })
        }
      />
    </>
  )
}

function CommitmentsCard({ data }: { data: ReturnType<typeof useCommitments>['data'] }) {
  return (
    <Card>
      <CardHeader className="flex-col">
        <CardTitle>Comprometido futuro</CardTitle>
        <CardDescription>Parcelas e despesas fixas previstas.</CardDescription>
      </CardHeader>
      <CardContent>
        {!data ? (
          <Skeleton className="h-24" />
        ) : (
          <>
            <p className="text-sm text-muted">
              Próximos 12 meses{' '}
              <span className="font-display text-xl font-semibold tabular text-foreground">
                {formatMoney(data.total)}
              </span>
            </p>
            {data.upcoming.length > 0 ? (
              <ul className="mt-3 grid gap-2">
                {data.upcoming.slice(0, 6).map((t) => (
                  <li key={t.id} className="flex items-center justify-between gap-3 text-sm">
                    <span className="min-w-0">
                      <span className="block truncate">{t.description}</span>
                      <span className="text-xs text-muted">
                        {formatDate(t.occurredOn)}
                        {t.sourceLabel && ` · ${t.sourceLabel}`}
                      </span>
                    </span>
                    <span className="shrink-0 font-medium tabular">{formatMoney(t.amount)}</span>
                  </li>
                ))}
              </ul>
            ) : (
              <p className="mt-3 text-sm text-muted">Nada vencendo nos próximos 45 dias.</p>
            )}
          </>
        )}
      </CardContent>
    </Card>
  )
}

function GoalDialog({
  month,
  current,
  currency,
  onClose,
}: {
  month: string
  current: number | null
  currency: string
  onClose: () => void
}) {
  const set = useSetSavingGoal()
  const remove = useDeleteSavingGoal()
  const [value, setValue] = useState(decimalToInput(current))
  const [error, setError] = useState('')

  const submit = (e: React.FormEvent) => {
    e.preventDefault()
    if (!isValidDecimal(value)) {
      setError('Informe um valor válido.')
      return
    }
    set.mutate(
      { month, amount: parseDecimal(value) ?? 0, currency },
      { onSuccess: onClose, onError: (err) => setError(errorMessage(err)) },
    )
  }

  return (
    <ResponsiveDialog
      open
      onOpenChange={(o) => !o && onClose()}
      title="Meta de economia"
      description={`Quanto você quer guardar em ${formatMonth(month)}.`}
      footer={
        <>
          {current != null && (
            <Button
              variant="ghost"
              loading={remove.isPending}
              onClick={() => remove.mutate(month, { onSuccess: onClose })}
            >
              Remover meta
            </Button>
          )}
          <Button type="submit" form="goal-form" loading={set.isPending}>
            Salvar
          </Button>
        </>
      }
    >
      <form id="goal-form" onSubmit={submit} noValidate>
        <Field id="goal-amount" label="Valor" error={error}>
          <MoneyInput
            id="goal-amount"
            currency={currency}
            autoFocus
            value={value}
            onChange={(e) => setValue(e.target.value)}
          />
        </Field>
      </form>
    </ResponsiveDialog>
  )
}
