package com.interchangeplanner.exchange.application;

import com.interchangeplanner.shared.money.Money;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Único ponto onde valores de moedas diferentes viram uma mesma moeda. Classe pura (sem Spring):
 * recebe as cotações já resolvidas para o usuário, então um cálculo inteiro usa sempre as mesmas taxas.
 */
public final class CurrencyConverter {

    private final Map<String, ResolvedRate> rates = new LinkedHashMap<>();

    public CurrencyConverter(Collection<ResolvedRate> rates) {
        rates.forEach(r -> this.rates.put(r.pairKey(), r));
    }

    public static CurrencyConverter identity() {
        return new CurrencyConverter(List.of());
    }

    public Money convert(Money money, String targetCurrency) {
        if (money.currency().equals(targetCurrency)) {
            return money;
        }
        return Money.of(money.amount().multiply(factor(money.currency(), targetCurrency)), targetCurrency);
    }

    /** Fator multiplicativo de {@code from} para {@code to} (1 se iguais). */
    public BigDecimal factor(String from, String to) {
        if (from.equals(to)) {
            return BigDecimal.ONE;
        }
        return rate(from, to).map(ResolvedRate::rate).orElseThrow(() -> new MissingExchangeRateException(from, to));
    }

    /** Cotação direta ou derivada do par inverso. */
    public Optional<ResolvedRate> rate(String base, String quote) {
        ResolvedRate direct = rates.get(ResolvedRate.key(base, quote));
        if (direct != null) {
            return Optional.of(direct);
        }
        return Optional.ofNullable(rates.get(ResolvedRate.key(quote, base))).map(ResolvedRate::inverse);
    }

    /** Cotações usadas (na direção configurada), para informar ao frontend. */
    public List<ResolvedRate> rates() {
        return List.copyOf(rates.values());
    }
}
