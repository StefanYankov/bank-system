package bg.nbu.banksystem.customer.domain;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

@DisplayName("Address (unit)")
class AddressTest {

    private static final String COUNTRY_CODE = "BG";
    private static final String CITY = "Sofia";
    private static final String POSTAL_CODE = "1000";
    private static final String ADDRESS_LINE_1 = "1 Alexander Battenberg Square";

    static Stream<Arguments> invalidMandatoryData() {
        return Stream.of(Arguments.of("countryCode", null, CITY, POSTAL_CODE, ADDRESS_LINE_1), Arguments.of("countryCode", "   ", CITY, POSTAL_CODE, ADDRESS_LINE_1), Arguments.of("city", COUNTRY_CODE, null, POSTAL_CODE, ADDRESS_LINE_1), Arguments.of("city", COUNTRY_CODE, "   ", POSTAL_CODE, ADDRESS_LINE_1), Arguments.of("postalCode", COUNTRY_CODE, CITY, null, ADDRESS_LINE_1), Arguments.of("postalCode", COUNTRY_CODE, CITY, "", ADDRESS_LINE_1), Arguments.of("addressLine1", COUNTRY_CODE, CITY, POSTAL_CODE, null), Arguments.of("addressLine1", COUNTRY_CODE, CITY, POSTAL_CODE, "   "));
    }

    @Test
    @DisplayName("Happy Path: Should create and normalize an Address")
    void shouldCreateNormalizedAddress() {
        // Act
        Address address = Address.of("  bg  ", "  Sofia  ", "  1000  ", "  1 Alexander Battenberg Square  ", "  Floor 2  ");

        // Assert
        assertThat(address.getCountryCode()).isEqualTo(COUNTRY_CODE);
        assertThat(address.getCity()).isEqualTo(CITY);
        assertThat(address.getPostalCode()).isEqualTo(POSTAL_CODE);
        assertThat(address.getAddressLine1()).isEqualTo(ADDRESS_LINE_1);
        assertThat(address.getAddressLine2()).isEqualTo("Floor 2");
    }

    @Test
    @DisplayName("Edge Case: Should allow a missing second address line")
    void shouldAllowMissingSecondAddressLine() {
        // Act
        Address address = Address.of(COUNTRY_CODE, CITY, POSTAL_CODE, ADDRESS_LINE_1, null);

        // Assert
        assertThat(address.getAddressLine2()).isNull();
    }

    @ParameterizedTest(name = "Edge Case: Should normalize \"{0}\" as a missing second address line")
    @ValueSource(strings = {"", "   "})
    @DisplayName("Edge Case: Should normalize a blank second address line")
    void shouldNormalizeBlankSecondAddressLine(String addressLine2) {
        // Act
        Address address = Address.of(COUNTRY_CODE, CITY, POSTAL_CODE, ADDRESS_LINE_1, addressLine2);

        // Assert
        assertThat(address.getAddressLine2()).isNull();
    }

    @ParameterizedTest(name = "Error Case: Should reject invalid {0}")
    @MethodSource("invalidMandatoryData")
    @DisplayName("Error Case: Should reject missing mandatory address data")
    void shouldRejectInvalidMandatoryData(String field, @Nullable String countryCode, @Nullable String city, @Nullable String postalCode, @Nullable String addressLine1) {
        // Act & Assert
        assertThatIllegalArgumentException().isThrownBy(() -> Address.of(countryCode, city, postalCode, addressLine1, null)).withMessageContaining(field);
    }

    @ParameterizedTest(name = "Error Case: Should reject country code \"{0}\"")
    @ValueSource(strings = {"B", "BGR", "1G", "B1"})
    @DisplayName("Error Case: Should require a two-letter country code")
    void shouldRejectInvalidCountryCode(String countryCode) {
        // Act & Assert
        assertThatIllegalArgumentException().isThrownBy(() -> Address.of(countryCode, CITY, POSTAL_CODE, ADDRESS_LINE_1, null)).withMessageContaining("countryCode");
    }
}