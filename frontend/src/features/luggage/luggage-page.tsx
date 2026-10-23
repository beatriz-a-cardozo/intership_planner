import { Check, Luggage, Package, Pencil, Plus, Scale, Trash2, TriangleAlert } from 'lucide-react'
import { useState } from 'react'
import { toast } from 'sonner'
import { errorMessage } from '@/api/errors'
import type { BagRequest, LuggageBag, LuggageItem } from '@/api/types'
import { ConfirmDialog } from '@/components/confirm-dialog'
import { EmptyState } from '@/components/empty-state'
import { ErrorState } from '@/components/error-state'
import { PageHeader } from '@/components/page-header'
import { StatCard } from '@/components/stat-card'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Skeleton } from '@/components/ui/skeleton'
import { formatPercent, formatWeight, pluralize } from '@/lib/format'
import { BAG_TYPE_LABELS, ITEM_CATEGORY_LABELS, WEIGHT_STATE_LABELS } from '@/lib/labels'
import { cn } from '@/lib/utils'
import {
  useDeleteBag,
  useDeleteItem,
  useLuggageItems,
  useLuggageSummary,
  usePackItem,
  useSaveBag,
} from './api'
import { BagDialog } from './bag-dialog'
import { ItemDialog } from './item-dialog'

const PRESETS: (BagRequest & { label: string })[] = [
  { label: 'Mala de mão · 10 kg', name: 'Mala de mão', type: 'CARRY_ON', maxWeightG: 10_000 },
  { label: 'Despachada · 23 kg', name: 'Mala despachada', type: 'CHECKED', maxWeightG: 23_000 },
  { label: 'Item pessoal (mochila)', name: 'Mochila', type: 'PERSONAL_ITEM' },
]

export function LuggagePage() {
  const summary = useLuggageSummary()
  const items = useLuggageItems()
  const saveBag = useSaveBag()
  const [bagDialog, setBagDialog] = useState<LuggageBag | 'new' | null>(null)
  const [itemDialog, setItemDialog] = useState<{ item?: LuggageItem; bagId?: string | null } | null>(null)
  const [deletingBag, setDeletingBag] = useState<LuggageBag | null>(null)
  const removeBag = useDeleteBag()

  const byBag = new Map<string | null, LuggageItem[]>()
  for (const i of items.data ?? []) byBag.set(i.bagId, [...(byBag.get(i.bagId) ?? []), i])
  const s = summary.data

  return (
    <>
      <PageHeader
        title="Mala"
        description="Malas com limite de peso, o que vai em cada uma e o checklist do que já está embalado."
        actions={
          <>
            <Button variant="outline" onClick={() => setBagDialog('new')}>
              <Luggage aria-hidden /> Mala
            </Button>
            <Button onClick={() => setItemDialog({})}>
              <Plus aria-hidden /> Item
            </Button>
          </>
        }
      />

      {summary.isPending || items.isPending ? (
        <Skeleton className="h-80 rounded-card" />
      ) : summary.isError || items.isError ? (
        <Card>
          <ErrorState
            error={summary.error ?? items.error}
            onRetry={() => {
              void summary.refetch()
              void items.refetch()
            }}
          />
        </Card>
      ) : s && s.bags.length === 0 && s.totalItems === 0 ? (
        <Card>
          <EmptyState
            icon={Luggage}
            title="Comece pelas malas"
            description="Crie as malas com o limite da companhia aérea; depois distribua os itens e acompanhe o peso."
            action={
              <div className="flex flex-wrap justify-center gap-2">
                {PRESETS.map(({ label, ...body }) => (
                  <Button
                    key={label}
                    variant="outline"
                    disabled={saveBag.isPending}
                    onClick={() => saveBag.mutate({ body }, { onError: (e) => toast.error(errorMessage(e)) })}
                  >
                    <Plus aria-hidden /> {label}
                  </Button>
                ))}
              </div>
            }
          />
        </Card>
      ) : (
        s && (
          <div className="grid gap-5">
            <div className="grid gap-4 sm:grid-cols-3">
              <StatCard
                label="Embalados"
                value={`${s.packedItems} de ${s.totalItems}`}
                icon={Check}
                tone="primary"
                hint={s.totalItems > 0 ? formatPercent((s.packedItems / s.totalItems) * 100) : undefined}
              />
              <StatCard
                label="Obrigatórios faltando"
                value={String(s.requiredNotPacked)}
                icon={TriangleAlert}
                hint={s.requiredNotPacked === 0 ? 'tudo certo' : 'ainda fora da mala'}
              />
              <StatCard
                label="Peso total"
                value={formatWeight(s.totalWeightG)}
                icon={Scale}
                hint="malas + não alocados"
              />
            </div>

            <div className="grid gap-5 lg:grid-cols-2">
              {s.bags.map((bag) => (
                <BagCard
                  key={bag.id}
                  bag={bag}
                  items={byBag.get(bag.id) ?? []}
                  onEdit={() => setBagDialog(bag)}
                  onDelete={() => setDeletingBag(bag)}
                  onAddItem={() => setItemDialog({ bagId: bag.id })}
                  onEditItem={(item) => setItemDialog({ item })}
                />
              ))}
              {(byBag.get(null)?.length ?? 0) > 0 && (
                <Card>
                  <CardHeader className="flex-col">
                    <CardTitle>Ainda não alocados</CardTitle>
                    <CardDescription>
                      {pluralize(s.unallocatedCount, 'item', 'itens')} · {formatWeight(s.unallocatedWeightG)}
                    </CardDescription>
                  </CardHeader>
                  <CardContent>
                    <ItemList items={byBag.get(null) ?? []} onEdit={(item) => setItemDialog({ item })} />
                  </CardContent>
                </Card>
              )}
            </div>
          </div>
        )
      )}

      {bagDialog !== null && (
        <BagDialog bag={bagDialog === 'new' ? undefined : bagDialog} onClose={() => setBagDialog(null)} />
      )}
      {itemDialog !== null && (
        <ItemDialog
          item={itemDialog.item}
          bagId={itemDialog.bagId}
          bags={s?.bags ?? []}
          onClose={() => setItemDialog(null)}
        />
      )}
      <ConfirmDialog
        open={deletingBag !== null}
        onOpenChange={(o) => !o && setDeletingBag(null)}
        title="Excluir mala?"
        description="Os itens dela voltam para “ainda não alocados”."
        confirmLabel="Excluir"
        destructive
        loading={removeBag.isPending}
        onConfirm={() =>
          deletingBag &&
          removeBag.mutate(deletingBag.id, {
            onSuccess: () => setDeletingBag(null),
            onError: (e) => toast.error(errorMessage(e)),
          })
        }
      />
    </>
  )
}

const STATE_FILL: Record<LuggageBag['state'], string> = {
  NO_LIMIT: 'bg-primary',
  OK: 'bg-primary',
  WARNING: 'bg-accent',
  OVER: 'bg-danger',
}

function BagCard({
  bag,
  items,
  onEdit,
  onDelete,
  onAddItem,
  onEditItem,
}: {
  bag: LuggageBag
  items: LuggageItem[]
  onEdit: () => void
  onDelete: () => void
  onAddItem: () => void
  onEditItem: (item: LuggageItem) => void
}) {
  const percent = bag.percent == null ? null : Math.min(100, bag.percent)
  return (
    <Card>
      <CardHeader>
        <div className="min-w-0">
          <CardTitle className="flex flex-wrap items-center gap-2">
            {bag.name}
            <Badge>{BAG_TYPE_LABELS[bag.type]}</Badge>
          </CardTitle>
          <CardDescription>
            {pluralize(bag.itemCount, 'item', 'itens')} · {bag.packedCount} embalado
            {bag.packedCount === 1 ? '' : 's'}
          </CardDescription>
        </div>
        <div className="flex shrink-0">
          <Button variant="ghost" size="icon" aria-label={`Editar ${bag.name}`} onClick={onEdit}>
            <Pencil />
          </Button>
          <Button variant="ghost" size="icon" aria-label={`Excluir ${bag.name}`} onClick={onDelete}>
            <Trash2 />
          </Button>
        </div>
      </CardHeader>
      <CardContent className="grid gap-4">
        <div>
          <div className="flex items-baseline justify-between gap-3 text-sm">
            <span>
              <span className="font-display text-2xl font-semibold tabular">{formatWeight(bag.weightG)}</span>
              {bag.maxWeightG != null && (
                <span className="text-muted"> de {formatWeight(bag.maxWeightG)}</span>
              )}
            </span>
            <span
              className={cn(
                'text-xs font-medium',
                bag.state === 'OVER' ? 'text-danger' : bag.state === 'WARNING' ? 'text-accent' : 'text-muted',
              )}
            >
              {WEIGHT_STATE_LABELS[bag.state]}
              {bag.percent != null && ` · ${formatPercent(bag.percent, 1)}`}
            </span>
          </div>
          {percent != null && (
            <div
              role="meter"
              aria-label={`Peso de ${bag.name}`}
              aria-valuemin={0}
              aria-valuemax={100}
              aria-valuenow={percent}
              aria-valuetext={`${formatWeight(bag.weightG)} de ${formatWeight(bag.maxWeightG)}`}
              className="relative mt-2 h-2.5 overflow-hidden rounded-full bg-surface-2"
            >
              <div
                className={cn('h-full rounded-full', STATE_FILL[bag.state])}
                style={{ width: `${percent}%` }}
              />
              {/* marca de 90%: a partir daqui vem o aviso */}
              <span className="absolute inset-y-0 left-[90%] w-px bg-foreground/30" aria-hidden />
            </div>
          )}
          <p className="mt-1.5 text-xs text-muted">
            {bag.state === 'OVER'
              ? `Passou ${formatWeight(bag.excessG)} do limite.`
              : bag.remainingG != null
                ? `Cabem mais ${formatWeight(bag.remainingG)}.`
                : 'Sem limite de peso definido.'}
            {bag.itemsWithoutWeight > 0 &&
              ` ${pluralize(bag.itemsWithoutWeight, 'item', 'itens')} sem peso (o total pode ser maior).`}
          </p>
        </div>
        {items.length > 0 ? (
          <ItemList items={items} onEdit={onEditItem} />
        ) : (
          <p className="text-sm text-muted">Nenhum item nesta mala.</p>
        )}
        <Button variant="outline" size="sm" className="justify-self-start" onClick={onAddItem}>
          <Plus aria-hidden /> Item nesta mala
        </Button>
      </CardContent>
    </Card>
  )
}

function ItemList({ items, onEdit }: { items: LuggageItem[]; onEdit: (item: LuggageItem) => void }) {
  const pack = usePackItem()
  const remove = useDeleteItem()
  return (
    <ul className="divide-y divide-border">
      {items.map((i) => (
        <li key={i.id} className="flex items-center gap-3 py-2">
          <button
            type="button"
            role="checkbox"
            aria-checked={i.packed}
            aria-label={i.packed ? `Desmarcar ${i.name} como embalado` : `Marcar ${i.name} como embalado`}
            disabled={pack.isPending}
            onClick={() =>
              pack.mutate({ id: i.id, packed: !i.packed }, { onError: (e) => toast.error(errorMessage(e)) })
            }
            className={cn(
              'grid size-6 shrink-0 place-items-center rounded-md border-2 transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring/40',
              i.packed
                ? 'border-primary bg-primary text-primary-foreground'
                : 'border-border hover:border-primary',
            )}
          >
            {i.packed && <Check className="size-3.5" aria-hidden />}
          </button>
          <div className="min-w-0 flex-1">
            <p className={cn('truncate text-sm', i.packed && 'text-muted line-through')}>
              {i.quantity > 1 && <span className="tabular">{i.quantity}× </span>}
              {i.name}
              {i.required && <span className="ml-1.5 text-xs font-medium text-accent">obrigatório</span>}
            </p>
            <p className="text-xs text-muted">
              {ITEM_CATEGORY_LABELS[i.category]}
              {i.totalWeightG != null ? ` · ${formatWeight(i.totalWeightG)}` : ' · sem peso'}
            </p>
          </div>
          <Button variant="ghost" size="icon" aria-label={`Editar ${i.name}`} onClick={() => onEdit(i)}>
            <Pencil />
          </Button>
          <Button
            variant="ghost"
            size="icon"
            aria-label={`Excluir ${i.name}`}
            disabled={remove.isPending}
            onClick={() => remove.mutate(i.id, { onError: (e) => toast.error(errorMessage(e)) })}
          >
            <Trash2 />
          </Button>
        </li>
      ))}
      {items.length === 0 && (
        <li className="py-2 text-sm text-muted">
          <Package className="mr-1 inline size-4" aria-hidden /> Nada aqui.
        </li>
      )}
    </ul>
  )
}
