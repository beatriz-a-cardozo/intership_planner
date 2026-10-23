import { Building2 } from 'lucide-react'
import { cn } from '@/lib/utils'
import { useImageBlob } from './api'

/**
 * Imagem servida pela API (precisa do token, então não dá para usar a URL direto no `src`). O object URL é
 * criado e revogado pelo callback de ref (cleanup do React 19), sem estado nem efeito.
 */
export function UniversityImage({
  universityId,
  imageId,
  alt,
  className,
}: {
  universityId: string
  imageId: string | null | undefined
  alt: string
  className?: string
}) {
  const blob = useImageBlob(universityId, imageId)

  if (!imageId || blob.isError) {
    return (
      <div className={cn('grid place-items-center bg-surface-2 text-muted', className)} aria-hidden>
        <Building2 className="size-6" />
      </div>
    )
  }
  if (!blob.data) return <div className={cn('animate-pulse bg-surface-2', className)} aria-hidden />

  const data = blob.data
  return (
    <img
      alt={alt}
      className={cn('object-cover', className)}
      ref={(el) => {
        if (!el) return
        const url = URL.createObjectURL(data)
        el.src = url
        return () => URL.revokeObjectURL(url)
      }}
    />
  )
}
