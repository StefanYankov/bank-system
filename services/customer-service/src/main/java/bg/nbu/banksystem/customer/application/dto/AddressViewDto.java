package bg.nbu.banksystem.customer.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

/**
 * Postal address returned by Customer Service.
 *
 * @param countryCode  two-letter country code
 * @param city         city or locality
 * @param postalCode   postal routing code
 * @param addressLine1 primary address description
 * @param addressLine2 additional address description, if any
 */
public record AddressViewDto(
        @Schema(description = "Two-letter country code.", example = "BG", requiredMode = Schema.RequiredMode.REQUIRED)
        String countryCode,

        @Schema(description = "City or locality.", example = "Sofia", requiredMode = Schema.RequiredMode.REQUIRED)
        String city,

        @Schema(description = "Postal routing code.", example = "1000", requiredMode = Schema.RequiredMode.REQUIRED)
        String postalCode,

        @Schema(description = "Primary address description.", example = "1 Alexander Battenberg Square",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String addressLine1,

        @Schema(description = "Additional address description, if any.", example = "Floor 2",
                requiredMode = Schema.RequiredMode.REQUIRED, nullable = true)
        @Nullable
        String addressLine2
) {
}