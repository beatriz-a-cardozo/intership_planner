import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { ResponseRange } from './application-panel'

const empty = {
  responseMinDate: null,
  responseLikelyDate: null,
  responseMaxDate: null,
  actualResponseDate: null,
}

describe('ResponseRange', () => {
  it('sem estimativa mostra aviso', () => {
    render(<ResponseRange application={empty} />)
    expect(screen.getByText('Sem estimativa de resposta.')).toBeInTheDocument()
  })

  it('mostra as três datas em texto', () => {
    render(
      <ResponseRange
        application={{
          ...empty,
          responseMinDate: '2027-05-01',
          responseLikelyDate: '2027-06-15',
          responseMaxDate: '2027-07-31',
        }}
      />,
    )
    expect(screen.getByText('01/05/2027')).toBeInTheDocument()
    expect(screen.getByText('15/06/2027')).toBeInTheDocument()
    expect(screen.getByText('31/07/2027')).toBeInTheDocument()
  })

  it('resposta recebida substitui a estimativa', () => {
    render(
      <ResponseRange
        application={{ ...empty, responseLikelyDate: '2027-06-15', actualResponseDate: '2027-06-02' }}
      />,
    )
    expect(screen.getByText('Resposta recebida em 02/06/2027')).toBeInTheDocument()
  })
})
