import { zodResolver } from '@hookform/resolvers/zod'
import { Ban, Pencil, Plus, RotateCcw, Trash2 } from 'lucide-react'
import { useState } from 'react'
import { Controller, useForm, useWatch } from 'react-hook-form'
import { toast } from 'sonner'
import { z } from 'zod'
import { errorMessage } from '@/api/errors'
import type { Expense, ExpenseDetail } from '@/api/types'
import { SplitMeter } from '@/components/charts/split-meter'
import { CatalogIcon } from '@/components/catalog-icon'
import { ConfirmDialog } from '@/components/confirm-dialog'
import { ErrorState } from '@/components/error-state'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Field } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { MoneyInput } from '@/components/ui/money-input'
import { ResponsiveDialog } from '@/components/ui/responsive-dialog'
import { SegmentedControl } from '@/components/ui/segmented'
import { Skeleton } from '@/components/ui/skeleton'
import { applyServerErrors } from '@/features/auth/use-server-errors'
import { isValidDecimal, parseDecimal } from '@/lib/decimal'
import { formatDate, formatMoney, formatRate, todayIso } from '@/lib/format'
import { RECURRENCE_LABELS, STATUS_LABELS, STATUS_TONES } from '@/lib/labels'
import { useAddPayment, useCancelExpense, useDeleteExpense, useDeletePayment, useExpense } from './api'

interface Props {
  expenseId: string
  onOpenChange: (open: boolean) => void
  onEdit: (expense: Expense) => void
}

/** Montado só quando aberto. */
export function ExpenseDetailDialog({ expenseId, onOpenChange, onEdit }: Props) {
  const detail = useExpense(expenseId)
  const cancel = useCancelExpense()
  const remove = useDeleteExpense()
  const [confirmDelete, setConfirmDelete] = useState(false)
  const [addingPayment, setAddingPayment] = useState(false)

  const e = detail.data?.expense

  return (
    <ResponsiveDialog
      open
      onOpenChange={onOpenChange}
      title={e?.name ?? 'Gasto'}
      description={e ? `${e.category.name}${e.cycle ? ` · ${e.cycle.name}` : ''}` : undefined}
      footer={
        e && (
          <>
            <Button variant="ghost" onClick={() => setConfirmDelete(true)}>
              <Trash2 aria-hidden /> Excluir
            </Button>
            <Button
              variant="outline"
              loading={cancel.isPending}
              onClick={() =>
                cancel.mutate(
                  { id: e.id, cancel: e.status !== 'CANCELLED' },
                  { onError: (err) => toast.error(errorMessage(err)) },
                )
              }
            >
              {e.status === 'CANCELLED' ? (
                <>
                  <RotateCcw aria-hidden /> Reativar
                </>
              ) : (
                <>
                  <Ban aria-hidden /> Cancelar gasto
                </>
              )}
            </Button>
            <Button onClick={() => onEdit(e)}>
              <Pencil aria-hidden /> Editar
            </Button>
          </>
        )
      }
    >
      {detail.isPending ? (
        <div className="grid gap-3">
          <Skeleton className="h-20" />
          <Skeleton className="h-32" />
        </div>
      ) : detail.isError ? (
        <ErrorState error={detail.error} onRetry={() => void detail.refetch()} />
      ) : (
        <DetailBody detail={detail.data} addingPayment={addingPayment} setAddingPayment={setAddingPayment} />
      )}

      <ConfirmDialog
        open={confirmDelete}
        onOpenChange={setConfirmDelete}
        title="Excluir gasto?"
        description="O gasto e seus pagamentos serão excluídos. Para só tirar dos totais, cancele o gasto."
        confirmLabel="Excluir"
        destructive
        loading={remove.isPending}
        onConfirm={() =>
          remove.mutate(expenseId, {
            onSuccess: () => {
              toast.success('Gasto excluído.')
              setConfirmDelete(false)
              onOpenChange(false)
            },
            onError: (err) => toast.error(errorMessage(err)),
          })
        }
      />
    </ResponsiveDialog>
  )
}

function DetailBody({
  detail,
  addingPayment,
  setAddingPayment,
}: {
  detail: ExpenseDetail
  addingPayment: boolean
  setAddingPayment: (v: boolean) => void
}) {
  const { expense: e, payments } = detail
  const removePayment = useDeletePayment()
  const share = e.total.amount > 0 ? e.paid.amount / e.total.amount : e.paid.amount > 0 ? 1 : 0

  return (
    <div className="grid gap-5">
      <div className="flex items-start gap-3">
        <CatalogIcon icon={e.category.icon} color={e.category.color} />
        <div className="min-w-0 flex-1">
          <p className="font-display text-2xl font-semibold tabular">{formatMoney(e.total)}</p>
          {e.total.currency !== e.totalConverted.currency && (
            <p className="text-sm text-muted tabular">≈ {formatMoney(e.totalConverted)} na cotação atual</p>
          )}
          <p className="mt-1 text-sm text-muted">
            {formatMoney({ amount: e.unitPrice, currency: e.currency })}
            {e.quantity > 1 && ` × ${e.quantity}`}
            {e.occurrences > 1 && ` × ${e.occurrences} (${RECURRENCE_LABELS[e.recurrence].toLowerCase()})`}
          </p>
        </div>
        <Badge tone={STATUS_TONES[e.status]}>{STATUS_LABELS[e.status]}</Badge>
      </div>

      <div className="flex flex-wrap gap-2">
        <Badge>{e.required ? 'Obrigatório' : 'Opcional'}</Badge>
        {e.refundable && <Badge tone="accent">Reembolsável</Badge>}
        {e.dueDate && (
          <Badge>
            {e.endDate ? `${formatDate(e.dueDate)} → ${formatDate(e.endDate)}` : formatDate(e.dueDate)}
          </Badge>
        )}
      </div>

      {e.notes && <p className="rounded-xl bg-surface-2 p-3 text-sm">{e.notes}</p>}

      <SplitMeter
        share={share}
        filledLabel="Pago"
        filledValue={formatMoney(e.paid)}
        restLabel="Falta"
        restValue={formatMoney(e.remaining)}
      />

      <section aria-labelledby="payments-title">
        <div className="mb-2 flex items-center justify-between">
          <h3 id="payments-title" className="text-sm font-semibold">
            Pagamentos
          </h3>
          {e.status !== 'CANCELLED' && !addingPayment && (
            <Button variant="outline" size="sm" onClick={() => setAddingPayment(true)}>
              <Plus aria-hidden /> Pagamento
            </Button>
          )}
        </div>
        {addingPayment && <PaymentForm expense={e} onDone={() => setAddingPayment(false)} />}
        {payments.length === 0 ? (
          !addingPayment && <p className="text-sm text-muted">Nenhum pagamento registrado.</p>
        ) : (
          <ul className="divide-y divide-border">
            {payments.map((p) => (
              <li key={p.id} className="flex items-center gap-3 py-2.5 text-sm">
                <div className="min-w-0 flex-1">
                  <p className="font-medium tabular">
                    {formatMoney(p.amount)}
                    {p.amount.currency !== e.currency && (
                      <span className="font-normal text-muted">
                        {' '}
                        = {formatMoney(p.amountInExpenseCurrency)} (taxa {formatRate(p.rateToExpenseCurrency)}
                        )
                      </span>
                    )}
                  </p>
                  <p className="truncate text-xs text-muted">
                    {formatDate(p.paidOn)}
                    {p.note && ` · ${p.note}`}
                  </p>
                </div>
                <Button
                  variant="ghost"
                  size="icon"
                  aria-label={`Remover pagamento de ${formatDate(p.paidOn)}`}
                  onClick={() =>
                    removePayment.mutate(
                      { expenseId: e.id, paymentId: p.id },
                      { onError: (err) => toast.error(errorMessage(err)) },
                    )
                  }
                >
                  <Trash2 />
                </Button>
              </li>
            ))}
          </ul>
        )}
      </section>
    </div>
  )
}

const paymentSchema = z.object({
  paidOn: z
    .string()
    .min(1, 'Informe a data.')
    .refine((v) => v <= todayIso(), 'A data não pode estar no futuro.'),
  amount: z
    .string()
    .refine((v) => isValidDecimal(v) && (parseDecimal(v) ?? 0) > 0, 'Informe um valor maior que zero.'),
  currency: z.enum(['BRL', 'EUR']),
  rate: z
    .string()
    .refine(
      (v) =>
        v.trim() === '' ||
        (isValidDecimal(v, { thousands: false, maxFraction: 8 }) &&
          (parseDecimal(v, { thousands: false }) ?? 0) > 0),
      'Taxa inválida.',
    ),
  note: z.string().max(500),
})
type PaymentForm = z.infer<typeof paymentSchema>

function PaymentForm({ expense, onDone }: { expense: Expense; onDone: () => void }) {
  const add = useAddPayment()
  const [formError, setFormError] = useState('')
  const {
    register,
    control,
    handleSubmit,
    setError,
    formState: { errors },
  } = useForm<PaymentForm>({
    resolver: zodResolver(paymentSchema),
    defaultValues: {
      paidOn: todayIso(),
      amount: expense.remaining.amount > 0 ? String(expense.remaining.amount).replace('.', ',') : '',
      currency: expense.currency as 'BRL' | 'EUR',
      rate: '',
      note: '',
    },
  })
  const currency = useWatch({ control, name: 'currency' })
  const otherCurrency = currency !== expense.currency

  const onSubmit = handleSubmit((v) =>
    add.mutate(
      {
        expenseId: expense.id,
        body: {
          paidOn: v.paidOn,
          amount: parseDecimal(v.amount) ?? 0,
          currency: v.currency,
          rateToExpenseCurrency:
            otherCurrency && v.rate.trim()
              ? (parseDecimal(v.rate, { thousands: false }) ?? undefined)
              : undefined,
          note: v.note.trim() || undefined,
        },
      },
      {
        onSuccess: () => {
          toast.success('Pagamento registrado.')
          onDone()
        },
        onError: (e) =>
          setFormError(applyServerErrors(e, setError, ['paidOn', 'amount', 'currency', 'note'])),
      },
    ),
  )

  return (
    <form onSubmit={onSubmit} noValidate className="mb-4 grid gap-3 rounded-xl border border-border p-4">
      {formError && (
        <div role="alert" className="rounded-lg bg-danger-soft px-3 py-2 text-sm text-danger">
          {formError}
        </div>
      )}
      <Controller
        control={control}
        name="currency"
        render={({ field }) => (
          <SegmentedControl
            ariaLabel="Moeda do pagamento"
            value={field.value}
            onValueChange={field.onChange}
            options={[
              { value: 'BRL', label: 'Pago em R$' },
              { value: 'EUR', label: 'Pago em €' },
            ]}
          />
        )}
      />
      <div className="grid gap-3 sm:grid-cols-2">
        <Field id="payment-amount" label="Valor" error={errors.amount?.message}>
          <MoneyInput
            id="payment-amount"
            currency={currency}
            aria-invalid={!!errors.amount}
            {...register('amount')}
          />
        </Field>
        <Field id="payment-date" label="Data" error={errors.paidOn?.message}>
          <Input
            id="payment-date"
            type="date"
            max={todayIso()}
            aria-invalid={!!errors.paidOn}
            {...register('paidOn')}
          />
        </Field>
        {otherCurrency && (
          <Field
            id="payment-rate"
            label={`Taxa ${currency}→${expense.currency} (opcional)`}
            error={errors.rate?.message}
            hint="Vazio = cotação de hoje. A taxa fica gravada no pagamento."
          >
            <Input
              id="payment-rate"
              inputMode="decimal"
              placeholder="Ex.: 6,12"
              aria-invalid={!!errors.rate}
              {...register('rate')}
            />
          </Field>
        )}
        <Field
          id="payment-note"
          label="Observação (opcional)"
          className={otherCurrency ? '' : 'sm:col-span-2'}
        >
          <Input id="payment-note" placeholder="Ex.: parcela 1/10" {...register('note')} />
        </Field>
      </div>
      <div className="flex justify-end gap-2">
        <Button type="button" variant="ghost" onClick={onDone}>
          Cancelar
        </Button>
        <Button type="submit" loading={add.isPending}>
          Registrar
        </Button>
      </div>
    </form>
  )
}
