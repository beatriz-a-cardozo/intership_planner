package com.interchangeplanner.importer;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.interchangeplanner.support.IntegrationTest;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/**
 * Regressão com a planilha REAL (plano técnico §1). Ela tem dados pessoais e não fica no repositório: o teste só roda
 * com {@code IMPORT_REGRESSION_FILE=/caminho/Gastos.xlsx}. Valores de referência com horizonte de 24 meses:
 * total R$ 347.893,28 → R$ 272.893,28 com o Sperrkonto; obrigatório R$ 275.793,28 → R$ 200.793,28; pago R$ 11.721,68;
 * custo mensal recorrente R$ 8.670.
 */
@EnabledIfEnvironmentVariable(named = "IMPORT_REGRESSION_FILE", matches = ".+")
class RealSpreadsheetIT extends IntegrationTest {

    @Test
    void totaisBatemComOsDaPlanilha() throws Exception {
        TestSession s = registerUser("real");
        fixEurBrl(s, "6");
        String objective = read(postAs(s, "/api/objectives", """
                {"name":"Mestrado na Alemanha","targetDate":"%s","currency":"BRL","targetMode":"DERIVED","horizonMonths":24}"""
                .formatted(LocalDate.now(USER_ZONE).plusMonths(18))).andExpect(status().isCreated()), "$.id");
        byte[] data = Files.readAllBytes(Path.of(System.getenv("IMPORT_REGRESSION_FILE")));
        String id = read(mvc.perform(MockMvcRequestBuilders.multipart("/api/imports")
                        .file(new MockMultipartFile("file", "Gastos.xlsx", "application/octet-stream", data))
                        .header(HttpHeaders.AUTHORIZATION, s.bearer()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sheets[0].rows.length()").value(53))
                .andExpect(jsonPath("$.sheets[0].errors").value(0))
                .andExpect(jsonPath("$.sheets[1].rows.length()").value(20))
                .andExpect(jsonPath("$.sheets[2].rows.length()").value(17)), "$.id");

        postAs(s, "/api/imports/{id}/commit", "{}", id)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.sheets[0].created").value(53))
                .andExpect(jsonPath("$.summary.sheets[1].created").value(20))
                .andExpect(jsonPath("$.summary.sheets[2].created").value(17));

        getAs(s, "/api/expenses/summary?objectiveId={id}", objective)
                .andExpect(jsonPath("$.totals.total.amount").value(347893.28))
                .andExpect(jsonPath("$.totals.totalAfterAdjustments.amount").value(272893.28))
                .andExpect(jsonPath("$.totals.required.amount").value(275793.28))
                .andExpect(jsonPath("$.totals.requiredAfterAdjustments.amount").value(200793.28))
                .andExpect(jsonPath("$.totals.paid.amount").value(11721.68))
                .andExpect(jsonPath("$.totals.monthlyRecurring.amount").value(8670.0));
        getAs(s, "/api/universities").andExpect(jsonPath("$.length()").value(19));
    }
}
