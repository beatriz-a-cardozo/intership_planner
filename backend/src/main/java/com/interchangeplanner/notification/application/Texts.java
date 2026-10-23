package com.interchangeplanner.notification.application;

import com.interchangeplanner.shared.money.Money;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Currency;
import java.util.Locale;

/** Formatação dos textos das notificações (gravados prontos, em pt-BR). */
final class Texts {

    private static final Locale PT_BR = Locale.of("pt", "BR");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private Texts() {
    }

    static String date(LocalDate d) {
        return d.format(DATE);
    }

    static String money(Money m) {
        NumberFormat f = NumberFormat.getCurrencyInstance(PT_BR);
        f.setCurrency(Currency.getInstance(m.currency()));
        return f.format(m.amount());
    }
}
