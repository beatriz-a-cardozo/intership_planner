import { zodResolver } from '@hookform/resolvers/zod'
import { useState } from 'react'
import { Controller, useForm } from 'react-hook-form'
import { toast } from 'sonner'
import { z } from 'zod'
import type { BagType, LuggageBag } from '@/api/types'
import { Button } from '@/components/ui/button'
import { Field } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { ResponsiveDialog } from '@/components/ui/responsive-dialog'
import { SegmentedControl } from '@/components/ui/segmented'
import { NativeSelect } from '@/components/ui/select'
import { applyServerErrors } from '@/features/auth/use-server-errors'
import { BAG_TYPE_LABELS } from '@/lib/labels'
import { useSaveBag } from './api'
import { fromGrams, isValidWeight, naturalUnit, toGrams } from './weight-input'

const schema = z.object({
  name: z.string().trim().min(1, 'Informe o nome.').max(80),
  type: z.enum(['CARRY_ON', 'CHECKED', 'PERSONAL_ITEM', 'OTHER']),
  maxWeight: z.string().refine(isValidWeight, 'Peso inválido.'),
  unit: z.enum(['g', 'kg']),
})
type Form = z.infer<typeof schema>

/** Montado só quando aberto. */
export function BagDialog({ bag, onClose }: { bag?: LuggageBag; onClose: () => void }) {
  const save = useSaveBag()
  const [formError, setFormError] = useState('')
  const unit = naturalUnit(bag?.maxWeightG, 'kg')
  const {
    register,
    control,
    handleSubmit,
    setError,
    formState: { errors },
  } = useForm<Form>({
    resolver: zodResolver(schema),
    defaultValues: {
      name: bag?.name ?? '',
      type: bag?.type ?? 'CHECKED',
      maxWeight: fromGrams(bag?.maxWeightG, unit),
      unit,
    },
  })

  const onSubmit = handleSubmit((v) =>
    save.mutate(
      {
        id: bag?.id,
        body: { name: v.name.trim(), type: v.type as BagType, maxWeightG: toGrams(v.maxWeight, v.unit) },
      },
      {
        onSuccess: () => {
          toast.success(bag ? 'Mala atualizada.' : 'Mala criada.')
          onClose()
        },
        onError: (e) => setFormError(applyServerErrors(e, setError, ['name'], { maxWeightG: 'maxWeight' })),
      },
    ),
  )

  return (
    <ResponsiveDialog
      open
      onOpenChange={(o) => !o && onClose()}
      title={bag ? 'Editar mala' : 'Nova mala'}
      footer={
        <>
          <Button variant="outline" onClick={onClose}>
            Cancelar
          </Button>
          <Button type="submit" form="bag-form" loading={save.isPending}>
            Salvar
          </Button>
        </>
      }
    >
      <form id="bag-form" onSubmit={onSubmit} noValidate className="grid gap-4">
        {formError && (
          <div role="alert" className="rounded-xl bg-danger-soft px-4 py-3 text-sm text-danger">
            {formError}
          </div>
        )}
        <Field id="bag-name" label="Nome" error={errors.name?.message}>
          <Input
            id="bag-name"
            placeholder="Ex.: Mala azul"
            aria-invalid={!!errors.name}
            {...register('name')}
          />
        </Field>
        <Field id="bag-type" label="Tipo">
          <NativeSelect id="bag-type" {...register('type')}>
            {Object.entries(BAG_TYPE_LABELS).map(([k, l]) => (
              <option key={k} value={k}>
                {l}
              </option>
            ))}
          </NativeSelect>
        </Field>
        <div className="grid grid-cols-[1fr_auto] items-end gap-3">
          <Field
            id="bag-max"
            label="Limite de peso"
            hint="O da companhia aérea (vazio = sem limite)"
            error={errors.maxWeight?.message}
          >
            <Input id="bag-max" inputMode="decimal" placeholder="23" {...register('maxWeight')} />
          </Field>
          <Controller
            control={control}
            name="unit"
            render={({ field }) => (
              <SegmentedControl
                ariaLabel="Unidade"
                value={field.value}
                onValueChange={field.onChange}
                options={[
                  { value: 'kg', label: 'kg' },
                  { value: 'g', label: 'g' },
                ]}
              />
            )}
          />
        </div>
      </form>
    </ResponsiveDialog>
  )
}
