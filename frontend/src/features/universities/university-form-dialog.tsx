import { zodResolver } from '@hookform/resolvers/zod'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { toast } from 'sonner'
import { z } from 'zod'
import type { Employability, InstitutionType, Nature, University } from '@/api/types'
import { Button } from '@/components/ui/button'
import { Field } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { MoneyInput } from '@/components/ui/money-input'
import { ResponsiveDialog } from '@/components/ui/responsive-dialog'
import { NativeSelect } from '@/components/ui/select'
import { Textarea } from '@/components/ui/textarea'
import { applyServerErrors } from '@/features/auth/use-server-errors'
import { decimalToInput, isValidDecimal, parseDecimal } from '@/lib/decimal'
import { COUNTRIES, EMPLOYABILITY_LABELS, INSTITUTION_TYPE_LABELS, NATURE_LABELS } from '@/lib/labels'
import { useSaveUniversity } from './api'

const intOrEmpty = z
  .string()
  .refine((v) => v.trim() === '' || /^\d+$/.test(v.trim()), 'Use um número inteiro.')

const schema = z.object({
  name: z.string().trim().min(1, 'Informe o nome.').max(200),
  ranking: intOrEmpty,
  institutionType: z.string(),
  nature: z.string(),
  city: z.string().max(120),
  region: z.string().max(120),
  countryCode: z.string(),
  population: intOrEmpty,
  costOfLiving: z.string().refine((v) => isValidDecimal(v, { allowEmpty: true }), 'Valor inválido.'),
  employability: z.string(),
  website: z.string().max(500),
  notes: z.string().max(2000),
})
type Form = z.infer<typeof schema>

const toInt = (v: string) => (v.trim() === '' ? undefined : Number(v))

/** Montado só quando aberto. */
export function UniversityFormDialog({
  university: u,
  onClose,
  onSaved,
}: {
  university?: University
  onClose: () => void
  onSaved?: (u: University) => void
}) {
  const save = useSaveUniversity()
  const [formError, setFormError] = useState('')
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors },
  } = useForm<Form>({
    resolver: zodResolver(schema),
    defaultValues: {
      name: u?.name ?? '',
      ranking: u?.ranking != null ? String(u.ranking) : '',
      institutionType: u?.institutionType ?? 'PUBLIC',
      nature: u?.nature ?? 'UNIVERSITY',
      city: u?.city ?? '',
      region: u?.region ?? '',
      countryCode: u?.countryCode ?? 'DE',
      population: u?.population != null ? String(u.population) : '',
      costOfLiving: decimalToInput(u?.costOfLiving?.amount),
      employability: u?.employability ?? '',
      website: u?.website ?? '',
      notes: u?.notes ?? '',
    },
  })

  const onSubmit = handleSubmit((v) =>
    save.mutate(
      {
        id: u?.id,
        body: {
          name: v.name.trim(),
          ranking: toInt(v.ranking),
          institutionType: (v.institutionType || undefined) as InstitutionType | undefined,
          nature: (v.nature || undefined) as Nature | undefined,
          city: v.city || undefined,
          region: v.region || undefined,
          countryCode: v.countryCode || undefined,
          population: toInt(v.population),
          costOfLivingAmount: parseDecimal(v.costOfLiving) ?? undefined,
          costOfLivingCurrency: v.costOfLiving.trim() ? 'EUR' : undefined,
          employability: (v.employability || undefined) as Employability | undefined,
          website: v.website || undefined,
          notes: v.notes || undefined,
        },
      },
      {
        onSuccess: (saved) => {
          toast.success(u ? 'Universidade atualizada.' : 'Universidade criada.')
          onClose()
          onSaved?.(saved)
        },
        onError: (e) =>
          setFormError(applyServerErrors(e, setError, ['name', 'ranking', 'countryCode', 'population'])),
      },
    ),
  )

  return (
    <ResponsiveDialog
      open
      onOpenChange={(o) => !o && onClose()}
      title={u ? 'Editar universidade' : 'Nova universidade'}
      footer={
        <>
          <Button variant="outline" onClick={onClose}>
            Cancelar
          </Button>
          <Button type="submit" form="university-form" loading={save.isPending}>
            Salvar
          </Button>
        </>
      }
    >
      <form id="university-form" onSubmit={onSubmit} noValidate className="grid gap-4">
        {formError && (
          <div role="alert" className="rounded-xl bg-danger-soft px-4 py-3 text-sm text-danger">
            {formError}
          </div>
        )}
        <Field id="uni-name" label="Nome" error={errors.name?.message}>
          <Input
            id="uni-name"
            placeholder="Ex.: Technische Universität München"
            aria-invalid={!!errors.name}
            {...register('name')}
          />
        </Field>
        <div className="grid gap-4 sm:grid-cols-2">
          <Field id="uni-nature" label="Natureza">
            <NativeSelect id="uni-nature" {...register('nature')}>
              {Object.entries(NATURE_LABELS).map(([k, l]) => (
                <option key={k} value={k}>
                  {l}
                </option>
              ))}
            </NativeSelect>
          </Field>
          <Field id="uni-type" label="Tipo">
            <NativeSelect id="uni-type" {...register('institutionType')}>
              {Object.entries(INSTITUTION_TYPE_LABELS).map(([k, l]) => (
                <option key={k} value={k}>
                  {l}
                </option>
              ))}
            </NativeSelect>
          </Field>
          <Field id="uni-city" label="Cidade">
            <Input id="uni-city" {...register('city')} />
          </Field>
          <Field id="uni-region" label="Estado">
            <Input id="uni-region" placeholder="Ex.: Bayern" {...register('region')} />
          </Field>
          <Field id="uni-country" label="País">
            <NativeSelect id="uni-country" {...register('countryCode')}>
              {COUNTRIES.map((c) => (
                <option key={c.code} value={c.code}>
                  {c.name}
                </option>
              ))}
            </NativeSelect>
          </Field>
          <Field id="uni-ranking" label="Ranking no país" error={errors.ranking?.message}>
            <Input
              id="uni-ranking"
              inputMode="numeric"
              aria-invalid={!!errors.ranking}
              {...register('ranking')}
            />
          </Field>
          <Field id="uni-cost" label="Custo de vida / mês" error={errors.costOfLiving?.message}>
            <MoneyInput
              id="uni-cost"
              currency="EUR"
              aria-invalid={!!errors.costOfLiving}
              {...register('costOfLiving')}
            />
          </Field>
          <Field id="uni-employability" label="Empregabilidade">
            <NativeSelect id="uni-employability" {...register('employability')}>
              <option value="">—</option>
              {Object.entries(EMPLOYABILITY_LABELS).map(([k, l]) => (
                <option key={k} value={k}>
                  {l}
                </option>
              ))}
            </NativeSelect>
          </Field>
          <Field id="uni-population" label="Habitantes da cidade" error={errors.population?.message}>
            <Input
              id="uni-population"
              inputMode="numeric"
              aria-invalid={!!errors.population}
              {...register('population')}
            />
          </Field>
          <Field id="uni-website" label="Site">
            <Input id="uni-website" type="url" placeholder="https://" {...register('website')} />
          </Field>
        </div>
        <Field id="uni-notes" label="Observações">
          <Textarea id="uni-notes" rows={2} {...register('notes')} />
        </Field>
      </form>
    </ResponsiveDialog>
  )
}
