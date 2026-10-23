package com.interchangeplanner.search;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.interchangeplanner.support.IntegrationTest;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/** Busca global (Fase 10) e endpoint interno dos jobs. */
class SearchIT extends IntegrationTest {

    @Test
    void buscaSemAcentoEmVariosTiposEOrdenaPeloInicio() throws Exception {
        TestSession bia = registerUser("busca");
        String uni = read(postAs(bia, "/api/universities", "{\"name\":\"Technische Universität München\",\"city\":\"München\"}"),
                "$.id");
        postAs(bia, "/api/programs", "{\"universityId\":\"%s\",\"name\":\"Informatik\"}".formatted(uni))
                .andExpect(status().isCreated());
        postAs(bia, "/api/tasks", "{\"title\":\"Pedir histórico na faculdade\"}").andExpect(status().isCreated());
        postAs(bia, "/api/luggage/items", "{\"name\":\"Histórico escolar\"}").andExpect(status().isCreated());
        postAs(bia, "/api/timeline/events", "{\"title\":\"Voo para Munique\",\"startDate\":\"%s\"}"
                .formatted(LocalDate.now(USER_ZONE).plusMonths(3))).andExpect(status().isCreated());

        getAs(bia, "/api/search?q=munchen")
                .andExpect(jsonPath("$.results[0].type").value("UNIVERSITY"))
                .andExpect(jsonPath("$.results[0].url").value("/universities/" + uni))
                // o programa aparece pelo subtítulo (nome da universidade)
                .andExpect(jsonPath("$.results[?(@.type == 'PROGRAM')].title").value(hasItem("Informatik")));

        getAs(bia, "/api/search?q=HISTORICO")
                .andExpect(jsonPath("$.results", hasSize(2)))
                // "Histórico escolar" começa com o termo: vem antes da tarefa
                .andExpect(jsonPath("$.results[?(@.type == 'LUGGAGE')].title").value(hasItem("Histórico escolar")))
                .andExpect(jsonPath("$.results[?(@.type == 'TASK')].url").value(hasItem("/tasks")));

        getAs(bia, "/api/search?q=voo&types=EVENT").andExpect(jsonPath("$.results", hasSize(1)));
        getAs(bia, "/api/search?q=m").andExpect(jsonPath("$.results", hasSize(0)));

        TestSession leo = registerUser("busca-b");
        getAs(leo, "/api/search?q=munchen")
                .andExpect(jsonPath("$.results[*].type").value(not(hasItem("UNIVERSITY"))));
    }

    @Test
    void endpointInternoDosJobsExigeOToken() throws Exception {
        mvc.perform(MockMvcRequestBuilders.post("/api/internal/jobs/daily"))
                .andExpect(status().isUnauthorized());
        mvc.perform(MockMvcRequestBuilders.post("/api/internal/jobs/daily").header("X-Job-Token", "errado"))
                .andExpect(status().isUnauthorized());
        // nos testes os jobs estão desligados: o endpoint responde, sem nada para rodar
        mvc.perform(MockMvcRequestBuilders.post("/api/internal/jobs/daily").header("X-Job-Token", "token-de-teste"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ran", hasSize(0)));
        mvc.perform(MockMvcRequestBuilders.post("/api/internal/jobs/outro").header("X-Job-Token", "token-de-teste"))
                .andExpect(status().isNotFound());
    }
}
