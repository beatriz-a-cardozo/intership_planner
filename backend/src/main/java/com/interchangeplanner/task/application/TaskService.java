package com.interchangeplanner.task.application;

import com.interchangeplanner.applications.domain.Application;
import com.interchangeplanner.applications.domain.ApplicationRepository;
import com.interchangeplanner.expense.domain.Expense;
import com.interchangeplanner.expense.domain.ExpenseRepository;
import com.interchangeplanner.objective.domain.Objective;
import com.interchangeplanner.objective.domain.ObjectiveRepository;
import com.interchangeplanner.shared.domain.Priority;
import com.interchangeplanner.shared.error.BusinessRuleException;
import com.interchangeplanner.shared.error.NotFoundException;
import com.interchangeplanner.task.api.TaskDtos.LinkRef;
import com.interchangeplanner.task.api.TaskDtos.TaskRequest;
import com.interchangeplanner.task.api.TaskDtos.TaskResponse;
import com.interchangeplanner.task.domain.Task;
import com.interchangeplanner.task.domain.TaskRepository;
import com.interchangeplanner.task.domain.TaskStatus;
import com.interchangeplanner.university.domain.Program;
import com.interchangeplanner.university.domain.ProgramRepository;
import com.interchangeplanner.university.domain.University;
import com.interchangeplanner.university.domain.UniversityRepository;
import com.interchangeplanner.user.application.UserClock;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tarefas do planejamento. Vínculos (objetivo, universidade, gasto, candidatura) são validados contra o
 * usuário (a FK composta do banco também garante); a listagem carrega os rótulos dos vínculos em lote.
 * Ordem: abertas antes das concluídas; depois prazo (sem prazo por último), prioridade e criação.
 */
@Service
public class TaskService {

    public record Filter(TaskStatus status, Priority priority, LocalDate dueBefore, UUID objectiveId) {
    }

    private static final Comparator<Task> ORDER = Comparator.comparing(Task::isDone)
            .thenComparing(Task::getDueDate, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(Task::getPriority, Comparator.reverseOrder())
            .thenComparing(Task::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()));

    private final TaskRepository tasks;
    private final ObjectiveRepository objectives;
    private final UniversityRepository universities;
    private final ExpenseRepository expenses;
    private final ApplicationRepository applications;
    private final ProgramRepository programs;
    private final UserClock userClock;

    public TaskService(TaskRepository tasks, ObjectiveRepository objectives, UniversityRepository universities,
            ExpenseRepository expenses, ApplicationRepository applications, ProgramRepository programs,
            UserClock userClock) {
        this.tasks = tasks;
        this.objectives = objectives;
        this.universities = universities;
        this.expenses = expenses;
        this.applications = applications;
        this.programs = programs;
        this.userClock = userClock;
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> list(UUID userId, Filter f) {
        List<Task> list = tasks.findByUserId(userId).stream()
                .filter(t -> f.status() == null || t.getStatus() == f.status())
                .filter(t -> f.priority() == null || t.getPriority() == f.priority())
                .filter(t -> f.dueBefore() == null || (t.getDueDate() != null && !t.getDueDate().isAfter(f.dueBefore())))
                .filter(t -> f.objectiveId() == null || f.objectiveId().equals(t.getObjectiveId()))
                .sorted(ORDER)
                .toList();
        return toResponses(userId, list);
    }

    @Transactional(readOnly = true)
    public TaskResponse get(UUID userId, UUID id) {
        return toResponses(userId, List.of(require(userId, id))).getFirst();
    }

    @Transactional
    public TaskResponse create(UUID userId, TaskRequest r) {
        Task task = new Task(userId, toData(userId, r));
        task.changeStatus(r.status() == null ? TaskStatus.TODO : r.status(), userClock.clock().instant());
        return toResponses(userId, List.of(tasks.save(task))).getFirst();
    }

    @Transactional
    public TaskResponse update(UUID userId, UUID id, TaskRequest r) {
        Task task = require(userId, id);
        task.update(toData(userId, r));
        if (r.status() != null) {
            task.changeStatus(r.status(), userClock.clock().instant());
        }
        return toResponses(userId, List.of(task)).getFirst();
    }

    @Transactional
    public TaskResponse changeStatus(UUID userId, UUID id, TaskStatus status) {
        Task task = require(userId, id);
        task.changeStatus(status, userClock.clock().instant());
        return toResponses(userId, List.of(task)).getFirst();
    }

    @Transactional
    public void delete(UUID userId, UUID id) {
        tasks.delete(require(userId, id));
    }

    public Task require(UUID userId, UUID id) {
        return tasks.findByIdAndUserId(id, userId).orElseThrow(() -> new NotFoundException("Tarefa"));
    }

    private Task.Data toData(UUID userId, TaskRequest r) {
        if (r.objectiveId() != null && objectives.findByIdAndUserId(r.objectiveId(), userId).isEmpty()) {
            throw new BusinessRuleException("Objetivo não encontrado.", "objectiveId");
        }
        if (r.universityId() != null && universities.findByIdAndUserId(r.universityId(), userId).isEmpty()) {
            throw new BusinessRuleException("Universidade não encontrada.", "universityId");
        }
        if (r.expenseId() != null && expenses.findByIdAndUserId(r.expenseId(), userId).isEmpty()) {
            throw new BusinessRuleException("Gasto não encontrado.", "expenseId");
        }
        if (r.applicationId() != null && applications.findByIdAndUserId(r.applicationId(), userId).isEmpty()) {
            throw new BusinessRuleException("Candidatura não encontrada.", "applicationId");
        }
        return new Task.Data(r.title(), r.description(), r.priority(), r.dueDate(), r.category(), r.objectiveId(),
                r.universityId(), r.expenseId(), r.applicationId());
    }

    /** Monta as respostas carregando os rótulos dos vínculos com uma consulta por tipo. */
    List<TaskResponse> toResponses(UUID userId, List<Task> list) {
        LocalDate today = userClock.today(userId);
        Map<UUID, Objective> objs = index(objectives.findAllById(ids(list, Task::getObjectiveId)), userId,
                Objective::getUserId, Objective::getId);
        Map<UUID, University> unis = index(universities.findAllById(ids(list, Task::getUniversityId)), userId,
                University::getUserId, University::getId);
        Map<UUID, Expense> exps = index(expenses.findAllById(ids(list, Task::getExpenseId)), userId,
                Expense::getUserId, Expense::getId);
        Map<UUID, Application> apps = index(applications.findAllById(ids(list, Task::getApplicationId)), userId,
                Application::getUserId, Application::getId);
        Map<UUID, Program> progs = index(programs.findAllById(apps.values().stream().map(Application::getProgramId).toList()),
                userId, Program::getUserId, Program::getId);

        return list.stream().map(t -> {
            Objective o = objs.get(t.getObjectiveId());
            University u = unis.get(t.getUniversityId());
            Expense e = exps.get(t.getExpenseId());
            Application a = apps.get(t.getApplicationId());
            Program p = a == null ? null : progs.get(a.getProgramId());
            boolean overdue = !t.isDone() && t.getDueDate() != null && t.getDueDate().isBefore(today);
            return new TaskResponse(t.getId(), t.getTitle(), t.getDescription(), t.getPriority(), t.getStatus(),
                    t.getDueDate(), t.getCategory(), overdue, t.getCompletedAt(),
                    o == null ? null : new LinkRef(o.getId(), o.getName(), null),
                    u == null ? null : new LinkRef(u.getId(), u.getName(), null),
                    e == null ? null : new LinkRef(e.getId(), e.getName(), null),
                    a == null ? null : new LinkRef(a.getId(), p == null ? "Candidatura" : p.getName(), a.getProgramId()),
                    t.getCreatedAt());
        }).toList();
    }

    private static List<UUID> ids(Collection<Task> list, Function<Task, UUID> getter) {
        return list.stream().map(getter).filter(Objects::nonNull).distinct().toList();
    }

    private static <T> Map<UUID, T> index(List<T> found, UUID userId, Function<T, UUID> owner, Function<T, UUID> id) {
        return found.stream().filter(x -> userId.equals(owner.apply(x)))
                .collect(Collectors.toMap(id, Function.identity()));
    }
}
