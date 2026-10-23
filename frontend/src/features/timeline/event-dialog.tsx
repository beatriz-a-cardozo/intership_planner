import { zodResolver } from '@hookform/resolvers/zod'
import { Trash2 } from 'lucide-react'
import { useState } from 'react'
import { Controller, useForm } from 'react-hook-form'
import { toast } from 'sonner'
import { z } from 'zod'
import { errorMessage } from '@/api/errors'
import type { EventType, Priority, TimelineItem } from '@/api/types'
import { Button } from '@/components/ui/button'
import { Field } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { ResponsiveDialog } from '@/components/ui/responsive-dialog'
import { NativeSelect } from '@/components/ui/select'
import { SwitchField } from '@/components/ui/switch'
import { Textarea } from '@/components/ui/textarea'
import { applyServerErrors } from '@/features/auth/use-server-errors'
import { todayIso } from '@/lib/format'
import { EVENT_TYPE_LABELS, PRIORITY_LABELS } from '@/lib/labels'
import { useDeleteEvent, useSaveEvent } from './api'

const schema = z
  .object({
    title: z.string().trim().min(1, 'Informe o título.').max(200),
    startDate: z.string().min(1, 'Informe a data.'),
    endDate: z.string(),
    type: z.enum(['MILESTONE', 'TRAVEL', 'EXAM', 'DOCUMENT', 'APPOINTMENT', 'PAYMENT', 'OTHER']),
    priority: z.enum(['LOW', 'MEDIUM', 'HIGH', 'URGENT']),
    done: z.boolean(),
    description: z.string().max(2000),
  })
  .refine((v) => !v.endDate || v.endDate >= v.startDate, {
    path: ['endDate'],
    message: 'O fim deve ser no mesmo dia ou depois do início.',
  })
type Form = z.infer<typeof schema>

/** Evento manual (montado só quando aberto). Itens automáticos não passam por aqui. */
export function EventDialog({ item, onClose }: { item?: TimelineItem; onClose: () => void }) {
  const save = useSaveEvent()
  const remove = useDeleteEvent()
  const [formError, setFormError] = useState('')
  const {
    register,
    control,
    handleSubmit,
    setError,
    formState: { errors },
  } = useForm<Form>({
    resolver: zodResolver(schema),
    defaultValues: {
      title: item?.title ?? '',
      startDate: item?.date ?? todayIso(),
      endDate: item?.endDate ?? '',
      type: item?.eventType ?? 'OTHER',
      priority: item?.priority ?? 'MEDIUM',
      done: item?.status === 'DONE',
      description: item?.description ?? '',
    },
  })

  const onSubmit = handleSubmit((v) =>
    save.mutate(
      {
        id: item?.sourceId,
        body: {
          title: v.title.trim(),
          startDate: v.startDate,
          endDate: v.endDate || undefined,
          type: v.type as EventType,
          priority: v.priority as Priority,
          done: v.done,
          description: v.description.trim() || undefined,
        },
      },
      {
        onSuccess: () => {
          toast.success(item ? 'Evento atualizado.' : 'Evento criado.')
          onClose()
        },
        onError: (e) => setFormError(applyServerErrors(e, setError, ['title', 'startDate', 'endDate'])),
      },
    ),
  )

  return (
    <ResponsiveDialog
      open
      onOpenChange={(o) => !o && onClose()}
      title={item ? 'Editar evento' : 'Novo evento'}
      description="Voo, prova, consulta no consulado… algo com data que não vem de outra tela."
      footer={
        <>
          {item && (
            <Button
              variant="ghost"
              className="mr-auto text-danger"
              loading={remove.isPending}
              onClick={() =>
                remove.mutate(item.sourceId, {
                  onSuccess: () => {
                    toast.success('Evento excluído.')
                    onClose()
                  },
                  onError: (e) => toast.error(errorMessage(e)),
                })
              }
            >
              <Trash2 aria-hidden /> Excluir
            </Button>
          )}
          <Button variant="outline" onClick={onClose}>
            Cancelar
          </Button>
          <Button type="submit" form="event-form" loading={save.isPending}>
            Salvar
          </Button>
        </>
      }
    >
      <form id="event-form" onSubmit={onSubmit} noValidate className="grid gap-4">
        {formError && (
          <div role="alert" className="rounded-xl bg-danger-soft px-4 py-3 text-sm text-danger">
            {formError}
          </div>
        )}
        <Field id="ev-title" label="Título" error={errors.title?.message}>
          <Input
            id="ev-title"
            placeholder="Ex.: Voo para Munique"
            aria-invalid={!!errors.title}
            {...register('title')}
          />
        </Field>
        <div className="grid grid-cols-2 gap-4">
          <Field id="ev-start" label="Data" error={errors.startDate?.message}>
            <Input id="ev-start" type="date" aria-invalid={!!errors.startDate} {...register('startDate')} />
          </Field>
          <Field id="ev-end" label="Até (opcional)" error={errors.endDate?.message}>
            <Input id="ev-end" type="date" aria-invalid={!!errors.endDate} {...register('endDate')} />
          </Field>
          <Field id="ev-type" label="Tipo">
            <NativeSelect id="ev-type" {...register('type')}>
              {Object.entries(EVENT_TYPE_LABELS).map(([k, l]) => (
                <option key={k} value={k}>
                  {l}
                </option>
              ))}
            </NativeSelect>
          </Field>
          <Field id="ev-priority" label="Prioridade">
            <NativeSelect id="ev-priority" {...register('priority')}>
              {Object.entries(PRIORITY_LABELS).map(([k, l]) => (
                <option key={k} value={k}>
                  {l}
                </option>
              ))}
            </NativeSelect>
          </Field>
        </div>
        <Field id="ev-description" label="Detalhes">
          <Textarea id="ev-description" rows={2} {...register('description')} />
        </Field>
        <Controller
          control={control}
          name="done"
          render={({ field }) => (
            <SwitchField
              id="ev-done"
              label="Já aconteceu / feito"
              checked={field.value}
              onCheckedChange={field.onChange}
            />
          )}
        />
      </form>
    </ResponsiveDialog>
  )
}
