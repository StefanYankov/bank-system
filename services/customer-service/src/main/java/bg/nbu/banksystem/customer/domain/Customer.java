package bg.nbu.banksystem.customer.domain;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.Entity;
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
 *
 * <p>
 * Individual and legal-entity Customers share one stable Customer identity
 * and common contact information while protecting their subtype-specific
 * registration data separately.
 * </p>
 */
@Getter
@Entity
@Table(name = "customers")
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(
        name = "customer_type",
        discriminatorType = DiscriminatorType.STRING,
        length = CustomerConstraints.CUSTOMER_TYPE_MAX_LENGTH
)
@Access(AccessType.FIELD)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class Customer extends BaseEntity {

    @Column(
            name = "customer_number",
            nullable = false,
            updatable = false,
            unique = true,
            length = CustomerConstraints.CUSTOMER_NUMBER_MAX_LENGTH
    )
    private String customerNumber;

    @Column(
            name = "email",
            nullable = false,
            length = CustomerConstraints.EMAIL_MAX_LENGTH
    )
    private String email;

    @Column(
            name = "phone",
            nullable = false,
            length = CustomerConstraints.PHONE_MAX_LENGTH
    )
    private String phone;

    /**
     * Constructor used by concrete Customer registration operations.
     *
     * @param id             application-assigned Customer identifier
     * @param customerNumber immutable operational Customer number
     * @param email          communication email
     * @param phone          communication phone
     */
    protected Customer(
            UUID id,
            String customerNumber,
            String email,
            String phone
    ) {
        super(id);

        this.customerNumber = requireText(
                customerNumber,
                "customerNumber"
        );

        this.email = CustomerEmails.normalize(
                email
        );

        this.phone = requireText(
                phone,
                "phone"
        );
    }

    /**
     * Requires non-blank text and removes surrounding whitespace.
     *
     * @param value     supplied value
     * @param fieldName field used in the error message
     * @return trimmed value
     * @throws IllegalArgumentException if the value is null or blank
     */
    protected static String requireText(
            @Nullable String value,
            String fieldName
    ) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    fieldName + " must not be blank"
            );
        }

        return value.trim();
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
     * @param email new communication email
     * @param phone new communication phone
     */
    public void updateContactInformation(
            String email,
            String phone
    ) {
        this.email = CustomerEmails.normalize(
                email
        );

        this.phone = requireText(
                phone,
                "phone"
        );
    }
}