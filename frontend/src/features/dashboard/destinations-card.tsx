import { GraduationCap, MapPin } from 'lucide-react'
import { lazy, Suspense, useCallback, useMemo, useState } from 'react'
import { Link } from 'react-router'
import { EmptyState } from '@/components/empty-state'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Skeleton } from '@/components/ui/skeleton'
import { useUniversityLocations } from '@/features/universities/api'
import { countryName } from '@/lib/labels'
import { cn } from '@/lib/utils'
import type { MapPoint } from './locations-map'

// Leaflet só é baixado quando o mapa aparece
const LocationsMap = lazy(() => import('./locations-map'))

/** Destinos escolhidos: mapa com as universidades dos programas marcados como "Escolhida" e a lista ao lado. */
export function DestinationsCard() {
  const locations = useUniversityLocations()
  const [selected, setSelected] = useState<string | null>(null)
  const list = useMemo(() => locations.data ?? [], [locations.data])
  const points = useMemo(
    () => list.filter((l): l is MapPoint => l.latitude != null && l.longitude != null),
    [list],
  )
  const onSelect = useCallback((id: string) => setSelected(id), [])

  if (locations.isPending) return <Skeleton className="h-96 rounded-card" />
  if (locations.isError) return null

  return (
    <Card>
      <CardHeader>
        <div>
          <CardTitle>Destinos escolhidos</CardTitle>
          <CardDescription>Universidades dos programas marcados como “Escolhida”.</CardDescription>
        </div>
        <Button asChild variant="ghost" size="sm">
          <Link to="/universities">Universidades</Link>
        </Button>
      </CardHeader>
      <CardContent>
        {list.length === 0 ? (
          <EmptyState
            icon={MapPin}
            title="Nenhum destino ainda"
            description="Marque programas como “Escolhida” para vê-los no mapa."
            className="py-6"
          />
        ) : (
          <div className="grid gap-4 @4xl:grid-cols-[1fr_17rem]">
            {points.length > 0 ? (
              <Suspense fallback={<Skeleton className="h-72 rounded-xl sm:h-80" />}>
                <LocationsMap points={points} selected={selected} onSelect={onSelect} />
              </Suspense>
            ) : (
              <p className="grid h-40 place-items-center rounded-xl bg-surface-2 px-4 text-center text-sm text-muted">
                Ainda localizando as cidades. Volte em instantes.
              </p>
            )}
            <ul className="grid content-start gap-1 @4xl:max-h-80 @4xl:overflow-y-auto">
              {list.map((l) => {
                const located = l.latitude != null
                return (
                  <li key={l.universityId}>
                    <div
                      className={cn(
                        'flex items-start gap-3 rounded-xl px-3 py-2.5 transition-colors',
                        selected === l.universityId ? 'bg-primary-soft' : 'hover:bg-surface-2',
                      )}
                    >
                      <button
                        type="button"
                        disabled={!located}
                        onClick={() => setSelected(l.universityId)}
                        aria-label={
                          located ? `Mostrar ${l.name} no mapa` : `${l.name}: cidade não localizada`
                        }
                        title={located ? 'Mostrar no mapa' : 'Cidade não localizada'}
                        className="mt-0.5 grid size-7 shrink-0 place-items-center rounded-lg bg-surface-2 text-muted hover:text-foreground disabled:opacity-50"
                      >
                        <MapPin className="size-4" aria-hidden />
                      </button>
                      <div className="min-w-0 flex-1">
                        <Link
                          to={`/universities/${l.universityId}`}
                          className="block truncate text-sm font-medium hover:underline"
                        >
                          {l.name}
                        </Link>
                        <p className="truncate text-xs text-muted">
                          {[l.city, countryName(l.countryCode)].filter(Boolean).join(', ') || 'Sem cidade'}
                        </p>
                        {l.programs.map((p) => (
                          <p
                            key={p.id}
                            className="mt-0.5 flex items-center gap-1 truncate text-xs text-muted"
                          >
                            <GraduationCap className="size-3 shrink-0" aria-hidden />
                            <Link to={`/programs/${p.id}`} className="truncate hover:underline">
                              {p.name}
                            </Link>
                          </p>
                        ))}
                      </div>
                    </div>
                  </li>
                )
              })}
            </ul>
          </div>
        )}
      </CardContent>
    </Card>
  )
}
