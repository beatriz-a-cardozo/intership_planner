package com.interchangeplanner.expense.domain;

/**
 * Periodicidade do gasto. O total é sempre {@code preço unitário × quantidade × ocorrências}; a periodicidade só
 * define as ocorrências SUGERIDAS a partir do horizonte do objetivo (mestrado de 24 meses: mensal = 24,
 * semestral = 4, anual = 2).
 */
public enum Recurrence {
    /** Uma vez (ocorrências = 1). */
    ONE_TIME,
    MONTHLY,
    SEMIANNUAL,
    ANNUAL,
    /** "Por UNI" da planilha: a quantidade é o número de unidades (ex.: 5 inscrições); ocorrências = 1. */
    PER_UNIT,
    /** Ocorrências informadas manualmente. */
    CUSTOM
}
