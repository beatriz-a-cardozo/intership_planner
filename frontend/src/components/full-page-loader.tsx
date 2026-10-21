import { BrandMark } from './brand'

export function FullPageLoader() {
  return (
    <div className="grid min-h-dvh place-items-center" role="status" aria-live="polite">
      <div className="flex flex-col items-center gap-3">
        <BrandMark className="size-10 animate-pulse" />
        <span className="sr-only">Carregando…</span>
      </div>
    </div>
  )
}
