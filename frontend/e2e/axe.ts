import AxeBuilder from '@axe-core/playwright'
import { expect, type Page } from '@playwright/test'

/**
 * Varre a página com o axe (WCAG 2.1 A/AA) e falha listando cada violação com os seletores afetados.
 * `soft` continua o teste depois da falha, para uma execução mostrar todas as páginas com problema.
 */
export async function expectAccessible(page: Page, context: string, { soft = false } = {}) {
  const { violations } = await new AxeBuilder({ page })
    .withTags(['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa'])
    .analyze()
  const report = violations.map(
    (v) => `${v.id} (${v.impact}): ${v.help}\n    ${v.nodes.map((n) => n.target.join(' ')).join('\n    ')}`,
  )
  ;(soft ? expect.soft : expect)(report, `Acessibilidade em ${context}`).toEqual([])
}
