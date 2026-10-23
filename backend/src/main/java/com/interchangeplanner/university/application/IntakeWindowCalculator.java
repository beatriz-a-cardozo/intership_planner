package com.interchangeplanner.university.application;

import com.interchangeplanner.university.domain.UniversityEnums.Term;
import java.time.LocalDate;
import java.time.YearMonth;

/**
 * Próxima janela de inscrição concreta a partir da janela recorrente do intake (dia/mês). Classe pura.
 *
 * <p>Para o intake com início das aulas no mês S do ano Y:</p>
 * <ul>
 *   <li>fechamento (mês F): no ano Y se F ≤ S, senão em Y−1 (ex.: inverno com início em out/Y fecha em
 *       15/jul/Y; verão com início em abr/Y fechando em "15/Novembro" fecha em nov/Y−1);</li>
 *   <li>abertura: o dia/mês mais recente antes do fechamento (ex.: "01/Dezembro – 01/Março" abre em dez/Y−1);</li>
 *   <li>a próxima janela é a do menor Y cujo fechamento (ou, sem fechamento, o início das aulas) ainda não passou.</li>
 * </ul>
 * Dias inexistentes no mês (31/abr, 29/fev) usam o último dia do mês.
 */
public final class IntakeWindowCalculator {

    public record Spec(Term term, Integer openMonth, Integer openDay, Integer closeMonth, Integer closeDay,
            int startMonth) {
    }

    public enum State {
        NOT_OPEN_YET,
        OPEN,
        CLOSED
    }

    /**
     * @param opensOn  nulo se a abertura não é conhecida
     * @param closesOn nulo se o prazo não é conhecido
     * @param state    em relação a "hoje"
     */
    public record Window(Term term, int intakeYear, LocalDate opensOn, LocalDate closesOn, LocalDate startsOn,
            State state) {
    }

    private IntakeWindowCalculator() {
    }

    public static Window next(Spec spec, LocalDate today) {
        for (int year = today.getYear(); year <= today.getYear() + 3; year++) {
            Window w = forYear(spec, year, today);
            LocalDate reference = w.closesOn() != null ? w.closesOn() : w.startsOn();
            if (!reference.isBefore(today)) {
                return w;
            }
        }
        // inalcançável com meses válidos; mantém o contrato
        return forYear(spec, today.getYear() + 1, today);
    }

    public static Window forYear(Spec spec, int intakeYear, LocalDate today) {
        LocalDate startsOn = LocalDate.of(intakeYear, spec.startMonth(), 1);
        LocalDate closesOn = null;
        if (spec.closeMonth() != null) {
            int closeYear = spec.closeMonth() <= spec.startMonth() ? intakeYear : intakeYear - 1;
            closesOn = date(closeYear, spec.closeMonth(), spec.closeDay());
        }
        LocalDate opensOn = null;
        if (spec.openMonth() != null) {
            if (closesOn != null) {
                LocalDate sameYear = date(closesOn.getYear(), spec.openMonth(), spec.openDay());
                opensOn = sameYear.isAfter(closesOn) ? date(closesOn.getYear() - 1, spec.openMonth(), spec.openDay()) : sameYear;
            } else {
                int openYear = spec.openMonth() <= spec.startMonth() ? intakeYear : intakeYear - 1;
                opensOn = date(openYear, spec.openMonth(), spec.openDay());
            }
        }
        return new Window(spec.term(), intakeYear, opensOn, closesOn, startsOn, state(opensOn, closesOn, today));
    }

    private static State state(LocalDate opensOn, LocalDate closesOn, LocalDate today) {
        if (closesOn != null && today.isAfter(closesOn)) {
            return State.CLOSED;
        }
        if (opensOn != null && today.isBefore(opensOn)) {
            return State.NOT_OPEN_YET;
        }
        return opensOn == null && closesOn == null ? State.NOT_OPEN_YET : State.OPEN;
    }

    private static LocalDate date(int year, int month, int day) {
        YearMonth ym = YearMonth.of(year, month);
        return ym.atDay(Math.min(day, ym.lengthOfMonth()));
    }
}
