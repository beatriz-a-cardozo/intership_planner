package com.interchangeplanner.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.interchangeplanner.support.IntegrationTest;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Exporta o contrato OpenAPI para {@code backend/openapi/openapi.json}. O frontend gera os tipos
 * TypeScript a partir desse arquivo ({@code npm run gen:api}); o CI falha se algum dos dois ficar desatualizado.
 */
class OpenApiSpecIT extends IntegrationTest {

    static final Path OUTPUT = Path.of("openapi", "openapi.json");

    @Test
    void exportaContratoOpenApi() throws Exception {
        String spec = mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertThat(spec).contains("\"/api/auth/login\"", "\"/api/me\"", "\"AuthResponse\"");
        // Convenção: campos de *Response são obrigatórios no contrato (tipos TS não opcionais)
        List<String> meRequired = JsonPath.read(spec, "$.components.schemas.MeResponse.required");
        assertThat(meRequired).containsExactly("displayName", "email", "id", "onboardingCompleted", "settings");
        String normalized = spec.strip() + "\n";

        Files.createDirectories(OUTPUT.getParent());
        Files.writeString(OUTPUT, normalized, StandardCharsets.UTF_8);
    }
}
