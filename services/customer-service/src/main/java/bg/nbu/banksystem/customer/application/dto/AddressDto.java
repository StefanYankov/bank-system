package bg.nbu.banksystem.customer.application.dto;

import bg.nbu.banksystem.customer.domain.CustomerConstraints;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.Nullable;

/**
 * Postal address supplied when registering a Customer or changing one of its addresses.
 *
 * @param countryCode  two-letter country code
 * @param city         city or locality
 * @param postalCode   postal routing code
 * @param addressLine1 primary address description
 * @param addressLine2 optional additional address description
 */
public record AddressDto(
        @Schema(description = "Two-letter country code.", example = "BG")
        @NotBlank
        @Pattern(regexp = "[A-Za-z]{2}")
        String countryCode,

        @Schema(description = "City or locality.", example = "Sofia")
        @NotBlank
        @Size(max = CustomerConstraints.CITY_MAX_LENGTH)
        String city,

        @Schema(description = "Postal routing code.", example = "1000")
        @NotBlank
        @Size(max = CustomerConstraints.POSTAL_CODE_MAX_LENGTH)
        String postalCode,

        @Schema(description = "Primary address description.", example = "1 Alexander Battenberg Square")
        @NotBlank
        @Size(max = CustomerConstraints.ADDRESS_LINE_MAX_LENGTH)
        String addressLine1,

        @Schema(description = "Optional additional address description.", example = "Floor 2")
        @Size(max = CustomerConstraints.ADDRESS_LINE_MAX_LENGTH)
        @Nullable
        String addressLine2
) {
}