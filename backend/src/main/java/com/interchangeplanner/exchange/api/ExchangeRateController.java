package com.interchangeplanner.exchange.api;

import com.interchangeplanner.exchange.application.ExchangeRateService;
import com.interchangeplanner.shared.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/exchange-rates")
@Tag(name = "Câmbio")
public class ExchangeRateController {

    public record RatePointResponse(LocalDate date, java.math.BigDecimal rate) {
    }

    private final ExchangeRateService service;
    private final Clock clock;

    public ExchangeRateController(ExchangeRateService service, Clock clock) {
        this.service = service;
        this.clock = clock;
    }

    @GetMapping("/current")
    @Operation(summary = "Cotação vigente para o usuário (manual, se ativa; senão a de mercado)")
    public ExchangeRateResponse current(CurrentUser user,
            @RequestParam(defaultValue = "EUR") String base, @RequestParam(defaultValue = "BRL") String quote) {
        return ExchangeRateResponse.of(service.resolve(user.id(), base, quote), service.market(base, quote));
    }

    @GetMapping("/history")
    @Operation(summary = "Histórico diário da cotação de mercado (padrão: últimos 90 dias)")
    public List<RatePointResponse> history(
            @RequestParam(defaultValue = "EUR") String base, @RequestParam(defaultValue = "BRL") String quote,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        LocalDate end = to != null ? to : LocalDate.now(clock);
        LocalDate start = from != null ? from : end.minusDays(90);
        return service.history(base, quote, start, end).stream()
                .map(p -> new RatePointResponse(p.date(), p.rate()))
                .toList();
    }

    @PutMapping("/override")
    @Operation(summary = "Define uma cotação manual (prevalece até ser removida)")
    public ExchangeRateResponse setOverride(CurrentUser user, @Valid @RequestBody ExchangeRateOverrideRequest request) {
        return ExchangeRateResponse.of(
                service.setOverride(user.id(), request.base(), request.quote(), request.rate()),
                service.market(request.base(), request.quote()));
    }

    @DeleteMapping("/override")
    @Operation(summary = "Remove a cotação manual e volta a usar a de mercado")
    public ResponseEntity<Void> clearOverride(CurrentUser user,
            @RequestParam(defaultValue = "EUR") String base, @RequestParam(defaultValue = "BRL") String quote) {
        service.clearOverride(user.id(), base, quote);
        return ResponseEntity.noContent().build();
    }
}
