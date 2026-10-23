package com.interchangeplanner.finance.ledger.api;

import com.interchangeplanner.exchange.api.ExchangeRateResponse;
import com.interchangeplanner.finance.ledger.domain.EntryKind;
import com.interchangeplanner.finance.ledger.domain.Frequency;
import com.interchangeplanner.finance.ledger.domain.PaymentMethod;
import com.interchangeplanner.finance.ledger.domain.TransactionStatus;
import com.interchangeplanner.shared.money.MoneyResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Contratos do controle financeiro (Fase 4). Agrupados para manter o pacote enxuto. */
public final class LedgerDtos {

    private LedgerDtos() {
    }

    // ------------------------------------------------------------------ categorias

    public record FinanceCategoryRequest(
            @NotNull(message = "Informe se é receita ou despesa.") EntryKind kind,
            @NotBlank(message = "Informe o nome.") @Size(max = 60, message = "Máximo de 60 caracteres.") String name,
            @NotBlank @Pattern(regexp = "#[0-9a-fA-F]{6}", message = "Use uma cor no formato #RRGGBB.") String color,
            @NotBlank @Pattern(regexp = "[a-z0-9-]{1,40}", message = "Ícone inválido.") String icon,
            Integer sortOrder,
            Boolean archived) {
    }

    public record FinanceCategoryResponse(UUID id, EntryKind kind, String name, String color, String icon,
            int sortOrder, boolean archived) {
    }

    public record CategoryRef(UUID id, String name, String color, String icon) {
    }

    // ------------------------------------------------------------------ lançamentos

    /** @param status padrão: CONFIRMED se a data já passou, PLANNED se é futura */
    public record TransactionRequest(
            @NotNull(message = "Informe se é receita ou despesa.") EntryKind kind,
            @NotNull(message = "Escolha a categoria.") UUID categoryId,
            UUID accountId,
            @NotNull(message = "Informe a data.") LocalDate occurredOn,
            @NotNull(message = "Informe o valor.") @Positive(message = "O valor deve ser maior que zero.")
            @Digits(integer = 15, fraction = 2, message = "Use no máximo 2 casas decimais.") BigDecimal amount,
            @NotNull(message = "Informe a moeda.") @Pattern(regexp = "[A-Z]{3}", message = "Moeda inválida.") String currency,
            @NotBlank(message = "Descreva o lançamento.") @Size(max = 160, message = "Máximo de 160 caracteres.") String description,
            TransactionStatus status) {
    }

    public record TransactionStatusRequest(@NotNull(message = "Informe o status.") TransactionStatus status) {
    }

    public enum TransactionSource {
        MANUAL,
        INSTALLMENT,
        RECURRING
    }

    /**
     * @param sourceLabel "Parcela 3/10" ou o nome da recorrência; nulo para avulsos
     */
    public record TransactionResponse(
            UUID id,
            EntryKind kind,
            LocalDate occurredOn,
            MoneyResponse amount,
            String description,
            CategoryRef category,
            @Schema(types = {"string", "null"}, format = "uuid") UUID accountId,
            TransactionStatus status,
            TransactionSource source,
            @Schema(types = {"string", "null"}) String sourceLabel,
            @Schema(types = {"string", "null"}, format = "uuid") UUID installmentPlanId,
            @Schema(types = {"string", "null"}, format = "uuid") UUID recurringEntryId) {
    }

    // ------------------------------------------------------------------ parcelamentos

    /** Informe o valor total OU o valor da parcela (um dos dois). */
    public record InstallmentPlanRequest(
            @NotNull(message = "Escolha a categoria.") UUID categoryId,
            UUID accountId,
            @NotBlank(message = "Descreva a compra.") @Size(max = 160, message = "Máximo de 160 caracteres.") String description,
            @NotNull(message = "Informe a moeda.") @Pattern(regexp = "[A-Z]{3}", message = "Moeda inválida.") String currency,
            @Positive(message = "O valor deve ser maior que zero.") @Digits(integer = 15, fraction = 2) BigDecimal totalAmount,
            @Positive(message = "O valor deve ser maior que zero.") @Digits(integer = 15, fraction = 2) BigDecimal installmentAmount,
            @NotNull(message = "Informe o número de parcelas.") @Min(value = 1, message = "Mínimo de 1 parcela.")
            @Max(value = 360, message = "Máximo de 360 parcelas.") Integer installmentsCount,
            @Min(value = 0, message = "Não pode ser negativo.") @Max(value = 359, message = "Máximo de 359.") Integer paidBeforeCount,
            @NotNull(message = "Informe o vencimento da próxima parcela.") LocalDate firstDueDate,
            PaymentMethod paymentMethod,
            @Size(max = 1000) String notes) {
    }

    public record InstallmentItem(int number, LocalDate dueDate, MoneyResponse amount, TransactionStatus status,
            UUID transactionId) {
    }

    /**
     * @param installmentAmount valor da parcela regular (a última pode ter centavos de diferença)
     * @param paidBeforeCount   parcelas pagas antes de entrar no app (contam em paidCount e paid)
     * @param firstDueDate      vencimento da primeira parcela acompanhada no app (nº paidBeforeCount + 1)
     * @param remainingCount    parcelas que faltam pagar
     * @param nextDueDate       próxima parcela não confirmada (nula se quitado)
     */
    public record InstallmentPlanResponse(
            UUID id,
            String description,
            CategoryRef category,
            @Schema(types = {"string", "null"}, format = "uuid") UUID accountId,
            MoneyResponse total,
            MoneyResponse installmentAmount,
            int installmentsCount,
            int paidBeforeCount,
            LocalDate firstDueDate,
            LocalDate lastDueDate,
            PaymentMethod paymentMethod,
            @Schema(types = {"string", "null"}) String notes,
            int paidCount,
            int remainingCount,
            MoneyResponse paid,
            MoneyResponse remaining,
            @Schema(types = {"string", "null"}, format = "date") LocalDate nextDueDate,
            List<InstallmentItem> installments) {
    }

    // ------------------------------------------------------------------ recorrências

    /**
     * @param intervalMonths obrigatório para EVERY_N_MONTHS
     * @param billingDay     dia de cobrança (1–31; padrão = dia de início). Ignorado no semanal.
     * @param autoConfirm    confirma sozinho no dia (ex.: débito automático, salário)
     */
    public record RecurringEntryRequest(
            @NotNull(message = "Escolha a categoria.") UUID categoryId,
            UUID accountId,
            @NotBlank(message = "Informe o nome.") @Size(max = 120, message = "Máximo de 120 caracteres.") String name,
            @NotNull(message = "Informe se é receita ou despesa.") EntryKind kind,
            @NotNull(message = "Informe o valor.") @Positive(message = "O valor deve ser maior que zero.")
            @Digits(integer = 15, fraction = 2) BigDecimal amount,
            @NotNull(message = "Informe a moeda.") @Pattern(regexp = "[A-Z]{3}", message = "Moeda inválida.") String currency,
            @NotNull(message = "Informe a frequência.") Frequency frequency,
            @Min(value = 1, message = "Mínimo de 1 mês.") @Max(value = 120, message = "Máximo de 120 meses.") Integer intervalMonths,
            @Min(value = 1, message = "Dia entre 1 e 31.") @Max(value = 31, message = "Dia entre 1 e 31.") Integer billingDay,
            @NotNull(message = "Informe o início.") LocalDate startDate,
            LocalDate endDate,
            Boolean autoConfirm,
            @Size(max = 1000) String notes) {
    }

    public record ActiveRequest(@NotNull Boolean active) {
    }

    /** @param monthlyEquivalent valor por mês (semanal × 52/12; anual ÷ 12; a cada N meses ÷ N) */
    public record RecurringEntryResponse(
            UUID id,
            String name,
            EntryKind kind,
            MoneyResponse amount,
            CategoryRef category,
            @Schema(types = {"string", "null"}, format = "uuid") UUID accountId,
            Frequency frequency,
            @Schema(types = {"integer", "null"}) Integer intervalMonths,
            @Schema(types = {"integer", "null"}) Integer billingDay,
            LocalDate startDate,
            @Schema(types = {"string", "null"}, format = "date") LocalDate endDate,
            boolean active,
            boolean autoConfirm,
            @Schema(types = {"string", "null"}) String notes,
            @Schema(types = {"string", "null"}, format = "date") LocalDate nextOccurrence,
            MoneyResponse monthlyEquivalent) {
    }

    // ------------------------------------------------------------------ metas mensais

    public record SavingGoalRequest(
            @NotNull(message = "Informe o valor.") @PositiveOrZero(message = "Valor inválido.")
            @Digits(integer = 15, fraction = 2) BigDecimal amount,
            @NotNull(message = "Informe a moeda.") @Pattern(regexp = "[A-Z]{3}", message = "Moeda inválida.") String currency) {
    }

    /** @param month "2026-11" */
    public record SavingGoalResponse(String month, MoneyResponse amount) {
    }

    // ------------------------------------------------------------------ visão mensal, séries e comprometido

    public record Amounts(MoneyResponse total, MoneyResponse confirmed, MoneyResponse planned) {
    }

    /**
     * @param oneOff       despesas avulsas
     * @param installments parcelas do mês
     * @param recurring    assinaturas/contas recorrentes do mês
     */
    public record ExpenseAmounts(MoneyResponse total, MoneyResponse confirmed, MoneyResponse planned,
            MoneyResponse oneOff, MoneyResponse installments, MoneyResponse recurring) {
    }

    public record CategoryTotal(CategoryRef category, MoneyResponse total, int count) {
    }

    /**
     * @param projectedSurplus receitas − despesas (previstas + confirmadas)
     * @param realizedSurplus  receitas − despesas, só confirmadas
     * @param goalRatio        sobra projetada ÷ meta (nulo sem meta)
     * @param pendingCount     lançamentos previstos em datas que já passaram (falta confirmar ou excluir)
     * @param committedNext12  parcelas + recorrências de despesa previstas nos próximos 12 meses
     */
    public record MonthlySummaryResponse(
            String month,
            String currency,
            Amounts income,
            ExpenseAmounts expenses,
            MoneyResponse projectedSurplus,
            MoneyResponse realizedSurplus,
            @Schema(types = {"object", "null"}) MoneyResponse savingGoal,
            @Schema(types = {"number", "null"}) BigDecimal goalRatio,
            List<CategoryTotal> expensesByCategory,
            List<CategoryTotal> incomeByCategory,
            List<TransactionResponse> transactions,
            int pendingCount,
            MoneyResponse committedNext12,
            List<ExchangeRateResponse> exchangeRates) {
    }

    /** Um mês da série: receitas, despesas, sobra e sobra acumulada no período. */
    public record MonthPoint(String month, MoneyResponse income, MoneyResponse expenses, MoneyResponse surplus,
            MoneyResponse cumulativeSurplus, @Schema(types = {"object", "null"}) MoneyResponse savingGoal) {
    }

    public record OverviewResponse(String currency, List<MonthPoint> months) {
    }

    public record CommitmentMonth(String month, MoneyResponse installments, MoneyResponse recurring, MoneyResponse total) {
    }

    public record CommitmentsResponse(String currency, MoneyResponse total, List<CommitmentMonth> months,
            List<TransactionResponse> upcoming) {
    }

    // ------------------------------------------------------------------ projeção do objetivo

    public record ProjectionMonth(String month, MoneyResponse plannedIncome, MoneyResponse plannedExpenses,
            MoneyResponse expectedSaving, MoneyResponse cumulative) {
    }

    /**
     * @param baselineMonthlySaving média mensal das sobras avulsas confirmadas nos meses-base
     * @param basisMonths           meses fechados usados na média
     * @param reachMonth            primeiro mês em que o acumulado alcança a meta (nulo se não alcança no horizonte)
     * @param deficitAtTarget       quanto falta no mês da data alvo (zero se alcança)
     */
    public record ProjectionResponse(
            UUID objectiveId,
            String currency,
            MoneyResponse target,
            MoneyResponse mobilized,
            MoneyResponse baselineMonthlySaving,
            List<String> basisMonths,
            boolean hasHistory,
            List<ProjectionMonth> months,
            @Schema(types = {"string", "null"}) String reachMonth,
            boolean reachesByTarget,
            MoneyResponse deficitAtTarget,
            List<ExchangeRateResponse> exchangeRates) {
    }
}
