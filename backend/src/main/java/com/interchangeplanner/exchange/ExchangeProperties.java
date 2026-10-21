package com.interchangeplanner.exchange;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuração de câmbio ({@code app.exchange.*}).
 *
 * @param providers   ordem de tentativa dos provedores (ids); vazio = nenhuma busca externa (ex.: testes)
 * @param staleAfter  idade a partir da qual a última cotação buscada é exibida como desatualizada
 * @param backfillDays dias de histórico buscados na primeira execução (gráfico de evolução)
 * @param pairs       pares acompanhados, com a cotação de reserva usada se nunca houve busca bem-sucedida
 */
@Validated
@ConfigurationProperties(prefix = "app.exchange")
public record ExchangeProperties(
        List<String> providers,
        @NotNull Duration staleAfter,
        @NotNull Duration httpTimeout,
        boolean refreshOnStartup,
        int backfillDays,
        @Valid @NotEmpty List<Pair> pairs,
        @Valid @NotNull Endpoint frankfurter,
        @Valid @NotNull Endpoint awesomeapi) {

    public ExchangeProperties {
        providers = providers == null ? List.of()
                : providers.stream().map(String::strip).filter(p -> !p.isEmpty()).toList();
    }

    public record Pair(
            @NotNull @Pattern(regexp = "[A-Z]{3}") String base,
            @NotNull @Pattern(regexp = "[A-Z]{3}") String quote,
            @NotNull @Positive BigDecimal fallbackRate) {
    }

    public record Endpoint(@NotBlank String baseUrl) {
    }
}
