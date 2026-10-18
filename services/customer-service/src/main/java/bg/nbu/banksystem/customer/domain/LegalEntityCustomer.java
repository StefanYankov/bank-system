package bg.nbu.banksystem.customer.domain;

import bg.nbu.banksystem.common.Identifiers;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Objects;

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

    @Column(name = "eik", nullable = false, updatable = false, unique = true, length = CustomerConstraints.EIK_MAX_LENGTH)
    private String eik;

    @Embedded
    @AttributeOverride(name = "countryCode", column = @Column(name = "registered_country_code", nullable = false, length = CustomerConstraints.COUNTRY_CODE_LENGTH))
    @AttributeOverride(name = "city", column = @Column(name = "registered_city", nullable = false, length = CustomerConstraints.CITY_MAX_LENGTH))
    @AttributeOverride(name = "postalCode", column = @Column(name = "registered_postal_code", nullable = false, length = CustomerConstraints.POSTAL_CODE_MAX_LENGTH))
    @AttributeOverride(name = "addressLine1", column = @Column(name = "registered_address_line_1", nullable = false, length = CustomerConstraints.ADDRESS_LINE_MAX_LENGTH))
    @AttributeOverride(name = "addressLine2", column = @Column(name = "registered_address_line_2", length = CustomerConstraints.ADDRESS_LINE_MAX_LENGTH))
    private Address registeredAddress;

    @Embedded
    @AttributeOverride(name = "countryCode", column = @Column(name = "correspondence_country_code", nullable = false, length = CustomerConstraints.COUNTRY_CODE_LENGTH))
    @AttributeOverride(name = "city", column = @Column(name = "correspondence_city", nullable = false, length = CustomerConstraints.CITY_MAX_LENGTH))
    @AttributeOverride(name = "postalCode", column = @Column(name = "correspondence_postal_code", nullable = false, length = CustomerConstraints.POSTAL_CODE_MAX_LENGTH))
    @AttributeOverride(name = "addressLine1", column = @Column(name = "correspondence_address_line_1", nullable = false, length = CustomerConstraints.ADDRESS_LINE_MAX_LENGTH))
    @AttributeOverride(name = "addressLine2", column = @Column(name = "correspondence_address_line_2", length = CustomerConstraints.ADDRESS_LINE_MAX_LENGTH))
    private Address correspondenceAddress;

    private LegalEntityCustomer(String customerNumber, String email, String phone, String companyName, String eik,
                                Address registeredAddress, Address correspondenceAddress) {
        super(Identifiers.newUuid(), customerNumber, email, phone);

        this.companyName = requireText(companyName, "companyName");

        this.eik = requireText(eik, "eik");

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
     * @param eik                   immutable Bulgarian legal-entity identifier
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

    public void updateCompanyName(String companyName) {
        this.companyName = requireText(companyName, "companyName");
    }

    /**
     * Replaces the registered address.
     *
     * @param registeredAddress new registered address
     */
    public void changeRegisteredAddress(Address registeredAddress) {
        this.registeredAddress = Objects.requireNonNull(registeredAddress, "registeredAddress must not be null");
    }

    /**
     * Replaces the correspondence address.
     *
     * @param correspondenceAddress new correspondence address
     */
    public void changeCorrespondenceAddress(Address correspondenceAddress) {
        this.correspondenceAddress = Objects.requireNonNull(correspondenceAddress,
                "correspondenceAddress must not be null");
    }
}