package bg.nbu.banksystem.customer.domain;

import bg.nbu.banksystem.common.Identifiers;
import bg.nbu.banksystem.starter.error.DomainException;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Customer representing a legal entity identified by EIK.
 */
@Getter
@Entity
@Table(name = "legal_entity_customers")
@DiscriminatorValue("LEGAL_ENTITY")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LegalEntityCustomer extends Customer {

    @Column(name = "company_name", nullable = false, length = CustomerConstraints.COMPANY_NAME_MAX_LENGTH)
    private String companyName;

    @Column(name = "eik", nullable = false, unique = true, length = CustomerConstraints.EIK_LENGTH)
    private String eik;

    @Embedded
    @AttributeOverride(name = "countryCode", column = @Column(name = "registered_country_code", nullable = false,
            length = CustomerConstraints.COUNTRY_CODE_LENGTH))
    @AttributeOverride(name = "city",
            column = @Column(name = "registered_city", nullable = false, length = CustomerConstraints.CITY_MAX_LENGTH))
    @AttributeOverride(name = "postalCode", column = @Column(name = "registered_postal_code", nullable = false,
            length = CustomerConstraints.POSTAL_CODE_MAX_LENGTH))
    @AttributeOverride(name = "addressLine1", column = @Column(name = "registered_address_line_1", nullable = false,
            length = CustomerConstraints.ADDRESS_LINE_MAX_LENGTH))
    @AttributeOverride(name = "addressLine2",
            column = @Column(name = "registered_address_line_2", length = CustomerConstraints.ADDRESS_LINE_MAX_LENGTH))
    private Address registeredAddress;

    @Embedded
    @AttributeOverride(name = "countryCode", column = @Column(name = "correspondence_country_code", nullable = false,
            length = CustomerConstraints.COUNTRY_CODE_LENGTH))
    @AttributeOverride(name = "city", column = @Column(name = "correspondence_city", nullable = false,
            length = CustomerConstraints.CITY_MAX_LENGTH))
    @AttributeOverride(name = "postalCode", column = @Column(name = "correspondence_postal_code", nullable = false,
            length = CustomerConstraints.POSTAL_CODE_MAX_LENGTH))
    @AttributeOverride(name = "addressLine1", column = @Column(name = "correspondence_address_line_1", nullable = false,
            length = CustomerConstraints.ADDRESS_LINE_MAX_LENGTH))
    @AttributeOverride(name = "addressLine2", column = @Column(name = "correspondence_address_line_2",
            length = CustomerConstraints.ADDRESS_LINE_MAX_LENGTH))
    private Address correspondenceAddress;

    @OneToMany(mappedBy = "legalEntityCustomer", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @Getter(AccessLevel.NONE)
    private final List<Representative> representatives = new ArrayList<>();

    private LegalEntityCustomer(String customerNumber, String email, String phone, String companyName, String eik,
                                Address registeredAddress, Address correspondenceAddress) {
        super(Identifiers.newUuid(), customerNumber, email, phone);

        this.companyName = requireText(companyName, "companyName");
        this.eik = Eiks.normalize(eik);
        this.registeredAddress = Objects.requireNonNull(registeredAddress, "registeredAddress must not be null");
        this.correspondenceAddress = Objects.requireNonNull(correspondenceAddress,
                "correspondenceAddress must not be null");
    }

    /**
     * Registers a new legal-entity Customer.
     *
     * @param customerNumber        immutable operational Customer number
     * @param email                 Customer email
     * @param phone                 Customer phone
     * @param companyName           registered company name
     * @param eik                   9-digit Bulgarian legal-entity identifier
     * @param registeredAddress     registered company address
     * @param correspondenceAddress correspondence address
     * @return registered legal-entity Customer
     */
    public static LegalEntityCustomer register(String customerNumber, String email, String phone, String companyName,
                                               String eik, Address registeredAddress, Address correspondenceAddress) {
        return new LegalEntityCustomer(customerNumber, email, phone, companyName, eik, registeredAddress,
                correspondenceAddress);
    }

    @Override
    public CustomerType getCustomerType() {
        return CustomerType.LEGAL_ENTITY;
    }

    /**
     * Returns a read-only snapshot of all Representatives, including ended ones.
     *
     * @return current and historical Representatives
     */
    public List<Representative> getRepresentatives() {
        return List.copyOf(representatives);
    }

    /**
     * Updates the registered company name.
     *
     * @param companyName new registered company name
     * @throws DomainException with {@link CustomerErrorCode#CUSTOMER_CLOSED} if the Customer is closed
     */
    public void updateCompanyName(String companyName) {
        ensureActive();

        this.companyName = requireText(companyName, "companyName");
    }

    /**
     * Corrects an EIK entered incorrectly at registration. The application service must verify that the new EIK is
     * not registered to another Customer before calling this method.
     *
     * @param eik the correct EIK
     * @throws DomainException with {@link CustomerErrorCode#CUSTOMER_CLOSED} if the Customer is closed
     */
    public void correctEik(String eik) {
        ensureActive();

        this.eik = Eiks.normalize(eik);
    }

    /**
     * Replaces the registered address.
     *
     * @param registeredAddress new registered address
     * @throws DomainException with {@link CustomerErrorCode#CUSTOMER_CLOSED} if the Customer is closed
     */
    public void changeRegisteredAddress(Address registeredAddress) {
        ensureActive();

        this.registeredAddress = Objects.requireNonNull(registeredAddress, "registeredAddress must not be null");
    }

    /**
     * Replaces the correspondence address.
     *
     * @param correspondenceAddress new correspondence address
     * @throws DomainException with {@link CustomerErrorCode#CUSTOMER_CLOSED} if the Customer is closed
     */
    public void changeCorrespondenceAddress(Address correspondenceAddress) {
        ensureActive();

        this.correspondenceAddress = Objects.requireNonNull(correspondenceAddress,
                "correspondenceAddress must not be null");
    }

    /**
     * Adds a Representative to this legal entity and synchronizes both sides of the relationship.
     *
     * @param individualCustomer optional link to an existing individual Customer
     * @param firstName          representative first name
     * @param middleName         optional representative middle name
     * @param lastName           representative last name
     * @param role               representation role
     * @param validFrom          first valid date
     * @return the newly added Representative
     * @throws DomainException with {@link CustomerErrorCode#CUSTOMER_CLOSED} if the Customer is closed
     */
    public Representative addRepresentative(@Nullable IndividualCustomer individualCustomer, String firstName,
                                            @Nullable String middleName, String lastName, RepresentativeRole role,
                                            LocalDate validFrom) {
        ensureActive();

        Representative representative = Representative.create(this, individualCustomer, firstName, middleName, lastName,
                role, validFrom);
        representatives.add(representative);

        return representative;
    }

    /**
     * Ends a Representative's authority on the supplied date.
     *
     * @param representativeId identifier of a Representative of this legal entity
     * @param validTo          final date of representation
     * @return the ended Representative
     * @throws DomainException with {@link CustomerErrorCode#CUSTOMER_CLOSED} if the Customer is closed, with
     *                         {@link CustomerErrorCode#REPRESENTATIVE_NOT_FOUND} if the Representative does not belong
     *                         to this legal entity, or with a Representative period error
     */
    public Representative endRepresentation(UUID representativeId, LocalDate validTo) {
        ensureActive();

        Representative representative = findRepresentative(representativeId);
        representative.endRepresentation(validTo);

        return representative;
    }

    /**
     * Corrects a data-entry mistake in one of this legal entity's Representatives.
     *
     * @param representativeId   identifier of a Representative of this legal entity
     * @param individualCustomer linked individual Customer, if any
     * @param firstName          corrected first name
     * @param middleName         corrected middle name, if any
     * @param lastName           corrected last name
     * @param role               corrected role
     * @param validFrom          corrected first valid date
     * @param validTo            corrected final valid date, or {@code null} if the representation has not ended
     * @return the corrected Representative
     * @throws DomainException with {@link CustomerErrorCode#CUSTOMER_CLOSED} if the Customer is closed, with
     *                         {@link CustomerErrorCode#REPRESENTATIVE_NOT_FOUND} if the Representative does not belong
     *                         to this legal entity, or with a Representative period error
     */
    public Representative correctRepresentative(UUID representativeId, @Nullable IndividualCustomer individualCustomer,
                                                String firstName, @Nullable String middleName, String lastName,
                                                RepresentativeRole role, LocalDate validFrom,
                                                @Nullable LocalDate validTo) {
        ensureActive();

        Representative representative = findRepresentative(representativeId);
        representative.correct(individualCustomer, firstName, middleName, lastName, role, validFrom, validTo);

        return representative;
    }

    private Representative findRepresentative(UUID representativeId) {
        Objects.requireNonNull(representativeId, "representativeId must not be null");

        for (Representative representative : representatives) {
            if (representative.getId().equals(representativeId)) {
                return representative;
            }
        }

        throw new DomainException(CustomerErrorCode.REPRESENTATIVE_NOT_FOUND);
    }
}
