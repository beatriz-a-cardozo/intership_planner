package com.interchangeplanner.university.location;

import com.interchangeplanner.university.domain.Program;
import com.interchangeplanner.university.domain.ProgramRepository;
import com.interchangeplanner.university.domain.University;
import com.interchangeplanner.university.domain.UniversityRepository;
import com.interchangeplanner.university.location.LocationDtos.MapLocation;
import com.interchangeplanner.university.location.LocationDtos.MapProgram;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * Localidades das universidades com programas escolhidos. Coordenadas que faltam são descobertas pela cidade e
 * gravadas, então cada universidade é geocodificada uma vez. Sem transação aberta durante a consulta externa.
 */
@Service
public class UniversityLocationService {

    /** Limita a espera de uma requisição (o Nominatim pede 1 consulta/s); as que faltarem vêm na próxima. */
    static final int MAX_LOOKUPS_PER_REQUEST = 5;

    private final ProgramRepository programs;
    private final UniversityRepository universities;
    private final Geocoder geocoder;

    public UniversityLocationService(ProgramRepository programs, UniversityRepository universities, Geocoder geocoder) {
        this.programs = programs;
        this.universities = universities;
        this.geocoder = geocoder;
    }

    public List<MapLocation> chosenLocations(UUID userId) {
        Map<UUID, List<Program>> byUniversity = programs.findByUserIdAndChosenTrueOrderByNameAsc(userId).stream()
                .collect(Collectors.groupingBy(Program::getUniversityId, LinkedHashMap::new, Collectors.toList()));
        if (byUniversity.isEmpty()) {
            return List.of();
        }
        List<University> list = universities.findAllById(byUniversity.keySet()).stream()
                .filter(u -> u.getUserId().equals(userId))
                .sorted(Comparator.comparing(University::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
        int lookups = 0;
        for (University u : list) {
            if (u.getLatitude() == null && u.getCity() != null && lookups < MAX_LOOKUPS_PER_REQUEST) {
                lookups++;
                geocoder.locate(u.getCity(), u.getRegion(), u.getCountryCode()).ifPresent(c -> {
                    u.setCoordinates(c.latitude(), c.longitude());
                    universities.save(u);
                });
            }
        }
        return list.stream()
                .map(u -> new MapLocation(u.getId(), u.getName(), u.getCity(), u.getRegion(), u.getCountryCode(),
                        u.getLatitude(), u.getLongitude(), byUniversity.get(u.getId()).stream()
                                .map(p -> new MapProgram(p.getId(), p.getName(), p.getDegree()))
                                .toList()))
                .toList();
    }
}
