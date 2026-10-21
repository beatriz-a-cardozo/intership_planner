import { render, screen } from '@testing-library/react'
import { MemoryRouter, Link } from 'react-router'
import { describe, expect, it } from 'vitest'
import { Button } from './button'

describe('Button', () => {
  // Regressão: com asChild o Slot exige um único filho (o spinner condicional quebrava a tela)
  it('asChild renderiza o link com o estilo do botão', () => {
    render(
      <MemoryRouter>
        <Button asChild>
          <Link to="/expenses">Ver gastos</Link>
        </Button>
      </MemoryRouter>,
    )
    const link = screen.getByRole('link', { name: 'Ver gastos' })
    expect(link).toHaveAttribute('href', '/expenses')
    expect(link.className).toContain('rounded-xl')
  })

  it('loading desabilita e marca como ocupado', () => {
    render(<Button loading>Salvar</Button>)
    const button = screen.getByRole('button', { name: 'Salvar' })
    expect(button).toBeDisabled()
    expect(button).toHaveAttribute('aria-busy', 'true')
  })
})
