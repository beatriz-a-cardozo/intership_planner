# Changelog

Formato baseado em [Keep a Changelog](https://keepachangelog.com/pt-BR/1.1.0/). Versões por fase do plano.

## [1.1.0] — Ajustes: meta por ciclos, mapa, guardar dinheiro e responsividade — 2026-10-23

### Adicionado
- **Mapa dos destinos** no dashboard: universidades dos programas marcados como "Escolhida" (Leaflet + OpenStreetMap,
  tiles escuros no tema escuro) com a lista ao lado. As coordenadas são descobertas pela cidade (Nominatim, no
  máximo 1 consulta/s) e gravadas; editar a universidade sem mudar a cidade mantém as coordenadas.
- **Guardar dinheiro** no dashboard e no objetivo: soma (ou retira) do saldo de hoje de uma conta vinculada, criando
  a "Reserva do intercâmbio" quando não há conta na moeda.
- Migration `V12__goal_cycles`: ciclos com "entra na meta".

### Alterado
- **Meta só com os ciclos de Preparação e Imigração** (configurável por ciclo em Configurações → Ciclos). Vale para a
  meta derivada e para as metas obrigatória e completa.
- **Responsividade:** os layouts das páginas reagem à largura da área de conteúdo (container queries), não da janela:
  com a barra lateral aberta, telas de 1024 px ficam em uma coluna em vez de duas colunas espremidas. Itens de grid
  podem encolher (listas e nomes longos não saem mais dos cards), palavras longas quebram, cabeçalhos com ações
  quebram linha, valores dos indicadores se ajustam ao card e as parcelas descem valor e ações para a linha de baixo
  no celular.
- Página da universidade: "Sobre" e "Programas" com cabeçalhos da mesma altura, cards alinhados.

### Corrigido
- Barra de comparação de programas não cobre mais a navegação inferior entre 768 e 1024 px.
- "Novo ciclo" (era "Nova ciclo") em Configurações.

## [1.0.0] — Fase 10: Polimento e deploy — 2026-10-22

### Adicionado
- **Busca global** (`GET /api/search`, botão de lupa e Ctrl/Cmd+K): objetivos, gastos, universidades, programas,
  tarefas, eventos, itens da mala e parcelamentos, sem diferenciar acentos, ordenada por relevância; setas e Enter
  navegam. Gastos abrem a lista já filtrada (`/expenses?q=`).
- **Imagem única de produção** (`deploy/Dockerfile`): frontend compilado dentro do backend (`SpaConfig`), assets
  com cache imutável, rotas do React Router servidas pelo `index.html`, JVM ajustada para 512 MB.
- **Deploy contínuo gratuito** no Render + Neon: `render.yaml` (Blueprint), workflow `deploy.yml` por tag com
  smoke test, `scheduled-jobs.yml` disparando os jobs pelo endpoint interno protegido por token
  (`/api/internal/jobs/*`), guia `docs/DEPLOY.md`.
- **E2E com Playwright + axe** na imagem de produção, no desktop e no celular, também no CI.
- ADR 0012.

### Alterado
- Cores ajustadas para contraste WCAG AA (âmbar, coral, verde e cinza secundário) e nomes acessíveis iguais ao
  texto visível (marca no menu e cartão da meta do mês), corrigidos a partir da varredura do axe.

### Corrigido
- **"Hoje" no fuso do usuário em todos os serviços:** entre 21h e meia-noite (Brasília), candidaturas,
  solicitações, saldos de contas, saldo inicial do onboarding, pagamentos, lançamentos, próxima ocorrência de
  recorrências e janelas de inscrição usavam a data UTC, já do dia seguinte. Os testes de integração passaram a usar
  o mesmo fuso, para não falharem conforme o horário do CI.

## [0.9.0] — Fases 8 e 9: Notificações e importação do Excel — 2026-10-22

### Adicionado
- Migrations `V10__notifications` (notificações, preferências, alertas de câmbio, ShedLock) e `V11__import`.
- **Notificações:** detectores de prazos de inscrição e solicitações, vencimentos de gastos e parcelas, respostas
  esperadas, tarefas e marcos do objetivo; antecedências configuráveis por categoria; deduplicação no banco; job
  diário às 08:00 e geração ao abrir o app; sino com contador; central com "não lidas", marcar todas e excluir.
- **Alertas de câmbio** com histerese (avisa uma vez e rearma quando a cotação se afasta 0,5%), avaliados a cada
  atualização de cotação.
- **ShedLock** em todos os jobs agendados.
- **Importação do Excel:** prévia linha a linha das abas Informações dos Gastos, Análise de Universidades e Mala
  com as correções do plano (§3), escolha de abas, objetivo e o que fazer com o que já existe; importação
  transacional e idempotente; resumo com links.
- Teste de regressão opcional com a planilha real; ADR 0011.

### Alterado
- **Timeline só com o planejamento:** gastos com data, períodos das universidades e tarefas (além dos eventos
  manuais). Parcelas, renovações de assinaturas, pagamentos feitos e a data alvo do objetivo saíram da linha do
  tempo e dos "próximos" do dashboard.

## [0.7.0] — Fases 6 e 7: Timeline, tarefas e mala + ajustes — 2026-10-22

### Adicionado
- Migrations `V7__tasks_timeline`, `V8__luggage` e `V9__university_images_installments_in_progress`.
- **Timeline:** itens projetados de gastos (cada ocorrência), pagamentos, parcelas, renovações de assinaturas,
  candidaturas (abertura, prazo, resposta esperada/recebida), solicitações, tarefas e data alvo do objetivo, com
  ids determinísticos, status (feito/pendente/atrasado) e filtro por origem; eventos manuais (voo, prova,
  consulta…); "próximos" no dashboard. Tela editorial por mês com a data em numeral grande e lados alternados.
- **Tarefas:** prioridade, prazo, status, categoria livre e vínculos com objetivo, universidade, gasto e
  candidatura; concluir com um clique.
- **Mala:** malas com limite (atalhos para mão 10 kg, despachada 23 kg, item pessoal), itens com quantidade e
  peso em g ou kg, medidor com aviso acima de 90% e alerta acima de 100%, não alocados e checklist de embalados.
- **Fotos das universidades:** galeria com envio múltiplo (reduzidas no navegador), capa nos cards e na página da
  universidade, ampliar e excluir.
- **Metas pelos gastos:** a obrigatória e a completa lado a lado no dashboard e no objetivo.
- **Parcelamento em andamento:** "já pagas" + vencimento da próxima; os cards mostram quantas parcelas faltam; atalho
  "É parcelado?" no novo lançamento.
- ADR 0010.

### Corrigido
- Builds com o backend lento (máquina sob carga) marcavam o container como "unhealthy" antes de ele terminar de
  subir: tolerância de início do healthcheck aumentada para 3 minutos.

## [0.5.0] — Fases 4 e 5: Controle financeiro, Universidades e candidaturas — 2026-10-22

### Adicionado
- Migrations `V5__personal_finance` (categorias de receita/despesa semeadas para quem já existia, lançamentos,
  parcelamentos, recorrências, meta mensal) e `V6__universities_programs_applications` (universidades, programas,
  requisitos de idioma, janelas de inscrição, candidaturas, histórico de status e solicitações).
- **Controle financeiro:** lançamentos planejados/confirmados, parcelamentos com redistribuição ao editar,
  assinaturas e recorrências (semanal, mensal, anual, a cada N meses) materializadas por 36 meses com job diário e
  confirmação automática, meta de economia por mês, resumo do mês, visão de 12 meses, compromissos futuros e
  **projeção** de quando a meta do objetivo é alcançada.
- **Universidades:** universidades e programas com requisitos tri-estado, proficiência, custos e bolsa; janelas de
  inscrição como regra anual com a próxima janela calculada; filtros e busca sem acento; comparador de 2 a 5
  programas com destaque do melhor valor.
- **Candidaturas:** "Escolhida" cria/arquiva a candidatura, status com histórico imutável, solicitações da
  universidade que mudam o status automaticamente, estimativa de resposta em faixa (mín · provável · máx).
- Dashboard: linha da projeção e cartão de candidaturas (ativas, resultados e próximos prazos/respostas).
- Frontend: Finanças em abas (Mês, Parcelas, Assinaturas, Contas) com gráfico receitas × despesas, projeção no
  objetivo; Universidades (cards/tabela, filtros, seleção para comparar), detalhe da universidade, detalhe do
  programa em abas (Geral, Requisitos, Custos, Prazos, Candidatura), comparador e quadro de Candidaturas.
- ADRs 0008 (lançamentos materializados e projeção) e 0009 (universidades e candidaturas).

### Corrigido
- Contrato OpenAPI: requisitos de idioma e janelas de inscrição declaravam como obrigatórios campos que a API
  devolve `null`.
- Diálogos com listas carregadas da API (conta → objetivo, lançamento → categoria/conta, parcelamento e recorrência →
  categoria, programa → universidade) perdiam o valor selecionado se a lista chegasse depois da abertura; editar uma
  conta vinculada podia desvinculá-la do objetivo ao salvar. O formulário agora só monta com as listas carregadas.
- Mês exibido como "Outubro De 2026" (só a primeira letra é maiúscula).

## [0.3.0] — Fases 2 e 3: Objetivos, câmbio, contas e Gastos — 2026-10-21

### Adicionado
- Migrations `V2__exchange_rates`, `V3__objectives_accounts`, `V4__expenses` (com FKs compostas `(id, user_id)`: o banco impede vincular dados de usuários diferentes) e catálogo padrão para usuários existentes.
- **Câmbio:** provedores Frankfurter (BCE) e AwesomeAPI em ordem configurável, histórico de 180 dias na primeira execução, job a cada 6h, cache em memória, selo "desatualizada", cotação de reserva, cotação manual por usuário; `GET /api/exchange-rates/current|history`, `PUT|DELETE /api/exchange-rates/override`.
- **Objetivos:** CRUD, objetivo principal único (índice parcial), horizonte em meses, escopo da meta, `ObjectiveProgressCalculator` (meta, mobilizado, faltam, meses restantes, economia mensal necessária, no ritmo) e ajustes manuais da meta.
- **Contas:** contas por moeda vinculáveis a um objetivo, histórico de saldos (um por dia), resumo por moeda + total convertido.
- **Onboarding** em 6 passos (o valor já guardado vira a conta "Reserva do intercâmbio") e **dashboard** agregado numa chamada.
- **Gastos:** categorias e ciclos por usuário (semeados no cadastro com os nomes da planilha + Viagem e Transporte), periodicidade com ocorrências sugeridas pelo horizonte, pagamentos parciais em qualquer moeda com taxa capturada, cancelar/reativar, filtros, resumo por ciclo/categoria/periodicidade/status, `preview-total`.
- Frontend: dashboard com anel de progresso, saldos, cotação com histórico; telas de objetivos, finanças (contas), gastos (gráficos, filtros, detalhe com pagamentos) e configurações (cotação manual, categorias e ciclos); tela de erro de rota.

### Corrigido
- **403 em requisições do navegador via nginx/Vite** (vinha da Fase 1): o proxy enviava `Host` sem a porta e o Spring tratava a chamada como CORS. Agora `Host` é preservado (`$http_host`; `changeOrigin: false` no Vite).
- `<Button asChild>` quebrava a tela (o Slot do Radix exige um único filho).
- Contrato OpenAPI: campos de respostas agora obrigatórios e anuláveis de forma explícita (`X | null` no TypeScript).

## [0.1.0] — Fase 1: Fundação — 2026-10-21

### Adicionado
- Monorepo `backend/` (Spring Boot 4.1.1, Java 21) e `frontend/` (React 19 + Vite 8), `docker-compose.yml` com PostgreSQL 17, API e frontend (nginx), `.env.example`.
- Migration `V1__baseline_users_auth_currency.sql`: `app_user`, `user_settings`, `refresh_token`, `currency` (BRL, EUR).
- Autenticação: registro, login, refresh token opaco rotativo com detecção de reuso por família, logout, bloqueio após 5 falhas de login (429 + `Retry-After`), limpeza diária de tokens expirados.
- `GET /api/me`, `GET|PUT /api/me/settings`, `GET /api/currencies`.
- Erros em RFC 9457 Problem Details com `traceId` e `errors[]` por campo; `X-Request-Id` em todas as respostas.
- OpenAPI (springdoc) exportado para `backend/openapi/openapi.json` e tipos TypeScript gerados (`npm run gen:api`).
- Frontend: AppShell responsivo (sidebar no desktop, bottom nav + "Mais" no mobile), tema claro/escuro/sistema persistido na conta, tokens de design, login, cadastro, configurações, páginas de módulos futuros.
- Testes: unitários (JWT, bloqueio de login, UUIDv7), integração com Testcontainers (migrations, fluxo de auth, isolamento entre usuários, OpenAPI), Vitest (cliente HTTP, cadastro, tema).
- CI no GitHub Actions, publicação de imagens no GHCR, Dependabot, README e ADRs.
