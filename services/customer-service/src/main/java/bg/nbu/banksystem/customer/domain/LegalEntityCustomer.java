package bg.nbu.banksystem.customer.domain;

import bg.nbu.banksystem.common.Identifiers;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Customer representing a legal entity identified by EIK.
 */
@Getter
@Entity
@Table(name = "legal_entity_customers")
@DiscriminatorValue("LEGAL_ENTITY")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LegalEntityCustomer extends Customer {

    @Column(
            name = "company_name",
            nullable = false,
            length = CustomerConstraints.COMPANY_NAME_MAX_LENGTH
    )
    private String companyName;

    @Column(
            name = "eik",
            nullable = false,
            updatable = false,
            unique = true,
            length = CustomerConstraints.EIK_MAX_LENGTH
    )
    private String eik;

    private LegalEntityCustomer(
            String customerNumber,
            String email,
            String phone,
            String companyName,
            String eik
    ) {
        super(
                Identifiers.newUuid(),
                customerNumber,
                email,
                phone
        );

        this.companyName = requireText(
                companyName,
                "companyName"
        );

        this.eik = requireText(
                eik,
                "eik"
        );
    }

    /**
     * Registers a new legal-entity Customer.
     *
     * @param customerNumber immutable operational Customer number
     * @param email          communication email
     * @param phone          communication phone
     * @param companyName    registered company name
     * @param eik            immutable Bulgarian legal-entity identifier
     * @return registered legal-entity Customer
     */
    public static LegalEntityCustomer register(
            String customerNumber,
            String email,
            String phone,
            String companyName,
            String eik
    ) {
        return new LegalEntityCustomer(
                customerNumber,
                email,
                phone,
                companyName,
                eik
        );
    }

    /**
     * Returns the concrete Customer type.
     *
     * @return legal-entity Customer type
     */
    @Override
    public CustomerType getCustomerType() {
        return CustomerType.LEGAL_ENTITY;
    }

    /**
     * Updates the registered company name.
     *
     * @param companyName new registered company name
     */
    public void updateCompanyName(String companyName) {
        this.companyName = requireText(
                companyName,
                "companyName"
        );
    }
}