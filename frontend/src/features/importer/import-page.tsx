import { ArrowLeft, CheckCircle2, FileSpreadsheet, Upload } from 'lucide-react'
import { useRef, useState } from 'react'
import { Link } from 'react-router'
import { toast } from 'sonner'
import { errorMessage } from '@/api/errors'
import type { CommitRequest, ImportPreview, ImportSheet, SheetPreview } from '@/api/types'
import { PageHeader } from '@/components/page-header'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Field } from '@/components/ui/field'
import { SegmentedControl } from '@/components/ui/segmented'
import { NativeSelect } from '@/components/ui/select'
import { SwitchField } from '@/components/ui/switch'
import { useObjectives } from '@/features/objectives/api'
import { formatDateTime, pluralize } from '@/lib/format'
import { IMPORT_STATUS_LABELS, IMPORT_STATUS_TONES } from '@/lib/labels'
import { cn } from '@/lib/utils'
import { useCommitImport, useUploadSpreadsheet } from './api'

/**
 * Importação da planilha em 3 passos: enviar → conferir a prévia linha a linha → importar. Nada é gravado antes do
 * "Importar"; importar a mesma planilha de novo com "pular" não duplica.
 */
export function ImportPage() {
  const [preview, setPreview] = useState<ImportPreview | null>(null)
  return (
    <>
      <Link
        to="/settings"
        className="mb-4 inline-flex items-center gap-1.5 text-sm text-muted hover:text-foreground"
      >
        <ArrowLeft className="size-4" aria-hidden /> Configurações
      </Link>
      <PageHeader
        title="Importar planilha"
        description="Traga gastos, universidades e itens de mala da sua planilha do Excel (.xlsx). Você confere tudo antes de importar."
      />
      {!preview ? (
        <UploadStep onPreview={setPreview} />
      ) : preview.status === 'COMMITTED' ? (
        <SummaryStep preview={preview} onRestart={() => setPreview(null)} />
      ) : (
        <PreviewStep preview={preview} onDone={setPreview} onCancel={() => setPreview(null)} />
      )}
    </>
  )
}

function UploadStep({ onPreview }: { onPreview: (p: ImportPreview) => void }) {
  const upload = useUploadSpreadsheet()
  const input = useRef<HTMLInputElement>(null)
  const [dragging, setDragging] = useState(false)

  const send = (file: File | undefined) => {
    if (!file) return
    upload.mutate(file, { onSuccess: onPreview, onError: (e) => toast.error(errorMessage(e)) })
  }

  return (
    <Card>
      <CardContent>
        <button
          type="button"
          onClick={() => input.current?.click()}
          onDragOver={(e) => {
            e.preventDefault()
            setDragging(true)
          }}
          onDragLeave={() => setDragging(false)}
          onDrop={(e) => {
            e.preventDefault()
            setDragging(false)
            send(e.dataTransfer.files[0])
          }}
          disabled={upload.isPending}
          className={cn(
            'grid w-full place-items-center gap-2 rounded-card border-2 border-dashed px-6 py-14 text-center transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring/40',
            dragging ? 'border-primary bg-primary-soft/40' : 'border-border hover:border-primary/50',
          )}
        >
          <FileSpreadsheet className="size-10 text-primary" aria-hidden />
          <span className="font-medium">
            {upload.isPending ? 'Lendo a planilha…' : 'Escolha ou arraste a planilha'}
          </span>
          <span className="text-sm text-muted">
            Abas lidas: Informações dos Gastos, Análise de Universidades e Mala. Até 5 MB.
          </span>
        </button>
        <input
          ref={input}
          type="file"
          accept=".xlsx,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
          className="sr-only"
          tabIndex={-1}
          aria-label="Escolher planilha"
          onChange={(e) => send(e.target.files?.[0])}
        />
      </CardContent>
    </Card>
  )
}

function PreviewStep({
  preview,
  onDone,
  onCancel,
}: {
  preview: ImportPreview
  onDone: (p: ImportPreview) => void
  onCancel: () => void
}) {
  const objectives = useObjectives()
  const commit = useCommitImport()
  const [sheets, setSheets] = useState<ImportSheet[]>(preview.sheets.map((s) => s.sheet))
  const [objectiveId, setObjectiveId] = useState(preview.objectiveId ?? '')
  const [duplicates, setDuplicates] = useState<'SKIP' | 'UPDATE'>('SKIP')
  const [sperrkonto, setSperrkonto] = useState(true)
  const [tab, setTab] = useState<ImportSheet>(preview.sheets[0]!.sheet)
  const current = preview.sheets.find((s) => s.sheet === tab) ?? preview.sheets[0]!
  const hasExpenses = preview.sheets.some((s) => s.sheet === 'EXPENSES')

  const submit = () => {
    const body: CommitRequest = {
      sheets,
      objectiveId: objectiveId || undefined,
      duplicates,
      sperrkontoAdjustment: sperrkonto,
    }
    commit.mutate(
      { id: preview.id, body },
      {
        onSuccess: (result) => {
          toast.success('Planilha importada.')
          onDone(result)
        },
        onError: (e) => toast.error(errorMessage(e)),
      },
    )
  }

  return (
    <div className="grid gap-5 lg:grid-cols-[1fr_20rem]">
      <Card className="min-w-0">
        <CardHeader className="flex-col">
          <CardTitle>Prévia de {preview.fileName}</CardTitle>
          <CardDescription>
            Nada foi gravado ainda. Linhas com erro não entram; as com aviso entram como mostrado.
            {preview.ignoredSheets.length > 0 && ` Abas ignoradas: ${preview.ignoredSheets.join(', ')}.`}
          </CardDescription>
        </CardHeader>
        <CardContent className="grid gap-4">
          <SegmentedControl
            ariaLabel="Aba"
            value={tab}
            onValueChange={setTab}
            options={preview.sheets.map((s) => ({ value: s.sheet, label: `${s.title} (${s.rows.length})` }))}
          />
          <SheetCounts sheet={current} />
          <div className="overflow-x-auto">
            <table className="w-full min-w-[34rem] text-sm">
              <caption className="sr-only">Linhas da aba {current.title}</caption>
              <thead>
                <tr className="border-b border-border text-left text-xs text-muted">
                  <th className="w-14 py-2 font-medium">Linha</th>
                  <th className="py-2 font-medium">Item</th>
                  <th className="w-28 py-2 font-medium">Situação</th>
                </tr>
              </thead>
              <tbody>
                {current.rows.map((r) => (
                  <tr key={r.row} className="border-b border-border align-top last:border-0">
                    <td className="py-2 tabular text-muted">{r.row}</td>
                    <td className="py-2">
                      <p>{r.label}</p>
                      {r.messages.length > 0 && (
                        <ul className="mt-0.5 grid gap-0.5 text-xs text-muted">
                          {r.messages.map((m, i) => (
                            <li key={i}>{m}</li>
                          ))}
                        </ul>
                      )}
                    </td>
                    <td className="py-2">
                      <Badge tone={IMPORT_STATUS_TONES[r.status]}>{IMPORT_STATUS_LABELS[r.status]}</Badge>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </CardContent>
      </Card>

      <Card className="self-start">
        <CardHeader className="flex-col">
          <CardTitle>Importar</CardTitle>
          <CardDescription>Expira em {formatDateTime(preview.expiresAt)}.</CardDescription>
        </CardHeader>
        <CardContent className="grid gap-4">
          <fieldset className="grid gap-1">
            <legend className="mb-1 text-sm font-medium">O que importar</legend>
            {preview.sheets.map((s) => (
              <SwitchField
                key={s.sheet}
                id={`sheet-${s.sheet}`}
                label={s.title}
                hint={pluralize(s.valid + s.warnings + s.duplicates, 'linha', 'linhas')}
                checked={sheets.includes(s.sheet)}
                onCheckedChange={(on) =>
                  setSheets((list) => (on ? [...list, s.sheet] : list.filter((x) => x !== s.sheet)))
                }
              />
            ))}
          </fieldset>
          {hasExpenses && sheets.includes('EXPENSES') && (
            <>
              <Field id="import-objective" label="Gastos vão para o objetivo">
                <NativeSelect
                  id="import-objective"
                  value={objectiveId}
                  onChange={(e) => setObjectiveId(e.target.value)}
                  disabled={!objectives.data}
                >
                  {!objectives.data && <option value={objectiveId}>Carregando…</option>}
                  {objectives.data?.map((o) => (
                    <option key={o.id} value={o.id}>
                      {o.name}
                    </option>
                  ))}
                </NativeSelect>
              </Field>
              <SwitchField
                id="import-sperrkonto"
                label="Descontar o Sperrkonto da meta"
                hint="Cria o ajuste −Sperrkonto uma única vez"
                checked={sperrkonto}
                onCheckedChange={setSperrkonto}
              />
            </>
          )}
          <Field id="import-duplicates" label="O que já existe">
            <NativeSelect
              id="import-duplicates"
              value={duplicates}
              onChange={(e) => setDuplicates(e.target.value as 'SKIP' | 'UPDATE')}
            >
              <option value="SKIP">Manter como está (pular)</option>
              <option value="UPDATE">Atualizar com a planilha</option>
            </NativeSelect>
          </Field>
          <div className="flex gap-2">
            <Button variant="outline" onClick={onCancel}>
              Outra planilha
            </Button>
            <Button
              className="flex-1"
              loading={commit.isPending}
              disabled={sheets.length === 0}
              onClick={submit}
            >
              <Upload aria-hidden /> Importar
            </Button>
          </div>
        </CardContent>
      </Card>
    </div>
  )
}

function SheetCounts({ sheet: s }: { sheet: SheetPreview }) {
  const items: [string, number, 'primary' | 'accent' | 'danger' | 'neutral'][] = [
    ['ok', s.valid, 'primary'],
    ['com aviso', s.warnings, 'accent'],
    ['já existem', s.duplicates, 'neutral'],
    ['com erro', s.errors, 'danger'],
  ]
  return (
    <p className="flex flex-wrap gap-2">
      {items
        .filter(([, n]) => n > 0)
        .map(([label, n, tone]) => (
          <Badge key={label} tone={tone}>
            {n} {label}
          </Badge>
        ))}
    </p>
  )
}

const SHEET_LINKS: Record<ImportSheet, { to: string; label: string }> = {
  EXPENSES: { to: '/expenses', label: 'Ver gastos' },
  UNIVERSITIES: { to: '/universities', label: 'Ver universidades' },
  LUGGAGE: { to: '/luggage', label: 'Ver mala' },
}

function SummaryStep({ preview, onRestart }: { preview: ImportPreview; onRestart: () => void }) {
  const summary = preview.summary
  const titles = Object.fromEntries(preview.sheets.map((s) => [s.sheet, s.title]))
  return (
    <Card>
      <CardContent className="grid gap-5">
        <div className="flex items-center gap-3">
          <CheckCircle2 className="size-8 text-primary" aria-hidden />
          <div>
            <p className="font-display text-xl font-semibold">Importação concluída</p>
            <p className="text-sm text-muted">{preview.fileName}</p>
          </div>
        </div>
        <ul className="grid gap-3 sm:grid-cols-3">
          {summary?.sheets.map((s) => (
            <li key={s.sheet} className="rounded-xl border border-border p-4">
              <p className="font-medium">{titles[s.sheet] ?? s.sheet}</p>
              <p className="mt-1 text-sm text-muted">
                {s.created} criado{s.created === 1 ? '' : 's'} · {s.updated} atualizado
                {s.updated === 1 ? '' : 's'} · {s.skipped} pulado{s.skipped === 1 ? '' : 's'}
              </p>
              <Link
                to={SHEET_LINKS[s.sheet].to}
                className="mt-2 inline-block text-sm text-primary hover:underline"
              >
                {SHEET_LINKS[s.sheet].label}
              </Link>
            </li>
          ))}
        </ul>
        {summary && summary.notes.length > 0 && (
          <ul className="grid gap-1 text-sm text-muted">
            {summary.notes.map((n, i) => (
              <li key={i}>· {n}</li>
            ))}
          </ul>
        )}
        <Button variant="outline" className="justify-self-start" onClick={onRestart}>
          Importar outra planilha
        </Button>
      </CardContent>
    </Card>
  )
}
