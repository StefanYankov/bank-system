package bg.nbu.banksystem.customer.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;

import java.util.Locale;

/**
 * Postal address stored as part of a Customer.
 *
 */
@Getter
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Address {

    @Column(name = "country_code", nullable = false, length = CustomerConstraints.COUNTRY_CODE_LENGTH)
    private String countryCode;

    @Column(name = "city", nullable = false, length = CustomerConstraints.CITY_MAX_LENGTH)
    private String city;

    @Column(name = "postal_code", nullable = false, length = CustomerConstraints.POSTAL_CODE_MAX_LENGTH)
    private String postalCode;

    @Column(name = "address_line_1", nullable = false, length = CustomerConstraints.ADDRESS_LINE_MAX_LENGTH)
    private String addressLine1;

    @Column(name = "address_line_2", length = CustomerConstraints.ADDRESS_LINE_MAX_LENGTH)
    private @Nullable String addressLine2;

    private Address(@Nullable String countryCode, @Nullable String city, @Nullable String postalCode, @Nullable String addressLine1, @Nullable String addressLine2) {
        this.countryCode = normalizeCountryCode(countryCode);
        this.city = requireText(city, "city");
        this.postalCode = requireText(postalCode, "postalCode");
        this.addressLine1 = requireText(addressLine1, "addressLine1");
        this.addressLine2 = normalizeOptionalText(addressLine2);
    }

    /**
     * Creates a normalized postal address.
     *
     * @param countryCode  two-letter country code
     * @param city         city or locality
     * @param postalCode   postal routing code
     * @param addressLine1 primary address description
     * @param addressLine2 optional additional address description
     * @return normalized Address
     */
    public static Address of(@Nullable String countryCode, @Nullable String city, @Nullable String postalCode, @Nullable String addressLine1, @Nullable String addressLine2) {
        return new Address(countryCode, city, postalCode, addressLine1, addressLine2);
    }

    private static String normalizeCountryCode(@Nullable String countryCode) {
        String normalized = requireText(countryCode, "countryCode").toUpperCase(Locale.ROOT);

        if (!normalized.matches("[A-Z]{2}")) {
            throw new IllegalArgumentException("countryCode must contain exactly two letters");
        }

        return normalized;
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
}