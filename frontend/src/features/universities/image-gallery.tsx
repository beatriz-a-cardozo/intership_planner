import { ImagePlus, Star, Trash2 } from 'lucide-react'
import { useRef, useState } from 'react'
import { toast } from 'sonner'
import { errorMessage } from '@/api/errors'
import type { UniversityImage as ImageMeta } from '@/api/types'
import { ConfirmDialog } from '@/components/confirm-dialog'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { ResponsiveDialog } from '@/components/ui/responsive-dialog'
import { Skeleton } from '@/components/ui/skeleton'
import { prepareImage } from '@/lib/image'
import { useDeleteImage, useSetCover, useUniversityImages, useUploadImage } from './api'
import { UniversityImage } from './university-image'

const MAX_IMAGES = 12

/** Fotos da universidade: envio (várias de uma vez, reduzidas no navegador), capa, ampliar e excluir. */
export function ImageGallery({
  universityId,
  universityName,
}: {
  universityId: string
  universityName: string
}) {
  const images = useUniversityImages(universityId)
  const upload = useUploadImage(universityId)
  const setCover = useSetCover(universityId)
  const remove = useDeleteImage(universityId)
  const input = useRef<HTMLInputElement>(null)
  const [sending, setSending] = useState(0)
  const [viewing, setViewing] = useState<ImageMeta | null>(null)
  const [deleting, setDeleting] = useState<ImageMeta | null>(null)

  const count = images.data?.length ?? 0

  const onFiles = async (files: FileList | null) => {
    if (!files?.length) return
    const list = Array.from(files).slice(0, Math.max(0, MAX_IMAGES - count))
    if (list.length < files.length) toast.warning(`Limite de ${MAX_IMAGES} fotos por universidade.`)
    setSending(list.length)
    let ok = 0
    for (const file of list) {
      try {
        await upload.mutateAsync({ file: await prepareImage(file) })
        ok++
      } catch (e) {
        toast.error(`${file.name}: ${e instanceof Error ? e.message : errorMessage(e)}`)
      } finally {
        setSending((n) => n - 1)
      }
    }
    if (ok > 0) toast.success(ok === 1 ? 'Foto adicionada.' : `${ok} fotos adicionadas.`)
    if (input.current) input.current.value = ''
  }

  return (
    <Card>
      <CardHeader className="items-center">
        <CardTitle>Fotos</CardTitle>
        <Button
          variant="outline"
          size="sm"
          disabled={count >= MAX_IMAGES || sending > 0}
          loading={sending > 0}
          onClick={() => input.current?.click()}
        >
          <ImagePlus aria-hidden /> Adicionar
        </Button>
        <input
          ref={input}
          type="file"
          accept="image/jpeg,image/png,image/webp"
          multiple
          className="sr-only"
          tabIndex={-1}
          aria-label="Escolher fotos"
          onChange={(e) => void onFiles(e.target.files)}
        />
      </CardHeader>
      <CardContent>
        {images.isPending ? (
          <Skeleton className="h-28" />
        ) : count === 0 ? (
          <button
            type="button"
            onClick={() => input.current?.click()}
            className="grid w-full place-items-center gap-1 rounded-xl border border-dashed border-border px-4 py-8 text-sm text-muted transition-colors hover:border-primary/50 hover:text-foreground"
          >
            <ImagePlus className="size-6" aria-hidden />
            Adicione fotos do campus e da cidade. A primeira vira a capa.
          </button>
        ) : (
          <ul className="grid grid-cols-2 gap-3 sm:grid-cols-3">
            {images.data?.map((img) => (
              <li key={img.id} className="group relative overflow-hidden rounded-xl">
                <button
                  type="button"
                  className="block w-full focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring/40"
                  onClick={() => setViewing(img)}
                  aria-label={`Ampliar foto${img.caption ? `: ${img.caption}` : ''}`}
                >
                  <UniversityImage
                    universityId={universityId}
                    imageId={img.id}
                    alt={img.caption ?? `Foto de ${universityName}`}
                    className="aspect-[4/3] w-full"
                  />
                </button>
                {img.cover && (
                  <Badge tone="primary" className="pointer-events-none absolute top-2 left-2">
                    <Star className="size-3" aria-hidden /> Capa
                  </Badge>
                )}
                <div className="absolute right-1.5 bottom-1.5 flex gap-1 opacity-100 transition-opacity sm:opacity-0 sm:group-focus-within:opacity-100 sm:group-hover:opacity-100">
                  {!img.cover && (
                    <Button
                      size="icon"
                      variant="outline"
                      className="size-8 bg-surface/90"
                      aria-label="Usar como capa"
                      disabled={setCover.isPending}
                      onClick={() =>
                        setCover.mutate(img.id, {
                          onSuccess: () => toast.success('Capa atualizada.'),
                          onError: (e) => toast.error(errorMessage(e)),
                        })
                      }
                    >
                      <Star />
                    </Button>
                  )}
                  <Button
                    size="icon"
                    variant="outline"
                    className="size-8 bg-surface/90"
                    aria-label="Excluir foto"
                    onClick={() => setDeleting(img)}
                  >
                    <Trash2 />
                  </Button>
                </div>
              </li>
            ))}
          </ul>
        )}
      </CardContent>

      {viewing && (
        <ResponsiveDialog
          open
          onOpenChange={(o) => !o && setViewing(null)}
          title={viewing.caption ?? universityName}
          className="sm:w-[min(56rem,calc(100vw-2rem))]"
        >
          <UniversityImage
            universityId={universityId}
            imageId={viewing.id}
            alt={viewing.caption ?? `Foto de ${universityName}`}
            className="max-h-[70vh] w-full rounded-xl object-contain"
          />
        </ResponsiveDialog>
      )}
      <ConfirmDialog
        open={!!deleting}
        onOpenChange={(o) => !o && setDeleting(null)}
        title="Excluir foto?"
        description={
          deleting?.cover
            ? 'Ela é a capa; a foto mais antiga passa a ser a capa.'
            : 'Esta ação não pode ser desfeita.'
        }
        confirmLabel="Excluir"
        destructive
        loading={remove.isPending}
        onConfirm={() =>
          deleting &&
          remove.mutate(deleting.id, {
            onSuccess: () => {
              toast.success('Foto excluída.')
              setDeleting(null)
            },
            onError: (e) => toast.error(errorMessage(e)),
          })
        }
      />
    </Card>
  )
}
