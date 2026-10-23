import { zodResolver } from '@hookform/resolvers/zod'
import { Pencil, Plus, Repeat, Trash2 } from 'lucide-react'
import { useState } from 'react'
import { Controller, useForm, useWatch } from 'react-hook-form'
import { toast } from 'sonner'
import { z } from 'zod'
import { errorMessage } from '@/api/errors'
import type { FinanceCategory, Frequency, Money, RecurringEntry } from '@/api/types'
import { useAuth } from '@/app/providers/auth-provider'
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
import { SwitchField } from '@/components/ui/switch'
import { applyServerErrors } from '@/features/auth/use-server-errors'
import { decimalToInput, isValidDecimal, parseDecimal } from '@/lib/decimal'
import { formatDate, formatMoney, todayIso } from '@/lib/format'
import { FREQUENCY_LABELS } from '@/lib/labels'
import { cn } from '@/lib/utils'
import {
  useDeleteRecurring,
  useFinanceCategories,
  useRecurringEntries,
  useSaveRecurring,
  useSetRecurringActive,
} from './ledger-api'

/** Soma por moeda dos equivalentes mensais (sem converter: os totais convertidos ficam na visão do mês). */
function monthlyTotals(entries: RecurringEntry[], kind: RecurringEntry['kind']) {
  const map = new Map<string, number>()
  entries
    .filter((e) => e.active && e.kind === kind)
    .forEach((e) =>
      map.set(
        e.monthlyEquivalent.currency,
        (map.get(e.monthlyEquivalent.currency) ?? 0) + e.monthlyEquivalent.amount,
      ),
    )
  return [...map.entries()].map(([currency, amount]): Money => ({ currency, amount }))
}

export function RecurringTab() {
  const entries = useRecurringEntries()
  const setActive = useSetRecurringActive()
  const remove = useDeleteRecurring()
  const [editing, setEditing] = useState<RecurringEntry | 'new' | null>(null)
  const [deleting, setDeleting] = useState<RecurringEntry | null>(null)

  const expenses = entries.data ? monthlyTotals(entries.data, 'EXPENSE') : []

  return (
    <Card>
      <CardHeader>
        <div>
          <CardTitle>Assinaturas e recorrências</CardTitle>
          <CardDescription>
            Contas fixas, assinaturas e salário. As próximas ocorrências entram sozinhas nos meses.
            {expenses.length > 0 && (
              <>
                {' '}
                Despesas fixas:{' '}
                <span className="font-medium text-foreground">
                  {expenses.map((m) => formatMoney(m)).join(' + ')}
                </span>
                /mês.
              </>
            )}
          </CardDescription>
        </div>
        <Button size="sm" onClick={() => setEditing('new')}>
          <Plus aria-hidden /> Recorrência
        </Button>
      </CardHeader>
      <CardContent>
        {entries.isPending ? (
          <Skeleton className="h-32" />
        ) : entries.isError ? (
          <ErrorState error={entries.error} onRetry={() => void entries.refetch()} />
        ) : entries.data.length === 0 ? (
          <EmptyState
            icon={Repeat}
            title="Nenhuma recorrência"
            description="Ex.: Spotify, aluguel, plano de celular, salário. Entram no comprometido futuro e na projeção."
            action={<Button onClick={() => setEditing('new')}>Criar recorrência</Button>}
          />
        ) : (
          <ul className="divide-y divide-border">
            {entries.data.map((e) => (
              <li
                key={e.id}
                className={cn('flex flex-wrap items-center gap-3 py-3', !e.active && 'opacity-60')}
              >
                <span
                  aria-hidden
                  className="size-2.5 shrink-0 rounded-full"
                  style={{ backgroundColor: e.category.color }}
                />
                <div className="min-w-0 flex-1">
                  <p className="flex flex-wrap items-center gap-2 font-medium">
                    {e.name}
                    {e.kind === 'INCOME' && <Badge tone="primary">Receita</Badge>}
                    {e.autoConfirm && <Badge>automática</Badge>}
                    {!e.active && <Badge tone="accent">Pausada</Badge>}
                  </p>
                  <p className="text-xs text-muted">
                    {frequencyText(e)} · {e.category.name}
                    {e.nextOccurrence && ` · próxima em ${formatDate(e.nextOccurrence)}`}
                  </p>
                </div>
                <div className="text-right">
                  <p className="font-semibold tabular">{formatMoney(e.amount)}</p>
                  {e.frequency !== 'MONTHLY' && (
                    <p className="text-xs text-muted tabular">≈ {formatMoney(e.monthlyEquivalent)}/mês</p>
                  )}
                </div>
                <div className="flex items-center gap-1">
                  <Button
                    variant="outline"
                    size="sm"
                    loading={setActive.isPending && setActive.variables?.id === e.id}
                    onClick={() =>
                      setActive.mutate(
                        { id: e.id, active: !e.active },
                        { onError: (err) => toast.error(errorMessage(err)) },
                      )
                    }
                  >
                    {e.active ? 'Pausar' : 'Retomar'}
                  </Button>
                  <Button
                    variant="ghost"
                    size="icon"
                    aria-label={`Editar ${e.name}`}
                    onClick={() => setEditing(e)}
                  >
                    <Pencil />
                  </Button>
                  <Button
                    variant="ghost"
                    size="icon"
                    aria-label={`Excluir ${e.name}`}
                    onClick={() => setDeleting(e)}
                  >
                    <Trash2 />
                  </Button>
                </div>
              </li>
            ))}
          </ul>
        )}
      </CardContent>

      {editing !== null && (
        <RecurringDialog entry={editing === 'new' ? undefined : editing} onClose={() => setEditing(null)} />
      )}
      <ConfirmDialog
        open={deleting !== null}
        onOpenChange={(o) => !o && setDeleting(null)}
        title="Excluir recorrência?"
        description="As próximas ocorrências serão removidas; as já confirmadas ficam no histórico. Para só interromper, pause."
        confirmLabel="Excluir"
        destructive
        loading={remove.isPending}
        onConfirm={() =>
          deleting &&
          remove.mutate(deleting.id, {
            onSuccess: () => setDeleting(null),
            onError: (err) => toast.error(errorMessage(err)),
          })
        }
      />
    </Card>
  )
}

function frequencyText(e: RecurringEntry) {
  if (e.frequency === 'EVERY_N_MONTHS') return `A cada ${e.intervalMonths} meses`
  if (e.frequency === 'MONTHLY' && e.billingDay) return `Mensal, dia ${e.billingDay}`
  return FREQUENCY_LABELS[e.frequency]
}

const schema = z
  .object({
    kind: z.enum(['INCOME', 'EXPENSE']),
    name: z.string().trim().min(1, 'Informe o nome.').max(120),
    categoryId: z.string().min(1, 'Escolha a categoria.'),
    amount: z
      .string()
      .refine((v) => isValidDecimal(v) && (parseDecimal(v) ?? 0) > 0, 'Informe um valor maior que zero.'),
    currency: z.enum(['BRL', 'EUR']),
    frequency: z.enum(['WEEKLY', 'MONTHLY', 'YEARLY', 'EVERY_N_MONTHS']),
    intervalMonths: z.string(),
    billingDay: z
      .string()
      .refine((v) => v === '' || (/^\d+$/.test(v) && +v >= 1 && +v <= 31), 'Dia entre 1 e 31.'),
    startDate: z.string().min(1, 'Informe o início.'),
    endDate: z.string(),
    autoConfirm: z.boolean(),
  })
  .refine(
    (v) => v.frequency !== 'EVERY_N_MONTHS' || (/^\d+$/.test(v.intervalMonths) && +v.intervalMonths >= 1),
    {
      path: ['intervalMonths'],
      message: 'Informe a cada quantos meses.',
    },
  )
  .refine((v) => !v.endDate || v.endDate >= v.startDate, {
    path: ['endDate'],
    message: 'O fim deve ser depois do início.',
  })
type Form = z.infer<typeof schema>

/** Montado só quando aberto; o formulário espera as categorias (ver `DialogLoading`). */
function RecurringDialog(props: { entry?: RecurringEntry; onClose: () => void }) {
  const categories = useFinanceCategories()
  if (categories.data) return <RecurringForm {...props} categories={categories.data} />
  return (
    <DialogLoading title={props.entry ? 'Editar recorrência' : 'Nova recorrência'} onClose={props.onClose} />
  )
}

function RecurringForm({
  entry,
  onClose,
  categories,
}: {
  entry?: RecurringEntry
  onClose: () => void
  categories: FinanceCategory[]
}) {
  const { user } = useAuth()
  const save = useSaveRecurring()
  const [formError, setFormError] = useState('')
  const {
    register,
    control,
    handleSubmit,
    setValue,
    setError,
    formState: { errors },
  } = useForm<Form>({
    resolver: zodResolver(schema),
    defaultValues: {
      kind: entry?.kind ?? 'EXPENSE',
      name: entry?.name ?? '',
      categoryId: entry?.category.id ?? '',
      amount: decimalToInput(entry?.amount.amount),
      currency: (entry?.amount.currency ?? user?.settings.primaryCurrency ?? 'BRL') as 'BRL' | 'EUR',
      frequency: entry?.frequency ?? 'MONTHLY',
      intervalMonths: entry?.intervalMonths ? String(entry.intervalMonths) : '',
      billingDay: entry?.billingDay ? String(entry.billingDay) : '',
      startDate: entry?.startDate ?? todayIso(),
      endDate: entry?.endDate ?? '',
      autoConfirm: entry?.autoConfirm ?? false,
    },
  })
  const [kind, currency, frequency] = useWatch({ control, name: ['kind', 'currency', 'frequency'] })

  const onSubmit = handleSubmit((v) =>
    save.mutate(
      {
        id: entry?.id,
        body: {
          kind: v.kind,
          name: v.name.trim(),
          categoryId: v.categoryId,
          amount: parseDecimal(v.amount) ?? 0,
          currency: v.currency,
          frequency: v.frequency as Frequency,
          intervalMonths: v.frequency === 'EVERY_N_MONTHS' ? Number(v.intervalMonths) : undefined,
          billingDay: v.frequency !== 'WEEKLY' && v.billingDay ? Number(v.billingDay) : undefined,
          startDate: v.startDate,
          endDate: v.endDate || undefined,
          autoConfirm: v.autoConfirm,
        },
      },
      {
        onSuccess: () => {
          toast.success(entry ? 'Recorrência atualizada.' : 'Recorrência criada.')
          onClose()
        },
        onError: (e) =>
          setFormError(
            applyServerErrors(e, setError, ['name', 'categoryId', 'amount', 'startDate', 'endDate']),
          ),
      },
    ),
  )

  return (
    <ResponsiveDialog
      open
      onOpenChange={(o) => !o && onClose()}
      title={entry ? 'Editar recorrência' : 'Nova recorrência'}
      description={entry ? 'Ocorrências futuras em aberto serão recalculadas.' : undefined}
      footer={
        <>
          <Button variant="outline" onClick={onClose}>
            Cancelar
          </Button>
          <Button type="submit" form="recurring-form" loading={save.isPending}>
            Salvar
          </Button>
        </>
      }
    >
      <form id="recurring-form" onSubmit={onSubmit} noValidate className="grid gap-4">
        {formError && (
          <div role="alert" className="rounded-xl bg-danger-soft px-4 py-3 text-sm text-danger">
            {formError}
          </div>
        )}
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
        <Field id="rec-name" label="Nome" error={errors.name?.message}>
          <Input
            id="rec-name"
            placeholder={kind === 'INCOME' ? 'Ex.: Salário' : 'Ex.: Spotify'}
            aria-invalid={!!errors.name}
            {...register('name')}
          />
        </Field>
        <Field id="rec-category" label="Categoria" error={errors.categoryId?.message}>
          <NativeSelect id="rec-category" aria-invalid={!!errors.categoryId} {...register('categoryId')}>
            <option value="">Escolha…</option>
            {categories
              .filter((c) => c.kind === kind && (!c.archived || c.id === entry?.category.id))
              .map((c) => (
                <option key={c.id} value={c.id}>
                  {c.name}
                </option>
              ))}
          </NativeSelect>
        </Field>
        <div className="grid grid-cols-[1fr_auto] items-end gap-3">
          <Field id="rec-amount" label="Valor" error={errors.amount?.message}>
            <MoneyInput
              id="rec-amount"
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
          <Field id="rec-frequency" label="Frequência">
            <NativeSelect id="rec-frequency" {...register('frequency')}>
              {Object.entries(FREQUENCY_LABELS).map(([k, l]) => (
                <option key={k} value={k}>
                  {l}
                </option>
              ))}
            </NativeSelect>
          </Field>
          {frequency === 'EVERY_N_MONTHS' ? (
            <Field id="rec-interval" label="A cada (meses)" error={errors.intervalMonths?.message}>
              <Input
                id="rec-interval"
                inputMode="numeric"
                placeholder="3"
                aria-invalid={!!errors.intervalMonths}
                {...register('intervalMonths')}
              />
            </Field>
          ) : frequency !== 'WEEKLY' ? (
            <Field
              id="rec-day"
              label="Dia de cobrança"
              error={errors.billingDay?.message}
              hint="Vazio = dia do início."
            >
              <Input
                id="rec-day"
                inputMode="numeric"
                placeholder="5"
                aria-invalid={!!errors.billingDay}
                {...register('billingDay')}
              />
            </Field>
          ) : null}
          <Field id="rec-start" label="Início" error={errors.startDate?.message}>
            <Input id="rec-start" type="date" aria-invalid={!!errors.startDate} {...register('startDate')} />
          </Field>
          <Field id="rec-end" label="Fim (opcional)" error={errors.endDate?.message}>
            <Input id="rec-end" type="date" aria-invalid={!!errors.endDate} {...register('endDate')} />
          </Field>
        </div>
        <Controller
          control={control}
          name="autoConfirm"
          render={({ field }) => (
            <SwitchField
              id="rec-auto"
              label="Confirmar automaticamente"
              hint="Para débito automático e salário: vira 'confirmado' no dia."
              checked={field.value}
              onCheckedChange={field.onChange}
            />
          )}
        />
      </form>
    </ResponsiveDialog>
  )
}
