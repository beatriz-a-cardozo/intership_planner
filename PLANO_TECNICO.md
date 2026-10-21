# Interchange Planner — Plano Técnico (v1, para aprovação)

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
12. Ambiguidades em aberto
13. Plano de implementação por fases

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

**Valores atuais calculados pela planilha** (servirão como teste de regressão da importação):
total geral R$ 237.593,28 · obrigatório R$ 189.393,28 · pago R$ 11.721,68 · Sperrkonto R$ 75.000 · custo mensal recorrente ≈ R$ 8.670/mês.

## 2. Fórmulas → regras de negócio

| Fórmula da planilha | Problema | Regra no sistema |
|---|---|---|
| `Preço Total = IF(Tipo="Único",P, IF("Por UNI",P*5, IF("Semestral",P*2, IF("Anual",P*1, IF("Mensal",P*12,0)))))` | Multiplicadores fixos (5 universidades, 12 meses, 2 semestres) e tipo desconhecido vira 0 | `total = preço_unitário × quantidade × ocorrências`. `ocorrências` é **armazenado** no gasto; quando não informado, o backend sugere a partir do **horizonte do objetivo** (ex.: 12 meses → Mensal=12, Semestral=2, Anual=1). Periodicidade desconhecida = erro de validação, nunca 0 |
| `Preço Unit. (€) = R$/6` | Câmbio fixo, e o valor em € é derivado do R$ (o dado original se perde) | Gasto guarda **moeda original + valor original**. Equivalente na outra moeda é calculado na leitura com a cotação vigente (com timestamp) |
| `Total Obrigatório = SUMIF(Obrigatório="Sim") − 75000` e `Total = SUM − 75000` | Subtração manual do Sperrkonto escondida na fórmula | **Mantido como na planilha (decisão sua)**: todos os gastos somam e o objetivo tem **ajustes manuais** (`objective_adjustment`: rótulo, valor, moeda) subtraídos dos totais — ex. “Sperrkonto financia o 1º ano: −R$ 75.000”. Fica explícito e visível no dashboard, em vez de escondido numa fórmula. Ver §4.3 |
| `Pago = SUMIF(Status="Pago", Valor Pago)` | “Pago” sem valor (Passaporte) soma 0; pagamento diferente do previsto (R$ 219,80 → R$ 224) | Pagamentos são registros próprios (`expense_payment`); o status é **derivado** de Σ pagamentos vs total (Pendente / Parcial / Pago); Cancelado é manual |
| `Pendente = Obrigatório − Pago` | Mistura escopos (pago inclui não obrigatórios) | `restante = Σ max(total − pago, 0)` calculado sobre o mesmo escopo de filtro |
| `SUMIF por Ciclo / Categoria / Tipo` (Auxiliares) | — | Endpoint de agregação `GET /api/expenses/summary?groupBy=cycle|category|recurrence` (uma query `GROUP BY`) |
| `Mensal: SUMIF(...)/12` | — | Indicador “custo mensal recorrente” no resumo |
| `E69 = euro × 6` | Câmbio fixo | Saldos convertidos com cotação real |
| `Economia: SUM(C2:C12)` | — | Soma planejada vs realizada, por mês, com projeção |

## 3. Problemas de dados encontrados (a importação precisa tratar)

- **Categorias inconsistentes**: `Proeficiência` vs `Proficiência`, `Candidaturas` vs `Candidatura`; `Viagem` e `Transporte` usados nos gastos mas ausentes em Auxiliares → normalização por similaridade + criação de categoria nova com aviso no preview.
- **Status ambíguo** (`Pago`, `Não`, `Sim`) → **regra decidida**: só é importado como pago o gasto que tem **Data de Pagamento**; ele gera um `expense_payment` com o Valor Pago. Todo o resto (`Não`, `Sim`, e `Pago` sem data — ex. Passaporte) entra como **Pendente**, com o status original guardado na observação.
- **Datas suspeitas**: Passagem aérea e Mala extra em **15/05/2026** (no passado; provavelmente 2027) → aviso “data no passado para gasto pendente”.
- **Valores com fórmula** (`=314*0.7`) → lê o valor calculado e guarda a fórmula na observação.
- **Coluna O sem cabeçalho** (“Obs. Parcelado em 10x”, “Obs. $227,85 USD”) → vai para observações.
- **Universidades**: TU Köln com longitude `73353` e latitude `5102608` (inválidas) e Tipo = `Sim`; cidade `Bayreich` (Bayreuth); Freiburg com estado “Freiburg im Breisgau”; taxa de estrangeiro `-`; linha 22 só com uma URL solta; rankings `0` para privadas (= sem ranking).
- **Prazos em texto livre**: `01/Dezembro - 01/Março`, `x - 15/Novembro`, `30/Abril-Junho`, `x` → parser para janela (abertura opcional, fechamento) dia/mês; o que não parsear vira aviso e fica em texto.
- **Proficiência em texto livre**: `B2 (TOEFL >= 90)`, `B2 (TOELF >= 88) + Alemão A1`, `B2/A2` → parser para requisitos estruturados (idioma, CEFR, teste, nota mínima); texto original preservado.
- **Requisitos tri-estado**: Darmstadt tem `Se necessário (GRE)` → `CONDITIONAL`.
- **Aba Economia**: **ignorada na importação** (decisão sua). O módulo Economia começa vazio.
- **Mala** só tem a primeira linha com o nome da mala (estilo célula mesclada) → propagar para baixo; “Nível de peso” vira peso estimado opcional.

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
- **Previsão de atingimento**: projeta mês a mês usando os valores planejados do módulo Economia (quando existem) ou a média das últimas N economias realizadas; retorna o mês previsto ou “não atinge até a data alvo, déficit de R$ X”.

### 4.4 Gastos
- `recurrence`: `ONE_TIME | MONTHLY | SEMIANNUAL | ANNUAL | PER_UNIT | CUSTOM`. `occurrences` sempre explícito no banco. `PER_UNIT` usa `quantity` (ex.: 5 inscrições) e `occurrences = 1`.
- Datas: `due_date` (primeira/única ocorrência) e opcional `end_date` para recorrentes (permite gerar parcelas na timeline).
- Pagamentos: `expense_payment` com moeda própria + taxa capturada no momento (TOEFL pago em USD; agência parcelada em 10×). `valor pago` = Σ pagamentos convertidos para a moeda do gasto com a taxa **do pagamento** (histórico não muda quando a cotação muda).
- Status: `PENDING / PARTIALLY_PAID / PAID` derivados; `CANCELLED` manual (persistido como flag `cancelled_at`).
- Categoria e ciclo: tabelas por usuário com seeds padrão (os 4 ciclos e as categorias da planilha + Viagem, Transporte), cor e ícone.

### 4.5 Contas / saldos
- `account` (nome, moeda, tipo: corrente, poupança, investimento, espécie, conta bloqueada, outro; objetivo opcional) + `account_balance_snapshot` (saldo em data). Saldo atual = último snapshot. O histórico alimenta o gráfico de evolução do patrimônio. BRL e EUR nunca são misturados: totais convertidos são apenas **visões**.

### 4.6 Economia mensal
- `monthly_record` único por (usuário, objetivo, `year_month`) com moeda, renda, gastos, economia planejada, economia realizada, observações. Breakdown opcional em `monthly_record_item` (categoria de gasto mensal + valor); se houver breakdown, `gastos` = soma dele (regra no backend). “Sobra” = renda − gastos é derivado; economia realizada é informada (pode diferir da sobra).

### 4.7 Universidades / programas / candidaturas
- `university` (dados da instituição) 1—N `program`.
- Escalas ordinais como enums: `Rating5 = LOW, LOW_MEDIUM, MEDIUM, MEDIUM_HIGH, HIGH`; empregabilidade ganha `EXCELLENT`.
- Requisitos (GRE, aptitude assessment, entrevista, NC) como `Requirement = REQUIRED | NOT_REQUIRED | CONDITIONAL | UNKNOWN`.
- Canal de inscrição: `DIRECT | UNI_ASSIST | VPD | PORTAL` + `portal_name` (C@MPUS, unisono, Augsburg). Envio de documentos: `ONLINE | PHYSICAL | BOTH`.
- `program_language_requirement` (N por programa): idioma, CEFR mínimo, teste, nota mínima, texto original.
- `program_intake` (N por programa): semestre (`WINTER | SUMMER | OTHER`), janela de inscrição recorrente (`open_month/day`, `close_month/day`), mês de início. A **candidatura** é que tem datas concretas de um ciclo específico.
- `application` guarda apenas `status`; o **resultado** é derivado do status (evita estados contraditórios como status “Aceita” + resultado “Rejeitada”). A API expõe os dois campos como pedido.
- Estimativa de resposta: `response_min_date`, `response_likely_date`, `response_max_date`, `response_confidence (LOW|MEDIUM|HIGH)`, `response_estimate_basis` (texto). Validação: min ≤ provável ≤ máx. A UI sempre rotula como “Estimativa baseada nos seus dados”.
- Latitude/longitude `NUMERIC(9,6)` com CHECK de faixa (prepara mapa; índices simples, PostGIS não é necessário agora).

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
                         ├─N monthly_record ──N monthly_record_item ─N─1 monthly_expense_category
                         ├─N application ─N─1 program ─N─1 university
                         ├─N task (─ university?, expense?, application?)
                         ├─N timeline_event (manual)
                         ├─N luggage_bag ──N luggage_item
                         └─N notification (opcional)
university 1─N program 1─N program_language_requirement
                       1─N program_intake
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
| `monthly_record` | user_id, objective_id, year_month (date dia 1), currency, income, expenses, planned_saving, actual_saving, notes | unique(user_id, objective_id, year_month) |
| `monthly_expense_category` / `monthly_record_item` | categoria + valor | — |
| `university` | user_id, name, ranking, institution_type (PUBLIC/PRIVATE), nature (UNIVERSITY/TU/HOCHSCHULE/OTHER), city, region, country_code, population, latitude, longitude, cost_of_living_amount + currency, employability, website, logo_url | idx(user_id, country_code), GIN trgm(name) |
| `program` | university_id, user_id, name, area, description, official_url, degree, language_summary, duration_semesters, gre, aptitude_assessment, interview, nc, application_channel, portal_name, document_submission, semester_fee + currency, non_eu_tuition + currency + period, scholarship_available, scholarship_details, scholarship_chance, admission_chance, application_deadline_text | idx(university_id), GIN trgm(name) |
| `program_language_requirement` | program_id, language, min_cefr, test_name, min_score, raw_text | idx(program_id) |
| `program_intake` | program_id, term, open_month, open_day, close_month, close_day, start_month | idx(program_id) |
| `application` | user_id, objective_id, program_id, status, submitted_at, deadline, response_min/likely/max_date, response_confidence, response_estimate_basis, actual_response_date, estimated_chance, notes | idx(user_id,status), idx(deadline), idx(response_likely_date), unique(user_id, program_id, intake_year, term) |
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

Migrations planejadas: `V1__baseline_users_auth.sql`, `V2__currency_exchange.sql`, `V3__objectives_accounts.sql`, `V4__expenses.sql`, `V5__monthly_finance.sql`, `V6__universities_programs_applications.sql`, `V7__tasks_timeline.sql`, `V8__luggage.sql`, `V9__notifications.sql`, `V10__import.sql`, `V11__search_trgm.sql`; seeds de dados padrão em migrations `R__` ou no cadastro do usuário (categorias/ciclos são copiados por usuário no onboarding). `spring.jpa.hibernate.ddl-auto=validate`.

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
├── finance/       (account, snapshots, monthly records, SavingsProjectionService)
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
`GET /monthly-records?objectiveId&from&to` · `POST /monthly-records` · `PUT|DELETE /monthly-records/{id}` · `GET /monthly-records/summary`

**Câmbio**
`GET /exchange-rates/current?base=EUR&quote=BRL` · `GET /exchange-rates/history?from&to` · `PUT|DELETE /exchange-rates/override` · `GET|POST /exchange-rate-alerts` · `PUT|DELETE /exchange-rate-alerts/{id}`

**Universidades / programas / candidaturas**
`GET /universities?country&city&type&nature&q` · `POST /universities` · `GET|PUT|DELETE /universities/{id}`
`GET /programs?universityId&language&gre&interview&nc&admissionChance&deadlineBefore&maxFee&q` · `POST /programs` · `GET|PUT|DELETE /programs/{id}` · `GET /programs/compare?ids=a,b,c` (2–5)
`GET /applications?status&objectiveId` · `POST /applications` · `GET|PUT|DELETE /applications/{id}` · `PATCH /applications/{id}/status`

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
| `/finance` | Contas e saldos, economia mensal (gráfico planejado × realizado, barras + linha acumulada), projeção |
| `/universities` | Cards / Tabela, filtros, seleção para comparar; detalhe com abas: Geral · Programas · Requisitos · Candidatura · Custos · Bolsa · Prazos |
| `/universities/compare?ids=` | Comparador lado a lado (scroll horizontal com primeira coluna fixa no mobile, destaque do melhor valor por linha) |
| `/applications` | Kanban por status (desktop) / lista agrupada (mobile); faixa de resposta mínima–provável–máxima |
| `/timeline` | Linha do tempo vertical por mês, filtros por origem/tipo |
| `/tasks` | Lista com prioridades e prazos |
| `/luggage` | Malas com medidor de peso (5,8 / 10 kg), itens com checkbox “embalado” |
| `/notifications` | Central + histórico |
| `/settings` | Perfil, tema, moedas, cotação manual, alertas de câmbio, preferências de notificação, categorias/ciclos, importação |
| `/onboarding` | 6 passos (objetivo, país, data alvo, quanto já tem, economia mensal, moeda) |
| `/login`, `/register` | — |

Mobile: bottom nav com **Início · Gastos · Timeline · Universidades · Economia**, e “Mais” abre sheet com o restante; FAB contextual para “adicionar”; formulários longos viram steps em drawer de tela cheia; tabelas viram listas de cards; alvos de toque ≥ 44px.

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
- CI (GitHub Actions): build + testes backend e frontend + geração de tipos OpenAPI conferida.

## 12. Decisões tomadas e ambiguidades restantes

Decididas por você:
1. Sperrkonto: mantido como na planilha → ajustes manuais do objetivo subtraídos dos totais.
2. Status da planilha: só é “pago” o que tem Data de Pagamento; o resto é Pendente.
3. Aba Economia: ignorada na importação.
4. Código: gravado numa pasta do seu computador, fase a fase.

Assumidas por mim (mudam com um ajuste simples se discordar):
5. Horizonte de custos recorrentes: `horizon_months` no objetivo, padrão 12 (como “UM ANO DE MESTRADO”), editável.
6. Escopo da meta derivada: configurável; padrão = obrigatórios.
7. “Por UNI”: `PER_UNIT` com quantidade explícita (5 na importação).
8. Valores de universidades assumidos em EUR (custo de vida/mês, taxa/semestre).
9. Mala “Nível de peso: Leve” → importa sem peso, com peso sugerido por categoria editável.
10. Moeda principal BRL, equivalente em EUR (toggle invertível).
11. Gastos pendentes com data no passado (Passagem e Mala extra em 15/05/2026) → aviso no preview, sem correção automática.

## 13. Plano por fases

Cada fase termina com: build verde, testes passando, migrations aplicadas, commits lógicos, funcionalidade completa ponta a ponta (API + banco + UI com loading/vazio/erro/sucesso + responsivo).

| Fase | Entregas | Testes-chave |
|---|---|---|
| **1. Fundação** | Monorepo `/backend` `/frontend`, docker-compose (postgres, backend, frontend), `.env.example`, Flyway V1, Spring Security + JWT + refresh, Problem Details, OpenAPI + geração de tipos TS, AppShell responsivo (sidebar/bottom nav), tema light/dark/system, tokens de design, login/registro, CI | auth, isolamento, migrations com Testcontainers |
| **2. Objetivos + câmbio base + contas** | CRUD objetivos, objetivo principal, contas e saldos BRL/EUR, `ExchangeRateProvider` + cache + fallback + override manual, `ObjectiveProgressCalculator`, onboarding, dashboard inicial | progresso, conversão, fallback de cotação |
| **3. Gastos** | Categorias/ciclos (seed por usuário), CRUD gastos, periodicidade/ocorrências, ajustes manuais do objetivo, pagamentos parciais em qualquer moeda, filtros, resumo por ciclo/categoria/periodicidade, gráficos, preview de total | cálculo por periodicidade, status derivado, ajustes aplicados aos totais |
| **4. Economia** | Registros mensais, breakdown por categoria, planejado × realizado, projeção de atingimento, economia mensal necessária no dashboard | projeção, meses restantes, viradas de ano |
| **5. Universidades** | University, Program, requisitos de idioma, intakes, Application com estimativa de resposta, cards/tabela, filtros, comparador 2–5 | janelas de inscrição, validação min≤provável≤máx, filtros |
| **6. Timeline + tarefas** | Projetores por módulo, eventos manuais, timeline editorial, CRUD tarefas, próximos eventos no dashboard | merge/ordenação, ids determinísticos |
| **7. Mala** | Malas com limite, itens, pesos, alertas, checklist “embalado” | peso total, excesso |
| **8. Notificações** | Detectores, dedupe, preferências/antecedências, alertas de câmbio com histerese, motivacionais, central + badge | dedupe, histerese, antecedências com `Clock` fixo |
| **9. Importação Excel** | Parsers das abas Gastos, Universidades, Mala e Auxiliares (Economia ignorada), normalização, preview, commit idempotente, resumo | regressão com `Gastos.xlsx` |
| **10. Polimento** | Busca global, acessibilidade (axe), performance (bundle split por rota, queries), E2E Playwright, README completo, documentação OpenAPI revisada | E2E dos fluxos principais |

Observação de ordem: o câmbio básico sobe para a Fase 2 porque o progresso do objetivo depende de converter o saldo em EUR; os **alertas** de câmbio continuam na Fase 8.
