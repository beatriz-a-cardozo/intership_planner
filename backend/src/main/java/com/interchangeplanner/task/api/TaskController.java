package com.interchangeplanner.task.api;

import com.interchangeplanner.shared.domain.Priority;
import com.interchangeplanner.shared.security.CurrentUser;
import com.interchangeplanner.task.api.TaskDtos.TaskRequest;
import com.interchangeplanner.task.api.TaskDtos.TaskResponse;
import com.interchangeplanner.task.api.TaskDtos.TaskStatusRequest;
import com.interchangeplanner.task.application.TaskService;
import com.interchangeplanner.task.domain.TaskStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
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
@RequestMapping("/api/tasks")
@Tag(name = "Tarefas")
public class TaskController {

    private final TaskService service;

    public TaskController(TaskService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Lista tarefas (abertas primeiro, por prazo e prioridade)")
    public List<TaskResponse> list(CurrentUser user, @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) Priority priority,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueBefore,
            @RequestParam(required = false) UUID objectiveId) {
        return service.list(user.id(), new TaskService.Filter(status, priority, dueBefore, objectiveId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cria tarefa")
    public TaskResponse create(CurrentUser user, @Valid @RequestBody TaskRequest r) {
        return service.create(user.id(), r);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalhe da tarefa")
    public TaskResponse get(CurrentUser user, @PathVariable UUID id) {
        return service.get(user.id(), id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Edita tarefa")
    public TaskResponse update(CurrentUser user, @PathVariable UUID id, @Valid @RequestBody TaskRequest r) {
        return service.update(user.id(), id, r);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Muda o status (concluir registra a data)")
    public TaskResponse changeStatus(CurrentUser user, @PathVariable UUID id, @Valid @RequestBody TaskStatusRequest r) {
        return service.changeStatus(user.id(), id, r.status());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Exclui tarefa")
    public ResponseEntity<Void> delete(CurrentUser user, @PathVariable UUID id) {
        service.delete(user.id(), id);
        return ResponseEntity.noContent().build();
    }
}
