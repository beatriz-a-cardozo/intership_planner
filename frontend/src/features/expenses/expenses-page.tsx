import { Filter, Plus, Receipt, Search, Target } from 'lucide-react'
import { useState } from 'react'
import { Link } from 'react-router'
import type { Expense, ExpenseGroupBy, ExpenseStatus, Objective } from '@/api/types'
import { SplitBarList } from '@/components/charts/split-bar-list'
import { SplitMeter } from '@/components/charts/split-meter'
import { CatalogIcon } from '@/components/catalog-icon'
import { EmptyState } from '@/components/empty-state'
import { ErrorState } from '@/components/error-state'
import { PageHeader } from '@/components/page-header'
import { StatCard } from '@/components/stat-card'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Field } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { SegmentedControl } from '@/components/ui/segmented'
import { NativeSelect } from '@/components/ui/select'
import { Sheet, SheetContent, SheetTrigger } from '@/components/ui/sheet'
import { Skeleton } from '@/components/ui/skeleton'
import { useObjectives } from '@/features/objectives/api'
import { useDebouncedValue } from '@/hooks/use-debounced-value'
import { formatDate, formatMoney, pluralize } from '@/lib/format'
import { RECURRENCE_LABELS, STATUS_LABELS, STATUS_TONES } from '@/lib/labels'
import { cn } from '@/lib/utils'
import { useCatalog, useExpenses, useExpenseSummary, type ExpenseFilters } from './api'
import { ExpenseDetailDialog } from './expense-detail-dialog'
import { ExpenseFormDialog } from './expense-form-dialog'

const GROUP_OPTIONS: { value: Exclude<ExpenseGroupBy, 'STATUS'>; label: string }[] = [
  { value: 'CYCLE', label: 'Ciclo' },
  { value: 'CATEGORY', label: 'Categoria' },
  { value: 'RECURRENCE', label: 'Periodicidade' },
]

export function ExpensesPage() {
  const objectives = useObjectives()
  const [selectedObjective, setSelectedObjective] = useState<string>()
  const objective =
    objectives.data?.find((o) => o.id === selectedObjective) ?? objectives.data?.find((o) => o.primary)

  if (objectives.isPending) return <Skeleton className="h-96 rounded-card" />
  if (objectives.isError) {
    return (
      <Card>
        <ErrorState error={objectives.error} onRetry={() => void objectives.refetch()} />
      </Card>
    )
  }
  if (!objective) {
    return (
      <>
        <PageHeader title="Gastos" />
        <Card>
          <EmptyState
            icon={Target}
            title="Crie um objetivo primeiro"
            description="Os gastos pertencem a um objetivo (ex.: mestrado na Alemanha)."
            action={
              <Button asChild>
                <Link to="/objectives">Ir para objetivos</Link>
              </Button>
            }
          />
        </Card>
      </>
    )
  }

  return (
    <ExpensesView
      key={objective.id}
      objective={objective}
      objectives={objectives.data}
      onObjectiveChange={setSelectedObjective}
    />
  )
}

function ExpensesView({
  objective,
  objectives,
  onObjectiveChange,
}: {
  objective: Objective
  objectives: Objective[]
  onObjectiveChange: (id: string) => void
}) {
  const [groupBy, setGroupBy] = useState<Exclude<ExpenseGroupBy, 'STATUS'>>('CYCLE')
  const [search, setSearch] = useState('')
  const [filters, setFilters] = useState<Omit<ExpenseFilters, 'objectiveId' | 'q'>>({})
  const [formFor, setFormFor] = useState<Expense | 'new' | null>(null)
  const [detailId, setDetailId] = useState<string | null>(null)
  const q = useDebouncedValue(search.trim())

  const summary = useExpenseSummary(objective.id, groupBy)
  const expenses = useExpenses({ objectiveId: objective.id, q: q || undefined, ...filters })
  const categories = useCatalog('categories')
  const activeFilters = Object.values(filters).filter((v) => v !== undefined).length

  // ordem dos grupos = ordem do resumo do backend (sort_order do catálogo / ordem dos enums)
  const groupOrder = summary.data?.groupBy === groupBy ? summary.data.groups.map((g) => g.key) : []
  const grouped = groupExpenses(expenses.data ?? [], groupBy, groupOrder)
  const totals = summary.data?.totals
  const paidShare = totals && totals.total.amount > 0 ? totals.paid.amount / totals.total.amount : 0

  const filterControls = (
    <div className="grid gap-4 sm:grid-cols-3">
      <Field id="filter-status" label="Status">
        <NativeSelect
          id="filter-status"
          value={filters.status ?? ''}
          onChange={(e) =>
            setFilters((f) => ({ ...f, status: (e.target.value || undefined) as ExpenseStatus | undefined }))
          }
        >
          <option value="">Todos</option>
          {(Object.keys(STATUS_LABELS) as ExpenseStatus[]).map((s) => (
            <option key={s} value={s}>
              {STATUS_LABELS[s]}
            </option>
          ))}
        </NativeSelect>
      </Field>
      <Field id="filter-required" label="Tipo">
        <NativeSelect
          id="filter-required"
          value={filters.required === undefined ? '' : String(filters.required)}
          onChange={(e) =>
            setFilters((f) => ({
              ...f,
              required: e.target.value === '' ? undefined : e.target.value === 'true',
            }))
          }
        >
          <option value="">Obrigatórios e opcionais</option>
          <option value="true">Só obrigatórios</option>
          <option value="false">Só opcionais</option>
        </NativeSelect>
      </Field>
      <Field id="filter-category" label="Categoria">
        <NativeSelect
          id="filter-category"
          value={filters.categoryId ?? ''}
          onChange={(e) => setFilters((f) => ({ ...f, categoryId: e.target.value || undefined }))}
        >
          <option value="">Todas</option>
          {categories.data?.map((c) => (
            <option key={c.id} value={c.id}>
              {c.name}
            </option>
          ))}
        </NativeSelect>
      </Field>
    </div>
  )

  return (
    <>
      <PageHeader
        title="Gastos"
        description="Tudo o que o objetivo custa, por ciclo e categoria, com pagamentos em BRL ou EUR."
        actions={
          <Button onClick={() => setFormFor('new')}>
            <Plus aria-hidden /> Novo gasto
          </Button>
        }
      />

      {objectives.length > 1 && (
        <div className="mb-5 max-w-sm">
          <Field id="objective-select" label="Objetivo">
            <NativeSelect
              id="objective-select"
              value={objective.id}
              onChange={(e) => onObjectiveChange(e.target.value)}
            >
              {objectives.map((o) => (
                <option key={o.id} value={o.id}>
                  {o.name}
                  {o.primary ? ' (principal)' : ''}
                </option>
              ))}
            </NativeSelect>
          </Field>
        </div>
      )}

      {/* Indicadores */}
      {summary.isPending ? (
        <div className="mb-5 grid grid-cols-2 gap-4 lg:grid-cols-4">
          {[0, 1, 2, 3].map((i) => (
            <Skeleton key={i} className="h-28 rounded-card" />
          ))}
        </div>
      ) : (
        totals && (
          <div className="mb-5 grid grid-cols-2 gap-4 lg:grid-cols-4">
            <StatCard
              label="Total"
              value={formatMoney(totals.total, { compact: totals.total.amount >= 1_000_000 })}
              hint={pluralize(totals.count, 'gasto', 'gastos')}
            />
            <StatCard
              label="Obrigatórios"
              value={formatMoney(totals.requiredAfterAdjustments)}
              tone="primary"
              hint={
                totals.adjustments.amount !== 0
                  ? `com ajustes (${formatMoney(totals.adjustments)})`
                  : 'sem ajustes'
              }
            />
            <StatCard label="Pago" value={formatMoney(totals.paid)} />
            <StatCard label="A pagar" value={formatMoney(totals.remaining)} tone="accent" />
          </div>
        )
      )}

      <div className="grid gap-5 lg:grid-cols-[1fr_1.5fr]">
        <Card className="lg:sticky lg:top-6 lg:self-start">
          <CardHeader className="flex-col gap-3">
            <CardTitle>Distribuição</CardTitle>
            <SegmentedControl
              ariaLabel="Agrupar por"
              value={groupBy}
              onValueChange={setGroupBy}
              options={GROUP_OPTIONS}
            />
          </CardHeader>
          <CardContent>
            {totals && totals.count > 0 ? (
              <div className="grid gap-6">
                <SplitMeter
                  share={paidShare}
                  filledLabel="Pago"
                  filledValue={formatMoney(totals.paid)}
                  restLabel="A pagar"
                  restValue={formatMoney(totals.remaining)}
                />
                <SplitBarList
                  ariaLabel={`Gastos por ${GROUP_OPTIONS.find((g) => g.value === groupBy)!.label.toLowerCase()}`}
                  rows={(summary.data?.groups ?? []).map((g) => ({
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
                {totals.monthlyRecurring.amount > 0 && (
                  <p className="text-sm text-muted">
                    Custo mensal recorrente:{' '}
                    <span className="font-medium text-foreground tabular">
                      {formatMoney(totals.monthlyRecurring)}
                    </span>
                  </p>
                )}
              </div>
            ) : (
              <p className="text-sm text-muted">Os gráficos aparecem quando houver gastos.</p>
            )}
          </CardContent>
        </Card>

        <div className="grid content-start gap-4">
          <div className="flex gap-2">
            <div className="relative flex-1">
              <Search
                className="pointer-events-none absolute left-3.5 top-1/2 size-4 -translate-y-1/2 text-muted"
                aria-hidden
              />
              <Input
                aria-label="Buscar gastos"
                placeholder="Buscar por nome ou observação"
                className="pl-10"
                value={search}
                onChange={(e) => setSearch(e.target.value)}
              />
            </div>
            {/* Filtros: gaveta no celular, linha no desktop */}
            <Sheet>
              <SheetTrigger asChild>
                <Button variant="outline" className="sm:hidden" aria-label="Filtros">
                  <Filter aria-hidden />
                  {activeFilters > 0 && <Badge tone="primary">{activeFilters}</Badge>}
                </Button>
              </SheetTrigger>
              <SheetContent title="Filtros">
                {filterControls}
                <Button variant="ghost" className="mt-4 w-full" onClick={() => setFilters({})}>
                  Limpar filtros
                </Button>
              </SheetContent>
            </Sheet>
          </div>
          <div className="hidden sm:block">{filterControls}</div>

          {expenses.isPending ? (
            <div className="grid gap-3">
              {[0, 1, 2].map((i) => (
                <Skeleton key={i} className="h-20 rounded-card" />
              ))}
            </div>
          ) : expenses.isError ? (
            <Card>
              <ErrorState error={expenses.error} onRetry={() => void expenses.refetch()} />
            </Card>
          ) : expenses.data.length === 0 ? (
            <Card>
              <EmptyState
                icon={Receipt}
                title={q || activeFilters ? 'Nenhum gasto encontrado' : 'Nenhum gasto ainda'}
                description={
                  q || activeFilters
                    ? 'Ajuste a busca ou os filtros.'
                    : 'Comece pelos grandes: passagem, visto, aluguel, seguro saúde.'
                }
                action={
                  !q && !activeFilters && <Button onClick={() => setFormFor('new')}>Adicionar gasto</Button>
                }
              />
            </Card>
          ) : (
            grouped.map((group) => (
              <section key={group.key} aria-labelledby={`group-${group.key}`}>
                <h2
                  id={`group-${group.key}`}
                  className="mb-2 px-1 text-xs font-medium uppercase tracking-[0.12em] text-muted"
                >
                  {group.label} · {group.items.length}
                </h2>
                <Card>
                  <ul className="divide-y divide-border">
                    {group.items.map((e) => (
                      <li key={e.id}>
                        <ExpenseRow expense={e} onOpen={() => setDetailId(e.id)} />
                      </li>
                    ))}
                  </ul>
                </Card>
              </section>
            ))
          )}
        </div>
      </div>

      {formFor !== null && (
        <ExpenseFormDialog
          objective={objective}
          expense={formFor === 'new' ? undefined : formFor}
          onOpenChange={(o) => !o && setFormFor(null)}
        />
      )}
      {detailId && (
        <ExpenseDetailDialog
          expenseId={detailId}
          onOpenChange={(o) => !o && setDetailId(null)}
          onEdit={(e) => {
            setDetailId(null)
            setFormFor(e)
          }}
        />
      )}
    </>
  )
}

function ExpenseRow({ expense: e, onOpen }: { expense: Expense; onOpen: () => void }) {
  const cancelled = e.status === 'CANCELLED'
  const share = e.total.amount > 0 ? Math.min(1, e.paid.amount / e.total.amount) : 0
  return (
    <button
      type="button"
      onClick={onOpen}
      className="flex w-full items-center gap-3 px-4 py-3.5 text-left transition-colors hover:bg-surface-2 focus-visible:bg-surface-2 focus-visible:outline-none sm:px-5"
    >
      <CatalogIcon icon={e.category.icon} color={e.category.color} size="sm" />
      <div className="min-w-0 flex-1">
        <p className={cn('truncate font-medium', cancelled && 'text-muted line-through')}>{e.name}</p>
        <p className="truncate text-xs text-muted">
          {e.category.name} · {RECURRENCE_LABELS[e.recurrence]}
          {e.occurrences > 1 && ` ×${e.occurrences}`}
          {e.quantity > 1 && ` · ${e.quantity} un.`}
          {e.dueDate && ` · ${formatDate(e.dueDate)}`}
          {!e.required && ' · opcional'}
        </p>
        {share > 0 && share < 1 && (
          <span className="mt-1.5 flex h-1 w-24 overflow-hidden rounded-full bg-surface-2" aria-hidden>
            <span className="bg-primary" style={{ width: `${share * 100}%` }} />
          </span>
        )}
      </div>
      <div className="shrink-0 text-right">
        <p className="font-semibold tabular">{formatMoney(e.total)}</p>
        {e.currency !== e.totalConverted.currency ? (
          <p className="text-xs text-muted tabular">≈ {formatMoney(e.totalConverted)}</p>
        ) : (
          e.status !== 'PENDING' && <Badge tone={STATUS_TONES[e.status]}>{STATUS_LABELS[e.status]}</Badge>
        )}
        {e.currency !== e.totalConverted.currency && e.status !== 'PENDING' && (
          <Badge tone={STATUS_TONES[e.status]} className="mt-0.5">
            {STATUS_LABELS[e.status]}
          </Badge>
        )}
      </div>
    </button>
  )
}

interface ExpenseGroupView {
  key: string
  label: string
  order: number
  items: Expense[]
}

/** Agrupamento só de apresentação (os totais vêm do backend no resumo). */
function groupExpenses(
  items: Expense[],
  by: Exclude<ExpenseGroupBy, 'STATUS'>,
  order: string[],
): ExpenseGroupView[] {
  const groups = new Map<string, ExpenseGroupView>()
  items.forEach((e, index) => {
    const key = by === 'CYCLE' ? (e.cycle?.id ?? 'none') : by === 'CATEGORY' ? e.category.id : e.recurrence
    const label =
      by === 'CYCLE'
        ? (e.cycle?.name ?? 'Sem ciclo')
        : by === 'CATEGORY'
          ? e.category.name
          : RECURRENCE_LABELS[e.recurrence]
    const known = order.indexOf(key)
    const position = known >= 0 ? known : order.length + index
    const g = groups.get(key) ?? { key, label, order: position, items: [] }
    g.items.push(e)
    groups.set(key, g)
  })
  return [...groups.values()].sort((a, b) => a.order - b.order)
}
