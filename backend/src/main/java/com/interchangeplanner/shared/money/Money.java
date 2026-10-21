package com.interchangeplanner.shared.money;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Valor monetário imutável. Escala interna 4 com HALF_EVEN; arredondar para 2 casas só na apresentação.
 *
 * <p>Somar moedas diferentes é proibido: {@link #plus} lança {@link CurrencyMismatchException}. Conversão
 * acontece apenas via {@code CurrencyConverter}.</p>
 */
public record Money(BigDecimal amount, String currency) implements Comparable<Money> {

    public static final int SCALE = 4;
    public static final RoundingMode ROUNDING = RoundingMode.HALF_EVEN;
    private static final Pattern CODE = Pattern.compile("[A-Z]{3}");

    public Money {
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(currency, "currency");
        if (!CODE.matcher(currency).matches()) {
            throw new IllegalArgumentException("Código de moeda inválido: " + currency);
        }
        amount = amount.setScale(SCALE, ROUNDING);
    }

    public static Money of(BigDecimal amount, String currency) {
        return new Money(amount, currency);
    }

    public static Money of(String amount, String currency) {
        return new Money(new BigDecimal(amount), currency);
    }

    public static Money zero(String currency) {
        return new Money(BigDecimal.ZERO, currency);
    }

    public Money plus(Money other) {
        requireSameCurrency(other);
        return new Money(amount.add(other.amount), currency);
    }

    public Money minus(Money other) {
        requireSameCurrency(other);
        return new Money(amount.subtract(other.amount), currency);
    }

    public Money times(BigDecimal factor) {
        return new Money(amount.multiply(factor), currency);
    }

    public Money times(long factor) {
        return times(BigDecimal.valueOf(factor));
    }

    /** Divide com arredondamento HALF_EVEN na escala interna. */
    public Money dividedBy(long divisor) {
        if (divisor == 0) {
            throw new ArithmeticException("Divisão por zero");
        }
        return new Money(amount.divide(BigDecimal.valueOf(divisor), SCALE, ROUNDING), currency);
    }

    public Money negate() {
        return new Money(amount.negate(), currency);
    }

    /** max(this, 0). */
    public Money atLeastZero() {
        return amount.signum() < 0 ? zero(currency) : this;
    }

    public Money min(Money other) {
        requireSameCurrency(other);
        return compareTo(other) <= 0 ? this : other;
    }

    public boolean isZero() {
        return amount.signum() == 0;
    }

    public boolean isPositive() {
        return amount.signum() > 0;
    }

    public boolean isNegative() {
        return amount.signum() < 0;
    }

    @Override
    public int compareTo(Money other) {
        requireSameCurrency(other);
        return amount.compareTo(other.amount);
    }

    private void requireSameCurrency(Money other) {
        if (!currency.equals(other.currency)) {
            throw new CurrencyMismatchException(currency, other.currency);
        }
    }

    @Override
    public String toString() {
        return amount.toPlainString() + " " + currency;
    }
}
