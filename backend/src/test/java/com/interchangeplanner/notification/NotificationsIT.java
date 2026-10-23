package com.interchangeplanner.notification;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.interchangeplanner.exchange.application.ExchangeRateService;
import com.interchangeplanner.exchange.provider.FetchedRate;
import com.interchangeplanner.notification.application.ExchangeAlertService;
import com.interchangeplanner.support.IntegrationTest;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** Detectores, deduplicação, preferências e alertas de câmbio (Fase 8). */
class NotificationsIT extends IntegrationTest {

    static final LocalDate TODAY = LocalDate.now(USER_ZONE);

    @Autowired
    ExchangeRateService exchange;

    @Autowired
    ExchangeAlertService alertService;

    TestSession bia;

    @BeforeEach
    void setUp() throws Exception {
        bia = registerUser("notif");
    }

    void task(String title, LocalDate due, String priority) throws Exception {
        postAs(bia, "/api/tasks", """
                {"title":"%s","dueDate":"%s","priority":"%s"}""".formatted(title, due, priority))
                .andExpect(status().isCreated());
    }

    @Test
    void geraAvisosDosPrazosSemRepetir() throws Exception {
        task("Traduzir histórico", TODAY.plusDays(1), "URGENT");
        task("Daqui a muito tempo", TODAY.plusDays(40), "LOW");

        postAs(bia, "/api/notifications/refresh", null).andExpect(jsonPath("$.created").value(1));
        // rodar de novo não duplica
        postAs(bia, "/api/notifications/refresh", null).andExpect(jsonPath("$.created").value(0));

        getAs(bia, "/api/notifications")
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].category").value("TASKS"))
                .andExpect(jsonPath("$.items[0].severity").value("CRITICAL"))
                .andExpect(jsonPath("$.items[0].title").value("Tarefa para amanhã: Traduzir histórico"))
                .andExpect(jsonPath("$.items[0].actionUrl").value("/tasks"))
                .andExpect(jsonPath("$.unreadCount").value(1));
    }

    @Test
    void lerExcluirEPreferencias() throws Exception {
        task("Agendar TOEFL", TODAY, "MEDIUM");
        postAs(bia, "/api/notifications/refresh", null);
        String id = read(getAs(bia, "/api/notifications"), "$.items[0].id");

        patchAs(bia, "/api/notifications/{id}/read", "{}", id).andExpect(jsonPath("$.read").value(true));
        getAs(bia, "/api/notifications/unread-count").andExpect(jsonPath("$.count").value(0));
        getAs(bia, "/api/notifications?unread=true").andExpect(jsonPath("$.items", hasSize(0)));
        deleteAs(bia, "/api/notifications/{id}", id).andExpect(status().isNoContent());

        // categoria desligada não gera
        getAs(bia, "/api/notification-preferences")
                .andExpect(jsonPath("$", hasSize(6)))
                .andExpect(jsonPath("$[?(@.category == 'DEADLINES')].leadDays[*]").value(hasItem(30)));
        putAs(bia, "/api/notification-preferences/TASKS", "{\"enabled\":false}")
                .andExpect(jsonPath("$.enabled").value(false));
        task("Outra para hoje", TODAY, "HIGH");
        postAs(bia, "/api/notifications/refresh", null).andExpect(jsonPath("$.created").value(0));

        putAs(bia, "/api/notification-preferences/TASKS", "{\"enabled\":true,\"leadDays\":[]}")
                .andExpect(status().isUnprocessableContent());
        putAs(bia, "/api/notification-preferences/TASKS", "{\"enabled\":true,\"leadDays\":[400]}")
                .andExpect(status().isBadRequest());
    }

    @Test
    void vencimentoDeGastoEMarcoDoObjetivo() throws Exception {
        String objective = read(postAs(bia, "/api/objectives", """
                {"name":"Mestrado","targetDate":"%s","currency":"BRL","targetMode":"MANUAL","targetAmount":1000}"""
                .formatted(TODAY.plusMonths(12))), "$.id");
        String cat = read(getAs(bia, "/api/expense-categories"), "$[0].id");
        postAs(bia, "/api/expenses", """
                {"objectiveId":"%s","categoryId":"%s","name":"Visto","currency":"BRL","unitPrice":600,
                 "recurrence":"ONE_TIME","required":true,"dueDate":"%s"}"""
                .formatted(objective, cat, TODAY.plusDays(6))).andExpect(status().isCreated());
        // conta vinculada com 600 de 1000 = 60%: marco de 50%
        String account = read(postAs(bia, "/api/accounts", """
                {"name":"Reserva","currency":"BRL","type":"SAVINGS","objectiveId":"%s"}""".formatted(objective)), "$.id");
        postAs(bia, "/api/accounts/{id}/balances", """
                {"asOf":"%s","balance":600}""".formatted(TODAY), account).andExpect(status().isOk());

        postAs(bia, "/api/notifications/refresh", null).andExpect(jsonPath("$.created").value(2));
        getAs(bia, "/api/notifications")
                .andExpect(jsonPath("$.items[?(@.category == 'PAYMENTS')].title").value(hasItem("Visto vence em 6 dias")))
                .andExpect(jsonPath("$.items[?(@.category == 'MOTIVATIONAL')].title")
                        .value(hasItem("Você chegou a 50% da meta")));
    }

    @Test
    void alertaDeCambioComHistereseEIsolamento() throws Exception {
        exchange.record(new FetchedRate("EUR", "BRL", new BigDecimal("6.10"), TODAY, "TEST"));
        String alert = read(postAs(bia, "/api/exchange-rate-alerts", """
                {"base":"EUR","quote":"BRL","condition":"BELOW","threshold":5.80}""")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.state").value("ARMED")), "$.id");

        exchange.record(new FetchedRate("EUR", "BRL", new BigDecimal("5.75"), TODAY, "TEST"));
        alertService.evaluateAll();
        alertService.evaluateAll();
        getAs(bia, "/api/notifications?unread=true")
                .andExpect(jsonPath("$.items[?(@.category == 'EXCHANGE')]", hasSize(1)))
                .andExpect(jsonPath("$.items[0].title").value("EUR abaixo de 5,8 BRL"));
        getAs(bia, "/api/exchange-rate-alerts").andExpect(jsonPath("$[0].state").value("TRIGGERED"));

        postAs(bia, "/api/exchange-rate-alerts", """
                {"base":"EUR","quote":"EUR","condition":"BELOW","threshold":1}""")
                .andExpect(status().isUnprocessableContent());

        TestSession leo = registerUser("notif-b");
        deleteAs(leo, "/api/exchange-rate-alerts/{id}", alert).andExpect(status().isNotFound());
        getAs(leo, "/api/notifications").andExpect(jsonPath("$.items", hasSize(0)));
    }
}
