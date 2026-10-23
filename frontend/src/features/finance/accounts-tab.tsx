import { ChevronDown, Landmark, Pencil, Plus, RefreshCw, Trash2, Wallet } from 'lucide-react'
import { useState } from 'react'
import { toast } from 'sonner'
import { errorMessage } from '@/api/errors'
import type { Account } from '@/api/types'
import { ConfirmDialog } from '@/components/confirm-dialog'
import { EmptyState } from '@/components/empty-state'
import { ErrorState } from '@/components/error-state'
import { RateBadge } from '@/components/rate-badge'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Skeleton } from '@/components/ui/skeleton'
import { useObjectives } from '@/features/objectives/api'
import { formatDate, formatMoney, pluralize } from '@/lib/format'
import { ACCOUNT_TYPE_LABELS } from '@/lib/labels'
import { cn } from '@/lib/utils'
import { AccountDialog, BalanceDialog } from './account-dialogs'
import { useAccounts, useAccountsSummary, useBalanceHistory, useDeleteAccount } from './api'

/** Aba "Contas": onde está o dinheiro, em cada moeda (Fase 2). */
export function AccountsTab() {
  const accounts = useAccounts()
  const summary = useAccountsSummary()
  const objectives = useObjectives()
  const remove = useDeleteAccount()
  const [editing, setEditing] = useState<Account | 'new' | null>(null)
  const [balanceFor, setBalanceFor] = useState<Account | null>(null)
  const [deleting, setDeleting] = useState<Account | null>(null)
  const [expanded, setExpanded] = useState<string | null>(null)

  const objectiveName = (id: string | null) => objectives.data?.find((o) => o.id === id)?.name

  return (
    <>
      <div className="grid gap-5">
        {summary.data && summary.data.accountCount > 0 && (
          <Card>
            <CardContent className="grid gap-5 sm:grid-cols-[1fr_auto] sm:items-end">
              <div>
                <p className="text-xs uppercase tracking-[0.1em] text-muted">Total convertido</p>
                <p className="mt-1 font-display text-4xl font-semibold tabular tracking-tight">
                  ≈ {formatMoney(summary.data.total)}
                </p>
                <div className="mt-3 flex flex-wrap gap-x-5 gap-y-1 text-sm">
                  {summary.data.byCurrency.map((m) => (
                    <span key={m.currency}>
                      <span className="text-muted">{m.currency} </span>
                      <span className="font-medium tabular">{formatMoney(m)}</span>
                    </span>
                  ))}
                </div>
              </div>
              {summary.data.exchangeRates[0] && <RateBadge rate={summary.data.exchangeRates[0]} />}
            </CardContent>
          </Card>
        )}

        <Card>
          <CardHeader>
            <div>
              <CardTitle>Contas</CardTitle>
              <CardDescription>
                Atualize o saldo sempre que quiser; o histórico fica guardado.
              </CardDescription>
            </div>
            <Button size="sm" onClick={() => setEditing('new')}>
              <Plus aria-hidden /> Nova conta
            </Button>
          </CardHeader>
          <CardContent>
            {accounts.isPending ? (
              <div className="grid gap-3">
                <Skeleton className="h-16" />
                <Skeleton className="h-16" />
              </div>
            ) : accounts.isError ? (
              <ErrorState error={accounts.error} onRetry={() => void accounts.refetch()} />
            ) : accounts.data.length === 0 ? (
              <EmptyState
                icon={Wallet}
                title="Nenhuma conta"
                description="Cadastre conta corrente, poupança, Wise, Sperrkonto... em BRL ou EUR."
                action={<Button onClick={() => setEditing('new')}>Adicionar conta</Button>}
              />
            ) : (
              <ul className="divide-y divide-border">
                {accounts.data.map((a) => (
                  <li key={a.id} className={cn('py-3', a.archived && 'opacity-60')}>
                    <div className="flex flex-wrap items-center gap-3">
                      <span
                        className="grid size-10 shrink-0 place-items-center rounded-xl bg-surface-2 text-muted"
                        aria-hidden
                      >
                        <Landmark className="size-[18px]" />
                      </span>
                      <div className="min-w-0 flex-1">
                        <p className="flex flex-wrap items-center gap-2 font-medium">
                          {a.name}
                          {a.archived && <Badge>Arquivada</Badge>}
                        </p>
                        <p className="truncate text-xs text-muted">
                          {ACCOUNT_TYPE_LABELS[a.type]}
                          {a.objectiveId && ` · ${objectiveName(a.objectiveId) ?? 'objetivo'}`}
                        </p>
                      </div>
                      <div className="text-right">
                        <p className="font-semibold tabular">{formatMoney(a.balance)}</p>
                        <p className="text-xs text-muted">
                          {a.balanceAsOf ? `em ${formatDate(a.balanceAsOf)}` : 'sem saldo'}
                        </p>
                      </div>
                      <div className="flex w-full justify-end gap-1 sm:w-auto">
                        <Button variant="outline" size="sm" onClick={() => setBalanceFor(a)}>
                          <RefreshCw aria-hidden /> Saldo
                        </Button>
                        <Button
                          variant="ghost"
                          size="icon"
                          aria-label={`Histórico de ${a.name}`}
                          aria-expanded={expanded === a.id}
                          onClick={() => setExpanded(expanded === a.id ? null : a.id)}
                        >
                          <ChevronDown
                            className={cn('transition-transform', expanded === a.id && 'rotate-180')}
                          />
                        </Button>
                        <Button
                          variant="ghost"
                          size="icon"
                          aria-label={`Editar ${a.name}`}
                          onClick={() => setEditing(a)}
                        >
                          <Pencil />
                        </Button>
                        <Button
                          variant="ghost"
                          size="icon"
                          aria-label={`Excluir ${a.name}`}
                          onClick={() => setDeleting(a)}
                        >
                          <Trash2 />
                        </Button>
                      </div>
                    </div>
                    {expanded === a.id && <BalanceHistory accountId={a.id} />}
                  </li>
                ))}
              </ul>
            )}
          </CardContent>
        </Card>
      </div>

      {editing !== null && (
        <AccountDialog
          open
          onOpenChange={(o) => !o && setEditing(null)}
          account={editing === 'new' ? undefined : editing}
        />
      )}
      {balanceFor && <BalanceDialog account={balanceFor} onOpenChange={(o) => !o && setBalanceFor(null)} />}
      <ConfirmDialog
        open={deleting !== null}
        onOpenChange={(o) => !o && setDeleting(null)}
        title="Excluir conta?"
        description={`"${deleting?.name}" e todo o histórico de saldos serão excluídos. Para só esconder, arquive a conta.`}
        confirmLabel="Excluir"
        destructive
        loading={remove.isPending}
        onConfirm={() =>
          deleting &&
          remove.mutate(deleting.id, {
            onSuccess: () => {
              toast.success('Conta excluída.')
              setDeleting(null)
            },
            onError: (e) => toast.error(errorMessage(e)),
          })
        }
      />
    </>
  )
}

function BalanceHistory({ accountId }: { accountId: string }) {
  const history = useBalanceHistory(accountId)
  if (history.isPending) return <Skeleton className="mt-3 h-16" />
  if (history.isError) return <p className="mt-3 text-sm text-danger">{errorMessage(history.error)}</p>
  if (!history.data.length)
    return <p className="mt-3 pl-13 text-sm text-muted">Nenhum saldo lançado ainda.</p>
  return (
    <div className="mt-3 rounded-xl bg-surface-2 p-3 sm:ml-13">
      <p className="mb-2 text-xs text-muted">{pluralize(history.data.length, 'registro', 'registros')}</p>
      <table className="w-full text-sm">
        <caption className="sr-only">Histórico de saldos</caption>
        <thead>
          <tr className="text-left text-xs text-muted">
            <th className="pb-1 font-normal">Data</th>
            <th className="pb-1 text-right font-normal">Saldo</th>
          </tr>
        </thead>
        <tbody>
          {history.data.map((s) => (
            <tr key={s.id}>
              <td className="py-1">{formatDate(s.asOf)}</td>
              <td className="py-1 text-right tabular">{formatMoney(s.balance)}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
