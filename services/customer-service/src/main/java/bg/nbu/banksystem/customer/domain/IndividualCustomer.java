package bg.nbu.banksystem.customer.domain;

import bg.nbu.banksystem.common.Identifiers;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;

/**
 * Customer representing a physical person identified by EGN.
 */
@Getter
@Entity
@Table(name = "individual_customers")
@DiscriminatorValue("INDIVIDUAL")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IndividualCustomer extends Customer {

    @Column(
            name = "first_name",
            nullable = false,
            length = CustomerConstraints.NAME_MAX_LENGTH
    )
    private String firstName;

    @Column(
            name = "middle_name",
            length = CustomerConstraints.NAME_MAX_LENGTH
    )
    private @Nullable String middleName;

    @Column(
            name = "last_name",
            nullable = false,
            length = CustomerConstraints.NAME_MAX_LENGTH
    )
    private String lastName;

    @Column(
            name = "egn",
            nullable = false,
            updatable = false,
            unique = true,
            length = CustomerConstraints.EGN_LENGTH
    )
    private String egn;

    private IndividualCustomer(
            String customerNumber,
            String email,
            String phone,
            String firstName,
            @Nullable String middleName,
            String lastName,
            String egn
    ) {
        super(
                Identifiers.newUuid(),
                customerNumber,
                email,
                phone
        );

        this.firstName = requireText(
                firstName,
                "firstName"
        );

        this.middleName = normalizeOptionalText(
                middleName
        );

        this.lastName = requireText(
                lastName,
                "lastName"
        );

        this.egn = requireText(
                egn,
                "egn"
        );
    }

    /**
     * Registers a new individual Customer.
     *
     * @param customerNumber immutable operational Customer number
     * @param email          communication email
     * @param phone          communication phone
     * @param firstName      first name
     * @param middleName     optional middle name
     * @param lastName       last name
     * @param egn            immutable Bulgarian personal identifier
     * @return registered individual Customer
     */
    public static IndividualCustomer register(
            String customerNumber,
            String email,
            String phone,
            String firstName,
            @Nullable String middleName,
            String lastName,
            String egn
    ) {
        return new IndividualCustomer(
                customerNumber,
                email,
                phone,
                firstName,
                middleName,
                lastName,
                egn
        );
    }

    private static @Nullable String normalizeOptionalText(
            @Nullable String value
    ) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }

    /**
     * Returns the concrete Customer type.
     *
     * @return individual Customer type
     */
    @Override
    public CustomerType getCustomerType() {
        return CustomerType.INDIVIDUAL;
    }

    /**
     * Updates editable personal-name information.
     *
     * @param firstName  first name
     * @param middleName optional middle name
     * @param lastName   last name
     */
    public void updateName(
            String firstName,
            @Nullable String middleName,
            String lastName
    ) {
        this.firstName = requireText(
                firstName,
                "firstName"
        );

        this.middleName = normalizeOptionalText(
                middleName
        );

        this.lastName = requireText(
                lastName,
                "lastName"
        );
    }
}