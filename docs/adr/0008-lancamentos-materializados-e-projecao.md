# ADR 0008 — Lançamentos materializados, parcelas e projeção de economia

**Status:** aceita · 2026-10-22

## Contexto
A Fase 4 acrescenta receitas, despesas, parcelamentos e assinaturas, e precisa responder "quanto sobra por mês"
e "quando eu chego na meta". Recorrências e parcelas podiam ser calculadas na leitura (regra + datas) ou gravadas
como lançamentos.

## Decisão
- **Tudo vira lançamento** (`finance_transaction`): parcelas e ocorrências de recorrências são gravadas como
  linhas com status `PLANNED`/`CONFIRMED`. Resumo do mês, visão de 12 meses e compromissos futuros são a mesma
  consulta sobre uma tabela só; editar ou confirmar uma ocorrência específica não exige exceções na regra.
- **Recorrências materializadas** do início do mês corrente até `app.finance.materialize-months` (padrão 36) à
  frente. Um job diário (`app.finance.materialize-cron`) e a inicialização estendem a janela e confirmam as
  ocorrências vencidas das assinaturas com `auto_confirm`. `unique(recurring_entry_id, occurred_on)` torna a
  materialização idempotente. Editar ou pausar uma recorrência apaga e recria só as ocorrências **planejadas**;
  as confirmadas são histórico e não mudam.
- **Parcelas:** `InstallmentCalculator` divide o total com os centavos na última parcela, uma por mês a partir da
  primeira data. Editar o plano mantém as parcelas já confirmadas e redistribui o restante nas demais. Parcela não
  é excluída sozinha (422): exclui-se o plano.
- **Status padrão por data:** lançamento manual com data até hoje nasce `CONFIRMED`; data futura nasce `PLANNED`.
- **Projeção** (`SavingsProjectionCalculator`, puro): a partir do mês seguinte, soma por mês
  `base + receitas planejadas − despesas planejadas`, onde a base é a média do líquido **avulso confirmado** dos
  últimos 3 meses com dados (sem recorrências e parcelas, que já entram como planejadas — senão contariam duas
  vezes). Informa o mês em que o acumulado + mobilizado alcança a meta, se isso acontece até a data alvo e o
  déficit na data alvo. Horizonte de 36 meses (o mesmo da materialização).
- Composite FKs `(id, user_id)` com `ON DELETE SET NULL (coluna)`: excluir uma conta ou categoria desvincula os
  lançamentos sem apagar histórico e sem permitir vínculo entre usuários.

## Consequências
- A tabela cresce de forma previsível (≈ 36 linhas por assinatura ativa); pausar remove as futuras.
- Uma recorrência criada hoje aparece imediatamente em todos os resumos, sem esperar o job.
- A projeção é tão boa quanto os lançamentos: sem histórico confirmado, a base é zero e o texto da tela diz isso.
