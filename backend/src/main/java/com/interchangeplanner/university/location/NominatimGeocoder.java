package com.interchangeplanner.university.location;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * OpenStreetMap Nominatim (sem chave). A política de uso pede no máximo 1 requisição por segundo e um User-Agent
 * identificável: as chamadas são espaçadas aqui e cada cidade é consultada uma vez (cache + coordenadas gravadas).
 */
public class NominatimGeocoder implements Geocoder {

    private static final Logger log = LoggerFactory.getLogger(NominatimGeocoder.class);
    private static final long MIN_INTERVAL_MS = 1_100;

    record Place(String lat, String lon) {
    }

    private final RestClient http;
    private final String baseUrl;
    private final Map<String, Optional<Coordinates>> cache = new ConcurrentHashMap<>();
    private long lastCall;

    public NominatimGeocoder(String baseUrl, String userAgent) {
        Duration timeout = Duration.ofSeconds(5);
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(timeout).build());
        factory.setReadTimeout(timeout);
        this.http = RestClient.builder().requestFactory(factory).defaultHeader("User-Agent", userAgent).build();
        this.baseUrl = baseUrl.replaceAll("/+$", "");
    }

    @Override
    public Optional<Coordinates> locate(String city, String region, String countryCode) {
        String key = String.join("|", city, region == null ? "" : region, countryCode == null ? "" : countryCode);
        Optional<Coordinates> cached = cache.get(key);
        if (cached != null) {
            return cached;
        }
        Optional<Coordinates> found = query(city, region, countryCode);
        if (found.isEmpty() && region != null) {
            // o estado às vezes vem escrito diferente do OSM: tenta só cidade + país
            found = query(city, null, countryCode);
        }
        cache.put(key, found);
        return found;
    }

    private synchronized Optional<Coordinates> query(String city, String region, String countryCode) {
        long wait = lastCall + MIN_INTERVAL_MS - System.currentTimeMillis();
        try {
            if (wait > 0) {
                Thread.sleep(wait);
            }
            List<Place> places = http.get()
                    .uri(baseUrl + "/search?city={city}&state={state}&countrycodes={country}&format=jsonv2&limit=1",
                            city, region == null ? "" : region, countryCode == null ? "" : countryCode.toLowerCase())
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<Place>>() {
                    });
            if (places == null || places.isEmpty()) {
                return Optional.empty();
            }
            Place p = places.getFirst();
            return Optional.of(new Coordinates(scale(p.lat()), scale(p.lon())));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        } catch (RestClientException | NumberFormatException e) {
            log.warn("Geocodificação falhou para {} ({}): {}", city, countryCode, e.getMessage());
            return Optional.empty();
        } finally {
            lastCall = System.currentTimeMillis();
        }
    }

    private static BigDecimal scale(String v) {
        return new BigDecimal(v).setScale(6, RoundingMode.HALF_UP);
    }
}
