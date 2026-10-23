import { z } from 'zod'
import type { ApplicationChannel, Program, ProgramRequest, Rating, Requirement, Term } from '@/api/types'
import { decimalToInput, isValidDecimal, parseDecimal } from '@/lib/decimal'

// Mapeamento puro do formulário de programa (strings do input ⇄ ProgramRequest), testável sem React.

const money = z.string().refine((v) => isValidDecimal(v, { allowEmpty: true }), 'Valor inválido.')
const dayOrEmpty = z.string().refine((v) => v === '' || (/^\d+$/.test(v) && +v >= 1 && +v <= 31), 'Dia 1–31.')

export const programSchema = z.object({
  universityId: z.string().min(1, 'Escolha a universidade.'),
  name: z.string().trim().min(1, 'Informe o nome.').max(200),
  degree: z.string().max(40),
  area: z.string().max(500),
  languageSummary: z.string().max(60),
  durationSemesters: z
    .string()
    .refine((v) => v === '' || (/^\d+$/.test(v) && +v >= 1 && +v <= 20), '1 a 20.'),
  officialUrl: z.string().max(500),
  gre: z.string(),
  aptitudeAssessment: z.string(),
  interview: z.string(),
  nc: z.string(),
  applicationChannel: z.string(),
  portalName: z.string().max(60),
  documentSubmission: z.string(),
  semesterFee: money,
  nonEuTuition: money,
  nonEuTuitionPeriod: z.string(),
  scholarshipAvailable: z.string(),
  scholarshipDetails: z.string().max(500),
  scholarshipChance: z.string(),
  admissionChance: z.string(),
  applicationDeadlineText: z.string().max(200),
  notes: z.string().max(2000),
  languageRequirements: z.array(
    z.object({
      language: z.string().regex(/^[a-zA-Z]{2,5}$/, 'Ex.: en, de'),
      minCefr: z.string(),
      testName: z.string().max(30),
      minScore: z
        .string()
        .refine(
          (v) => isValidDecimal(v, { allowEmpty: true, thousands: false, maxFraction: 1 }),
          'Nota inválida.',
        ),
      rawText: z.string().max(200),
    }),
  ),
  intakes: z.array(
    z.object({
      term: z.enum(['WINTER', 'SUMMER', 'OTHER']),
      openDay: dayOrEmpty,
      openMonth: z.string(),
      closeDay: dayOrEmpty,
      closeMonth: z.string(),
      startMonth: z.string().min(1),
    }),
  ),
})
export type ProgramForm = z.infer<typeof programSchema>

const s = (v: string | number | null | undefined) => (v == null ? '' : String(v))
const orUndefined = (v: string) => (v.trim() === '' ? undefined : v.trim())
const intOrUndefined = (v: string) => (v.trim() === '' ? undefined : Number(v))

export function toProgramForm(p: Program | undefined, universityId?: string): ProgramForm {
  return {
    universityId: p?.university.id ?? universityId ?? '',
    name: p?.name ?? '',
    degree: p?.degree ?? 'M.Sc.',
    area: p?.area ?? '',
    languageSummary: p?.languageSummary ?? '',
    durationSemesters: s(p?.durationSemesters ?? 4),
    officialUrl: p?.officialUrl ?? '',
    gre: p?.gre ?? 'UNKNOWN',
    aptitudeAssessment: p?.aptitudeAssessment ?? 'UNKNOWN',
    interview: p?.interview ?? 'UNKNOWN',
    nc: p?.nc ?? 'UNKNOWN',
    applicationChannel: p?.applicationChannel ?? '',
    portalName: p?.portalName ?? '',
    documentSubmission: p?.documentSubmission ?? '',
    semesterFee: decimalToInput(p?.semesterFee?.amount),
    nonEuTuition: decimalToInput(p?.nonEuTuition?.amount),
    nonEuTuitionPeriod: p?.nonEuTuitionPeriod ?? 'SEMESTER',
    scholarshipAvailable: p?.scholarshipAvailable == null ? '' : String(p.scholarshipAvailable),
    scholarshipDetails: p?.scholarshipDetails ?? '',
    scholarshipChance: p?.scholarshipChance ?? '',
    admissionChance: p?.admissionChance ?? '',
    applicationDeadlineText: p?.applicationDeadlineText ?? '',
    notes: p?.notes ?? '',
    languageRequirements: (p?.languageRequirements ?? []).map((r) => ({
      language: r.language,
      minCefr: r.minCefr ?? '',
      testName: r.testName ?? '',
      minScore: s(r.minScore).replace('.', ','),
      rawText: r.rawText ?? '',
    })),
    intakes: (
      p?.intakes ?? [
        { term: 'WINTER', openMonth: null, openDay: null, closeMonth: null, closeDay: null, startMonth: 10 },
      ]
    ).map((i) => ({
      term: i.term,
      openDay: s(i.openDay),
      openMonth: s(i.openMonth),
      closeDay: s(i.closeDay),
      closeMonth: s(i.closeMonth),
      startMonth: s(i.startMonth),
    })),
  }
}

export function toProgramRequest(v: ProgramForm): ProgramRequest {
  return {
    universityId: v.universityId,
    name: v.name.trim(),
    degree: orUndefined(v.degree),
    area: orUndefined(v.area),
    languageSummary: orUndefined(v.languageSummary),
    durationSemesters: intOrUndefined(v.durationSemesters),
    officialUrl: orUndefined(v.officialUrl),
    gre: v.gre as Requirement,
    aptitudeAssessment: v.aptitudeAssessment as Requirement,
    interview: v.interview as Requirement,
    nc: v.nc as Requirement,
    applicationChannel: (v.applicationChannel || undefined) as ApplicationChannel | undefined,
    portalName: orUndefined(v.portalName),
    documentSubmission: (v.documentSubmission || undefined) as ProgramRequest['documentSubmission'],
    semesterFeeAmount: parseDecimal(v.semesterFee) ?? undefined,
    semesterFeeCurrency: v.semesterFee.trim() ? 'EUR' : undefined,
    nonEuTuitionAmount: parseDecimal(v.nonEuTuition) ?? undefined,
    nonEuTuitionCurrency: v.nonEuTuition.trim() ? 'EUR' : undefined,
    nonEuTuitionPeriod: v.nonEuTuition.trim()
      ? (v.nonEuTuitionPeriod as ProgramRequest['nonEuTuitionPeriod'])
      : undefined,
    scholarshipAvailable: v.scholarshipAvailable === '' ? undefined : v.scholarshipAvailable === 'true',
    scholarshipDetails: orUndefined(v.scholarshipDetails),
    scholarshipChance: (v.scholarshipChance || undefined) as Rating | undefined,
    admissionChance: (v.admissionChance || undefined) as Rating | undefined,
    applicationDeadlineText: orUndefined(v.applicationDeadlineText),
    notes: orUndefined(v.notes),
    languageRequirements: v.languageRequirements.map((r) => ({
      language: r.language.toLowerCase(),
      minCefr: (r.minCefr || null) as LanguageCefr,
      testName: orUndefined(r.testName) ?? null,
      minScore: parseDecimal(r.minScore, { thousands: false }) ?? null,
      rawText: orUndefined(r.rawText) ?? null,
    })),
    intakes: v.intakes.map((i) => ({
      term: i.term as Term,
      openDay: intOrUndefined(i.openDay) ?? null,
      openMonth: intOrUndefined(i.openMonth) ?? null,
      closeDay: intOrUndefined(i.closeDay) ?? null,
      closeMonth: intOrUndefined(i.closeMonth) ?? null,
      startMonth: Number(i.startMonth),
    })),
  }
}
type LanguageCefr = NonNullable<ProgramRequest['languageRequirements']>[number]['minCefr']
