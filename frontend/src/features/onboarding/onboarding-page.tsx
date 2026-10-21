import { zodResolver } from '@hookform/resolvers/zod'
import { useQueryClient } from '@tanstack/react-query'
import { ArrowLeft, ArrowRight, Check } from 'lucide-react'
import { useState } from 'react'
import { Controller, useForm, useWatch } from 'react-hook-form'
import { useNavigate } from 'react-router'
import { api } from '@/api/client'
import { ApiError } from '@/api/errors'
import { invalidateMoney } from '@/api/query-keys'
import type { OnboardingResponse } from '@/api/types'
import { useAuth } from '@/app/providers/auth-provider'
import { Brand } from '@/components/brand'
import { Button } from '@/components/ui/button'
import { describedBy, Field } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { MoneyInput } from '@/components/ui/money-input'
import { SegmentedControl } from '@/components/ui/segmented'
import { NativeSelect } from '@/components/ui/select'
import { applyServerErrors } from '@/features/auth/use-server-errors'
import { COUNTRIES } from '@/lib/labels'
import { cn } from '@/lib/utils'
import {
  onboardingDefaults,
  onboardingSchema,
  STEP_FIELDS,
  toOnboardingRequest,
  type OnboardingForm,
} from './schema'

const STEPS = [
  { title: 'Qual é o seu objetivo?', subtitle: 'Dê um nome que te motive a abrir o app todo dia.' },
  { title: 'Para onde você vai?', subtitle: 'País e cidade do intercâmbio (a cidade é opcional).' },
  {
    title: 'Quando você quer estar lá?',
    subtitle: 'A data alvo define quantos meses você tem para se preparar.',
  },
  {
    title: 'Quanto você já guardou?',
    subtitle: 'Vira uma conta "Reserva do intercâmbio", que você atualiza depois.',
  },
  {
    title: 'Quanto consegue guardar por mês?',
    subtitle: 'Usamos para dizer se você está no ritmo certo. Pode pular.',
  },
  { title: 'Moeda e meta', subtitle: 'Em que moeda você quer acompanhar o total, e como a meta é definida.' },
]

const TARGET_DATE_HINT = 'Ex.: o dia do voo ou o início das aulas.'
const SUGGESTIONS = ['Mestrado na Alemanha', 'Intercâmbio de idiomas', 'Graduação sanduíche']
const CURRENCY_OPTIONS = [
  { value: 'BRL' as const, label: 'Real (R$)' },
  { value: 'EUR' as const, label: 'Euro (€)' },
]

export function OnboardingPage() {
  const [step, setStep] = useState(0)
  const [formError, setFormError] = useState('')
  const { user, setUser } = useAuth()
  const navigate = useNavigate()
  const queryClient = useQueryClient()

  const {
    register,
    control,
    handleSubmit,
    trigger,
    setValue,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<OnboardingForm>({ resolver: zodResolver(onboardingSchema), defaultValues: onboardingDefaults })

  const last = step === STEPS.length - 1
  const firstName = user?.displayName.split(' ')[0] ?? ''

  const next = async () => {
    if (await trigger(STEP_FIELDS[step])) setStep((s) => s + 1)
  }

  const onSubmit = handleSubmit(async (values) => {
    setFormError('')
    try {
      const result = await api.post<OnboardingResponse>('/api/onboarding', toOnboardingRequest(values))
      invalidateMoney(queryClient)
      setUser(result.user)
      navigate('/', { replace: true })
    } catch (error) {
      const message = applyServerErrors(
        error,
        setError,
        Object.keys(onboardingDefaults) as (keyof OnboardingForm)[],
      )
      setFormError(message)
      // volta para o passo do primeiro campo com erro do servidor
      const serverFields = error instanceof ApiError ? Object.keys(error.fieldErrors) : []
      const firstStepWithError = STEP_FIELDS.findIndex((fields) =>
        fields.some((f) => serverFields.includes(f)),
      )
      if (firstStepWithError >= 0) setStep(firstStepWithError)
    }
  })

  const targetMode = useWatch({ control, name: 'targetMode' })
  const currency = useWatch({ control, name: 'currency' })
  const savingsCurrency = useWatch({ control, name: 'currentSavingsCurrency' })

  return (
    <div className="flex min-h-dvh flex-col bg-background">
      <header className="flex items-center justify-between px-5 py-5 sm:px-10">
        <Brand />
        <span className="text-sm text-muted tabular" aria-live="polite">
          Passo {step + 1} de {STEPS.length}
        </span>
      </header>

      <div className="px-5 sm:px-10" aria-hidden>
        <div className="mx-auto flex max-w-xl gap-1.5">
          {STEPS.map((_, i) => (
            <span
              key={i}
              className={cn('h-1.5 flex-1 rounded-full', i <= step ? 'bg-primary' : 'bg-surface-2')}
            />
          ))}
        </div>
      </div>

      <form
        onSubmit={(e) => {
          e.preventDefault()
          if (last) void onSubmit()
          else void next()
        }}
        noValidate
        className="mx-auto flex w-full max-w-xl flex-1 flex-col px-5 pb-8 pt-10 sm:px-0"
      >
        {step === 0 && firstName && (
          <p className="mb-2 text-sm text-muted">Olá, {firstName}! Vamos montar seu plano.</p>
        )}
        <h1 className="font-display text-3xl font-semibold tracking-tight sm:text-4xl">
          {STEPS[step]!.title}
        </h1>
        <p className="mt-2 text-sm text-muted sm:text-base">{STEPS[step]!.subtitle}</p>

        {formError && (
          <div role="alert" className="mt-6 rounded-xl bg-danger-soft px-4 py-3 text-sm text-danger">
            {formError}
          </div>
        )}

        <div className="mt-8 grid gap-5">
          {step === 0 && (
            <>
              <Field id="objectiveName" label="Nome do objetivo" error={errors.objectiveName?.message}>
                <Input
                  id="objectiveName"
                  autoFocus
                  placeholder="Ex.: Mestrado na Alemanha"
                  aria-invalid={!!errors.objectiveName}
                  aria-describedby={describedBy('objectiveName', errors.objectiveName?.message)}
                  {...register('objectiveName')}
                />
              </Field>
              <div className="flex flex-wrap gap-2">
                {SUGGESTIONS.map((s) => (
                  <button
                    key={s}
                    type="button"
                    onClick={() => setValue('objectiveName', s, { shouldValidate: true })}
                    className="h-9 rounded-full border border-border bg-surface px-3.5 text-sm text-muted hover:border-primary hover:text-primary"
                  >
                    {s}
                  </button>
                ))}
              </div>
            </>
          )}

          {step === 1 && (
            <div className="grid gap-4 sm:grid-cols-2">
              <Field id="countryCode" label="País" error={errors.countryCode?.message}>
                <NativeSelect id="countryCode" {...register('countryCode')}>
                  {COUNTRIES.map((c) => (
                    <option key={c.code} value={c.code}>
                      {c.name}
                    </option>
                  ))}
                </NativeSelect>
              </Field>
              <Field id="city" label="Cidade (opcional)" error={errors.city?.message}>
                <Input id="city" placeholder="Ex.: Munique" {...register('city')} />
              </Field>
            </div>
          )}

          {step === 2 && (
            <Field
              id="targetDate"
              label="Data alvo"
              error={errors.targetDate?.message}
              hint={TARGET_DATE_HINT}
            >
              <Input
                id="targetDate"
                type="date"
                aria-invalid={!!errors.targetDate}
                aria-describedby={describedBy('targetDate', errors.targetDate?.message, TARGET_DATE_HINT)}
                {...register('targetDate')}
              />
            </Field>
          )}

          {step === 3 && (
            <>
              <Controller
                control={control}
                name="currentSavingsCurrency"
                render={({ field }) => (
                  <SegmentedControl
                    ariaLabel="Moeda do valor guardado"
                    value={field.value}
                    onValueChange={field.onChange}
                    options={CURRENCY_OPTIONS}
                  />
                )}
              />
              <Field
                id="currentSavings"
                label="Valor já guardado (opcional)"
                error={errors.currentSavings?.message}
              >
                <MoneyInput
                  id="currentSavings"
                  currency={savingsCurrency}
                  placeholder="0,00"
                  aria-invalid={!!errors.currentSavings}
                  {...register('currentSavings')}
                />
              </Field>
            </>
          )}

          {step === 4 && (
            <Field
              id="plannedMonthlySaving"
              label={`Economia mensal planejada em ${currency === 'EUR' ? 'euros' : 'reais'} (opcional)`}
              error={errors.plannedMonthlySaving?.message}
            >
              <MoneyInput
                id="plannedMonthlySaving"
                currency={currency}
                placeholder="0,00"
                aria-invalid={!!errors.plannedMonthlySaving}
                {...register('plannedMonthlySaving')}
              />
            </Field>
          )}

          {step === 5 && (
            <>
              <div className="grid gap-1.5">
                <span className="text-sm font-medium">Moeda principal do objetivo</span>
                <Controller
                  control={control}
                  name="currency"
                  render={({ field }) => (
                    <SegmentedControl
                      ariaLabel="Moeda do objetivo"
                      value={field.value}
                      onValueChange={field.onChange}
                      options={CURRENCY_OPTIONS}
                    />
                  )}
                />
              </div>
              <div className="grid gap-1.5">
                <span className="text-sm font-medium">Meta</span>
                <Controller
                  control={control}
                  name="targetMode"
                  render={({ field }) => (
                    <SegmentedControl
                      ariaLabel="Como definir a meta"
                      value={field.value}
                      onValueChange={field.onChange}
                      options={[
                        { value: 'DERIVED', label: 'Calcular pelos gastos' },
                        { value: 'MANUAL', label: 'Definir um valor' },
                      ]}
                    />
                  )}
                />
                <p className="text-xs text-muted">
                  {targetMode === 'DERIVED'
                    ? 'A meta será a soma dos gastos que você cadastrar (e pode mudar depois).'
                    : 'Você informa o valor total que quer juntar.'}
                </p>
              </div>
              {targetMode === 'MANUAL' && (
                <Field id="targetAmount" label="Valor da meta" error={errors.targetAmount?.message}>
                  <MoneyInput
                    id="targetAmount"
                    currency={currency}
                    placeholder="0,00"
                    aria-invalid={!!errors.targetAmount}
                    {...register('targetAmount')}
                  />
                </Field>
              )}
            </>
          )}
        </div>

        <div className="mt-auto flex gap-3 pt-10">
          {step > 0 && (
            <Button type="button" variant="outline" size="lg" onClick={() => setStep((s) => s - 1)}>
              <ArrowLeft aria-hidden /> Voltar
            </Button>
          )}
          <Button type="submit" size="lg" className="flex-1" loading={isSubmitting}>
            {last ? (
              <>
                <Check aria-hidden /> Começar
              </>
            ) : (
              <>
                Continuar <ArrowRight aria-hidden />
              </>
            )}
          </Button>
        </div>
      </form>
    </div>
  )
}
