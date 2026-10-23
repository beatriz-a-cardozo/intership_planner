import { createElement } from 'react'
import type { TimelineItem } from '@/api/types'
import { itemIcon } from './timeline-item'

/** Ícone do item da timeline (o tipo do ícone vem de `itemIcon`). */
export function TimelineIcon({ item, className }: { item: TimelineItem; className?: string }) {
  return createElement(itemIcon(item), { className, 'aria-hidden': true })
}
