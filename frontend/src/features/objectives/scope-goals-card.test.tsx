import { render, screen, within } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import type { ObjectiveProgress, ScopeGoal } from '@/api/types'
import { ScopeGoalsCard } from './scope-goals-card'

const brl = (amount: number) => ({ amount, currency: 'BRL' })

function goal(
  scope: ScopeGoal['scope'],
  target: number,
  remaining: number,
  percent: number,
  configured: boolean,
) {
  return {
    scope,
    plannedExpenses: brl(target + 75000),
    target: brl(target),
    mobilized: brl(target - remaining),
    remaining: brl(remaining),
    ratio: percent / 100,
    percentComplete: percent,
    monthlySavingNeeded: brl(remaining / 10),
    configured,
  } as ScopeGoal
}

function progress(targetMode: 'DERIVED' | 'MANUAL'): ObjectiveProgress {
  return {
    targetMode,
    overdue: false,
    scopeGoals: [goal('REQUIRED_ONLY', 19500, 18600, 4.6, true), goal('ALL', 20500, 19600, 4.4, false)],
  } as unknown as ObjectiveProgress
}

describe('ScopeGoalsCard', () => {
  it('mostra a meta obrigatória e a completa, marcando a usada', () => {
    render(<ScopeGoalsCard progress={progress('DERIVED')} />)
    const required = screen.getByRole('region', { name: 'Meta obrigatória' })
    const all = screen.getByRole('region', { name: 'Meta completa' })
    expect(within(required).getByText('usada como meta')).toBeInTheDocument()
    expect(within(all).queryByText('usada como meta')).not.toBeInTheDocument()
    expect(within(required).getByText('R$ 19.500,00')).toBeInTheDocument()
    expect(within(all).getByText('R$ 20.500,00')).toBeInTheDocument()
    expect(within(all).getByRole('progressbar')).toHaveAttribute('aria-valuenow', '4.4')
  })

  it('com meta fixa, as duas são só referência', () => {
    render(<ScopeGoalsCard progress={progress('MANUAL')} />)
    expect(screen.getByText(/meta é um valor fixo/)).toBeInTheDocument()
    expect(screen.queryByText('usada como meta')).not.toBeInTheDocument()
  })
})
