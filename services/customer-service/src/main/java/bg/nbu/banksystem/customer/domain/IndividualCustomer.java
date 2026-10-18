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

    @Column(name = "egn", nullable = false, updatable = false, unique = true, length = CustomerConstraints.EGN_LENGTH)
    private String egn;

    @Embedded
    @AttributeOverride(name = "countryCode", column = @Column(name = "residential_country_code", nullable = false, length = CustomerConstraints.COUNTRY_CODE_LENGTH))
    @AttributeOverride(name = "city", column = @Column(name = "residential_city", nullable = false, length = CustomerConstraints.CITY_MAX_LENGTH))
    @AttributeOverride(name = "postalCode", column = @Column(name = "residential_postal_code", nullable = false, length = CustomerConstraints.POSTAL_CODE_MAX_LENGTH))
    @AttributeOverride(name = "addressLine1", column = @Column(name = "residential_address_line_1", nullable = false, length = CustomerConstraints.ADDRESS_LINE_MAX_LENGTH))
    @AttributeOverride(name = "addressLine2", column = @Column(name = "residential_address_line_2", length = CustomerConstraints.ADDRESS_LINE_MAX_LENGTH))
    private Address residentialAddress;

    private IndividualCustomer(String customerNumber, String email, String phone, String firstName,
                               @Nullable String middleName, String lastName, String egn, Address residentialAddress) {
        super(Identifiers.newUuid(), customerNumber, email, phone);

        this.firstName = requireText(firstName, "firstName");

        this.middleName = normalizeOptionalText(middleName);

        this.lastName = requireText(lastName, "lastName");

        this.egn = requireText(egn, "egn");

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
     * @param egn                immutable Bulgarian personal identifier
     * @param residentialAddress residential postal address
     * @return registered individual Customer
     */
    public static IndividualCustomer register(String customerNumber, String email, String phone, String firstName,
                                              @Nullable String middleName, String lastName, String egn,
                                              Address residentialAddress) {
        return new IndividualCustomer(customerNumber, email, phone, firstName, middleName, lastName, egn,
                residentialAddress);
    }

    private static @Nullable String normalizeOptionalText(@Nullable String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }

    @Override
    public CustomerType getCustomerType() {
        return CustomerType.INDIVIDUAL;
    }

    public void updateName(String firstName, @Nullable String middleName, String lastName) {
        this.firstName = requireText(firstName, "firstName");

        this.middleName = normalizeOptionalText(middleName);

        this.lastName = requireText(lastName, "lastName");
    }

    /**
     * Replaces the complete residential address.
     *
     * @param residentialAddress new residential address
     */
    public void changeResidentialAddress(Address residentialAddress) {
        this.residentialAddress = Objects.requireNonNull(residentialAddress, "residentialAddress must not be null");
    }
}