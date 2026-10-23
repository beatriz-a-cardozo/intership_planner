import {
  Building2,
  GitCompareArrows,
  GraduationCap,
  LayoutGrid,
  MapPin,
  Plus,
  Rows3,
  Search,
} from 'lucide-react'
import { useDeferredValue, useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router'
import { toast } from 'sonner'
import { errorMessage } from '@/api/errors'
import type { Program, Rating } from '@/api/types'
import { EmptyState } from '@/components/empty-state'
import { ErrorState } from '@/components/error-state'
import { PageHeader } from '@/components/page-header'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { SegmentedControl } from '@/components/ui/segmented'
import { NativeSelect } from '@/components/ui/select'
import { Skeleton } from '@/components/ui/skeleton'
import { SwitchField } from '@/components/ui/switch'
import { formatDate, formatMoney, pluralize } from '@/lib/format'
import {
  APPLICATION_STATUS_LABELS,
  countryName,
  NATURE_LABELS,
  RATING_LABELS,
  RESULT_TONES,
} from '@/lib/labels'
import { cn } from '@/lib/utils'
import { usePrograms, useSetChosen, useUniversities, type ProgramFilters } from './api'
import { RatingBadge, RequirementChips, StatusSelect, WindowBadge } from './bits'
import { ProgramFormDialog } from './program-form-dialog'
import { UniversityFormDialog } from './university-form-dialog'
import { UniversityImage } from './university-image'

type Tab = 'programs' | 'universities'
type Layout = 'cards' | 'table'

const MAX_COMPARE = 5

export function UniversitiesPage() {
  const [params, setParams] = useSearchParams()
  const tab: Tab = params.get('tab') === 'universities' ? 'universities' : 'programs'
  const [creating, setCreating] = useState<'university' | 'program' | null>(null)
  const navigate = useNavigate()

  return (
    <>
      <PageHeader
        title="Universidades"
        description="Programas que você está avaliando, os escolhidos para se candidatar e o andamento de cada candidatura."
        actions={
          <>
            <Button variant="outline" onClick={() => setCreating('university')}>
              <Building2 aria-hidden /> Universidade
            </Button>
            <Button onClick={() => setCreating('program')}>
              <Plus aria-hidden /> Programa
            </Button>
          </>
        }
      />

      <SegmentedControl
        className="mb-5"
        ariaLabel="Visualização"
        value={tab}
        onValueChange={(v) => setParams(v === 'programs' ? {} : { tab: v }, { replace: true })}
        options={[
          { value: 'programs', label: 'Programas' },
          { value: 'universities', label: 'Universidades' },
        ]}
      />

      {tab === 'programs' ? (
        <ProgramsTab onCreate={() => setCreating('program')} />
      ) : (
        <UniversitiesTab onCreate={() => setCreating('university')} />
      )}

      {creating === 'university' && (
        <UniversityFormDialog
          onClose={() => setCreating(null)}
          onSaved={(u) => navigate(`/universities/${u.id}`)}
        />
      )}
      {creating === 'program' && (
        <ProgramFormDialog onClose={() => setCreating(null)} onSaved={(p) => navigate(`/programs/${p.id}`)} />
      )}
    </>
  )
}

// ------------------------------------------------------------------ programas

function ProgramsTab({ onCreate }: { onCreate: () => void }) {
  const [search, setSearch] = useState('')
  const [chosen, setChosen] = useState<'' | 'true'>('')
  const [minChance, setMinChance] = useState<'' | Rating>('')
  const [noGre, setNoGre] = useState(false)
  const [layout, setLayout] = useState<Layout>('cards')
  const [selected, setSelected] = useState<string[]>([])
  const q = useDeferredValue(search.trim())
  const navigate = useNavigate()

  const filters: ProgramFilters = {
    q: q || undefined,
    chosen: chosen ? true : undefined,
    minAdmissionChance: minChance || undefined,
    gre: noGre ? 'NOT_REQUIRED' : undefined,
  }
  const programs = usePrograms(filters)
  const filtered = !!(filters.q || filters.chosen || filters.minAdmissionChance || filters.gre)

  const toggle = (id: string) =>
    setSelected((s) =>
      s.includes(id) ? s.filter((x) => x !== id) : s.length >= MAX_COMPARE ? s : [...s, id],
    )

  return (
    <>
      <div className="mb-5 grid gap-3 sm:grid-cols-[1fr_auto_auto_auto] sm:items-center">
        <div className="relative">
          <Search
            className="pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2 text-muted"
            aria-hidden
          />
          <Input
            aria-label="Buscar programa, universidade ou cidade"
            placeholder="Buscar programa, universidade, cidade…"
            className="pl-9"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
        </div>
        <NativeSelect
          aria-label="Escolha"
          value={chosen}
          onChange={(e) => setChosen(e.target.value as '' | 'true')}
        >
          <option value="">Todos os programas</option>
          <option value="true">Só os escolhidos</option>
        </NativeSelect>
        <NativeSelect
          aria-label="Chance mínima de admissão"
          value={minChance}
          onChange={(e) => setMinChance(e.target.value as '' | Rating)}
        >
          <option value="">Qualquer chance</option>
          {Object.entries(RATING_LABELS).map(([k, l]) => (
            <option key={k} value={k}>
              Chance {l.toLowerCase()} ou mais
            </option>
          ))}
        </NativeSelect>
        <SwitchField
          id="filter-no-gre"
          label="Sem GRE"
          checked={noGre}
          onCheckedChange={setNoGre}
          className="sm:min-h-10"
        />
      </div>

      <div className="mb-3 flex items-center justify-between gap-3">
        <p className="text-sm text-muted" aria-live="polite">
          {programs.data ? pluralize(programs.data.length, 'programa', 'programas') : ' '}
        </p>
        <SegmentedControl
          ariaLabel="Layout"
          className="w-auto"
          value={layout}
          onValueChange={setLayout}
          options={[
            { value: 'cards', label: 'Cards', icon: <LayoutGrid aria-hidden /> },
            { value: 'table', label: 'Tabela', icon: <Rows3 aria-hidden /> },
          ]}
        />
      </div>

      {programs.isPending ? (
        <div className="grid gap-4 @2xl:grid-cols-2">
          <Skeleton className="h-48 rounded-card" />
          <Skeleton className="h-48 rounded-card" />
        </div>
      ) : programs.isError ? (
        <Card>
          <ErrorState error={programs.error} onRetry={() => void programs.refetch()} />
        </Card>
      ) : programs.data.length === 0 ? (
        <Card>
          {filtered ? (
            <EmptyState
              icon={Search}
              title="Nenhum programa com esses filtros"
              description="Ajuste a busca ou limpe os filtros."
            />
          ) : (
            <EmptyState
              icon={GraduationCap}
              title="Nenhum programa ainda"
              description="Cadastre os programas que você está avaliando. Marque os escolhidos para acompanhar a candidatura."
              action={<Button onClick={onCreate}>Adicionar programa</Button>}
            />
          )}
        </Card>
      ) : layout === 'cards' ? (
        <ul className="grid gap-4 @2xl:grid-cols-2">
          {programs.data.map((p) => (
            <li key={p.id}>
              <ProgramCard
                program={p}
                selected={selected.includes(p.id)}
                onToggleCompare={() => toggle(p.id)}
              />
            </li>
          ))}
        </ul>
      ) : (
        <ProgramTable programs={programs.data} selected={selected} onToggleCompare={toggle} />
      )}

      {selected.length > 0 && (
        <div className="sticky bottom-20 z-20 mt-5 flex items-center justify-between gap-3 rounded-card border border-border bg-surface p-3 shadow-card lg:bottom-4">
          <p className="text-sm">
            <span className="font-medium">{selected.length}</span> de {MAX_COMPARE} selecionados
            {selected.length < 2 && <span className="text-muted"> · escolha ao menos 2</span>}
          </p>
          <div className="flex gap-2">
            <Button variant="ghost" size="sm" onClick={() => setSelected([])}>
              Limpar
            </Button>
            <Button
              size="sm"
              disabled={selected.length < 2}
              onClick={() => navigate(`/universities/compare?ids=${selected.join(',')}`)}
            >
              <GitCompareArrows aria-hidden /> Comparar
            </Button>
          </div>
        </div>
      )}
    </>
  )
}

function ChosenSwitch({ program }: { program: Program }) {
  const setChosen = useSetChosen()
  return (
    <SwitchField
      id={`chosen-${program.id}`}
      label="Escolhida"
      checked={program.chosen}
      disabled={setChosen.isPending}
      onCheckedChange={(chosen) =>
        setChosen.mutate(
          { id: program.id, chosen },
          {
            onSuccess: () =>
              toast.success(
                chosen
                  ? 'Programa escolhido: candidatura criada.'
                  : 'Programa desmarcado; a candidatura foi arquivada.',
              ),
            onError: (e) => toast.error(errorMessage(e)),
          },
        )
      }
    />
  )
}

function CompareCheckbox({
  id,
  checked,
  onChange,
  label,
}: {
  id: string
  checked: boolean
  onChange: () => void
  label: string
}) {
  return (
    <label className="flex min-h-11 cursor-pointer items-center gap-2 text-sm text-muted">
      <input
        id={id}
        type="checkbox"
        className="size-4 accent-[var(--primary)]"
        checked={checked}
        onChange={onChange}
      />
      <span>
        Comparar<span className="sr-only"> {label}</span>
      </span>
    </label>
  )
}

export function ProgramCard({
  program: p,
  selected = false,
  onToggleCompare,
}: {
  program: Program
  selected?: boolean
  onToggleCompare?: () => void
}) {
  const win = p.intakes.map((i) => i.nextWindow).find((w) => w.state !== 'CLOSED') ?? p.intakes[0]?.nextWindow
  const place = [p.university.city, countryName(p.university.countryCode)].filter(Boolean).join(', ')
  return (
    <Card className={cn('flex h-full flex-col p-5', selected && 'border-primary/50 ring-1 ring-primary/30')}>
      <div className="flex items-start justify-between gap-3">
        {p.university.coverImageId && (
          <UniversityImage
            universityId={p.university.id}
            imageId={p.university.coverImageId}
            alt=""
            className="size-12 shrink-0 rounded-xl"
          />
        )}
        <div className="min-w-0 flex-1">
          <Link
            to={`/programs/${p.id}`}
            className="font-semibold hover:text-primary focus-visible:underline focus-visible:outline-none"
          >
            {p.name}
            {p.degree && <span className="font-normal text-muted"> · {p.degree}</span>}
          </Link>
          <p className="mt-0.5 truncate text-sm text-muted">
            <Link to={`/universities/${p.university.id}`} className="hover:text-foreground">
              {p.university.name}
            </Link>
          </p>
        </div>
        {p.university.ranking != null && <Badge>#{p.university.ranking}</Badge>}
      </div>

      <div className="mt-3 grid gap-1.5 text-sm text-muted">
        {place && (
          <span className="flex items-center gap-2">
            <MapPin className="size-4 shrink-0" aria-hidden /> {place}
          </span>
        )}
        {p.semesterFee && (
          <span>
            Taxa semestral <span className="tabular text-foreground">{formatMoney(p.semesterFee)}</span>
            {p.nonEuTuition && (
              <>
                {' '}
                · estrangeiro <span className="tabular text-foreground">{formatMoney(p.nonEuTuition)}</span>
              </>
            )}
          </span>
        )}
      </div>

      <div className="mt-3 flex flex-wrap gap-1.5">
        {win && <WindowBadge state={win.state} closesOn={win.closesOn} />}
        <RatingBadge value={p.admissionChance} prefix="Admissão" />
        <RequirementChips program={p} />
      </div>

      <div className="mt-auto grid gap-2 border-t border-border pt-3">
        <ChosenSwitch program={p} />
        {p.chosen && p.application && (
          <div className="flex items-center justify-between gap-3">
            <span className="text-sm text-muted">Candidatura {p.application.intakeYear}</span>
            <StatusSelect id={`status-${p.id}`} application={p.application} className="w-56 max-w-[60%]" />
          </div>
        )}
        {onToggleCompare && (
          <CompareCheckbox
            id={`compare-${p.id}`}
            checked={selected}
            onChange={onToggleCompare}
            label={p.name}
          />
        )}
      </div>
    </Card>
  )
}

function ProgramTable({
  programs,
  selected,
  onToggleCompare,
}: {
  programs: Program[]
  selected: string[]
  onToggleCompare: (id: string) => void
}) {
  return (
    <Card className="overflow-x-auto p-0">
      <table className="w-full min-w-[46rem] text-sm">
        <thead>
          <tr className="border-b border-border text-left text-xs text-muted">
            <th className="w-10 px-4 py-3 font-medium">
              <span className="sr-only">Comparar</span>
            </th>
            <th className="px-2 py-3 font-medium">Programa</th>
            <th className="px-2 py-3 font-medium">Próximo prazo</th>
            <th className="px-2 py-3 text-right font-medium">Taxa semestral</th>
            <th className="px-2 py-3 font-medium">Admissão</th>
            <th className="px-4 py-3 font-medium">Candidatura</th>
          </tr>
        </thead>
        <tbody>
          {programs.map((p) => (
            <tr
              key={p.id}
              className={cn(
                'border-b border-border last:border-0',
                selected.includes(p.id) && 'bg-primary-soft/40',
              )}
            >
              <td className="px-4 py-3">
                <input
                  type="checkbox"
                  aria-label={`Comparar ${p.name}`}
                  className="size-4 accent-[var(--primary)]"
                  checked={selected.includes(p.id)}
                  onChange={() => onToggleCompare(p.id)}
                />
              </td>
              <td className="px-2 py-3">
                <Link to={`/programs/${p.id}`} className="font-medium hover:text-primary">
                  {p.name}
                </Link>
                <p className="text-xs text-muted">{p.university.name}</p>
              </td>
              <td className="px-2 py-3 tabular">{p.nextDeadline ? formatDate(p.nextDeadline) : '—'}</td>
              <td className="px-2 py-3 text-right tabular">
                {p.semesterFee ? formatMoney(p.semesterFee) : '—'}
              </td>
              <td className="px-2 py-3">{p.admissionChance ? RATING_LABELS[p.admissionChance] : '—'}</td>
              <td className="px-4 py-3">
                {p.chosen && p.application ? (
                  <Badge tone={RESULT_TONES[p.application.result]}>
                    {APPLICATION_STATUS_LABELS[p.application.status]}
                  </Badge>
                ) : (
                  <span className="text-muted">Não escolhido</span>
                )}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </Card>
  )
}

// ------------------------------------------------------------------ universidades

function UniversitiesTab({ onCreate }: { onCreate: () => void }) {
  const universities = useUniversities()

  if (universities.isPending)
    return (
      <div className="grid gap-4 sm:grid-cols-2">
        <Skeleton className="h-32 rounded-card" />
        <Skeleton className="h-32 rounded-card" />
      </div>
    )
  if (universities.isError)
    return (
      <Card>
        <ErrorState error={universities.error} onRetry={() => void universities.refetch()} />
      </Card>
    )
  if (universities.data.length === 0)
    return (
      <Card>
        <EmptyState
          icon={Building2}
          title="Nenhuma universidade"
          description="Cadastre as universidades que te interessam; depois adicione os programas de cada uma."
          action={<Button onClick={onCreate}>Adicionar universidade</Button>}
        />
      </Card>
    )

  return (
    <ul className="grid gap-4 sm:grid-cols-2">
      {universities.data.map((u) => (
        <li key={u.id}>
          <Link
            to={`/universities/${u.id}`}
            className="block h-full rounded-card focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring/40"
          >
            <Card className="h-full overflow-hidden transition-colors hover:border-primary/40">
              {u.coverImageId && (
                <UniversityImage
                  universityId={u.id}
                  imageId={u.coverImageId}
                  alt=""
                  className="h-32 w-full"
                />
              )}
              <div className="p-5">
                <div className="flex items-start justify-between gap-3">
                  <h2 className="font-semibold">{u.name}</h2>
                  {u.ranking != null && <Badge>#{u.ranking}</Badge>}
                </div>
                <p className="mt-1 text-sm text-muted">
                  {[
                    u.nature && NATURE_LABELS[u.nature],
                    [u.city, countryName(u.countryCode)].filter(Boolean).join(', '),
                  ]
                    .filter(Boolean)
                    .join(' · ')}
                </p>
                <p className="mt-4 text-sm">
                  {pluralize(u.programCount, 'programa', 'programas')}
                  {u.chosenCount > 0 && (
                    <span className="text-primary">
                      {' '}
                      · {pluralize(u.chosenCount, 'escolhido', 'escolhidos')}
                    </span>
                  )}
                </p>
              </div>
            </Card>
          </Link>
        </li>
      ))}
    </ul>
  )
}
