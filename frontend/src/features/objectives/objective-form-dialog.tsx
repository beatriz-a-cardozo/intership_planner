import { zodResolver } from '@hookform/resolvers/zod'
import { useState } from 'react'
import { Controller, useForm, useWatch } from 'react-hook-form'
import { toast } from 'sonner'
import { z } from 'zod'
import type { Objective, ObjectiveRequest } from '@/api/types'
import { Button } from '@/components/ui/button'
import { Field } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { MoneyInput } from '@/components/ui/money-input'
import { ResponsiveDialog } from '@/components/ui/responsive-dialog'
import { SegmentedControl } from '@/components/ui/segmented'
import { NativeSelect } from '@/components/ui/select'
import { Textarea } from '@/components/ui/textarea'
import { applyServerErrors } from '@/features/auth/use-server-errors'
import { decimalToInput, isValidDecimal, parseDecimal } from '@/lib/decimal'
import { COUNTRIES, EXPENSE_SCOPE_LABELS } from '@/lib/labels'
import { useSaveObjective } from './api'

const money = z.string().refine((v) => isValidDecimal(v, { allowEmpty: true }), 'Valor inválido.')

const schema = z
  .object({
    name: z.string().trim().min(2, 'Informe o nome.').max(120),
    description: z.string().max(1000),
    countryCode: z.string(),
    city: z.string().max(120),
    startDate: z.string(),
    targetDate: z.string().min(1, 'Informe a data alvo.'),
    currency: z.enum(['BRL', 'EUR']),
    targetMode: z.enum(['DERIVED', 'MANUAL']),
    targetAmount: money,
    plannedMonthlySaving: money,
    horizonMonths: z.string().refine((v) => /^\d+$/.test(v) && +v >= 1 && +v <= 120, 'Entre 1 e 120 meses.'),
    expenseScope: z.enum(['REQUIRED_ONLY', 'ALL']),
    status: z.enum(['ACTIVE', 'COMPLETED', 'ARCHIVED']),
  })
  .refine(
    (v) =>
      v.targetMode === 'DERIVED' ||
      (v.targetAmount.trim() !== '' && (parseDecimal(v.targetAmount) ?? -1) >= 0),
    {
      path: ['targetAmount'],
      message: 'Informe o valor da meta.',
    },
  )
  .refine((v) => !v.startDate || v.startDate <= v.targetDate, {
    path: ['startDate'],
    message: 'O início deve ser antes da data alvo.',
  })

type Form = z.infer<typeof schema>

function toForm(o?: Objective): Form {
  return {
    name: o?.name ?? '',
    description: o?.description ?? '',
    countryCode: o?.countryCode ?? 'DE',
    city: o?.city ?? '',
    startDate: o?.startDate ?? '',
    targetDate: o?.targetDate ?? '',
    currency: (o?.currency as 'BRL' | 'EUR') ?? 'BRL',
    targetMode: o?.targetMode ?? 'DERIVED',
    targetAmount: decimalToInput(o?.targetAmount),
    plannedMonthlySaving: decimalToInput(o?.plannedMonthlySaving),
    horizonMonths: String(o?.horizonMonths ?? 24),
    expenseScope: o?.expenseScope ?? 'REQUIRED_ONLY',
    status: o?.status ?? 'ACTIVE',
  }
}

function toRequest(v: Form): ObjectiveRequest {
  return {
    name: v.name.trim(),
    description: v.description.trim() || undefined,
    countryCode: v.countryCode || undefined,
    city: v.city.trim() || undefined,
    startDate: v.startDate || undefined,
    targetDate: v.targetDate,
    currency: v.currency,
    targetMode: v.targetMode,
    targetAmount: v.targetMode === 'MANUAL' ? (parseDecimal(v.targetAmount) ?? undefined) : undefined,
    plannedMonthlySaving: parseDecimal(v.plannedMonthlySaving) ?? undefined,
    horizonMonths: Number(v.horizonMonths),
    expenseScope: v.expenseScope,
    status: v.status,
  }
}

interface Props {
  open: boolean
  onOpenChange: (open: boolean) => void
  objective?: Objective
  onSaved?: (o: Objective) => void
}

/** Montado só quando aberto (o pai renderiza condicionalmente). */
export function ObjectiveFormDialog({ open, onOpenChange, objective, onSaved }: Props) {
  const save = useSaveObjective()
  const [formError, setFormError] = useState('')
  const {
    register,
    control,
    handleSubmit,
    setError,
    formState: { errors },
  } = useForm<Form>({ resolver: zodResolver(schema), defaultValues: toForm(objective) })

  const targetMode = useWatch({ control, name: 'targetMode' })
  const currency = useWatch({ control, name: 'currency' })

  const onSubmit = handleSubmit((values) =>
    save.mutate(
      { id: objective?.id, body: toRequest(values) },
      {
        onSuccess: (saved) => {
          toast.success(objective ? 'Objetivo atualizado.' : 'Objetivo criado.')
          onOpenChange(false)
          onSaved?.(saved)
        },
        onError: (error) =>
          setFormError(applyServerErrors(error, setError, Object.keys(values) as (keyof Form)[])),
      },
    ),
  )

  return (
    <ResponsiveDialog
      open={open}
      onOpenChange={onOpenChange}
      title={objective ? 'Editar objetivo' : 'Novo objetivo'}
      footer={
        <>
          <Button variant="outline" onClick={() => onOpenChange(false)}>
            Cancelar
          </Button>
          <Button type="submit" form="objective-form" loading={save.isPending}>
            Salvar
          </Button>
        </>
      }
    >
      <form id="objective-form" onSubmit={onSubmit} noValidate className="grid gap-4">
        {formError && (
          <div role="alert" className="rounded-xl bg-danger-soft px-4 py-3 text-sm text-danger">
            {formError}
          </div>
        )}
        <Field id="name" label="Nome" error={errors.name?.message}>
          <Input id="name" aria-invalid={!!errors.name} {...register('name')} />
        </Field>
        <div className="grid gap-4 sm:grid-cols-2">
          <Field id="countryCode" label="País">
            <NativeSelect id="countryCode" {...register('countryCode')}>
              {COUNTRIES.map((c) => (
                <option key={c.code} value={c.code}>
                  {c.name}
                </option>
              ))}
            </NativeSelect>
          </Field>
          <Field id="city" label="Cidade" error={errors.city?.message}>
            <Input id="city" {...register('city')} />
          </Field>
          <Field id="startDate" label="Início (opcional)" error={errors.startDate?.message}>
            <Input id="startDate" type="date" aria-invalid={!!errors.startDate} {...register('startDate')} />
          </Field>
          <Field id="targetDate" label="Data alvo" error={errors.targetDate?.message}>
            <Input
              id="targetDate"
              type="date"
              aria-invalid={!!errors.targetDate}
              {...register('targetDate')}
            />
          </Field>
        </div>

        <div className="grid gap-1.5">
          <span className="text-sm font-medium">Moeda do objetivo</span>
          <Controller
            control={control}
            name="currency"
            render={({ field }) => (
              <SegmentedControl
                ariaLabel="Moeda do objetivo"
                value={field.value}
                onValueChange={field.onChange}
                options={[
                  { value: 'BRL', label: 'Real (R$)' },
                  { value: 'EUR', label: 'Euro (€)' },
                ]}
              />
            )}
          />
        </div>

        <div className="grid gap-1.5">
          <span className="text-sm font-medium">Meta</span>
          <Controller
            control={control}
            name="targetMode"
            render={({ field }) => (
              <SegmentedControl
                ariaLabel="Como definir a meta"
                value={field.value}
                onValueChange={field.onChange}
                options={[
                  { value: 'DERIVED', label: 'Pelos gastos' },
                  { value: 'MANUAL', label: 'Valor fixo' },
                ]}
              />
            )}
          />
        </div>
        {targetMode === 'MANUAL' ? (
          <Field id="targetAmount" label="Valor da meta" error={errors.targetAmount?.message}>
            <MoneyInput
              id="targetAmount"
              currency={currency}
              aria-invalid={!!errors.targetAmount}
              {...register('targetAmount')}
            />
          </Field>
        ) : (
          <Field id="expenseScope" label="Gastos que compõem a meta">
            <NativeSelect id="expenseScope" {...register('expenseScope')}>
              {Object.entries(EXPENSE_SCOPE_LABELS).map(([value, label]) => (
                <option key={value} value={value}>
                  {label}
                </option>
              ))}
            </NativeSelect>
          </Field>
        )}

        <div className="grid gap-4 sm:grid-cols-2">
          <Field
            id="plannedMonthlySaving"
            label="Economia mensal planejada"
            error={errors.plannedMonthlySaving?.message}
          >
            <MoneyInput id="plannedMonthlySaving" currency={currency} {...register('plannedMonthlySaving')} />
          </Field>
          <Field
            id="horizonMonths"
            label="Duração (meses)"
            error={errors.horizonMonths?.message}
            hint="Base das ocorrências: mestrado de 2 anos = 24."
          >
            <Input
              id="horizonMonths"
              inputMode="numeric"
              aria-invalid={!!errors.horizonMonths}
              {...register('horizonMonths')}
            />
          </Field>
        </div>

        {objective && (
          <Field id="status" label="Situação">
            <NativeSelect id="status" {...register('status')}>
              <option value="ACTIVE">Ativo</option>
              <option value="COMPLETED">Concluído</option>
              <option value="ARCHIVED">Arquivado</option>
            </NativeSelect>
          </Field>
        )}

        <Field id="description" label="Descrição (opcional)" error={errors.description?.message}>
          <Textarea id="description" rows={3} {...register('description')} />
        </Field>
      </form>
    </ResponsiveDialog>
  )
}
