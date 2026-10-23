import { Info, TrendingUp } from 'lucide-react'
import { Link } from 'react-router'
import { TrendLine } from '@/components/charts/trend-line'
import { ErrorState } from '@/components/error-state'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Skeleton } from '@/components/ui/skeleton'
import { useProjection } from '@/features/finance/ledger-api'
import { formatAmount, formatMoney, formatMonth } from '@/lib/format'

/** Previsão de atingimento com base no controle financeiro (média das sobras + parcelas e recorrências). */
export function ProjectionCard({ objectiveId }: { objectiveId: string }) {
  const projection = useProjection(objectiveId)

  return (
    <Card>
      <CardHeader className="flex-col">
        <CardTitle>Previsão</CardTitle>
        <CardDescription>Com base no seu controle financeiro. Estimativa, não garantia.</CardDescription>
      </CardHeader>
      <CardContent>
        {projection.isPending ? (
          <Skeleton className="h-48" />
        ) : projection.isError ? (
          <ErrorState error={projection.error} onRetry={() => void projection.refetch()} />
        ) : (
          (() => {
            const p = projection.data
            return (
              <div className="grid gap-4">
                <div className="flex items-start gap-3">
                  <span
                    className="grid size-10 shrink-0 place-items-center rounded-xl bg-primary-soft text-primary"
                    aria-hidden
                  >
                    <TrendingUp className="size-5" />
                  </span>
                  <div>
                    {p.reachesByTarget ? (
                      <p className="font-semibold">Meta alcançada em {formatMonth(p.reachMonth)}</p>
                    ) : (
                      <p className="font-semibold">
                        Faltariam <span className="tabular">{formatMoney(p.deficitAtTarget)}</span> na data
                        alvo
                      </p>
                    )}
                    <p className="text-sm text-muted">
                      {p.reachesByTarget
                        ? 'Antes ou na data alvo, mantendo o ritmo atual.'
                        : p.reachMonth
                          ? `No ritmo atual, alcança em ${formatMonth(p.reachMonth)}.`
                          : 'No ritmo atual, não alcança nos próximos 3 anos.'}
                    </p>
                  </div>
                </div>
                {p.months.length > 1 && (
                  <TrendLine
                    ariaLabel="Valor acumulado projetado mês a mês"
                    reference={{
                      value: p.target.amount,
                      label: `Meta ${formatMoney(p.target, { compact: true })}`,
                    }}
                    markKey={p.reachMonth}
                    formatAxis={(v) => formatAmount(v, p.currency, { compact: true })}
                    points={p.months.map((m) => ({
                      key: m.month,
                      label: formatMonth(m.month, 'medium'),
                      title: formatMonth(m.month),
                      value: m.cumulative.amount,
                      valueLabel: formatMoney(m.cumulative),
                    }))}
                  />
                )}
                <p className="flex items-start gap-2 text-xs text-muted">
                  <Info className="mt-0.5 size-3.5 shrink-0" aria-hidden />
                  <span>
                    {p.hasHistory
                      ? `Sobra média dos lançamentos avulsos (${p.basisMonths.map((m) => formatMonth(m, 'short')).join(', ')}): ${formatMoney(p.baselineMonthlySaving)}/mês, mais parcelas e recorrências programadas.`
                      : 'Sem lançamentos confirmados nos últimos meses: a previsão usa só parcelas e recorrências. '}
                    {!p.hasHistory && (
                      <Link to="/finance" className="font-medium text-primary hover:underline">
                        Registrar no controle financeiro
                      </Link>
                    )}
                  </span>
                </p>
              </div>
            )
          })()
        )}
      </CardContent>
    </Card>
  )
}
