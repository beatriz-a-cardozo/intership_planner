import { Outlet } from 'react-router'
import { Brand } from '@/components/brand'

export function AuthLayout() {
  return (
    <div className="grid min-h-dvh lg:grid-cols-[1.1fr_1fr]">
      <section className="relative hidden overflow-hidden bg-primary p-12 text-primary-foreground lg:flex lg:flex-col lg:justify-between">
        <Brand className="[&_rect]:fill-primary-foreground [&_path]:stroke-primary" />
        <div>
          <p className="font-display text-5xl leading-[1.05] tracking-tight">
            Cada real guardado
            <br />é um passo
            <br />
            <span className="text-accent-soft">mais perto.</span>
          </p>
          <p className="mt-6 max-w-md text-primary-foreground/85">
            Planeje gastos, acompanhe sua economia, compare universidades e veja o cronograma até a viagem num só
            lugar.
          </p>
        </div>
        <p className="text-sm text-primary-foreground/80">Planejamento financeiro e de intercâmbio</p>
        {/* Linha do tempo decorativa: assinatura visual do produto */}
        <div aria-hidden className="pointer-events-none absolute right-16 top-0 h-full w-1.5 bg-accent/80" />
      </section>
      <section className="flex flex-col px-5 py-8 sm:px-10">
        <div className="lg:hidden">
          <Brand />
        </div>
        <div className="mx-auto flex w-full max-w-sm flex-1 flex-col justify-center py-10">
          <Outlet />
        </div>
      </section>
    </div>
  )
}
