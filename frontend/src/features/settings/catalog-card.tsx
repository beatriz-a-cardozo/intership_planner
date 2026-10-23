import { zodResolver } from '@hookform/resolvers/zod'
import { Archive, Pencil, Plus, Target, Trash2 } from 'lucide-react'
import { useState } from 'react'
import { Controller, useForm } from 'react-hook-form'
import { toast } from 'sonner'
import { z } from 'zod'
import { errorMessage } from '@/api/errors'
import type { CatalogItem } from '@/api/types'
import { CatalogIcon } from '@/components/catalog-icon'
import { ConfirmDialog } from '@/components/confirm-dialog'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Field } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { ResponsiveDialog } from '@/components/ui/responsive-dialog'
import { SegmentedControl } from '@/components/ui/segmented'
import { Skeleton } from '@/components/ui/skeleton'
import { SwitchField } from '@/components/ui/switch'
import { applyServerErrors } from '@/features/auth/use-server-errors'
import {
  type CatalogKind,
  useCatalog,
  useDeleteCatalogItem,
  useSaveCatalogItem,
} from '@/features/expenses/api'
import { CATALOG_COLORS, CATALOG_ICONS } from '@/lib/catalog'
import { cn } from '@/lib/utils'

const LABELS: Record<CatalogKind, { singular: string; plural: string; novo: string }> = {
  categories: { singular: 'categoria', plural: 'Categorias', novo: 'Nova' },
  cycles: { singular: 'ciclo', plural: 'Ciclos', novo: 'Novo' },
}

/** Categorias e ciclos de gasto: tudo editável; o que está em uso pode ser arquivado. */
export function CatalogCard() {
  const [kind, setKind] = useState<CatalogKind>('categories')
  const items = useCatalog(kind)
  const remove = useDeleteCatalogItem(kind)
  const [editing, setEditing] = useState<CatalogItem | 'new' | null>(null)
  const [deleting, setDeleting] = useState<CatalogItem | null>(null)

  return (
    <Card>
      <CardHeader className="flex-col gap-3 sm:flex-row sm:items-center">
        <div className="flex-1">
          <CardTitle>Categorias e ciclos de gasto</CardTitle>
          <CardDescription>
            {kind === 'cycles'
              ? 'Os ciclos marcados “Na meta” formam o valor necessário do objetivo (padrão: Preparação e Imigração).'
              : 'Ajuste nomes, cores e ícones. Itens em uso podem ser arquivados.'}
          </CardDescription>
        </div>
        <SegmentedControl
          ariaLabel="Tipo de catálogo"
          value={kind}
          onValueChange={setKind}
          options={[
            { value: 'categories', label: 'Categorias' },
            { value: 'cycles', label: 'Ciclos' },
          ]}
        />
      </CardHeader>
      <CardContent>
        {items.isPending ? (
          <Skeleton className="h-40" />
        ) : items.isError ? (
          <p className="text-sm text-danger">{errorMessage(items.error)}</p>
        ) : (
          <>
            <ul className="grid gap-1 sm:grid-cols-2">
              {items.data.map((item) => (
                <li
                  key={item.id}
                  className={cn(
                    'flex items-center gap-3 rounded-xl px-2 py-1.5',
                    item.archived && 'opacity-60',
                  )}
                >
                  <CatalogIcon icon={item.icon} color={item.color} size="sm" />
                  <span className="min-w-0 flex-1 truncate text-sm">{item.name}</span>
                  {item.inGoal && (
                    <Badge tone="primary">
                      <Target className="size-3" aria-hidden /> Na meta
                    </Badge>
                  )}
                  {item.archived && (
                    <Badge>
                      <Archive className="size-3" aria-hidden /> Arquivado
                    </Badge>
                  )}
                  <Button
                    variant="ghost"
                    size="icon"
                    aria-label={`Editar ${item.name}`}
                    onClick={() => setEditing(item)}
                  >
                    <Pencil />
                  </Button>
                  <Button
                    variant="ghost"
                    size="icon"
                    aria-label={`Excluir ${item.name}`}
                    onClick={() => setDeleting(item)}
                  >
                    <Trash2 />
                  </Button>
                </li>
              ))}
            </ul>
            <Button variant="outline" className="mt-4" onClick={() => setEditing('new')}>
              <Plus aria-hidden /> {LABELS[kind].novo} {LABELS[kind].singular}
            </Button>
          </>
        )}
      </CardContent>

      {editing !== null && (
        <CatalogItemDialog
          kind={kind}
          item={editing === 'new' ? undefined : editing}
          nextOrder={(items.data?.length ?? 0) + 1}
          onOpenChange={(o) => !o && setEditing(null)}
        />
      )}
      <ConfirmDialog
        open={deleting !== null}
        onOpenChange={(o) => !o && setDeleting(null)}
        title={`Excluir ${LABELS[kind].singular}?`}
        description={`"${deleting?.name}" será excluído(a). Se estiver em uso por gastos, arquive em vez de excluir.`}
        confirmLabel="Excluir"
        destructive
        loading={remove.isPending}
        onConfirm={() =>
          deleting &&
          remove.mutate(deleting.id, {
            onSuccess: () => setDeleting(null),
            onError: (e) => {
              toast.error(errorMessage(e))
              setDeleting(null)
            },
          })
        }
      />
    </Card>
  )
}

const schema = z.object({
  name: z.string().trim().min(1, 'Informe o nome.').max(60),
  color: z.string().regex(/^#[0-9a-fA-F]{6}$/),
  icon: z.string().min(1),
  archived: z.boolean(),
  inGoal: z.boolean(),
})
type Form = z.infer<typeof schema>

function CatalogItemDialog({
  kind,
  item,
  nextOrder,
  onOpenChange,
}: {
  kind: CatalogKind
  item?: CatalogItem
  nextOrder: number
  onOpenChange: (open: boolean) => void
}) {
  const save = useSaveCatalogItem(kind)
  const [formError, setFormError] = useState('')
  const {
    register,
    control,
    handleSubmit,
    setError,
    formState: { errors },
  } = useForm<Form>({
    resolver: zodResolver(schema),
    defaultValues: {
      name: item?.name ?? '',
      color: item?.color ?? CATALOG_COLORS[0]!,
      icon: item?.icon ?? 'tag',
      archived: item?.archived ?? false,
      inGoal: item?.inGoal ?? false,
    },
  })

  const onSubmit = handleSubmit((v) =>
    save.mutate(
      {
        id: item?.id,
        body: {
          name: v.name.trim(),
          color: v.color,
          icon: v.icon,
          archived: v.archived,
          sortOrder: item?.sortOrder ?? nextOrder,
          inGoal: kind === 'cycles' ? v.inGoal : undefined,
        },
      },
      {
        onSuccess: () => onOpenChange(false),
        onError: (e) => setFormError(applyServerErrors(e, setError, ['name', 'color', 'icon'])),
      },
    ),
  )

  return (
    <ResponsiveDialog
      open
      onOpenChange={onOpenChange}
      title={item ? `Editar ${LABELS[kind].singular}` : `${LABELS[kind].novo} ${LABELS[kind].singular}`}
      footer={
        <>
          <Button variant="outline" onClick={() => onOpenChange(false)}>
            Cancelar
          </Button>
          <Button type="submit" form="catalog-form" loading={save.isPending}>
            Salvar
          </Button>
        </>
      }
    >
      <form id="catalog-form" onSubmit={onSubmit} noValidate className="grid gap-4">
        {formError && (
          <div role="alert" className="rounded-xl bg-danger-soft px-4 py-3 text-sm text-danger">
            {formError}
          </div>
        )}
        <Field id="catalog-name" label="Nome" error={errors.name?.message}>
          <Input id="catalog-name" aria-invalid={!!errors.name} {...register('name')} />
        </Field>
        <Controller
          control={control}
          name="color"
          render={({ field }) => (
            <fieldset>
              <legend className="mb-2 text-sm font-medium">Cor</legend>
              <div className="flex flex-wrap gap-2">
                {CATALOG_COLORS.map((c) => (
                  <button
                    key={c}
                    type="button"
                    aria-label={`Cor ${c}`}
                    aria-pressed={field.value === c}
                    onClick={() => field.onChange(c)}
                    className={cn(
                      'size-9 rounded-full ring-offset-2 ring-offset-surface transition',
                      field.value === c ? 'ring-2 ring-foreground' : 'hover:scale-110',
                    )}
                    style={{ backgroundColor: c }}
                  />
                ))}
              </div>
            </fieldset>
          )}
        />
        <Controller
          control={control}
          name="icon"
          render={({ field }) => (
            <fieldset>
              <legend className="mb-2 text-sm font-medium">Ícone</legend>
              <div className="flex flex-wrap gap-2">
                {Object.entries(CATALOG_ICONS).map(([name, Icon]) => (
                  <button
                    key={name}
                    type="button"
                    aria-label={`Ícone ${name}`}
                    aria-pressed={field.value === name}
                    onClick={() => field.onChange(name)}
                    className={cn(
                      'grid size-10 place-items-center rounded-xl border',
                      field.value === name
                        ? 'border-primary bg-primary-soft text-primary'
                        : 'border-border text-muted hover:text-foreground',
                    )}
                  >
                    <Icon className="size-[18px]" aria-hidden />
                  </button>
                ))}
              </div>
            </fieldset>
          )}
        />
        {kind === 'cycles' && (
          <Controller
            control={control}
            name="inGoal"
            render={({ field }) => (
              <SwitchField
                id="catalog-in-goal"
                label="Entra na meta"
                hint="Os gastos deste ciclo somam no valor necessário do objetivo."
                checked={field.value}
                onCheckedChange={field.onChange}
              />
            )}
          />
        )}
        {item && (
          <Controller
            control={control}
            name="archived"
            render={({ field }) => (
              <SwitchField
                id="catalog-archived"
                label="Arquivar"
                hint="Some das opções de novos gastos; os gastos existentes continuam."
                checked={field.value}
                onCheckedChange={field.onChange}
              />
            )}
          />
        )}
      </form>
    </ResponsiveDialog>
  )
}
