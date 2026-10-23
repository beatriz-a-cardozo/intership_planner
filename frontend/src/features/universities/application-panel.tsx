import { zodResolver } from '@hookform/resolvers/zod'
import { Check, ClipboardList, Pencil, Plus, Trash2, Undo2 } from 'lucide-react'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { toast } from 'sonner'
import { z } from 'zod'
import { errorMessage } from '@/api/errors'
import type {
  Application,
  ApplicationUpdateRequest,
  Term,
  UniversityRequestItem,
  UniversityRequestRequest,
} from '@/api/types'
import { EmptyState } from '@/components/empty-state'
import { ErrorState } from '@/components/error-state'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Field } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { ResponsiveDialog } from '@/components/ui/responsive-dialog'
import { NativeSelect } from '@/components/ui/select'
import { Skeleton } from '@/components/ui/skeleton'
import { Textarea } from '@/components/ui/textarea'
import { applyServerErrors } from '@/features/auth/use-server-errors'
import { formatDate, todayIso } from '@/lib/format'
import {
  APPLICATION_STATUS_LABELS,
  CONFIDENCE_LABELS,
  REQUEST_TYPE_LABELS,
  RESULT_TONES,
  TERM_LABELS,
} from '@/lib/labels'
import { cn } from '@/lib/utils'
import {
  useApplication,
  useApplicationHistory,
  useApplicationRequests,
  useDeleteRequest,
  useSaveRequest,
  useUpdateApplication,
} from './api'
import { StatusSelect } from './bits'

/**
 * Faixa da resposta esperada: mínimo → provável → máximo numa régua; o texto sempre acompanha.
 * Datas já passadas ficam esmaecidas para mostrar onde estamos.
 */
export function ResponseRange({
  application: a,
}: {
  application: Pick<
    Application,
    'responseMinDate' | 'responseLikelyDate' | 'responseMaxDate' | 'actualResponseDate'
  >
}) {
  if (a.actualResponseDate)
    return <p className="text-sm">Resposta recebida em {formatDate(a.actualResponseDate)}</p>
  if (!a.responseMinDate && !a.responseLikelyDate && !a.responseMaxDate)
    return <p className="text-sm text-muted">Sem estimativa de resposta.</p>

  const min = a.responseMinDate ?? a.responseLikelyDate ?? a.responseMaxDate!
  const max = a.responseMaxDate ?? a.responseLikelyDate ?? min
  const t = (iso: string) => new Date(`${iso}T00:00:00`).getTime()
  const span = t(max) - t(min)
  const pos = (iso: string) => (span <= 0 ? 50 : ((t(iso) - t(min)) / span) * 100)
  const today = todayIso()
  const todayPos = today > min && today < max ? pos(today) : null

  return (
    <div>
      <div className="relative mx-1.5 h-2 rounded-full bg-surface-2" aria-hidden>
        <div className="absolute inset-y-0 rounded-full bg-primary/25" style={{ left: 0, right: 0 }} />
        {todayPos != null && (
          <div className="absolute inset-y-[-3px] w-px bg-muted" style={{ left: `${todayPos}%` }} />
        )}
        {a.responseLikelyDate && (
          <span
            className="absolute top-1/2 size-3.5 -translate-x-1/2 -translate-y-1/2 rounded-full border-2 border-surface bg-primary"
            style={{ left: `${pos(a.responseLikelyDate)}%` }}
          />
        )}
      </div>
      <dl className="mt-2 grid grid-cols-3 text-xs">
        <div>
          <dt className="text-muted">Mais cedo</dt>
          <dd className="tabular">{a.responseMinDate ? formatDate(a.responseMinDate) : '—'}</dd>
        </div>
        <div className="text-center">
          <dt className="text-muted">Provável</dt>
          <dd className="font-medium tabular">
            {a.responseLikelyDate ? formatDate(a.responseLikelyDate) : '—'}
          </dd>
        </div>
        <div className="text-right">
          <dt className="text-muted">Mais tarde</dt>
          <dd className="tabular">{a.responseMaxDate ? formatDate(a.responseMaxDate) : '—'}</dd>
        </div>
      </dl>
    </div>
  )
}

export function ApplicationPanel({ applicationId }: { applicationId: string }) {
  const application = useApplication(applicationId)
  const history = useApplicationHistory(applicationId)
  const [editing, setEditing] = useState(false)

  if (application.isPending) return <Skeleton className="h-64 rounded-card" />
  if (application.isError)
    return (
      <Card>
        <ErrorState error={application.error} onRetry={() => void application.refetch()} />
      </Card>
    )
  const a = application.data

  return (
    <div className="grid gap-6 @4xl:grid-cols-2">
      <Card>
        <CardHeader className="items-center">
          <CardTitle>
            Candidatura · {TERM_LABELS[a.term]} {a.intakeYear}
          </CardTitle>
          <Button variant="outline" size="sm" onClick={() => setEditing(true)}>
            <Pencil aria-hidden /> Editar
          </Button>
        </CardHeader>
        <CardContent className="grid gap-5">
          <div className="flex flex-wrap items-center gap-3">
            <StatusSelect id={`status-${a.id}`} application={a} className="w-64 max-w-full" />
            <Badge tone={RESULT_TONES[a.result]}>
              {a.result === 'PENDING' ? 'Em andamento' : APPLICATION_STATUS_LABELS[a.status]}
            </Badge>
          </div>
          <dl className="grid grid-cols-2 gap-3 text-sm">
            <div>
              <dt className="text-muted">Prazo</dt>
              <dd className="tabular">{formatDate(a.deadline ?? a.window?.closesOn)}</dd>
            </div>
            <div>
              <dt className="text-muted">Enviada em</dt>
              <dd className="tabular">{a.submittedAt ? formatDate(a.submittedAt) : 'Ainda não'}</dd>
            </div>
          </dl>
          <div>
            <p className="mb-2 text-sm font-medium">
              Resposta esperada
              {a.responseConfidence && (
                <span className="font-normal text-muted">
                  {' '}
                  · confiança {CONFIDENCE_LABELS[a.responseConfidence].toLowerCase()}
                </span>
              )}
            </p>
            <ResponseRange application={a} />
            {a.responseEstimateBasis && (
              <p className="mt-2 text-xs text-muted">Base: {a.responseEstimateBasis}</p>
            )}
          </div>
          {a.notes && (
            <p className="whitespace-pre-line border-t border-border pt-4 text-sm text-muted">{a.notes}</p>
          )}
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>Histórico</CardTitle>
        </CardHeader>
        <CardContent>
          {history.isPending ? (
            <Skeleton className="h-24" />
          ) : history.isError ? (
            <ErrorState error={history.error} onRetry={() => void history.refetch()} />
          ) : (
            <ol className="grid gap-0">
              {history.data.map((h, i) => (
                <li key={h.id} className="relative grid grid-cols-[1rem_1fr] gap-3 pb-4 last:pb-0">
                  <span
                    aria-hidden
                    className={cn(
                      'relative z-10 mt-1.5 size-2.5 rounded-full',
                      i === history.data.length - 1 ? 'bg-primary' : 'bg-border',
                    )}
                  />
                  {i < history.data.length - 1 && (
                    <span aria-hidden className="absolute top-4 bottom-0 left-[4.5px] w-px bg-border" />
                  )}
                  <div>
                    <p className="text-sm font-medium">{APPLICATION_STATUS_LABELS[h.status]}</p>
                    <p className="text-xs text-muted tabular">{formatDate(h.changedOn, 'medium')}</p>
                    {h.note && <p className="mt-1 text-sm text-muted">{h.note}</p>}
                  </div>
                </li>
              ))}
            </ol>
          )}
        </CardContent>
      </Card>

      <RequestsCard applicationId={a.id} />

      {editing && <ApplicationEditDialog application={a} onClose={() => setEditing(false)} />}
    </div>
  )
}

// ------------------------------------------------------------------ edição / estimativa

const optionalDate = z.string()
const editSchema = z
  .object({
    intakeYear: z.string().regex(/^\d{4}$/, 'Ano inválido.'),
    term: z.enum(['WINTER', 'SUMMER', 'OTHER']),
    deadline: optionalDate,
    submittedAt: optionalDate,
    responseMinDate: optionalDate,
    responseLikelyDate: optionalDate,
    responseMaxDate: optionalDate,
    actualResponseDate: optionalDate,
    responseConfidence: z.string(),
    responseEstimateBasis: z.string().max(300),
    notes: z.string().max(2000),
  })
  .superRefine((v, ctx) => {
    const ordered = [v.responseMinDate, v.responseLikelyDate, v.responseMaxDate].filter(Boolean)
    if (ordered.some((d, i) => i > 0 && d < ordered[i - 1]!)) {
      ctx.addIssue({
        code: 'custom',
        path: ['responseLikelyDate'],
        message: 'Use mais cedo ≤ provável ≤ mais tarde.',
      })
    }
  })
type EditForm = z.infer<typeof editSchema>

const orUndefined = (v: string) => (v.trim() ? v.trim() : undefined)

function ApplicationEditDialog({
  application: a,
  onClose,
}: {
  application: Application
  onClose: () => void
}) {
  const update = useUpdateApplication()
  const [formError, setFormError] = useState('')
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors },
  } = useForm<EditForm>({
    resolver: zodResolver(editSchema),
    defaultValues: {
      intakeYear: String(a.intakeYear),
      term: a.term,
      deadline: a.deadline ?? '',
      submittedAt: a.submittedAt ?? '',
      responseMinDate: a.responseMinDate ?? '',
      responseLikelyDate: a.responseLikelyDate ?? '',
      responseMaxDate: a.responseMaxDate ?? '',
      actualResponseDate: a.actualResponseDate ?? '',
      responseConfidence: a.responseConfidence ?? '',
      responseEstimateBasis: a.responseEstimateBasis ?? '',
      notes: a.notes ?? '',
    },
  })

  const onSubmit = handleSubmit((v) => {
    const body: ApplicationUpdateRequest = {
      intakeYear: Number(v.intakeYear),
      term: v.term as Term,
      objectiveId: a.objectiveId ?? undefined,
      deadline: orUndefined(v.deadline),
      submittedAt: orUndefined(v.submittedAt),
      responseMinDate: orUndefined(v.responseMinDate),
      responseLikelyDate: orUndefined(v.responseLikelyDate),
      responseMaxDate: orUndefined(v.responseMaxDate),
      actualResponseDate: orUndefined(v.actualResponseDate),
      responseConfidence: (v.responseConfidence ||
        undefined) as ApplicationUpdateRequest['responseConfidence'],
      responseEstimateBasis: orUndefined(v.responseEstimateBasis),
      notes: orUndefined(v.notes),
    }
    update.mutate(
      { id: a.id, body },
      {
        onSuccess: () => {
          toast.success('Candidatura atualizada.')
          onClose()
        },
        onError: (e) =>
          setFormError(
            applyServerErrors(e, setError, [
              'intakeYear',
              'term',
              'responseMinDate',
              'responseLikelyDate',
              'responseMaxDate',
            ]),
          ),
      },
    )
  })

  return (
    <ResponsiveDialog
      open
      onOpenChange={(o) => !o && onClose()}
      title="Editar candidatura"
      description="Prazos e a estimativa de quando a resposta deve chegar."
      footer={
        <>
          <Button variant="outline" onClick={onClose}>
            Cancelar
          </Button>
          <Button type="submit" form="application-form" loading={update.isPending}>
            Salvar
          </Button>
        </>
      }
    >
      <form id="application-form" onSubmit={onSubmit} noValidate className="grid gap-4">
        {formError && (
          <div role="alert" className="rounded-xl bg-danger-soft px-4 py-3 text-sm text-danger">
            {formError}
          </div>
        )}
        <div className="grid grid-cols-2 gap-4">
          <Field id="app-term" label="Semestre">
            <NativeSelect id="app-term" {...register('term')}>
              {Object.entries(TERM_LABELS).map(([k, l]) => (
                <option key={k} value={k}>
                  {l}
                </option>
              ))}
            </NativeSelect>
          </Field>
          <Field id="app-year" label="Ano de início" error={errors.intakeYear?.message}>
            <Input
              id="app-year"
              inputMode="numeric"
              aria-invalid={!!errors.intakeYear}
              {...register('intakeYear')}
            />
          </Field>
          <Field id="app-deadline" label="Prazo" hint="Vazio = prazo do programa">
            <Input id="app-deadline" type="date" {...register('deadline')} />
          </Field>
          <Field id="app-submitted" label="Enviada em">
            <Input id="app-submitted" type="date" {...register('submittedAt')} />
          </Field>
        </div>
        <fieldset className="grid gap-4">
          <legend className="mb-1 text-sm font-medium">Resposta esperada</legend>
          <div className="grid gap-4 sm:grid-cols-3">
            <Field id="app-min" label="Mais cedo" error={errors.responseMinDate?.message}>
              <Input id="app-min" type="date" {...register('responseMinDate')} />
            </Field>
            <Field id="app-likely" label="Provável" error={errors.responseLikelyDate?.message}>
              <Input
                id="app-likely"
                type="date"
                aria-invalid={!!errors.responseLikelyDate}
                {...register('responseLikelyDate')}
              />
            </Field>
            <Field id="app-max" label="Mais tarde" error={errors.responseMaxDate?.message}>
              <Input id="app-max" type="date" {...register('responseMaxDate')} />
            </Field>
          </div>
          <div className="grid gap-4 sm:grid-cols-[10rem_1fr]">
            <Field id="app-confidence" label="Confiança">
              <NativeSelect id="app-confidence" {...register('responseConfidence')}>
                <option value="">—</option>
                {Object.entries(CONFIDENCE_LABELS).map(([k, l]) => (
                  <option key={k} value={k}>
                    {l}
                  </option>
                ))}
              </NativeSelect>
            </Field>
            <Field id="app-basis" label="Baseado em">
              <Input
                id="app-basis"
                placeholder="Ex.: relatos do ano passado no fórum"
                {...register('responseEstimateBasis')}
              />
            </Field>
          </div>
          <Field
            id="app-actual"
            label="Resposta recebida em"
            hint="Preenchido automaticamente ao marcar aceito/recusado"
          >
            <Input id="app-actual" type="date" {...register('actualResponseDate')} />
          </Field>
        </fieldset>
        <Field id="app-notes" label="Observações">
          <Textarea id="app-notes" rows={2} {...register('notes')} />
        </Field>
      </form>
    </ResponsiveDialog>
  )
}

// ------------------------------------------------------------------ solicitações da universidade

function RequestsCard({ applicationId }: { applicationId: string }) {
  const requests = useApplicationRequests(applicationId)
  const save = useSaveRequest(applicationId)
  const remove = useDeleteRequest(applicationId)
  const [editing, setEditing] = useState<UniversityRequestItem | 'new' | null>(null)

  const toggleFulfilled = (r: UniversityRequestItem) =>
    save.mutate(
      {
        id: r.id,
        body: {
          type: r.type,
          description: r.description,
          requestedOn: r.requestedOn,
          dueDate: r.dueDate ?? undefined,
          fulfilledOn: r.pending ? todayIso() : undefined,
        },
      },
      { onError: (e) => toast.error(errorMessage(e)) },
    )

  return (
    <Card className="@4xl:col-span-2">
      <CardHeader className="items-center">
        <div>
          <CardTitle>Solicitações da universidade</CardTitle>
          <p className="mt-1 text-sm text-muted">
            Uma pendência com a candidatura em análise muda o status para “item solicitado”; resolvida a
            última, volta para análise.
          </p>
        </div>
        <Button variant="outline" size="sm" onClick={() => setEditing('new')}>
          <Plus aria-hidden /> Solicitação
        </Button>
      </CardHeader>
      <CardContent>
        {requests.isPending ? (
          <Skeleton className="h-20" />
        ) : requests.isError ? (
          <ErrorState error={requests.error} onRetry={() => void requests.refetch()} />
        ) : requests.data.length === 0 ? (
          <EmptyState
            icon={ClipboardList}
            title="Nenhuma solicitação"
            description="Documentos, entrevistas ou testes pedidos pela universidade aparecem aqui."
          />
        ) : (
          <ul className="divide-y divide-border">
            {requests.data.map((r) => (
              <li key={r.id} className="flex flex-wrap items-center gap-3 py-3">
                <div className="min-w-0 flex-1">
                  <p className={cn('text-sm font-medium', !r.pending && 'text-muted line-through')}>
                    {r.description}
                  </p>
                  <p className="text-xs text-muted">
                    {REQUEST_TYPE_LABELS[r.type]} · pedido em {formatDate(r.requestedOn)}
                    {r.dueDate && ` · até ${formatDate(r.dueDate)}`}
                    {r.fulfilledOn && ` · entregue em ${formatDate(r.fulfilledOn)}`}
                  </p>
                </div>
                {r.overdue && <Badge tone="danger">Atrasada</Badge>}
                <div className="flex gap-1">
                  <Button
                    variant="ghost"
                    size="sm"
                    disabled={save.isPending}
                    onClick={() => toggleFulfilled(r)}
                  >
                    {r.pending ? (
                      <>
                        <Check aria-hidden /> Entregue
                      </>
                    ) : (
                      <>
                        <Undo2 aria-hidden /> Reabrir
                      </>
                    )}
                  </Button>
                  <Button
                    variant="ghost"
                    size="icon"
                    aria-label={`Editar ${r.description}`}
                    onClick={() => setEditing(r)}
                  >
                    <Pencil />
                  </Button>
                  <Button
                    variant="ghost"
                    size="icon"
                    aria-label={`Excluir ${r.description}`}
                    disabled={remove.isPending}
                    onClick={() =>
                      remove.mutate(r.id, {
                        onSuccess: () => toast.success('Solicitação excluída.'),
                        onError: (e) => toast.error(errorMessage(e)),
                      })
                    }
                  >
                    <Trash2 />
                  </Button>
                </div>
              </li>
            ))}
          </ul>
        )}
      </CardContent>
      {editing && (
        <RequestDialog
          applicationId={applicationId}
          request={editing === 'new' ? undefined : editing}
          onClose={() => setEditing(null)}
        />
      )}
    </Card>
  )
}

const requestSchema = z.object({
  type: z.string(),
  description: z.string().trim().min(1, 'Descreva a solicitação.').max(300),
  requestedOn: z.string().min(1, 'Informe a data.'),
  dueDate: z.string(),
})
type RequestForm = z.infer<typeof requestSchema>

function RequestDialog({
  applicationId,
  request: r,
  onClose,
}: {
  applicationId: string
  request?: UniversityRequestItem
  onClose: () => void
}) {
  const save = useSaveRequest(applicationId)
  const [formError, setFormError] = useState('')
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors },
  } = useForm<RequestForm>({
    resolver: zodResolver(requestSchema),
    defaultValues: {
      type: r?.type ?? 'DOCUMENT',
      description: r?.description ?? '',
      requestedOn: r?.requestedOn ?? todayIso(),
      dueDate: r?.dueDate ?? '',
    },
  })

  const onSubmit = handleSubmit((v) =>
    save.mutate(
      {
        id: r?.id,
        body: {
          type: v.type as UniversityRequestRequest['type'],
          description: v.description.trim(),
          requestedOn: v.requestedOn,
          dueDate: v.dueDate || undefined,
          fulfilledOn: r?.fulfilledOn ?? undefined,
        },
      },
      {
        onSuccess: () => {
          toast.success(r ? 'Solicitação atualizada.' : 'Solicitação registrada.')
          onClose()
        },
        onError: (e) =>
          setFormError(applyServerErrors(e, setError, ['description', 'requestedOn', 'dueDate'])),
      },
    ),
  )

  return (
    <ResponsiveDialog
      open
      onOpenChange={(o) => !o && onClose()}
      title={r ? 'Editar solicitação' : 'Nova solicitação'}
      footer={
        <>
          <Button variant="outline" onClick={onClose}>
            Cancelar
          </Button>
          <Button type="submit" form="request-form" loading={save.isPending}>
            Salvar
          </Button>
        </>
      }
    >
      <form id="request-form" onSubmit={onSubmit} noValidate className="grid gap-4">
        {formError && (
          <div role="alert" className="rounded-xl bg-danger-soft px-4 py-3 text-sm text-danger">
            {formError}
          </div>
        )}
        <Field id="req-type" label="Tipo">
          <NativeSelect id="req-type" {...register('type')}>
            {Object.entries(REQUEST_TYPE_LABELS).map(([k, l]) => (
              <option key={k} value={k}>
                {l}
              </option>
            ))}
          </NativeSelect>
        </Field>
        <Field id="req-description" label="O que foi pedido" error={errors.description?.message}>
          <Input
            id="req-description"
            placeholder="Ex.: Histórico traduzido e apostilado"
            aria-invalid={!!errors.description}
            {...register('description')}
          />
        </Field>
        <div className="grid grid-cols-2 gap-4">
          <Field id="req-requested" label="Pedido em" error={errors.requestedOn?.message}>
            <Input id="req-requested" type="date" {...register('requestedOn')} />
          </Field>
          <Field id="req-due" label="Entregar até" error={errors.dueDate?.message}>
            <Input id="req-due" type="date" {...register('dueDate')} />
          </Field>
        </div>
      </form>
    </ResponsiveDialog>
  )
}
