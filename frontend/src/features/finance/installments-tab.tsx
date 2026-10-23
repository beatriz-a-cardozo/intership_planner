import { zodResolver } from '@hookform/resolvers/zod'
import { ChevronDown, CreditCard, Pencil, Plus, Trash2 } from 'lucide-react'
import { useState } from 'react'
import { Controller, useForm, useWatch } from 'react-hook-form'
import { toast } from 'sonner'
import { z } from 'zod'
import { errorMessage } from '@/api/errors'
import type { FinanceCategory, InstallmentPlan, PaymentMethod } from '@/api/types'
import { useAuth } from '@/app/providers/auth-provider'
import { SplitMeter } from '@/components/charts/split-meter'
import { ConfirmDialog } from '@/components/confirm-dialog'
import { EmptyState } from '@/components/empty-state'
import { ErrorState } from '@/components/error-state'
import { Badge } from '@/components/ui/badge'
import { DialogLoading } from '@/components/dialog-loading'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Field } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { MoneyInput } from '@/components/ui/money-input'
import { ResponsiveDialog } from '@/components/ui/responsive-dialog'
import { SegmentedControl } from '@/components/ui/segmented'
import { NativeSelect } from '@/components/ui/select'
import { Skeleton } from '@/components/ui/skeleton'
import { applyServerErrors } from '@/features/auth/use-server-errors'
import { decimalToInput, isValidDecimal, parseDecimal } from '@/lib/decimal'
import { formatDate, formatMoney, todayIso } from '@/lib/format'
import { PAYMENT_METHOD_LABELS } from '@/lib/labels'
import { cn } from '@/lib/utils'
import {
  useDeleteInstallmentPlan,
  useFinanceCategories,
  useInstallmentPlans,
  useSaveInstallmentPlan,
} from './ledger-api'

export function InstallmentsTab() {
  const plans = useInstallmentPlans()
  const remove = useDeleteInstallmentPlan()
  const [editing, setEditing] = useState<InstallmentPlan | 'new' | null>(null)
  const [deleting, setDeleting] = useState<InstallmentPlan | null>(null)
  const [expanded, setExpanded] = useState<string | null>(null)

  return (
    <Card>
      <CardHeader>
        <div>
          <CardTitle>Parcelamentos</CardTitle>
          <CardDescription>Cada parcela vira um lançamento previsto no mês do vencimento.</CardDescription>
        </div>
        <Button size="sm" onClick={() => setEditing('new')}>
          <Plus aria-hidden /> Parcelamento
        </Button>
      </CardHeader>
      <CardContent>
        {plans.isPending ? (
          <Skeleton className="h-32" />
        ) : plans.isError ? (
          <ErrorState error={plans.error} onRetry={() => void plans.refetch()} />
        ) : plans.data.length === 0 ? (
          <EmptyState
            icon={CreditCard}
            title="Nenhum parcelamento"
            description="Ex.: agência em 10x, passagem em 6x. As parcelas entram no comprometido futuro e na projeção."
            action={<Button onClick={() => setEditing('new')}>Criar parcelamento</Button>}
          />
        ) : (
          <ul className="divide-y divide-border">
            {plans.data.map((p) => {
              const done = p.paidCount === p.installmentsCount
              return (
                <li key={p.id} className="py-4">
                  <div className="flex flex-wrap items-start gap-x-3 gap-y-1">
                    {/* largura mínima: no celular, valor e botões descem para a linha de baixo em vez de espremer o texto */}
                    <div className="min-w-[min(100%,16rem)] flex-1">
                      <p className="flex flex-wrap items-center gap-2 font-medium">
                        {p.description}
                        {done ? (
                          <Badge tone="primary">Quitado</Badge>
                        ) : (
                          <Badge>
                            {p.remainingCount === 1
                              ? 'falta 1 parcela'
                              : `faltam ${p.remainingCount} parcelas`}
                          </Badge>
                        )}
                      </p>
                      <p className="text-xs text-muted">
                        {p.installmentsCount}x de {formatMoney(p.installmentAmount)} ·{' '}
                        {PAYMENT_METHOD_LABELS[p.paymentMethod]}
                        {p.nextDueDate && ` · próxima em ${formatDate(p.nextDueDate)}`}
                        {p.paidBeforeCount > 0 &&
                          ` · ${p.paidBeforeCount} paga${p.paidBeforeCount > 1 ? 's' : ''} antes do app`}
                      </p>
                    </div>
                    <div className="ml-auto flex items-center gap-1">
                      <p className="mr-1 font-semibold tabular">{formatMoney(p.total)}</p>
                      <Button
                        variant="ghost"
                        size="icon"
                        aria-label={`Parcelas de ${p.description}`}
                        aria-expanded={expanded === p.id}
                        onClick={() => setExpanded(expanded === p.id ? null : p.id)}
                      >
                        <ChevronDown
                          className={cn('transition-transform', expanded === p.id && 'rotate-180')}
                        />
                      </Button>
                      <Button
                        variant="ghost"
                        size="icon"
                        aria-label={`Editar ${p.description}`}
                        onClick={() => setEditing(p)}
                      >
                        <Pencil />
                      </Button>
                      <Button
                        variant="ghost"
                        size="icon"
                        aria-label={`Excluir ${p.description}`}
                        onClick={() => setDeleting(p)}
                      >
                        <Trash2 />
                      </Button>
                    </div>
                  </div>
                  <SplitMeter
                    className="mt-3"
                    share={p.total.amount > 0 ? p.paid.amount / p.total.amount : 0}
                    filledLabel={`Pagas ${p.paidCount}/${p.installmentsCount}`}
                    filledValue={formatMoney(p.paid)}
                    restLabel="Restante"
                    restValue={formatMoney(p.remaining)}
                  />
                  {expanded === p.id && (
                    <table className="mt-3 w-full text-sm">
                      <caption className="sr-only">Parcelas de {p.description}</caption>
                      <thead>
                        <tr className="text-left text-xs text-muted">
                          <th className="pb-1 font-normal">Parcela</th>
                          <th className="pb-1 font-normal">Vencimento</th>
                          <th className="pb-1 text-right font-normal">Valor</th>
                          <th className="pb-1 text-right font-normal">Situação</th>
                        </tr>
                      </thead>
                      <tbody>
                        {p.installments.map((i) => (
                          <tr key={i.number}>
                            <td className="py-1">
                              {i.number}/{p.installmentsCount}
                            </td>
                            <td className="py-1">{formatDate(i.dueDate)}</td>
                            <td className="py-1 text-right tabular">{formatMoney(i.amount)}</td>
                            <td className="py-1 text-right">
                              <Badge tone={i.status === 'CONFIRMED' ? 'primary' : 'neutral'}>
                                {i.status === 'CONFIRMED' ? 'Paga' : 'Prevista'}
                              </Badge>
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  )}
                </li>
              )
            })}
          </ul>
        )}
      </CardContent>

      {editing !== null && (
        <InstallmentDialog plan={editing === 'new' ? undefined : editing} onClose={() => setEditing(null)} />
      )}
      <ConfirmDialog
        open={deleting !== null}
        onOpenChange={(o) => !o && setDeleting(null)}
        title="Excluir parcelamento?"
        description="As parcelas em aberto serão removidas; as já confirmadas ficam no histórico."
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

const schema = z
  .object({
    description: z.string().trim().min(1, 'Descreva a compra.').max(160),
    categoryId: z.string().min(1, 'Escolha a categoria.'),
    currency: z.enum(['BRL', 'EUR']),
    mode: z.enum(['total', 'installment']),
    amount: z
      .string()
      .refine((v) => isValidDecimal(v) && (parseDecimal(v) ?? 0) > 0, 'Informe um valor maior que zero.'),
    count: z.string().refine((v) => /^\d+$/.test(v) && +v >= 1 && +v <= 360, 'Entre 1 e 360 parcelas.'),
    paidBefore: z.string().refine((v) => v.trim() === '' || /^\d+$/.test(v.trim()), 'Use um número inteiro.'),
    firstDueDate: z.string().min(1, 'Informe o vencimento.'),
    paymentMethod: z.enum(['CREDIT_CARD', 'BOLETO', 'PIX', 'DEBIT', 'BANK_TRANSFER', 'OTHER']),
  })
  .refine((v) => !/^\d+$/.test(v.count) || Number(v.paidBefore || 0) < Number(v.count), {
    path: ['paidBefore'],
    message: 'Deve faltar pelo menos uma parcela.',
  })
type Form = z.infer<typeof schema>

/** Valores vindos do "Novo lançamento" quando a pessoa marca que a compra é parcelada. */
export interface InstallmentDraft {
  description?: string
  categoryId?: string
  currency?: 'BRL' | 'EUR'
  installmentAmount?: string
}

/** Montado só quando aberto; o formulário espera as categorias (ver `DialogLoading`). */
export function InstallmentDialog(props: {
  plan?: InstallmentPlan
  draft?: InstallmentDraft
  onClose: () => void
}) {
  const categories = useFinanceCategories()
  if (categories.data) return <InstallmentForm {...props} categories={categories.data} />
  return (
    <DialogLoading title={props.plan ? 'Editar parcelamento' : 'Novo parcelamento'} onClose={props.onClose} />
  )
}

function InstallmentForm({
  plan,
  draft,
  onClose,
  categories,
}: {
  plan?: InstallmentPlan
  draft?: InstallmentDraft
  onClose: () => void
  categories: FinanceCategory[]
}) {
  const { user } = useAuth()
  const save = useSaveInstallmentPlan()
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
      description: plan?.description ?? draft?.description ?? '',
      categoryId: plan?.category.id ?? draft?.categoryId ?? '',
      currency: (plan?.total.currency ?? draft?.currency ?? user?.settings.primaryCurrency ?? 'BRL') as
        'BRL' | 'EUR',
      mode: draft?.installmentAmount ? 'installment' : 'total',
      amount: plan ? decimalToInput(plan.total.amount) : (draft?.installmentAmount ?? ''),
      count: plan ? String(plan.installmentsCount) : '',
      paidBefore: plan && plan.paidBeforeCount > 0 ? String(plan.paidBeforeCount) : '',
      firstDueDate: plan?.firstDueDate ?? todayIso(),
      paymentMethod: plan?.paymentMethod ?? 'CREDIT_CARD',
    },
  })
  const [currency, mode, paidBefore] = useWatch({ control, name: ['currency', 'mode', 'paidBefore'] })
  const inProgress = Number(paidBefore || 0) > 0

  const onSubmit = handleSubmit((v) => {
    const amount = parseDecimal(v.amount) ?? 0
    save.mutate(
      {
        id: plan?.id,
        body: {
          description: v.description.trim(),
          categoryId: v.categoryId,
          currency: v.currency,
          totalAmount: v.mode === 'total' ? amount : undefined,
          installmentAmount: v.mode === 'installment' ? amount : undefined,
          installmentsCount: Number(v.count),
          paidBeforeCount: v.paidBefore.trim() ? Number(v.paidBefore) : 0,
          firstDueDate: v.firstDueDate,
          paymentMethod: v.paymentMethod as PaymentMethod,
        },
      },
      {
        onSuccess: () => {
          toast.success(plan ? 'Parcelamento atualizado.' : 'Parcelamento criado.')
          onClose()
        },
        onError: (e) =>
          setFormError(
            applyServerErrors(e, setError, ['description', 'categoryId', 'firstDueDate', 'paidBefore'], {
              paidBeforeCount: 'paidBefore',
            }),
          ),
      },
    )
  })

  return (
    <ResponsiveDialog
      open
      onOpenChange={(o) => !o && onClose()}
      title={plan ? 'Editar parcelamento' : 'Novo parcelamento'}
      description={
        plan && plan.paidCount > 0 ? 'Parcelas já pagas são mantidas; o restante é redistribuído.' : undefined
      }
      footer={
        <>
          <Button variant="outline" onClick={onClose}>
            Cancelar
          </Button>
          <Button type="submit" form="installment-form" loading={save.isPending}>
            Salvar
          </Button>
        </>
      }
    >
      <form id="installment-form" onSubmit={onSubmit} noValidate className="grid gap-4">
        {formError && (
          <div role="alert" className="rounded-xl bg-danger-soft px-4 py-3 text-sm text-danger">
            {formError}
          </div>
        )}
        <Field id="inst-description" label="Compra" error={errors.description?.message}>
          <Input
            id="inst-description"
            placeholder="Ex.: Agência de intercâmbio"
            aria-invalid={!!errors.description}
            {...register('description')}
          />
        </Field>
        <Field id="inst-category" label="Categoria" error={errors.categoryId?.message}>
          <NativeSelect id="inst-category" aria-invalid={!!errors.categoryId} {...register('categoryId')}>
            <option value="">Escolha…</option>
            {categories
              .filter((c) => c.kind === 'EXPENSE' && (!c.archived || c.id === plan?.category.id))
              .map((c) => (
                <option key={c.id} value={c.id}>
                  {c.name}
                </option>
              ))}
          </NativeSelect>
        </Field>
        <Controller
          control={control}
          name="mode"
          render={({ field }) => (
            <SegmentedControl
              ariaLabel="Informar"
              value={field.value}
              onValueChange={field.onChange}
              options={[
                { value: 'total', label: 'Valor total' },
                { value: 'installment', label: 'Valor da parcela' },
              ]}
            />
          )}
        />
        <div className="grid grid-cols-[1fr_auto] items-end gap-3">
          <Field
            id="inst-amount"
            label={mode === 'total' ? 'Valor total' : 'Valor de cada parcela'}
            error={errors.amount?.message}
          >
            <MoneyInput
              id="inst-amount"
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
          <Field id="inst-count" label="Total de parcelas" error={errors.count?.message}>
            <Input
              id="inst-count"
              inputMode="numeric"
              placeholder="10"
              aria-invalid={!!errors.count}
              {...register('count')}
            />
          </Field>
          <Field
            id="inst-paid-before"
            label="Já pagas"
            hint="Se a compra já está em andamento"
            error={errors.paidBefore?.message}
          >
            <Input
              id="inst-paid-before"
              inputMode="numeric"
              placeholder="0"
              aria-invalid={!!errors.paidBefore}
              {...register('paidBefore')}
            />
          </Field>
        </div>
        <div className="grid gap-4 sm:grid-cols-2">
          <Field
            id="inst-first"
            label={inProgress ? 'Próxima parcela vence em' : '1º vencimento'}
            error={errors.firstDueDate?.message}
          >
            <Input
              id="inst-first"
              type="date"
              aria-invalid={!!errors.firstDueDate}
              {...register('firstDueDate')}
            />
          </Field>
          <Field id="inst-method" label="Forma">
            <NativeSelect id="inst-method" {...register('paymentMethod')}>
              {Object.entries(PAYMENT_METHOD_LABELS).map(([k, l]) => (
                <option key={k} value={k}>
                  {l}
                </option>
              ))}
            </NativeSelect>
          </Field>
        </div>
      </form>
    </ResponsiveDialog>
  )
}
