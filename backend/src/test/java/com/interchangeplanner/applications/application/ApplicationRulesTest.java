package com.interchangeplanner.applications.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.interchangeplanner.applications.domain.ApplicationStatus;
import com.interchangeplanner.applications.domain.ApplicationStatus.Result;
import com.interchangeplanner.shared.error.BusinessRuleException;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class ApplicationRulesTest {

    @Test
    void resultadoEhDerivadoDoStatus() {
        assertThat(ApplicationStatus.ACCEPTED.result()).isEqualTo(Result.ACCEPTED);
        assertThat(ApplicationStatus.ENROLLED.result()).isEqualTo(Result.ACCEPTED);
        assertThat(ApplicationStatus.REJECTED.result()).isEqualTo(Result.REJECTED);
        assertThat(ApplicationStatus.WAITLISTED.result()).isEqualTo(Result.WAITLISTED);
        assertThat(ApplicationStatus.ITEM_REQUESTED.result()).isEqualTo(Result.PENDING);
        assertThat(ApplicationStatus.AWAITING_OPENING.result()).isEqualTo(Result.PENDING);
    }

    @Test
    void estimativaDeRespostaExigeMinimaProvavelMaximaEmOrdem() {
        LocalDate jan = LocalDate.of(2027, 1, 10);
        LocalDate feb = LocalDate.of(2027, 2, 10);
        LocalDate mar = LocalDate.of(2027, 3, 10);
        assertThatCode(() -> ApplicationService.validateEstimate(jan, feb, mar)).doesNotThrowAnyException();
        assertThatCode(() -> ApplicationService.validateEstimate(jan, null, mar)).doesNotThrowAnyException();
        assertThatCode(() -> ApplicationService.validateEstimate(feb, feb, feb)).doesNotThrowAnyException();
        assertThatThrownBy(() -> ApplicationService.validateEstimate(feb, jan, mar)).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> ApplicationService.validateEstimate(jan, mar, feb)).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> ApplicationService.validateEstimate(mar, null, jan)).isInstanceOf(BusinessRuleException.class);
    }
}
