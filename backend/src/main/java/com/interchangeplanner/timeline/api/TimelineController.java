package com.interchangeplanner.timeline.api;

import com.interchangeplanner.shared.security.CurrentUser;
import com.interchangeplanner.timeline.api.TimelineDtos.EventDoneRequest;
import com.interchangeplanner.timeline.api.TimelineDtos.EventRequest;
import com.interchangeplanner.timeline.api.TimelineDtos.Source;
import com.interchangeplanner.timeline.api.TimelineDtos.TimelineItem;
import com.interchangeplanner.timeline.api.TimelineDtos.TimelineResponse;
import com.interchangeplanner.timeline.application.TimelineEventService;
import com.interchangeplanner.timeline.application.TimelineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/timeline")
@Tag(name = "Timeline")
public class TimelineController {

    private final TimelineService timeline;
    private final TimelineEventService events;

    public TimelineController(TimelineService timeline, TimelineEventService events) {
        this.timeline = timeline;
        this.events = events;
    }

    @GetMapping
    @Operation(summary = "Itens da timeline no período (padrão: mês anterior até 12 meses à frente; máx. 3 anos)")
    public TimelineResponse timeline(CurrentUser user,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Set<Source> sources) {
        return timeline.timeline(user.id(), from, to, sources);
    }

    @PostMapping("/events")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cria evento manual")
    public TimelineItem create(CurrentUser user, @Valid @RequestBody EventRequest r) {
        return events.create(user.id(), r);
    }

    @PutMapping("/events/{id}")
    @Operation(summary = "Edita evento manual")
    public TimelineItem update(CurrentUser user, @PathVariable UUID id, @Valid @RequestBody EventRequest r) {
        return events.update(user.id(), id, r);
    }

    @PatchMapping("/events/{id}/done")
    @Operation(summary = "Marca evento manual como feito / não feito")
    public TimelineItem done(CurrentUser user, @PathVariable UUID id, @Valid @RequestBody EventDoneRequest r) {
        return events.setDone(user.id(), id, r.done());
    }

    @DeleteMapping("/events/{id}")
    @Operation(summary = "Exclui evento manual")
    public ResponseEntity<Void> delete(CurrentUser user, @PathVariable UUID id) {
        events.delete(user.id(), id);
        return ResponseEntity.noContent().build();
    }
}
