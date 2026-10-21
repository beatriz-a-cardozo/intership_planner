# Changelog

Formato baseado em [Keep a Changelog](https://keepachangelog.com/pt-BR/1.1.0/). Versões por fase do plano.

## [0.3.0] — Fases 2 e 3: Objetivos, câmbio, contas e Gastos — 2026-10-21

### Adicionado
- Migrations `V2__exchange_rates`, `V3__objectives_accounts`, `V4__expenses` (com FKs compostas `(id, user_id)`: o banco impede vincular dados de usuários diferentes) e catálogo padrão para usuários existentes.
- **Câmbio:** provedores Frankfurter (BCE) e AwesomeAPI em ordem configurável, histórico de 180 dias na primeira execução, job a cada 6h, cache em memória, selo "desatualizada", cotação de reserva, cotação manual por usuário; `GET /api/exchange-rates/current|history`, `PUT|DELETE /api/exchange-rates/override`.
- **Objetivos:** CRUD, objetivo principal único (índice parcial), horizonte em meses, escopo da meta, `ObjectiveProgressCalculator` (meta, mobilizado, faltam, meses restantes, economia mensal necessária, no ritmo) e ajustes manuais da meta.
- **Contas:** contas por moeda vinculáveis a um objetivo, histórico de saldos (um por dia), resumo por moeda + total convertido.
- **Onboarding** em 6 passos (o valor já guardado vira a conta "Reserva do intercâmbio") e **dashboard** agregado numa chamada.
- **Gastos:** categorias e ciclos por usuário (semeados no cadastro com os nomes da planilha + Viagem e Transporte), periodicidade com ocorrências sugeridas pelo horizonte, pagamentos parciais em qualquer moeda com taxa capturada, cancelar/reativar, filtros, resumo por ciclo/categoria/periodicidade/status, `preview-total`.
- Frontend: dashboard com anel de progresso, saldos, cotação com histórico; telas de objetivos, finanças (contas), gastos (gráficos, filtros, detalhe com pagamentos) e configurações (cotação manual, categorias e ciclos); tela de erro de rota.

### Corrigido
- **403 em requisições do navegador via nginx/Vite** (vinha da Fase 1): o proxy enviava `Host` sem a porta e o Spring tratava a chamada como CORS. Agora `Host` é preservado (`$http_host`; `changeOrigin: false` no Vite).
- `<Button asChild>` quebrava a tela (o Slot do Radix exige um único filho).
- Contrato OpenAPI: campos de respostas agora obrigatórios e anuláveis de forma explícita (`X | null` no TypeScript).

## [0.1.0] — Fase 1: Fundação — 2026-10-21

### Adicionado
- Monorepo `backend/` (Spring Boot 4.1.1, Java 21) e `frontend/` (React 19 + Vite 8), `docker-compose.yml` com PostgreSQL 17, API e frontend (nginx), `.env.example`.
- Migration `V1__baseline_users_auth_currency.sql`: `app_user`, `user_settings`, `refresh_token`, `currency` (BRL, EUR).
- Autenticação: registro, login, refresh token opaco rotativo com detecção de reuso por família, logout, bloqueio após 5 falhas de login (429 + `Retry-After`), limpeza diária de tokens expirados.
- `GET /api/me`, `GET|PUT /api/me/settings`, `GET /api/currencies`.
- Erros em RFC 9457 Problem Details com `traceId` e `errors[]` por campo; `X-Request-Id` em todas as respostas.
- OpenAPI (springdoc) exportado para `backend/openapi/openapi.json` e tipos TypeScript gerados (`npm run gen:api`).
- Frontend: AppShell responsivo (sidebar no desktop, bottom nav + "Mais" no mobile), tema claro/escuro/sistema persistido na conta, tokens de design, login, cadastro, configurações, páginas de módulos futuros.
- Testes: unitários (JWT, bloqueio de login, UUIDv7), integração com Testcontainers (migrations, fluxo de auth, isolamento entre usuários, OpenAPI), Vitest (cliente HTTP, cadastro, tema).
- CI no GitHub Actions, publicação de imagens no GHCR, Dependabot, README e ADRs.
