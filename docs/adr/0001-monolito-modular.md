# ADR 0001 — Monólito modular por domínio

**Status:** aceita · 2026-10-21

## Contexto
Produto de um time pequeno (uma pessoa), com muitos domínios relacionados (objetivo, gastos, finanças, câmbio,
universidades, timeline, notificações) que compartilham o mesmo banco e transações.

## Decisão
Um único deployable Spring Boot organizado em **package-by-feature** (`auth`, `user`, `objective`, `expense`…),
cada um com `api` (controllers/DTOs), `application` (casos de uso, calculadoras) e `domain` (entidades,
repositórios). Código transversal fica em `shared` e `config`.

## Consequências
- Deploy, testes e transações simples; refatorar entre módulos é barato.
- Fronteiras explícitas permitem extrair um módulo depois, se necessário.
- Exige disciplina: módulos conversam por services públicos, não por repositórios uns dos outros.
