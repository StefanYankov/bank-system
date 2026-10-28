package bg.nbu.banksystem.customer.domain;

import bg.nbu.banksystem.common.Identifiers;
import bg.nbu.banksystem.starter.error.DomainException;
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

    @Column(name = "valid_from", nullable = false)
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

    /**
     * Ends the representation on the supplied date.
     *
     * <p>
     * Package-private: callers use {@link LegalEntityCustomer#endRepresentation(java.util.UUID, LocalDate)}, which
     * first checks that the legal entity is not closed.
     * </p>
     *
     * @param validTo final date of representation
     * @throws DomainException with {@link CustomerErrorCode#REPRESENTATION_ALREADY_ENDED} if the representation has
     *                         already ended, or with {@link CustomerErrorCode#INVALID_REPRESENTATION_PERIOD} if the end
     *                         date precedes the start date
     */
    void endRepresentation(LocalDate validTo) {
        Objects.requireNonNull(validTo, "validTo must not be null");

        if (this.validTo != null) {
            throw new DomainException(CustomerErrorCode.REPRESENTATION_ALREADY_ENDED);
        }

        if (validTo.isBefore(validFrom)) {
            throw new DomainException(CustomerErrorCode.INVALID_REPRESENTATION_PERIOD);
        }

        this.validTo = validTo;
    }

    /**
     * Corrects a data-entry mistake in this representation.
     *
     * <p>
     * A correction replaces the recorded values because the wrong values never reflected reality. A real change,
     * such as the same person representing the legal entity again after a gap, is recorded as a new representation
     * instead. Package-private: callers use {@link LegalEntityCustomer#correctRepresentative}, which first checks
     * that the legal entity is not closed.
     * </p>
     *
     * @param individualCustomer linked individual Customer, if any
     * @param firstName          corrected first name
     * @param middleName         corrected middle name, if any
     * @param lastName           corrected last name
     * @param role               corrected role
     * @param validFrom          corrected first valid date
     * @param validTo            corrected final valid date, or {@code null} if the representation has not ended
     * @throws DomainException with {@link CustomerErrorCode#INVALID_REPRESENTATION_PERIOD} if the end date precedes
     *                         the start date
     */
    void correct(@Nullable IndividualCustomer individualCustomer, String firstName, @Nullable String middleName,
                 String lastName, RepresentativeRole role, LocalDate validFrom, @Nullable LocalDate validTo) {
        String correctedFirstName = requireText(firstName, "firstName");
        String correctedLastName = requireText(lastName, "lastName");
        RepresentativeRole correctedRole = Objects.requireNonNull(role, "role must not be null");
        LocalDate correctedValidFrom = Objects.requireNonNull(validFrom, "validFrom must not be null");

        if (validTo != null && validTo.isBefore(correctedValidFrom)) {
            throw new DomainException(CustomerErrorCode.INVALID_REPRESENTATION_PERIOD);
        }

        this.individualCustomer = individualCustomer;
        this.firstName = correctedFirstName;
        this.middleName = normalizeOptionalText(middleName);
        this.lastName = correctedLastName;
        this.role = correctedRole;
        this.validFrom = correctedValidFrom;

        this.validTo = validTo;
    }

    /**
     * Indicates whether the representation is active on the supplied date. Both {@code validFrom} and
     * {@code validTo} are inclusive.
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

    private static @Nullable String normalizeOptionalText(@Nullable String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }
}