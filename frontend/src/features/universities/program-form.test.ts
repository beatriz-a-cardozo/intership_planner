import { describe, expect, it } from 'vitest'
import type { Program } from '@/api/types'
import { programSchema, toProgramForm, toProgramRequest } from './program-form'

const filled = () => ({
  ...toProgramForm(undefined, 'uni-1'),
  name: '  Informatik ',
  semesterFee: '1.234,50',
  nonEuTuition: '',
  languageRequirements: [
    { language: 'EN', minCefr: '', testName: 'TOEFL iBT', minScore: '88,5', rawText: '' },
  ],
  intakes: [
    {
      term: 'WINTER' as const,
      openDay: '',
      openMonth: '',
      closeDay: '15',
      closeMonth: '7',
      startMonth: '10',
    },
  ],
})

describe('formulário de programa', () => {
  it('novo programa começa com a universidade escolhida e uma janela de inverno', () => {
    const form = toProgramForm(undefined, 'uni-1')
    expect(form.universityId).toBe('uni-1')
    expect(form.intakes).toEqual([
      { term: 'WINTER', openDay: '', openMonth: '', closeDay: '', closeMonth: '', startMonth: '10' },
    ])
    expect(form.gre).toBe('UNKNOWN')
  })

  it('converte decimais pt-BR e manda null nos campos aninhados vazios', () => {
    const req = toProgramRequest(filled())
    expect(req.name).toBe('Informatik')
    expect(req.semesterFeeAmount).toBe(1234.5)
    expect(req.semesterFeeCurrency).toBe('EUR')
    expect(req.nonEuTuitionAmount).toBeUndefined()
    expect(req.nonEuTuitionPeriod).toBeUndefined()
    expect(req.languageRequirements).toEqual([
      { language: 'en', minCefr: null, testName: 'TOEFL iBT', minScore: 88.5, rawText: null },
    ])
    expect(req.intakes).toEqual([
      { term: 'WINTER', openDay: null, openMonth: null, closeDay: 15, closeMonth: 7, startMonth: 10 },
    ])
  })

  it('bolsa tri-estado: vazio = não sei', () => {
    expect(toProgramRequest({ ...filled(), scholarshipAvailable: '' }).scholarshipAvailable).toBeUndefined()
    expect(toProgramRequest({ ...filled(), scholarshipAvailable: 'false' }).scholarshipAvailable).toBe(false)
  })

  it('valida dia do prazo e código do idioma', () => {
    const bad = { ...filled(), intakes: [{ ...filled().intakes[0]!, closeDay: '32' }] }
    expect(programSchema.safeParse(bad).success).toBe(false)
    const badLang = {
      ...filled(),
      languageRequirements: [{ ...filled().languageRequirements[0]!, language: 'inglês' }],
    }
    expect(programSchema.safeParse(badLang).success).toBe(false)
    expect(programSchema.safeParse(filled()).success).toBe(true)
  })

  it('ida e volta preserva os dados de um programa existente', () => {
    const program = {
      id: 'p-1',
      university: { id: 'uni-1' },
      name: 'Data Science',
      degree: 'M.Sc.',
      gre: 'REQUIRED',
      aptitudeAssessment: 'NOT_REQUIRED',
      interview: 'CONDITIONAL',
      nc: 'UNKNOWN',
      semesterFee: { amount: 150.25, currency: 'EUR' },
      nonEuTuition: { amount: 1500, currency: 'EUR' },
      nonEuTuitionPeriod: 'SEMESTER',
      scholarshipAvailable: true,
      languageRequirements: [
        { language: 'en', minCefr: 'C1', testName: 'IELTS', minScore: 6.5, rawText: null },
      ],
      intakes: [
        {
          term: 'SUMMER',
          openDay: 1,
          openMonth: 12,
          closeDay: 15,
          closeMonth: 1,
          startMonth: 4,
          nextWindow: {},
        },
      ],
    } as unknown as Program
    const req = toProgramRequest(toProgramForm(program))
    expect(req).toMatchObject({
      universityId: 'uni-1',
      gre: 'REQUIRED',
      interview: 'CONDITIONAL',
      semesterFeeAmount: 150.25,
      nonEuTuitionAmount: 1500,
      nonEuTuitionPeriod: 'SEMESTER',
      scholarshipAvailable: true,
      languageRequirements: [
        { language: 'en', minCefr: 'C1', testName: 'IELTS', minScore: 6.5, rawText: null },
      ],
      intakes: [{ term: 'SUMMER', openDay: 1, openMonth: 12, closeDay: 15, closeMonth: 1, startMonth: 4 }],
    })
  })
})
