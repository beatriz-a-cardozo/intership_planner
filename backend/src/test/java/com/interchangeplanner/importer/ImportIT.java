package com.interchangeplanner.importer;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.interchangeplanner.support.IntegrationTest;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/** Importação da planilha: prévia, regras de normalização, importar, idempotência e isolamento (Fase 9). */
class ImportIT extends IntegrationTest {

    static final LocalDate TODAY = LocalDate.now(USER_ZONE);

    TestSession bia;
    String objective;

    ResultActions upload(TestSession s, byte[] data, String name) throws Exception {
        return mvc.perform(MockMvcRequestBuilders.multipart("/api/imports")
                .file(new MockMultipartFile("file", name,
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", data))
                .header(HttpHeaders.AUTHORIZATION, s.bearer()));
    }

    @BeforeEach
    void setUp() throws Exception {
        bia = registerUser("imp");
        fixEurBrl(bia, "6");
        objective = read(postAs(bia, "/api/objectives", """
                {"name":"Mestrado","targetDate":"%s","currency":"BRL","targetMode":"DERIVED","horizonMonths":24}"""
                .formatted(TODAY.plusMonths(20))).andExpect(status().isCreated()), "$.id");
    }

    @Test
    void previaMostraCadaLinhaComAvisosEErros() throws Exception {
        upload(bia, TestWorkbook.build(TODAY), "Gastos.xlsx")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PREVIEW"))
                .andExpect(jsonPath("$.ignoredSheets").value(hasItem("economia")))
                .andExpect(jsonPath("$.sheets[0].sheet").value("EXPENSES"))
                .andExpect(jsonPath("$.sheets[0].rows", hasSize(11)))
                .andExpect(jsonPath("$.sheets[0].errors").value(1))
                .andExpect(jsonPath("$.sheets[0].rows[?(@.label == 'Seguro trimestral')].status").value(hasItem("ERROR")))
                .andExpect(jsonPath("$.sheets[0].rows[?(@.label == 'TOEFL iBT')].messages[0]")
                        .value(hasItem(containsString("unificada com \"Proficiência\""))))
                .andExpect(jsonPath("$.sheets[0].rows[?(@.label == 'Passagem Aérea (IDA)')].messages[0]")
                        .value(hasItem(containsString("movida para"))))
                .andExpect(jsonPath("$.sheets[0].rows[?(@.label == 'Ração do gato')].messages[0]")
                        .value(hasItem(containsString("será criada"))))
                // a linha só com URL é descartada: 3 programas
                .andExpect(jsonPath("$.sheets[1].rows", hasSize(3)))
                .andExpect(jsonPath("$.sheets[1].rows[?(@.label =~ /.*Saarlandes.*/)].messages[*]")
                        .value(hasItem(containsString("considerada pública"))))
                .andExpect(jsonPath("$.sheets[2].rows", hasSize(2)));
        // nada foi gravado na prévia
        getAs(bia, "/api/expenses?objectiveId={id}", objective).andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void importaComAsRegrasDaPlanilhaEEhIdempotente() throws Exception {
        String id = read(upload(bia, TestWorkbook.build(TODAY), "Gastos.xlsx"), "$.id");
        postAs(bia, "/api/imports/{id}/commit", "{\"duplicates\":\"SKIP\"}", id)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMMITTED"))
                .andExpect(jsonPath("$.summary.sheets[0].created").value(10))
                .andExpect(jsonPath("$.summary.sheets[1].created").value(3))
                .andExpect(jsonPath("$.summary.sheets[2].created").value(2))
                .andExpect(jsonPath("$.summary.notes").value(hasItem(containsString("Pets"))))
                .andExpect(jsonPath("$.summary.notes").value(hasItem(containsString("Sperrkonto"))))
                .andExpect(jsonPath("$.summary.notes").value(hasItem("2 universidades criadas.")));

        // total = 9500 + 1317,68 + 219,80 (=314*0.7) + 75000 + 2700×24 + 2400×4 + 500×2 + 0 + 4500 + 100
        getAs(bia, "/api/expenses/summary?objectiveId={id}", objective)
                .andExpect(jsonPath("$.totals.total.amount").value(166037.48))
                .andExpect(jsonPath("$.totals.adjustments.amount").value(-75000.0))
                .andExpect(jsonPath("$.totals.totalAfterAdjustments.amount").value(91037.48))
                .andExpect(jsonPath("$.totals.paid.amount").value(11041.68))
                .andExpect(jsonPath("$.totals.count").value(10));
        getAs(bia, "/api/expenses?objectiveId={id}&q=Passagem", objective)
                .andExpect(jsonPath("$[0].dueDate").value(TODAY.minusMonths(5).plusYears(1).toString()));
        getAs(bia, "/api/expenses?objectiveId={id}&q=prova", objective)
                .andExpect(jsonPath("$[0].unitPrice").value(219.8))
                .andExpect(jsonPath("$[0].status").value("PAID"));

        getAs(bia, "/api/programs?q=Informatik")
                .andExpect(jsonPath("$[0].degree").value("M.Sc."))
                .andExpect(jsonPath("$[0].applicationChannel").value("UNI_ASSIST"))
                .andExpect(jsonPath("$[0].languageRequirements[0].testName").value("TOEFL iBT"))
                .andExpect(jsonPath("$[0].intakes", hasSize(2)));
        getAs(bia, "/api/programs?q=Engineering")
                .andExpect(jsonPath("$[0].name").value("Software Engineering"))
                .andExpect(jsonPath("$[0].portalName").value("Augsburg"));
        getAs(bia, "/api/luggage/summary")
                .andExpect(jsonPath("$.bags[0].name").value("Mala de Mão"))
                .andExpect(jsonPath("$.bags[0].type").value("CARRY_ON"))
                .andExpect(jsonPath("$.bags[0].weightG").value(300));

        // a mesma sessão não importa duas vezes
        postAs(bia, "/api/imports/{id}/commit", "{}", id).andExpect(status().isConflict());

        // importar a mesma planilha de novo com "pular" não cria nada
        String again = read(upload(bia, TestWorkbook.build(TODAY), "Gastos.xlsx")
                .andExpect(jsonPath("$.sheets[0].duplicates").value(10)), "$.id");
        postAs(bia, "/api/imports/{id}/commit", "{\"duplicates\":\"SKIP\"}", again)
                .andExpect(jsonPath("$.summary.sheets[0].created").value(0))
                .andExpect(jsonPath("$.summary.sheets[0].skipped").value(10))
                .andExpect(jsonPath("$.summary.sheets[1].skipped").value(3))
                .andExpect(jsonPath("$.summary.sheets[2].skipped").value(2));
        getAs(bia, "/api/expenses/summary?objectiveId={id}", objective)
                .andExpect(jsonPath("$.totals.count").value(10))
                .andExpect(jsonPath("$.totals.adjustments.amount").value(-75000.0));
    }

    @Test
    void escolheAbasEAtualizaDuplicados() throws Exception {
        String id = read(upload(bia, TestWorkbook.build(TODAY), "Gastos.xlsx"), "$.id");
        postAs(bia, "/api/imports/{id}/commit", "{\"sheets\":[\"LUGGAGE\"]}", id)
                .andExpect(jsonPath("$.summary.sheets", hasSize(1)));
        getAs(bia, "/api/expenses?objectiveId={id}", objective).andExpect(jsonPath("$", hasSize(0)));

        String again = read(upload(bia, TestWorkbook.build(TODAY), "Gastos.xlsx"), "$.id");
        postAs(bia, "/api/imports/{id}/commit", "{\"sheets\":[\"LUGGAGE\"],\"duplicates\":\"UPDATE\"}", again)
                .andExpect(jsonPath("$.summary.sheets[0].updated").value(2));
    }

    @Test
    void recusaArquivoInvalidoEIsolaSessoes() throws Exception {
        upload(bia, "nome;valor\nx;1".getBytes(), "dados.csv")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errors[0].field").value("file"));
        String id = read(upload(bia, TestWorkbook.build(TODAY), "Gastos.xlsx"), "$.id");
        TestSession leo = registerUser("imp-b");
        getAs(leo, "/api/imports/{id}", id).andExpect(status().isNotFound());
        postAs(leo, "/api/imports/{id}/commit", "{}", id).andExpect(status().isNotFound());
    }
}
