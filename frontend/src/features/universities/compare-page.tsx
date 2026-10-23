import { ArrowLeft, GitCompareArrows, Star } from 'lucide-react'
import type { ReactNode } from 'react'
import { Link, useSearchParams } from 'react-router'
import type { Program } from '@/api/types'
import { EmptyState } from '@/components/empty-state'
import { ErrorState } from '@/components/error-state'
import { PageHeader } from '@/components/page-header'
import { Card } from '@/components/ui/card'
import { Skeleton } from '@/components/ui/skeleton'
import { formatDate, formatMoney } from '@/lib/format'
import {
  APPLICATION_STATUS_LABELS,
  countryName,
  EMPLOYABILITY_LABELS,
  RATING_LABELS,
  REQUIREMENT_LABELS,
} from '@/lib/labels'
import { cn } from '@/lib/utils'
import { useCompare } from './api'

interface Row {
  label: string
  /** Chave em `highlights` (melhor valor da linha, calculado no backend). */
  highlight?: string
  render: (p: Program) => ReactNode
}

const ROWS: Row[] = [
  {
    label: 'Cidade',
    render: (p) =>
      [p.university.city, countryName(p.university.countryCode)].filter(Boolean).join(', ') || '—',
  },
  {
    label: 'Ranking no país',
    highlight: 'ranking',
    render: (p) => (p.university.ranking != null ? `#${p.university.ranking}` : '—'),
  },
  { label: 'Próximo prazo', highlight: 'nextDeadline', render: (p) => formatDate(p.nextDeadline) },
  {
    label: 'Taxa semestral',
    highlight: 'semesterFee',
    render: (p) => (p.semesterFee ? formatMoney(p.semesterFee) : '—'),
  },
  {
    label: 'Taxa de estrangeiro',
    highlight: 'nonEuTuition',
    render: (p) => (p.nonEuTuition ? formatMoney(p.nonEuTuition) : 'Não cobra'),
  },
  {
    label: 'Custo de vida / mês',
    highlight: 'costOfLiving',
    render: (p) => (p.university.costOfLiving ? formatMoney(p.university.costOfLiving) : '—'),
  },
  {
    label: 'Chance de admissão',
    highlight: 'admissionChance',
    render: (p) => (p.admissionChance ? RATING_LABELS[p.admissionChance] : '—'),
  },
  {
    label: 'Chance de bolsa',
    highlight: 'scholarshipChance',
    render: (p) => (p.scholarshipChance ? RATING_LABELS[p.scholarshipChance] : '—'),
  },
  {
    label: 'Empregabilidade',
    highlight: 'employability',
    render: (p) => (p.university.employability ? EMPLOYABILITY_LABELS[p.university.employability] : '—'),
  },
  { label: 'Idioma', render: (p) => p.languageSummary ?? '—' },
  { label: 'GRE', render: (p) => REQUIREMENT_LABELS[p.gre] },
  { label: 'Entrevista', render: (p) => REQUIREMENT_LABELS[p.interview] },
  { label: 'NC', render: (p) => REQUIREMENT_LABELS[p.nc] },
  {
    label: 'Candidatura',
    render: (p) => (p.application ? APPLICATION_STATUS_LABELS[p.application.status] : 'Não escolhido'),
  },
]

export function ComparePage() {
  const [params] = useSearchParams()
  const ids = (params.get('ids') ?? '').split(',').filter(Boolean)
  const compare = useCompare(ids)

  return (
    <>
      <Link
        to="/universities"
        className="mb-4 inline-flex items-center gap-1.5 text-sm text-muted hover:text-foreground"
      >
        <ArrowLeft className="size-4" aria-hidden /> Universidades
      </Link>
      <PageHeader
        title="Comparar programas"
        description="Lado a lado; a estrela marca o melhor valor de cada linha."
      />

      {ids.length < 2 || ids.length > 5 ? (
        <Card>
          <EmptyState
            icon={GitCompareArrows}
            title="Escolha de 2 a 5 programas"
            description="Na lista de programas, marque “Comparar” nos que quiser ver lado a lado."
          />
        </Card>
      ) : compare.isPending ? (
        <Skeleton className="h-96 rounded-card" />
      ) : compare.isError ? (
        <Card>
          <ErrorState error={compare.error} onRetry={() => void compare.refetch()} />
        </Card>
      ) : (
        <Card className="overflow-x-auto p-0">
          <table
            className="w-full border-separate border-spacing-0 text-sm"
            style={{ minWidth: `${10 + compare.data.programs.length * 11}rem` }}
          >
            <caption className="sr-only">Comparação de {compare.data.programs.length} programas</caption>
            <thead>
              <tr>
                <th
                  scope="col"
                  className="sticky left-0 z-10 w-40 border-b border-border bg-surface px-4 py-3 text-left"
                >
                  <span className="sr-only">Critério</span>
                </th>
                {compare.data.programs.map((p) => (
                  <th
                    key={p.id}
                    scope="col"
                    className="border-b border-border px-4 py-3 text-left align-bottom font-normal"
                  >
                    <Link to={`/programs/${p.id}`} className="font-semibold hover:text-primary">
                      {p.name}
                    </Link>
                    <p className="text-xs text-muted">{p.university.name}</p>
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              {ROWS.map((row) => {
                const best = row.highlight ? (compare.data.highlights[row.highlight] ?? []) : []
                return (
                  <tr key={row.label}>
                    <th
                      scope="row"
                      className="sticky left-0 z-10 border-b border-border bg-surface px-4 py-2.5 text-left font-normal text-muted"
                    >
                      {row.label}
                    </th>
                    {compare.data.programs.map((p) => {
                      const isBest = best.includes(p.id)
                      return (
                        <td
                          key={p.id}
                          className={cn(
                            'border-b border-border px-4 py-2.5 tabular',
                            isBest && 'bg-primary-soft/60 font-medium text-foreground',
                          )}
                        >
                          <span className="inline-flex items-center gap-1.5">
                            {row.render(p)}
                            {isBest && (
                              <>
                                <Star className="size-3.5 fill-primary text-primary" aria-hidden />
                                <span className="sr-only">(melhor)</span>
                              </>
                            )}
                          </span>
                        </td>
                      )
                    })}
                  </tr>
                )
              })}
            </tbody>
          </table>
        </Card>
      )}
    </>
  )
}
