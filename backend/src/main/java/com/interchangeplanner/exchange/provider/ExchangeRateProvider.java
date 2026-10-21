package com.interchangeplanner.exchange.provider;

import java.time.LocalDate;
import java.util.List;

/** Fonte externa de cotações. Implementações novas entram sem mudar o serviço (ordem via configuração). */
public interface ExchangeRateProvider {

    /** Identificador usado em {@code app.exchange.providers} e gravado como {@code source}. */
    String id();

    /** Cotação mais recente: 1 base = rate quote. */
    FetchedRate latest(String base, String quote);

    /** Série diária (opcional). Provedores sem histórico retornam lista vazia. */
    default List<FetchedRate> history(String base, String quote, LocalDate from, LocalDate to) {
        return List.of();
    }
}
