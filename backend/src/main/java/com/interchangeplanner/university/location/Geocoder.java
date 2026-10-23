package com.interchangeplanner.university.location;

import java.math.BigDecimal;
import java.util.Optional;

/** Descobre as coordenadas de uma cidade (para o mapa do dashboard). Falhas viram vazio, nunca exceção. */
public interface Geocoder {

    record Coordinates(BigDecimal latitude, BigDecimal longitude) {
    }

    Optional<Coordinates> locate(String city, String region, String countryCode);
}
