import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { OnboardingPage } from './onboarding-page'

const postMock = vi.fn()
const setUserMock = vi.fn()

vi.mock('@/api/client', () => ({ api: { post: (...args: unknown[]) => postMock(...args) } }))
vi.mock('@/app/providers/auth-provider', () => ({
  useAuth: () => ({
    user: { displayName: 'Beatriz Cardozo', onboardingCompleted: false },
    setUser: setUserMock,
  }),
}))

function futureDate() {
  const d = new Date()
  d.setFullYear(d.getFullYear() + 1)
  return d.toISOString().slice(0, 10)
}

function setup() {
  render(
    <QueryClientProvider client={new QueryClient()}>
      <MemoryRouter>
        <OnboardingPage />
      </MemoryRouter>
    </QueryClientProvider>,
  )
  return userEvent.setup()
}

const next = () => screen.getByRole('button', { name: /continuar/i })

describe('OnboardingPage', () => {
  beforeEach(() => {
    postMock.mockReset()
    setUserMock.mockReset()
  })

  it('não avança sem o nome do objetivo', async () => {
    const user = setup()
    expect(screen.getByText('Olá, Beatriz! Vamos montar seu plano.')).toBeInTheDocument()
    await user.click(next())
    expect(await screen.findByText('Dê um nome ao seu objetivo.')).toBeInTheDocument()
    expect(screen.getByText('Passo 1 de 6')).toBeInTheDocument()
  })

  it('exige data alvo no futuro', async () => {
    const user = setup()
    await user.click(screen.getByRole('button', { name: 'Mestrado na Alemanha' }))
    await user.click(next())
    await user.click(next())
    await user.type(screen.getByLabelText('Data alvo'), '2020-01-01')
    await user.click(next())
    expect(await screen.findByText('A data alvo precisa estar no futuro.')).toBeInTheDocument()
  })

  it('percorre os 6 passos e envia os valores convertidos', async () => {
    postMock.mockResolvedValueOnce({ user: { onboardingCompleted: true }, objective: { id: 'o1' } })
    const user = setup()

    await user.type(screen.getByLabelText('Nome do objetivo'), 'Mestrado na Alemanha')
    await user.click(next())
    await user.type(screen.getByLabelText('Cidade (opcional)'), 'Munique')
    await user.click(next())
    await user.type(screen.getByLabelText('Data alvo'), futureDate())
    await user.click(next())
    await user.click(screen.getByRole('radio', { name: 'Euro (€)' }))
    await user.type(screen.getByLabelText('Valor já guardado (opcional)'), '855,58')
    await user.click(next())
    await user.type(screen.getByLabelText(/Economia mensal planejada/), '3.000')
    await user.click(next())
    await user.click(screen.getByRole('button', { name: /começar/i }))

    expect(postMock).toHaveBeenCalledWith('/api/onboarding', {
      objectiveName: 'Mestrado na Alemanha',
      countryCode: 'DE',
      city: 'Munique',
      targetDate: futureDate(),
      currency: 'BRL',
      targetAmount: undefined,
      currentSavings: 855.58,
      currentSavingsCurrency: 'EUR',
      plannedMonthlySaving: 3000,
    })
    expect(setUserMock).toHaveBeenCalledWith({ onboardingCompleted: true })
  })

  it('meta de valor fixo exige o valor', async () => {
    const user = setup()
    await user.type(screen.getByLabelText('Nome do objetivo'), 'X')
    await user.type(screen.getByLabelText('Nome do objetivo'), 'Y')
    await user.click(next())
    await user.click(next())
    await user.type(screen.getByLabelText('Data alvo'), futureDate())
    await user.click(next())
    await user.click(next())
    await user.click(next())
    await user.click(screen.getByRole('radio', { name: 'Definir um valor' }))
    await user.click(screen.getByRole('button', { name: /começar/i }))
    expect(await screen.findByText('Informe o valor da meta.')).toBeInTheDocument()
    expect(postMock).not.toHaveBeenCalled()
  })
})
