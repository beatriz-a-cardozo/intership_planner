import { toast } from 'sonner'
import { errorMessage } from '@/api/errors'
import type { Application, ApplicationStatus, Program, Rating, Requirement, WindowState } from '@/api/types'
import { Badge } from '@/components/ui/badge'
import { NativeSelect } from '@/components/ui/select'
import { formatDate } from '@/lib/format'
import {
  APPLICATION_STATUSES,
  APPLICATION_STATUS_LABELS,
  RATING_LABELS,
  REQUIREMENT_LABELS,
  WINDOW_STATE_LABELS,
} from '@/lib/labels'
import { useChangeStatus } from './api'

/** Chance (Baixa → Alta) com tom de acordo com o nível; o texto sempre acompanha a cor. */
export function RatingBadge({ value, prefix }: { value: Rating | null; prefix?: string }) {
  if (!value) return null
  const tone =
    value === 'HIGH' || value === 'MEDIUM_HIGH' ? 'primary' : value === 'MEDIUM' ? 'neutral' : 'accent'
  return (
    <Badge tone={tone}>
      {prefix && `${prefix} `}
      {RATING_LABELS[value].toLowerCase()}
    </Badge>
  )
}

export function WindowBadge({ state, closesOn }: { state: WindowState; closesOn: string | null }) {
  return (
    <Badge tone={state === 'OPEN' ? 'primary' : 'neutral'}>
      {WINDOW_STATE_LABELS[state]}
      {closesOn && state !== 'CLOSED' && ` · até ${formatDate(closesOn)}`}
    </Badge>
  )
}

/** Requisitos tri-estado em chips curtos (só os que importam: exige / se necessário). */
export function RequirementChips({
  program,
}: {
  program: Pick<Program, 'gre' | 'interview' | 'nc' | 'aptitudeAssessment'>
}) {
  const items: [string, Requirement][] = [
    ['GRE', program.gre],
    ['Entrevista', program.interview],
    ['NC', program.nc],
    ['Aptitude test', program.aptitudeAssessment],
  ]
  const relevant = items.filter(([, r]) => r === 'REQUIRED' || r === 'CONDITIONAL')
  if (relevant.length === 0) return null
  return (
    <span className="flex flex-wrap gap-1">
      {relevant.map(([label, r]) => (
        <Badge key={label} tone={r === 'REQUIRED' ? 'accent' : 'neutral'}>
          {label}: {REQUIREMENT_LABELS[r].toLowerCase()}
        </Badge>
      ))}
    </span>
  )
}

/** Seletor de status da candidatura (muda direto do card; o histórico é registrado no backend). */
export function StatusSelect({
  application,
  id,
  className,
}: {
  application: Pick<Application, 'id' | 'status'>
  id: string
  className?: string
}) {
  const change = useChangeStatus()
  return (
    <div className={className}>
      <label htmlFor={id} className="sr-only">
        Status da candidatura
      </label>
      <NativeSelect
        id={id}
        value={application.status}
        disabled={change.isPending}
        className="h-9 text-sm"
        onChange={(e) =>
          change.mutate(
            { id: application.id, status: e.target.value as ApplicationStatus },
            {
              onSuccess: () => toast.success('Status atualizado.'),
              onError: (err) => toast.error(errorMessage(err)),
            },
          )
        }
      >
        {APPLICATION_STATUSES.map((s) => (
          <option key={s} value={s}>
            {APPLICATION_STATUS_LABELS[s]}
          </option>
        ))}
      </NativeSelect>
    </div>
  )
}
