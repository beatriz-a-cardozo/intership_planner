package com.interchangeplanner.user;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.interchangeplanner.auth.application.AccessTokenService;
import com.interchangeplanner.support.IntegrationTest;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

class UserSettingsAndIsolationIT extends IntegrationTest {

    @Autowired
    AccessTokenService accessTokens;

    private static String settingsJson(String theme, String locale, String tz, String primary, String secondary) {
        return """
                {"theme":"%s","locale":"%s","timezone":"%s","primaryCurrency":"%s","secondaryCurrency":"%s"}"""
                .formatted(theme, locale, tz, primary, secondary);
    }

    @Test
    void atualizaPreferenciasDoProprioUsuario() throws Exception {
        TestSession bia = registerUser("settings");
        mvc.perform(put("/api/me/settings")
                        .header(HttpHeaders.AUTHORIZATION, bia.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(settingsJson("DARK", "pt-BR", "Europe/Berlin", "EUR", "BRL")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.theme").value("DARK"))
                .andExpect(jsonPath("$.primaryCurrency").value("EUR"));

        mvc.perform(get("/api/me/settings").header(HttpHeaders.AUTHORIZATION, bia.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.timezone").value("Europe/Berlin"));
    }

    @Test
    void validaRegrasDasPreferencias() throws Exception {
        TestSession bia = registerUser("rules");
        mvc.perform(put("/api/me/settings")
                        .header(HttpHeaders.AUTHORIZATION, bia.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(settingsJson("LIGHT", "pt-BR", "America/Sao_Paulo", "BRL", "BRL")))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errors[0].field").value("secondaryCurrency"));

        mvc.perform(put("/api/me/settings")
                        .header(HttpHeaders.AUTHORIZATION, bia.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(settingsJson("LIGHT", "pt-BR", "Marte/Olympus", "BRL", "EUR")))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errors[0].field").value("timezone"));

        mvc.perform(put("/api/me/settings")
                        .header(HttpHeaders.AUTHORIZATION, bia.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(settingsJson("LIGHT", "pt-BR", "America/Sao_Paulo", "USD", "EUR")))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errors[0].field").value("primaryCurrency"));

        mvc.perform(put("/api/me/settings")
                        .header(HttpHeaders.AUTHORIZATION, bia.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(settingsJson("ROXO", "pt-BR", "America/Sao_Paulo", "BRL", "EUR")))
                .andExpect(status().isBadRequest());
    }

    /** Isolamento: cada token só enxerga e altera os dados do próprio usuário. */
    @Test
    void usuarioBNuncaAlteraNemVeDadosDeA() throws Exception {
        TestSession a = registerUser("alice");
        TestSession b = registerUser("bruno");

        mvc.perform(put("/api/me/settings")
                        .header(HttpHeaders.AUTHORIZATION, b.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(settingsJson("DARK", "en-US", "UTC", "EUR", "BRL")))
                .andExpect(status().isOk());

        mvc.perform(get("/api/me").header(HttpHeaders.AUTHORIZATION, a.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(a.userId().toString()))
                .andExpect(jsonPath("$.email").value(a.email().toLowerCase()))
                .andExpect(jsonPath("$.settings.theme").value("SYSTEM"))
                .andExpect(jsonPath("$.settings.primaryCurrency").value("BRL"));

        mvc.perform(get("/api/me").header(HttpHeaders.AUTHORIZATION, b.bearer()))
                .andExpect(jsonPath("$.id").value(b.userId().toString()))
                .andExpect(jsonPath("$.settings.theme").value("DARK"));
    }

    @Test
    void tokenValidoDeUsuarioInexistenteRetorna404() throws Exception {
        String token = accessTokens.issue(UUID.randomUUID()).value();
        mvc.perform(get("/api/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound());
    }
}
