import { zodResolver } from '@hookform/resolvers/zod'
import { useState } from 'react'
import { Controller, useForm } from 'react-hook-form'
import { toast } from 'sonner'
import { z } from 'zod'
import type { Account, AccountType } from '@/api/types'
import { Button } from '@/components/ui/button'
import { Field } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { MoneyInput } from '@/components/ui/money-input'
import { ResponsiveDialog } from '@/components/ui/responsive-dialog'
import { SegmentedControl } from '@/components/ui/segmented'
import { NativeSelect } from '@/components/ui/select'
import { SwitchField } from '@/components/ui/switch'
import { applyServerErrors } from '@/features/auth/use-server-errors'
import { useObjectives } from '@/features/objectives/api'
import { decimalToInput, isValidDecimal, parseDecimal } from '@/lib/decimal'
import { todayIso } from '@/lib/format'
import { ACCOUNT_TYPE_LABELS } from '@/lib/labels'
import { useRecordBalance, useSaveAccount } from './api'

const accountSchema = z.object({
  name: z.string().trim().min(1, 'Informe o nome.').max(80),
  currency: z.enum(['BRL', 'EUR']),
  type: z.enum(['CHECKING', 'SAVINGS', 'INVESTMENT', 'CASH', 'BLOCKED', 'OTHER']),
  objectiveId: z.string(),
  archived: z.boolean(),
})
type AccountForm = z.infer<typeof accountSchema>

/** Montado só quando aberto (o pai renderiza condicionalmente): o formulário nasce com os valores certos. */
export function AccountDialog({
  open,
  onOpenChange,
  account,
}: {
  open: boolean
  onOpenChange: (open: boolean) => void
  account?: Account
}) {
  const save = useSaveAccount()
  const objectives = useObjectives()
  const [formError, setFormError] = useState('')
  const defaults = (): AccountForm => ({
    name: account?.name ?? '',
    currency: (account?.currency as 'BRL' | 'EUR') ?? 'BRL',
    type: account?.type ?? 'SAVINGS',
    objectiveId: account ? (account.objectiveId ?? '') : (objectives.data?.find((o) => o.primary)?.id ?? ''),
    archived: account?.archived ?? false,
  })
  const {
    register,
    control,
    handleSubmit,
    setError,
    formState: { errors },
  } = useForm<AccountForm>({ resolver: zodResolver(accountSchema), defaultValues: defaults() })

  const onSubmit = handleSubmit((v) =>
    save.mutate(
      {
        id: account?.id,
        body: {
          name: v.name.trim(),
          currency: v.currency,
          type: v.type,
          objectiveId: v.objectiveId || undefined,
          archived: v.archived,
        },
      },
      {
        onSuccess: () => {
          toast.success(account ? 'Conta atualizada.' : 'Conta criada.')
          onOpenChange(false)
        },
        onError: (e) =>
          setFormError(applyServerErrors(e, setError, ['name', 'currency', 'type', 'objectiveId'])),
      },
    ),
  )

  return (
    <ResponsiveDialog
      open={open}
      onOpenChange={onOpenChange}
      title={account ? 'Editar conta' : 'Nova conta'}
      footer={
        <>
          <Button variant="outline" onClick={() => onOpenChange(false)}>
            Cancelar
          </Button>
          <Button type="submit" form="account-form" loading={save.isPending}>
            Salvar
          </Button>
        </>
      }
    >
      <form id="account-form" onSubmit={onSubmit} noValidate className="grid gap-4">
        {formError && (
          <div role="alert" className="rounded-xl bg-danger-soft px-4 py-3 text-sm text-danger">
            {formError}
          </div>
        )}
        <Field id="account-name" label="Nome" error={errors.name?.message}>
          <Input
            id="account-name"
            placeholder="Ex.: Nubank, Wise, Sperrkonto"
            aria-invalid={!!errors.name}
            {...register('name')}
          />
        </Field>
        <div className="grid gap-1.5">
          <span className="text-sm font-medium">Moeda</span>
          <Controller
            control={control}
            name="currency"
            render={({ field }) => (
              <SegmentedControl
                ariaLabel="Moeda da conta"
                value={field.value}
                onValueChange={(v) => !account && field.onChange(v)}
                options={[
                  { value: 'BRL', label: 'Real (R$)' },
                  { value: 'EUR', label: 'Euro (€)' },
                ]}
              />
            )}
          />
          {account && (
            <p className="text-xs text-muted">A moeda não muda depois de criada (o histórico está nela).</p>
          )}
        </div>
        <Field id="account-type" label="Tipo">
          <NativeSelect id="account-type" {...register('type')}>
            {(Object.keys(ACCOUNT_TYPE_LABELS) as AccountType[]).map((t) => (
              <option key={t} value={t}>
                {ACCOUNT_TYPE_LABELS[t]}
              </option>
            ))}
          </NativeSelect>
        </Field>
        <Field
          id="account-objective"
          label="Conta para o objetivo"
          hint="O saldo de contas vinculadas soma no progresso da meta."
        >
          <NativeSelect id="account-objective" {...register('objectiveId')}>
            <option value="">Nenhum (não entra no progresso)</option>
            {objectives.data?.map((o) => (
              <option key={o.id} value={o.id}>
                {o.name}
              </option>
            ))}
          </NativeSelect>
        </Field>
        {account && (
          <Controller
            control={control}
            name="archived"
            render={({ field }) => (
              <SwitchField
                id="account-archived"
                label="Arquivar"
                hint="Some dos totais, mas o histórico é mantido."
                checked={field.value}
                onCheckedChange={field.onChange}
              />
            )}
          />
        )}
      </form>
    </ResponsiveDialog>
  )
}

const balanceSchema = z.object({
  asOf: z
    .string()
    .min(1, 'Informe a data.')
    .refine((v) => v <= todayIso(), 'A data não pode estar no futuro.'),
  balance: z.string().refine((v) => isValidDecimal(v, { allowNegative: true }), 'Informe um valor válido.'),
})
type BalanceForm = z.infer<typeof balanceSchema>

/** Montado só quando aberto: o formulário nasce com os valores da conta. */
export function BalanceDialog({
  account,
  onOpenChange,
}: {
  account: Account
  onOpenChange: (open: boolean) => void
}) {
  const record = useRecordBalance()
  const [formError, setFormError] = useState('')
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors },
  } = useForm<BalanceForm>({
    resolver: zodResolver(balanceSchema),
    defaultValues: { asOf: todayIso(), balance: decimalToInput(account.balance.amount || null) },
  })

  const onSubmit = handleSubmit((v) =>
    record.mutate(
      { accountId: account.id, body: { asOf: v.asOf, balance: parseDecimal(v.balance) ?? 0 } },
      {
        onSuccess: () => {
          toast.success('Saldo atualizado.')
          onOpenChange(false)
        },
        onError: (e) => setFormError(applyServerErrors(e, setError, ['asOf', 'balance'])),
      },
    ),
  )

  return (
    <ResponsiveDialog
      open
      onOpenChange={onOpenChange}
      title="Atualizar saldo"
      description={`${account.name} · ${account.currency}`}
      footer={
        <>
          <Button variant="outline" onClick={() => onOpenChange(false)}>
            Cancelar
          </Button>
          <Button type="submit" form="balance-form" loading={record.isPending}>
            Salvar saldo
          </Button>
        </>
      }
    >
      <form id="balance-form" onSubmit={onSubmit} noValidate className="grid gap-4 sm:grid-cols-2">
        {formError && (
          <div role="alert" className="rounded-xl bg-danger-soft px-4 py-3 text-sm text-danger sm:col-span-2">
            {formError}
          </div>
        )}
        <Field id="balance-value" label="Saldo" error={errors.balance?.message}>
          <MoneyInput
            id="balance-value"
            currency={account.currency}
            autoFocus
            aria-invalid={!!errors.balance}
            {...register('balance')}
          />
        </Field>
        <Field
          id="balance-date"
          label="Data"
          error={errors.asOf?.message}
          hint="Na mesma data, substitui o valor."
        >
          <Input
            id="balance-date"
            type="date"
            max={todayIso()}
            aria-invalid={!!errors.asOf}
            {...register('asOf')}
          />
        </Field>
      </form>
    </ResponsiveDialog>
  )
}
