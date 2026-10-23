import { ArrowLeft, ExternalLink, GraduationCap, Pencil, Plus, Trash2 } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router'
import { toast } from 'sonner'
import { errorMessage } from '@/api/errors'
import { ConfirmDialog } from '@/components/confirm-dialog'
import { EmptyState } from '@/components/empty-state'
import { ErrorState } from '@/components/error-state'
import { PageHeader } from '@/components/page-header'
import { Button } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'
import { Skeleton } from '@/components/ui/skeleton'
import { formatMoney } from '@/lib/format'
import { countryName, EMPLOYABILITY_LABELS, INSTITUTION_TYPE_LABELS, NATURE_LABELS } from '@/lib/labels'
import { useDeleteUniversity, useUniversity } from './api'
import { ImageGallery } from './image-gallery'
import { ProgramFormDialog } from './program-form-dialog'
import { ProgramCard } from './universities-page'
import { UniversityFormDialog } from './university-form-dialog'
import { UniversityImage } from './university-image'

export function UniversityDetailPage() {
  const { id = '' } = useParams()
  const detail = useUniversity(id)
  const remove = useDeleteUniversity()
  const navigate = useNavigate()
  const [dialog, setDialog] = useState<'edit' | 'program' | 'delete' | null>(null)

  if (detail.isPending) return <Skeleton className="h-64 rounded-card" />
  if (detail.isError)
    return (
      <Card>
        <ErrorState error={detail.error} onRetry={() => void detail.refetch()} />
      </Card>
    )

  const { university: u, programs } = detail.data
  const facts: [string, string | null][] = [
    ['Natureza', u.nature && NATURE_LABELS[u.nature]],
    ['Tipo', u.institutionType && INSTITUTION_TYPE_LABELS[u.institutionType]],
    ['Ranking no país', u.ranking != null ? `#${u.ranking}` : null],
    ['Custo de vida', u.costOfLiving && `${formatMoney(u.costOfLiving)} / mês`],
    ['Empregabilidade', u.employability && EMPLOYABILITY_LABELS[u.employability]],
    ['Habitantes', u.population != null ? u.population.toLocaleString('pt-BR') : null],
  ]

  return (
    <>
      <Link
        to="/universities?tab=universities"
        className="mb-4 inline-flex items-center gap-1.5 text-sm text-muted hover:text-foreground"
      >
        <ArrowLeft className="size-4" aria-hidden /> Universidades
      </Link>
      <PageHeader
        title={u.name}
        description={[u.city, u.region, countryName(u.countryCode)].filter(Boolean).join(', ') || undefined}
        actions={
          <>
            <Button
              variant="outline"
              size="icon"
              aria-label="Excluir universidade"
              onClick={() => setDialog('delete')}
            >
              <Trash2 />
            </Button>
            <Button variant="outline" onClick={() => setDialog('edit')}>
              <Pencil aria-hidden /> Editar
            </Button>
          </>
        }
      />

      {u.coverImageId && (
        <UniversityImage
          universityId={u.id}
          imageId={u.coverImageId}
          alt={`Capa de ${u.name}`}
          className="mb-6 h-44 w-full rounded-card sm:h-64"
        />
      )}

      <div className="grid gap-6 @4xl:grid-cols-[20rem_1fr]">
        <section aria-labelledby="about-title" className="grid content-start gap-6">
          {/* cabeçalho da mesma altura do de "Programas": os cards das duas colunas começam no mesmo nível */}
          <div className="-mb-3 flex min-h-9 items-center">
            <h2 id="about-title" className="font-semibold">
              Sobre
            </h2>
          </div>
          <Card>
            <CardContent>
              <dl className="grid gap-3 text-sm">
                {facts.map(([label, value]) => (
                  <div key={label} className="flex justify-between gap-3">
                    <dt className="text-muted">{label}</dt>
                    <dd className="text-right tabular">{value ?? '—'}</dd>
                  </div>
                ))}
              </dl>
              {u.website && (
                <a
                  href={u.website}
                  target="_blank"
                  rel="noreferrer"
                  className="mt-4 inline-flex items-center gap-1.5 text-sm text-primary hover:underline"
                >
                  Site oficial <ExternalLink className="size-3.5" aria-hidden />
                </a>
              )}
              {u.notes && (
                <p className="mt-4 whitespace-pre-line border-t border-border pt-4 text-sm text-muted">
                  {u.notes}
                </p>
              )}
            </CardContent>
          </Card>
          <ImageGallery universityId={u.id} universityName={u.name} />
        </section>

        <section aria-labelledby="programs-title">
          <div className="mb-3 flex min-h-9 items-center justify-between gap-3">
            <h2 id="programs-title" className="font-semibold">
              Programas
            </h2>
            <Button size="sm" onClick={() => setDialog('program')}>
              <Plus aria-hidden /> Programa
            </Button>
          </div>
          {programs.length === 0 ? (
            <Card>
              <EmptyState
                icon={GraduationCap}
                title="Nenhum programa"
                description="Adicione os programas desta universidade que você quer avaliar."
              />
            </Card>
          ) : (
            <ul className="grid gap-4 @5xl:grid-cols-2">
              {programs.map((p) => (
                <li key={p.id}>
                  <ProgramCard program={p} />
                </li>
              ))}
            </ul>
          )}
        </section>
      </div>

      {dialog === 'edit' && <UniversityFormDialog university={u} onClose={() => setDialog(null)} />}
      {dialog === 'program' && (
        <ProgramFormDialog
          universityId={u.id}
          onClose={() => setDialog(null)}
          onSaved={(p) => navigate(`/programs/${p.id}`)}
        />
      )}
      <ConfirmDialog
        open={dialog === 'delete'}
        onOpenChange={(o) => !o && setDialog(null)}
        title="Excluir universidade?"
        description={
          programs.length > 0
            ? `Os ${programs.length} programas e as candidaturas ligadas a eles também serão excluídos.`
            : 'Esta ação não pode ser desfeita.'
        }
        confirmLabel="Excluir"
        destructive
        loading={remove.isPending}
        onConfirm={() =>
          remove.mutate(u.id, {
            onSuccess: () => {
              toast.success('Universidade excluída.')
              navigate('/universities?tab=universities', { replace: true })
            },
            onError: (e) => toast.error(errorMessage(e)),
          })
        }
      />
    </>
  )
}
