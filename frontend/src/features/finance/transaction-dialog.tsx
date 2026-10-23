import { zodResolver } from '@hookform/resolvers/zod'
import { useState } from 'react'
import { Controller, useForm, useWatch } from 'react-hook-form'
import { toast } from 'sonner'
import { z } from 'zod'
import type { Account, EntryKind, FinanceCategory, Transaction } from '@/api/types'
import { DialogLoading } from '@/components/dialog-loading'
import { Button } from '@/components/ui/button'
import { Field } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { MoneyInput } from '@/components/ui/money-input'
import { ResponsiveDialog } from '@/components/ui/responsive-dialog'
import { SegmentedControl } from '@/components/ui/segmented'
import { NativeSelect } from '@/components/ui/select'
import { SwitchField } from '@/components/ui/switch'
import { applyServerErrors } from '@/features/auth/use-server-errors'
import { decimalToInput, isValidDecimal, parseDecimal } from '@/lib/decimal'
import { todayIso } from '@/lib/format'
import { useAccounts } from './api'
import type { InstallmentDraft } from './installments-tab'
import { useFinanceCategories, useSaveTransaction } from './ledger-api'

const schema = z.object({
  kind: z.enum(['INCOME', 'EXPENSE']),
  description: z.string().trim().min(1, 'Descreva o lançamento.').max(160),
  amount: z
    .string()
    .refine((v) => isValidDecimal(v) && (parseDecimal(v) ?? 0) > 0, 'Informe um valor maior que zero.'),
  currency: z.enum(['BRL', 'EUR']),
  occurredOn: z.string().min(1, 'Informe a data.'),
  categoryId: z.string().min(1, 'Escolha a categoria.'),
  accountId: z.string(),
  confirmed: z.boolean(),
})
type Form = z.infer<typeof schema>

interface Props {
  transaction?: Transaction
  defaultKind?: EntryKind
  defaultDate?: string
  defaultCurrency: string
  onOpenChange: (open: boolean) => void
  /** Troca para o cadastro de parcelamento levando o que já foi digitado (só em despesa nova). */
  onInstallment?: (draft: InstallmentDraft) => void
}

/**
 * Lançamento avulso (ou edição de um gerado por parcela/recorrência). Montado só quando aberto; o
 * formulário espera categorias e contas (ver `DialogLoading`).
 */
export function TransactionDialog(props: Props) {
  const categories = useFinanceCategories()
  const accounts = useAccounts()
  if (categories.data && accounts.data)
    return <TransactionForm {...props} categories={categories.data} accounts={accounts.data} />
  return (
    <DialogLoading
      title={props.transaction ? 'Editar lançamento' : 'Novo lançamento'}
      onClose={() => props.onOpenChange(false)}
    />
  )
}

function TransactionForm({
  transaction: t,
  defaultKind = 'EXPENSE',
  defaultDate,
  defaultCurrency,
  onOpenChange,
  onInstallment,
  categories,
  accounts,
}: Props & { categories: FinanceCategory[]; accounts: Account[] }) {
  const save = useSaveTransaction()
  const [formError, setFormError] = useState('')
  const {
    register,
    control,
    handleSubmit,
    setValue,
    getValues,
    setError,
    formState: { errors },
  } = useForm<Form>({
    resolver: zodResolver(schema),
    defaultValues: {
      kind: t?.kind ?? defaultKind,
      description: t?.description ?? '',
      amount: decimalToInput(t?.amount.amount),
      currency: (t?.amount.currency ?? defaultCurrency) as 'BRL' | 'EUR',
      occurredOn: t?.occurredOn ?? defaultDate ?? todayIso(),
      categoryId: t?.category.id ?? '',
      accountId: t?.accountId ?? '',
      confirmed: t ? t.status === 'CONFIRMED' : (defaultDate ?? todayIso()) <= todayIso(),
    },
  })
  const [kind, currency] = useWatch({ control, name: ['kind', 'currency'] })
  const options = categories.filter((c) => c.kind === kind && (!c.archived || c.id === t?.category.id))

  const onSubmit = handleSubmit((v) =>
    save.mutate(
      {
        id: t?.id,
        body: {
          kind: v.kind,
          description: v.description.trim(),
          amount: parseDecimal(v.amount) ?? 0,
          currency: v.currency,
          occurredOn: v.occurredOn,
          categoryId: v.categoryId,
          accountId: v.accountId || undefined,
          status: v.confirmed ? 'CONFIRMED' : 'PLANNED',
        },
      },
      {
        onSuccess: () => {
          toast.success(t ? 'Lançamento atualizado.' : 'Lançamento criado.')
          onOpenChange(false)
        },
        onError: (e) =>
          setFormError(applyServerErrors(e, setError, ['description', 'amount', 'occurredOn', 'categoryId'])),
      },
    ),
  )

  return (
    <ResponsiveDialog
      open
      onOpenChange={onOpenChange}
      title={t ? 'Editar lançamento' : 'Novo lançamento'}
      description={t?.sourceLabel ? `Gerado por: ${t.sourceLabel}` : undefined}
      footer={
        <>
          <Button variant="outline" onClick={() => onOpenChange(false)}>
            Cancelar
          </Button>
          <Button type="submit" form="transaction-form" loading={save.isPending}>
            Salvar
          </Button>
        </>
      }
    >
      <form id="transaction-form" onSubmit={onSubmit} noValidate className="grid gap-4">
        {formError && (
          <div role="alert" className="rounded-xl bg-danger-soft px-4 py-3 text-sm text-danger">
            {formError}
          </div>
        )}
        {!t && (
          <Controller
            control={control}
            name="kind"
            render={({ field }) => (
              <SegmentedControl
                ariaLabel="Tipo"
                value={field.value}
                onValueChange={(v) => {
                  field.onChange(v)
                  setValue('categoryId', '')
                }}
                options={[
                  { value: 'EXPENSE', label: 'Despesa' },
                  { value: 'INCOME', label: 'Receita' },
                ]}
              />
            )}
          />
        )}
        {!t && kind === 'EXPENSE' && onInstallment && (
          <button
            type="button"
            className="-mt-1 justify-self-start text-sm text-primary underline-offset-4 hover:underline"
            onClick={() => {
              const v = getValues()
              onInstallment({
                description: v.description,
                categoryId: v.categoryId || undefined,
                currency: v.currency,
                installmentAmount: v.amount || undefined,
              })
            }}
          >
            É parcelado? Cadastrar como parcelamento
          </button>
        )}
        <Field id="tx-description" label="Descrição" error={errors.description?.message}>
          <Input
            id="tx-description"
            placeholder={kind === 'INCOME' ? 'Ex.: Salário de outubro' : 'Ex.: Mercado'}
            aria-invalid={!!errors.description}
            {...register('description')}
          />
        </Field>
        <div className="grid grid-cols-[1fr_auto] items-end gap-3">
          <Field id="tx-amount" label="Valor" error={errors.amount?.message}>
            <MoneyInput
              id="tx-amount"
              currency={currency}
              aria-invalid={!!errors.amount}
              {...register('amount')}
            />
          </Field>
          <Controller
            control={control}
            name="currency"
            render={({ field }) => (
              <SegmentedControl
                ariaLabel="Moeda"
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
        <div className="grid gap-4 sm:grid-cols-2">
          <Field id="tx-date" label="Data" error={errors.occurredOn?.message}>
            <Input id="tx-date" type="date" aria-invalid={!!errors.occurredOn} {...register('occurredOn')} />
          </Field>
          <Field id="tx-category" label="Categoria" error={errors.categoryId?.message}>
            <NativeSelect id="tx-category" aria-invalid={!!errors.categoryId} {...register('categoryId')}>
              <option value="">Escolha…</option>
              {options.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.name}
                </option>
              ))}
            </NativeSelect>
          </Field>
        </div>
        <Field id="tx-account" label="Conta (opcional)">
          <NativeSelect id="tx-account" {...register('accountId')}>
            <option value="">Nenhuma</option>
            {accounts
              .filter((a) => !a.archived || a.id === t?.accountId)
              .map((a) => (
                <option key={a.id} value={a.id}>
                  {a.name} ({a.currency})
                </option>
              ))}
          </NativeSelect>
        </Field>
        <Controller
          control={control}
          name="confirmed"
          render={({ field }) => (
            <SwitchField
              id="tx-confirmed"
              label="Já aconteceu"
              hint="Desligado = previsto (entra nas projeções, mas não no realizado)."
              checked={field.value}
              onCheckedChange={field.onChange}
            />
          )}
        />
      </form>
    </ResponsiveDialog>
  )
}
