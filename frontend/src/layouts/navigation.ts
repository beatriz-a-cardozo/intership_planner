import {
  Bell,
  CalendarRange,
  GraduationCap,
  LayoutDashboard,
  ListChecks,
  Luggage,
  Receipt,
  Settings,
  Target,
  Wallet,
  type LucideIcon,
} from 'lucide-react'

export interface NavItem {
  to: string
  label: string
  shortLabel?: string
  icon: LucideIcon
  /** Fase do roadmap em que o módulo é entregue. */
  phase: number
  /** Aparece na barra inferior do mobile (máx. 5 incluindo "Mais"). */
  mobilePrimary?: boolean
  description: string
}

export const CURRENT_PHASE = 1

export const NAV_ITEMS: NavItem[] = [
  { to: '/', label: 'Dashboard', shortLabel: 'Início', icon: LayoutDashboard, phase: 1, mobilePrimary: true, description: 'Visão geral do seu objetivo.' },
  { to: '/objectives', label: 'Objetivos', icon: Target, phase: 2, description: 'Metas, prazos, progresso e projeção de cada objetivo.' },
  { to: '/expenses', label: 'Gastos', icon: Receipt, phase: 3, mobilePrimary: true, description: 'Planejamento de gastos por ciclo e categoria, pagamentos e moedas.' },
  { to: '/finance', label: 'Finanças', icon: Wallet, phase: 4, mobilePrimary: true, description: 'Receitas, despesas, parcelas, assinaturas e saldos em BRL e EUR.' },
  { to: '/universities', label: 'Universidades', shortLabel: 'Unis', icon: GraduationCap, phase: 5, mobilePrimary: true, description: 'Universidades, programas, candidaturas e comparador.' },
  { to: '/timeline', label: 'Timeline', icon: CalendarRange, phase: 6, description: 'Cronograma com prazos, pagamentos, respostas e eventos.' },
  { to: '/tasks', label: 'Tarefas', icon: ListChecks, phase: 6, description: 'Tarefas com prioridade, prazo e vínculos.' },
  { to: '/luggage', label: 'Mala', icon: Luggage, phase: 7, description: 'Itens, malas e controle de peso.' },
  { to: '/notifications', label: 'Notificações', icon: Bell, phase: 8, description: 'Prazos, pagamentos, câmbio e progresso.' },
  { to: '/settings', label: 'Configurações', icon: Settings, phase: 1, description: 'Perfil, tema e moedas.' },
]

export const isAvailable = (item: NavItem) => item.phase <= CURRENT_PHASE
