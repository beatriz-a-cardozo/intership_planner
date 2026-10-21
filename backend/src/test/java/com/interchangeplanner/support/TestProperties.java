package com.interchangeplanner.support;

import com.interchangeplanner.config.AppProperties;
import java.time.Duration;
import java.util.List;

/** {@link AppProperties} para testes unitários (sem contexto Spring). */
public final class TestProperties {

    public static final String JWT_SECRET = "unit-test-secret-0123456789abcdefghij";

    private TestProperties() {
    }

    public static AppProperties defaults() {
        return new AppProperties(
                "Interchange Planner",
                new AppProperties.Security(
                        new AppProperties.Jwt(JWT_SECRET, "interchange-planner", Duration.ofMinutes(15)),
                        new AppProperties.RefreshToken(Duration.ofDays(30), "ip_refresh", "/api/auth", true, "Strict"),
                        new AppProperties.Login(5, Duration.ofMinutes(15))),
                new AppProperties.Cors(List.of()));
    }
}
