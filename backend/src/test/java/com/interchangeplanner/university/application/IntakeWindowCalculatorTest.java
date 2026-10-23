package com.interchangeplanner.university.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.interchangeplanner.university.application.IntakeWindowCalculator.Spec;
import com.interchangeplanner.university.application.IntakeWindowCalculator.State;
import com.interchangeplanner.university.application.IntakeWindowCalculator.Window;
import com.interchangeplanner.university.domain.UniversityEnums.Term;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class IntakeWindowCalculatorTest {

    static LocalDate d(int y, int m, int day) {
        return LocalDate.of(y, m, day);
    }

    /** "01/Dezembro - 01/Março" para o Wintersemester (início em outubro). */
    static final Spec WINTER_DEC_MAR = new Spec(Term.WINTER, 12, 1, 3, 1, 10);

    @Test
    void janelaQueAtravessaOAnoAbreEmDezembroDoAnoAnterior() {
        Window w = IntakeWindowCalculator.next(WINTER_DEC_MAR, d(2026, 10, 22));
        assertThat(w.intakeYear()).isEqualTo(2027);
        assertThat(w.opensOn()).isEqualTo(d(2026, 12, 1));
        assertThat(w.closesOn()).isEqualTo(d(2027, 3, 1));
        assertThat(w.startsOn()).isEqualTo(d(2027, 10, 1));
        assertThat(w.state()).isEqualTo(State.NOT_OPEN_YET);
    }

    @Test
    void dentroDaJanelaEstaAberta() {
        Window w = IntakeWindowCalculator.next(WINTER_DEC_MAR, d(2027, 1, 15));
        assertThat(w.intakeYear()).isEqualTo(2027);
        assertThat(w.state()).isEqualTo(State.OPEN);
        // no próprio dia do fechamento ainda está aberta
        assertThat(IntakeWindowCalculator.next(WINTER_DEC_MAR, d(2027, 3, 1)).state()).isEqualTo(State.OPEN);
        assertThat(IntakeWindowCalculator.next(WINTER_DEC_MAR, d(2027, 3, 2)).intakeYear()).isEqualTo(2028);
    }

    @Test
    void veraoComPrazoEmNovembroFechaNoAnoAnterior() {
        // "x - 15/Novembro" (Sommersemester, início em abril): sem abertura conhecida
        Window w = IntakeWindowCalculator.next(new Spec(Term.SUMMER, null, null, 11, 15, 4), d(2026, 10, 22));
        assertThat(w.intakeYear()).isEqualTo(2027);
        assertThat(w.closesOn()).isEqualTo(d(2026, 11, 15));
        assertThat(w.opensOn()).isNull();
        assertThat(w.state()).isEqualTo(State.OPEN);
    }

    @Test
    void prazoDoAnoJaPassadoPulaParaOProximoCiclo() {
        Window w = IntakeWindowCalculator.next(new Spec(Term.WINTER, 5, 1, 7, 15, 10), d(2026, 7, 20));
        assertThat(w.intakeYear()).isEqualTo(2027);
        assertThat(w.opensOn()).isEqualTo(d(2027, 5, 1));
        assertThat(w.closesOn()).isEqualTo(d(2027, 7, 15));
    }

    @Test
    void semPrazoConhecidoUsaOInicioDasAulas() {
        Window w = IntakeWindowCalculator.next(new Spec(Term.WINTER, null, null, null, null, 10), d(2026, 10, 22));
        assertThat(w.intakeYear()).isEqualTo(2027);
        assertThat(w.closesOn()).isNull();
        assertThat(w.state()).isEqualTo(State.NOT_OPEN_YET);
    }

    @Test
    void diaInexistenteUsaOUltimoDiaDoMes() {
        Window w = IntakeWindowCalculator.forYear(new Spec(Term.WINTER, null, null, 2, 29, 10), 2027, d(2026, 1, 1));
        assertThat(w.closesOn()).isEqualTo(d(2027, 2, 28));
    }
}
