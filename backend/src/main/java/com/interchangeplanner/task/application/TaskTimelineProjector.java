package com.interchangeplanner.task.application;

import com.interchangeplanner.task.domain.Task;
import com.interchangeplanner.task.domain.TaskRepository;
import com.interchangeplanner.timeline.api.TimelineDtos.Kind;
import com.interchangeplanner.timeline.api.TimelineDtos.Source;
import com.interchangeplanner.timeline.api.TimelineDtos.TimelineItem;
import com.interchangeplanner.timeline.application.TimelineProjector;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Prazos das tarefas. */
@Component
public class TaskTimelineProjector implements TimelineProjector {

    private final TaskRepository tasks;

    public TaskTimelineProjector(TaskRepository tasks) {
        this.tasks = tasks;
    }

    @Override
    public Set<Source> sources() {
        return Set.of(Source.TASK);
    }

    @Override
    public List<TimelineItem> project(UUID userId, LocalDate from, LocalDate to, LocalDate today) {
        return tasks.findByUserIdAndDueDateBetween(userId, from, to).stream()
                .map(t -> toItem(t, today))
                .toList();
    }

    static TimelineItem toItem(Task t, LocalDate today) {
        return new TimelineItem("task:" + t.getId() + ":due", Source.TASK, Kind.TASK, t.getDueDate(), null, t.getTitle(),
                t.getCategory(), TimelineProjector.status(t.isDone(), t.getDueDate(), today), t.getPriority(), null,
                t.getId(), null, null, t.getDescription());
    }
}
