# ADR 0006 — Gastos, meta derivada e ajustes manuais

**Status:** aceita · 2026-10-21

## Contexto
A planilha calculava totais com multiplicadores fixos (`Mensal = P*12`, `Por UNI = P*5`) e subtraía o Sperrkonto
escondido na fórmula (`SUM − 75000`). O mestrado tem 2 anos, e os valores precisam ser auditáveis.

## Decisão
- `total = preço unitário × quantidade × ocorrências`, com **ocorrências sempre gravadas**. Na criação, se não
  informadas, são sugeridas pelo horizonte do objetivo (`horizon_months`, padrão 24): mensal 24, semestral 4,
  anual 2, arredondando períodos parciais para cima. Único e por unidade têm 1 ocorrência (CHECK no banco);
  personalizado exige o valor. Mudar o horizonte depois **não** altera gastos existentes (são valores explícitos).
- Status derivado dos pagamentos, nunca gravado (exceto `cancelled_at`). Cancelado sai de todos os totais.
- Ajustes manuais do objetivo (`objective_adjustment`, valor com sinal e moeda) somados **uma única vez** à
  necessidade planejada e visíveis no dashboard. O plano previa `applies_to (ALL/REQUIRED)`; foi removido porque o
  único caso real (Sperrkonto) se aplica aos dois totais e o campo criaria combinações difíceis de explicar.
- "Quanto já tem" do onboarding vira uma **conta** vinculada ao objetivo (e não um campo `initial_saved_amount`):
  entra no "mobilizado" pela mesma regra dos demais saldos e pode ser atualizado depois.
- Categorias e ciclos por usuário, semeados por evento de cadastro (`UserRegisteredEvent`), sem o módulo de auth
  conhecer o de gastos. A migration V4 aplica o mesmo catálogo a quem já existia.
- Listagem e resumos de gastos carregam todos os gastos do objetivo (duas queries, sem N+1) e aplicam filtros e
  status em memória: a lista de um objetivo é pequena (dezenas), e assim o status derivado é filtrável sem
  duplicar a regra em SQL. Paginação entra se algum dia for necessária.

## Consequências
- Meta derivada sem gastos vale zero; o frontend mostra "meta vazia" em vez de "100%".
- A regressão com a planilha real (§1 do plano) é feita na Fase 9, junto com a importação.
