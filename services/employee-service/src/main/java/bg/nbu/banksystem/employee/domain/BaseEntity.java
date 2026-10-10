package bg.nbu.banksystem.employee.domain;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;
import lombok.Getter;
import org.hibernate.proxy.HibernateProxy;
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
    private Long version;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @CreatedBy
    @Column(name = "created_by", nullable = false, updatable = false)
    private String createdBy;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @LastModifiedBy
    @Column(name = "updated_by", nullable = false)
    private String updatedBy;

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

    /**
     * Compares persistent entity identity using the effective Hibernate entity type
     * and UUID identifier.
     *
     * @param other the object to compare
     * @return {@code true} when both objects represent the same persistent entity
     */
    @SuppressWarnings("EqualsDoesntCheckParameterClass") // Hibernate proxy-aware class comparison is performed through effectiveClass().
    @Override
    public final boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (other == null || effectiveClass(this) != effectiveClass(other)) {
            return false;
        }

        BaseEntity that = (BaseEntity) other;
        return id != null && id.equals(that.id);
    }

    /**
     * Returns a stable hash code based on the effective persistent entity type.
     *
     * @return the entity type's hash code
     */
    @Override
    public final int hashCode() {
        return effectiveClass(this).hashCode();
    }

    private static Class<?> effectiveClass(Object entity) {
        if (entity instanceof HibernateProxy proxy) {
            return proxy.getHibernateLazyInitializer().getPersistentClass();
        }

        return entity.getClass();
    }
}