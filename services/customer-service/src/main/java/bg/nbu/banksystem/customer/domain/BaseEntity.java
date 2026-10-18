package bg.nbu.banksystem.customer.domain;

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
 * Base class for persistent aggregate roots using application-assigned UUID identifiers.
 *
 * <p>
 * Provides stable identity, optimistic locking, and creation and modification
 * auditing. Concrete entities should expose domain operations rather than unrestricted
 * setters for their business state.
 * </p>
 *
 * <p>
 * The auditing fields record when and by whom the current database row was created
 * and last modified. They do not provide a complete history of all changes.
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

    /**
     * Constructor used by Hibernate when reconstructing an entity from persistence.
     */
    protected BaseEntity() {
    }

    /**
     * Creates a new entity with an application-assigned identifier.
     *
     * @param id the stable identifier of the new entity
     */
    protected BaseEntity(UUID id) {
        this.id = Objects.requireNonNull(id, "id must not be null");
    }

    private static Class<?> effectiveClass(BaseEntity entity) {
        if (entity instanceof HibernateProxy proxy) {
            return proxy.getHibernateLazyInitializer().getPersistentClass();
        }

        return entity.getClass();
    }

    private static @Nullable UUID effectiveIdentifier(BaseEntity entity) {
        if (entity instanceof HibernateProxy proxy) {
            return (UUID) proxy.getHibernateLazyInitializer().getIdentifier();
        }

        return entity.id;
    }

    /**
     * Compares persistent identity using the effective Hibernate entity type
     * and UUID identifier.
     *
     * @param other the object to compare
     * @return {@code true} when both objects represent the same persistent entity
     */
    @Override
    public final boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (!(other instanceof BaseEntity that)) {
            return false;
        }

        if (effectiveClass(this) != effectiveClass(that)) {
            return false;
        }

        UUID thisId = effectiveIdentifier(this);
        UUID otherId = effectiveIdentifier(that);

        return thisId != null && thisId.equals(otherId);
    }

    /**
     * Returns a stable hash code based on the effective persistent entity type.
     *
     * @return the effective entity type's hash code
     */
    @Override
    public final int hashCode() {
        return effectiveClass(this).hashCode();
    }
}