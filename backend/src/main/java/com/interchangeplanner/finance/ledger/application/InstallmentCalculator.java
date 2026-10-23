package com.interchangeplanner.finance.ledger.application;

import com.interchangeplanner.shared.money.Money;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Divide um parcelamento em parcelas mensais. Classe pura.
 *
 * <ul>
 *   <li>Valores em centavos: todas as parcelas iguais (arredondadas para baixo) e a <b>última absorve a
 *       diferença</b>, de modo que a soma é exatamente o total (R$ 100 em 3x = 33,33 + 33,33 + 33,34).</li>
 *   <li>Vencimentos: sempre calculados a partir da 1ª data ({@code first.plusMonths(i)}), então um vencimento
 *       dia 31 vira 28/29 em fevereiro e volta a 31 em março, sem "escorregar".</li>
 *   <li>Parcelamento em andamento ({@link #inProgress}): as parcelas pagas antes de entrar no app não viram
 *       lançamentos; o valor delas conta como já pago e as datas partem do vencimento da próxima parcela.</li>
 * </ul>
 */
public final class InstallmentCalculator {

    public record Installment(int number, LocalDate dueDate, Money amount) {
    }

    /**
     * @param paidBefore soma das parcelas pagas antes de entrar no app (1..k)
     * @param open       parcelas k+1..N, com vencimentos a partir da próxima
     */
    public record Schedule(Money paidBefore, List<Installment> open) {
    }

    private InstallmentCalculator() {
    }

    public static List<Installment> schedule(Money total, int count, LocalDate firstDueDate) {
        return inProgress(total, count, 0, firstDueDate).open();
    }

    /**
     * Parcelamento de {@code count} parcelas do qual {@code paidBefore} já foram pagas fora do app.
     * Os valores seguem a divisão do total em {@code count} (centavos na última); a parcela k+1 vence em
     * {@code nextDueDate} e as seguintes mês a mês a partir dela.
     */
    public static Schedule inProgress(Money total, int count, int paidBefore, LocalDate nextDueDate) {
        if (count < 1) {
            throw new IllegalArgumentException("Número de parcelas deve ser >= 1");
        }
        if (paidBefore < 0 || paidBefore >= count) {
            throw new IllegalArgumentException("Parcelas já pagas deve estar entre 0 e count - 1");
        }
        if (!total.isPositive()) {
            throw new IllegalArgumentException("Total deve ser positivo");
        }
        BigDecimal cents = total.amount().setScale(2, RoundingMode.HALF_EVEN);
        BigDecimal regular = cents.divide(BigDecimal.valueOf(count), 2, RoundingMode.DOWN);
        BigDecimal last = cents.subtract(regular.multiply(BigDecimal.valueOf(count - 1L)));

        List<Installment> open = new ArrayList<>(count - paidBefore);
        for (int i = paidBefore; i < count; i++) {
            BigDecimal value = i == count - 1 ? last : regular;
            open.add(new Installment(i + 1, nextDueDate.plusMonths(i - paidBefore), Money.of(value, total.currency())));
        }
        Money before = Money.of(regular.multiply(BigDecimal.valueOf(paidBefore)), total.currency());
        return new Schedule(before, open);
    }

    /** Total a partir do valor da parcela (quando a pessoa informa "10x de R$ 219,80"). */
    public static Money totalFromInstallment(Money installment, int count) {
        return installment.times(count);
    }
}
