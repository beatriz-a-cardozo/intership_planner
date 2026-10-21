# ADR 0004 — UUIDv7 gerado na aplicação e convenções de schema

**Status:** aceita · 2026-10-21

## Decisão
- PK `uuid` versão 7 (RFC 9562) gerada na construção da entidade: id existe antes do `persist`
  (equals/hashCode estáveis) e é ordenável no tempo (índices B-tree compactos).
- `created_at`/`updated_at` `timestamptz` (UTC), `version bigint` para lock otimista (409 em conflito).
- Enums como `varchar` + `CHECK`, evoluíveis sem `ALTER TYPE`.
- Dinheiro `numeric(19,4)` + `char(3)` de moeda; arredondamento só na apresentação.
- Schema exclusivamente via Flyway; Hibernate em `validate`.

## Consequências
- Ids não revelam contagem de registros e não exigem ida ao banco para serem criados.
- Constraints do banco (ex.: e-mail normalizado, senha obrigatória em conta local) são a última linha de defesa e são testadas.
