package com.interchangeplanner.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.interchangeplanner.support.IntegrationTest;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

class AuthFlowIT extends IntegrationTest {

    @Nested
    class Register {

        @Test
        void criaContaComPreferenciasPadraoEIniciaSessao() throws Exception {
            String email = uniqueEmail("Bia");
            MvcResult result = mvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(registerJson("  Beatriz  ", "  " + email.toUpperCase() + " ", PASSWORD)))
                    .andExpect(status().isCreated())
                    .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                    .andExpect(jsonPath("$.tokenType").value("Bearer"))
                    .andExpect(jsonPath("$.expiresIn").value(900))
                    .andExpect(jsonPath("$.accessToken", notNullValue()))
                    .andExpect(jsonPath("$.user.email").value(email.toLowerCase()))
                    .andExpect(jsonPath("$.user.displayName").value("Beatriz"))
                    .andExpect(jsonPath("$.user.onboardingCompleted").value(false))
                    .andExpect(jsonPath("$.user.settings.theme").value("SYSTEM"))
                    .andExpect(jsonPath("$.user.settings.primaryCurrency").value("BRL"))
                    .andExpect(jsonPath("$.user.settings.secondaryCurrency").value("EUR"))
                    .andExpect(jsonPath("$.user.settings.timezone").value("America/Sao_Paulo"))
                    .andExpect(jsonPath("$.refreshToken").doesNotExist())
                    .andReturn();

            String setCookie = result.getResponse().getHeader(HttpHeaders.SET_COOKIE);
            assertThat(setCookie)
                    .startsWith(REFRESH_COOKIE + "=")
                    .contains("HttpOnly", "Secure", "SameSite=Strict", "Path=/api/auth", "Max-Age=2592000");
        }

        @Test
        void recusaEmailDuplicadoIgnorandoMaiusculas() throws Exception {
            TestSession existing = registerUser("dup");
            mvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(registerJson("Outra", existing.email().toUpperCase(), PASSWORD)))
                    .andExpect(status().isConflict())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.errors[0].field").value("email"))
                    .andExpect(jsonPath("$.errors[0].message").value("E-mail já cadastrado."))
                    .andExpect(jsonPath("$.traceId", notNullValue()));
        }

        @Test
        void validaCamposComProblemDetails() throws Exception {
            mvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(registerJson("", "nao-e-email", "curta")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.title").value("Dados inválidos"))
                    .andExpect(jsonPath("$.errors[?(@.field == 'email')]").exists())
                    .andExpect(jsonPath("$.errors[?(@.field == 'password')]").exists())
                    .andExpect(jsonPath("$.errors[?(@.field == 'displayName')]").exists());
        }

        @Test
        void exigeSenhaComLetrasENumeros() throws Exception {
            mvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(registerJson("Bia", uniqueEmail("pw"), "somenteletras")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].field").value("password"));
        }

        @Test
        void jsonMalformadoViraProblemDetails() throws Exception {
            mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("{"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.traceId", notNullValue()));
        }
    }

    @Nested
    class Login {

        @Test
        void autenticaEAcessaRotaProtegida() throws Exception {
            TestSession registered = registerUser("login");
            MvcResult result = mvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginJson(registered.email().toUpperCase(), PASSWORD)))
                    .andExpect(status().isOk())
                    .andReturn();
            TestSession session = sessionFrom(result, registered.email());

            mvc.perform(get("/api/me").header(HttpHeaders.AUTHORIZATION, session.bearer()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(registered.userId().toString()));
        }

        @Test
        void mesmaRespostaParaEmailInexistenteESenhaErrada() throws Exception {
            TestSession registered = registerUser("wrong");
            String wrongPassword = mvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginJson(registered.email(), "senhaErrada9")))
                    .andExpect(status().isUnauthorized())
                    .andReturn().getResponse().getContentAsString();
            String unknownEmail = mvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginJson(uniqueEmail("ghost"), "senhaErrada9")))
                    .andExpect(status().isUnauthorized())
                    .andReturn().getResponse().getContentAsString();

            assertThat(wrongPassword).contains("E-mail ou senha incorretos.");
            assertThat(unknownEmail).contains("E-mail ou senha incorretos.");
        }

        @Test
        void bloqueiaAposFalhasSeguidas() throws Exception {
            TestSession registered = registerUser("brute");
            for (int i = 0; i < 5; i++) {
                mvc.perform(post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(loginJson(registered.email(), "senhaErrada9")))
                        .andExpect(status().isUnauthorized());
            }
            // Bloqueado mesmo com a senha correta
            mvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginJson(registered.email(), PASSWORD)))
                    .andExpect(status().isTooManyRequests())
                    .andExpect(header().exists(HttpHeaders.RETRY_AFTER))
                    .andExpect(jsonPath("$.detail", containsString("Tente novamente")));
        }
    }

    @Nested
    class Refresh {

        @Test
        void rotacionaOTokenARCadaUso() throws Exception {
            TestSession session = registerUser("rotate");

            MvcResult result = mvc.perform(post("/api/auth/refresh").cookie(session.refreshCookie()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.user.id").value(session.userId().toString()))
                    .andReturn();
            TestSession renewed = sessionFrom(result, session.email());

            assertThat(renewed.refreshCookie().getValue()).isNotEqualTo(session.refreshCookie().getValue());
            mvc.perform(get("/api/me").header(HttpHeaders.AUTHORIZATION, renewed.bearer()))
                    .andExpect(status().isOk());
        }

        @Test
        void reusoDeTokenRotacionadoRevogaAFamiliaInteira() throws Exception {
            TestSession session = registerUser("reuse");
            Cookie original = session.refreshCookie();

            MvcResult first = mvc.perform(post("/api/auth/refresh").cookie(original))
                    .andExpect(status().isOk())
                    .andReturn();
            Cookie rotated = first.getResponse().getCookie(REFRESH_COOKIE);

            // Atacante reapresenta o token antigo
            MvcResult reuse = mvc.perform(post("/api/auth/refresh").cookie(original))
                    .andExpect(status().isUnauthorized())
                    .andReturn();
            assertThat(reuse.getResponse().getHeader(HttpHeaders.SET_COOKIE)).contains("Max-Age=0");

            // O token legítimo mais recente também deixou de valer
            mvc.perform(post("/api/auth/refresh").cookie(rotated))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void semCookieOuComTokenDesconhecidoRetorna401() throws Exception {
            mvc.perform(post("/api/auth/refresh"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.title").value("Sessão expirada"));
            mvc.perform(post("/api/auth/refresh").cookie(new Cookie(REFRESH_COOKIE, "inventado")))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    class Logout {

        @Test
        void revogaORefreshTokenELimpaOCookie() throws Exception {
            TestSession session = registerUser("logout");

            MvcResult result = mvc.perform(post("/api/auth/logout").cookie(session.refreshCookie()))
                    .andExpect(status().isNoContent())
                    .andReturn();
            assertThat(result.getResponse().getHeader(HttpHeaders.SET_COOKIE)).contains("Max-Age=0");

            mvc.perform(post("/api/auth/refresh").cookie(session.refreshCookie()))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void eIdempotenteSemCookie() throws Exception {
            mvc.perform(post("/api/auth/logout")).andExpect(status().isNoContent());
        }
    }

    @Nested
    class AccessToken {

        @Test
        void rotaProtegidaSemTokenRetorna401EmProblemDetails() throws Exception {
            mvc.perform(get("/api/me"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"))
                    .andExpect(jsonPath("$.status").value(401))
                    .andExpect(jsonPath("$.traceId", notNullValue()));
        }

        @Test
        void tokenAdulteradoRetorna401() throws Exception {
            TestSession session = registerUser("tamper");
            String tampered = session.accessToken().substring(0, session.accessToken().length() - 2) + "xx";
            mvc.perform(get("/api/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + tampered))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void requestIdEhPropagadoParaResposta() throws Exception {
            mvc.perform(get("/api/currencies").header("X-Request-Id", "abc12345-trace"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("X-Request-Id", "abc12345-trace"));
        }
    }
}
