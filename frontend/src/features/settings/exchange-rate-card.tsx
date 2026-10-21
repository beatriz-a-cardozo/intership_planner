import { useState } from 'react'
import { toast } from 'sonner'
import { errorMessage } from '@/api/errors'
import { RateBadge } from '@/components/rate-badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Field } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { Skeleton } from '@/components/ui/skeleton'
import { useClearRateOverride, useExchangeRate, useSetRateOverride } from '@/features/finance/api'
import { isValidDecimal, parseDecimal } from '@/lib/decimal'
import { formatRate } from '@/lib/format'
import { rateSourceLabel } from '@/lib/labels'

/** Cotação manual EUR→BRL: prevalece em todos os cálculos até ser removida. */
export function ExchangeRateCard() {
  const rate = useExchangeRate('EUR', 'BRL')
  const setOverride = useSetRateOverride()
  const clearOverride = useClearRateOverride()
  const [value, setValue] = useState('')
  const [error, setError] = useState('')

  const submit = (e: React.FormEvent) => {
    e.preventDefault()
    const n = parseDecimal(value, { thousands: false })
    if (!isValidDecimal(value, { thousands: false, maxFraction: 8 }) || !n || n <= 0) {
      setError('Informe uma cotação maior que zero (ex.: 6,20).')
      return
    }
    setError('')
    setOverride.mutate(
      { base: 'EUR', quote: 'BRL', rate: n },
      {
        onSuccess: () => {
          setValue('')
          toast.success('Cotação manual ativada.')
        },
        onError: (err) => setError(errorMessage(err)),
      },
    )
  }

  return (
    <Card>
      <CardHeader>
        <div>
          <CardTitle>Cotação do euro</CardTitle>
          <CardDescription>
            Usamos a cotação de referência automaticamente. Defina uma manual para simular ou usar a taxa do
            seu banco.
          </CardDescription>
        </div>
      </CardHeader>
      <CardContent className="grid gap-4">
        {rate.isPending ? (
          <Skeleton className="h-14" />
        ) : rate.isError ? (
          <p className="text-sm text-danger">{errorMessage(rate.error)}</p>
        ) : (
          <div className="flex flex-wrap items-center justify-between gap-3 rounded-xl bg-surface-2 p-4">
            <div>
              <p className="font-display text-2xl font-semibold tabular">
                € 1 = R$ {formatRate(rate.data.rate)}
              </p>
              <p className="text-xs text-muted">
                Fonte: {rateSourceLabel(rate.data.source)}
                {rate.data.manual &&
                  rate.data.marketRate != null &&
                  ` · mercado: R$ ${formatRate(rate.data.marketRate)}`}
              </p>
            </div>
            <div className="flex items-center gap-2">
              <RateBadge rate={rate.data} />
              {rate.data.manual && (
                <Button
                  variant="outline"
                  size="sm"
                  loading={clearOverride.isPending}
                  onClick={() =>
                    clearOverride.mutate(
                      { base: 'EUR', quote: 'BRL' },
                      {
                        onSuccess: () => toast.success('Voltamos a usar a cotação de mercado.'),
                        onError: (err) => toast.error(errorMessage(err)),
                      },
                    )
                  }
                >
                  Usar mercado
                </Button>
              )}
            </div>
          </div>
        )}
        <form onSubmit={submit} noValidate className="flex flex-col gap-3 sm:flex-row sm:items-end">
          <Field id="manual-rate" label="Cotação manual (R$ por €)" error={error} className="flex-1">
            <Input
              id="manual-rate"
              inputMode="decimal"
              placeholder="Ex.: 6,20"
              value={value}
              aria-invalid={!!error}
              onChange={(e) => setValue(e.target.value)}
            />
          </Field>
          <Button type="submit" loading={setOverride.isPending}>
            Definir
          </Button>
        </form>
      </CardContent>
    </Card>
  )
}
