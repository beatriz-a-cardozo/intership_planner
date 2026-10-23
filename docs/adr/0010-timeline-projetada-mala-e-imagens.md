# ADR 0010 — Timeline projetada, tarefas, mala, imagens e metas por escopo

**Status:** aceita · 2026-10-22

## Contexto
A Fase 6 junta numa linha do tempo os prazos de quase todos os módulos; a Fase 7 controla o peso das malas.
Na mesma entrega vieram três pedidos: fotos das universidades, ver a meta obrigatória e a completa ao mesmo
tempo e cadastrar um parcelamento que já está em andamento.

## Decisão
- **Timeline projetada, não copiada.** Só os eventos manuais ficam em tabela (`timeline_event`). Os demais itens
  são calculados na leitura por um `TimelineProjector` por módulo (vencimentos de gastos, candidaturas e
  solicitações, tarefas). Cada um faz no máximo uma consulta por origem, filtrada pelo período, e devolve ids
  determinísticos (`expense:{id}:due:2`, `application:{id}:deadline`). O
  `TimelineService` junta, remove duplicados pelo id e ordena por data, tipo e título. Período máximo: 3 anos.
- **Status calculado:** feito, pendente ou atrasado (passou da data sem ser feito). A resposta esperada só fica
  atrasada depois da data máxima da faixa.
- **Só o planejamento entra** (revisado na mesma versão): gastos que têm data, períodos das universidades
  (abertura, prazo, resposta, solicitações) e tarefas, além dos eventos manuais. Finanças (parcelas, assinaturas,
  lançamentos), pagamentos já feitos e a data alvo do objetivo ficam fora da linha do tempo. Os vencimentos de
  parcelas continuam avisados pelas notificações (ADR 0011). Candidaturas arquivadas também ficam de fora.
- **Dashboard:** "próximos" = atrasados dos últimos 30 dias + pendentes dos próximos 60, atrasados primeiro.
- **Tarefas** com vínculos opcionais (objetivo, universidade, gasto, candidatura) por FK composta
  `ON DELETE SET NULL (coluna)`: excluir a origem só desvincula. `completed_at` existe se e só se o status é
  DONE (CHECK no banco).
- **Mala em gramas inteiros.** O `LuggageWeightCalculator` soma quantidade × peso unitário. Itens sem peso são
  contados à parte, porque o total pode estar subestimado. O alerta é decidido pelos gramas exatos, não pelo
  percentual arredondado: 9.001 g de 10 kg dá 90,0% arredondado, mas já passou de 90% e gera aviso.
  Excluir uma mala devolve os itens para "não alocados".
- **Imagens no banco**, em duas tabelas: metadados e bytes. Assim listar nunca carrega os bytes, e um
  PostgreSQL gratuito (Fase 10) serve sem storage externo.
  - O tipo é detectado pelos primeiros bytes do arquivo (JPEG, PNG ou WebP), nunca pelo nome ou pelo
    Content-Type enviado. Limites: 5 MB por imagem e 12 por universidade.
  - O navegador reduz a foto para 1600 px em WebP antes de enviar (tipicamente ~300 KB).
  - A imagem é servida autenticada, com cache privado e imutável (o id muda se a imagem mudar).
  - `university.cover_image_id` evita uma consulta por card.
- **Duas metas por escopo:** o progresso devolve `scopeGoals`, a meta derivada só com os obrigatórios e a com
  obrigatórios + opcionais. As duas usam a mesma calculadora e os mesmos ajustes manuais. A meta principal
  continua sendo a do escopo configurado (ou o valor fixo).
- **Parcelamento em andamento:** `paid_before_count` guarda as parcelas pagas antes de entrar no app. Elas não
  viram lançamentos (não reescrevem meses passados), só contam como pagas. O vencimento informado é o da
  próxima parcela, e as seguintes partem dele. Uma parcela já confirmada no app não pode passar a "paga antes".

## Consequências
- Mudar um gasto, uma candidatura ou uma tarefa muda a timeline na hora, sem sincronização.
- Sobrescritas de itens automáticos (ocultar, mudar prioridade) ficam para depois. O plano previa
  `timeline_event_override`, que ainda não existe.
- Imagens grandes pesam no banco. Se o volume crescer, as tabelas separadas permitem mover os bytes para um
  storage de objetos sem mudar a API.
