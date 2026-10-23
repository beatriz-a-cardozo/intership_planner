# ADR 0009 — Universidades, janelas de inscrição e candidaturas

**Status:** aceita · 2026-10-22

## Contexto
A planilha tinha uma aba de universidades com prazos em texto ("01/Dezembro - 01/Março"), requisitos como
"Sim"/"Não"/"Se necessário" e uma coluna "Escolhida". A Fase 5 precisa de prazos que se recalculam a cada ano,
comparação lado a lado e acompanhamento de candidaturas.

## Decisão
- **Prazos como regra anual** (`program_intake`: semestre, dia/mês de abertura e de prazo, mês de início das
  aulas), não como datas. `IntakeWindowCalculator` (puro) devolve a próxima janela: se o mês do prazo é ≤ mês de
  início, o prazo cai no mesmo ano das aulas; senão, no ano anterior (inverno com prazo em julho → mesmo ano;
  verão com prazo em janeiro para abril → mesmo ano; inverno com prazo em dezembro → ano anterior). Estado
  `NOT_OPEN_YET` / `OPEN` / `CLOSED`. O texto original fica em `application_deadline_text` para conferência.
- **Requisitos tri-estado + desconhecido** (`REQUIRED`, `NOT_REQUIRED`, `CONDITIONAL`, `UNKNOWN`): "não sei" é
  diferente de "não exige" e não deve passar em filtros de "sem GRE".
- **"Escolhida" cria a candidatura.** `PATCH /api/programs/{id}/chosen` cria (ou reativa) a candidatura do próximo
  ciclo; desmarcar **arquiva** em vez de excluir, preservando histórico. Uma candidatura por
  `(programa, ano, semestre)`.
- **Histórico imutável** (`application_status_history`): cada mudança de status grava uma linha com data e nota.
  Aceito/recusado preenchem a data real da resposta.
- **Solicitações da universidade** sincronizam o status: uma pendência registrada com a candidatura em análise
  (`DOCUMENTS_UNDER_REVIEW` ou `AWAITING_RESULT`) leva a `ITEM_REQUESTED`; quando a última pendência é atendida ou
  removida, volta para `DOCUMENTS_UNDER_REVIEW`. Outros status (entrevista, resultado) não são tocados. Ambas as
  mudanças entram no histórico.
- **Estimativa de resposta** como faixa `mín ≤ provável ≤ máx` (CHECK no banco) com confiança e base textual —
  respostas de universidades alemãs variam semanas; uma data única passaria falsa precisão.
- **Comparador** de 2 a 5 programas; o backend marca o melhor valor de cada critério (ranking, taxas e custo de
  vida convertidos para EUR com a mesma cotação, chances, empregabilidade, prazo mais próximo). Linha sem
  diferença (empate total) não recebe destaque.

## Consequências
- Prazos de anos seguintes aparecem sozinhos; se a universidade mudar a data, edita-se a regra.
- Arquivar em vez de excluir mantém o histórico se o programa voltar a ser escolhido no mesmo ciclo.
- A Timeline (Fase 6) pode consumir prazos, respostas esperadas e solicitações sem novas tabelas.
