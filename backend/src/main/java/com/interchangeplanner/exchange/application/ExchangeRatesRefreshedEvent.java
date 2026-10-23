package com.interchangeplanner.exchange.application;

/** Publicado depois de cada atualização de cotações (alertas de câmbio escutam sem o câmbio conhecê-los). */
public record ExchangeRatesRefreshedEvent(int updated) {
}
