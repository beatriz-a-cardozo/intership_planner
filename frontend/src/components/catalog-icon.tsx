import { Tag } from 'lucide-react'
import { CATALOG_ICONS } from '@/lib/catalog'
import { cn } from '@/lib/utils'

interface CatalogIconProps {
  icon: string
  color: string
  size?: 'sm' | 'md'
  className?: string
}

/** Ícone sobre um fundo suave da cor da categoria (a cor identifica; o nome vem sempre ao lado). */
export function CatalogIcon({ icon, color, size = 'md', className }: CatalogIconProps) {
  const Icon = CATALOG_ICONS[icon] ?? Tag
  return (
    <span
      aria-hidden
      className={cn(
        'grid shrink-0 place-items-center rounded-xl',
        size === 'md' ? 'size-10' : 'size-8',
        className,
      )}
      style={{ backgroundColor: `color-mix(in oklab, ${color} 16%, transparent)`, color }}
    >
      <Icon className={size === 'md' ? 'size-[18px]' : 'size-4'} />
    </span>
  )
}
