package bg.nbu.banksystem.customer.application.dto;

import bg.nbu.banksystem.customer.domain.CustomerConstraints;
import bg.nbu.banksystem.customer.domain.RepresentativeRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.Nullable;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Corrected values for a Representative recorded with a data-entry mistake.
 *
 * @param individualCustomerId identifier of an existing individual Customer, if linked
 * @param firstName            first name, when no Customer is linked
 * @param middleName           optional middle name, when no Customer is linked
 * @param lastName             last name, when no Customer is linked
 * @param role                 corrected role
 * @param validFrom            corrected first valid date
 * @param validTo              corrected final valid date, or {@code null} if the representation has not ended
 */
public record RepresentativeCorrectionDto(
        @Schema(description = "Identifier of an existing individual Customer, if linked.",
                example = "01923b7c-8f6d-7a12-9d1e-4b2f8c3e5a10")
        @Nullable
        UUID individualCustomerId,

        @Schema(description = "First name. Required when no Customer is linked; must be empty otherwise.",
                example = "Serafim")
        @Size(max = CustomerConstraints.NAME_MAX_LENGTH)
        @Nullable
        String firstName,

        @Schema(description = "Optional middle name. Must be empty when a Customer is linked.", example = "Ivanov")
        @Size(max = CustomerConstraints.NAME_MAX_LENGTH)
        @Nullable
        String middleName,

        @Schema(description = "Last name. Required when no Customer is linked; must be empty otherwise.",
                example = "Gerasimoff")
        @Size(max = CustomerConstraints.NAME_MAX_LENGTH)
        @Nullable
        String lastName,

        @Schema(description = "Corrected representation role.", example = "LEGAL_REPRESENTATIVE")
        @NotNull
        RepresentativeRole role,

        @Schema(description = "Corrected first valid date.", example = "2026-01-01")
        @NotNull
        LocalDate validFrom,

        @Schema(description = "Corrected final valid date, inclusive. Null if the representation has not ended.",
                example = "2026-12-31")
        @Nullable
        LocalDate validTo
) {
}