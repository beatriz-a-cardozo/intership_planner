import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError } from '@/api/errors'
import { RegisterPage } from './register-page'

const registerMock = vi.fn()
vi.mock('@/app/providers/auth-provider', () => ({
  useAuth: () => ({ register: registerMock }),
}))

function setup() {
  render(
    <MemoryRouter>
      <RegisterPage />
    </MemoryRouter>,
  )
  const user = userEvent.setup()
  return { user }
}

async function fill(user: ReturnType<typeof userEvent.setup>, overrides: Partial<Record<string, string>> = {}) {
  const values = { name: 'Beatriz', email: 'bia@example.com', password: 'senhaForte1', confirm: 'senhaForte1', ...overrides }
  await user.type(screen.getByLabelText('Nome'), values.name)
  await user.type(screen.getByLabelText('E-mail'), values.email)
  await user.type(screen.getByLabelText('Senha'), values.password)
  await user.type(screen.getByLabelText('Confirmar senha'), values.confirm)
}

describe('RegisterPage', () => {
  beforeEach(() => registerMock.mockReset())

  it('valida os campos antes de enviar', async () => {
    const { user } = setup()
    await user.click(screen.getByRole('button', { name: 'Criar conta' }))
    expect(await screen.findByText('Informe seu nome.')).toBeInTheDocument()
    expect(screen.getByText('Informe um e-mail válido.')).toBeInTheDocument()
    expect(registerMock).not.toHaveBeenCalled()
  })

  it('exige senhas iguais', async () => {
    const { user } = setup()
    await fill(user, { confirm: 'outraSenha1' })
    await user.click(screen.getByRole('button', { name: 'Criar conta' }))
    expect(await screen.findByText('As senhas não conferem.')).toBeInTheDocument()
    expect(registerMock).not.toHaveBeenCalled()
  })

  it('envia os dados sem a confirmação de senha', async () => {
    registerMock.mockResolvedValueOnce(undefined)
    const { user } = setup()
    await fill(user)
    await user.click(screen.getByRole('button', { name: 'Criar conta' }))
    expect(registerMock).toHaveBeenCalledWith({ displayName: 'Beatriz', email: 'bia@example.com', password: 'senhaForte1' })
  })

  it('mostra erros de validação vindos do servidor no campo correto', async () => {
    registerMock.mockRejectedValueOnce(
      new ApiError({ status: 409, title: 'Conflito', errors: [{ field: 'email', message: 'E-mail já cadastrado.' }] }),
    )
    const { user } = setup()
    await fill(user)
    await user.click(screen.getByRole('button', { name: 'Criar conta' }))
    expect(await screen.findByText('E-mail já cadastrado.')).toBeInTheDocument()
  })
})
