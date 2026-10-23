package com.interchangeplanner.finance;

import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.interchangeplanner.finance.ledger.application.RecurringService;
import com.interchangeplanner.support.IntegrationTest;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class FinanceIT extends IntegrationTest {

    static final LocalDate TODAY = LocalDate.now(USER_ZONE);
    static final YearMonth MONTH = YearMonth.now(USER_ZONE);

    @Autowired
    RecurringService recurringService;

    TestSession bia;

    String category(TestSession s, String name) throws Exception {
        List<String> ids = read(getAs(s, "/api/finance/categories"), "$[?(@.name == '" + name + "')].id");
        return ids.getFirst();
    }

    String planJson(String category, String total, String installment, int count, LocalDate first) {
        return """
                {"categoryId":"%s","description":"Notebook","currency":"BRL","totalAmount":%s,"installmentAmount":%s,
                 "installmentsCount":%d,"firstDueDate":"%s"}""".formatted(category, total, installment, count, first);
    }

    String recurringJson(String category, String name, String kind, String amount, String currency, LocalDate start,
            boolean autoConfirm) {
        return """
                {"categoryId":"%s","name":"%s","kind":"%s","amount":%s,"currency":"%s","frequency":"MONTHLY",
                 "startDate":"%s","autoConfirm":%s}""".formatted(category, name, kind, amount, currency, start, autoConfirm);
    }

    @BeforeEach
    void setUp() throws Exception {
        bia = registerUser("fin");
        fixEurBrl(bia, "6");
    }

    @Test
    void cadastroSemeiaCategoriasDeReceitaEDespesa() throws Exception {
        getAs(bia, "/api/finance/categories")
                .andExpect(jsonPath("$", hasSize(11)))
                .andExpect(jsonPath("$[?(@.kind == 'INCOME')].name").value(hasItems("Salário", "Freelance")))
                .andExpect(jsonPath("$[?(@.kind == 'EXPENSE')].name").value(hasItems("Moradia", "Assinaturas")));
    }

    @Nested
    class Lancamentos {

        @Test
        void statusPadraoPelaDataECategoriaDoTipoCerto() throws Exception {
            postAs(bia, "/api/finance/transactions", """
                    {"kind":"EXPENSE","categoryId":"%s","occurredOn":"%s","amount":120.5,"currency":"BRL","description":"Mercado"}"""
                    .formatted(category(bia, "Alimentação"), TODAY))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.status").value("CONFIRMED"))
                    .andExpect(jsonPath("$.source").value("MANUAL"))
                    .andExpect(jsonPath("$.category.name").value("Alimentação"));
            postAs(bia, "/api/finance/transactions", """
                    {"kind":"EXPENSE","categoryId":"%s","occurredOn":"%s","amount":80,"currency":"BRL","description":"Show"}"""
                    .formatted(category(bia, "Lazer"), TODAY.plusDays(10)))
                    .andExpect(jsonPath("$.status").value("PLANNED"));
            postAs(bia, "/api/finance/transactions", """
                    {"kind":"INCOME","categoryId":"%s","occurredOn":"%s","amount":10,"currency":"BRL","description":"X"}"""
                    .formatted(category(bia, "Lazer"), TODAY))
                    .andExpect(status().isUnprocessableContent())
                    .andExpect(jsonPath("$.errors[0].field").value("categoryId"));
        }
    }

    @Nested
    class Parcelamentos {

        @Test
        void geraParcelasComCentavosNaUltimaERedistribuiAoEditar() throws Exception {
            String cat = category(bia, "Educação");
            LocalDate first = TODAY.plusMonths(1).withDayOfMonth(10);
            String id = read(postAs(bia, "/api/finance/installment-plans", planJson(cat, "100", "null", 3, first))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.installments", hasSize(3)))
                    .andExpect(jsonPath("$.installments[0].amount.amount").value(33.33))
                    .andExpect(jsonPath("$.installments[2].amount.amount").value(33.34))
                    .andExpect(jsonPath("$.installmentAmount.amount").value(33.33))
                    .andExpect(jsonPath("$.lastDueDate").value(first.plusMonths(2).toString())), "$.id");

            // confirma a 1ª parcela
            String firstTx = read(getAs(bia, "/api/finance/installment-plans/{id}", id), "$.installments[0].transactionId");
            patchAs(bia, "/api/finance/transactions/" + firstTx + "/status", "{\"status\":\"CONFIRMED\"}")
                    .andExpect(status().isOk());

            // novo total 120: restam 86,67 para as parcelas 2 e 3
            putAs(bia, "/api/finance/installment-plans/{id}", planJson(cat, "120", "null", 3, first), id)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.paidCount").value(1))
                    .andExpect(jsonPath("$.paid.amount").value(33.33))
                    .andExpect(jsonPath("$.installments[0].amount.amount").value(33.33))
                    .andExpect(jsonPath("$.installments[1].amount.amount").value(43.33))
                    .andExpect(jsonPath("$.installments[2].amount.amount").value(43.34))
                    .andExpect(jsonPath("$.remaining.amount").value(86.67));

            putAs(bia, "/api/finance/installment-plans/{id}", planJson(cat, "120", "null", 0, first), id)
                    .andExpect(status().isBadRequest());

            // excluir mantém a parcela confirmada como histórico
            deleteAs(bia, "/api/finance/installment-plans/{id}", id).andExpect(status().isNoContent());
            getAs(bia, "/api/finance/transactions?from={f}&to={t}", first.minusMonths(1), first.plusMonths(3))
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].status").value("CONFIRMED"))
                    .andExpect(jsonPath("$[0].installmentPlanId").isEmpty());
        }

        @Test
        void parcelamentoEmAndamentoSoGeraAsParcelasQueFaltam() throws Exception {
            String cat = category(bia, "Educação");
            LocalDate next = TODAY.plusMonths(1).withDayOfMonth(5);
            String json = """
                    {"categoryId":"%s","description":"Curso","currency":"BRL","installmentAmount":219.80,
                     "installmentsCount":10,"paidBeforeCount":%d,"firstDueDate":"%s"}""";
            String id = read(postAs(bia, "/api/finance/installment-plans", json.formatted(cat, 3, next))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.total.amount").value(2198.0))
                    .andExpect(jsonPath("$.paidBeforeCount").value(3))
                    .andExpect(jsonPath("$.paidCount").value(3))
                    .andExpect(jsonPath("$.remainingCount").value(7))
                    .andExpect(jsonPath("$.paid.amount").value(659.4))
                    .andExpect(jsonPath("$.remaining.amount").value(1538.6))
                    .andExpect(jsonPath("$.installments", hasSize(7)))
                    .andExpect(jsonPath("$.installments[0].number").value(4))
                    .andExpect(jsonPath("$.installments[0].dueDate").value(next.toString()))
                    .andExpect(jsonPath("$.nextDueDate").value(next.toString()))
                    .andExpect(jsonPath("$.lastDueDate").value(next.plusMonths(6).toString())), "$.id");

            // nenhuma parcela antiga vira lançamento: o mês passado continua vazio
            getAs(bia, "/api/finance/transactions?from={f}&to={t}", TODAY.minusMonths(4), TODAY)
                    .andExpect(jsonPath("$", hasSize(0)));

            // confirmada a parcela 4 no app, ela não pode passar a "paga antes"
            String tx = read(getAs(bia, "/api/finance/installment-plans/{id}", id), "$.installments[0].transactionId");
            patchAs(bia, "/api/finance/transactions/" + tx + "/status", "{\"status\":\"CONFIRMED\"}");
            putAs(bia, "/api/finance/installment-plans/{id}", json.formatted(cat, 4, next), id)
                    .andExpect(status().isUnprocessableContent())
                    .andExpect(jsonPath("$.errors[0].field").value("paidBeforeCount"));
            getAs(bia, "/api/finance/installment-plans/{id}", id)
                    .andExpect(jsonPath("$.paidCount").value(4))
                    .andExpect(jsonPath("$.remainingCount").value(6));

            // todas pagas antes não faz sentido
            postAs(bia, "/api/finance/installment-plans", json.formatted(cat, 10, next))
                    .andExpect(status().isUnprocessableContent())
                    .andExpect(jsonPath("$.errors[0].field").value("paidBeforeCount"));
        }

        @Test
        void valorDaParcelaOuTotalUmDosDois() throws Exception {
            String cat = category(bia, "Educação");
            postAs(bia, "/api/finance/installment-plans", planJson(cat, "null", "219.80", 10, TODAY))
                    .andExpect(jsonPath("$.total.amount").value(2198.0));
            postAs(bia, "/api/finance/installment-plans", planJson(cat, "100", "10", 10, TODAY))
                    .andExpect(status().isUnprocessableContent());
        }

        @Test
        void parcelaNaoEhExcluidaIsoladamente() throws Exception {
            String id = read(postAs(bia, "/api/finance/installment-plans", planJson(category(bia, "Educação"), "50", "null", 2,
                    TODAY)), "$.id");
            String tx = read(getAs(bia, "/api/finance/installment-plans/{id}", id), "$.installments[0].transactionId");
            deleteAs(bia, "/api/finance/transactions/{id}", tx).andExpect(status().isUnprocessableContent());
        }
    }

    @Nested
    class Recorrencias {

        @Test
        void materializaPausaRetomaEConfirmaAutomaticamente() throws Exception {
            String id = read(postAs(bia, "/api/finance/recurring",
                    recurringJson(category(bia, "Assinaturas"), "Spotify", "EXPENSE", "21.90", "BRL", TODAY, true))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.nextOccurrence").value(TODAY.toString()))
                    .andExpect(jsonPath("$.monthlyEquivalent.amount").value(21.9)), "$.id");

            String range = "/api/finance/transactions?from=" + TODAY + "&to=" + TODAY.plusMonths(11);
            getAs(bia, range).andExpect(jsonPath("$", hasSize(12)))
                    .andExpect(jsonPath("$[0].source").value("RECURRING"))
                    .andExpect(jsonPath("$[0].sourceLabel").value("Spotify"));

            // job diário: a ocorrência de hoje é confirmada sozinha
            recurringService.extendAndAutoConfirm();
            getAs(bia, "/api/finance/transactions?from={d}&to={d}", TODAY, TODAY)
                    .andExpect(jsonPath("$[0].status").value("CONFIRMED"));

            patchAs(bia, "/api/finance/recurring/" + id + "/active", "{\"active\":false}");
            getAs(bia, range).andExpect(jsonPath("$", hasSize(1))); // só a confirmada
            patchAs(bia, "/api/finance/recurring/" + id + "/active", "{\"active\":true}");
            getAs(bia, range).andExpect(jsonPath("$", hasSize(12)));
        }
    }

    @Nested
    class Visoes {

        @Test
        void resumoDoMesSeparaAvulsosParcelasERecorrenciasEConverteMoedas() throws Exception {
            LocalDate day = MONTH.atDay(1);
            postAs(bia, "/api/finance/transactions", """
                    {"kind":"INCOME","categoryId":"%s","occurredOn":"%s","amount":5000,"currency":"BRL","description":"Salário","status":"CONFIRMED"}"""
                    .formatted(category(bia, "Salário"), day));
            postAs(bia, "/api/finance/transactions", """
                    {"kind":"EXPENSE","categoryId":"%s","occurredOn":"%s","amount":100,"currency":"EUR","description":"Livro","status":"CONFIRMED"}"""
                    .formatted(category(bia, "Educação"), day));
            postAs(bia, "/api/finance/recurring", recurringJson(category(bia, "Moradia"), "Aluguel", "EXPENSE", "1500", "BRL",
                    day, false));
            putAs(bia, "/api/finance/saving-goals/{m}", "{\"amount\":2000,\"currency\":\"BRL\"}", MONTH.toString())
                    .andExpect(jsonPath("$.month").value(MONTH.toString()));

            getAs(bia, "/api/finance/summary?month={m}", MONTH.toString())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.currency").value("BRL"))
                    .andExpect(jsonPath("$.income.total.amount").value(5000.0))
                    .andExpect(jsonPath("$.expenses.total.amount").value(2100.0))
                    .andExpect(jsonPath("$.expenses.oneOff.amount").value(600.0))
                    .andExpect(jsonPath("$.expenses.recurring.amount").value(1500.0))
                    .andExpect(jsonPath("$.expenses.confirmed.amount").value(600.0))
                    .andExpect(jsonPath("$.projectedSurplus.amount").value(2900.0))
                    .andExpect(jsonPath("$.realizedSurplus.amount").value(4400.0))
                    .andExpect(jsonPath("$.savingGoal.amount").value(2000.0))
                    .andExpect(jsonPath("$.goalRatio").value(1.45))
                    .andExpect(jsonPath("$.expensesByCategory[0].category.name").value("Moradia"))
                    .andExpect(jsonPath("$.transactions", hasSize(3)))
                    .andExpect(jsonPath("$.committedNext12.amount").value(18000.0));

            getAs(bia, "/api/finance/overview?from={f}&to={t}", MONTH.toString(), MONTH.plusMonths(1).toString())
                    .andExpect(jsonPath("$.months", hasSize(2)))
                    .andExpect(jsonPath("$.months[0].surplus.amount").value(2900.0))
                    .andExpect(jsonPath("$.months[1].surplus.amount").value(-1500.0))
                    .andExpect(jsonPath("$.months[1].cumulativeSurplus.amount").value(1400.0));

            getAs(bia, "/api/finance/commitments?months=3")
                    .andExpect(jsonPath("$.months", hasSize(3)))
                    .andExpect(jsonPath("$.months[1].recurring.amount").value(1500.0));

            getAs(bia, "/api/finance/summary?month=2026-13").andExpect(status().isUnprocessableContent());
        }

        @Test
        void projecaoDoObjetivoComSalarioProgramado() throws Exception {
            String objective = read(postAs(bia, "/api/objectives", """
                    {"name":"Mestrado","targetDate":"%s","currency":"BRL","targetMode":"MANUAL","targetAmount":100000}"""
                    .formatted(MONTH.plusMonths(6).atDay(15))), "$.id");
            postAs(bia, "/api/finance/recurring", recurringJson(category(bia, "Salário"), "Salário", "INCOME", "10000", "BRL",
                    MONTH.plusMonths(1).atDay(5), false));

            // 6 meses × 10.000 = 60.000 na data alvo → faltam 40.000; alcança em 10 meses
            getAs(bia, "/api/objectives/{id}/projection", objective)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.hasHistory").value(false))
                    .andExpect(jsonPath("$.baselineMonthlySaving.amount").value(0.0))
                    .andExpect(jsonPath("$.reachesByTarget").value(false))
                    .andExpect(jsonPath("$.deficitAtTarget.amount").value(40000.0))
                    .andExpect(jsonPath("$.reachMonth").value(MONTH.plusMonths(10).toString()))
                    .andExpect(jsonPath("$.months[0].plannedIncome.amount").value(10000.0));
        }
    }

    @Test
    void usuarioBNaoAcessaDadosFinanceirosDeA() throws Exception {
        String plan = read(postAs(bia, "/api/finance/installment-plans", planJson(category(bia, "Educação"), "300", "null", 3,
                TODAY)), "$.id");
        String rec = read(postAs(bia, "/api/finance/recurring", recurringJson(category(bia, "Assinaturas"), "Netflix",
                "EXPENSE", "39.90", "BRL", TODAY, false)), "$.id");
        String tx = read(postAs(bia, "/api/finance/transactions", """
                {"kind":"EXPENSE","categoryId":"%s","occurredOn":"%s","amount":1,"currency":"BRL","description":"X"}"""
                .formatted(category(bia, "Outros"), TODAY)), "$.id");

        TestSession bruno = registerUser("finb");
        getAs(bruno, "/api/finance/installment-plans/{id}", plan).andExpect(status().isNotFound());
        deleteAs(bruno, "/api/finance/recurring/{id}", rec).andExpect(status().isNotFound());
        deleteAs(bruno, "/api/finance/transactions/{id}", tx).andExpect(status().isNotFound());
        postAs(bruno, "/api/finance/transactions", """
                {"kind":"EXPENSE","categoryId":"%s","occurredOn":"%s","amount":1,"currency":"BRL","description":"X"}"""
                .formatted(category(bia, "Outros"), TODAY)).andExpect(status().isNotFound());
        getAs(bruno, "/api/finance/summary").andExpect(jsonPath("$.transactions", hasSize(0)));
    }
}
