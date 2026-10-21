package com.interchangeplanner.support;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Base dos testes de integração: contexto completo + PostgreSQL real (Testcontainers) + MockMvc.
 * O contexto (e o container) é compartilhado entre as classes de teste; cada teste usa e-mails únicos.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(PostgresContainerConfig.class)
public abstract class IntegrationTest {

    public static final String REFRESH_COOKIE = "ip_refresh";
    public static final String PASSWORD = "senhaForte1";

    @Autowired
    protected MockMvc mvc;

    /** Sessão de um usuário de teste: access token + cookie de refresh. */
    public record TestSession(UUID userId, String email, String accessToken, Cookie refreshCookie) {

        public String bearer() {
            return "Bearer " + accessToken;
        }
    }

    protected static String uniqueEmail(String prefix) {
        return prefix + "-" + UUID.randomUUID() + "@example.com";
    }

    protected static String registerJson(String name, String email, String password) {
        return """
                {"displayName":"%s","email":"%s","password":"%s"}""".formatted(name, email, password);
    }

    protected static String loginJson(String email, String password) {
        return """
                {"email":"%s","password":"%s"}""".formatted(email, password);
    }

    protected TestSession registerUser(String prefix) throws Exception {
        String email = uniqueEmail(prefix);
        MvcResult result = mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson(prefix, email, PASSWORD)))
                .andExpect(status().isCreated())
                .andReturn();
        return sessionFrom(result, email);
    }

    protected static TestSession sessionFrom(MvcResult result, String email) throws Exception {
        String body = result.getResponse().getContentAsString();
        String token = JsonPath.read(body, "$.accessToken");
        String userId = JsonPath.read(body, "$.user.id");
        Cookie cookie = result.getResponse().getCookie(REFRESH_COOKIE);
        return new TestSession(UUID.fromString(userId), email, token, cookie);
    }
}
