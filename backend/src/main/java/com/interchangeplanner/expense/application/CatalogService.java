package com.interchangeplanner.expense.application;

import com.interchangeplanner.expense.api.CatalogRequest;
import com.interchangeplanner.expense.api.CatalogResponse;
import com.interchangeplanner.expense.domain.CatalogItem;
import com.interchangeplanner.expense.domain.CatalogRepository;
import com.interchangeplanner.shared.error.ConflictException;
import com.interchangeplanner.shared.error.NotFoundException;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;
import org.springframework.transaction.annotation.Transactional;

/** CRUD de catálogo por usuário, compartilhado por categorias e ciclos. */
public abstract class CatalogService<T extends CatalogItem> {

    @FunctionalInterface
    protected interface Factory<T> {
        T create(UUID userId, String name, String color, String icon, int sortOrder);
    }

    private final CatalogRepository<T> repository;
    private final Factory<T> factory;
    private final Predicate<UUID> inUse;
    private final String label;

    protected CatalogService(CatalogRepository<T> repository, Factory<T> factory, Predicate<UUID> inUse, String label) {
        this.repository = repository;
        this.factory = factory;
        this.inUse = inUse;
        this.label = label;
    }

    @Transactional(readOnly = true)
    public List<CatalogResponse> list(UUID userId) {
        return entities(userId).stream().map(CatalogResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<T> entities(UUID userId) {
        return repository.findByUserIdOrderBySortOrderAscNameAsc(userId);
    }

    @Transactional(readOnly = true)
    public T require(UUID userId, UUID id) {
        return repository.findByIdAndUserId(id, userId).orElseThrow(() -> new NotFoundException(label));
    }

    @Transactional
    public CatalogResponse create(UUID userId, CatalogRequest request) {
        if (repository.existsByUserIdAndNameIgnoreCase(userId, request.name().strip())) {
            throw duplicate();
        }
        int order = request.sortOrder() != null ? request.sortOrder() : entities(userId).size() + 1;
        T item = factory.create(userId, request.name(), request.color().toLowerCase(), request.icon(), order);
        applyExtras(item, request);
        return CatalogResponse.from(repository.save(item));
    }

    @Transactional
    public CatalogResponse update(UUID userId, UUID id, CatalogRequest request) {
        T item = require(userId, id);
        if (repository.existsByUserIdAndNameIgnoreCaseAndIdNot(userId, request.name().strip(), id)) {
            throw duplicate();
        }
        item.update(request.name(), request.color().toLowerCase(), request.icon(),
                request.sortOrder() != null ? request.sortOrder() : item.getSortOrder(),
                Boolean.TRUE.equals(request.archived()));
        applyExtras(item, request);
        return CatalogResponse.from(item);
    }

    /** Só apaga o que não está em uso; o que está em uso pode ser arquivado. */
    @Transactional
    public void delete(UUID userId, UUID id) {
        T item = require(userId, id);
        if (inUse.test(id)) {
            throw new ConflictException(label + " em uso por gastos. Arquive em vez de excluir.", "id");
        }
        repository.delete(item);
    }

    /** Campos próprios de um tipo de catálogo (ex.: "entra na meta" dos ciclos). */
    protected void applyExtras(T item, CatalogRequest request) {
    }

    private ConflictException duplicate() {
        return new ConflictException("Já existe " + label.toLowerCase() + " com esse nome.", "name");
    }
}
