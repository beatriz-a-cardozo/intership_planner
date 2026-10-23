import { useState } from 'react'
import { toast } from 'sonner'
import { errorMessage } from '@/api/errors'
import type { NotificationPreference } from '@/api/types'
import { ErrorState } from '@/components/error-state'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Skeleton } from '@/components/ui/skeleton'
import { SwitchField } from '@/components/ui/switch'
import { NOTIFICATION_CATEGORY_LABELS } from '@/lib/labels'
import { useNotificationPreferences, useSavePreference } from './api'
import { parseLeadDays } from './lead-days'

export function NotificationPreferencesCard() {
  const prefs = useNotificationPreferences()
  return (
    <Card id="notificacoes">
      <CardHeader className="flex-col">
        <CardTitle>Notificações</CardTitle>
        <CardDescription>
          Escolha o que avisar e com quantos dias de antecedência (ex.: 30, 7, 1 = um mês, uma semana e um dia
          antes).
        </CardDescription>
      </CardHeader>
      <CardContent>
        {prefs.isPending ? (
          <Skeleton className="h-48" />
        ) : prefs.isError ? (
          <ErrorState error={prefs.error} onRetry={() => void prefs.refetch()} />
        ) : (
          <ul className="divide-y divide-border">
            {prefs.data.map((p) => (
              <PreferenceRow key={p.category} pref={p} />
            ))}
          </ul>
        )}
      </CardContent>
    </Card>
  )
}

function PreferenceRow({ pref }: { pref: NotificationPreference }) {
  const save = useSavePreference()
  const [text, setText] = useState(pref.leadDays.join(', '))
  const [error, setError] = useState('')
  const id = `pref-${pref.category}`

  const submit = (enabled: boolean, leads: number[] | undefined) =>
    save.mutate(
      { category: pref.category, body: { enabled, leadDays: leads } },
      {
        onSuccess: () => toast.success('Preferência salva.'),
        onError: (e) => toast.error(errorMessage(e)),
      },
    )

  return (
    <li className="grid gap-2 py-3 sm:grid-cols-[1fr_auto] sm:items-center">
      <SwitchField
        id={id}
        label={NOTIFICATION_CATEGORY_LABELS[pref.category]}
        hint={pref.usesLeadDays ? `Padrão: ${pref.defaultLeadDays.join(', ')} dias antes` : undefined}
        checked={pref.enabled}
        disabled={save.isPending}
        onCheckedChange={(enabled) => submit(enabled, pref.usesLeadDays ? pref.leadDays : undefined)}
      />
      {pref.usesLeadDays && (
        <form
          className="flex items-start gap-2"
          onSubmit={(e) => {
            e.preventDefault()
            const leads = parseLeadDays(text)
            if (!leads) {
              setError('Use até 5 números de 0 a 365, separados por vírgula.')
              return
            }
            setError('')
            setText(leads.join(', '))
            submit(pref.enabled, leads)
          }}
        >
          <div>
            <label htmlFor={`${id}-leads`} className="sr-only">
              Antecedências em dias para {NOTIFICATION_CATEGORY_LABELS[pref.category]}
            </label>
            <Input
              id={`${id}-leads`}
              value={text}
              onChange={(e) => setText(e.target.value)}
              className="h-9 w-32"
              inputMode="numeric"
              disabled={!pref.enabled}
              aria-invalid={!!error}
              aria-describedby={error ? `${id}-err` : undefined}
            />
            {error && (
              <p id={`${id}-err`} className="mt-1 max-w-48 text-xs text-danger">
                {error}
              </p>
            )}
          </div>
          <Button type="submit" size="sm" variant="outline" disabled={!pref.enabled || save.isPending}>
            Salvar
          </Button>
        </form>
      )}
    </li>
  )
}
