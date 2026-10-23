package com.interchangeplanner.timeline.application;

import com.interchangeplanner.objective.domain.ObjectiveRepository;
import com.interchangeplanner.shared.error.BusinessRuleException;
import com.interchangeplanner.shared.error.NotFoundException;
import com.interchangeplanner.timeline.api.TimelineDtos.EventRequest;
import com.interchangeplanner.timeline.api.TimelineDtos.Kind;
import com.interchangeplanner.timeline.api.TimelineDtos.Source;
import com.interchangeplanner.timeline.api.TimelineDtos.TimelineItem;
import com.interchangeplanner.timeline.domain.EventType;
import com.interchangeplanner.timeline.domain.TimelineEvent;
import com.interchangeplanner.timeline.domain.TimelineEventRepository;
import com.interchangeplanner.user.application.UserClock;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Eventos manuais: CRUD e projeção na timeline. */
@Service
public class TimelineEventService implements TimelineProjector {

    private final TimelineEventRepository events;
    private final ObjectiveRepository objectives;
    private final UserClock userClock;

    public TimelineEventService(TimelineEventRepository events, ObjectiveRepository objectives, UserClock userClock) {
        this.events = events;
        this.objectives = objectives;
        this.userClock = userClock;
    }

    @Transactional
    public TimelineItem create(UUID userId, EventRequest r) {
        return toItem(events.save(new TimelineEvent(userId, toData(userId, r))), userClock.today(userId));
    }

    @Transactional
    public TimelineItem update(UUID userId, UUID id, EventRequest r) {
        TimelineEvent e = require(userId, id);
        e.update(toData(userId, r));
        return toItem(e, userClock.today(userId));
    }

    @Transactional
    public TimelineItem setDone(UUID userId, UUID id, boolean done) {
        TimelineEvent e = require(userId, id);
        e.setDone(done);
        return toItem(e, userClock.today(userId));
    }

    @Transactional
    public void delete(UUID userId, UUID id) {
        events.delete(require(userId, id));
    }

    @Override
    public Set<Source> sources() {
        return Set.of(Source.EVENT);
    }

    @Override
    public List<TimelineItem> project(UUID userId, LocalDate from, LocalDate to, LocalDate today) {
        return events.overlapping(userId, from, to).stream().map(e -> toItem(e, today)).toList();
    }

    private TimelineEvent.Data toData(UUID userId, EventRequest r) {
        if (r.endDate() != null && r.endDate().isBefore(r.startDate())) {
            throw new BusinessRuleException("O fim deve ser no mesmo dia ou depois do início.", "endDate");
        }
        if (r.objectiveId() != null && objectives.findByIdAndUserId(r.objectiveId(), userId).isEmpty()) {
            throw new BusinessRuleException("Objetivo não encontrado.", "objectiveId");
        }
        return new TimelineEvent.Data(r.title(), r.description(), r.startDate(), r.endDate(), r.type(), r.priority(),
                Boolean.TRUE.equals(r.done()), r.objectiveId());
    }

    private TimelineEvent require(UUID userId, UUID id) {
        return events.findByIdAndUserId(id, userId).orElseThrow(() -> new NotFoundException("Evento"));
    }

    static TimelineItem toItem(TimelineEvent e, LocalDate today) {
        return new TimelineItem("event:" + e.getId(), Source.EVENT,
                e.getType() == EventType.MILESTONE ? Kind.MILESTONE : Kind.EVENT, e.getStartDate(), e.getEndDate(),
                e.getTitle(), null, TimelineProjector.status(e.isDone(), e.lastDay(), today), e.getPriority(), null,
                e.getId(), null, e.getType(), e.getDescription());
    }
}
