import { zodResolver } from '@hookform/resolvers/zod'
import { Monitor, Moon, Sun } from 'lucide-react'
import { useForm } from 'react-hook-form'
import { toast } from 'sonner'
import { z } from 'zod'
import { errorMessage } from '@/api/errors'
import type { ThemePreference, UserSettings } from '@/api/types'
import { useAuth } from '@/app/providers/auth-provider'
import { useTheme } from '@/app/providers/theme-provider'
import { PageHeader } from '@/components/page-header'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Field } from '@/components/ui/field'
import { SegmentedControl } from '@/components/ui/segmented'
import { NativeSelect } from '@/components/ui/select'
import { Skeleton } from '@/components/ui/skeleton'
import { useCurrencies, useUpdateSettings } from './api'
import { CatalogCard } from './catalog-card'
import { ExchangeRateCard } from './exchange-rate-card'

const TIMEZONES = [
  'America/Sao_Paulo',
  'America/Manaus',
  'America/Fortaleza',
  'America/Recife',
  'America/Belem',
  'America/Cuiaba',
  'America/Rio_Branco',
  'America/Noronha',
  'Europe/Berlin',
  'Europe/Lisbon',
  'Europe/London',
  'UTC',
]

const preferencesSchema = z
  .object({
    primaryCurrency: z.string().length(3),
    secondaryCurrency: z.string().length(3),
    timezone: z.string().min(1),
  })
  .refine((v) => v.primaryCurrency !== v.secondaryCurrency, {
    path: ['secondaryCurrency'],
    message: 'Escolha uma moeda diferente da principal.',
  })

type PreferencesForm = z.infer<typeof preferencesSchema>

function ThemeCard({ settings }: { settings: UserSettings }) {
  const { preference, setPreference } = useTheme()
  const { user, setUser } = useAuth()
  const update = useUpdateSettings()

  const onChange = (theme: ThemePreference) => {
    const previous = preference
    setPreference(theme) // aplica na hora; persiste no servidor em seguida
    update.mutate(
      { ...settings, theme },
      {
        onSuccess: (saved) => user && setUser({ ...user, settings: saved }),
        onError: (error) => {
          setPreference(previous)
          toast.error(errorMessage(error))
        },
      },
    )
  }

  return (
    <Card>
      <CardHeader className="flex-col">
        <CardTitle>Aparência</CardTitle>
        <CardDescription>O tema é salvo na sua conta e vale para todos os seus dispositivos.</CardDescription>
      </CardHeader>
      <CardContent>
        <SegmentedControl
          ariaLabel="Tema"
          value={preference}
          onValueChange={onChange}
          options={[
            { value: 'LIGHT', label: 'Claro', icon: <Sun aria-hidden /> },
            { value: 'DARK', label: 'Escuro', icon: <Moon aria-hidden /> },
            { value: 'SYSTEM', label: 'Sistema', icon: <Monitor aria-hidden /> },
          ]}
        />
      </CardContent>
    </Card>
  )
}

function PreferencesCard({ settings }: { settings: UserSettings }) {
  const { user, setUser } = useAuth()
  const currencies = useCurrencies()
  const update = useUpdateSettings()
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isDirty },
  } = useForm<PreferencesForm>({
    resolver: zodResolver(preferencesSchema),
    values: {
      primaryCurrency: settings.primaryCurrency,
      secondaryCurrency: settings.secondaryCurrency,
      timezone: settings.timezone,
    },
  })

  const onSubmit = handleSubmit((values) =>
    update.mutate(
      { ...settings, ...values },
      {
        onSuccess: (saved) => {
          if (user) setUser({ ...user, settings: saved })
          reset(values)
          toast.success('Preferências salvas.')
        },
        onError: (error) => toast.error(errorMessage(error)),
      },
    ),
  )

  const timezones = TIMEZONES.includes(settings.timezone) ? TIMEZONES : [settings.timezone, ...TIMEZONES]

  return (
    <Card>
      <CardHeader className="flex-col">
        <CardTitle>Moedas e região</CardTitle>
        <CardDescription>
          A moeda principal é usada nos totais; a secundária aparece como equivalente (ex.: € 500 ≈ R$ 3.100).
        </CardDescription>
      </CardHeader>
      <CardContent>
        {currencies.isPending ? (
          <div className="grid gap-4 sm:grid-cols-3">
            <Skeleton className="h-16" />
            <Skeleton className="h-16" />
            <Skeleton className="h-16" />
          </div>
        ) : (
          <form onSubmit={onSubmit} className="grid gap-5">
            <div className="grid gap-4 sm:grid-cols-3">
              <Field id="primaryCurrency" label="Moeda principal" error={errors.primaryCurrency?.message}>
                <NativeSelect id="primaryCurrency" {...register('primaryCurrency')}>
                  {(currencies.data ?? []).map((c) => (
                    <option key={c.code} value={c.code}>
                      {c.code} — {c.name}
                    </option>
                  ))}
                </NativeSelect>
              </Field>
              <Field
                id="secondaryCurrency"
                label="Moeda secundária"
                error={errors.secondaryCurrency?.message}
              >
                <NativeSelect
                  id="secondaryCurrency"
                  aria-invalid={!!errors.secondaryCurrency}
                  {...register('secondaryCurrency')}
                >
                  {(currencies.data ?? []).map((c) => (
                    <option key={c.code} value={c.code}>
                      {c.code} — {c.name}
                    </option>
                  ))}
                </NativeSelect>
              </Field>
              <Field id="timezone" label="Fuso horário" hint="Usado em lembretes e prazos.">
                <NativeSelect id="timezone" {...register('timezone')}>
                  {timezones.map((tz) => (
                    <option key={tz} value={tz}>
                      {tz.replace('_', ' ')}
                    </option>
                  ))}
                </NativeSelect>
              </Field>
            </div>
            <div className="flex justify-end">
              <Button
                type="submit"
                disabled={!isDirty}
                loading={update.isPending}
                className="w-full sm:w-auto"
              >
                Salvar preferências
              </Button>
            </div>
          </form>
        )}
      </CardContent>
    </Card>
  )
}

export function SettingsPage() {
  const { user } = useAuth()
  if (!user) return null
  return (
    <>
      <PageHeader title="Configurações" description="Aparência, moedas, cotação e catálogo de gastos." />
      <div className="grid gap-5">
        <Card>
          <CardHeader className="flex-col">
            <CardTitle>Perfil</CardTitle>
          </CardHeader>
          <CardContent className="grid gap-1 text-sm">
            <p className="font-medium">{user.displayName}</p>
            <p className="text-muted">{user.email}</p>
          </CardContent>
        </Card>
        <ThemeCard settings={user.settings} />
        <PreferencesCard settings={user.settings} />
        <ExchangeRateCard />
        <CatalogCard />
      </div>
    </>
  )
}
