import { Plus, Trash2 } from 'lucide-react'
import { useState } from 'react'
import { toast } from 'sonner'
import { errorMessage } from '@/api/errors'
import type { ExchangeAlert } from '@/api/types'
import { ErrorState } from '@/components/error-state'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Field } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { NativeSelect } from '@/components/ui/select'
import { Skeleton } from '@/components/ui/skeleton'
import { isValidDecimal, parseDecimal } from '@/lib/decimal'
import { formatDateTime, formatRate } from '@/lib/format'
import { useDeleteExchangeAlert, useExchangeAlerts, useSaveExchangeAlert } from './api'

/**
 * Alertas de cotação do euro. Avisa uma vez quando a condição vale e só volta a avisar depois que a cotação se
 * afastar 0,5% da meta (evita um aviso por atualização enquanto o euro oscila perto do valor).
 */
export function ExchangeAlertsCard() {
  const alerts = useExchangeAlerts()
  const save = useSaveExchangeAlert()
  const remove = useDeleteExchangeAlert()
  const [condition, setCondition] = useState<'BELOW' | 'ABOVE'>('BELOW')
  const [value, setValue] = useState('')
  const [error, setError] = useState('')

  const add = () => {
    if (
      !isValidDecimal(value, { thousands: false, maxFraction: 4 }) ||
      (parseDecimal(value, { thousands: false }) ?? 0) <= 0
    ) {
      setError('Informe uma cotação, ex.: 5,80')
      return
    }
    setError('')
    save.mutate(
      {
        body: {
          base: 'EUR',
          quote: 'BRL',
          condition,
          threshold: parseDecimal(value, { thousands: false }) ?? 0,
        },
      },
      {
        onSuccess: () => {
          toast.success('Alerta criado.')
          setValue('')
        },
        onError: (e) => toast.error(errorMessage(e)),
      },
    )
  }

  return (
    <Card>
      <CardHeader className="flex-col">
        <CardTitle>Alertas de câmbio</CardTitle>
        <CardDescription>
          Receba um aviso quando o euro passar de um valor. Usa a cotação de mercado, não a manual.
        </CardDescription>
      </CardHeader>
      <CardContent className="grid gap-4">
        <form
          className="grid gap-3 sm:grid-cols-[10rem_10rem_auto] sm:items-end"
          onSubmit={(e) => {
            e.preventDefault()
            add()
          }}
        >
          <Field id="alert-condition" label="Avisar quando €1 ficar">
            <NativeSelect
              id="alert-condition"
              value={condition}
              onChange={(e) => setCondition(e.target.value as 'BELOW' | 'ABOVE')}
            >
              <option value="BELOW">abaixo de</option>
              <option value="ABOVE">acima de</option>
            </NativeSelect>
          </Field>
          <Field id="alert-value" label="Cotação (R$)" error={error}>
            <Input
              id="alert-value"
              inputMode="decimal"
              placeholder="5,80"
              value={value}
              onChange={(e) => setValue(e.target.value)}
              aria-invalid={!!error}
            />
          </Field>
          <Button type="submit" loading={save.isPending}>
            <Plus aria-hidden /> Criar alerta
          </Button>
        </form>
        {alerts.isPending ? (
          <Skeleton className="h-16" />
        ) : alerts.isError ? (
          <ErrorState error={alerts.error} onRetry={() => void alerts.refetch()} />
        ) : alerts.data.length > 0 ? (
          <ul className="divide-y divide-border">
            {alerts.data.map((a) => (
              <AlertRow
                key={a.id}
                alert={a}
                deleting={remove.isPending}
                onDelete={() => remove.mutate(a.id, { onError: (e) => toast.error(errorMessage(e)) })}
              />
            ))}
          </ul>
        ) : null}
      </CardContent>
    </Card>
  )
}

function AlertRow({
  alert: a,
  deleting,
  onDelete,
}: {
  alert: ExchangeAlert
  deleting: boolean
  onDelete: () => void
}) {
  return (
    <li className="flex flex-wrap items-center gap-3 py-2.5">
      <div className="min-w-0 flex-1">
        <p className="text-sm font-medium">
          €1 {a.condition === 'BELOW' ? 'abaixo de' : 'acima de'} R$ {formatRate(a.threshold)}
        </p>
        <p className="text-xs text-muted">
          {a.lastRate != null ? `Agora: R$ ${formatRate(a.lastRate)}` : 'Aguardando a próxima cotação'}
          {a.lastTriggeredAt && ` · último aviso ${formatDateTime(a.lastTriggeredAt)}`}
        </p>
      </div>
      <Badge tone={a.state === 'TRIGGERED' ? 'accent' : 'neutral'}>
        {a.state === 'TRIGGERED' ? 'Avisado · rearma ao se afastar' : 'Ativo'}
      </Badge>
      <Button variant="ghost" size="icon" aria-label="Excluir alerta" disabled={deleting} onClick={onDelete}>
        <Trash2 />
      </Button>
    </li>
  )
}
