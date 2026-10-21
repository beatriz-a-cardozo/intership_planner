import { describe, expect, it } from 'vitest'
import { expenseSchema, toExpenseRequest, toPreviewRequest, type ExpenseForm } from './expense-form'

const base: ExpenseForm = {
  name: 'Aluguel',
  categoryId: 'cat-1',
  cycleId: '',
  currency: 'EUR',
  unitPrice: '650',
  quantity: '',
  recurrence: 'MONTHLY',
  occurrences: '',
  required: true,
  refundable: false,
  dueDate: '',
  endDate: '',
  notes: '',
}

describe('formulário de gasto', () => {
  it('ocorrências vazias deixam o backend sugerir pelo horizonte', () => {
    const req = toExpenseRequest(base, 'obj-1')
    expect(req.occurrences).toBeUndefined()
    expect(req.quantity).toBeUndefined()
    expect(req.cycleId).toBeUndefined()
    expect(req.unitPrice).toBe(650)
  })

  it('único e por unidade nunca enviam ocorrências nem data final', () => {
    const req = toExpenseRequest(
      {
        ...base,
        recurrence: 'PER_UNIT',
        unitPrice: '180',
        quantity: '5',
        occurrences: '3',
        endDate: '2027-01-01',
      },
      'obj-1',
    )
    expect(req).toMatchObject({
      recurrence: 'PER_UNIT',
      quantity: 5,
      occurrences: undefined,
      endDate: undefined,
    })
  })

  it('personalizado exige ocorrências', () => {
    const result = expenseSchema.safeParse({ ...base, recurrence: 'CUSTOM' })
    expect(result.success).toBe(false)
    expect(result.error?.issues[0]?.path).toEqual(['occurrences'])
  })

  it('preview só é pedido com dados de cálculo válidos', () => {
    expect(toPreviewRequest({ ...base, unitPrice: '' }, 'o')).toBeNull()
    expect(toPreviewRequest({ ...base, unitPrice: '1.250,00' }, 'o')).toEqual({
      objectiveId: 'o',
      currency: 'EUR',
      unitPrice: 1250,
      quantity: undefined,
      recurrence: 'MONTHLY',
      occurrences: undefined,
    })
    expect(toPreviewRequest({ ...base, recurrence: 'CUSTOM' }, 'o')).toBeNull()
    expect(toPreviewRequest({ ...base, quantity: '0' }, 'o')).toBeNull()
  })

  it('data final não pode ser antes da inicial', () => {
    const result = expenseSchema.safeParse({ ...base, dueDate: '2027-05-01', endDate: '2027-04-01' })
    expect(result.success).toBe(false)
  })
})
