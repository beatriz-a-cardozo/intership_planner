package com.interchangeplanner.user.application;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** "Hoje" no fuso do usuário: atraso, mês corrente e próximos eventos dependem dele. */
@Component
public class UserClock {

    private final UserService users;
    private final Clock clock;

    public UserClock(UserService users, Clock clock) {
        this.users = users;
        this.clock = clock;
    }

    public LocalDate today(UUID userId) {
        return LocalDate.now(clock.withZone(ZoneId.of(users.settings(userId).timezone())));
    }

    public Clock clock() {
        return clock;
    }
}
