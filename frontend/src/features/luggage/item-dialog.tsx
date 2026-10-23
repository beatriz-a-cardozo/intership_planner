import { zodResolver } from '@hookform/resolvers/zod'
import { useState } from 'react'
import { Controller, useForm } from 'react-hook-form'
import { toast } from 'sonner'
import { z } from 'zod'
import type { ItemCategory, LuggageBag, LuggageItem, Priority } from '@/api/types'
import { Button } from '@/components/ui/button'
import { Field } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { ResponsiveDialog } from '@/components/ui/responsive-dialog'
import { SegmentedControl } from '@/components/ui/segmented'
import { NativeSelect } from '@/components/ui/select'
import { SwitchField } from '@/components/ui/switch'
import { Textarea } from '@/components/ui/textarea'
import { applyServerErrors } from '@/features/auth/use-server-errors'
import { ITEM_CATEGORY_LABELS, PRIORITY_LABELS } from '@/lib/labels'
import { useSaveItem } from './api'
import { fromGrams, isValidWeight, naturalUnit, toGrams } from './weight-input'

const schema = z.object({
  name: z.string().trim().min(1, 'Informe o item.').max(120),
  bagId: z.string(),
  category: z.enum([
    'DOCUMENTS',
    'CLOTHING',
    'ELECTRONICS',
    'HYGIENE',
    'HEALTH',
    'ACCESSORIES',
    'FOOD',
    'OTHER',
  ]),
  quantity: z.string().refine((v) => /^\d+$/.test(v) && +v >= 1 && +v <= 999, 'Entre 1 e 999.'),
  weight: z.string().refine(isValidWeight, 'Peso inválido.'),
  unit: z.enum(['g', 'kg']),
  priority: z.enum(['LOW', 'MEDIUM', 'HIGH', 'URGENT']),
  required: z.boolean(),
  packed: z.boolean(),
  notes: z.string().max(500),
})
type Form = z.infer<typeof schema>

/**
 * Item da mala (montado só quando aberto). As malas chegam prontas da página (já carregadas), então o
 * select da mala nasce com a opção certa.
 */
export function ItemDialog({
  item,
  bagId,
  bags,
  onClose,
}: {
  item?: LuggageItem
  bagId?: string | null
  bags: LuggageBag[]
  onClose: () => void
}) {
  const save = useSaveItem()
  const [formError, setFormError] = useState('')
  const unit = naturalUnit(item?.unitWeightG, 'g')
  const {
    register,
    control,
    handleSubmit,
    setError,
    formState: { errors },
  } = useForm<Form>({
    resolver: zodResolver(schema),
    defaultValues: {
      name: item?.name ?? '',
      bagId: item ? (item.bagId ?? '') : (bagId ?? bags[0]?.id ?? ''),
      category: item?.category ?? 'OTHER',
      quantity: String(item?.quantity ?? 1),
      weight: fromGrams(item?.unitWeightG, unit),
      unit,
      priority: item?.priority ?? 'MEDIUM',
      required: item?.required ?? false,
      packed: item?.packed ?? false,
      notes: item?.notes ?? '',
    },
  })

  const onSubmit = handleSubmit((v) =>
    save.mutate(
      {
        id: item?.id,
        body: {
          name: v.name.trim(),
          bagId: v.bagId || undefined,
          category: v.category as ItemCategory,
          quantity: Number(v.quantity),
          unitWeightG: toGrams(v.weight, v.unit),
          priority: v.priority as Priority,
          required: v.required,
          packed: v.packed,
          notes: v.notes.trim() || undefined,
        },
      },
      {
        onSuccess: () => {
          toast.success(item ? 'Item atualizado.' : 'Item adicionado.')
          onClose()
        },
        onError: (e) =>
          setFormError(
            applyServerErrors(e, setError, ['name', 'bagId', 'quantity'], { unitWeightG: 'weight' }),
          ),
      },
    ),
  )

  return (
    <ResponsiveDialog
      open
      onOpenChange={(o) => !o && onClose()}
      title={item ? 'Editar item' : 'Novo item'}
      footer={
        <>
          <Button variant="outline" onClick={onClose}>
            Cancelar
          </Button>
          <Button type="submit" form="item-form" loading={save.isPending}>
            Salvar
          </Button>
        </>
      }
    >
      <form id="item-form" onSubmit={onSubmit} noValidate className="grid gap-4">
        {formError && (
          <div role="alert" className="rounded-xl bg-danger-soft px-4 py-3 text-sm text-danger">
            {formError}
          </div>
        )}
        <Field id="item-name" label="Item" error={errors.name?.message}>
          <Input
            id="item-name"
            placeholder="Ex.: Passaporte"
            aria-invalid={!!errors.name}
            {...register('name')}
          />
        </Field>
        <div className="grid grid-cols-2 gap-4">
          <Field id="item-bag" label="Mala" error={errors.bagId?.message}>
            <NativeSelect id="item-bag" {...register('bagId')}>
              <option value="">Ainda não alocado</option>
              {bags.map((b) => (
                <option key={b.id} value={b.id}>
                  {b.name}
                </option>
              ))}
            </NativeSelect>
          </Field>
          <Field id="item-category" label="Categoria">
            <NativeSelect id="item-category" {...register('category')}>
              {Object.entries(ITEM_CATEGORY_LABELS).map(([k, l]) => (
                <option key={k} value={k}>
                  {l}
                </option>
              ))}
            </NativeSelect>
          </Field>
        </div>
        <div className="grid grid-cols-[6rem_1fr_auto] items-end gap-3">
          <Field id="item-qty" label="Qtd." error={errors.quantity?.message}>
            <Input
              id="item-qty"
              inputMode="numeric"
              aria-invalid={!!errors.quantity}
              {...register('quantity')}
            />
          </Field>
          <Field id="item-weight" label="Peso de cada" error={errors.weight?.message}>
            <Input id="item-weight" inputMode="decimal" placeholder="opcional" {...register('weight')} />
          </Field>
          <Controller
            control={control}
            name="unit"
            render={({ field }) => (
              <SegmentedControl
                ariaLabel="Unidade do peso"
                value={field.value}
                onValueChange={field.onChange}
                options={[
                  { value: 'g', label: 'g' },
                  { value: 'kg', label: 'kg' },
                ]}
              />
            )}
          />
        </div>
        <Field id="item-priority" label="Prioridade">
          <NativeSelect id="item-priority" {...register('priority')}>
            {Object.entries(PRIORITY_LABELS).map(([k, l]) => (
              <option key={k} value={k}>
                {l}
              </option>
            ))}
          </NativeSelect>
        </Field>
        <Controller
          control={control}
          name="required"
          render={({ field }) => (
            <SwitchField
              id="item-required"
              label="Obrigatório"
              hint="Aparece no alerta enquanto não estiver embalado"
              checked={field.value}
              onCheckedChange={field.onChange}
            />
          )}
        />
        <Controller
          control={control}
          name="packed"
          render={({ field }) => (
            <SwitchField
              id="item-packed"
              label="Já embalado"
              checked={field.value}
              onCheckedChange={field.onChange}
            />
          )}
        />
        <Field id="item-notes" label="Observações">
          <Textarea id="item-notes" rows={2} {...register('notes')} />
        </Field>
      </form>
    </ResponsiveDialog>
  )
}
