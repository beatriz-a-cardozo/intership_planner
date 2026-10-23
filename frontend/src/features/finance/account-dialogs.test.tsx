import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import type { Account } from '@/api/types'
import { AccountDialog } from './account-dialogs'

let resolveObjectives: (value: unknown) => void = () => {}

vi.mock('@/api/client', () => ({
  api: {
    get: (url: string) =>
      url === '/api/objectives'
        ? new Promise((resolve) => {
            resolveObjectives = resolve
          })
        : Promise.resolve([]),
  },
}))

const account: Account = {
  id: 'acc-1',
  name: 'Sperrkonto',
  currency: 'EUR',
  type: 'BLOCKED',
  objectiveId: 'obj-2',
  archived: false,
  balance: { amount: 0, currency: 'EUR' },
  balanceAsOf: null,
}

describe('AccountDialog', () => {
  it('mantém o objetivo vinculado quando a lista de objetivos chega depois', async () => {
    render(
      <QueryClientProvider client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}>
        <AccountDialog open onOpenChange={() => {}} account={account} />
      </QueryClientProvider>,
    )

    expect(screen.queryByLabelText('Nome')).not.toBeInTheDocument()
    resolveObjectives([
      { id: 'obj-1', name: 'Mestrado', primary: true },
      { id: 'obj-2', name: 'Intercâmbio de idiomas', primary: false },
    ])

    const select = await screen.findByRole<HTMLSelectElement>('combobox', { name: /objetivo/i })
    expect(select.value).toBe('obj-2')
  })
})
