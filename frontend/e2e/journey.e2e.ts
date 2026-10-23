import { expect, test, type BrowserContext, type Page } from '@playwright/test'
import { expectAccessible } from './axe'

// Jornada principal com um usuário novo: cadastro → onboarding → gasto → busca global → páginas principais.
// Os passos dependem uns dos outros, então rodam em série na mesma página.
test.describe.configure({ mode: 'serial' })

let context: BrowserContext
let page: Page
const email = `e2e-${Date.now()}-${Math.random().toString(36).slice(2, 7)}@example.test`
const password = `E2e-${Math.random().toString(36).slice(2)}-Senha!`

function futureDate() {
  const d = new Date()
  d.setFullYear(d.getFullYear() + 1)
  return d.toISOString().slice(0, 10)
}

test.beforeAll(async ({ browser }, testInfo) => {
  // contexto explícito (o axe exige) com as opções do projeto: baseURL, viewport, locale…
  context = await browser.newContext(testInfo.project.use)
  page = await context.newPage()
})

test.afterAll(async () => {
  await context.close()
})

test('cadastro', async () => {
  await page.goto('/register')
  await expectAccessible(page, '/register')
  await page.getByLabel('Nome', { exact: true }).fill('Pessoa de Teste')
  await page.getByLabel('E-mail').fill(email)
  await page.getByLabel('Senha', { exact: true }).fill(password)
  await page.getByLabel('Confirmar senha').fill(password)
  await page.getByRole('button', { name: 'Criar conta' }).click()
  // primeira requisição pesada numa JVM recém-iniciada (BCrypt custo 12): mais folga que o padrão
  await expect(page).toHaveURL(/\/onboarding$/, { timeout: 30_000 })
})

test('onboarding cria o objetivo e abre o dashboard', async () => {
  await expect(page.getByText('Olá, Pessoa! Vamos montar seu plano.')).toBeVisible()
  await expectAccessible(page, '/onboarding')
  const next = page.getByRole('button', { name: /continuar/i })
  await page.getByLabel('Nome do objetivo').fill('Mestrado E2E')
  await next.click()
  await page.getByLabel('Cidade (opcional)').fill('Munique')
  await next.click()
  await page.getByLabel('Data alvo').fill(futureDate())
  await next.click()
  await page.getByRole('radio', { name: 'Euro (€)' }).check()
  await next.click()
  await page.getByLabel(/Economia mensal planejada/).fill('3.000')
  await next.click()
  await page.getByRole('button', { name: /começar/i }).click()
  await expect(page).toHaveURL(/\/$/)
  await expect(page.getByText('Mestrado E2E').first()).toBeVisible()
})

test('guarda dinheiro para o objetivo', async () => {
  await page.goto('/')
  await page.getByRole('button', { name: 'Guardar dinheiro' }).click()
  const dialog = page.getByRole('dialog', { name: 'Guardar dinheiro' })
  await expectAccessible(page, 'diálogo guardar dinheiro')
  await dialog.getByLabel('Valor', { exact: true }).fill('1.500')
  await dialog.getByRole('button', { name: 'Guardar', exact: true }).click()
  await expect(dialog).toBeHidden()
  await expect(page.getByText('R$ 1.500,00').first()).toBeVisible()
})

test('cadastra um gasto', async () => {
  await page.goto('/expenses')
  await page.getByRole('button', { name: 'Novo gasto' }).click()
  const dialog = page.getByRole('dialog', { name: 'Novo gasto' })
  await expectAccessible(page, 'diálogo de gasto')
  await dialog.getByLabel('Gasto', { exact: true }).fill('Seguro saúde estudantil')
  await dialog.getByLabel('Categoria', { exact: true }).selectOption({ index: 1 })
  await dialog.getByLabel('Valor', { exact: true }).fill('1.234,56')
  await dialog.getByRole('button', { name: 'Salvar' }).click()
  await expect(dialog).toBeHidden()
  await expect(page.getByText('Seguro saúde estudantil').first()).toBeVisible()
})

test('busca global encontra o gasto sem acento e abre a lista filtrada', async () => {
  await page.goto('/')
  await page.getByRole('button', { name: 'Buscar (Ctrl+K)' }).first().click()
  const input = page.getByRole('combobox', { name: /Buscar em/ })
  await input.fill('saude')
  const option = page.getByRole('option', { name: /Seguro saúde estudantil/ })
  await expect(option).toBeVisible()
  await expectAccessible(page, 'busca global')
  await input.press('Enter')
  await expect(page).toHaveURL(/\/expenses\?q=/)
  await expect(page.getByLabel('Buscar gastos')).toHaveValue('Seguro saúde estudantil')
})

const PAGES = [
  '/',
  '/objectives',
  '/expenses',
  '/finance',
  '/universities',
  '/applications',
  '/timeline',
  '/tasks',
  '/luggage',
  '/notifications',
  '/import',
  '/settings',
]

test('páginas principais sem violações de acessibilidade', async () => {
  test.setTimeout(PAGES.length * 20_000) // uma varredura do axe por tela
  for (const path of PAGES) {
    await test.step(path, async () => {
      await page.goto(path)
      await expect(page.getByRole('heading', { level: 1 })).toBeVisible()
      await page.waitForLoadState('networkidle')
      await expectAccessible(page, path, { soft: true })
    })
  }
})
