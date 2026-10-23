package com.interchangeplanner.expense;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.interchangeplanner.support.IntegrationTest;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ExpensesIT extends IntegrationTest {

    static final String TODAY = LocalDate.now(USER_ZONE).toString();

    TestSession bia;
    String objective;

    String categoryId(TestSession s, String name) throws Exception {
        List<String> ids = read(getAs(s, "/api/expense-categories"), "$[?(@.name == '" + name + "')].id");
        return ids.getFirst();
    }

    String cycleId(TestSession s, String name) throws Exception {
        List<String> ids = read(getAs(s, "/api/expense-cycles"), "$[?(@.name == '" + name + "')].id");
        return ids.getFirst();
    }

    String objectiveFor(TestSession s) throws Exception {
        return read(postAs(s, "/api/objectives", """
                {"name":"Mestrado","targetDate":"%s","currency":"BRL","targetMode":"DERIVED"}"""
                .formatted(LocalDate.now(USER_ZONE).plusMonths(12))).andExpect(status().isCreated()), "$.id");
    }

    static String expenseJson(String objective, String category, String cycle, String name, String currency,
            String unitPrice, Integer quantity, String recurrence, Integer occurrences, boolean required) {
        return """
                {"objectiveId":"%s","categoryId":"%s","cycleId":%s,"name":"%s","currency":"%s","unitPrice":%s,
                 "quantity":%s,"recurrence":"%s","occurrences":%s,"required":%s}"""
                .formatted(objective, category, cycle == null ? "null" : "\"" + cycle + "\"", name, currency, unitPrice,
                        quantity, recurrence, occurrences, required);
    }

    String createExpense(String json) throws Exception {
        return read(postAs(bia, "/api/expenses", json).andExpect(status().isCreated()), "$.expense.id");
    }

    @BeforeEach
    void setUp() throws Exception {
        bia = registerUser("exp");
        fixEurBrl(bia, "6");
        objective = objectiveFor(bia);
    }

    @Test
    void cadastroSemeiaCatalogoPadraoDaPlanilha() throws Exception {
        getAs(bia, "/api/expense-cycles")
                .andExpect(jsonPath("$[*].name").value(
                        containsInAnyOrder("Preparação", "Imigração", "Vivência", "Emergência")));
        getAs(bia, "/api/expense-categories")
                .andExpect(jsonPath("$", hasSize(14)))
                .andExpect(jsonPath("$[*].name").value(hasItems("Proficiência", "Candidatura", "Viagem", "Transporte")));
    }

    @Nested
    class FluxoCompleto {

        String aluguel;
        String inscricoes;
        String curso;

        @BeforeEach
        void criaGastos() throws Exception {
            String moradia = categoryId(bia, "Moradia");
            String candidatura = categoryId(bia, "Candidatura");
            String proficiencia = categoryId(bia, "Proficiência");
            String vivencia = cycleId(bia, "Vivência");
            String preparacao = cycleId(bia, "Preparação");

            // mensal sem ocorrências: o backend sugere 24 (horizonte do mestrado)
            aluguel = read(postAs(bia, "/api/expenses",
                    expenseJson(objective, moradia, vivencia, "Aluguel", "EUR", "650", null, "MONTHLY", null, true))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.expense.occurrences").value(24))
                    .andExpect(jsonPath("$.expense.total.amount").value(15600.0))
                    .andExpect(jsonPath("$.expense.total.currency").value("EUR"))
                    .andExpect(jsonPath("$.expense.totalConverted.amount").value(93600.0))
                    .andExpect(jsonPath("$.expense.category.name").value("Moradia"))
                    .andExpect(jsonPath("$.expense.status").value("PENDING")), "$.expense.id");

            // "Por UNI": 5 inscrições de R$ 180
            inscricoes = createExpense(expenseJson(objective, candidatura, preparacao, "Inscrições", "BRL", "180", 5,
                    "PER_UNIT", null, true));
            curso = createExpense(expenseJson(objective, proficiencia, null, "Curso de alemão", "BRL", "1000", null,
                    "ONE_TIME", null, false));

            postAs(bia, "/api/objectives/{id}/adjustments", """
                    {"label":"Sperrkonto","amount":-75000,"currency":"BRL"}""", objective)
                    .andExpect(status().isCreated());
        }

        @Test
        void pagamentosParciaisEmOutraMoedaDerivamOStatus() throws Exception {
            postAs(bia, "/api/expenses/{id}/payments", """
                    {"paidOn":"%s","amount":400,"currency":"BRL"}""".formatted(TODAY), inscricoes)
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.expense.status").value("PARTIALLY_PAID"))
                    .andExpect(jsonPath("$.expense.paid.amount").value(400.0))
                    .andExpect(jsonPath("$.expense.remaining.amount").value(500.0));

            // € 100 sem taxa informada: captura a cotação do usuário (6) => R$ 600
            postAs(bia, "/api/expenses/{id}/payments", """
                    {"paidOn":"%s","amount":100,"currency":"EUR","note":"Cartão internacional"}""".formatted(TODAY),
                    inscricoes)
                    .andExpect(jsonPath("$.expense.status").value("PAID"))
                    .andExpect(jsonPath("$.expense.paid.amount").value(1000.0))
                    .andExpect(jsonPath("$.expense.remaining.amount").value(0.0))
                    .andExpect(jsonPath("$.payments", hasSize(2)))
                    .andExpect(jsonPath("$.payments[1].rateToExpenseCurrency").value(6.0))
                    .andExpect(jsonPath("$.payments[1].amountInExpenseCurrency.amount").value(600.0));

            // a cotação muda depois: o valor pago histórico NÃO muda
            fixEurBrl(bia, "7");
            getAs(bia, "/api/expenses/{id}", inscricoes).andExpect(jsonPath("$.expense.paid.amount").value(1000.0));

            String paymentId = read(getAs(bia, "/api/expenses/{id}/payments", inscricoes), "$[1].id");
            deleteAs(bia, "/api/expenses/{id}/payments/{p}", inscricoes, paymentId)
                    .andExpect(jsonPath("$.expense.status").value("PARTIALLY_PAID"));
        }

        @Test
        void resumoAplicaOAjusteUmaVezESeparaObrigatorios() throws Exception {
            postAs(bia, "/api/expenses/{id}/payments", """
                    {"paidOn":"%s","amount":900,"currency":"BRL"}""".formatted(TODAY), inscricoes);

            getAs(bia, "/api/expenses/summary?objectiveId={id}", objective)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.currency").value("BRL"))
                    .andExpect(jsonPath("$.totals.total.amount").value(95500.0))
                    .andExpect(jsonPath("$.totals.required.amount").value(94500.0))
                    .andExpect(jsonPath("$.totals.optional.amount").value(1000.0))
                    .andExpect(jsonPath("$.totals.paid.amount").value(900.0))
                    .andExpect(jsonPath("$.totals.remaining.amount").value(94600.0))
                    .andExpect(jsonPath("$.totals.adjustments.amount").value(-75000.0))
                    .andExpect(jsonPath("$.totals.totalAfterAdjustments.amount").value(20500.0))
                    .andExpect(jsonPath("$.totals.requiredAfterAdjustments.amount").value(19500.0))
                    .andExpect(jsonPath("$.totals.monthlyRecurring.amount").value(3900.0))
                    .andExpect(jsonPath("$.totals.count").value(3))
                    .andExpect(jsonPath("$.groupBy").value("CYCLE"))
                    .andExpect(jsonPath("$.groups[*].label").value(containsInAnyOrder("Preparação", "Vivência", "Sem ciclo")));

            getAs(bia, "/api/expenses/summary?objectiveId={id}&groupBy=RECURRENCE", objective)
                    .andExpect(jsonPath("$.groups[*].label").value(containsInAnyOrder("Único", "Mensal", "Por unidade")));

            // meta derivada (só obrigatórios) = 94.500 − 75.000 = 19.500; pago no escopo = 900
            getAs(bia, "/api/objectives/{id}/progress", objective)
                    .andExpect(jsonPath("$.plannedExpenses.amount").value(94500.0))
                    .andExpect(jsonPath("$.adjustments.amount").value(-75000.0))
                    .andExpect(jsonPath("$.target.amount").value(19500.0))
                    .andExpect(jsonPath("$.paid.amount").value(900.0))
                    .andExpect(jsonPath("$.remaining.amount").value(18600.0))
                    // as duas metas lado a lado: obrigatória (94.500 − 75.000) e completa (95.500 − 75.000)
                    .andExpect(jsonPath("$.scopeGoals[0].scope").value("REQUIRED_ONLY"))
                    .andExpect(jsonPath("$.scopeGoals[0].configured").value(true))
                    .andExpect(jsonPath("$.scopeGoals[0].target.amount").value(19500.0))
                    .andExpect(jsonPath("$.scopeGoals[0].remaining.amount").value(18600.0))
                    .andExpect(jsonPath("$.scopeGoals[1].scope").value("ALL"))
                    .andExpect(jsonPath("$.scopeGoals[1].configured").value(false))
                    .andExpect(jsonPath("$.scopeGoals[1].plannedExpenses.amount").value(95500.0))
                    .andExpect(jsonPath("$.scopeGoals[1].target.amount").value(20500.0))
                    .andExpect(jsonPath("$.scopeGoals[1].remaining.amount").value(19600.0));
        }

        @Test
        void cancelarTiraDosTotaisEReativarDevolve() throws Exception {
            postAs(bia, "/api/expenses/{id}/cancel", null, curso)
                    .andExpect(jsonPath("$.expense.status").value("CANCELLED"));
            getAs(bia, "/api/expenses/summary?objectiveId={id}", objective)
                    .andExpect(jsonPath("$.totals.total.amount").value(94500.0))
                    .andExpect(jsonPath("$.totals.cancelledCount").value(1));
            postAs(bia, "/api/expenses/{id}/payments", """
                    {"paidOn":"%s","amount":10,"currency":"BRL"}""".formatted(TODAY), curso)
                    .andExpect(status().isUnprocessableContent());

            postAs(bia, "/api/expenses/{id}/restore", null, curso)
                    .andExpect(jsonPath("$.expense.status").value("PENDING"));
            getAs(bia, "/api/expenses/summary?objectiveId={id}", objective)
                    .andExpect(jsonPath("$.totals.total.amount").value(95500.0));
        }

        @Test
        void filtrosDaListagem() throws Exception {
            getAs(bia, "/api/expenses?objectiveId={id}", objective).andExpect(jsonPath("$", hasSize(3)));
            getAs(bia, "/api/expenses?objectiveId={id}&required=false", objective)
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].name").value("Curso de alemão"));
            getAs(bia, "/api/expenses?objectiveId={id}&currency=EUR", objective)
                    .andExpect(jsonPath("$[0].name").value("Aluguel"));
            getAs(bia, "/api/expenses?q=INSCRI").andExpect(jsonPath("$", hasSize(1)));
            getAs(bia, "/api/expenses?status=PAID").andExpect(jsonPath("$", hasSize(0)));
        }

        @Test
        void categoriaEmUsoNaoPodeSerExcluida() throws Exception {
            deleteAs(bia, "/api/expense-categories/{id}", categoryId(bia, "Moradia")).andExpect(status().isConflict());
            deleteAs(bia, "/api/expense-categories/{id}", categoryId(bia, "Higiene")).andExpect(status().isNoContent());
        }
    }

    @Test
    void previewCalculaSemSalvar() throws Exception {
        postAs(bia, "/api/expenses/preview-total", """
                {"objectiveId":"%s","currency":"EUR","unitPrice":100,"recurrence":"SEMIANNUAL"}""".formatted(objective))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.occurrences").value(4))
                .andExpect(jsonPath("$.suggestedOccurrences").value(4))
                .andExpect(jsonPath("$.horizonMonths").value(24))
                .andExpect(jsonPath("$.total.amount").value(400.0))
                .andExpect(jsonPath("$.totalConverted.amount").value(2400.0));
        getAs(bia, "/api/expenses?objectiveId={id}", objective).andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void regrasDeOcorrencias() throws Exception {
        String cat = categoryId(bia, "Documentos");
        postAs(bia, "/api/expenses", expenseJson(objective, cat, null, "X", "BRL", "10", null, "CUSTOM", null, true))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errors[0].field").value("occurrences"));
        postAs(bia, "/api/expenses", expenseJson(objective, cat, null, "X", "BRL", "10", null, "ONE_TIME", 3, true))
                .andExpect(status().isUnprocessableContent());
        postAs(bia, "/api/expenses", expenseJson(objective, cat, null, "X", "BRL", "-1", null, "ONE_TIME", null, true))
                .andExpect(status().isBadRequest());
    }

    @Test
    void ajustesSaoCrudDoObjetivo() throws Exception {
        String id = read(postAs(bia, "/api/objectives/{id}/adjustments", """
                {"label":"Sperrkonto","amount":-75000,"currency":"BRL"}""", objective), "$.id");
        putAs(bia, "/api/objectives/{o}/adjustments/{a}", """
                {"label":"Sperrkonto (€ 11.904)","amount":-11904,"currency":"EUR"}""", objective, id)
                .andExpect(jsonPath("$.amount.currency").value("EUR"));
        getAs(bia, "/api/expenses/summary?objectiveId={id}", objective)
                .andExpect(jsonPath("$.totals.adjustments.amount").value(-71424.0));
        postAs(bia, "/api/objectives/{id}/adjustments", """
                {"label":"Zero","amount":0,"currency":"BRL"}""", objective)
                .andExpect(status().isUnprocessableContent());
        deleteAs(bia, "/api/objectives/{o}/adjustments/{a}", objective, id).andExpect(status().isNoContent());
    }

    @Test
    void usuarioBNuncaAcessaGastosCategoriasOuObjetivoDeA() throws Exception {
        String cat = categoryId(bia, "Visto");
        String expense = createExpense(expenseJson(objective, cat, null, "Visto", "EUR", "75", null, "ONE_TIME", null,
                true));

        TestSession bruno = registerUser("expb");
        String brunoObjective = objectiveFor(bruno);
        String brunoCat = categoryId(bruno, "Visto");

        getAs(bruno, "/api/expenses/{id}", expense).andExpect(status().isNotFound());
        putAs(bruno, "/api/expenses/{id}", expenseJson(brunoObjective, brunoCat, null, "X", "BRL", "1", null,
                "ONE_TIME", null, true), expense).andExpect(status().isNotFound());
        deleteAs(bruno, "/api/expenses/{id}", expense).andExpect(status().isNotFound());
        postAs(bruno, "/api/expenses/{id}/payments", """
                {"paidOn":"%s","amount":1,"currency":"EUR"}""".formatted(TODAY), expense).andExpect(status().isNotFound());
        // criar gasto no objetivo de A, ou usar a categoria de A no próprio objetivo
        postAs(bruno, "/api/expenses", expenseJson(objective, brunoCat, null, "X", "BRL", "1", null, "ONE_TIME", null,
                true)).andExpect(status().isNotFound());
        postAs(bruno, "/api/expenses", expenseJson(brunoObjective, cat, null, "X", "BRL", "1", null, "ONE_TIME", null,
                true)).andExpect(status().isNotFound());
        getAs(bruno, "/api/expenses/summary?objectiveId={id}", objective).andExpect(status().isNotFound());
        getAs(bruno, "/api/expenses").andExpect(jsonPath("$", hasSize(0)));
        deleteAs(bruno, "/api/expense-categories/{id}", cat).andExpect(status().isNotFound());
    }
}
