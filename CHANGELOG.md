# Changelog

Formato baseado em [Keep a Changelog](https://keepachangelog.com/pt-BR/1.1.0/). Versões por fase do plano.

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
