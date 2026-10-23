import {
  CalendarDays,
  CircleCheck,
  CreditCard,
  DoorOpen,
  FileText,
  Flag,
  ListChecks,
  Mail,
  PenLine,
  Plane,
  Users,
  Wallet,
  type LucideIcon,
} from 'lucide-react'
import type { TimelineItem } from '@/api/types'

/** Ícone pelo tipo do item (e pelo tipo do evento manual). */
export function itemIcon(item: TimelineItem): LucideIcon {
  if (item.source === 'EVENT') {
    switch (item.eventType) {
      case 'TRAVEL':
        return Plane
      case 'EXAM':
        return PenLine
      case 'DOCUMENT':
        return FileText
      case 'APPOINTMENT':
        return Users
      case 'PAYMENT':
        return Wallet
      case 'MILESTONE':
        return Flag
      default:
        return CalendarDays
    }
  }
  switch (item.kind) {
    case 'DEADLINE':
      return CalendarDays
    case 'PAYMENT_DUE':
      return CreditCard
    case 'WINDOW_OPENS':
      return DoorOpen
    case 'RESPONSE':
      return Mail
    case 'TASK':
      return ListChecks
    case 'MILESTONE':
      return Flag
    default:
      return CircleCheck
  }
}

/** Para onde o item leva (eventos manuais abrem a edição em vez de navegar). */
export function itemLink(item: TimelineItem): string | null {
  switch (item.source) {
    case 'EXPENSE':
      return '/expenses'
    case 'APPLICATION':
    case 'REQUEST':
      return item.parentId ? `/programs/${item.parentId}?tab=application` : '/applications'
    case 'TASK':
      return '/tasks'
    default:
      return null
  }
}
