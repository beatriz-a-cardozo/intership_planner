package com.interchangeplanner.exchange.application;

import com.interchangeplanner.exchange.ExchangeProperties;
import com.interchangeplanner.exchange.domain.ExchangeRate;
import com.interchangeplanner.exchange.domain.ExchangeRateRepository;
import com.interchangeplanner.exchange.domain.UserExchangeRateOverride;
import com.interchangeplanner.exchange.domain.UserExchangeRateOverrideRepository;
import com.interchangeplanner.exchange.provider.ExchangeRateProvider;
import com.interchangeplanner.exchange.provider.FetchedRate;
import com.interchangeplanner.shared.error.BusinessRuleException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cotações: busca nos provedores (em ordem, com fallback), guarda histórico, mantém a vigente em cache
 * e resolve a taxa para cada usuário (a manual, se ativa, prevalece).
 *
 * <p>Falha do provedor nunca quebra a aplicação: mantém-se a última cotação válida, marcada como
 * {@code stale} quando passa de {@code app.exchange.stale-after}. Sem nenhuma cotação gravada, usa a
 * reserva configurada ({@code fallback-rate}), também marcada como desatualizada.</p>
 */
@Service
public class ExchangeRateService {

    private static final Logger log = LoggerFactory.getLogger(ExchangeRateService.class);

    public record RatePoint(LocalDate date, BigDecimal rate) {
    }

    private final ExchangeRateRepository rates;
    private final UserExchangeRateOverrideRepository overrides;
    private final List<ExchangeRateProvider> providers;
    private final ExchangeProperties properties;
    private final Clock clock;
    /** Última cotação gravada por par configurado (direção configurada). */
    private final Map<String, ExchangeRate> latest = new ConcurrentHashMap<>();

    public ExchangeRateService(ExchangeRateRepository rates, UserExchangeRateOverrideRepository overrides,
            List<ExchangeRateProvider> available, ExchangeProperties properties, Clock clock) {
        this.rates = rates;
        this.overrides = overrides;
        this.properties = properties;
        this.clock = clock;
        Map<String, ExchangeRateProvider> byId = available.stream()
                .collect(Collectors.toMap(ExchangeRateProvider::id, Function.identity()));
        this.providers = properties.providers().stream()
                .map(id -> {
                    ExchangeRateProvider p = byId.get(id);
                    if (p == null) {
                        throw new IllegalStateException("Provedor de câmbio desconhecido em app.exchange.providers: " + id);
                    }
                    return p;
                })
                .toList();
    }

    // ------------------------------------------------------------------ busca e gravação

    /** Atualiza todos os pares configurados. Retorna quantos foram atualizados com sucesso. */
    public int refreshAll() {
        int updated = 0;
        for (ExchangeProperties.Pair pair : properties.pairs()) {
            if (refresh(pair)) {
                updated++;
            }
        }
        return updated;
    }

    private boolean refresh(ExchangeProperties.Pair pair) {
        for (ExchangeRateProvider provider : providers) {
            try {
                record(provider.latest(pair.base(), pair.quote()));
                return true;
            } catch (RuntimeException e) {
                log.warn("Provedor de câmbio {} falhou para {}/{}: {}", provider.id(), pair.base(), pair.quote(),
                        e.getMessage());
            }
        }
        if (!providers.isEmpty()) {
            log.warn("Nenhum provedor respondeu para {}/{}; mantendo a última cotação válida", pair.base(), pair.quote());
        }
        return false;
    }

    /** Grava uma cotação obtida agora e a torna a vigente. */
    public ExchangeRate record(FetchedRate fetched) {
        ExchangeRate saved = rates.save(new ExchangeRate(fetched.base(), fetched.quote(), fetched.rate(),
                fetched.source(), fetched.effectiveDate(), clock.instant()));
        latest.put(ResolvedRate.key(saved.getBase(), saved.getQuote()), saved);
        return saved;
    }

    /** Primeira execução: preenche o histórico recente para o gráfico de evolução. */
    @Transactional
    public int backfillHistoryIfEmpty() {
        int inserted = 0;
        LocalDate today = LocalDate.now(clock);
        for (ExchangeProperties.Pair pair : properties.pairs()) {
            if (properties.backfillDays() <= 0 || rates.existsByBaseAndQuote(pair.base(), pair.quote())) {
                continue;
            }
            for (ExchangeRateProvider provider : providers) {
                try {
                    List<FetchedRate> series = provider.history(pair.base(), pair.quote(),
                            today.minusDays(properties.backfillDays()), today);
                    if (series.isEmpty()) {
                        continue;
                    }
                    // fetched_at = início do dia de referência: o histórico nunca parece "fresco"
                    rates.saveAll(series.stream().map(f -> new ExchangeRate(f.base(), f.quote(), f.rate(), f.source(),
                            f.effectiveDate(), f.effectiveDate().atStartOfDay().toInstant(ZoneOffset.UTC))).toList());
                    latest.remove(ResolvedRate.key(pair.base(), pair.quote()));
                    inserted += series.size();
                    break;
                } catch (RuntimeException e) {
                    log.warn("Histórico de câmbio indisponível em {}: {}", provider.id(), e.getMessage());
                }
            }
        }
        return inserted;
    }

    /** True se algum par não tem cotação ou ela está desatualizada (usado na inicialização). */
    public boolean needsRefresh() {
        return properties.pairs().stream().anyMatch(p -> market(p.base(), p.quote()).stale());
    }

    // ------------------------------------------------------------------ leitura

    /** Cotação de mercado (ignora a manual). */
    public ResolvedRate market(String base, String quote) {
        ExchangeProperties.Pair pair = pairFor(base, quote);
        String key = ResolvedRate.key(pair.base(), pair.quote());
        ExchangeRate stored = latest.computeIfAbsent(key,
                k -> rates.findFirstByBaseAndQuoteOrderByFetchedAtDesc(pair.base(), pair.quote()).orElse(null));
        ResolvedRate resolved = stored == null
                ? new ResolvedRate(pair.base(), pair.quote(), pair.fallbackRate(), ResolvedRate.SOURCE_FALLBACK,
                        null, null, true, false)
                : new ResolvedRate(stored.getBase(), stored.getQuote(), stored.getRate(), stored.getSource(),
                        stored.getEffectiveDate(), stored.getFetchedAt(), isStale(stored.getFetchedAt()), false);
        return pair.base().equals(base) ? resolved : resolved.inverse();
    }

    /** Cotação válida para o usuário: a manual ativa prevalece sobre a de mercado. */
    public ResolvedRate resolve(UUID userId, String base, String quote) {
        return manual(userId, base, quote).orElseGet(() -> market(base, quote));
    }

    /** Conversor com as taxas do usuário para todos os pares configurados. */
    public CurrencyConverter converterFor(UUID userId) {
        List<ResolvedRate> resolved = new ArrayList<>();
        for (ExchangeProperties.Pair pair : properties.pairs()) {
            resolved.add(resolve(userId, pair.base(), pair.quote()));
        }
        return new CurrencyConverter(resolved);
    }

    public List<RatePoint> history(String base, String quote, LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            throw new BusinessRuleException("A data inicial deve ser anterior à final.", "from");
        }
        ExchangeProperties.Pair pair = pairFor(base, quote);
        boolean inverse = !pair.base().equals(base);
        return rates.dailyHistory(pair.base(), pair.quote(), from, to).stream()
                .map(r -> {
                    ResolvedRate point = new ResolvedRate(r.getBase(), r.getQuote(), r.getRate(), r.getSource(),
                            r.getEffectiveDate(), r.getFetchedAt(), false, false);
                    return new RatePoint(r.getEffectiveDate(), (inverse ? point.inverse() : point).rate());
                })
                .toList();
    }

    // ------------------------------------------------------------------ cotação manual

    @Transactional
    public ResolvedRate setOverride(UUID userId, String base, String quote, BigDecimal rate) {
        pairFor(base, quote);
        if (rate == null || rate.signum() <= 0) {
            throw new BusinessRuleException("A cotação deve ser maior que zero.", "rate");
        }
        // Uma única cotação manual por par: grava na direção pedida e desativa a inversa.
        overrides.findByUserIdAndBaseAndQuote(userId, quote, base).ifPresent(UserExchangeRateOverride::deactivate);
        overrides.findByUserIdAndBaseAndQuote(userId, base, quote)
                .ifPresentOrElse(o -> o.activate(rate),
                        () -> overrides.save(new UserExchangeRateOverride(userId, base, quote, rate)));
        return resolve(userId, base, quote);
    }

    @Transactional
    public void clearOverride(UUID userId, String base, String quote) {
        overrides.findByUserIdAndBaseAndQuote(userId, base, quote).ifPresent(UserExchangeRateOverride::deactivate);
        overrides.findByUserIdAndBaseAndQuote(userId, quote, base).ifPresent(UserExchangeRateOverride::deactivate);
    }

    private Optional<ResolvedRate> manual(UUID userId, String base, String quote) {
        Optional<ResolvedRate> direct = overrides.findByUserIdAndBaseAndQuote(userId, base, quote)
                .filter(UserExchangeRateOverride::isActive)
                .map(this::toResolved);
        if (direct.isPresent()) {
            return direct;
        }
        return overrides.findByUserIdAndBaseAndQuote(userId, quote, base)
                .filter(UserExchangeRateOverride::isActive)
                .map(o -> toResolved(o).inverse());
    }

    private ResolvedRate toResolved(UserExchangeRateOverride o) {
        Instant when = o.getUpdatedAt() != null ? o.getUpdatedAt() : clock.instant();
        return new ResolvedRate(o.getBase(), o.getQuote(), o.getRate(), ResolvedRate.SOURCE_MANUAL, null, when,
                false, true);
    }

    private boolean isStale(Instant fetchedAt) {
        return fetchedAt.plus(properties.staleAfter()).isBefore(clock.instant());
    }

    /** Par configurado (em qualquer direção) ou erro de regra de negócio. */
    private ExchangeProperties.Pair pairFor(String base, String quote) {
        return properties.pairs().stream()
                .filter(p -> (p.base().equals(base) && p.quote().equals(quote))
                        || (p.base().equals(quote) && p.quote().equals(base)))
                .findFirst()
                .orElseThrow(() -> new BusinessRuleException("Par de moedas não suportado: " + base + "/" + quote,
                        "quote"));
    }
}
