import { ArrowLeft, ExternalLink, Pencil, Trash2 } from 'lucide-react'
import { useState, type ReactNode } from 'react'
import { Link, useNavigate, useParams, useSearchParams } from 'react-router'
import { toast } from 'sonner'
import { errorMessage } from '@/api/errors'
import type { Program } from '@/api/types'
import { ConfirmDialog } from '@/components/confirm-dialog'
import { ErrorState } from '@/components/error-state'
import { PageHeader } from '@/components/page-header'
import { Button } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'
import { SegmentedControl } from '@/components/ui/segmented'
import { Skeleton } from '@/components/ui/skeleton'
import { SwitchField } from '@/components/ui/switch'
import { formatDate, formatMoney } from '@/lib/format'
import {
  CHANNEL_LABELS,
  countryName,
  EMPLOYABILITY_LABELS,
  MONTH_NAMES,
  RATING_LABELS,
  REQUIREMENT_LABELS,
  TERM_LABELS,
} from '@/lib/labels'
import { useDeleteProgram, useProgram, useSetChosen } from './api'
import { ApplicationPanel } from './application-panel'
import { RatingBadge, WindowBadge } from './bits'
import { ProgramFormDialog } from './program-form-dialog'

const TABS = [
  { value: 'overview', label: 'Geral' },
  { value: 'requirements', label: 'Requisitos' },
  { value: 'costs', label: 'Custos' },
  { value: 'deadlines', label: 'Prazos' },
  { value: 'application', label: 'Candidatura' },
] as const
type Tab = (typeof TABS)[number]['value']

const DOCS: Record<string, string> = {
  ONLINE: 'Online',
  PHYSICAL: 'Físico (correio)',
  BOTH: 'Online e físico',
}
const PERIOD: Record<string, string> = { SEMESTER: 'por semestre', YEAR: 'por ano', TOTAL: 'curso inteiro' }

function Facts({ items }: { items: [string, ReactNode][] }) {
  return (
    <dl className="grid gap-x-8 gap-y-4 text-sm sm:grid-cols-2">
      {items.map(([label, value]) => (
        <div key={label}>
          <dt className="text-muted">{label}</dt>
          <dd className="mt-0.5">{value ?? '—'}</dd>
        </div>
      ))}
    </dl>
  )
}

export function ProgramDetailPage() {
  const { id = '' } = useParams()
  const program = useProgram(id)
  const [params, setParams] = useSearchParams()
  const requested = params.get('tab')
  const tab: Tab = TABS.some((t) => t.value === requested) ? (requested as Tab) : 'overview'
  const [dialog, setDialog] = useState<'edit' | 'delete' | null>(null)
  const remove = useDeleteProgram()
  const setChosen = useSetChosen()
  const navigate = useNavigate()

  if (program.isPending) return <Skeleton className="h-64 rounded-card" />
  if (program.isError)
    return (
      <Card>
        <ErrorState error={program.error} onRetry={() => void program.refetch()} />
      </Card>
    )
  const p = program.data

  return (
    <>
      <Link
        to="/universities"
        className="mb-4 inline-flex items-center gap-1.5 text-sm text-muted hover:text-foreground"
      >
        <ArrowLeft className="size-4" aria-hidden /> Universidades
      </Link>
      <PageHeader
        eyebrow={p.university.name}
        title={p.degree ? `${p.name} · ${p.degree}` : p.name}
        description={
          [p.university.city, countryName(p.university.countryCode)].filter(Boolean).join(', ') || undefined
        }
        actions={
          <>
            <Button
              variant="outline"
              size="icon"
              aria-label="Excluir programa"
              onClick={() => setDialog('delete')}
            >
              <Trash2 />
            </Button>
            <Button variant="outline" onClick={() => setDialog('edit')}>
              <Pencil aria-hidden /> Editar
            </Button>
          </>
        }
      />

      <Card className="mb-5">
        <CardContent className="flex flex-wrap items-center justify-between gap-4 py-3 sm:py-3">
          <SwitchField
            id="program-chosen"
            label="Escolhida para candidatura"
            hint={
              p.chosen
                ? 'A candidatura aparece no quadro de candidaturas.'
                : 'Marque para criar a candidatura e acompanhar o status.'
            }
            checked={p.chosen}
            disabled={setChosen.isPending}
            onCheckedChange={(chosen) =>
              setChosen.mutate(
                { id: p.id, chosen },
                {
                  onSuccess: () => {
                    toast.success(chosen ? 'Candidatura criada.' : 'Candidatura arquivada.')
                    if (chosen) setParams({ tab: 'application' }, { replace: true })
                  },
                  onError: (e) => toast.error(errorMessage(e)),
                },
              )
            }
            className="flex-1"
          />
        </CardContent>
      </Card>

      <SegmentedControl
        ariaLabel="Seção do programa"
        className="mb-5 overflow-x-auto"
        value={tab}
        onValueChange={(v) => setParams(v === 'overview' ? {} : { tab: v }, { replace: true })}
        options={TABS.map((t) => ({ value: t.value, label: t.label }))}
      />

      {tab === 'overview' && <OverviewTab program={p} />}
      {tab === 'requirements' && <RequirementsTab program={p} />}
      {tab === 'costs' && <CostsTab program={p} />}
      {tab === 'deadlines' && <DeadlinesTab program={p} />}
      {tab === 'application' &&
        (p.application ? (
          <ApplicationPanel applicationId={p.application.id} />
        ) : (
          <Card>
            <CardContent className="text-sm text-muted">
              Este programa ainda não foi escolhido. Ative “Escolhida para candidatura” acima para acompanhar
              status, prazos e solicitações.
            </CardContent>
          </Card>
        ))}

      {dialog === 'edit' && <ProgramFormDialog program={p} onClose={() => setDialog(null)} />}
      <ConfirmDialog
        open={dialog === 'delete'}
        onOpenChange={(o) => !o && setDialog(null)}
        title="Excluir programa?"
        description={
          p.application
            ? 'A candidatura e o histórico deste programa também serão excluídos.'
            : 'Esta ação não pode ser desfeita.'
        }
        confirmLabel="Excluir"
        destructive
        loading={remove.isPending}
        onConfirm={() =>
          remove.mutate(p.id, {
            onSuccess: () => {
              toast.success('Programa excluído.')
              navigate('/universities', { replace: true })
            },
            onError: (e) => toast.error(errorMessage(e)),
          })
        }
      />
    </>
  )
}

function OverviewTab({ program: p }: { program: Program }) {
  return (
    <Card>
      <CardContent className="grid gap-6">
        <Facts
          items={[
            ['Área / foco', p.area],
            ['Idioma das aulas', p.languageSummary],
            ['Duração', p.durationSemesters != null ? `${p.durationSemesters} semestres` : null],
            ['Chance de admissão', p.admissionChance && <RatingBadge value={p.admissionChance} />],
            ['Ranking da universidade', p.university.ranking != null ? `#${p.university.ranking}` : null],
            [
              'Empregabilidade',
              p.university.employability && EMPLOYABILITY_LABELS[p.university.employability],
            ],
          ]}
        />
        {p.officialUrl && (
          <a
            href={p.officialUrl}
            target="_blank"
            rel="noreferrer"
            className="inline-flex items-center gap-1.5 text-sm text-primary hover:underline"
          >
            Página oficial do programa <ExternalLink className="size-3.5" aria-hidden />
          </a>
        )}
        {p.description && <p className="whitespace-pre-line text-sm">{p.description}</p>}
        {p.notes && (
          <p className="whitespace-pre-line border-t border-border pt-4 text-sm text-muted">{p.notes}</p>
        )}
      </CardContent>
    </Card>
  )
}

function RequirementsTab({ program: p }: { program: Program }) {
  return (
    <div className="grid gap-5 lg:grid-cols-2">
      <Card>
        <CardContent>
          <h2 className="mb-4 text-sm font-semibold">Exigências</h2>
          <Facts
            items={[
              ['GRE', REQUIREMENT_LABELS[p.gre]],
              ['Aptitude test', REQUIREMENT_LABELS[p.aptitudeAssessment]],
              ['Entrevista', REQUIREMENT_LABELS[p.interview]],
              ['NC (numerus clausus)', REQUIREMENT_LABELS[p.nc]],
            ]}
          />
        </CardContent>
      </Card>
      <Card>
        <CardContent>
          <h2 className="mb-4 text-sm font-semibold">Proficiência</h2>
          {p.languageRequirements.length === 0 ? (
            <p className="text-sm text-muted">Nenhum requisito de idioma cadastrado.</p>
          ) : (
            <ul className="divide-y divide-border">
              {p.languageRequirements.map((r, i) => (
                <li key={i} className="flex items-baseline justify-between gap-3 py-2.5 text-sm first:pt-0">
                  <span className="font-medium uppercase">{r.language}</span>
                  <span className="text-right text-muted">
                    {[
                      r.minCefr,
                      r.testName &&
                        `${r.testName}${r.minScore != null ? ` ≥ ${String(r.minScore).replace('.', ',')}` : ''}`,
                    ]
                      .filter(Boolean)
                      .join(' · ') ||
                      r.rawText ||
                      '—'}
                  </span>
                </li>
              ))}
            </ul>
          )}
        </CardContent>
      </Card>
    </div>
  )
}

function CostsTab({ program: p }: { program: Program }) {
  return (
    <Card>
      <CardContent>
        <Facts
          items={[
            [
              'Taxa semestral',
              p.semesterFee && <span className="tabular">{formatMoney(p.semesterFee)}</span>,
            ],
            [
              'Taxa para estrangeiros',
              p.nonEuTuition ? (
                <span className="tabular">
                  {formatMoney(p.nonEuTuition)} {p.nonEuTuitionPeriod && PERIOD[p.nonEuTuitionPeriod]}
                </span>
              ) : (
                'Não cobra'
              ),
            ],
            [
              'Custo de vida na cidade',
              p.university.costOfLiving && (
                <span className="tabular">{formatMoney(p.university.costOfLiving)} / mês</span>
              ),
            ],
            [
              'Bolsa',
              p.scholarshipAvailable == null ? 'Não sei' : p.scholarshipAvailable ? 'Tem' : 'Não tem',
            ],
            ['Chance de bolsa', p.scholarshipChance && RATING_LABELS[p.scholarshipChance]],
            ['Detalhes da bolsa', p.scholarshipDetails],
          ]}
        />
      </CardContent>
    </Card>
  )
}

function DeadlinesTab({ program: p }: { program: Program }) {
  const dayMonth = (d: number | null, m: number | null) =>
    m ? `${d ? `${d} de ` : ''}${MONTH_NAMES[m - 1]!.toLowerCase()}` : null
  return (
    <div className="grid gap-5">
      <Card>
        <CardContent>
          <Facts
            items={[
              ['Canal de inscrição', p.applicationChannel && CHANNEL_LABELS[p.applicationChannel]],
              ['Portal', p.portalName],
              ['Envio de documentos', p.documentSubmission && DOCS[p.documentSubmission]],
              ['Prazo (texto original)', p.applicationDeadlineText],
            ]}
          />
        </CardContent>
      </Card>
      {p.intakes.length === 0 ? (
        <Card>
          <CardContent className="text-sm text-muted">Nenhuma janela de inscrição cadastrada.</CardContent>
        </Card>
      ) : (
        <ul className="grid gap-4 md:grid-cols-2">
          {p.intakes.map((i) => {
            const w = i.nextWindow
            return (
              <li key={i.term}>
                <Card className="h-full">
                  <CardContent className="grid gap-3">
                    <div className="flex items-center justify-between gap-3">
                      <h2 className="font-semibold">
                        {TERM_LABELS[i.term]} {w.intakeYear}
                      </h2>
                      <WindowBadge state={w.state} closesOn={w.closesOn} />
                    </div>
                    <dl className="grid grid-cols-3 gap-3 text-sm">
                      <div>
                        <dt className="text-muted">Abre</dt>
                        <dd className="tabular">{formatDate(w.opensOn)}</dd>
                      </div>
                      <div>
                        <dt className="text-muted">Prazo</dt>
                        <dd className="font-medium tabular">{formatDate(w.closesOn)}</dd>
                      </div>
                      <div>
                        <dt className="text-muted">Aulas</dt>
                        <dd className="tabular">{formatDate(w.startsOn)}</dd>
                      </div>
                    </dl>
                    <p className="text-xs text-muted">
                      Todo ano:{' '}
                      {[
                        dayMonth(i.openDay, i.openMonth) && `abre ${dayMonth(i.openDay, i.openMonth)}`,
                        dayMonth(i.closeDay, i.closeMonth) && `fecha ${dayMonth(i.closeDay, i.closeMonth)}`,
                      ]
                        .filter(Boolean)
                        .join(', ') || 'datas não informadas'}
                      .
                    </p>
                  </CardContent>
                </Card>
              </li>
            )
          })}
        </ul>
      )}
    </div>
  )
}
