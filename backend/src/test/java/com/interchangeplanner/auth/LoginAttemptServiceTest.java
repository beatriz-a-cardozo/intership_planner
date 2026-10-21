package com.interchangeplanner.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.interchangeplanner.auth.application.LoginAttemptService;
import com.interchangeplanner.auth.application.TooManyLoginAttemptsException;
import com.interchangeplanner.support.MutableClock;
import com.interchangeplanner.support.TestProperties;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LoginAttemptServiceTest {

    private static final String EMAIL = "bia@example.com";

    private MutableClock clock;
    private LoginAttemptService service;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(Instant.parse("2026-10-21T12:00:00Z"));
        service = new LoginAttemptService(TestProperties.defaults(), clock);
    }

    private void fail(int times) {
        for (int i = 0; i < times; i++) {
            service.recordFailure(EMAIL);
        }
    }

    @Test
    void permiteAteOLimiteDeFalhas() {
        fail(4);
        assertThatCode(() -> service.checkAllowed(EMAIL)).doesNotThrowAnyException();
    }

    @Test
    void bloqueiaNaQuintaFalhaEInformaTempoRestante() {
        fail(5);
        clock.advance(Duration.ofMinutes(5));
        assertThatThrownBy(() -> service.checkAllowed(EMAIL))
                .isInstanceOfSatisfying(TooManyLoginAttemptsException.class,
                        e -> assertThat(e.getRetryAfter()).isEqualTo(Duration.ofMinutes(10)))
                .hasMessageContaining("10 minuto(s)");
    }

    @Test
    void liberaQuandoOBloqueioExpiraERecomecaAContagem() {
        fail(5);
        clock.advance(Duration.ofMinutes(15));
        assertThatCode(() -> service.checkAllowed(EMAIL)).doesNotThrowAnyException();

        fail(1);
        assertThatCode(() -> service.checkAllowed(EMAIL)).doesNotThrowAnyException();
    }

    @Test
    void sucessoZeraAsFalhas() {
        fail(4);
        service.recordSuccess(EMAIL);
        fail(4);
        assertThatCode(() -> service.checkAllowed(EMAIL)).doesNotThrowAnyException();
    }

    @Test
    void contagemEhPorEmail() {
        fail(5);
        assertThatCode(() -> service.checkAllowed("outra@example.com")).doesNotThrowAnyException();
    }
}
