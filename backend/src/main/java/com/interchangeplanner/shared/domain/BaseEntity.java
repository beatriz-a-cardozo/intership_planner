package com.interchangeplanner.shared.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import java.util.Objects;
import java.util.UUID;
import org.hibernate.Hibernate;

/**
 * Entidade com identificador UUIDv7 atribuído na criação do objeto (não pelo banco):
 * o id existe antes do persist, então equals/hashCode são estáveis.
 */
@MappedSuperclass
public abstract class BaseEntity extends AuditedEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id = UuidV7.generate();

    public UUID getId() {
        return id;
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) {
            return false;
        }
        return Objects.equals(id, ((BaseEntity) o).id);
    }

    @Override
    public final int hashCode() {
        return Objects.hashCode(id);
    }
}
