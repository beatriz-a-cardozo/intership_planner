package com.interchangeplanner.objective;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.interchangeplanner.support.IntegrationTest;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Meta só com os ciclos marcados, "guardar para o intercâmbio" e as localidades do mapa. */
class GoalCyclesAndSavingsIT extends IntegrationTest {

    TestSession bia;
    String objective;

    @BeforeEach
    void setUp() throws Exception {
        bia = registerUser("goal");
        fixEurBrl(bia, "6");
        objective = read(postAs(bia, "/api/objectives", """
                {"name":"Mestrado","targetDate":"%s","currency":"BRL","targetMode":"DERIVED"}"""
                .formatted(LocalDate.now(USER_ZONE).plusMonths(12))).andExpect(status().isCreated()), "$.id");
    }

    String cycle(String name) throws Exception {
        List<String> ids = read(getAs(bia, "/api/expense-cycles"), "$[?(@.name == '" + name + "')].id");
        return ids.getFirst();
    }

    void expense(String cycleId, String name, String price) throws Exception {
        String category = read(getAs(bia, "/api/expense-categories"), "$[0].id");
        postAs(bia, "/api/expenses", """
                {"objectiveId":"%s","categoryId":"%s","cycleId":%s,"name":"%s","currency":"BRL","unitPrice":%s,
                 "quantity":1,"recurrence":"ONE_TIME","required":true}"""
                .formatted(objective, category, cycleId == null ? "null" : "\"" + cycleId + "\"", name, price))
                .andExpect(status().isCreated());
    }

    @Test
    void metaSomaSoPreparacaoEImigracao() throws Exception {
        getAs(bia, "/api/expense-cycles")
                .andExpect(jsonPath("$[?(@.name == 'Preparação')].inGoal").value(true))
                .andExpect(jsonPath("$[?(@.name == 'Imigração')].inGoal").value(true))
                .andExpect(jsonPath("$[?(@.name == 'Vivência')].inGoal").value(false))
                .andExpect(jsonPath("$[?(@.name == 'Emergência')].inGoal").value(false));
        expense(cycle("Preparação"), "TOEFL", "1000");
        expense(cycle("Imigração"), "Visto", "500");
        expense(cycle("Vivência"), "Aluguel", "9000");
        expense(null, "Sem ciclo", "300");

        getAs(bia, "/api/objectives/{id}/progress", objective)
                .andExpect(jsonPath("$.plannedExpenses.amount").value(1500))
                .andExpect(jsonPath("$.target.amount").value(1500))
                .andExpect(jsonPath("$.scopeGoals[1].plannedExpenses.amount").value(1500));

        // Vivência passa a contar; os demais campos do ciclo não mudam
        String vivencia = cycle("Vivência");
        putAs(bia, "/api/expense-cycles/{id}", """
                {"name":"Vivência","color":"#b7791f","icon":"home","inGoal":true}""", vivencia)
                .andExpect(jsonPath("$.inGoal").value(true));
        getAs(bia, "/api/objectives/{id}/progress", objective).andExpect(jsonPath("$.target.amount").value(10500));

        // Nenhum ciclo marcado: a meta volta a considerar todos os gastos
        for (String name : List.of("Preparação", "Imigração", "Vivência", "Emergência")) {
            putAs(bia, "/api/expense-cycles/{id}", """
                    {"name":"%s","color":"#1f4d3f","icon":"home","inGoal":false}""".formatted(name), cycle(name))
                    .andExpect(status().isOk());
        }
        getAs(bia, "/api/objectives/{id}/progress", objective).andExpect(jsonPath("$.target.amount").value(10800));
    }

    @Test
    void guardarCriaReservaEAcumulaNoSaldoDeHoje() throws Exception {
        postAs(bia, "/api/accounts/savings/{id}", "{\"amount\":1500.50}", objective)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Reserva do intercâmbio"))
                .andExpect(jsonPath("$.currency").value("BRL"))
                .andExpect(jsonPath("$.objectiveId").value(objective))
                .andExpect(jsonPath("$.balance.amount").value(1500.5));
        postAs(bia, "/api/accounts/savings/{id}", "{\"amount\":499.50}", objective)
                .andExpect(jsonPath("$.balance.amount").value(2000));
        postAs(bia, "/api/accounts/savings/{id}", "{\"amount\":-500}", objective)
                .andExpect(jsonPath("$.balance.amount").value(1500));
        getAs(bia, "/api/accounts").andExpect(jsonPath("$", hasSize(1)));
        getAs(bia, "/api/objectives/{id}/progress", objective)
                .andExpect(jsonPath("$.accountsBalance.amount").value(1500));

        // em euro vai para outra conta, criada na moeda
        postAs(bia, "/api/accounts/savings/{id}", "{\"amount\":100,\"currency\":\"EUR\"}", objective)
                .andExpect(jsonPath("$.currency").value("EUR"));
        getAs(bia, "/api/objectives/{id}/progress", objective)
                .andExpect(jsonPath("$.accountsBalance.amount").value(2100));

        postAs(bia, "/api/accounts/savings/{id}", "{\"amount\":-5000}", objective)
                .andExpect(status().isUnprocessableEntity());
        postAs(bia, "/api/accounts/savings/{id}", "{\"amount\":0}", objective)
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void guardarNumaContaDeOutroObjetivoOuDeOutroUsuarioFalha() throws Exception {
        String loose = read(postAs(bia, "/api/accounts", """
                {"name":"Corrente","currency":"BRL","type":"CHECKING"}""").andExpect(status().isCreated()), "$.id");
        postAs(bia, "/api/accounts/savings/{id}", "{\"amount\":10,\"accountId\":\"%s\"}".formatted(loose), objective)
                .andExpect(status().isUnprocessableEntity());

        TestSession other = registerUser("goal-outro");
        postAs(other, "/api/accounts/savings/{id}", "{\"amount\":10}", objective).andExpect(status().isNotFound());
    }

    @Test
    void mapaListaUniversidadesEscolhidasEMantemCoordenadas() throws Exception {
        getAs(bia, "/api/universities/locations").andExpect(jsonPath("$", hasSize(0)));
        String kiel = read(postAs(bia, "/api/universities", """
                {"name":"Uni Kiel","city":"Kiel","countryCode":"DE","latitude":54.323293,"longitude":10.122765}""")
                .andExpect(status().isCreated()), "$.id");
        String tum = read(postAs(bia, "/api/universities", """
                {"name":"TUM","city":"Munique","countryCode":"DE"}""").andExpect(status().isCreated()), "$.id");
        String cs = program(kiel, "Computer Science");
        program(tum, "Informatik");
        patchAs(bia, "/api/programs/" + cs + "/chosen", "{\"chosen\":true}").andExpect(status().isOk());

        getAs(bia, "/api/universities/locations")
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Uni Kiel"))
                .andExpect(jsonPath("$[0].latitude").value(54.323293))
                .andExpect(jsonPath("$[0].programs[0].name").value("Computer Science"));

        // editar sem coordenadas mantém as descobertas; mudar a cidade as apaga (o mapa procura de novo)
        putAs(bia, "/api/universities/{id}", """
                {"name":"Christian-Albrechts-Universität","city":"Kiel","countryCode":"DE"}""", kiel)
                .andExpect(status().isOk());
        getAs(bia, "/api/universities/locations").andExpect(jsonPath("$[0].longitude").value(10.122765));
        putAs(bia, "/api/universities/{id}", """
                {"name":"Christian-Albrechts-Universität","city":"Lübeck","countryCode":"DE"}""", kiel)
                .andExpect(status().isOk());
        getAs(bia, "/api/universities/locations").andExpect(jsonPath("$[0].latitude").doesNotExist());

        TestSession other = registerUser("goal-mapa");
        getAs(other, "/api/universities/locations").andExpect(jsonPath("$", hasSize(0)));
    }

    String program(String universityId, String name) throws Exception {
        return read(postAs(bia, "/api/programs", """
                {"universityId":"%s","name":"%s","degree":"M.Sc."}""".formatted(universityId, name))
                .andExpect(status().isCreated()), "$.id");
    }
}
