package bg.nbu.banksystem.account.domain;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;
import lombok.Getter;
import org.hibernate.proxy.HibernateProxy;
import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Base class for persistent Account Service entities using application-assigned UUID identifiers.
 *
 * <p>
 * Provides stable identity, optimistic locking, and creation and modification auditing. Concrete
 * entities expose domain operations rather than unrestricted setters.
 * </p>
 */
@Getter
@MappedSuperclass
@Access(AccessType.FIELD)
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Version
    @Column(name = "version", nullable = false)
    private @Nullable Long version;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private @Nullable Instant createdAt;

    @CreatedBy
    @Column(name = "created_by", nullable = false, updatable = false)
    private @Nullable String createdBy;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private @Nullable Instant updatedAt;

    @LastModifiedBy
    @Column(name = "updated_by", nullable = false)
    private @Nullable String updatedBy;

    protected BaseEntity() {
    }

    /**
     * Creates an entity with an application-assigned identifier.
     *
     * @param id stable entity identifier
     */
    protected BaseEntity(UUID id) {
        this.id = Objects.requireNonNull(id, "id must not be null");
    }

    private static Class<?> effectiveClass(Object entity) {
        if (entity instanceof HibernateProxy proxy) {
            return proxy.getHibernateLazyInitializer().getPersistentClass();
        }

        return entity.getClass();
    }

    @SuppressWarnings("EqualsDoesntCheckParameterClass")
    @Override
    public final boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }

        if (other == null) {
            return false;
        }

        if (effectiveClass(this) != effectiveClass(other)) {
            return false;
        }

        BaseEntity that = (BaseEntity) other;
        return Objects.equals(id, that.id);
    }

    @Override
    public final int hashCode() {
        return effectiveClass(this).hashCode();
    }
}
