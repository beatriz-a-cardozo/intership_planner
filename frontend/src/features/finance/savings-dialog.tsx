import { zodResolver } from '@hookform/resolvers/zod'
import { useState } from 'react'
import { Controller, useForm, useWatch } from 'react-hook-form'
import { toast } from 'sonner'
import { z } from 'zod'
import type { Account } from '@/api/types'
import { DialogLoading } from '@/components/dialog-loading'
import { Button } from '@/components/ui/button'
import { Field } from '@/components/ui/field'
import { MoneyInput } from '@/components/ui/money-input'
import { ResponsiveDialog } from '@/components/ui/responsive-dialog'
import { SegmentedControl } from '@/components/ui/segmented'
import { NativeSelect } from '@/components/ui/select'
import { applyServerErrors } from '@/features/auth/use-server-errors'
import { isValidDecimal, parseDecimal } from '@/lib/decimal'
import { formatMoney } from '@/lib/format'
import { useAccounts, useAddSavings } from './api'

/** Valor da opção "nova reserva" no seletor de conta: `new:BRL`, `new:EUR`. */
const NEW_PREFIX = 'new:'

const schema = z.object({
  direction: z.enum(['IN', 'OUT']),
  target: z.string().min(1),
  amount: z
    .string()
    .refine((v) => isValidDecimal(v) && (parseDecimal(v) ?? 0) > 0, 'Informe um valor maior que zero.'),
})
type SavingsForm = z.infer<typeof schema>

interface SavingsDialogProps {
  objectiveId: string
  objectiveCurrency: string
  onClose: () => void
}

/** "Guardar dinheiro": soma (ou retira) do saldo de hoje de uma conta vinculada ao objetivo. Montado só quando aberto. */
export function SavingsDialog(props: SavingsDialogProps) {
  const accounts = useAccounts()
  if (accounts.data) {
    const linked = accounts.data.filter((a) => a.objectiveId === props.objectiveId && !a.archived)
    return <SavingsFormDialog {...props} accounts={linked} />
  }
  return <DialogLoading title="Guardar dinheiro" onClose={props.onClose} />
}

function SavingsFormDialog({
  objectiveId,
  objectiveCurrency,
  onClose,
  accounts,
}: SavingsDialogProps & { accounts: Account[] }) {
  const save = useAddSavings()
  const [formError, setFormError] = useState('')
  // Moedas sem conta vinculada ganham a opção de criar a "Reserva do intercâmbio" nelas
  const newOptions = ['BRL', 'EUR'].filter((c) => !accounts.some((a) => a.currency === c))
  const firstTarget =
    accounts.find((a) => a.currency === objectiveCurrency)?.id ??
    accounts[0]?.id ??
    `${NEW_PREFIX}${objectiveCurrency}`
  const {
    control,
    register,
    handleSubmit,
    setError,
    formState: { errors },
  } = useForm<SavingsForm>({
    resolver: zodResolver(schema),
    defaultValues: { direction: 'IN', target: firstTarget, amount: '' },
  })
  const target = useWatch({ control, name: 'target' })
  const direction = useWatch({ control, name: 'direction' })
  const account = accounts.find((a) => a.id === target)
  const currency = account?.currency ?? target.slice(NEW_PREFIX.length)

  const onSubmit = handleSubmit((v) => {
    const value = parseDecimal(v.amount) ?? 0
    save.mutate(
      {
        objectiveId,
        body: {
          amount: v.direction === 'OUT' ? -value : value,
          accountId: account?.id ?? null,
          currency: account ? null : currency,
        },
      },
      {
        onSuccess: (a) => {
          toast.success(
            `${v.direction === 'OUT' ? 'Retirada registrada' : 'Guardado'}: ${a.name} agora tem ${formatMoney(a.balance)}.`,
          )
          onClose()
        },
        onError: (e) => setFormError(applyServerErrors(e, setError, ['amount'])),
      },
    )
  })

  return (
    <ResponsiveDialog
      open
      onOpenChange={(o) => !o && onClose()}
      title="Guardar dinheiro"
      description="Soma no saldo de hoje de uma conta do objetivo e entra no progresso da meta."
      footer={
        <>
          <Button variant="outline" onClick={onClose}>
            Cancelar
          </Button>
          <Button type="submit" form="savings-form" loading={save.isPending}>
            {direction === 'OUT' ? 'Retirar' : 'Guardar'}
          </Button>
        </>
      }
    >
      <form id="savings-form" onSubmit={onSubmit} noValidate className="grid gap-4">
        {formError && (
          <div role="alert" className="rounded-xl bg-danger-soft px-4 py-3 text-sm text-danger">
            {formError}
          </div>
        )}
        <Controller
          control={control}
          name="direction"
          render={({ field }) => (
            <SegmentedControl
              ariaLabel="Guardar ou retirar"
              value={field.value}
              onValueChange={field.onChange}
              options={[
                { value: 'IN', label: 'Guardar' },
                { value: 'OUT', label: 'Retirar' },
              ]}
            />
          )}
        />
        <Field id="savings-amount" label="Valor" error={errors.amount?.message}>
          <MoneyInput
            id="savings-amount"
            autoFocus
            currency={currency}
            placeholder="0,00"
            aria-invalid={!!errors.amount}
            {...register('amount')}
          />
        </Field>
        <Field
          id="savings-account"
          label="Conta"
          hint={
            account
              ? `Saldo atual: ${formatMoney(account.balance)}`
              : 'A conta "Reserva do intercâmbio" é criada e vinculada ao objetivo.'
          }
        >
          <NativeSelect id="savings-account" {...register('target')}>
            {accounts.map((a) => (
              <option key={a.id} value={a.id}>
                {a.name} ({a.currency})
              </option>
            ))}
            {newOptions.map((c) => (
              <option key={c} value={`${NEW_PREFIX}${c}`}>
                Nova reserva em {c === 'BRL' ? 'real (R$)' : 'euro (€)'}
              </option>
            ))}
          </NativeSelect>
        </Field>
      </form>
    </ResponsiveDialog>
  )
}
