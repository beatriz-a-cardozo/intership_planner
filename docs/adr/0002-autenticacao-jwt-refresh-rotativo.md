# ADR 0002 — JWT curto + refresh token opaco rotativo em cookie

**Status:** aceita · 2026-10-21

## Contexto
SPA e API no mesmo domínio (nginx/Vite fazem proxy de `/api`). Precisamos de sessão longa e confortável no
celular sem expor credenciais duráveis a JavaScript (XSS).

## Decisão
- **Access token**: JWT HS256 de 15 min, emitido com `NimbusJwtEncoder` e validado pelo Spring Security OAuth2
  Resource Server (nada de parsing manual). Guardado só em memória no frontend.
- **Refresh token**: 32 bytes aleatórios, persistido apenas como hash SHA-256, enviado em cookie
  `HttpOnly; Secure; SameSite=Strict; Path=/api/auth`. Cada uso rotaciona o token.
- **Detecção de reuso**: tokens do mesmo login compartilham `family_id`; apresentar um token já revogado
  revoga a família inteira.
- CSRF desabilitado: só `/api/auth/refresh` e `/logout` aceitam o cookie, com SameSite=Strict e path restrito,
  e o corpo da resposta não é legível por outra origem (CORS).
- Bloqueio de login em memória após N falhas por e-mail.

## Consequências
- Roubo de access token tem janela curta; roubo de refresh token é detectado no primeiro uso concorrente.
- Duas abas renovando exatamente ao mesmo tempo com o mesmo cookie podem disparar a detecção de reuso;
  o cliente faz refresh *single-flight* por aba, o que torna isso raro. Se virar problema, adicionar uma
  janela de tolerância curta.
- O bloqueio de login é por instância; com várias instâncias, migrar para um store compartilhado.
- Trocar HS256 por RS256 afeta apenas `JwtConfig`.
