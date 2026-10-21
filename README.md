# Interchange Planner

> Nome provisório. Para renomear: `APP_NAME` (backend/compose) e `VITE_APP_NAME` (frontend).

Planejamento financeiro e de intercâmbio: objetivo com meta e progresso, gastos por ciclo e categoria em BRL/EUR,
controle financeiro (receitas, despesas, parcelas, assinaturas), universidades e candidaturas, timeline, mala
e notificações. O plano técnico completo está em [`PLANO_TECNICO_v2.md`](PLANO_TECNICO_v2.md).

## Status das fases

- [x] **1. Fundação** — monorepo, Docker, Flyway V1, autenticação JWT + refresh rotativo, Problem Details, OpenAPI → tipos TS, AppShell responsivo, tema claro/escuro/sistema, login/registro, CI
- [ ] 2. Objetivos, câmbio base, contas e onboarding
- [ ] 3. Gastos
- [ ] 4. Controle Financeiro
- [ ] 5. Universidades e candidaturas
- [ ] 6. Timeline e tarefas
- [ ] 7. Mala
- [ ] 8. Notificações
- [ ] 9. Importação do Excel
- [ ] 10. Polimento e deploy

## Arquitetura

```
┌──────────────────────┐     /api (mesmo domínio)     ┌────────────────────────────┐      ┌──────────────┐
│ frontend (nginx)     │ ───────────────────────────▶ │ backend (Spring Boot)      │ ───▶ │ PostgreSQL 17│
│ React 19 + Vite SPA  │   JWT Bearer + cookie refresh │ monólito modular por domínio│      │ Flyway       │
└──────────────────────┘                               └────────────────────────────┘      └──────────────┘
```

Backend em **package-by-feature** (`com.interchangeplanner`):

| Pacote | Responsabilidade |
|---|---|
| `shared` | Entidade base (UUIDv7, auditoria, lock otimista), erros RFC 9457, `CurrentUser`, `RequestIdFilter` |
| `config` | Segurança, JWT, CORS, OpenAPI, Clock, agendamento, propriedades `app.*` validadas |
| `auth` | Registro, login, refresh rotativo com detecção de reuso, logout, bloqueio por tentativas |
| `user` | Perfil e preferências (tema, idioma, fuso, moedas) |
| `currency` | Catálogo de moedas suportadas (BRL, EUR) |

Os próximos módulos (`objective`, `expense`, `finance`, `exchange`, …) seguem a mesma estrutura `api / application / domain`.
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
```

Cobertura da Fase 1: emissão/validação de JWT (expirado, assinatura inválida), bloqueio de login, UUIDv7,
migrations e constraints no PostgreSQL real, fluxo completo de autenticação (registro, duplicidade, validação,
login, refresh rotativo, **reuso de refresh revoga a família**, logout), Problem Details, isolamento entre usuários,
cliente HTTP com refresh single-flight, formulário de cadastro e resolução de tema.

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
│   └── src/main/java/com/interchangeplanner/{shared,config,auth,user,currency}
├── frontend/                SPA React (Dockerfile + nginx)
│   └── src/{app,api,layouts,components,features,lib,styles}
├── docs/adr/                Decisões de arquitetura
├── .github/                 CI, imagens Docker, Dependabot
├── docker-compose.yml
└── .env.example
```

## Endpoints (Fase 1)

| Método | Rota | Auth | Descrição |
|---|---|---|---|
| POST | `/api/auth/register` | — | Cria conta e inicia sessão (201) |
| POST | `/api/auth/login` | — | Login; 401 genérico, 429 após 5 falhas |
| POST | `/api/auth/refresh` | cookie | Rotaciona o refresh token e devolve novo access token |
| POST | `/api/auth/logout` | cookie | Revoga a sessão (204) |
| GET | `/api/me` | Bearer | Usuário autenticado + preferências |
| GET / PUT | `/api/me/settings` | Bearer | Preferências (tema, idioma, fuso, moedas) |
| GET | `/api/currencies` | — | Moedas suportadas |

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

Os cálculos (total por periodicidade, meta, progresso, conversão, status de pagamento etc.) estão no
[plano técnico, §2 e §4](PLANO_TECNICO_v2.md) e serão documentados aqui conforme cada fase for entregue.

## CI/CD e deploy

- `ci.yml` (push/PR): backend `mvnw verify` com Testcontainers; verificação do `openapi.json`; frontend `gen:api` (verifica tipos), `lint`, `typecheck`, `test`, `build`.
- `docker.yml` (push na `main` e tags `v*`): build e publicação das imagens `-backend` e `-frontend` no GitHub Container Registry.
- Dependabot semanal para Maven e npm; mensal para Actions e imagens Docker.
- Deploy contínuo em plataforma gratuita: Fase 10.

## Decisões de arquitetura

Ver [`docs/adr/`](docs/adr). Histórico de mudanças em [`CHANGELOG.md`](CHANGELOG.md).
