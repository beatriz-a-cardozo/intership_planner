package com.interchangeplanner.shared.money;

import java.math.BigDecimal;

/** Valor monetário na API: arredondado para a escala de apresentação (2 casas). */
public record MoneyResponse(BigDecimal amount, String currency) {

    public static MoneyResponse of(Money money) {
        return new MoneyResponse(money.amount().setScale(2, Money.ROUNDING), money.currency());
    }
}
