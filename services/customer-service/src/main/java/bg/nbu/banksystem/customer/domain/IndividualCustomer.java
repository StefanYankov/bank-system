package bg.nbu.banksystem.customer.domain;

import bg.nbu.banksystem.common.Identifiers;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * Customer representing a physical person identified by EGN.
 */
@Getter
@Entity
@Table(name = "individual_customers")
@DiscriminatorValue("INDIVIDUAL")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IndividualCustomer extends Customer {

    @Column(name = "first_name", nullable = false, length = CustomerConstraints.NAME_MAX_LENGTH)
    private String firstName;

    @Column(name = "middle_name", length = CustomerConstraints.NAME_MAX_LENGTH)
    private @Nullable String middleName;

    @Column(name = "last_name", nullable = false, length = CustomerConstraints.NAME_MAX_LENGTH)
    private String lastName;

    @Column(name = "egn", nullable = false, unique = true, length = CustomerConstraints.EGN_LENGTH)
    private String egn;

    @Embedded
    private Address residentialAddress;

    private IndividualCustomer(String customerNumber, String email, String phone, String firstName,
                               @Nullable String middleName, String lastName, String egn, Address residentialAddress) {
        super(Identifiers.newUuid(), customerNumber, email, phone);

        this.firstName = requireText(firstName, "firstName");
        this.middleName = normalizeOptionalText(middleName);
        this.lastName = requireText(lastName, "lastName");
        this.egn = Egns.normalize(egn);
        this.residentialAddress = Objects.requireNonNull(residentialAddress, "residentialAddress must not be null");
    }

    /**
     * Registers a new individual Customer.
     *
     * @param customerNumber     immutable operational Customer number
     * @param email              Customer email
     * @param phone              Customer phone
     * @param firstName          first name
     * @param middleName         optional middle name
     * @param lastName           last name
     * @param egn                Bulgarian personal identifier
     * @param residentialAddress residential postal address
     * @return registered individual Customer
     */
    public static IndividualCustomer register(String customerNumber, String email, String phone, String firstName,
                                              @Nullable String middleName, String lastName, String egn,
                                              Address residentialAddress) {
        return new IndividualCustomer(customerNumber, email, phone, firstName, middleName, lastName, egn,
                residentialAddress);
    }

    @Override
    public CustomerType getCustomerType() {
        return CustomerType.INDIVIDUAL;
    }

    private static @Nullable String normalizeOptionalText(@Nullable String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }

    /**
     * Updates editable personal-name information. The EGN is changed through {@link #correctEgn(String)}.
     *
     * @param firstName  first name
     * @param middleName optional middle name
     * @param lastName   last name
     * @throws bg.nbu.banksystem.starter.error.DomainException with {@link CustomerErrorCode#CUSTOMER_CLOSED}
     *                                                         if the Customer is closed
     */
    public void updateName(String firstName, @Nullable String middleName, String lastName) {
        ensureActive();

        this.firstName = requireText(firstName, "firstName");
        this.middleName = normalizeOptionalText(middleName);
        this.lastName = requireText(lastName, "lastName");
    }

    /**
     * Replaces the complete residential address.
     *
     * @param residentialAddress new residential address
     * @throws bg.nbu.banksystem.starter.error.DomainException with {@link CustomerErrorCode#CUSTOMER_CLOSED}
     *                                                         if the Customer is closed
     */
    public void changeResidentialAddress(Address residentialAddress) {
        ensureActive();

        this.residentialAddress = Objects.requireNonNull(residentialAddress, "residentialAddress must not be null");
    }

    /**
     * Corrects an EGN entered incorrectly at registration. The application service must verify that the new EGN
     * is not registered to another Customer before calling this method.
     *
     * @param egn the correct EGN
     * @throws bg.nbu.banksystem.starter.error.DomainException with {@link CustomerErrorCode#CUSTOMER_CLOSED}
     *                                                         if the Customer is closed
     */
    public void correctEgn(String egn) {
        ensureActive();

        this.egn = Egns.normalize(egn);
    }
}