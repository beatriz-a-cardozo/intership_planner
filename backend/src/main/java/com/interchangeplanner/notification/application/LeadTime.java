package com.interchangeplanner.notification.application;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Collection;

/**
 * Qual antecedência vale para uma data. Com antecedências {30, 7, 1} e o prazo daqui a 5 dias, vale a de 7: o aviso
 * "faltam 7 dias" sai uma vez (a chave de deduplicação inclui a antecedência). Se o job não rodar num dia, o aviso sai
 * no seguinte, porque basta estar dentro da janela. Datas passadas não geram aviso.
 */
public final class LeadTime {

    private LeadTime() {
    }

    /** A menor antecedência L com 0 ≤ dias até a data ≤ L; nulo se a data já passou ou está longe demais. */
    public static Integer bucket(LocalDate today, LocalDate date, Collection<Integer> leadDays) {
        if (date == null || leadDays.isEmpty()) {
            return null;
        }
        long days = ChronoUnit.DAYS.between(today, date);
        if (days < 0) {
            return null;
        }
        return leadDays.stream().filter(l -> days <= l).min(Integer::compare).orElse(null);
    }

    public static int max(Collection<Integer> leadDays) {
        return leadDays.stream().max(Integer::compare).orElse(0);
    }

    /** "hoje", "amanhã", "em 7 dias". */
    public static String phrase(LocalDate today, LocalDate date) {
        long days = ChronoUnit.DAYS.between(today, date);
        return days <= 0 ? "hoje" : days == 1 ? "amanhã" : "em " + days + " dias";
    }
}
