import { Construction } from 'lucide-react'
import { Link } from 'react-router'
import { PageHeader } from '@/components/page-header'
import { Button } from '@/components/ui/button'
import { Card } from '@/components/ui/card'
import { EmptyState } from '@/components/empty-state'
import type { NavItem } from '@/layouts/navigation'

/** Página exibida para módulos ainda não entregues no roadmap (sem dados falsos). */
export function PlannedModulePage({ item }: { item: NavItem }) {
  return (
    <>
      <PageHeader title={item.label} eyebrow={`Chega na fase ${item.phase}`} />
      <Card>
        <EmptyState
          icon={Construction}
          title="Este módulo ainda está em construção"
          description={item.description}
          action={
            <Button asChild variant="outline">
              <Link to="/">Voltar ao dashboard</Link>
            </Button>
          }
        />
      </Card>
    </>
  )
}
