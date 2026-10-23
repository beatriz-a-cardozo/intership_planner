# ADR 0012 — Busca em memória, imagem única de deploy e jobs disparados de fora

**Status:** aceita · 2026-10-22

## Contexto
A Fase 10 fecha o projeto: busca global, acessibilidade, testes ponta a ponta e publicação numa plataforma
gratuita. O app é pessoal (uma pessoa, centenas de registros, não milhões) e o orçamento de hospedagem é zero.

## Decisão — busca global
- **Em memória, no backend:** `GET /api/search?q=` carrega os registros do usuário (objetivos, gastos,
  universidades, programas, tarefas, eventos, itens da mala e parcelamentos) e compara textos normalizados por
  `TextKey` (sem acento, minúsculas, espaços colapsados), o mesmo normalizador da importação.
- Pontuação simples: o título começa com o termo (3), uma palavra do título começa com ele (2), contém no título
  ou no subtítulo (1). Até 6 resultados por tipo; mínimo de 2 letras.
- Cada resultado já traz a URL da tela que o mostra; o frontend só navega. Gastos abrem a lista filtrada
  (`/expenses?q=`).
- **Alternativas descartadas:** `pg_trgm`/`tsvector` exigiriam índices, `unaccent` e consultas por tabela para um
  volume que cabe em memória; um motor de busca externo seria mais um serviço para manter. Se o volume crescer,
  a troca fica restrita ao `SearchService`.

## Decisão — imagem única
- `deploy/Dockerfile` compila o frontend com `VITE_API_BASE_URL=""` e o coloca em `classpath:/static` do backend.
  O `SpaConfig` (ativo só quando o `index.html` existe) serve `/assets/**` com cache de 1 ano imutável,
  `index.html` sem cache e devolve o `index.html` para as rotas do React Router.
- **Por quê:** um serviço gratuito em vez de dois (só um "acorda" no primeiro acesso), mesmo domínio para páginas e
  API (cookie de refresh `SameSite=Strict` funciona sem CORS nem proxy entre domínios) e um único artefato testado.
- O Docker Compose de desenvolvimento continua com nginx + backend separados; as duas formas convivem.
- Segurança: GET/HEAD fora de `/api`, `/actuator`, `/v3`, `/swagger-ui` e `/error` são públicos (são arquivos
  estáticos); todo dado continua atrás do JWT.

## Decisão — jobs disparados de fora
- No plano gratuito o serviço dorme sem acesso, e o `@Scheduled` dorme junto. Um workflow agendado do GitHub
  Actions chama `POST /api/internal/jobs/{daily|exchange}` com o header `X-Job-Token`.
- O token é comparado em tempo constante; sem token configurado o endpoint responde 404 (fica desligado).
- Os jobs já usavam ShedLock e são idempotentes (notificações deduplicadas no banco, materialização por chave),
  então o agendador interno e o externo podem coexistir.

## Decisão — CD e qualidade
- Deploy só por tag `v*` (ou manual), depois do CI completo; `autoDeploy` do Render desligado.
- O CI roda Playwright na **mesma imagem** que vai para produção, com axe (WCAG 2.1 A/AA) nas telas principais,
  no desktop e no celular. A primeira varredura encontrou contraste insuficiente nos tons âmbar, coral, verde e
  cinza claro e nomes acessíveis diferentes do texto visível; os tokens de cor foram ajustados para ≥ 4,5:1.

## Consequências
- O primeiro acesso depois de um tempo parado leva cerca de 50 s (limite do plano gratuito, documentado).
- O GitHub desativa crons de repositórios inativos por 60 dias; reativar é um clique (documentado em
  `docs/DEPLOY.md`).
- Trocar de plataforma exige só um serviço que rode a imagem Docker com as mesmas variáveis de ambiente.
