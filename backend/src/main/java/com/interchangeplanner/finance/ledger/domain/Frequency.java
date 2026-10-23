package com.interchangeplanner.finance.ledger.domain;

/** Periodicidade de uma recorrência. EVERY_N_MONTHS usa {@code intervalMonths} (ex.: 3 = trimestral). */
public enum Frequency {
    WEEKLY,
    MONTHLY,
    YEARLY,
    EVERY_N_MONTHS
}
