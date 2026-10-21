# ADR 0005 — Câmbio: provedores em cascata, histórico próprio e cotação manual

**Status:** aceita · 2026-10-21

## Contexto
O progresso do objetivo soma saldos e gastos em BRL e EUR. A cotação precisa ser confiável, explicável
("cotação de 21/10 08:00") e o app não pode quebrar quando um provedor gratuito sai do ar.

## Decisão
- `ExchangeRateProvider` (interface) com **Frankfurter** (BCE, sem chave, diário) e **AwesomeAPI** (intradiário)
  tentados na ordem de `app.exchange.providers`. Provedor desconhecido na configuração falha na inicialização.
- Cada busca bem-sucedida vira uma linha em `exchange_rate` (histórico próprio). Guardamos só a direção configurada
  (EUR→BRL); o inverso é calculado. Na primeira execução, 180 dias de histórico são carregados para o gráfico.
- Cotação vigente em cache em memória (por instância), invalidado a cada gravação.
- **Falha não quebra nada:** mantém-se a última cotação; passa a ser marcada `stale` depois de `stale-after`
  (12h, o dobro do intervalo do job). Sem nenhuma cotação gravada, usa `fallback-rate` (6,0, como a planilha), sempre `stale`.
- Cotação manual por usuário (`user_exchange_rate_override`), na direção pedida; prevalece até ser removida.
- `CurrencyConverter` é imutável e montado uma vez por requisição: todos os números de uma resposta usam a mesma taxa,
  e a resposta informa qual foi usada.
- Pagamentos guardam a taxa do momento do lançamento (`rate_to_expense_currency`).

## Consequências
- O plano previa Caffeine para o cache; um mapa concorrente atende ao caso (2 moedas) sem dependência extra.
- Com várias instâncias, cada uma busca as cotações (idempotente); ShedLock entra junto com os jobs da Fase 8.
- Adicionar USD = 1 insert em `currency` + 1 par em `app.exchange.pairs`.
