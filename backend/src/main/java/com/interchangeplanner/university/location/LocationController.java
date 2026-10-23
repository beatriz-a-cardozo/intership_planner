package com.interchangeplanner.university.location;

import com.interchangeplanner.shared.security.CurrentUser;
import com.interchangeplanner.university.location.LocationDtos.MapLocation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Universidades e programas")
public class LocationController {

    private final UniversityLocationService locations;

    public LocationController(UniversityLocationService locations) {
        this.locations = locations;
    }

    @GetMapping("/api/universities/locations")
    @Operation(summary = "Localidades das universidades com programas escolhidos (mapa do dashboard)")
    public List<MapLocation> locations(CurrentUser user) {
        return locations.chosenLocations(user.id());
    }
}
