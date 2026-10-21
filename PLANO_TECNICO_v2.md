# Interchange Planner — Plano Técnico (v2, aprovado em 21/10/2026)

> Nome provisório. O nome vive em uma única configuração (`APP_NAME` no backend, `VITE_APP_NAME` no frontend) e em um arquivo de tokens de marca, então trocar depois é uma alteração de 3 linhas.

Sumário
1. Análise da planilha `Gastos.xlsx`
2. Fórmulas → regras de negócio
3. Problemas de dados encontrados (impactam a importação)
4. Decisões de domínio (com justificativa)
5. Modelo de entidades e relacionamentos
6. Esquema do banco (PostgreSQL + Flyway)
7. Arquitetura do backend
8. API REST
9. Arquitetura do frontend, páginas e componentes
10. Identidade visual
11. Segurança, observabilidade, testes
12. Decisões e ambiguidades
13. Plano de implementação por fases
14. Fluxo de entrega, README, CI/CD e deploy

---

## 1. Análise da planilha

| Aba | O que é de fato | Estrutura |
|---|---|---|
| **DASHBOARD** | Indicadores + 3 gráficos (por ciclo, por categoria, por tipo) + gráfico de status | `Total Obrigatório`, `Total não obrigatório`, `Pago`, `Pendente`; saldos guardados (R$ 1.777,26 e € 855,58); conversão € × 6 fixa |
| **Informações dos Gastos** | Tabela `Tabela1`, 53 gastos | Gasto, Categoria, Ciclo, Preço Unit. (R$), Preço Total (R$), Preço Unit. (€), Preço Total (€), Tipo, Obrigatório?, Data, Detalhes, Status, Data de Pagamento, Valor Pago, + coluna O sem cabeçalho com observações |
| **Análise de Universidades** | Tabela `Tabela3`, 20 linhas = **20 programas** em 19 universidades (TUM aparece 2×) | 31 colunas (instituição + localização + programa + requisitos + prazos + custos + bolsa + chances) |
| **Mala** | Tabela `Tabela4`, 17 itens (todos documentos, todos na mala de mão) | Mala, Qnt., Item, Categoria, Nível de peso (“Leve”) — sem peso numérico |
| **Economia** | Plano de economia mensal NOV→SET, total R$ 21.000 | Só mês (sem ano) e valor |
| **Auxiliares** | Listas de domínio + agregações que alimentam os gráficos | Ciclos (4), Categorias (12), Tipos (5) com `SUMIF/SUMIFS` |

> A planilha é **apenas a base inicial**: tudo que for importado (gastos, universidades, itens de mala, categorias) é editável no app, e novos registros podem ser criados livremente.

**Valores de referência** (teste de regressão da importação), já com o horizonte de **2 anos de mestrado** (mensal ×24, semestral ×4, anual ×2, Sperrkonto descontado uma única vez):
total geral R$ 347.893,28 → com ajuste do Sperrkonto **R$ 272.893,28** · obrigatório R$ 275.793,28 → **R$ 200.793,28** · pago R$ 11.721,68 · custo mensal recorrente ≈ R$ 8.670/mês.
(Na planilha original, com 12 meses: R$ 162.593,28 e R$ 114.393,28.)

## 2. Fórmulas → regras de negócio

| Fórmula da planilha | Problema | Regra no sistema |
|---|---|---|
| `Preço Total = IF(Tipo="Único",P, IF("Por UNI",P*5, IF("Semestral",P*2, IF("Anual",P*1, IF("Mensal",P*12,0)))))` | Multiplicadores fixos (5 universidades, 12 meses, 2 semestres) e tipo desconhecido vira 0 | `total = preço_unitário × quantidade × ocorrências`. `ocorrências` é **armazenado** no gasto; quando não informado, o backend sugere a partir do **horizonte do objetivo** (mestrado = 24 meses → Mensal=24, Semestral=4, Anual=2), e você pode alterar em cada gasto. Periodicidade desconhecida = erro de validação, nunca 0 |
| `Preço Unit. (€) = R$/6` | Câmbio fixo, e o valor em € é derivado do R$ (o dado original se perde) | Gasto guarda **moeda original + valor original**. Equivalente na outra moeda é calculado na leitura com a cotação vigente (com timestamp) |
| `Total Obrigatório = SUMIF(Obrigatório="Sim") − 75000` e `Total = SUM − 75000` | Subtração manual do Sperrkonto escondida na fórmula | **Mantido como na planilha (decisão sua)**: todos os gastos somam e o objetivo tem **ajustes manuais** (`objective_adjustment`: rótulo, valor, moeda) subtraídos dos totais — ex. “Sperrkonto: −R$ 75.000”, aplicado **uma única vez**, independente do horizonte. Fica explícito e visível no dashboard, em vez de escondido numa fórmula. Ver §4.3 |
| `Pago = SUMIF(Status="Pago", Valor Pago)` | “Pago” sem valor (Passaporte) soma 0; pagamento diferente do previsto (R$ 219,80 → R$ 224) | Pagamentos são registros próprios (`expense_payment`); o status é **derivado** de Σ pagamentos vs total (Pendente / Parcial / Pago); Cancelado é manual |
| `Pendente = Obrigatório − Pago` | Mistura escopos (pago inclui não obrigatórios) | `restante = Σ max(total − pago, 0)` calculado sobre o mesmo escopo de filtro |
| `SUMIF por Ciclo / Categoria / Tipo` (Auxiliares) | — | Endpoint de agregação `GET /api/expenses/summary?groupBy=cycle|category|recurrence` (uma query `GROUP BY`) |
| `Mensal: SUMIF(...)/12` | — | Indicador “custo mensal recorrente” no resumo |
| `E69 = euro × 6` | Câmbio fixo | Saldos convertidos com cotação real |
| `Economia: SUM(C2:C12)` | Aba ignorada | Substituída pelo módulo de **Controle Financeiro** (§4.6) |

## 3. Tratamento dos dados na importação (decidido)

- **Erros de digitação em categorias** são corrigidos e unificados automaticamente: `Proeficiência` → **Proficiência**, `Candidaturas` → **Candidatura** (normalização por similaridade; o preview mostra o que foi unificado).
- **Categorias novas**: **Viagem** (ida para a Alemanha e itens necessários para ela) e **Transporte** (locomoção dentro do país: bicicleta, Deutschlandticket) entram como categorias padrão.
- **Status**: só é pago o gasto com **Data de Pagamento** (gera um pagamento com o Valor Pago). `Não`, `Sim` e `Pago` sem data entram como **Pendente**.
- **Datas**: Passagem aérea e Mala extra em 15/05/**2026** são corrigidas para 15/05/**2027**.
- **Valores com fórmula** (`=314*0.7`) → valor calculado; a fórmula vai para a observação.
- **Coluna O sem cabeçalho** (“Obs. Parcelado em 10x”, “Obs. $227,85 USD”) → observações.
- **Universidades**: **latitude/longitude ignoradas** na importação (campos continuam no modelo, vazios e editáveis, para o mapa futuro); Tipo = `Sim` (TU Köln) → Pública com aviso; `Bayreich` → Bayreuth; taxa `-` → vazio; linha 22 (só URL) descartada; ranking `0` → sem ranking.
- **Prazos em texto livre** (`01/Dezembro - 01/Março`, `x - 15/Novembro`, `30/Abril-Junho`) → janela abertura/fechamento dia/mês; o que não parsear fica como texto + aviso.
- **Proficiência em texto livre** (`B2 (TOEFL >= 90)`, `B2 (TOELF >= 88) + Alemão A1`, `B2/A2`) → requisitos estruturados; texto original preservado.
- **Requisitos tri-estado**: `Se necessário (GRE)` → `CONDITIONAL`.
- **Aba Economia**: ignorada.
- **Mala**: nome da mala propagado para as linhas de baixo; “Nível de peso” vira peso estimado opcional. Itens, pesos e malas editáveis no app.

## 4. Decisões de domínio

### 4.1 Multiusuário e escopo
- Todo dado pertence a um `app_user`. Entidades “raiz” têm `user_id` direto (mesmo quando também têm `objective_id`) para que **toda** query de repositório filtre por dono (`findByIdAndUserId`). Recurso de outro usuário retorna **404** (não 403) para não vazar existência.
- **Universidades e programas pertencem ao usuário** (não há catálogo global ainda). O schema prevê `visibility` futura para catálogo compartilhado sem migração destrutiva.
- Um objetivo pode ser marcado como **principal** (`is_primary`, índice único parcial por usuário).

### 4.2 Dinheiro e moedas
- Value object `Money(amount: BigDecimal, currency: CurrencyCode)`; colunas `NUMERIC(19,4)` + `CHAR(3)`. Escala 2 apenas na apresentação; `RoundingMode.HALF_EVEN`.
- Moedas suportadas numa tabela `currency` (BRL, EUR inicialmente; adicionar USD = 1 insert). Soma de valores de moedas diferentes **só** acontece via `CurrencyConverter`, nunca diretamente — o `Money.plus()` lança exceção se as moedas divergirem.
- Toda resposta monetária agregada informa `{ amount, currency, rateUsed, rateTimestamp, rateSource }` para o frontend mostrar “≈ R$ 3.100 · cotação de 21/10 08:00”.

### 4.3 Meta do objetivo e progresso (núcleo financeiro)
Definições (todas no backend, `ObjectiveProgressCalculator`):
- **Necessidade planejada** = Σ total dos gastos não cancelados no escopo do objetivo (escopo configurável: só obrigatórios ou todos) − Σ **ajustes manuais** do objetivo (convertidos). Equivale ao `−75000` da planilha, mas com rótulo e visível. Gastos podem ser marcados como `refundable` (ex. Kaution) apenas como informação.
- **Meta** = valor manual (`target_amount`) **ou** “derivada dos gastos” (`target_mode = MANUAL | DERIVED`).
- **Mobilizado** = total já pago (convertido) + saldo disponível nas contas vinculadas ao objetivo (convertido).
- **Progresso** = mobilizado / meta (limitado a 100% na barra, valor real exposto).
- **Faltam** = max(meta − mobilizado, 0).
- **Economia mensal necessária** = faltam / meses restantes até `target_date` (mês corrente conta se ainda não fechado).
- **Previsão de atingimento**: projeta mês a mês usando o Controle Financeiro — média das sobras realizadas dos últimos meses, descontando parcelas e assinaturas já comprometidas nos meses futuros; retorna o mês previsto ou “não atinge até a data alvo, déficit de R$ X”.

### 4.4 Gastos
- `recurrence`: `ONE_TIME | MONTHLY | SEMIANNUAL | ANNUAL | PER_UNIT | CUSTOM`. `occurrences` sempre explícito no banco. `PER_UNIT` usa `quantity` (ex.: 5 inscrições) e `occurrences = 1`.
- Datas: `due_date` (primeira/única ocorrência) e opcional `end_date` para recorrentes (permite gerar parcelas na timeline).
- Pagamentos: `expense_payment` com moeda própria + taxa capturada no momento (TOEFL pago em USD; agência parcelada em 10×). `valor pago` = Σ pagamentos convertidos para a moeda do gasto com a taxa **do pagamento** (histórico não muda quando a cotação muda).
- Status: `PENDING / PARTIALLY_PAID / PAID` derivados; `CANCELLED` manual (persistido como flag `cancelled_at`).
- Categoria e ciclo: tabelas por usuário com seeds padrão (os 4 ciclos e as categorias da planilha + Viagem, Transporte), cor e ícone; tudo editável, novos itens livres.

### 4.5 Contas / saldos
- `account` (nome, moeda, tipo: corrente, poupança, investimento, espécie, conta bloqueada, outro; objetivo opcional) + `account_balance_snapshot` (saldo em data). Saldo atual = último snapshot. O histórico alimenta o gráfico de evolução do patrimônio. BRL e EUR nunca são misturados: totais convertidos são apenas **visões**.

### 4.6 Controle Financeiro (substitui a aba “Economia”)
Módulo de finanças pessoais: quanto você recebe, quanto gasta, parcelas e assinaturas, e quanto sobra para o objetivo.
- `finance_category` (por usuário, tipo `INCOME | EXPENSE`, cor/ícone; seeds: Salário, Freelance, Moradia, Alimentação, Transporte, Saúde, Lazer, Assinaturas, Educação, Outros).
- `transaction`: data, tipo (receita/despesa), valor + moeda, categoria, descrição, conta opcional, status `PLANNED | CONFIRMED`, vínculo opcional a parcelamento ou recorrência.
- `installment_plan` (**parcelas**): descrição, valor total **ou** valor da parcela, nº de parcelas, data da 1ª, categoria, forma de pagamento. O backend gera as parcelas como transações `PLANNED` (com ajuste de centavos na última); editar o plano regenera só as parcelas futuras ainda não confirmadas. Mostra “parcela 3/10”.
- `recurring_entry` (**assinaturas e recorrências**): nome, valor + moeda, tipo receita/despesa, frequência (semanal, mensal, anual, personalizada em meses), dia de cobrança, início, fim opcional, ativa/pausada, confirmação automática ou manual. Serve para assinaturas (Spotify, iCloud), contas fixas e salário.
- `monthly_saving_goal`: meta de economia por mês (opcional).
- **Visão mensal (calculada no backend)**: receitas, despesas (avulsas + parcelas + assinaturas), **sobra = receitas − despesas**, meta de economia × realizado, e **comprometido futuro** (parcelas e assinaturas dos próximos meses).
- Gráficos: receitas × despesas por mês, despesas por categoria, evolução da sobra acumulada, linha do tempo de parcelas a vencer.
- Integração: a sobra mensal alimenta a projeção do objetivo (§4.3); parcelas e renovações de assinatura geram eventos na timeline e notificações.

### 4.7 Universidades / programas / candidaturas
- `university` (dados da instituição) 1—N `program`.
- Escalas ordinais como enums: `Rating5 = LOW, LOW_MEDIUM, MEDIUM, MEDIUM_HIGH, HIGH`; empregabilidade ganha `EXCELLENT`.
- Requisitos (GRE, aptitude assessment, entrevista, NC) como `Requirement = REQUIRED | NOT_REQUIRED | CONDITIONAL | UNKNOWN`.
- Canal de inscrição: `DIRECT | UNI_ASSIST | VPD | PORTAL` + `portal_name` (C@MPUS, unisono, Augsburg). Envio de documentos: `ONLINE | PHYSICAL | BOTH`.
- `program_language_requirement` (N por programa): idioma, CEFR mínimo, teste, nota mínima, texto original.
- `program_intake` (N por programa): semestre (`WINTER | SUMMER | OTHER`), janela de inscrição recorrente (`open_month/day`, `close_month/day`), mês de início. A **candidatura** é que tem datas concretas de um ciclo específico.
- **Escolhida**: cada programa tem a opção **“Escolhida”**. Marcar cria a candidatura (`application`) daquele programa com status inicial *Aguardando abertura das inscrições*; desmarcar arquiva a candidatura (não apaga o histórico). Na lista/tabela de universidades aparecem o selo “Escolhida” e o status da candidatura, com filtro por ambos.
- **Status da candidatura** (ordenados): Aguardando abertura das inscrições · Documentos em análise · Solicitado exame · Solicitado entrevista · **Solicitado item** · Lista de espera · Aguardando resultado · Aprovado · Não aceito · Matriculada. Mudanças de status ficam em `application_status_history` (data + observação).
- **Solicitações da universidade** (`application_request`): qualquer coisa que a universidade peça durante o processo — documento, exame, entrevista, carta de motivação, recomendação, teste, pagamento ou outro — com descrição, data do pedido, prazo, data de atendimento e status (pendente/atendida). O status “Solicitado item” resume que há solicitações pendentes; os prazos delas entram na timeline e nas notificações.
- O **resultado** é derivado do status (Aprovado/Matriculada → aceita; Não aceito → rejeitada; Lista de espera → lista de espera; demais → pendente), evitando estados contraditórios.
- Estimativa de resposta: `response_min_date`, `response_likely_date`, `response_max_date`, `response_confidence (LOW|MEDIUM|HIGH)`, `response_estimate_basis` (texto). Validação: min ≤ provável ≤ máx. A UI sempre rotula como “Estimativa baseada nos seus dados”.
- Latitude/longitude `NUMERIC(9,6)` opcionais com CHECK de faixa (prepara o mapa futuro; não são importadas da planilha).

### 4.8 Timeline
- **Eventos manuais** ficam em `timeline_event`.
- **Eventos automáticos são projeções** calculadas na leitura a partir das entidades de origem (gastos, pagamentos, candidaturas, intakes, tarefas, objetivo), com `sourceType + sourceId` e id determinístico (`expense:{uuid}:due`). Não são copiados para uma tabela — assim nunca ficam dessincronizados quando a origem muda ou é apagada.
- `TimelineProjector` (uma implementação por módulo, interface comum) + `TimelineService` que faz o merge ordenado por data dentro do intervalo pedido. Cada projetor faz **uma** query indexada por data → sem N+1.
- Sobrescritas do usuário sobre eventos automáticos (ocultar, mudar prioridade) em `timeline_event_override` (fase posterior, schema já previsto).

### 4.9 Notificações
- `Notification` (in-app), `NotificationPreference` (por categoria: ativo, antecedências em dias, ex. `{30,7,1}`), `ExchangeRateAlert`, `NotificationRule` (regras genéricas futuras).
- Geração por **detectores** (`NotificationDetector`: prazos, pagamentos, compras, respostas esperadas, câmbio, financeiro, motivacional) rodando em job diário 08:00 (`America/Sao_Paulo`, configurável por usuário) e no job de câmbio.
- **Deduplicação** por `dedupe_key` com índice único `(user_id, dedupe_key)`, ex.: `deadline:{applicationId}:7d`. Insert com `ON CONFLICT DO NOTHING`.
- **Alerta de câmbio com histerese**: estados `ARMED → TRIGGERED`; só volta a `ARMED` quando a condição deixa de valer por uma margem (ex. 0,5%), evitando spam enquanto o euro fica abaixo da meta.
- Entrega via `NotificationChannel` (interface); implementação inicial `InAppChannel`. Web Push / e-mail entram como novas implementações sem mexer nos detectores.
- `ShedLock` nos jobs para não duplicar se houver mais de uma instância.
- Motivacional: templates com placeholders (`{percent}`, `{remaining}`) + gatilhos reais (marcos 25/50/75/90%, mês fechado acima do planejado, X tarefas concluídas); no máximo 1 por semana por padrão.

### 4.10 Cotação
- `ExchangeRateProvider` (interface) → `FrankfurterProvider` (dados do BCE, sem chave, diário) como principal e `AwesomeApiProvider` (BRL intradiário) como secundário; ordem configurável.
- `ExchangeRateService`: job a cada 6h, grava em `exchange_rate` (base, quote, rate `NUMERIC(19,8)`, source, `fetched_at`, `effective_date`). Cache Caffeine em memória para a cotação vigente.
- Fallback: falha do provider → mantém a última válida e marca `stale=true` na resposta.
- Cotação manual: `user_exchange_rate_override` (ativa/inativa); quando ativa, prevalece para aquele usuário e a UI mostra o selo “manual”.

### 4.11 Mala
- `luggage_bag` (nome, tipo, peso máximo em gramas, objetivo) e `luggage_item` (mala opcional = “ainda não alocado”, categoria, quantidade, peso unitário em gramas, prioridade, obrigatório, embalado, observação). Pesos em **gramas inteiros** (evita arredondamento). Peso total e excesso calculados no backend; alerta quando > 100% e aviso em > 90%.

### 4.12 Busca global
- `GET /api/search?q=` com `pg_trgm` (índices GIN de trigram em nomes/títulos), uma query `UNION ALL` limitada por tipo, sempre filtrada por `user_id`.

---

## 5. Entidades e relacionamentos

```
app_user 1─1 user_settings
app_user 1─N refresh_token
app_user 1─N objective ──┬─N expense ──N expense_payment
                         │     ├─ N─1 expense_category (do usuário)
                         │     ├─ N─1 expense_cycle    (do usuário)
                         │     └─ N─1 university (opcional)
                         ├─N objective_adjustment
                         ├─N account ──N account_balance_snapshot
                         ├─N application ─N─1 program ─N─1 university
                         │     └─N application_status_history
                         ├─N task (─ university?, expense?, application?)
                         ├─N timeline_event (manual)
                         ├─N luggage_bag ──N luggage_item
                         └─N notification (opcional)
university 1─N program 1─N program_language_requirement
                       1─N program_intake
app_user 1─N transaction, installment_plan, recurring_entry, finance_category, monthly_saving_goal
app_user 1─N notification, notification_preference, exchange_rate_alert, user_exchange_rate_override
exchange_rate (global)   currency (global)   import_session (por usuário)
```

## 6. Esquema do banco (resumo das tabelas)

Convenções: PK `uuid` (gerado na aplicação, UUIDv7 para ordem temporal e índices melhores), `created_at`/`updated_at` `timestamptz`, `version bigint` (lock otimista), datas sem horário em `date`, dinheiro `numeric(19,4)`, enums como `varchar` + `CHECK` (mais fácil evoluir que enum nativo do Postgres), FKs com índice.

| Tabela | Colunas principais | Índices / restrições |
|---|---|---|
| `app_user` | email (citext), password_hash, display_name, onboarding_completed_at, auth_provider | unique(email) |
| `user_settings` | theme (LIGHT/DARK/SYSTEM), locale, timezone, primary_currency, secondary_currency | PK=user_id |
| `refresh_token` | user_id, token_hash, expires_at, revoked_at, family_id | idx(user_id), unique(token_hash) |
| `currency` | code CHAR(3), name, symbol, scale | seed BRL, EUR |
| `objective` | user_id, name, description, country_code, city, start_date, target_date, currency, target_mode, target_amount, initial_saved_amount, status, is_primary, color, icon, image_url, horizon_months, expense_scope (REQUIRED_ONLY/ALL) | idx(user_id,status); unique parcial (user_id) where is_primary |
| `expense_category` / `expense_cycle` | user_id, name, color, icon, sort_order, archived | unique(user_id, lower(name)) |
| `objective_adjustment` | objective_id, label, amount (com sinal), currency, applies_to (ALL/REQUIRED) | idx(objective_id) |
| `expense` | user_id, objective_id, category_id, cycle_id, university_id?, name, description, currency, unit_price, quantity, recurrence, occurrences, refundable, required, due_date, end_date, cancelled_at, notes | idx(user_id, objective_id), idx(objective_id, due_date), idx(category_id), CHECK(quantity>0, occurrences>0, unit_price>=0) |
| `expense_payment` | expense_id, paid_at (date), amount, currency, rate_to_expense_currency, note | idx(expense_id) |
| `account` | user_id, objective_id?, name, currency, type, archived | idx(user_id) |
| `account_balance_snapshot` | account_id, as_of (date), balance | unique(account_id, as_of) |
| `finance_category` | user_id, name, kind (INCOME/EXPENSE), color, icon, archived | unique(user_id, kind, lower(name)) |
| `transaction` | user_id, account_id?, category_id, kind, occurred_on (date), amount, currency, description, status (PLANNED/CONFIRMED), installment_plan_id?, installment_number?, recurring_entry_id? | idx(user_id, occurred_on), idx(installment_plan_id), idx(recurring_entry_id), unique(recurring_entry_id, occurred_on) |
| `installment_plan` | user_id, category_id, description, total_amount, installment_amount, installments_count, first_due_date, currency, payment_method | idx(user_id) |
| `recurring_entry` | user_id, category_id, name, kind, amount, currency, frequency, interval_months, billing_day, start_date, end_date?, active, auto_confirm | idx(user_id, active) |
| `monthly_saving_goal` | user_id, year_month, amount, currency | unique(user_id, year_month) |
| `university` | user_id, name, ranking, institution_type (PUBLIC/PRIVATE), nature (UNIVERSITY/TU/HOCHSCHULE/OTHER), city, region, country_code, population, latitude, longitude, cost_of_living_amount + currency, employability, website, logo_url | idx(user_id, country_code), GIN trgm(name) |
| `program` | university_id, user_id, chosen, name, area, description, official_url, degree, language_summary, duration_semesters, gre, aptitude_assessment, interview, nc, application_channel, portal_name, document_submission, semester_fee + currency, non_eu_tuition + currency + period, scholarship_available, scholarship_details, scholarship_chance, admission_chance, application_deadline_text | idx(university_id), GIN trgm(name) |
| `program_language_requirement` | program_id, language, min_cefr, test_name, min_score, raw_text | idx(program_id) |
| `program_intake` | program_id, term, open_month, open_day, close_month, close_day, start_month | idx(program_id) |
| `application` | user_id, objective_id, program_id, status, archived_at, submitted_at, deadline, response_min/likely/max_date, response_confidence, response_estimate_basis, actual_response_date, estimated_chance, notes | idx(user_id,status), idx(deadline), idx(response_likely_date), unique(user_id, program_id, intake_year, term) |
| `application_status_history` | application_id, status, changed_at, note | idx(application_id) |
| `application_request` | application_id, user_id, type (DOCUMENT/EXAM/INTERVIEW/LETTER/RECOMMENDATION/TEST/PAYMENT/OTHER), description, requested_on, due_date, fulfilled_on, status | idx(application_id), idx(user_id, due_date) |
| `task` | user_id, objective_id, title, description, priority, due_date, status, category, university_id?, expense_id?, application_id?, completed_at | idx(user_id, status, due_date) |
| `timeline_event` | user_id, objective_id, title, description, start_date, end_date, type, priority, status, related_type?, related_id? | idx(user_id, start_date) |
| `luggage_bag` | user_id, objective_id, name, type, max_weight_g | — |
| `luggage_item` | user_id, bag_id?, objective_id, name, category, quantity, unit_weight_g, priority, required, packed, notes | idx(bag_id) |
| `notification` | user_id, type, severity, title, body, action_url, source_type, source_id, dedupe_key, read_at, created_at | unique(user_id, dedupe_key), idx(user_id, read_at, created_at desc) |
| `notification_preference` | user_id, category, enabled, lead_days int[] | unique(user_id, category) |
| `exchange_rate_alert` | user_id, base, quote, condition (BELOW/ABOVE/DROP_PERCENT), threshold, baseline_rate, state, last_triggered_at, active | idx(active) |
| `exchange_rate` | base, quote, rate, source, effective_date, fetched_at | idx(base, quote, fetched_at desc) |
| `user_exchange_rate_override` | user_id, base, quote, rate, active | unique(user_id, base, quote) |
| `motivational_template` | code, category, text_pt, trigger, min_interval_days | seed |
| `import_session` | user_id, file_name, status, preview jsonb, expires_at | idx(user_id) |
| `shedlock` | padrão ShedLock | — |

Migrations planejadas: `V1__baseline_users_auth_currency.sql`, `V2__exchange_rates.sql`, `V3__objectives_accounts.sql`, `V4__expenses.sql`, `V5__personal_finance.sql`, `V6__universities_programs_applications.sql`, `V7__tasks_timeline.sql`, `V8__luggage.sql`, `V9__notifications.sql`, `V10__import.sql`, `V11__search_trgm.sql`; seeds de dados padrão em migrations `R__` ou no cadastro do usuário (categorias/ciclos são copiados por usuário no onboarding). `spring.jpa.hibernate.ddl-auto=validate`.

## 7. Arquitetura do backend

- **Java 21 LTS**, **Spring Boot 4.x** (versão estável mais recente no momento da implementação; confirmo a versão exata na Fase 1), Maven, PostgreSQL 17.
- **Monólito modular por domínio** (package-by-feature), com camadas dentro de cada módulo:

```
com.interchangeplanner
├── shared/        (Money, CurrencyCode, BaseEntity, ProblemDetails, pagination, security utils, clock)
├── config/        (Security, CORS, Jackson, OpenAPI, Scheduling, Cache)
├── auth/          (controller, service, JwtService, refresh tokens, UserDetails)
├── user/          (settings, onboarding)
├── objective/     (controller, service, ObjectiveProgressCalculator, repository, dto, mapper, entity)
├── finance/       (accounts, transactions, installments, recurring entries, MonthlySummaryService, SavingsProjectionService)
├── expense/       (expense, payments, categories, cycles, ExpenseCalculator, ExpenseSummaryService)
├── exchange/      (ExchangeRateProvider + impls, ExchangeRateService, CurrencyConverter, alerts)
├── university/    (university, program, requirements, intakes, comparison)
├── application/   (candidaturas, ResponseEstimate)
├── task/
├── timeline/      (TimelineProjector por módulo, TimelineService)
├── luggage/       (LuggageWeightCalculator)
├── notification/  (detectors, channels, preferences, scheduler, motivational)
├── dashboard/     (DashboardService — orquestra os serviços acima)
├── search/
└── importer/      (ExcelImportService, sheet parsers por aba, normalizers, PreviewService)
```

- Controllers finos (validação de entrada, delega, mapeia). Regras em services e em **calculadoras puras** (sem Spring, 100% testáveis).
- DTOs como `record`s; mapeamento com **MapStruct** (gerado em compile time, sem reflexão). Lombok **não** será usado: records + MapStruct cobrem o caso e evitam acoplamento com o compilador.
- Erros: `@RestControllerAdvice` retornando **RFC 9457 Problem Details** (`type`, `title`, `status`, `detail`, `errors[]` por campo, `traceId`).
- `Clock` injetado em tudo que depende de “hoje” (testes determinísticos de prazos).
- JPA: `open-in-view=false`, fetch LAZY por padrão, `@EntityGraph`/projeções para listagens, paginação com `Pageable` + `Specification` para filtros.
- OpenAPI via **springdoc** (`/swagger-ui`, `/v3/api-docs`); o frontend **gera os tipos TypeScript a partir do OpenAPI** (`openapi-typescript`) — elimina duplicação de contratos.
- Jobs com `@Scheduled` + ShedLock. Apache POI apenas dentro de `importer`.

## 8. API REST

Prefixo `/api`, JSON, paginação `?page&size&sort`, filtros por query string. Todos os IDs recebidos são validados contra o usuário autenticado.

**Auth / usuário**
`POST /auth/register` · `POST /auth/login` · `POST /auth/refresh` · `POST /auth/logout` · `GET /me` · `GET|PUT /me/settings` · `POST /onboarding`

**Objetivos**
`GET|POST /objectives` · `GET|PUT|DELETE /objectives/{id}` · `PUT /objectives/{id}/primary` · `GET /objectives/{id}/progress` · `GET /objectives/{id}/projection`

**Gastos**
`GET /expenses?objectiveId&categoryId&cycleId&status&required&currency&from&to&universityId&q` · `POST /expenses` · `GET|PUT|DELETE /expenses/{id}` · `POST /expenses/preview-total` (cálculo sem salvar, para o formulário mostrar o total em tempo real sem duplicar regra no TS) · `GET /expenses/summary?objectiveId&groupBy=cycle|category|recurrence|status` · `GET|POST /expenses/{id}/payments` · `DELETE /expenses/{id}/payments/{paymentId}` · `POST /expenses/{id}/cancel`
`GET|POST|PUT|DELETE /expense-categories[/{id}]` · `GET|POST|PUT|DELETE /expense-cycles[/{id}]`

**Finanças**
`GET|POST /accounts` · `PUT|DELETE /accounts/{id}` · `POST /accounts/{id}/balances` · `GET /accounts/summary` (por moeda + convertido)
`GET|POST /finance/transactions?from&to&kind&categoryId&status` · `PUT|DELETE /finance/transactions/{id}` · `PATCH /finance/transactions/{id}/confirm`
`GET|POST /finance/installment-plans` · `GET|PUT|DELETE /finance/installment-plans/{id}`
`GET|POST /finance/recurring` · `PUT|DELETE /finance/recurring/{id}` · `PATCH /finance/recurring/{id}/active`
`GET|POST|PUT|DELETE /finance/categories[/{id}]` · `GET|PUT /finance/saving-goals`
`GET /finance/summary?month=2026-11` (visão mensal) · `GET /finance/overview?from&to` (série para gráficos) · `GET /finance/commitments?months=12` (parcelas/assinaturas futuras)

**Câmbio**
`GET /exchange-rates/current?base=EUR&quote=BRL` · `GET /exchange-rates/history?from&to` · `PUT|DELETE /exchange-rates/override` · `GET|POST /exchange-rate-alerts` · `PUT|DELETE /exchange-rate-alerts/{id}`

**Universidades / programas / candidaturas**
`GET /universities?country&city&type&nature&q` · `POST /universities` · `GET|PUT|DELETE /universities/{id}`
`GET /programs?universityId&chosen&applicationStatus&language&gre&interview&nc&admissionChance&deadlineBefore&maxFee&q` · `POST /programs` · `GET|PUT|DELETE /programs/{id}` · `PATCH /programs/{id}/chosen` · `GET /programs/compare?ids=a,b,c` (2–5)
`GET /applications?status&objectiveId` · `POST /applications` · `GET|PUT|DELETE /applications/{id}` · `PATCH /applications/{id}/status` · `GET /applications/{id}/history` · `GET|POST /applications/{id}/requests` · `PUT|DELETE /applications/{id}/requests/{requestId}`

**Planejamento**
`GET /timeline?objectiveId&from&to&types&sources` · `POST /timeline/events` · `PUT|DELETE /timeline/events/{id}`
`GET /tasks?status&priority&objectiveId&dueBefore` · `POST /tasks` · `PUT|DELETE /tasks/{id}` · `PATCH /tasks/{id}/status`
`GET|POST /luggage/bags` · `PUT|DELETE /luggage/bags/{id}` · `GET /luggage/items?bagId&category&packed` · `POST /luggage/items` · `PUT|DELETE /luggage/items/{id}` · `PATCH /luggage/items/{id}/packed` · `GET /luggage/summary`

**Notificações**
`GET /notifications?unread&page` · `GET /notifications/unread-count` · `PATCH /notifications/{id}/read` · `POST /notifications/read-all` · `GET|PUT /notification-preferences`

**Agregados**
`GET /dashboard?objectiveId` → `{ objective, progress, financial, balances, expenses, universities, applications, upcoming[], notifications[], exchangeRate }` · `GET /search?q&types`

**Importação**
`POST /imports` (multipart) → preview com linhas `VALID | WARNING | ERROR | DUPLICATE` por aba · `GET /imports/{id}` · `POST /imports/{id}/commit` (opções: abas a importar, objetivo destino, estratégia de duplicados `SKIP|UPDATE`) → resumo

## 9. Frontend

Stack: React 19 + TypeScript + Vite, React Router (data router), TanStack Query, TanStack Table (comparação, colunas ocultáveis, ordenação), Tailwind CSS v4, **shadcn/ui** (Radix — acessível, código no repo, sem lock-in visual), Recharts, React Hook Form + Zod, date-fns, lucide-react, Vitest + Testing Library + MSW, Playwright para fluxos E2E críticos.

```
frontend/src
├── app/            (router, providers, query client, theme provider, auth guard)
├── api/            (cliente fetch com refresh automático, tipos gerados do OpenAPI)
├── layouts/        (AppShell: Sidebar desktop / BottomNav + "Mais" sheet mobile, AuthLayout)
├── components/ui/  (shadcn: button, card, dialog/drawer, sheet, tabs, accordion, form, badge, progress…)
├── components/     (MoneyAmount, DualCurrency, ProgressRing, StatCard, EmptyState, ErrorState, PageHeader,
│                   FilterBar, DateRangeBar, ConfirmDialog, ResponsiveDialog = Dialog no desktop / Drawer no mobile)
├── features/
│   ├── dashboard/  ├── objectives/ ├── expenses/ ├── finance/ ├── exchange/
│   ├── universities/ ├── applications/ ├── timeline/ ├── tasks/ ├── luggage/
│   ├── notifications/ ├── search/ ├── settings/ ├── onboarding/ ├── importer/ └── auth/
│      (cada feature: api.ts com hooks de query/mutation, schemas.ts (Zod), components/, pages/)
├── hooks/  ├── utils/ (formatação apenas: Intl.NumberFormat, datas) └── styles/ (tokens)
```

Regra: o frontend **formata**, não **calcula**. Totais, progresso, conversões, status e pesos vêm prontos da API; o formulário de gasto chama `preview-total` (debounced) para mostrar o total.

**Páginas**
| Rota | Conteúdo |
|---|---|
| `/` Dashboard | Hero do objetivo (anel de progresso, faltam R$ X, R$ Y/mês), saldos BRL/EUR + total convertido, cotação com selo de frescor, resumo de gastos (barra empilhada pago/pendente + por ciclo), próximos eventos, candidaturas, alertas |
| `/objectives`, `/objectives/:id` | Lista de objetivos, detalhe com projeção |
| `/expenses` | Lista agrupável (ciclo/categoria), filtros em sheet no mobile, detalhe com pagamentos; gráficos por ciclo/categoria |
| `/finance` | Controle financeiro: seletor de mês, cards receitas/despesas/sobra/meta, lançamentos, abas **Parcelas** e **Assinaturas**, comprometido futuro, gráficos; contas e saldos BRL/EUR |
| `/universities` | Cards / Tabela com selo “Escolhida” e status da candidatura (alteráveis direto do card), filtros, seleção para comparar; detalhe com abas: Geral · Programas · Requisitos · Candidatura · Custos · Bolsa · Prazos |
| `/universities/compare?ids=` | Comparador lado a lado (scroll horizontal com primeira coluna fixa no mobile, destaque do melhor valor por linha) |
| `/applications` | Kanban por status (desktop) / lista agrupada (mobile); faixa de resposta mínima–provável–máxima |
| `/timeline` | Linha do tempo vertical por mês, filtros por origem/tipo |
| `/tasks` | Lista com prioridades e prazos |
| `/luggage` | Malas com medidor de peso (5,8 / 10 kg), itens com checkbox “embalado” |
| `/notifications` | Central + histórico |
| `/settings` | Perfil, tema, moedas, cotação manual, alertas de câmbio, preferências de notificação, categorias/ciclos, importação |
| `/onboarding` | 6 passos (objetivo, país, data alvo, quanto já tem, economia mensal, moeda) |
| `/login`, `/register` | — |

Mobile: bottom nav com **Início · Gastos · Timeline · Universidades · Finanças**, e “Mais” abre sheet com o restante; FAB contextual para “adicionar”; formulários longos viram steps em drawer de tela cheia; tabelas viram listas de cards; alvos de toque ≥ 44px.

## 10. Identidade visual (própria)

Das referências aproveito **ideias**, não layout:
- Do app de viagem: superfícies claras e calmas, cards bem arredondados, um único verde como acento, muito respiro.
- Do pôster da combustão: a **timeline editorial** — uma “espinha” vertical colorida, datas em numerais grandes e condensados (`MAR` / `27`), eventos alternando lados no desktop. Fica como assinatura visual da página Timeline e do resumo no dashboard.

Proposta: neutros quentes (off-white `#F6F5F1` / grafite), acento **verde-pinho** profundo para progresso e ação, **âmbar** para atenção e **coral** apenas para alertas/excesso. Tipografia: *Inter* para interface com números tabulares, *Fraunces* (serifada) só em títulos de destaque e no valor do hero. Dark mode desenhado de verdade (não inversão): fundo `#0F1412` esverdeado, cards elevados por luminosidade, não por sombra. Tokens CSS em `:root`/`.dark`, tema LIGHT/DARK/SYSTEM persistido no backend.

## 11. Segurança, observabilidade, testes

**Segurança**: BCrypt (strength 12) via `PasswordEncoder`; JWT de acesso curto (15 min) validado pelo **Spring Security OAuth2 Resource Server** (Nimbus), assinado com chave de `.env`; refresh token opaco rotativo (hash no banco, cookie `HttpOnly; Secure; SameSite=Strict`, detecção de reuso por família); CORS por variável de ambiente; rate limit simples no login; validação Bean Validation em todos os DTOs; limites de upload na importação (tamanho, tipo MIME, zip-bomb). Arquitetura preparada para OAuth (`auth_provider`, `oauth2-client` depois).
**Observabilidade**: logs estruturados (JSON em prod) com `traceId`; filtro que **não** loga corpo de requisição, senhas, tokens nem valores financeiros; Actuator `health`/`info`; métricas dos jobs (execuções, notificações geradas, falhas do provider de câmbio).
**Testes**:
- Unitários das calculadoras: total de gasto por periodicidade, conversão, progresso/meta/faltam/economia mensal necessária, projeção, status de pagamento, peso de mala, janelas de inscrição, estimativa de resposta, histerese de alerta.
- Integração com **Testcontainers (PostgreSQL real)**: migrations Flyway, repositórios, isolamento entre usuários (teste explícito: usuário B nunca acessa recurso de A em todos os endpoints).
- Controller tests (MockMvc) dos endpoints principais e do Problem Details.
- Importação: teste de regressão com a própria `Gastos.xlsx` (contagens e totais conhecidos do §1).
- Frontend: Vitest para componentes críticos (formulário de gasto, filtros, medidor de peso, onboarding), MSW para API; Playwright para login → onboarding → criar gasto → ver dashboard.
- CI/CD: ver §14.

## 12. Decisões e ambiguidades

Decididas por você:
1. Mestrado de **2 anos**: horizonte padrão 24 meses; Sperrkonto (R$ 75.000) é ajuste único, descontado uma vez.
2. Status: só é pago o que tem Data de Pagamento; `Sim` = `Não`.
3. Erros de digitação de categorias corrigidos automaticamente; Viagem e Transporte com os significados que você definiu.
4. Datas de 2026 da passagem e mala extra → 2027. Lat/long ignorados.
5. Aba Economia ignorada; no lugar, módulo de Controle Financeiro com receitas, despesas, parcelas e assinaturas.
6. Universidades com “Escolhida” + os novos status de candidatura.
7. A planilha é só base: tudo editável e expansível no app.
8. Código entregue numa pasta do seu computador; você faz o commit no GitHub ao fim de cada fase.

Assumidas por mim (mudam fácil):
9. Escopo da meta derivada configurável; padrão = obrigatórios.
10. “Por UNI” = `PER_UNIT` com quantidade explícita (5 na importação).
11. Valores de universidades em EUR (custo de vida/mês, taxa/semestre).
12. Moeda principal BRL, equivalente em EUR.
13. “Solicitado…” = qualquer item que a universidade peça → status **Solicitado item** + registro de solicitações (`application_request`).

## 13. Plano por fases

Cada fase termina com: build verde, testes passando, CI verde, README atualizado, migrations aplicadas, funcionalidade completa ponta a ponta (API + banco + UI com loading/vazio/erro/sucesso + responsivo).

| Fase | Entregas | Testes-chave |
|---|---|---|
| **1. Fundação** | Monorepo `/backend` `/frontend`, docker-compose (postgres, backend, frontend), `.env.example`, Flyway V1, Spring Security + JWT + refresh, Problem Details, OpenAPI + geração de tipos TS, AppShell responsivo (sidebar/bottom nav), tema light/dark/system, tokens de design, login/registro, **CI no GitHub Actions**, README base | auth, isolamento, migrations com Testcontainers |
| **2. Objetivos + câmbio base + contas** | CRUD objetivos, objetivo principal, contas e saldos BRL/EUR, `ExchangeRateProvider` + cache + fallback + override manual, `ObjectiveProgressCalculator`, onboarding, dashboard inicial | progresso, conversão, fallback de cotação |
| **3. Gastos** | Categorias/ciclos (seed por usuário), CRUD gastos, periodicidade/ocorrências, ajustes manuais do objetivo, pagamentos parciais em qualquer moeda, filtros, resumo por ciclo/categoria/periodicidade, gráficos, preview de total | cálculo por periodicidade, status derivado, ajustes aplicados aos totais |
| **4. Controle Financeiro** | Lançamentos, categorias, parcelamentos, assinaturas/recorrências, visão mensal, comprometido futuro, metas de economia, gráficos, projeção do objetivo | geração de parcelas (centavos), recorrências por frequência, viradas de ano, projeção |
| **5. Universidades** | University, Program, “Escolhida”, status e histórico da candidatura, requisitos de idioma, intakes, estimativa de resposta, cards/tabela, filtros, comparador 2–5 | janelas de inscrição, validação min≤provável≤máx, filtros |
| **6. Timeline + tarefas** | Projetores por módulo, eventos manuais, timeline editorial, CRUD tarefas, próximos eventos no dashboard | merge/ordenação, ids determinísticos |
| **7. Mala** | Malas com limite, itens, pesos, alertas, checklist “embalado” | peso total, excesso |
| **8. Notificações** | Detectores, dedupe, preferências/antecedências, alertas de câmbio com histerese, motivacionais, central + badge | dedupe, histerese, antecedências com `Clock` fixo |
| **9. Importação Excel** | Parsers das abas Gastos, Universidades, Mala e Auxiliares (Economia ignorada), normalização, preview, commit idempotente, resumo | regressão com `Gastos.xlsx` |
| **10. Polimento + deploy** | Busca global, acessibilidade (axe), performance, E2E Playwright, documentação OpenAPI revisada, **CD e deploy em plataforma gratuita** | E2E dos fluxos principais, smoke test pós-deploy |

Observação de ordem: o câmbio básico sobe para a Fase 2 porque o progresso do objetivo depende de converter o saldo em EUR; os **alertas** de câmbio continuam na Fase 8.

## 14. Fluxo de entrega, README, CI/CD e deploy

**Entrega por fase**: gravo o código na pasta conectada, rodo build e testes, e te passo um resumo do que mudou + uma **mensagem de commit sugerida** (Conventional Commits). Você revisa e faz o commit/push no GitHub.

**README** (atualizado a cada fase), com: visão geral e screenshots · status das fases (checklist) · arquitetura (diagrama de módulos) · stack e versões · pré-requisitos · como rodar com Docker · como rodar backend e frontend fora do Docker · variáveis de ambiente (tabela com descrição e exemplo) · banco e migrations · testes · estrutura de pastas · endpoints principais + link do Swagger · regras de negócio (cálculos) · CI/CD e deploy · decisões de arquitetura (ADRs curtas em `docs/adr/`). Também um `CHANGELOG.md` por fase.

**CI (GitHub Actions, desde a Fase 1)**
- `ci.yml` em push/PR: backend `mvn verify` (unitários + Testcontainers com Postgres) · frontend `lint`, `typecheck`, `test`, `build` · checagem de que os tipos TS gerados do OpenAPI estão atualizados · cache de Maven e npm.
- `docker.yml` em push na `main`/tags: build das imagens backend e frontend e publicação no GitHub Container Registry.
- Dependabot para Maven, npm e Actions; CodeQL opcional.

**CD e deploy (Fase 10)**: `deploy.yml` disparado por tag `v*` (ou manualmente). Candidatos gratuitos, a confirmar na Fase 10 porque os planos grátis mudam com frequência: frontend estático em Cloudflare Pages ou Vercel; backend (imagem Docker) em Render ou Koyeb; PostgreSQL em Neon ou Supabase. Ponto de atenção: backends gratuitos costumam “dormir” sem tráfego, o que pararia os jobs das 08:00 — a solução prevista é um endpoint interno protegido por token, acionado por um cron do GitHub Actions.
