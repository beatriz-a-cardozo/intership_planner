package com.interchangeplanner.expense.application;

import com.interchangeplanner.expense.domain.ExpenseStatus;
import com.interchangeplanner.expense.domain.Recurrence;
import com.interchangeplanner.shared.error.BusinessRuleException;
import com.interchangeplanner.shared.money.Money;

/**
 * Regras de cálculo de gastos (plano técnico §2 e §4.4). Classe pura.
 *
 * <p>Substitui a fórmula {@code IF(Tipo="Mensal",P*12, ...)} da planilha: em vez de multiplicadores fixos,
 * {@code total = preço unitário × quantidade × ocorrências}, com ocorrências SUGERIDAS pelo horizonte do
 * objetivo e sempre gravadas explicitamente. Periodicidade desconhecida nunca vira zero.</p>
 */
public final class ExpenseCalculator {

    private ExpenseCalculator() {
    }

    /**
     * Ocorrências sugeridas para o horizonte (meses) do objetivo. Mestrado de 24 meses: mensal 24,
     * semestral 4, anual 2. Períodos parciais contam como uma ocorrência (arredonda para cima).
     *
     * @return sugestão, ou {@code null} para {@code CUSTOM} (sem sugestão possível)
     */
    public static Integer suggestedOccurrences(Recurrence recurrence, int horizonMonths) {
        if (horizonMonths < 1) {
            throw new IllegalArgumentException("Horizonte deve ser >= 1 mês");
        }
        return switch (recurrence) {
            case ONE_TIME, PER_UNIT -> 1;
            case MONTHLY -> horizonMonths;
            case SEMIANNUAL -> ceilDiv(horizonMonths, 6);
            case ANNUAL -> ceilDiv(horizonMonths, 12);
            case CUSTOM -> null;
        };
    }

    /**
     * Ocorrências a gravar: a informada pelo usuário ou, se ausente, a sugerida.
     * Único e por unidade têm sempre 1 ocorrência; personalizado exige o valor.
     */
    public static int resolveOccurrences(Recurrence recurrence, Integer requested, int horizonMonths) {
        if (requested != null && requested < 1) {
            throw new BusinessRuleException("As ocorrências devem ser pelo menos 1.", "occurrences");
        }
        return switch (recurrence) {
            case ONE_TIME, PER_UNIT -> {
                if (requested != null && requested != 1) {
                    throw new BusinessRuleException(
                            "Gastos únicos e por unidade têm uma ocorrência; use a quantidade para várias unidades.",
                            "occurrences");
                }
                yield 1;
            }
            case CUSTOM -> {
                if (requested == null) {
                    throw new BusinessRuleException("Informe o número de ocorrências.", "occurrences");
                }
                yield requested;
            }
            default -> requested != null ? requested : suggestedOccurrences(recurrence, horizonMonths);
        };
    }

    public static Money total(Money unitPrice, int quantity, int occurrences) {
        if (quantity < 1 || occurrences < 1) {
            throw new IllegalArgumentException("Quantidade e ocorrências devem ser >= 1");
        }
        return unitPrice.times((long) quantity * occurrences);
    }

    /**
     * Status derivado: cancelado é manual; senão compara o pago (na moeda do gasto) com o total.
     * Gasto de valor zero só fica pago se houver pagamento registrado.
     */
    public static ExpenseStatus status(boolean cancelled, Money total, Money paid) {
        if (cancelled) {
            return ExpenseStatus.CANCELLED;
        }
        if (!paid.isPositive()) {
            return ExpenseStatus.PENDING;
        }
        return paid.compareTo(total) >= 0 ? ExpenseStatus.PAID : ExpenseStatus.PARTIALLY_PAID;
    }

    /** max(total − pago, 0): pagar a mais não gera "restante negativo". */
    public static Money remaining(Money total, Money paid) {
        return total.minus(paid).atLeastZero();
    }

    private static int ceilDiv(int a, int b) {
        return Math.floorDiv(a + b - 1, b);
    }
}
