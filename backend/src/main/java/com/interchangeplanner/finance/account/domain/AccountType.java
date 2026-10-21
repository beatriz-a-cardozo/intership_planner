package com.interchangeplanner.finance.account.domain;

/** BLOCKED = conta bloqueada (ex.: Sperrkonto). */
public enum AccountType {
    CHECKING,
    SAVINGS,
    INVESTMENT,
    CASH,
    BLOCKED,
    OTHER
}
