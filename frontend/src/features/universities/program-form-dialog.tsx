import { zodResolver } from '@hookform/resolvers/zod'
import { Plus, Trash2 } from 'lucide-react'
import { useState, type ReactNode } from 'react'
import { useFieldArray, useForm } from 'react-hook-form'
import { toast } from 'sonner'
import type { Program, University } from '@/api/types'
import { DialogLoading } from '@/components/dialog-loading'
import { Button } from '@/components/ui/button'
import { Field } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { MoneyInput } from '@/components/ui/money-input'
import { ResponsiveDialog } from '@/components/ui/responsive-dialog'
import { NativeSelect } from '@/components/ui/select'
import { Textarea } from '@/components/ui/textarea'
import { applyServerErrors } from '@/features/auth/use-server-errors'
import { CHANNEL_LABELS, MONTH_NAMES, RATING_LABELS, REQUIREMENT_LABELS, TERM_LABELS } from '@/lib/labels'
import { useSaveProgram, useUniversities } from './api'
import { programSchema, toProgramForm, toProgramRequest, type ProgramForm as Form } from './program-form'

function Section({ title, children }: { title: string; children: ReactNode }) {
  return (
    <fieldset className="grid gap-4">
      <legend className="mb-1 text-xs font-semibold uppercase tracking-[0.12em] text-muted">{title}</legend>
      {children}
    </fieldset>
  )
}

function RequirementSelect({
  id,
  label,
  register,
}: {
  id: string
  label: string
  register: ReturnType<typeof useForm<Form>>['register']
}) {
  return (
    <Field id={`prog-${id}`} label={label}>
      <NativeSelect id={`prog-${id}`} {...register(id as 'gre')}>
        {Object.entries(REQUIREMENT_LABELS).map(([k, l]) => (
          <option key={k} value={k}>
            {k === 'UNKNOWN' ? 'Não sei' : l}
          </option>
        ))}
      </NativeSelect>
    </Field>
  )
}

interface ProgramFormDialogProps {
  program?: Program
  universityId?: string
  onClose: () => void
  onSaved?: (p: Program) => void
}

/** Montado só quando aberto; o formulário espera a lista de universidades (ver `DialogLoading`). */
export function ProgramFormDialog(props: ProgramFormDialogProps) {
  const universities = useUniversities()
  if (universities.data) return <ProgramForm {...props} universities={universities.data} />
  return (
    <DialogLoading
      title={props.program ? 'Editar programa' : 'Novo programa'}
      onClose={props.onClose}
      className="sm:w-[min(44rem,calc(100vw-2rem))]"
    />
  )
}

function ProgramForm({
  program,
  universityId,
  onClose,
  onSaved,
  universities,
}: ProgramFormDialogProps & { universities: University[] }) {
  const save = useSaveProgram()
  const [formError, setFormError] = useState('')
  const {
    register,
    control,
    handleSubmit,
    setError,
    formState: { errors },
  } = useForm<Form>({
    resolver: zodResolver(programSchema),
    defaultValues: toProgramForm(program, universityId),
  })
  const reqs = useFieldArray({ control, name: 'languageRequirements' })
  const intakes = useFieldArray({ control, name: 'intakes' })

  const onSubmit = handleSubmit((v) =>
    save.mutate(
      { id: program?.id, body: toProgramRequest(v) },
      {
        onSuccess: (saved) => {
          toast.success(program ? 'Programa atualizado.' : 'Programa criado.')
          onClose()
          onSaved?.(saved)
        },
        onError: (e) => setFormError(applyServerErrors(e, setError, ['name', 'universityId'])),
      },
    ),
  )

  const months = MONTH_NAMES.map((m, i) => (
    <option key={m} value={i + 1}>
      {m}
    </option>
  ))

  return (
    <ResponsiveDialog
      open
      onOpenChange={(o) => !o && onClose()}
      title={program ? 'Editar programa' : 'Novo programa'}
      className="sm:w-[min(44rem,calc(100vw-2rem))]"
      footer={
        <>
          <Button variant="outline" onClick={onClose}>
            Cancelar
          </Button>
          <Button type="submit" form="program-form" loading={save.isPending}>
            Salvar
          </Button>
        </>
      }
    >
      <form id="program-form" onSubmit={onSubmit} noValidate className="grid gap-7">
        {formError && (
          <div role="alert" className="rounded-xl bg-danger-soft px-4 py-3 text-sm text-danger">
            {formError}
          </div>
        )}
        <Section title="Geral">
          <Field id="prog-university" label="Universidade" error={errors.universityId?.message}>
            <NativeSelect
              id="prog-university"
              aria-invalid={!!errors.universityId}
              {...register('universityId')}
            >
              <option value="">Escolha…</option>
              {universities.map((u) => (
                <option key={u.id} value={u.id}>
                  {u.name}
                </option>
              ))}
            </NativeSelect>
          </Field>
          <div className="grid gap-4 sm:grid-cols-[1fr_8rem]">
            <Field id="prog-name" label="Programa" error={errors.name?.message}>
              <Input
                id="prog-name"
                placeholder="Ex.: Informatik"
                aria-invalid={!!errors.name}
                {...register('name')}
              />
            </Field>
            <Field id="prog-degree" label="Grau">
              <Input id="prog-degree" {...register('degree')} />
            </Field>
          </div>
          <Field id="prog-area" label="Área / foco">
            <Input
              id="prog-area"
              placeholder="Ex.: AI, sistemas distribuídos, segurança"
              {...register('area')}
            />
          </Field>
          <div className="grid gap-4 sm:grid-cols-3">
            <Field id="prog-lang" label="Idioma das aulas">
              <Input id="prog-lang" placeholder="Englisch" {...register('languageSummary')} />
            </Field>
            <Field id="prog-duration" label="Semestres" error={errors.durationSemesters?.message}>
              <Input id="prog-duration" inputMode="numeric" {...register('durationSemesters')} />
            </Field>
            <Field id="prog-admission" label="Chance de admissão">
              <NativeSelect id="prog-admission" {...register('admissionChance')}>
                <option value="">—</option>
                {Object.entries(RATING_LABELS).map(([k, l]) => (
                  <option key={k} value={k}>
                    {l}
                  </option>
                ))}
              </NativeSelect>
            </Field>
          </div>
          <Field id="prog-url" label="Página oficial">
            <Input id="prog-url" type="url" placeholder="https://" {...register('officialUrl')} />
          </Field>
        </Section>

        <Section title="Requisitos">
          <div className="grid gap-4 sm:grid-cols-4">
            <RequirementSelect id="gre" label="GRE" register={register} />
            <RequirementSelect id="aptitudeAssessment" label="Aptitude test" register={register} />
            <RequirementSelect id="interview" label="Entrevista" register={register} />
            <RequirementSelect id="nc" label="NC" register={register} />
          </div>
          <div className="grid gap-3">
            <p className="text-sm font-medium">Proficiência</p>
            {reqs.fields.map((f, i) => (
              <div
                key={f.id}
                className="grid grid-cols-2 gap-2 rounded-xl border border-border p-3 sm:grid-cols-[5rem_5rem_1fr_6rem_auto]"
              >
                <Input
                  aria-label="Idioma"
                  placeholder="en"
                  {...register(`languageRequirements.${i}.language`)}
                />
                <NativeSelect aria-label="Nível mínimo" {...register(`languageRequirements.${i}.minCefr`)}>
                  <option value="">—</option>
                  {['A1', 'A2', 'B1', 'B2', 'C1', 'C2'].map((c) => (
                    <option key={c} value={c}>
                      {c}
                    </option>
                  ))}
                </NativeSelect>
                <Input
                  aria-label="Teste"
                  placeholder="TOEFL iBT"
                  {...register(`languageRequirements.${i}.testName`)}
                />
                <Input
                  aria-label="Nota mínima"
                  placeholder="88"
                  inputMode="decimal"
                  {...register(`languageRequirements.${i}.minScore`)}
                />
                <Button
                  type="button"
                  variant="ghost"
                  size="icon"
                  aria-label="Remover requisito"
                  onClick={() => reqs.remove(i)}
                >
                  <Trash2 />
                </Button>
                {errors.languageRequirements?.[i] && (
                  <p className="col-span-full text-xs text-danger">
                    {errors.languageRequirements[i]?.language?.message ??
                      errors.languageRequirements[i]?.minScore?.message}
                  </p>
                )}
              </div>
            ))}
            <Button
              type="button"
              variant="outline"
              size="sm"
              className="justify-self-start"
              onClick={() =>
                reqs.append({ language: 'en', minCefr: 'B2', testName: '', minScore: '', rawText: '' })
              }
            >
              <Plus aria-hidden /> Requisito de idioma
            </Button>
          </div>
        </Section>

        <Section title="Inscrição e prazos">
          <div className="grid gap-4 sm:grid-cols-3">
            <Field id="prog-channel" label="Canal">
              <NativeSelect id="prog-channel" {...register('applicationChannel')}>
                <option value="">—</option>
                {Object.entries(CHANNEL_LABELS).map(([k, l]) => (
                  <option key={k} value={k}>
                    {l}
                  </option>
                ))}
              </NativeSelect>
            </Field>
            <Field id="prog-portal" label="Portal">
              <Input id="prog-portal" placeholder="C@MPUS, unisono…" {...register('portalName')} />
            </Field>
            <Field id="prog-docs" label="Documentos">
              <NativeSelect id="prog-docs" {...register('documentSubmission')}>
                <option value="">—</option>
                <option value="ONLINE">Online</option>
                <option value="PHYSICAL">Físico (correio)</option>
                <option value="BOTH">Online e físico</option>
              </NativeSelect>
            </Field>
          </div>
          <Field id="prog-deadline-text" label="Prazo (texto original)" hint="Ex.: 01/Dezembro - 01/Março">
            <Input id="prog-deadline-text" {...register('applicationDeadlineText')} />
          </Field>
          <div className="grid gap-3">
            <p className="text-sm font-medium">Janelas de inscrição (calculamos as datas de cada ano)</p>
            {intakes.fields.map((f, i) => (
              <div key={f.id} className="grid gap-2 rounded-xl border border-border p-3">
                <div className="grid grid-cols-[1fr_auto] gap-2">
                  <NativeSelect aria-label="Semestre" {...register(`intakes.${i}.term`)}>
                    {Object.entries(TERM_LABELS).map(([k, l]) => (
                      <option key={k} value={k}>
                        {l}
                      </option>
                    ))}
                  </NativeSelect>
                  <Button
                    type="button"
                    variant="ghost"
                    size="icon"
                    aria-label="Remover janela"
                    onClick={() => intakes.remove(i)}
                  >
                    <Trash2 />
                  </Button>
                </div>
                <div className="grid grid-cols-[4.5rem_1fr] gap-2 sm:grid-cols-[4.5rem_1fr_4.5rem_1fr]">
                  <Input
                    aria-label="Dia de abertura"
                    placeholder="Dia"
                    inputMode="numeric"
                    {...register(`intakes.${i}.openDay`)}
                  />
                  <NativeSelect aria-label="Mês de abertura" {...register(`intakes.${i}.openMonth`)}>
                    <option value="">Abre em…</option>
                    {months}
                  </NativeSelect>
                  <Input
                    aria-label="Dia do prazo"
                    placeholder="Dia"
                    inputMode="numeric"
                    {...register(`intakes.${i}.closeDay`)}
                  />
                  <NativeSelect aria-label="Mês do prazo" {...register(`intakes.${i}.closeMonth`)}>
                    <option value="">Prazo em…</option>
                    {months}
                  </NativeSelect>
                </div>
                <NativeSelect aria-label="Mês de início das aulas" {...register(`intakes.${i}.startMonth`)}>
                  {MONTH_NAMES.map((m, k) => (
                    <option key={m} value={k + 1}>
                      Aulas começam em {m.toLowerCase()}
                    </option>
                  ))}
                </NativeSelect>
                {(errors.intakes?.[i]?.openDay || errors.intakes?.[i]?.closeDay) && (
                  <p className="text-xs text-danger">Dia entre 1 e 31.</p>
                )}
              </div>
            ))}
            {intakes.fields.length < 4 && (
              <Button
                type="button"
                variant="outline"
                size="sm"
                className="justify-self-start"
                onClick={() =>
                  intakes.append({
                    term: 'SUMMER',
                    openDay: '',
                    openMonth: '',
                    closeDay: '',
                    closeMonth: '',
                    startMonth: '4',
                  })
                }
              >
                <Plus aria-hidden /> Janela de inscrição
              </Button>
            )}
          </div>
        </Section>

        <Section title="Custos e bolsa">
          <div className="grid gap-4 sm:grid-cols-3">
            <Field id="prog-fee" label="Taxa semestral" error={errors.semesterFee?.message}>
              <MoneyInput id="prog-fee" currency="EUR" {...register('semesterFee')} />
            </Field>
            <Field id="prog-tuition" label="Taxa de estrangeiro" error={errors.nonEuTuition?.message}>
              <MoneyInput id="prog-tuition" currency="EUR" {...register('nonEuTuition')} />
            </Field>
            <Field id="prog-tuition-period" label="Cobrada por">
              <NativeSelect id="prog-tuition-period" {...register('nonEuTuitionPeriod')}>
                <option value="SEMESTER">Semestre</option>
                <option value="YEAR">Ano</option>
                <option value="TOTAL">Curso inteiro</option>
              </NativeSelect>
            </Field>
            <Field id="prog-scholarship" label="Bolsa">
              <NativeSelect id="prog-scholarship" {...register('scholarshipAvailable')}>
                <option value="">Não sei</option>
                <option value="true">Tem</option>
                <option value="false">Não tem</option>
              </NativeSelect>
            </Field>
            <Field id="prog-scholarship-chance" label="Chance de bolsa">
              <NativeSelect id="prog-scholarship-chance" {...register('scholarshipChance')}>
                <option value="">—</option>
                {Object.entries(RATING_LABELS).map(([k, l]) => (
                  <option key={k} value={k}>
                    {l}
                  </option>
                ))}
              </NativeSelect>
            </Field>
            <Field id="prog-scholarship-details" label="Detalhes da bolsa">
              <Input
                id="prog-scholarship-details"
                placeholder="Deutschlandstipendium"
                {...register('scholarshipDetails')}
              />
            </Field>
          </div>
        </Section>

        <Field id="prog-notes" label="Observações">
          <Textarea id="prog-notes" rows={2} {...register('notes')} />
        </Field>
      </form>
    </ResponsiveDialog>
  )
}
