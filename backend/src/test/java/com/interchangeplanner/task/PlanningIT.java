package com.interchangeplanner.task;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.interchangeplanner.support.IntegrationTest;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Tarefas, eventos manuais e a timeline que junta tudo (Fase 6). */
class PlanningIT extends IntegrationTest {

    static final LocalDate TODAY = LocalDate.now(USER_ZONE);

    TestSession bia;
    String objective;

    @BeforeEach
    void setUp() throws Exception {
        bia = registerUser("plan");
        fixEurBrl(bia, "6");
        objective = read(postAs(bia, "/api/objectives", """
                {"name":"Mestrado","targetDate":"%s","currency":"BRL","targetMode":"DERIVED"}"""
                .formatted(TODAY.plusMonths(10))).andExpect(status().isCreated()), "$.id");
    }

    String task(String title, LocalDate due, String priority) throws Exception {
        return read(postAs(bia, "/api/tasks", """
                {"title":"%s","dueDate":%s,"priority":"%s","objectiveId":"%s"}"""
                .formatted(title, due == null ? "null" : "\"" + due + "\"", priority, objective))
                .andExpect(status().isCreated()), "$.id");
    }

    @Test
    void tarefasOrdenadasConclusaoEAtraso() throws Exception {
        String atrasada = task("Traduzir histórico", TODAY.minusDays(2), "HIGH");
        task("Agendar TOEFL", TODAY.plusDays(5), "MEDIUM");
        task("Ideias de moradia", null, "LOW");

        getAs(bia, "/api/tasks")
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].title").value("Traduzir histórico"))
                .andExpect(jsonPath("$[0].overdue").value(true))
                .andExpect(jsonPath("$[0].objective.label").value("Mestrado"))
                .andExpect(jsonPath("$[2].title").value("Ideias de moradia"));

        patchAs(bia, "/api/tasks/{id}/status", "{\"status\":\"DONE\"}", atrasada)
                .andExpect(jsonPath("$.status").value("DONE"))
                .andExpect(jsonPath("$.overdue").value(false))
                .andExpect(jsonPath("$.completedAt").isNotEmpty());
        // concluída vai para o fim
        getAs(bia, "/api/tasks").andExpect(jsonPath("$[2].title").value("Traduzir histórico"));
        getAs(bia, "/api/tasks?status=DONE").andExpect(jsonPath("$", hasSize(1)));

        patchAs(bia, "/api/tasks/{id}/status", "{\"status\":\"TODO\"}", atrasada)
                .andExpect(jsonPath("$.completedAt").isEmpty());
    }

    @Test
    void timelineJuntaEventosTarefasGastosEObjetivoComIdsEstaveis() throws Exception {
        task("Agendar TOEFL", TODAY.plusDays(5), "MEDIUM");
        String event = read(postAs(bia, "/api/timeline/events", """
                {"title":"Voo para Munique","startDate":"%s","endDate":"%s","type":"TRAVEL","priority":"HIGH"}"""
                .formatted(TODAY.plusMonths(9), TODAY.plusMonths(9).plusDays(1)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.source").value("EVENT"))
                .andExpect(jsonPath("$.endDate").value(TODAY.plusMonths(9).plusDays(1).toString())), "$.sourceId");

        String cat = read(getAs(bia, "/api/expense-categories"), "$[0].id");
        postAs(bia, "/api/expenses", """
                {"objectiveId":"%s","categoryId":"%s","name":"Aluguel","currency":"EUR","unitPrice":650,
                 "recurrence":"MONTHLY","occurrences":3,"required":true,"dueDate":"%s"}"""
                .formatted(objective, cat, TODAY.plusMonths(1))).andExpect(status().isCreated());

        String body = getAs(bia, "/api/timeline?from={f}&to={t}", TODAY, TODAY.plusMonths(12))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[?(@.source == 'TASK')]", hasSize(1)))
                .andExpect(jsonPath("$.items[?(@.source == 'EVENT')].title").value(hasItem("Voo para Munique")))
                .andExpect(jsonPath("$.items[?(@.source == 'EXPENSE')]", hasSize(3)))
                .andExpect(jsonPath("$.items[?(@.source == 'EXPENSE')].subtitle").value(hasItem("Vencimento · 2/3")))
                .andExpect(jsonPath("$.items[0].source").value("TASK"))
                .andReturn().getResponse().getContentAsString();
        // ids determinísticos: a mesma consulta devolve os mesmos ids
        List<String> ids = read(getAs(bia, "/api/timeline?from={f}&to={t}", TODAY, TODAY.plusMonths(12)), "$.items[*].id");
        org.assertj.core.api.Assertions.assertThat(body).contains(ids.toArray(String[]::new));

        // filtro por origem
        getAs(bia, "/api/timeline?from={f}&to={t}&sources=EVENT", TODAY, TODAY.plusMonths(12))
                .andExpect(jsonPath("$.items", hasSize(1)));

        // evento feito / excluído
        patchAs(bia, "/api/timeline/events/{id}/done", "{\"done\":true}", event)
                .andExpect(jsonPath("$.status").value("DONE"));
        deleteAs(bia, "/api/timeline/events/{id}", event).andExpect(status().isNoContent());
        getAs(bia, "/api/timeline?sources=EVENT").andExpect(jsonPath("$.items", hasSize(0)));
    }

    @Test
    void financasNaoEntramNaTimeline() throws Exception {
        List<String> cats = read(getAs(bia, "/api/finance/categories"), "$[?(@.name == 'Educação')].id");
        String cat = cats.getFirst();
        postAs(bia, "/api/finance/installment-plans", """
                {"categoryId":"%s","description":"Curso","currency":"BRL","totalAmount":900,"installmentsCount":3,
                 "firstDueDate":"%s"}""".formatted(cat, TODAY.plusDays(10))).andExpect(status().isCreated());
        getAs(bia, "/api/timeline?from={f}&to={t}", TODAY, TODAY.plusMonths(12))
                .andExpect(jsonPath("$.items", hasSize(0)));
        // a data alvo do objetivo também não entra (é do dashboard, não da linha do tempo)
        getAs(bia, "/api/dashboard").andExpect(jsonPath("$.upcoming", hasSize(0)));
    }

    @Test
    void dashboardMostraProximosSemConcluidosEAtrasadosPrimeiro() throws Exception {
        task("Daqui a pouco", TODAY.plusDays(3), "MEDIUM");
        String feita = task("Já feita", TODAY.plusDays(1), "MEDIUM");
        task("Atrasada", TODAY.minusDays(4), "MEDIUM");
        patchAs(bia, "/api/tasks/{id}/status", "{\"status\":\"DONE\"}", feita);

        getAs(bia, "/api/dashboard")
                .andExpect(jsonPath("$.upcoming[0].title").value("Atrasada"))
                .andExpect(jsonPath("$.upcoming[0].status").value("OVERDUE"))
                .andExpect(jsonPath("$.upcoming[*].title").value(not(hasItem("Já feita"))));
    }

    @Test
    void validacoesEIsolamento() throws Exception {
        postAs(bia, "/api/timeline/events", """
                {"title":"X","startDate":"%s","endDate":"%s"}""".formatted(TODAY, TODAY.minusDays(1)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errors[0].field").value("endDate"));
        getAs(bia, "/api/timeline?from={f}&to={t}", TODAY, TODAY.plusYears(4))
                .andExpect(status().isUnprocessableContent());

        String minha = task("Minha", TODAY, "LOW");
        TestSession leo = registerUser("plan-b");
        getAs(leo, "/api/tasks/{id}", minha).andExpect(status().isNotFound());
        getAs(leo, "/api/tasks").andExpect(jsonPath("$", hasSize(0)));
        postAs(leo, "/api/tasks", """
                {"title":"Vínculo alheio","objectiveId":"%s"}""".formatted(objective))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errors[0].field").value("objectiveId"));
        getAs(leo, "/api/timeline").andExpect(jsonPath("$.items", hasSize(0)));
    }
}
