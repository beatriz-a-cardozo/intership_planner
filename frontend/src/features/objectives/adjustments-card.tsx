import { zodResolver } from '@hookform/resolvers/zod'
import { Pencil, Plus, Scale, Trash2 } from 'lucide-react'
import { useState } from 'react'
import { Controller, useForm, useWatch } from 'react-hook-form'
import { toast } from 'sonner'
import { z } from 'zod'
import { errorMessage } from '@/api/errors'
import type { Adjustment } from '@/api/types'
import { ConfirmDialog } from '@/components/confirm-dialog'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Field } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { MoneyInput } from '@/components/ui/money-input'
import { ResponsiveDialog } from '@/components/ui/responsive-dialog'
import { SegmentedControl } from '@/components/ui/segmented'
import { Skeleton } from '@/components/ui/skeleton'
import { applyServerErrors } from '@/features/auth/use-server-errors'
import { decimalToInput, isValidDecimal, parseDecimal } from '@/lib/decimal'
import { formatMoney } from '@/lib/format'
import { useAdjustments, useDeleteAdjustment, useSaveAdjustment } from './api'

const schema = z.object({
  label: z.string().trim().min(1, 'Descreva o ajuste.').max(120),
  direction: z.enum(['reduce', 'increase']),
  amount: z
    .string()
    .refine((v) => isValidDecimal(v) && (parseDecimal(v) ?? 0) > 0, 'Informe um valor maior que zero.'),
  currency: z.enum(['BRL', 'EUR']),
})
type Form = z.infer<typeof schema>

function toForm(a: Adjustment | undefined, currency: string): Form {
  return {
    label: a?.label ?? '',
    direction: a && a.amount.amount > 0 ? 'increase' : 'reduce',
    amount: a ? decimalToInput(Math.abs(a.amount.amount)) : '',
    currency: (a?.amount.currency ?? currency) as 'BRL' | 'EUR',
  }
}

/**
 * Ajustes manuais e visíveis da necessidade planejada (ex.: "Sperrkonto −R$ 75.000"), aplicados uma única vez.
 * Substitui o "−75000" escondido nas fórmulas da planilha.
 */
export function AdjustmentsCard({ objectiveId, currency }: { objectiveId: string; currency: string }) {
  const adjustments = useAdjustments(objectiveId)
  const remove = useDeleteAdjustment(objectiveId)
  const [editing, setEditing] = useState<Adjustment | 'new' | null>(null)
  const [deleting, setDeleting] = useState<Adjustment | null>(null)

  return (
    <Card>
      <CardHeader>
        <div>
          <CardTitle>Ajustes da meta</CardTitle>
          <CardDescription>
            Valores somados ou descontados uma única vez (ex.: Sperrkonto, reembolsos).
          </CardDescription>
        </div>
        <Button variant="outline" size="sm" onClick={() => setEditing('new')}>
          <Plus aria-hidden /> Ajuste
        </Button>
      </CardHeader>
      <CardContent>
        {adjustments.isPending ? (
          <Skeleton className="h-12" />
        ) : !adjustments.data?.length ? (
          <p className="flex items-center gap-2 text-sm text-muted">
            <Scale className="size-4" aria-hidden /> Nenhum ajuste. Os gastos entram na meta como estão.
          </p>
        ) : (
          <ul className="divide-y divide-border">
            {adjustments.data.map((a) => (
              <li key={a.id} className="flex items-center justify-between gap-3 py-2.5">
                <span className="min-w-0 truncate text-sm">{a.label}</span>
                <span className="flex shrink-0 items-center gap-1">
                  <span
                    className={`text-sm font-medium tabular ${a.amount.amount < 0 ? 'text-primary' : 'text-accent'}`}
                  >
                    {a.amount.amount > 0 ? '+' : ''}
                    {formatMoney(a.amount)}
                  </span>
                  <Button
                    variant="ghost"
                    size="icon"
                    aria-label={`Editar ${a.label}`}
                    onClick={() => setEditing(a)}
                  >
                    <Pencil />
                  </Button>
                  <Button
                    variant="ghost"
                    size="icon"
                    aria-label={`Excluir ${a.label}`}
                    onClick={() => setDeleting(a)}
                  >
                    <Trash2 />
                  </Button>
                </span>
              </li>
            ))}
          </ul>
        )}
      </CardContent>

      {editing !== null && (
        <AdjustmentDialog
          objectiveId={objectiveId}
          currency={currency}
          adjustment={editing === 'new' ? undefined : editing}
          open
          onOpenChange={(o) => !o && setEditing(null)}
        />
      )}
      <ConfirmDialog
        open={deleting !== null}
        onOpenChange={(o) => !o && setDeleting(null)}
        title="Excluir ajuste?"
        description={`"${deleting?.label}" deixará de afetar a meta.`}
        confirmLabel="Excluir"
        destructive
        loading={remove.isPending}
        onConfirm={() =>
          deleting &&
          remove.mutate(deleting.id, {
            onSuccess: () => setDeleting(null),
            onError: (e) => toast.error(errorMessage(e)),
          })
        }
      />
    </Card>
  )
}

function AdjustmentDialog({
  objectiveId,
  currency,
  adjustment,
  open,
  onOpenChange,
}: {
  objectiveId: string
  currency: string
  adjustment?: Adjustment
  open: boolean
  onOpenChange: (open: boolean) => void
}) {
  const save = useSaveAdjustment(objectiveId)
  const [formError, setFormError] = useState('')
  const {
    register,
    control,
    handleSubmit,
    setError,
    formState: { errors },
  } = useForm<Form>({ resolver: zodResolver(schema), defaultValues: toForm(adjustment, currency) })

  const selectedCurrency = useWatch({ control, name: 'currency' })

  const onSubmit = handleSubmit((v) => {
    const value = parseDecimal(v.amount) ?? 0
    save.mutate(
      {
        id: adjustment?.id,
        body: {
          label: v.label.trim(),
          amount: v.direction === 'reduce' ? -value : value,
          currency: v.currency,
        },
      },
      {
        onSuccess: () => onOpenChange(false),
        onError: (e) => setFormError(applyServerErrors(e, setError, ['label', 'amount', 'currency'])),
      },
    )
  })

  return (
    <ResponsiveDialog
      open={open}
      onOpenChange={onOpenChange}
      title={adjustment ? 'Editar ajuste' : 'Novo ajuste'}
      footer={
        <>
          <Button variant="outline" onClick={() => onOpenChange(false)}>
            Cancelar
          </Button>
          <Button type="submit" form="adjustment-form" loading={save.isPending}>
            Salvar
          </Button>
        </>
      }
    >
      <form id="adjustment-form" onSubmit={onSubmit} noValidate className="grid gap-4">
        {formError && (
          <div role="alert" className="rounded-xl bg-danger-soft px-4 py-3 text-sm text-danger">
            {formError}
          </div>
        )}
        <Field id="label" label="Descrição" error={errors.label?.message}>
          <Input
            id="label"
            placeholder="Ex.: Sperrkonto (fica bloqueado, volta mês a mês)"
            {...register('label')}
          />
        </Field>
        <Controller
          control={control}
          name="direction"
          render={({ field }) => (
            <SegmentedControl
              ariaLabel="Efeito do ajuste"
              value={field.value}
              onValueChange={field.onChange}
              options={[
                { value: 'reduce', label: 'Reduz a meta' },
                { value: 'increase', label: 'Aumenta a meta' },
              ]}
            />
          )}
        />
        <div className="grid grid-cols-[1fr_auto] items-end gap-3">
          <Field id="amount" label="Valor" error={errors.amount?.message}>
            <MoneyInput
              id="amount"
              currency={selectedCurrency}
              aria-invalid={!!errors.amount}
              {...register('amount')}
            />
          </Field>
          <Controller
            control={control}
            name="currency"
            render={({ field }) => (
              <SegmentedControl
                ariaLabel="Moeda do ajuste"
                value={field.value}
                onValueChange={field.onChange}
                options={[
                  { value: 'BRL', label: 'R$' },
                  { value: 'EUR', label: '€' },
                ]}
              />
            )}
          />
        </div>
      </form>
    </ResponsiveDialog>
  )
}
