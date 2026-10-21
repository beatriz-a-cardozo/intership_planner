import { AlertTriangle, Hand, RefreshCw } from 'lucide-react'
import type { ExchangeRate } from '@/api/types'
import { Badge } from '@/components/ui/badge'
import { formatDate, formatDateTime } from '@/lib/format'

/** Selo de frescor da cotação: manual, desatualizada (com ícone) ou data da atualização. */
export function RateBadge({ rate }: { rate: ExchangeRate }) {
  if (rate.manual) {
    return (
      <Badge tone="accent">
        <Hand className="size-3" aria-hidden /> Cotação manual
      </Badge>
    )
  }
  if (rate.source === 'FALLBACK') {
    return (
      <Badge tone="danger">
        <AlertTriangle className="size-3" aria-hidden /> Cotação de reserva
      </Badge>
    )
  }
  if (rate.stale) {
    return (
      <Badge tone="danger">
        <AlertTriangle className="size-3" aria-hidden /> Desatualizada · {formatDate(rate.effectiveDate)}
      </Badge>
    )
  }
  return (
    <Badge>
      <RefreshCw className="size-3" aria-hidden />{' '}
      {rate.fetchedAt ? formatDateTime(rate.fetchedAt) : formatDate(rate.effectiveDate)}
    </Badge>
  )
}
