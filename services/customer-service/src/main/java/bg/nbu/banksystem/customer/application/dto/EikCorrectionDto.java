package bg.nbu.banksystem.customer.application.dto;

import bg.nbu.banksystem.customer.application.validation.ValidEik;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * Corrected EIK for a data-entry mistake made at registration.
 *
 * @param eik the correct 9-digit EIK
 */
public record EikCorrectionDto(
        @Schema(description = "Correct 9-digit Bulgarian legal-entity identifier (EIK).", example = "123456786")
        @NotBlank
        @ValidEik
        String eik
) {
}