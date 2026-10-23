package com.interchangeplanner.timeline;

import static org.assertj.core.api.Assertions.assertThat;

import com.interchangeplanner.applications.api.ApplicationDtos.ApplicationResponse;
import com.interchangeplanner.applications.api.ApplicationDtos.CycleWindow;
import com.interchangeplanner.applications.api.ApplicationDtos.ProgramRef;
import com.interchangeplanner.applications.application.ApplicationTimelineProjector;
import com.interchangeplanner.applications.domain.ApplicationStatus;
import com.interchangeplanner.expense.application.ExpenseTimelineProjector;
import com.interchangeplanner.expense.domain.Recurrence;
import com.interchangeplanner.timeline.api.TimelineDtos.ItemStatus;
import com.interchangeplanner.timeline.api.TimelineDtos.Kind;
import com.interchangeplanner.timeline.api.TimelineDtos.Source;
import com.interchangeplanner.timeline.api.TimelineDtos.TimelineItem;
import com.interchangeplanner.timeline.application.TimelineProjector;
import com.interchangeplanner.timeline.application.TimelineService;
import com.interchangeplanner.university.application.IntakeWindowCalculator;
import com.interchangeplanner.university.domain.UniversityEnums.Term;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TimelineRulesTest {

    static final LocalDate TODAY = LocalDate.of(2026, 10, 22);
    static final LocalDate FROM = LocalDate.of(2026, 1, 1);
    static final LocalDate TO = LocalDate.of(2028, 12, 31);

    static TimelineItem item(String id, LocalDate date, Kind kind, String title) {
        return new TimelineItem(id, Source.EVENT, kind, date, null, title, null, ItemStatus.PENDING, null, null,
                UUID.randomUUID(), null, null, null);
    }

    @Test
    void mergeOrdenaPorDataTipoETituloEUmIdApareceUmaVez() {
        LocalDate d = LocalDate.of(2027, 3, 1);
        var merged = TimelineService.merge(List.of(
                List.of(item("b", d, Kind.TASK, "Zeta"), item("a", d.minusDays(1), Kind.EVENT, "Antes")),
                List.of(item("c", d, Kind.DEADLINE, "Prazo"), item("b", d, Kind.TASK, "Duplicado"),
                        item("d", d, Kind.TASK, "alfa"))));
        assertThat(merged).extracting(TimelineItem::id).containsExactly("a", "c", "d", "b");
        assertThat(merged).extracting(TimelineItem::title).doesNotContain("Duplicado");
    }

    @Test
    void statusPelaDataEConclusao() {
        assertThat(TimelineProjector.status(true, TODAY.minusDays(5), TODAY)).isEqualTo(ItemStatus.DONE);
        assertThat(TimelineProjector.status(false, TODAY.minusDays(1), TODAY)).isEqualTo(ItemStatus.OVERDUE);
        assertThat(TimelineProjector.status(false, TODAY, TODAY)).isEqualTo(ItemStatus.PENDING);
    }

    @Test
    void ocorrenciasDeGastosRecorrentesRespeitamPeriodoEDataFinal() {
        LocalDate first = LocalDate.of(2027, 1, 31);
        assertThat(ExpenseTimelineProjector.occurrenceDates(Recurrence.MONTHLY, first, null, 3))
                .containsExactly(first, LocalDate.of(2027, 2, 28), LocalDate.of(2027, 3, 31));
        assertThat(ExpenseTimelineProjector.occurrenceDates(Recurrence.SEMIANNUAL, first, LocalDate.of(2027, 12, 31), 4))
                .containsExactly(first, LocalDate.of(2027, 7, 31));
        assertThat(ExpenseTimelineProjector.occurrenceDates(Recurrence.PER_UNIT, first, null, 1)).containsExactly(first);
        assertThat(ExpenseTimelineProjector.occurrenceDates(Recurrence.CUSTOM, first, null, 5)).containsExactly(first);
    }

    @Test
    void candidaturaGeraAberturaPrazoERespostaComIdsDeterministicos() {
        UUID id = UUID.fromString("00000000-0000-7000-8000-000000000001");
        var window = new CycleWindow(LocalDate.of(2027, 1, 1), LocalDate.of(2027, 5, 31), LocalDate.of(2027, 10, 1),
                IntakeWindowCalculator.State.NOT_OPEN_YET);
        var app = application(id, ApplicationStatus.AWAITING_OPENING, null, window, LocalDate.of(2027, 7, 1),
                LocalDate.of(2027, 7, 15), LocalDate.of(2027, 8, 10), null);

        var items = ApplicationTimelineProjector.applicationItems(app, FROM, TO, TODAY);
        assertThat(items).extracting(TimelineItem::id).containsExactly(
                "application:" + id + ":opens", "application:" + id + ":deadline", "application:" + id + ":response");
        assertThat(items.get(1).status()).isEqualTo(ItemStatus.PENDING);
        assertThat(items.get(2).date()).isEqualTo(LocalDate.of(2027, 7, 15));
        assertThat(items.get(2).endDate()).isEqualTo(LocalDate.of(2027, 8, 10));

        // enviada: a abertura some e o prazo fica cumprido; resposta recebida substitui a esperada
        var sent = application(id, ApplicationStatus.ACCEPTED, LocalDate.of(2027, 3, 1), window, LocalDate.of(2027, 7, 1),
                LocalDate.of(2027, 7, 15), null, LocalDate.of(2027, 6, 30));
        var sentItems = ApplicationTimelineProjector.applicationItems(sent, FROM, TO, LocalDate.of(2027, 7, 2));
        assertThat(sentItems).extracting(TimelineItem::kind).containsExactly(Kind.DEADLINE, Kind.RESPONSE);
        assertThat(sentItems).extracting(TimelineItem::status).containsOnly(ItemStatus.DONE);
        assertThat(sentItems.get(1).date()).isEqualTo(LocalDate.of(2027, 6, 30));
    }

    static ApplicationResponse application(UUID id, ApplicationStatus status, LocalDate submitted, CycleWindow window,
            LocalDate min, LocalDate likely, LocalDate max, LocalDate actual) {
        var program = new ProgramRef(UUID.randomUUID(), "Informatik", UUID.randomUUID(), "TUM", "München", "DE");
        return new ApplicationResponse(id, program, null, status, status.result(), 2027, Term.WINTER, false, submitted,
                null, min, likely, max, null, null, actual, null, window, 0, null);
    }
}
