import { zodResolver } from '@hookform/resolvers/zod'
import { useQuery } from '@tanstack/react-query'
import { Calculator, Loader2 } from 'lucide-react'
import { useState } from 'react'
import { Controller, useForm, useWatch } from 'react-hook-form'
import { toast } from 'sonner'
import type { Expense, Objective, Recurrence } from '@/api/types'
import { Button } from '@/components/ui/button'
import { describedBy, Field } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { MoneyInput } from '@/components/ui/money-input'
import { ResponsiveDialog } from '@/components/ui/responsive-dialog'
import { SegmentedControl } from '@/components/ui/segmented'
import { NativeSelect } from '@/components/ui/select'
import { SwitchField } from '@/components/ui/switch'
import { Textarea } from '@/components/ui/textarea'
import { applyServerErrors } from '@/features/auth/use-server-errors'
import { useDebouncedValue } from '@/hooks/use-debounced-value'
import { formatMoney, pluralize } from '@/lib/format'
import { RECURRENCE_HINTS, RECURRENCE_LABELS } from '@/lib/labels'
import { previewTotal, useCatalog, useSaveExpense } from './api'
import {
  expenseSchema,
  SINGLE_OCCURRENCE,
  toExpenseForm,
  toExpenseRequest,
  toPreviewRequest,
  type ExpenseForm,
} from './expense-form'

interface Props {
  objective: Objective
  expense?: Expense
  onOpenChange: (open: boolean) => void
}

/** Montado só quando aberto (o pai renderiza condicionalmente). */
export function ExpenseFormDialog({ objective, expense, onOpenChange }: Props) {
  const categories = useCatalog('categories')
  const cycles = useCatalog('cycles')
  const save = useSaveExpense()
  const [formError, setFormError] = useState('')

  const activeCategories = (categories.data ?? []).filter((c) => !c.archived || c.id === expense?.category.id)
  const activeCycles = (cycles.data ?? []).filter((c) => !c.archived || c.id === expense?.cycle?.id)

  const {
    register,
    control,
    handleSubmit,
    setError,
    formState: { errors },
  } = useForm<ExpenseForm>({
    resolver: zodResolver(expenseSchema),
    defaultValues: toExpenseForm(expense, { currency: objective.currency }),
  })

  const [currency, unitPrice, quantity, recurrence, occurrences] = useWatch({
    control,
    name: ['currency', 'unitPrice', 'quantity', 'recurrence', 'occurrences'],
  })
  const previewRequest = useDebouncedValue(
    toPreviewRequest({ currency, unitPrice, quantity, recurrence, occurrences }, objective.id),
  )
  const preview = useQuery({
    queryKey: ['expenses', 'preview', previewRequest],
    queryFn: () => previewTotal(previewRequest!),
    enabled: previewRequest !== null,
    retry: false,
    staleTime: 60_000,
  })

  const single = SINGLE_OCCURRENCE.includes(recurrence)

  const onSubmit = handleSubmit((values) =>
    save.mutate(
      { id: expense?.id, body: toExpenseRequest(values, objective.id) },
      {
        onSuccess: () => {
          toast.success(expense ? 'Gasto atualizado.' : 'Gasto adicionado.')
          onOpenChange(false)
        },
        onError: (e) =>
          setFormError(applyServerErrors(e, setError, Object.keys(values) as (keyof ExpenseForm)[])),
      },
    ),
  )

  return (
    <ResponsiveDialog
      open
      onOpenChange={onOpenChange}
      title={expense ? 'Editar gasto' : 'Novo gasto'}
      description={`Objetivo: ${objective.name}`}
      footer={
        <>
          <Button variant="outline" onClick={() => onOpenChange(false)}>
            Cancelar
          </Button>
          <Button type="submit" form="expense-form" loading={save.isPending}>
            Salvar
          </Button>
        </>
      }
    >
      <form id="expense-form" onSubmit={onSubmit} noValidate className="grid gap-4">
        {formError && (
          <div role="alert" className="rounded-xl bg-danger-soft px-4 py-3 text-sm text-danger">
            {formError}
          </div>
        )}

        <Field id="expense-name" label="Gasto" error={errors.name?.message}>
          <Input
            id="expense-name"
            autoFocus={!expense}
            placeholder="Ex.: Aluguel, passagem aérea, TOEFL"
            aria-invalid={!!errors.name}
            aria-describedby={describedBy('expense-name', errors.name?.message)}
            {...register('name')}
          />
        </Field>

        <div className="grid gap-4 sm:grid-cols-2">
          <Field id="expense-category" label="Categoria" error={errors.categoryId?.message}>
            <NativeSelect
              id="expense-category"
              aria-invalid={!!errors.categoryId}
              {...register('categoryId')}
            >
              <option value="">Escolha…</option>
              {activeCategories.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.name}
                </option>
              ))}
            </NativeSelect>
          </Field>
          <Field id="expense-cycle" label="Ciclo">
            <NativeSelect id="expense-cycle" {...register('cycleId')}>
              <option value="">Sem ciclo</option>
              {activeCycles.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.name}
                </option>
              ))}
            </NativeSelect>
          </Field>
        </div>

        <div className="grid gap-1.5">
          <span className="text-sm font-medium">Moeda do gasto</span>
          <Controller
            control={control}
            name="currency"
            render={({ field }) => (
              <SegmentedControl
                ariaLabel="Moeda do gasto"
                value={field.value}
                onValueChange={field.onChange}
                options={[
                  { value: 'BRL', label: 'Real (R$)' },
                  { value: 'EUR', label: 'Euro (€)' },
                ]}
              />
            )}
          />
          <p className="text-xs text-muted">
            Guardamos o valor na moeda original; a conversão usa a cotação do dia.
          </p>
        </div>

        <div className="grid gap-4 sm:grid-cols-2">
          <Field id="expense-recurrence" label="Periodicidade" hint={RECURRENCE_HINTS[recurrence]}>
            <NativeSelect id="expense-recurrence" {...register('recurrence')}>
              {(Object.keys(RECURRENCE_LABELS) as Recurrence[]).map((r) => (
                <option key={r} value={r}>
                  {RECURRENCE_LABELS[r]}
                </option>
              ))}
            </NativeSelect>
          </Field>
          <Field
            id="expense-price"
            label={recurrence === 'PER_UNIT' ? 'Preço por unidade' : 'Valor'}
            error={errors.unitPrice?.message}
          >
            <MoneyInput
              id="expense-price"
              currency={currency}
              placeholder="0,00"
              aria-invalid={!!errors.unitPrice}
              {...register('unitPrice')}
            />
          </Field>
          <Field id="expense-quantity" label="Quantidade" error={errors.quantity?.message} hint="Padrão: 1.">
            <Input
              id="expense-quantity"
              inputMode="numeric"
              placeholder="1"
              aria-invalid={!!errors.quantity}
              {...register('quantity')}
            />
          </Field>
          {!single && (
            <Field
              id="expense-occurrences"
              label="Ocorrências"
              error={errors.occurrences?.message}
              hint={
                recurrence === 'CUSTOM'
                  ? 'Obrigatório no personalizado.'
                  : `Vazio = sugestão pelo objetivo (${objective.horizonMonths} meses).`
              }
            >
              <Input
                id="expense-occurrences"
                inputMode="numeric"
                placeholder={
                  preview.data?.suggestedOccurrences != null ? String(preview.data.suggestedOccurrences) : ''
                }
                aria-invalid={!!errors.occurrences}
                {...register('occurrences')}
              />
            </Field>
          )}
        </div>

        {/* Total calculado no backend (mesma regra do salvamento) */}
        <div
          className="flex items-center gap-3 rounded-xl bg-primary-soft px-4 py-3 text-sm"
          aria-live="polite"
        >
          {preview.isFetching ? (
            <Loader2 className="size-4 animate-spin text-primary" aria-hidden />
          ) : (
            <Calculator className="size-4 text-primary" aria-hidden />
          )}
          {preview.data && previewRequest ? (
            <span>
              <span className="text-muted">
                {pluralize(preview.data.occurrences, 'ocorrência', 'ocorrências')} · total{' '}
              </span>
              <span className="font-semibold tabular">{formatMoney(preview.data.total)}</span>
              {preview.data.total.currency !== preview.data.totalConverted.currency && (
                <span className="text-muted tabular"> ≈ {formatMoney(preview.data.totalConverted)}</span>
              )}
            </span>
          ) : (
            <span className="text-muted">Informe valor e periodicidade para ver o total.</span>
          )}
        </div>

        <div className="grid gap-4 sm:grid-cols-2">
          <Field id="expense-due" label={single ? 'Data (opcional)' : 'Início (opcional)'}>
            <Input id="expense-due" type="date" {...register('dueDate')} />
          </Field>
          {!single && (
            <Field id="expense-end" label="Fim (opcional)" error={errors.endDate?.message}>
              <Input id="expense-end" type="date" aria-invalid={!!errors.endDate} {...register('endDate')} />
            </Field>
          )}
        </div>

        <div className="grid divide-y divide-border rounded-xl border border-border px-4">
          <Controller
            control={control}
            name="required"
            render={({ field }) => (
              <SwitchField
                id="expense-required"
                label="Obrigatório"
                hint="Entra na meta quando o objetivo considera só obrigatórios."
                checked={field.value}
                onCheckedChange={field.onChange}
              />
            )}
          />
          <Controller
            control={control}
            name="refundable"
            render={({ field }) => (
              <SwitchField
                id="expense-refundable"
                label="Reembolsável"
                hint="Ex.: caução (Kaution). Apenas informativo."
                checked={field.value}
                onCheckedChange={field.onChange}
              />
            )}
          />
        </div>

        <Field id="expense-notes" label="Observações (opcional)" error={errors.notes?.message}>
          <Textarea id="expense-notes" rows={2} placeholder="Ex.: parcelado em 10x" {...register('notes')} />
        </Field>
      </form>
    </ResponsiveDialog>
  )
}
