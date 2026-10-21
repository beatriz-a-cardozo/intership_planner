# ADR 0003 — OpenAPI como fonte única do contrato

**Status:** aceita · 2026-10-21

## Contexto
Frontend e backend precisam concordar em DTOs, enums e formatos de erro sem duplicar definições à mão.

## Decisão
- springdoc gera o OpenAPI a partir dos controllers/records.
- O teste `OpenApiSpecIT` exporta o contrato para `backend/openapi/openapi.json` (servidor relativo `/`,
  chaves ordenadas → saída determinística).
- O frontend gera `src/api/schema.d.ts` com `openapi-typescript` (`npm run gen:api`) e deriva seus tipos
  em `src/api/types.ts`.
- Ambos os arquivos são versionados; o CI falha se estiverem desatualizados.

## Consequências
- Mudou um DTO no backend → o `tsc` do frontend aponta tudo que precisa ajustar.
- Regras de negócio continuam só no backend; o frontend formata (ver plano, §9).
