import { zodResolver } from '@hookform/resolvers/zod'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { toast } from 'sonner'
import { z } from 'zod'
import type { Application, Objective, Priority, Task, TaskStatus, University } from '@/api/types'
import { DialogLoading } from '@/components/dialog-loading'
import { Button } from '@/components/ui/button'
import { Field } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { ResponsiveDialog } from '@/components/ui/responsive-dialog'
import { NativeSelect } from '@/components/ui/select'
import { Textarea } from '@/components/ui/textarea'
import { applyServerErrors } from '@/features/auth/use-server-errors'
import { useObjectives } from '@/features/objectives/api'
import { useApplications, useUniversities } from '@/features/universities/api'
import { PRIORITY_LABELS, TASK_STATUS_LABELS } from '@/lib/labels'
import { useSaveTask } from './api'

const schema = z.object({
  title: z.string().trim().min(1, 'Informe a tarefa.').max(200),
  description: z.string().max(2000),
  priority: z.enum(['LOW', 'MEDIUM', 'HIGH', 'URGENT']),
  status: z.enum(['TODO', 'IN_PROGRESS', 'DONE']),
  dueDate: z.string(),
  category: z.string().max(40),
  objectiveId: z.string(),
  universityId: z.string(),
  applicationId: z.string(),
})
type Form = z.infer<typeof schema>

interface Props {
  task?: Task
  onClose: () => void
}

/** Montado só quando aberto; o formulário espera as listas dos vínculos (ver `DialogLoading`). */
export function TaskDialog(props: Props) {
  const objectives = useObjectives()
  const universities = useUniversities()
  const applications = useApplications()
  if (objectives.data && universities.data && applications.data) {
    return (
      <TaskForm
        {...props}
        objectives={objectives.data}
        universities={universities.data}
        applications={applications.data}
      />
    )
  }
  return <DialogLoading title={props.task ? 'Editar tarefa' : 'Nova tarefa'} onClose={props.onClose} />
}

function TaskForm({
  task: t,
  onClose,
  objectives,
  universities,
  applications,
}: Props & { objectives: Objective[]; universities: University[]; applications: Application[] }) {
  const save = useSaveTask()
  const [formError, setFormError] = useState('')
  const primary = objectives.find((o) => o.primary)
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors },
  } = useForm<Form>({
    resolver: zodResolver(schema),
    defaultValues: {
      title: t?.title ?? '',
      description: t?.description ?? '',
      priority: t?.priority ?? 'MEDIUM',
      status: t?.status ?? 'TODO',
      dueDate: t?.dueDate ?? '',
      category: t?.category ?? '',
      objectiveId: t ? (t.objective?.id ?? '') : (primary?.id ?? ''),
      universityId: t?.university?.id ?? '',
      applicationId: t?.application?.id ?? '',
    },
  })

  const onSubmit = handleSubmit((v) =>
    save.mutate(
      {
        id: t?.id,
        body: {
          title: v.title.trim(),
          description: v.description.trim() || undefined,
          priority: v.priority as Priority,
          status: v.status as TaskStatus,
          dueDate: v.dueDate || undefined,
          category: v.category.trim() || undefined,
          objectiveId: v.objectiveId || undefined,
          universityId: v.universityId || undefined,
          applicationId: v.applicationId || undefined,
          // o vínculo com gasto é mantido como está (não é editável aqui)
          expenseId: t?.expense?.id ?? undefined,
        },
      },
      {
        onSuccess: () => {
          toast.success(t ? 'Tarefa atualizada.' : 'Tarefa criada.')
          onClose()
        },
        onError: (e) =>
          setFormError(
            applyServerErrors(e, setError, [
              'title',
              'dueDate',
              'objectiveId',
              'universityId',
              'applicationId',
            ]),
          ),
      },
    ),
  )

  return (
    <ResponsiveDialog
      open
      onOpenChange={(o) => !o && onClose()}
      title={t ? 'Editar tarefa' : 'Nova tarefa'}
      footer={
        <>
          <Button variant="outline" onClick={onClose}>
            Cancelar
          </Button>
          <Button type="submit" form="task-form" loading={save.isPending}>
            Salvar
          </Button>
        </>
      }
    >
      <form id="task-form" onSubmit={onSubmit} noValidate className="grid gap-4">
        {formError && (
          <div role="alert" className="rounded-xl bg-danger-soft px-4 py-3 text-sm text-danger">
            {formError}
          </div>
        )}
        <Field id="task-title" label="Tarefa" error={errors.title?.message}>
          <Input
            id="task-title"
            placeholder="Ex.: Traduzir o histórico"
            aria-invalid={!!errors.title}
            {...register('title')}
          />
        </Field>
        <div className="grid gap-4 sm:grid-cols-3">
          <Field id="task-due" label="Prazo" error={errors.dueDate?.message}>
            <Input id="task-due" type="date" {...register('dueDate')} />
          </Field>
          <Field id="task-priority" label="Prioridade">
            <NativeSelect id="task-priority" {...register('priority')}>
              {Object.entries(PRIORITY_LABELS).map(([k, l]) => (
                <option key={k} value={k}>
                  {l}
                </option>
              ))}
            </NativeSelect>
          </Field>
          <Field id="task-status" label="Status">
            <NativeSelect id="task-status" {...register('status')}>
              {Object.entries(TASK_STATUS_LABELS).map(([k, l]) => (
                <option key={k} value={k}>
                  {l}
                </option>
              ))}
            </NativeSelect>
          </Field>
        </div>
        <Field id="task-category" label="Categoria" hint="Livre: documentos, idioma, moradia…">
          <Input id="task-category" {...register('category')} />
        </Field>
        <fieldset className="grid gap-4 sm:grid-cols-3">
          <legend className="mb-1 text-sm font-medium">Relacionada a (opcional)</legend>
          <Field id="task-objective" label="Objetivo">
            <NativeSelect id="task-objective" {...register('objectiveId')}>
              <option value="">Nenhum</option>
              {objectives.map((o) => (
                <option key={o.id} value={o.id}>
                  {o.name}
                </option>
              ))}
            </NativeSelect>
          </Field>
          <Field id="task-university" label="Universidade">
            <NativeSelect id="task-university" {...register('universityId')}>
              <option value="">Nenhuma</option>
              {universities.map((u) => (
                <option key={u.id} value={u.id}>
                  {u.name}
                </option>
              ))}
            </NativeSelect>
          </Field>
          <Field id="task-application" label="Candidatura">
            <NativeSelect id="task-application" {...register('applicationId')}>
              <option value="">Nenhuma</option>
              {applications.map((a) => (
                <option key={a.id} value={a.id}>
                  {a.program.name} · {a.program.universityName}
                </option>
              ))}
            </NativeSelect>
          </Field>
        </fieldset>
        <Field id="task-description" label="Detalhes">
          <Textarea id="task-description" rows={3} {...register('description')} />
        </Field>
      </form>
    </ResponsiveDialog>
  )
}
