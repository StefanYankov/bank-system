package bg.nbu.banksystem.customer.domain;

import bg.nbu.banksystem.starter.error.DomainException;
import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

/**
 * Base aggregate for Customers served by the bank.
 */
@Getter
@Entity
@Table(name = "customers")
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "customer_type", discriminatorType = DiscriminatorType.STRING, length = CustomerConstraints.CUSTOMER_TYPE_MAX_LENGTH)
@Access(AccessType.FIELD)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class Customer extends BaseEntity {

    @Column(name = "customer_number", nullable = false, updatable = false, unique = true, length = CustomerConstraints.CUSTOMER_NUMBER_MAX_LENGTH)
    private String customerNumber;

    @Column(name = "email", nullable = false, length = CustomerConstraints.EMAIL_MAX_LENGTH)
    private String email;

    @Column(name = "phone", nullable = false, length = CustomerConstraints.PHONE_MAX_LENGTH)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = CustomerConstraints.STATUS_MAX_LENGTH)
    private CustomerStatus status;

    /**
     * Constructor used by concrete Customer registration operations.
     *
     * @param id             application-assigned Customer identifier
     * @param customerNumber immutable operational Customer number
     * @param email          Customer email
     * @param phone          Customer phone
     */
    protected Customer(UUID id, String customerNumber, String email, String phone) {
        super(id);

        this.customerNumber = requireText(customerNumber, "customerNumber");
        this.email = CustomerEmails.normalize(email);
        this.phone = requireText(phone, "phone");
        this.status = CustomerStatus.ACTIVE;
    }

    /**
     * Requires non-blank text and removes surrounding whitespace.
     *
     * @param value     supplied value
     * @param fieldName field used in the error message
     * @return trimmed value
     * @throws IllegalArgumentException if the value is null or blank
     */
    protected static String requireText(@Nullable String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }

        return value.trim();
    }

    /**
     * Closes the bank's relationship with the Customer.
     *
     * @throws DomainException with {@link CustomerErrorCode#CUSTOMER_ALREADY_CLOSED}
     *                         if the Customer is already closed
     */
    public void close() {
        if (status == CustomerStatus.CLOSED) {
            throw new DomainException(CustomerErrorCode.CUSTOMER_ALREADY_CLOSED);
        }

        this.status = CustomerStatus.CLOSED;
    }

    /**
     * Reopens a closed Customer relationship.
     *
     * @throws DomainException with {@link CustomerErrorCode#CUSTOMER_ALREADY_ACTIVE}
     *                         if the Customer is already active
     */
    public void reopen() {
        if (status == CustomerStatus.ACTIVE) {
            throw new DomainException(CustomerErrorCode.CUSTOMER_ALREADY_ACTIVE);
        }

        this.status = CustomerStatus.ACTIVE;
    }

    /**
     * Rejects changes to a closed Customer.
     *
     * <p>
     * Every operation that changes Customer state, including subtype
     * operations, must call this method before changing anything.
     * </p>
     *
     * @throws DomainException with {@link CustomerErrorCode#CUSTOMER_CLOSED}
     *                         if the Customer is closed
     */
    protected void ensureActive() {
        if (status == CustomerStatus.CLOSED) {
            throw new DomainException(CustomerErrorCode.CUSTOMER_CLOSED);
        }
    }

    /**
     * Returns the concrete Customer type.
     *
     * @return concrete Customer type
     */
    @Transient
    public abstract CustomerType getCustomerType();

    /**
     * Updates shared Customer contact information.
     *
     * @param email new Customer email
     * @param phone new Customer phone
     * @throws DomainException with {@link CustomerErrorCode#CUSTOMER_CLOSED}
     *                         if the Customer is closed
     */
    public void updateContactInformation(String email, String phone) {
        ensureActive();

        this.email = CustomerEmails.normalize(email);
        this.phone = requireText(phone, "phone");
    }
}