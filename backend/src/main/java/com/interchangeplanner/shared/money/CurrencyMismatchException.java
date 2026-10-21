package com.interchangeplanner.shared.money;

/** Erro de programação: operação aritmética entre moedas diferentes sem conversão explícita. */
public class CurrencyMismatchException extends IllegalArgumentException {

    public CurrencyMismatchException(String left, String right) {
        super("Operação entre moedas diferentes sem conversão: " + left + " e " + right);
    }
}
