# ADR 0011 — Notificações deduplicadas e importação da planilha em duas etapas

**Status:** aceita · 2026-10-22

## Contexto
A Fase 8 avisa com antecedência sobre prazos, vencimentos, respostas, tarefas, câmbio e marcos do objetivo. Os
avisos não podem se repetir, mesmo com o job rodando várias vezes ou com mais de uma instância da API. A Fase 9
traz a planilha original (`Gastos.xlsx`) para o app com as correções decididas no plano (§3), sem duplicar dados
quando a pessoa importa de novo.

## Decisão — notificações
- **Um detector por categoria** (`NotificationDetector`): prazos (inscrição e solicitações), pagamentos (vencimentos
  de gastos e parcelas), respostas esperadas, tarefas e marcos do objetivo. Os detectores produzem rascunhos com
  **chave de deduplicação** estável, por exemplo `deadline:application:{id}:{data}:7d`.
- **A deduplicação é do banco:** índice único `(user_id, dedupe_key)` e insert com `ON CONFLICT DO NOTHING`.
  Rodar o job de novo, reiniciar a API ou chamar `POST /api/notifications/refresh` nunca repete um aviso.
- **Antecedências por categoria**, configuráveis (padrões: prazos 30/7/1, pagamentos 7/1, respostas 7/0,
  tarefas 1/0). Para cada data vale a menor antecedência que ainda a contém: com o prazo a 5 dias, vale a de 7.
  Se o job não rodar num dia, o aviso sai no seguinte. Datas passadas não geram aviso.
- **Gravidade:** ≤ 1 dia é crítico, ≤ 7 dias é atenção; tarefa urgente é sempre crítica.
- **Alertas de câmbio com histerese** (`ExchangeAlertEvaluator`, puro):
  - ARMED → TRIGGERED quando a condição vale (avisa uma vez).
  - Só volta a ARMED quando a cotação se afasta 0,5% da meta.
  - Usa a cotação de mercado (nunca a manual nem a de reserva).
  - É avaliado depois de cada atualização de câmbio (evento) e no job diário.
- **Marcos (25/50/75/90/100%):** só o maior marco atingido gera aviso, cada marco uma vez, no máximo uma
  mensagem motivacional por semana.
- **Job diário às 08:00** (`app.notifications.zone`, padrão America/Sao_Paulo) e na inicialização. Ao abrir o app,
  o frontend chama o refresh, então os avisos aparecem sem esperar o job.
- **ShedLock** em todos os jobs agendados, com o relógio do banco: escalar a API não duplica cotações nem avisos.
- A entrega é só in-app. Push e e-mail entrariam como novos canais, sem mudar os detectores.

## Decisão — importação
- **Duas etapas.**
  1. `POST /api/imports` lê o `.xlsx` e devolve a prévia linha a linha (válida, aviso, erro, já existe), guardada
     numa sessão (`import_session`, 2 h).
  2. `POST /api/imports/{id}/commit` aplica os rascunhos pelos **mesmos serviços das telas**, com as mesmas
     validações e regras, tudo numa transação: um erro desfaz tudo e aponta aba e linha.
- **Idempotência:** uma sessão só é importada uma vez (409 na segunda). Importar a mesma planilha de novo
  encontra o que já existe pelo nome normalizado (gasto no objetivo, programa na universidade, item na mala) e
  pula ou atualiza, conforme a escolha da pessoa.
- **Regras do plano (§3):**
  - Categorias com erro de digitação são unificadas por similaridade (Proeficiência → Proficiência).
  - Só é pago o que tem Data de Pagamento.
  - Uma fórmula vira o valor calculado, e o texto da fórmula vai para as observações.
  - "Por UNI" = 5 unidades. As ocorrências são sugeridas pelo horizonte do objetivo.
  - O ajuste "−Sperrkonto" é criado uma única vez.
  - Data já passada de gasto não pago vai para o ano seguinte (corrige a passagem e a mala extra).
  - Tipo "Sim" vira Pública; Bayreich vira Bayreuth; ranking 0 fica sem ranking; taxa "-" fica vazia.
  - Prazos e proficiência em texto livre são estruturados, e o texto original fica preservado.
  - Latitude e longitude são ignoradas.
  - Na mala, o nome da mala e a categoria são propagados para as linhas de baixo, e o nível de peso vira um peso
    estimado.
  - A aba Economia é ignorada.
- **Segurança:** só `.xlsx`, conferido pela assinatura zip; no máximo 5 MB; proporção mínima de compressão contra
  zip-bomb (POI `ZipSecureFile`).
- **Testes:** uma planilha sintética com a mesma estrutura (gerada no teste) cobre as regras. A planilha real tem
  dados pessoais e não vai para o repositório. Ela é usada por `RealSpreadsheetIT`, que só roda com
  `IMPORT_REGRESSION_FILE` e confere os valores de referência do plano: R$ 347.893,28 / R$ 272.893,28 /
  obrigatório R$ 200.793,28 / pago R$ 11.721,68 / mensal R$ 8.670.

## Consequências
- Notificações antigas ficam até a pessoa excluí-las; limpeza automática pode vir depois.
- Uma importação grande é uma transação longa. Com dezenas de linhas, isso é aceitável.
