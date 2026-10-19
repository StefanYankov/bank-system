package bg.nbu.banksystem.customer.domain;

import bg.nbu.banksystem.common.Identifiers;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Represents a person authorized to act for a legal-entity Customer.
 */
@Getter
@Entity
@Table(name = "representatives")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Representative extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "legal_entity_customer_id", nullable = false, updatable = false)
    private LegalEntityCustomer legalEntityCustomer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "individual_customer_id")
    private @Nullable IndividualCustomer individualCustomer;

    @Column(name = "first_name", nullable = false, length = CustomerConstraints.NAME_MAX_LENGTH)
    private String firstName;

    @Column(name = "middle_name", length = CustomerConstraints.NAME_MAX_LENGTH)
    private @Nullable String middleName;

    @Column(name = "last_name", nullable = false, length = CustomerConstraints.NAME_MAX_LENGTH)
    private String lastName;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = CustomerConstraints.REPRESENTATIVE_ROLE_MAX_LENGTH)
    private RepresentativeRole role;

    @Column(name = "valid_from", nullable = false, updatable = false)
    private LocalDate validFrom;

    @Column(name = "valid_to")
    private @Nullable LocalDate validTo;

    private Representative(LegalEntityCustomer legalEntityCustomer, @Nullable IndividualCustomer individualCustomer,
                           String firstName, @Nullable String middleName, String lastName, RepresentativeRole role,
                           LocalDate validFrom) {
        super(Identifiers.newUuid());

        this.legalEntityCustomer = Objects.requireNonNull(legalEntityCustomer, "legalEntityCustomer must not be null");
        this.individualCustomer = individualCustomer;
        this.firstName = requireText(firstName, "firstName");
        this.middleName = normalizeOptionalText(middleName);
        this.lastName = requireText(lastName, "lastName");
        this.role = Objects.requireNonNull(role, "role must not be null");
        this.validFrom = Objects.requireNonNull(validFrom, "validFrom must not be null");
    }

    static Representative create(LegalEntityCustomer legalEntityCustomer,
                                 @Nullable IndividualCustomer individualCustomer, String firstName,
                                 @Nullable String middleName, String lastName, RepresentativeRole role,
                                 LocalDate validFrom) {
        return new Representative(legalEntityCustomer, individualCustomer, firstName, middleName, lastName, role,
                validFrom);
    }

    private static String requireText(@Nullable String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }

        return value.trim();
    }

    private static @Nullable String normalizeOptionalText(@Nullable String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }

    /**
     * Ends the Representative's authority on the supplied date.
     *
     * @param validTo final date of representation
     */
    public void endRepresentation(LocalDate validTo) {
        Objects.requireNonNull(validTo, "validTo must not be null");

        if (validTo.isBefore(validFrom)) {
            throw new IllegalArgumentException("validTo must not be before validFrom");
        }

        if (this.validTo != null) {
            throw new IllegalStateException("representation has already ended");
        }

        this.validTo = validTo;
    }

    /**
     * Indicates whether the representation is active on the supplied date.
     *
     * @param date date being evaluated
     * @return whether the Representative is active on that date
     */
    public boolean isActiveOn(LocalDate date) {
        Objects.requireNonNull(date, "date must not be null");

        if (date.isBefore(validFrom)) {
            return false;
        }

        if (validTo == null) {
            return true;
        }

        return !date.isAfter(validTo);
    }
}