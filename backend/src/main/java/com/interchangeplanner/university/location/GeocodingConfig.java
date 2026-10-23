package com.interchangeplanner.university.location;

import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** {@code app.geocoding.provider}: "nominatim" (padrão) ou vazio para desligar (testes, uso offline). */
@Configuration
public class GeocodingConfig {

    @Bean
    public Geocoder geocoder(@Value("${app.geocoding.provider:}") String provider,
            @Value("${app.geocoding.nominatim.base-url:https://nominatim.openstreetmap.org}") String baseUrl,
            @Value("${app.geocoding.user-agent:InterchangePlanner/1.0}") String userAgent) {
        if ("nominatim".equalsIgnoreCase(provider.strip())) {
            return new NominatimGeocoder(baseUrl, userAgent);
        }
        return (city, region, countryCode) -> Optional.empty();
    }
}
