import { useSearchParams } from 'react-router'
import { PageHeader } from '@/components/page-header'
import { SegmentedControl } from '@/components/ui/segmented'
import { AccountsTab } from './accounts-tab'
import { InstallmentsTab } from './installments-tab'
import { MonthTab } from './month-tab'
import { RecurringTab } from './recurring-tab'

const TABS = [
  { value: 'month', label: 'Mês' },
  { value: 'installments', label: 'Parcelas' },
  { value: 'recurring', label: 'Assinaturas' },
  { value: 'accounts', label: 'Contas' },
] as const
type Tab = (typeof TABS)[number]['value']

/** Controle financeiro: visão do mês, parcelamentos, recorrências e contas (aba na URL: ?tab=). */
export function FinancePage() {
  const [params, setParams] = useSearchParams()
  const requested = params.get('tab')
  const tab: Tab = TABS.some((t) => t.value === requested) ? (requested as Tab) : 'month'

  return (
    <>
      <PageHeader
        title="Finanças"
        description="Quanto entra, quanto sai, o que já está comprometido e quanto sobra para o objetivo."
      />
      <SegmentedControl
        ariaLabel="Seção"
        className="mb-5"
        value={tab}
        onValueChange={(v) => setParams(v === 'month' ? {} : { tab: v }, { replace: true })}
        options={TABS.map((t) => ({ value: t.value, label: t.label }))}
      />
      {tab === 'month' && <MonthTab />}
      {tab === 'installments' && <InstallmentsTab />}
      {tab === 'recurring' && <RecurringTab />}
      {tab === 'accounts' && <AccountsTab />}
    </>
  )
}
