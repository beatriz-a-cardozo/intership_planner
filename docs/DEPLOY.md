# Deploy gratuito: Render + Neon

A aplicação vai para a internet como **um único serviço**: a imagem `deploy/Dockerfile` compila o frontend e o
coloca dentro do backend, que serve as páginas e a API no mesmo endereço (sem CORS, cookie de refresh `SameSite=Strict`
funcionando). O banco é um PostgreSQL gratuito do **Neon**. Motivos e alternativas no
[ADR 0012](adr/0012-busca-imagem-unica-e-jobs-externos.md).

```
GitHub (tag v*) ──▶ Actions: CI completo + E2E ──▶ Deploy Hook ──▶ Render (Docker, plano free)
                                                                     │  https://<nome>.onrender.com
                                                                     ▼
                                                                Neon PostgreSQL (free)
GitHub Actions (cron) ──▶ POST /api/internal/jobs/{daily|exchange}  (X-Job-Token)
```

Custo: **zero**. Tempo total: uns 20 minutos, a maior parte esperando o primeiro build.

> Os passos abaixo envolvem criar contas e copiar segredos nos sites oficiais. Nenhum segredo entra no
> repositório; eles ficam no Render e nos *secrets* do GitHub.

---

## 0. Antes de começar

1. Todo o código (incluindo `render.yaml`, `deploy/` e `.github/workflows/`) precisa estar na branch **`main`** do
   GitHub. O Render lê o `render.yaml` dessa branch.
2. O CI da `main` deve estar verde (Actions → **CI**).

## 1. Banco no Neon

1. Entre em <https://neon.tech> com a conta do GitHub e crie um projeto:
   - **Postgres version:** 17
   - **Region:** *AWS Europe Central 1 (Frankfurt)*, a mesma região do Render, para a latência ficar baixa.
2. No painel do projeto, clique em **Connect**. Desmarque **Connection pooling** (o app já tem o pool HikariCP e o
   Flyway prefere conexão direta) e copie os dados:

   | No Neon | Vira, no Render |
   |---|---|
   | host (`ep-xxxx.eu-central-1.aws.neon.tech`) e banco (`neondb`) | `DB_URL` = `jdbc:postgresql://ep-xxxx.eu-central-1.aws.neon.tech/neondb?sslmode=require` |
   | usuário (`neondb_owner`) | `DB_USERNAME` |
   | senha | `DB_PASSWORD` |

   Repare que o `DB_URL` começa com `jdbc:postgresql://` e **não** leva usuário e senha (eles vão separados).

As tabelas são criadas sozinhas pelo Flyway na primeira inicialização.

## 2. Serviço no Render

1. Entre em <https://render.com> com a conta do GitHub e autorize o acesso ao repositório.
2. **New → Blueprint**, escolha o repositório e a branch `main`. O Render lê o `render.yaml` e mostra o serviço
   `intercambio-planner` (Docker, plano Free, Frankfurt).
3. Preencha as três variáveis pedidas (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`) com os valores do passo 1.
   `JWT_SECRET` e `INTERNAL_JOB_TOKEN` são gerados pelo próprio Render; não precisa mexer.
4. **Apply**. O primeiro build leva de 5 a 10 minutos. Quando terminar, abra o endereço do serviço
   (`https://intercambio-planner.onrender.com` ou parecido) e crie sua conta.
5. Ainda no serviço:
   - **Settings → Deploy Hook:** copie a URL (é um segredo: quem tiver a URL dispara deploys).
   - **Environment:** revele e copie o valor de `INTERNAL_JOB_TOKEN`.

## 3. Segredos no GitHub

No repositório: **Settings → Secrets and variables → Actions**.

| Aba | Nome | Valor |
|---|---|---|
| Secrets | `RENDER_DEPLOY_HOOK_URL` | a URL do Deploy Hook |
| Secrets | `INTERNAL_JOB_TOKEN` | o mesmo valor de `INTERNAL_JOB_TOKEN` no Render |
| Variables | `APP_URL` | o endereço público, sem barra no fim (`https://intercambio-planner.onrender.com`) |

## 4. Publicar uma versão

O deploy acontece quando uma tag `v*` é enviada:

```bash
git tag v1.0.0
git push origin v1.0.0
```

O workflow **Deploy** roda o CI completo (backend com Testcontainers, frontend e E2E com axe na própria imagem de
deploy), chama o Deploy Hook, espera a versão nova responder em `/actuator/info` e faz um *smoke test* (página
inicial, uma rota do React, `/api/currencies` e `/api/me` respondendo 401). Também dá para rodar manualmente em
Actions → **Deploy** → *Run workflow*.

O `autoDeploy` do Render fica desligado de propósito: nada vai para o ar sem o CI verde.

## 5. Jobs agendados

No plano gratuito o serviço dorme depois de 15 minutos sem acesso, e o agendador interno dorme junto. O workflow
**Jobs agendados** acorda a aplicação e dispara os jobs pelo endpoint interno (protegido pelo token):

| Quando (Brasília) | Job | O que faz |
|---|---|---|
| todo dia às 08:00 | `daily` | materializa recorrências, gera as notificações do dia, limpa refresh tokens vencidos |
| a cada 6 h | `exchange` | atualiza as cotações e avalia os alertas de câmbio |

Os jobs usam ShedLock: se o agendador interno também estiver acordado, nada roda duas vezes. Enquanto `APP_URL` e
`INTERNAL_JOB_TOKEN` não existirem, o workflow termina sem fazer nada.

## Limites do plano gratuito

- **Primeiro acesso lento:** depois de 15 min parado, o Render leva cerca de 50 s para acordar o serviço. O Neon
  também suspende o banco depois de 5 min sem uso, mas acorda em menos de 1 s.
- **Memória:** 512 MB. A JVM está ajustada para isso (`deploy/Dockerfile`); em teste, o app usa cerca de 320 MB.
- **750 horas/mês** de serviço no Render: suficiente para um serviço ligado o mês inteiro.
- **0,5 GB de banco** no Neon: sobra para uso pessoal (fotos das universidades são reduzidas antes do envio).
- O GitHub **desativa crons** de repositórios sem nenhuma atividade por 60 dias. Se isso acontecer, reative em
  Actions → **Jobs agendados** → *Enable workflow*.

## Problemas comuns

| Sintoma | Causa provável |
|---|---|
| Deploy falha no Render com `password authentication failed` | `DB_USERNAME`/`DB_PASSWORD` trocados ou com espaço sobrando |
| `The connection attempt failed` | `DB_URL` sem `jdbc:` ou sem `?sslmode=require`, ou o host do *pooler* em vez do direto |
| Login funciona, mas ao recarregar a página pede login de novo | `REFRESH_COOKIE_SECURE` precisa ser `true` (HTTPS); já vem assim no `render.yaml` |
| Workflow **Deploy** falha em "Configuração presente" | faltou `RENDER_DEPLOY_HOOK_URL` ou `APP_URL` (passo 3) |
| **Jobs agendados** responde 401 | o segredo `INTERNAL_JOB_TOKEN` do GitHub não é igual ao do Render |

## Rodar a imagem de produção localmente

```bash
docker build -f deploy/Dockerfile -t interchange-planner .
docker run --rm -p 8080:8080 -e DB_URL=jdbc:postgresql://host.docker.internal:5432/interchange_planner \
  -e DB_USERNAME=interchange -e DB_PASSWORD=... -e JWT_SECRET="$(openssl rand -hex 32)" interchange-planner
```

E os testes E2E contra ela: `cd frontend && E2E_BASE_URL=http://localhost:8080 npm run e2e`.
