package com.interchangeplanner.objective;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.interchangeplanner.support.IntegrationTest;
import java.time.LocalDate;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ObjectivesAndAccountsIT extends IntegrationTest {

    static final String TARGET = LocalDate.now(USER_ZONE).plusMonths(8).toString();

    static String objectiveJson(String name, String mode, String target) {
        return """
                {"name":"%s","countryCode":"de","city":"Munique","targetDate":"%s","currency":"BRL",
                 "targetMode":"%s","targetAmount":%s}""".formatted(name, TARGET, mode, target);
    }

    String createObjective(TestSession s, String name) throws Exception {
        return read(postAs(s, "/api/objectives", objectiveJson(name, "DERIVED", "null"))
                .andExpect(status().isCreated()), "$.id");
    }

    static String accountJson(String name, String currency, String type, String objectiveId) {
        return """
                {"name":"%s","currency":"%s","type":"%s","objectiveId":%s}"""
                .formatted(name, currency, type, objectiveId == null ? "null" : "\"" + objectiveId + "\"");
    }

    @Nested
    class Objetivos {

        @Test
        void primeiroObjetivoViraPrincipalETrocaDePrincipalEhExclusiva() throws Exception {
            TestSession bia = registerUser("obj");
            String first = createObjective(bia, "Mestrado na Alemanha");
            String second = createObjective(bia, "Intercâmbio curto");

            getAs(bia, "/api/objectives/{id}", first)
                    .andExpect(jsonPath("$.primary").value(true))
                    .andExpect(jsonPath("$.countryCode").value("DE"))
                    .andExpect(jsonPath("$.horizonMonths").value(24))
                    .andExpect(jsonPath("$.expenseScope").value("REQUIRED_ONLY"))
                    .andExpect(jsonPath("$.targetAmount").isEmpty());
            getAs(bia, "/api/objectives/{id}", second).andExpect(jsonPath("$.primary").value(false));

            putAs(bia, "/api/objectives/{id}/primary", null, second).andExpect(status().isOk());

            getAs(bia, "/api/objectives")
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].id").value(second))
                    .andExpect(jsonPath("$[0].primary").value(true))
                    .andExpect(jsonPath("$[1].primary").value(false));
        }

        @Test
        void excluirOPrincipalPromoveOutro() throws Exception {
            TestSession bia = registerUser("objdel");
            String first = createObjective(bia, "A");
            String second = createObjective(bia, "B");
            deleteAs(bia, "/api/objectives/{id}", first).andExpect(status().isNoContent());
            getAs(bia, "/api/objectives/{id}", second).andExpect(jsonPath("$.primary").value(true));
        }

        @Test
        void metaManualExigeValor() throws Exception {
            TestSession bia = registerUser("objmanual");
            postAs(bia, "/api/objectives", objectiveJson("X", "MANUAL", "null"))
                    .andExpect(status().isUnprocessableContent())
                    .andExpect(jsonPath("$.errors[0].field").value("targetAmount"));
        }

        @Test
        void objetivoDeOutroUsuarioRetorna404() throws Exception {
            TestSession a = registerUser("obja");
            TestSession b = registerUser("objb");
            String objective = createObjective(a, "Da Alice");

            getAs(b, "/api/objectives/{id}", objective).andExpect(status().isNotFound());
            putAs(b, "/api/objectives/{id}", objectiveJson("Hack", "DERIVED", "null"), objective)
                    .andExpect(status().isNotFound());
            deleteAs(b, "/api/objectives/{id}", objective).andExpect(status().isNotFound());
            getAs(b, "/api/objectives/{id}/progress", objective).andExpect(status().isNotFound());
            postAs(b, "/api/accounts", accountJson("Conta", "BRL", "SAVINGS", objective)).andExpect(status().isNotFound());
            getAs(b, "/api/objectives").andExpect(jsonPath("$", hasSize(0)));
        }
    }

    @Nested
    class Contas {

        @Test
        void saldoNoMesmoDiaSubstituiEResumoConverteParaAMoedaPrincipal() throws Exception {
            TestSession bia = registerUser("acc");
            fixEurBrl(bia, "6");
            String eur = read(postAs(bia, "/api/accounts", accountJson("Conta Alemanha", "EUR", "CHECKING", null))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.balance.amount").value(0.0))
                    .andExpect(jsonPath("$.balanceAsOf").isEmpty()), "$.id");
            String brl = read(postAs(bia, "/api/accounts", accountJson("Poupança", "BRL", "SAVINGS", null)), "$.id");

            postAs(bia, "/api/accounts/{id}/balances", "{\"balance\":800}", eur).andExpect(status().isOk());
            postAs(bia, "/api/accounts/{id}/balances", "{\"balance\":855.58}", eur).andExpect(status().isOk());
            postAs(bia, "/api/accounts/{id}/balances", "{\"asOf\":\"2026-01-10\",\"balance\":100}", eur);
            postAs(bia, "/api/accounts/{id}/balances", "{\"balance\":1777.26}", brl);

            getAs(bia, "/api/accounts/{id}/balances", eur)
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].balance.amount").value(855.58));

            // Saldos da planilha: R$ 1.777,26 + € 855,58 × 6 = R$ 6.910,74
            getAs(bia, "/api/accounts/summary")
                    .andExpect(jsonPath("$.accountCount").value(2))
                    .andExpect(jsonPath("$.byCurrency", hasSize(2)))
                    .andExpect(jsonPath("$.total.currency").value("BRL"))
                    .andExpect(jsonPath("$.total.amount").value(6910.74))
                    .andExpect(jsonPath("$.exchangeRates[0].manual").value(true));
        }

        @Test
        void regrasDaConta() throws Exception {
            TestSession bia = registerUser("accrules");
            String id = read(postAs(bia, "/api/accounts", accountJson("Conta", "EUR", "CHECKING", null)), "$.id");
            putAs(bia, "/api/accounts/{id}", accountJson("Conta", "BRL", "CHECKING", null), id)
                    .andExpect(status().isUnprocessableContent())
                    .andExpect(jsonPath("$.errors[0].field").value("currency"));
            postAs(bia, "/api/accounts/{id}/balances",
                    "{\"asOf\":\"" + LocalDate.now(USER_ZONE).plusDays(3) + "\",\"balance\":10}", id)
                    .andExpect(status().isUnprocessableContent());
            postAs(bia, "/api/accounts", accountJson("Dólar", "USD", "CASH", null))
                    .andExpect(status().isUnprocessableContent());
        }

        @Test
        void contaArquivadaSaiDoResumo() throws Exception {
            TestSession bia = registerUser("accarch");
            String id = read(postAs(bia, "/api/accounts", accountJson("Velha", "BRL", "OTHER", null)), "$.id");
            postAs(bia, "/api/accounts/{id}/balances", "{\"balance\":500}", id);
            putAs(bia, "/api/accounts/{id}", """
                    {"name":"Velha","currency":"BRL","type":"OTHER","archived":true}""", id)
                    .andExpect(jsonPath("$.archived").value(true));
            getAs(bia, "/api/accounts/summary")
                    .andExpect(jsonPath("$.accountCount").value(0))
                    .andExpect(jsonPath("$.total.amount").value(0.0));
        }

        @Test
        void contaDeOutroUsuarioRetorna404() throws Exception {
            TestSession a = registerUser("acca");
            TestSession b = registerUser("accb");
            String id = read(postAs(a, "/api/accounts", accountJson("Da Alice", "BRL", "SAVINGS", null)), "$.id");
            postAs(b, "/api/accounts/{id}/balances", "{\"balance\":1}", id).andExpect(status().isNotFound());
            getAs(b, "/api/accounts/{id}/balances", id).andExpect(status().isNotFound());
            deleteAs(b, "/api/accounts/{id}", id).andExpect(status().isNotFound());
            getAs(b, "/api/accounts").andExpect(jsonPath("$", hasSize(0)));
        }
    }

    @Nested
    class OnboardingEDashboard {

        static String onboardingJson(String targetDate, String targetAmount) {
            return """
                    {"objectiveName":"Mestrado na Alemanha","countryCode":"DE","city":"Munique","targetDate":"%s",
                     "currency":"BRL","targetAmount":%s,"currentSavings":855.58,"currentSavingsCurrency":"EUR",
                     "plannedMonthlySaving":3000}""".formatted(targetDate, targetAmount);
        }

        @Test
        void dashboardSemObjetivoMostraSoContasECotacao() throws Exception {
            TestSession bia = registerUser("dash0");
            getAs(bia, "/api/dashboard")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.objective").isEmpty())
                    .andExpect(jsonPath("$.progress").isEmpty())
                    .andExpect(jsonPath("$.expenses").isEmpty())
                    .andExpect(jsonPath("$.accounts.accountCount").value(0))
                    .andExpect(jsonPath("$.exchangeRate.base").value("EUR"));
        }

        @Test
        void onboardingCriaObjetivoPrincipalReservaVinculadaEConclui() throws Exception {
            TestSession bia = registerUser("onb");
            fixEurBrl(bia, "6");
            getAs(bia, "/api/me").andExpect(jsonPath("$.onboardingCompleted").value(false));

            postAs(bia, "/api/onboarding", onboardingJson(TARGET, "120000"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.user.onboardingCompleted").value(true))
                    .andExpect(jsonPath("$.objective.primary").value(true))
                    .andExpect(jsonPath("$.objective.targetMode").value("MANUAL"));

            getAs(bia, "/api/accounts")
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].name").value("Reserva do intercâmbio"))
                    .andExpect(jsonPath("$[0].currency").value("EUR"))
                    .andExpect(jsonPath("$[0].balance.amount").value(855.58));

            // € 855,58 × 6 = R$ 5.133,48 mobilizados de R$ 120.000
            getAs(bia, "/api/dashboard")
                    .andExpect(jsonPath("$.objective.name").value("Mestrado na Alemanha"))
                    .andExpect(jsonPath("$.progress.target.amount").value(120000.0))
                    .andExpect(jsonPath("$.progress.accountsBalance.amount").value(5133.48))
                    .andExpect(jsonPath("$.progress.mobilized.amount").value(5133.48))
                    .andExpect(jsonPath("$.progress.remaining.amount").value(114866.52))
                    .andExpect(jsonPath("$.progress.monthsRemaining").value(9))
                    .andExpect(jsonPath("$.progress.plannedMonthlySaving.amount").value(3000.0))
                    .andExpect(jsonPath("$.progress.onTrack").value(false))
                    .andExpect(jsonPath("$.exchangeRate.manual").value(true));

            postAs(bia, "/api/onboarding", onboardingJson(TARGET, "null")).andExpect(status().isConflict());
        }

        @Test
        void onboardingExigeDataFutura() throws Exception {
            TestSession bia = registerUser("onbpast");
            postAs(bia, "/api/onboarding", onboardingJson(LocalDate.now(USER_ZONE).minusDays(1).toString(), "null"))
                    .andExpect(status().isUnprocessableContent())
                    .andExpect(jsonPath("$.errors[0].field").value("targetDate"));
            getAs(bia, "/api/objectives").andExpect(jsonPath("$", hasSize(0)));
        }
    }

    @Test
    void exclusaoDoObjetivoDesvinculaAConta() throws Exception {
        TestSession bia = registerUser("unlink");
        String objective = createObjective(bia, "Temporário");
        String account = read(postAs(bia, "/api/accounts", accountJson("Reserva", "BRL", "SAVINGS", objective)), "$.id");
        deleteAs(bia, "/api/objectives/{id}", objective).andExpect(status().isNoContent());
        String objectiveId = read(getAs(bia, "/api/accounts"), "$[0].objectiveId");
        assertThat(objectiveId).isNull();
        assertThat(account).isNotNull();
    }
}
