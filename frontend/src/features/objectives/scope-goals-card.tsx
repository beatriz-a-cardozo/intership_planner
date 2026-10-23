import type { ObjectiveProgress, ScopeGoal } from '@/api/types'
import { Badge } from '@/components/ui/badge'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { formatMoney, formatPercent } from '@/lib/format'

const COPY: Record<ScopeGoal['scope'], { title: string; hint: string }> = {
  REQUIRED_ONLY: { title: 'Meta obrigatória', hint: 'Só os gastos obrigatórios' },
  ALL: { title: 'Meta completa', hint: 'Obrigatórios + opcionais' },
}

/**
 * As duas metas derivadas lado a lado: o mínimo para ir (obrigatórios) e o conforto (todos os gastos).
 * Os ajustes manuais (ex.: Sperrkonto) entram nas duas. Valores e percentuais vêm prontos da API.
 */
export function ScopeGoalsCard({ progress }: { progress: ObjectiveProgress }) {
  const goals = progress.scopeGoals
  const derived = progress.targetMode === 'DERIVED'
  return (
    <Card>
      <CardHeader className="flex-col">
        <CardTitle>Metas pelos gastos</CardTitle>
        <CardDescription>
          {derived
            ? 'Só os ciclos que entram na meta (padrão: Preparação e Imigração): o obrigatório e com os opcionais.'
            : 'Sua meta é um valor fixo; estas são as referências calculadas pelos gastos.'}
        </CardDescription>
      </CardHeader>
      <CardContent className="grid gap-6 sm:grid-cols-2">
        {goals.map((g) => (
          <GoalColumn key={g.scope} goal={g} active={derived && g.configured} overdue={progress.overdue} />
        ))}
      </CardContent>
    </Card>
  )
}

function GoalColumn({ goal: g, active, overdue }: { goal: ScopeGoal; active: boolean; overdue: boolean }) {
  const copy = COPY[g.scope]
  const empty = g.target.amount === 0
  const percent = Math.min(100, Math.max(0, g.percentComplete))
  return (
    <section aria-label={copy.title} className="grid content-start gap-2">
      <div className="flex flex-wrap items-center gap-2">
        <h3 className="text-sm font-semibold">{copy.title}</h3>
        {active && <Badge tone="primary">usada como meta</Badge>}
      </div>
      <p className="text-xs text-muted">{copy.hint}</p>
      <p className="font-display text-2xl font-semibold tabular tracking-tight">{formatMoney(g.target)}</p>
      {empty ? (
        <p className="text-sm text-muted">Sem gastos neste escopo ainda.</p>
      ) : (
        <>
          <div
            role="progressbar"
            aria-label={`Progresso da ${copy.title.toLowerCase()}`}
            aria-valuemin={0}
            aria-valuemax={100}
            aria-valuenow={percent}
            className="h-2 overflow-hidden rounded-full bg-surface-2"
          >
            <div className="h-full rounded-full bg-primary" style={{ width: `${percent}%` }} />
          </div>
          <p className="text-sm">
            <span className="font-medium tabular">{formatPercent(g.percentComplete, 0)}</span>
            <span className="text-muted">
              {' '}
              · faltam <span className="tabular text-foreground">{formatMoney(g.remaining)}</span>
            </span>
          </p>
          {!overdue && g.remaining.amount > 0 && (
            <p className="text-xs text-muted">
              <span className="tabular text-foreground">{formatMoney(g.monthlySavingNeeded)}</span> por mês
              até a data alvo
            </p>
          )}
        </>
      )}
    </section>
  )
}
