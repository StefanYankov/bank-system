package bg.nbu.banksystem.customer.application.dto;

import bg.nbu.banksystem.customer.application.validation.ValidEgn;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * Corrected EGN for a data-entry mistake made at registration.
 *
 * @param egn the correct EGN
 */
public record EgnCorrectionDto(
        @Schema(description = "Correct Bulgarian personal identifier (EGN).", example = "6101047500")
        @NotBlank
        @ValidEgn
        String egn
) {
}