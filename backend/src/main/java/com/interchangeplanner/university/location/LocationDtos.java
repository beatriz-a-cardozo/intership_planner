package com.interchangeplanner.university.location;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public final class LocationDtos {

    private LocationDtos() {
    }

    @Schema(name = "MapLocation",
            description = "Universidade com programas escolhidos; sem coordenadas se a cidade não foi encontrada")
    public record MapLocation(UUID universityId, String name,
            @Schema(types = {"string", "null"}) String city,
            @Schema(types = {"string", "null"}) String region,
            @Schema(types = {"string", "null"}) String countryCode,
            @Schema(types = {"number", "null"}) BigDecimal latitude,
            @Schema(types = {"number", "null"}) BigDecimal longitude,
            List<MapProgram> programs) {
    }

    @Schema(name = "MapProgram")
    public record MapProgram(UUID id, String name, @Schema(types = {"string", "null"}) String degree) {
    }
}
