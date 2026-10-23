# Interchange Planner

> Nome provisório. Para renomear: `APP_NAME` (backend/compose) e `VITE_APP_NAME` (frontend).

Planejamento financeiro e de intercâmbio: objetivo com meta e progresso, gastos por ciclo e categoria em BRL/EUR,
controle financeiro (receitas, despesas, parcelas, assinaturas), universidades e candidaturas, timeline, mala
e notificações. O plano técnico completo está em [`PLANO_TECNICO_v2.md`](PLANO_TECNICO_v2.md).

## Status das fases

- [x] **1. Fundação** — monorepo, Docker, Flyway V1, autenticação JWT + refresh rotativo, Problem Details, OpenAPI → tipos TS, AppShell responsivo, tema claro/escuro/sistema, login/registro, CI
- [x] **2. Objetivos, câmbio, contas e onboarding** — CRUD de objetivos e objetivo principal, contas e saldos BRL/EUR, cotação com 2 provedores + fallback + cotação manual, meta e progresso, onboarding em 6 passos, dashboard
- [x] **3. Gastos** — categorias e ciclos por usuário, gastos com periodicidade/ocorrências, pagamentos parciais em qualquer moeda, ajustes manuais da meta (ex.: Sperrkonto), filtros, resumos e gráficos, total em tempo real
- [x] **4. Controle financeiro** — receitas e despesas por categoria, parcelamentos, assinaturas/recorrências materializadas, meta de economia mensal, visão de 12 meses, compromissos futuros e projeção de quando a meta é alcançada
- [x] **5. Universidades e candidaturas** — universidades e programas, requisitos, janelas de inscrição recalculadas a cada ano, "Escolhida" cria a candidatura, quadro de status com histórico, solicitações da universidade, estimativa de resposta e comparador
- [x] **6. Timeline e tarefas** — timeline editorial do planejamento: gastos com data, períodos das universidades (inscrição, prazo, resposta, solicitações) e tarefas, projetados na leitura, + eventos manuais; tarefas com prioridade, prazo e vínculos; próximos itens no dashboard
- [x] **7. Mala** — malas com limite de peso, itens com quantidade e peso em gramas, alertas acima de 90% e de 100%, não alocados e checklist de embalados
- [x] **Ajustes** — fotos das universidades (galeria + capa nos cards), metas obrigatória e completa lado a lado, parcelamento já em andamento ("parcela 4 de 10") e atalho "É parcelado?" no lançamento
- [x] **8. Notificações** — avisos de prazos, vencimentos (gastos e parcelas), respostas esperadas, tarefas, câmbio (alertas com histerese) e marcos do objetivo, com antecedências por categoria, deduplicação no banco, sino com contador e central; ShedLock nos jobs
- [x] **9. Importação do Excel** — prévia linha a linha (válida/aviso/erro/já existe) das abas Gastos, Universidades e Mala com as correções do plano, importação transacional e idempotente; regressão com a planilha real batendo os valores de referência
- [x] **10. Polimento e deploy** — busca global (Ctrl+K, sem acento), acessibilidade WCAG AA verificada com axe, E2E com Playwright (desktop e celular) na imagem de produção, imagem única (frontend dentro do backend), deploy contínuo no Render + Neon por tag e jobs agendados pelo GitHub Actions

## Arquitetura

```
┌──────────────────────┐     /api (mesmo domínio)     ┌────────────────────────────┐      ┌──────────────┐
│ frontend (nginx)     │ ───────────────────────────▶ │ backend (Spring Boot)      │ ───▶ │ PostgreSQL 17│
│ React 19 + Vite SPA  │   JWT Bearer + cookie refresh │ monólito modular por domínio│      │ Flyway       │
└──────────────────────┘                               └────────────────────────────┘      └──────────────┘
```

Em produção é **uma imagem só** (`deploy/Dockerfile`): o frontend compilado vai para dentro do backend, que serve
as páginas e a API no mesmo endereço. Deploy gratuito no Render + Neon: [`docs/DEPLOY.md`](docs/DEPLOY.md).

Backend em **package-by-feature** (`com.interchangeplanner`):

| Pacote | Responsabilidade |
|---|---|
| `shared` | Entidade base (UUIDv7, auditoria, lock otimista), erros RFC 9457, `CurrentUser`, `RequestIdFilter` |
| `config` | Segurança, JWT, CORS, OpenAPI, Clock, agendamento, propriedades `app.*` validadas |
| `auth` | Registro, login, refresh rotativo com detecção de reuso, logout, bloqueio por tentativas |
| `user` | Perfil e preferências (tema, idioma, fuso, moedas) |
| `currency` | Catálogo de moedas suportadas (BRL, EUR) |
| `exchange` | Provedores de cotação (Frankfurter, AwesomeAPI), histórico, cache, fallback, cotação manual, `CurrencyConverter` |
| `objective` | Objetivos, objetivo principal, ajustes manuais da meta, `ObjectiveProgressCalculator` |
| `finance.account` | Contas e histórico de saldos |
| `finance.ledger` | Lançamentos, categorias de receita/despesa, parcelamentos, recorrências, meta mensal, resumos e projeção (`InstallmentCalculator`, `RecurrenceScheduler`, `SavingsProjectionCalculator`) |
| `expense` | Gastos, pagamentos, categorias e ciclos, `ExpenseCalculator`, resumos |
| `university` | Universidades, programas, requisitos de idioma, janelas de inscrição (`IntakeWindowCalculator`), comparador |
| `applications` | Candidaturas, histórico de status, solicitações da universidade, estimativa de resposta |
| `task` | Tarefas com prioridade, prazo e vínculos (objetivo, universidade, gasto, candidatura) |
| `timeline` | Eventos manuais, `TimelineProjector` (um por módulo) e `TimelineService` (junta e ordena) |
| `luggage` | Malas e itens, `LuggageWeightCalculator` (pesos em gramas e alertas) |
| `notification` | Detectores por categoria, preferências, alertas de câmbio (`ExchangeAlertEvaluator`), job diário |
| `importer` | Leitura do .xlsx (Apache POI), parsers das abas, prévia em sessão e importação |
| `onboarding` / `dashboard` | Orquestração: criação guiada do objetivo e tela inicial (sem regras próprias) |

Cada módulo segue `api / application / domain`. Regras ficam em **calculadoras puras** (sem Spring), testadas isoladamente.
Decisões registradas em [`docs/adr/`](docs/adr).

## Stack

| Camada | Tecnologia |
|---|---|
| Backend | Java 21, Spring Boot 4.1.1, Spring Security (OAuth2 Resource Server / Nimbus), Spring Data JPA, Flyway, springdoc-openapi 3.1, MapStruct 1.6 |
| Banco | PostgreSQL 17 |
| Frontend | React 19, TypeScript 5.9, Vite 8, React Router 7, TanStack Query 5, Tailwind CSS 4, Radix UI, React Hook Form + Zod 4, lucide-react, sonner |
| Testes | JUnit 5, MockMvc, Testcontainers (PostgreSQL real), Vitest + Testing Library |
| Infra | Docker / Docker Compose, nginx, GitHub Actions, GHCR, Dependabot |

## Pré-requisitos

- **Docker** (Docker Desktop no Windows/macOS) — para rodar tudo e para os testes de integração
- Para desenvolver fora do Docker: **JDK 21+** e **Node.js 22.22+** (o Maven vem pelo wrapper `./mvnw`)

## Rodando com Docker

```bash
cp .env.example .env        # preencha JWT_SECRET (openssl rand -base64 48) e DB_PASSWORD
docker compose up --build
```

- App: http://localhost:8081
- API / Swagger UI: http://localhost:8080/swagger-ui.html
- Health: http://localhost:8080/actuator/health

## Rodando fora do Docker (desenvolvimento)

```bash
# 1. Só o banco
docker compose up -d postgres

# 2. Backend (perfil dev: segredo de desenvolvimento e cookie sem Secure)
cd backend
SPRING_PROFILES_ACTIVE=dev DB_PASSWORD=<a do .env> ./mvnw spring-boot:run
#   Windows PowerShell: $env:SPRING_PROFILES_ACTIVE="dev"; $env:DB_PASSWORD="..."; .\mvnw.cmd spring-boot:run

# 3. Frontend (Vite com proxy /api -> :8080)
cd frontend
npm ci
npm run dev                 # http://localhost:5173
```

## Variáveis de ambiente

| Variável | Onde | Descrição | Exemplo |
|---|---|---|---|
| `APP_NAME` | backend, compose | Nome exibido do produto | `Interchange Planner` |
| `VITE_APP_NAME` | frontend (build) | Nome no frontend | `Interchange Planner` |
| `VITE_API_BASE_URL` | frontend (build) | URL da API; vazio = mesmo domínio (`/api`) | *(vazio)* |
| `VITE_DEV_API_TARGET` | frontend (dev) | Alvo do proxy do Vite | `http://localhost:8080` |
| `DB_URL` | backend | JDBC do PostgreSQL | `jdbc:postgresql://localhost:5432/interchange_planner` |
| `DB_USERNAME` / `DB_PASSWORD` | backend, compose | Credenciais do banco | `interchange` / — |
| `JWT_SECRET` | backend | **Obrigatória.** Chave HMAC do JWT, ≥ 32 caracteres | `openssl rand -base64 48` |
| `JWT_ACCESS_TOKEN_TTL` | backend | Validade do access token | `15m` |
| `REFRESH_TOKEN_TTL` | backend | Validade do refresh token | `30d` |
| `REFRESH_COOKIE_SECURE` | backend | Flag `Secure` do cookie (true em HTTPS) | `true` |
| `APP_CORS_ALLOWED_ORIGINS` | backend | Origens extras no CORS, separadas por vírgula | `https://app.exemplo.com` |
| `LOGIN_MAX_FAILED_ATTEMPTS` / `LOGIN_LOCK_DURATION` | backend | Bloqueio contra força bruta | `5` / `15m` |
| `APP_JOBS_ENABLED` | backend | Liga/desliga jobs agendados | `true` |
| `EXCHANGE_PROVIDERS` | backend | Ordem dos provedores de câmbio (vazio = sem busca externa) | `frankfurter,awesomeapi` |
| `EXCHANGE_REFRESH_CRON` | backend | Atualização das cotações (UTC) | `0 0 */6 * * *` |
| `EXCHANGE_STALE_AFTER` | backend | Sem atualização por esse tempo = selo "desatualizada" | `12h` |
| `NOTIFICATIONS_CRON` / `NOTIFICATIONS_ZONE` | backend | Geração diária das notificações | `0 0 8 * * *` / `America/Sao_Paulo` |
| `INTERNAL_JOB_TOKEN` | backend | Token do endpoint `/api/internal/jobs/*` (jobs disparados de fora); vazio = desligado | `openssl rand -hex 32` |
| `PORT` | backend | Porta HTTP (o Render define sozinho) | `8080` |
| `E2E_BASE_URL` | testes E2E | Endereço do app que o Playwright testa | `http://localhost:8080` |
| `IMPORT_REGRESSION_FILE` | testes | Caminho da planilha real para `RealSpreadsheetIT` (opcional) | `~/Intercâmbio/Gastos.xlsx` |
| `BACKEND_URL` | frontend (nginx, runtime) | Para onde o nginx encaminha `/api` | `http://backend:8080` |
| `SPRING_PROFILES_ACTIVE` | backend | `dev` para desenvolvimento local | `dev` |

## Banco e migrations

- Migrations Flyway em `backend/src/main/resources/db/migration` (`V{n}__descricao.sql`), aplicadas na inicialização.
- `spring.jpa.hibernate.ddl-auto=validate`: o Hibernate só valida; o schema é 100% das migrations.
- Convenções: PK `uuid` (UUIDv7 gerado na aplicação), `created_at`/`updated_at` em `timestamptz`, `version` para lock otimista, enums como `varchar` + `CHECK`, dinheiro em `numeric(19,4)`.

## Testes

```bash
cd backend && ./mvnw verify      # unitários (*Test) + integração (*IT, Testcontainers — precisa do Docker)
cd frontend && npm test          # Vitest
npm run lint && npm run typecheck
E2E_BASE_URL=http://localhost:8080 npm run e2e   # Playwright + axe contra o app no ar (cria um usuário novo)
```

Hoje: **102 testes unitários + 90 de integração** no backend (+ a regressão com a planilha real, opcional), **45** no frontend
e a jornada **E2E** (cadastro → onboarding → gasto → busca → 12 telas varridas pelo axe) no desktop e no celular.

- **Fase 1:** JWT (expirado, assinatura inválida), bloqueio de login, UUIDv7, migrations e constraints, fluxo de
  autenticação (refresh rotativo, **reuso revoga a família**), Problem Details, isolamento entre usuários, cliente HTTP
  com refresh single-flight, cadastro e tema.
- **Fase 2:** `Money` (moedas nunca se misturam), conversão direta/inversa, provedores (formato real das APIs, erros),
  ordem e fallback de provedores, cotação desatualizada com `Clock` controlado, reserva sem cotação, cotação manual
  por usuário, meta/progresso/meses restantes/virada de ano/objetivo vencido, objetivo principal único, contas e saldos
  (substituição no mesmo dia), onboarding, dashboard, isolamento.
- **Fase 3:** ocorrências por periodicidade e horizonte, regras de único/por unidade/personalizado, total, status
  derivado, pagamento em outra moeda com **taxa capturada** (não muda com a cotação), cancelamento fora dos totais,
  **ajuste do Sperrkonto aplicado uma vez**, resumos por ciclo/periodicidade, filtros, catálogo em uso não é excluído,
  isolamento de gastos e categorias; no frontend, onboarding, formulário de gasto e leitura de valores em pt-BR.
- **Fase 4:** parcelas (centavos na última, vencimento dia 31, valor da parcela ou total), recorrências (meses
  curtos, dia de cobrança já passado, semanal/anual/a cada N meses atravessando o ano, data final), projeção
  (alcança antes, parcelas reduzem a economia, salário programado, déficit, nunca alcança), parcelamento
  redistribuído ao editar, parcela não excluída sozinha, materialização com pausa/retomada e confirmação
  automática, status padrão pela data, resumo do mês com conversão de moedas, isolamento.
- **Fase 5:** janelas de inscrição (atravessando o ano, aberta, prazo no ano anterior, próximo ciclo, dia
  inexistente), estimativa de resposta em ordem, resultado derivado do status, filtros, comparador com destaques,
  "Escolhida" cria e desmarcar arquiva, histórico e "item solicitado" automático, duplicata no mesmo ciclo,
  exclusão em cascata, isolamento; no frontend, formulário de programa, faixa de resposta e navegação entre meses.
- **Fases 6 e 7 + ajustes:** merge/ordenação e ids estáveis da timeline, ocorrências de gastos recorrentes,
  abertura/prazo/resposta das candidaturas, tarefas (ordem, conclusão, atraso), eventos, próximos no dashboard,
  peso da mala (soma em gramas, aviso >90% pelos gramas exatos, excesso, não alocados), fotos (tipo pelos bytes,
  capa automática, isolamento), metas obrigatória e completa, parcelamento em andamento; no frontend, conversão
  de peso, cartão das duas metas e diálogo de conta com lista carregada depois.
- **Fases 8 e 9:** escolha da antecedência, histerese do alerta de câmbio, avisos sem repetição, preferências,
  vencimento de gasto e marco do objetivo, isolamento; parsers de prazo e proficiência em texto livre, unificação de
  categorias por similaridade, prévia e importação de uma planilha sintética (fórmula, data passada, tipo inválido,
  categoria nova, Sperrkonto, mala propagada), idempotência, abas escolhidas, atualização de duplicados, arquivo
  inválido. `RealSpreadsheetIT` importa a planilha real quando `IMPORT_REGRESSION_FILE` aponta para ela.
- **Fase 10:** busca sem acento, ordem por relevância e isolamento; endpoint interno de jobs (sem token, token
  errado, job desconhecido). E2E: jornada completa e axe (WCAG 2.1 A/AA) em todas as telas principais.

## Contrato OpenAPI → TypeScript

O `OpenApiSpecIT` exporta o contrato para `backend/openapi/openapi.json` durante o `./mvnw verify`.
O frontend gera os tipos a partir dele:

```bash
cd frontend && npm run gen:api   # atualiza src/api/schema.d.ts
```

Os dois arquivos são versionados; o CI falha se algum estiver desatualizado.

## Estrutura de pastas

```
.
├── backend/                 API Spring Boot (mvnw, Dockerfile, openapi/openapi.json)
│   └── src/main/java/com/interchangeplanner/{shared,config,auth,user,currency,exchange,objective,finance,expense,university,applications,task,timeline,luggage,notification,importer,onboarding,dashboard}
├── frontend/                SPA React (Dockerfile + nginx)
│   └── src/{app,api,layouts,components,features,lib,styles}
├── docs/adr/                Decisões de arquitetura
├── .github/                 CI, imagens Docker, Dependabot
├── docker-compose.yml
└── .env.example
```

## Endpoints

| Método | Rota | Auth | Descrição |
|---|---|---|---|
| POST | `/api/auth/register` | — | Cria conta e inicia sessão (201) |
| POST | `/api/auth/login` | — | Login; 401 genérico, 429 após 5 falhas |
| POST | `/api/auth/refresh` | cookie | Rotaciona o refresh token e devolve novo access token |
| POST | `/api/auth/logout` | cookie | Revoga a sessão (204) |
| GET | `/api/me` | Bearer | Usuário autenticado + preferências |
| GET / PUT | `/api/me/settings` | Bearer | Preferências (tema, idioma, fuso, moedas) |
| GET | `/api/currencies` | — | Moedas suportadas |
| POST | `/api/onboarding` | Bearer | Cria objetivo principal + reserva inicial e conclui o onboarding |
| GET | `/api/dashboard` | Bearer | Objetivo, progresso, saldos, resumo de gastos e cotação numa chamada |
| GET / POST | `/api/objectives` | Bearer | Lista (principal primeiro) / cria |
| GET / PUT / DELETE | `/api/objectives/{id}` | Bearer | Detalhe / edita / exclui (gastos em cascata; contas desvinculadas) |
| PUT | `/api/objectives/{id}/primary` | Bearer | Torna principal |
| GET | `/api/objectives/{id}/progress` | Bearer | Meta, mobilizado, faltam, economia mensal necessária |
| GET / POST / PUT / DELETE | `/api/objectives/{id}/adjustments[/{adjustmentId}]` | Bearer | Ajustes manuais da meta |
| GET / POST / PUT / DELETE | `/api/accounts[/{id}]` | Bearer | Contas |
| GET / POST | `/api/accounts/{id}/balances` | Bearer | Histórico / lança saldo numa data |
| GET | `/api/accounts/summary` | Bearer | Saldos por moeda + total convertido |
| GET | `/api/exchange-rates/current` · `/history` | Bearer | Cotação vigente (com selo de frescor) / série diária |
| PUT / DELETE | `/api/exchange-rates/override` | Bearer | Cotação manual |
| GET / POST | `/api/expenses` | Bearer | Lista com filtros (`objectiveId, categoryId, cycleId, status, required, currency, from, to, q`) / cria |
| GET / PUT / DELETE | `/api/expenses/{id}` | Bearer | Detalhe com pagamentos / edita / exclui |
| POST | `/api/expenses/{id}/cancel` · `/restore` | Bearer | Cancela (sai dos totais) / reativa |
| GET / POST / DELETE | `/api/expenses/{id}/payments[/{paymentId}]` | Bearer | Pagamentos parciais em qualquer moeda |
| GET | `/api/expenses/summary` | Bearer | Totais e grupos convertidos (`groupBy`: CYCLE, CATEGORY, RECURRENCE, STATUS) |
| POST | `/api/expenses/preview-total` | Bearer | Ocorrências e total sem salvar |
| GET / POST / PUT / DELETE | `/api/expense-categories[/{id}]` · `/api/expense-cycles[/{id}]` | Bearer | Catálogo do usuário |
| GET / POST / PUT / DELETE | `/api/finance/categories[/{id}]` | Bearer | Categorias de receita e despesa |
| GET / POST / PUT / DELETE | `/api/finance/transactions[/{id}]` | Bearer | Lançamentos (`from, to, kind, categoryId, status`) |
| PATCH | `/api/finance/transactions/{id}/status` | Bearer | Confirma / volta para planejado |
| GET / POST / PUT / DELETE | `/api/finance/installment-plans[/{id}]` | Bearer | Parcelamentos (gera as parcelas) |
| GET / POST / PUT / DELETE | `/api/finance/recurring[/{id}]` | Bearer | Assinaturas e recorrências |
| PATCH | `/api/finance/recurring/{id}/active` | Bearer | Pausa / retoma |
| GET · PUT / DELETE | `/api/finance/saving-goals` · `/{yyyy-MM}` | Bearer | Meta de economia por mês |
| GET | `/api/finance/summary?month=` | Bearer | Mês: receitas, despesas, sobra, por categoria, meta |
| GET | `/api/finance/overview?from=&to=` | Bearer | Série mensal (até 36 meses) |
| GET | `/api/finance/commitments?months=` | Bearer | Parcelas e assinaturas a vencer |
| GET | `/api/objectives/{id}/projection` | Bearer | Quando a meta é alcançada no ritmo atual |
| GET / POST / PUT / DELETE | `/api/universities[/{id}]` | Bearer | Universidades (detalhe inclui os programas) |
| GET / POST / PUT / DELETE | `/api/programs[/{id}]` | Bearer | Programas com filtros (`chosen, gre, nc, minAdmissionChance, maxSemesterFee, q`…) |
| PATCH | `/api/programs/{id}/chosen` | Bearer | Marca como escolhido (cria a candidatura) / desmarca (arquiva) |
| GET | `/api/programs/compare?ids=` | Bearer | 2 a 5 programas com o melhor valor de cada critério |
| GET / POST / PUT / DELETE | `/api/applications[/{id}]` | Bearer | Candidaturas |
| PATCH | `/api/applications/{id}/status` | Bearer | Muda o status (grava histórico) |
| GET | `/api/applications/{id}/history` | Bearer | Histórico de status |
| GET / POST / PUT / DELETE | `/api/applications/{id}/requests[/{requestId}]` | Bearer | Solicitações da universidade |
| GET / POST | `/api/universities/{id}/images` | Bearer | Fotos (multipart, JPEG/PNG/WebP até 5 MB; a 1ª vira capa) |
| GET | `/api/universities/{id}/images/{imageId}/content` | Bearer | Bytes da foto (cache privado e imutável) |
| PATCH / DELETE | `/api/universities/{id}/images/{imageId}[/cover]` | Bearer | Legenda, capa, exclusão |
| GET / POST / PUT / DELETE | `/api/tasks[/{id}]` | Bearer | Tarefas (`status, priority, dueBefore, objectiveId`) |
| PATCH | `/api/tasks/{id}/status` | Bearer | Muda o status (concluir registra a data) |
| GET | `/api/timeline?from&to&sources` | Bearer | Itens projetados + eventos manuais (padrão: mês anterior a +12 meses) |
| POST / PUT / DELETE | `/api/timeline/events[/{id}]` | Bearer | Eventos manuais |
| PATCH | `/api/timeline/events/{id}/done` | Bearer | Marca evento como feito |
| GET | `/api/luggage/summary` | Bearer | Malas com peso e alertas, não alocados, checklist |
| GET / POST / PUT / DELETE | `/api/luggage/bags[/{id}]` | Bearer | Malas |
| GET / POST / PUT / DELETE | `/api/luggage/items[/{id}]` | Bearer | Itens (`bagId, unallocated, category, packed`) |
| PATCH | `/api/luggage/items/{id}/packed` | Bearer | Marca como embalado |
| GET | `/api/notifications?unread&page&size` · `/unread-count` | Bearer | Central de notificações / badge |
| POST | `/api/notifications/refresh` · `/read-all` | Bearer | Gera agora (idempotente) / marca todas como lidas |
| PATCH / DELETE | `/api/notifications/{id}[/read]` | Bearer | Marca como lida / exclui |
| GET / PUT | `/api/notification-preferences[/{category}]` | Bearer | Ativa/desativa e antecedências por categoria |
| GET / POST / PUT / DELETE | `/api/exchange-rate-alerts[/{id}]` | Bearer | Alertas de câmbio |
| POST | `/api/imports` | Bearer | Envia o .xlsx e devolve a prévia (nada é gravado) |
| GET / POST | `/api/imports/{id}[/commit]` | Bearer | Prévia ou resumo / importa (abas, objetivo, duplicados) |
| GET | `/api/search?q=&types=` | Bearer | Busca global (sem acento, até 6 por tipo, com a URL da tela) |
| POST | `/api/internal/jobs/{daily\|exchange}` | `X-Job-Token` | Dispara os jobs (usado pelo cron do GitHub no plano gratuito) |

Documentação interativa: `/swagger-ui.html` · contrato: `/v3/api-docs`.

## Segurança (resumo)

- Senhas com BCrypt (custo 12). Login com resposta idêntica para e-mail inexistente e senha errada (inclusive no tempo de resposta).
- **Access token** JWT HS256 de 15 min, só em memória no navegador (nunca em `localStorage`).
- **Refresh token** opaco, rotativo, guardado como hash SHA-256; cookie `HttpOnly; Secure; SameSite=Strict; Path=/api/auth`.
  Reapresentar um token já rotacionado revoga toda a família (detecção de roubo).
- Todo recurso é filtrado pelo usuário do token (`CurrentUser`); recurso de outro usuário responde 404.
- Erros em RFC 9457 com `traceId` (também no header `X-Request-Id`); stack traces nunca são expostos.
- Logs em JSON (ECS) em produção; corpo de requisição, senhas e tokens não são logados.

## Regras de negócio

Todas no backend; o frontend só formata. Detalhes no [plano técnico, §2 e §4](PLANO_TECNICO_v2.md).

**Dinheiro e câmbio**
- Valores guardados na moeda **original** (`numeric(19,4)`); somar moedas diferentes sem conversão é erro de programação.
- Um cálculo inteiro usa sempre a **mesma** cotação (`CurrencyConverter`); a resposta informa qual (fonte, data, se é manual ou está desatualizada).
- Cotação: Frankfurter (BCE), com a AwesomeAPI como reserva, a cada 6h. Se os dois falharem, mantém a última e marca **desatualizada** depois de 12h. Sem nenhuma cotação gravada, usa a reserva configurada (€ 1 = R$ 6, como a planilha). A **cotação manual** do usuário prevalece até ser removida.

**Gastos**
- `total = preço unitário × quantidade × ocorrências`. Ocorrências sugeridas pelo horizonte do objetivo (24 meses: mensal 24, semestral 4, anual 2; períodos parciais arredondam para cima) e sempre gravadas. Único e por unidade têm 1 ocorrência; personalizado exige o valor.
- Pagamentos em qualquer moeda; a taxa para a moeda do gasto é **capturada no lançamento**. Status derivado: pendente, parcial ou pago (pago ≥ total); cancelado é manual e sai de todos os totais.
- Restante = Σ max(total − pago, 0) por gasto. Custo mensal recorrente = Σ preço × quantidade dos gastos mensais.

**Meta e progresso**
- Necessidade planejada = gastos no escopo (só obrigatórios, por padrão) + **ajustes manuais** com sinal, aplicados uma única vez (ex.: Sperrkonto −R$ 75.000).
- Meta = valor fixo (MANUAL) ou max(necessidade, 0) (DERIVED). Mobilizado = já pago + saldo das contas vinculadas ao objetivo.
- Faltam = max(meta − mobilizado, 0). Meses restantes = do mês atual ao mês da data alvo, inclusive. Economia mensal necessária = faltam ÷ meses restantes.

**Controle financeiro** ([ADR 0008](docs/adr/0008-lancamentos-materializados-e-projecao.md))
- Parcelas e ocorrências de recorrências são lançamentos `PLANNED`/`CONFIRMED`. Recorrências materializadas até 36 meses à frente (job diário); assinaturas com confirmação automática viram `CONFIRMED` no vencimento.
- Parcelamento: centavos na última parcela; editar mantém as confirmadas e redistribui o restante. Editar ou pausar uma recorrência só muda as ocorrências planejadas.
- Lançamento manual com data até hoje nasce confirmado; futuro, planejado.
- Projeção: base = média do líquido avulso confirmado dos últimos 3 meses com dados; cada mês futuro soma base + receitas planejadas − despesas planejadas; informa o mês em que mobilizado + acumulado ≥ meta.

**Universidades e candidaturas** ([ADR 0009](docs/adr/0009-universidades-e-candidaturas.md))
- Prazos são regras anuais (dia/mês de abertura e prazo + mês de início das aulas); a próxima janela é recalculada a cada ano, com estado não aberta / aberta / encerrada.
- Marcar "Escolhida" cria (ou reativa) a candidatura do próximo ciclo; desmarcar arquiva. Cada mudança de status entra no histórico; aceito/recusado registram a data da resposta.
- Solicitação pendente com a candidatura em análise → "item solicitado"; resolvida a última → volta para "documentos em análise".
- Estimativa de resposta: mín ≤ provável ≤ máx. Comparador: 2 a 5 programas, valores em EUR com a mesma cotação, empates sem destaque.
- Fotos: tipo detectado pelos bytes (nunca pelo nome), até 5 MB e 12 por universidade; o navegador reduz para 1600 px em WebP antes de enviar.

**Timeline, tarefas e mala** ([ADR 0010](docs/adr/0010-timeline-projetada-mala-e-imagens.md))
- Itens automáticos são projetados na leitura (ids determinísticos, nunca dessincronizados); só eventos manuais são gravados. Status: feito, pendente ou atrasado.
- Entram só itens do planejamento: vencimentos de gastos com data (uma ocorrência por período), abertura/prazo/resposta das candidaturas ativas, solicitações e tarefas com prazo, além dos eventos manuais. Finanças (parcelas, assinaturas, lançamentos) ficam fora.
- Dashboard: atrasados dos últimos 30 dias + pendentes dos próximos 60.
- Mala: peso = Σ quantidade × peso unitário (gramas); aviso acima de 90% e alerta acima de 100%, decididos pelos gramas exatos; itens sem peso são sinalizados.

**Ajustes**
- Duas metas pelos gastos lado a lado: obrigatória (só obrigatórios) e completa (obrigatórios + opcionais), ambas com os ajustes manuais.
- Parcelamento em andamento: as parcelas pagas antes do app contam como pagas e não viram lançamentos; o vencimento informado é o da próxima.

**Notificações e importação** ([ADR 0011](docs/adr/0011-notificacoes-e-importacao.md))
- Cada aviso tem chave de deduplicação; o banco recusa repetidos (`ON CONFLICT DO NOTHING`). Antecedências por categoria (padrão: prazos 30/7/1, pagamentos 7/1, respostas 7/0, tarefas 1/0); vale a menor antecedência que contém a data.
- Alerta de câmbio: avisa uma vez quando a cotação de mercado cruza a meta e só rearma quando ela se afasta 0,5%.
- Marcos do objetivo (25/50/75/90/100%): só o maior atingido, uma vez cada, no máximo um por semana.
- Importação: prévia guardada por 2 h; importar é uma transação com os mesmos serviços das telas; duplicados (pelo nome) são pulados ou atualizados; a mesma sessão não importa duas vezes.

## CI/CD e deploy

- `ci.yml` (push/PR, e reutilizado pelo deploy): backend `mvnw verify` com Testcontainers; verificação do `openapi.json`; frontend `gen:api` (verifica tipos), `lint`, `typecheck`, `test`, `build`; **E2E** com Playwright + axe na imagem de produção.
- `deploy.yml` (tags `v*` ou manual): CI completo → Deploy Hook do Render → espera a versão nova em `/actuator/info` → *smoke test*.
- `scheduled-jobs.yml`: acorda o serviço gratuito e dispara os jobs diários (08:00) e as cotações (a cada 6 h).
- `docker.yml` (push na `main` e tags `v*`): build e publicação das imagens `-backend` e `-frontend` no GitHub Container Registry.
- Dependabot semanal para Maven e npm; mensal para Actions e imagens Docker.
- Passo a passo do deploy gratuito (Render + Neon), limites e problemas comuns: [`docs/DEPLOY.md`](docs/DEPLOY.md).

## Decisões de arquitetura

Ver [`docs/adr/`](docs/adr). Histórico de mudanças em [`CHANGELOG.md`](CHANGELOG.md).
