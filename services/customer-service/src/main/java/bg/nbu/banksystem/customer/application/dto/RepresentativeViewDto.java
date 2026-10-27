package bg.nbu.banksystem.customer.application.dto;

import bg.nbu.banksystem.customer.domain.RepresentativeRole;
import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Representative of a legal-entity Customer.
 *
 * <p>
 * Names are the snapshot recorded when the representation was created. A linked individual Customer is exposed
 * by identifier only, so the legal-entity view never reveals that person's private data.
 * </p>
 *
 * @param id                   technical Representative identifier
 * @param individualCustomerId identifier of the linked individual Customer, if any
 * @param firstName            recorded first name
 * @param middleName           recorded middle name, if any
 * @param lastName             recorded last name
 * @param role                 representation role
 * @param validFrom            first valid date, inclusive
 * @param validTo              final valid date, inclusive, if the representation has ended
 */
public record RepresentativeViewDto(
        @Schema(description = "Technical Representative identifier.", example = "01923b7c-8f6d-7a12-9d1e-4b2f8c3e5a11",
                requiredMode = Schema.RequiredMode.REQUIRED)
        UUID id,

        @Schema(description = "Identifier of the linked individual Customer, if any.",
                example = "01923b7c-8f6d-7a12-9d1e-4b2f8c3e5a10", requiredMode = Schema.RequiredMode.REQUIRED,
                nullable = true)
        @Nullable
        UUID individualCustomerId,

        @Schema(description = "Recorded first name.", example = "Serafim", requiredMode = Schema.RequiredMode.REQUIRED)
        String firstName,

        @Schema(description = "Recorded middle name, if any.", example = "Ivanov",
                requiredMode = Schema.RequiredMode.REQUIRED, nullable = true)
        @Nullable
        String middleName,

        @Schema(description = "Recorded last name.", example = "Gerasimoff",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String lastName,

        @Schema(description = "Representation role.", example = "LEGAL_REPRESENTATIVE",
                requiredMode = Schema.RequiredMode.REQUIRED)
        RepresentativeRole role,

        @Schema(description = "First valid date, inclusive.", example = "2026-01-01",
                requiredMode = Schema.RequiredMode.REQUIRED)
        LocalDate validFrom,

        @Schema(description = "Final valid date, inclusive, if the representation has ended.", example = "2026-12-31",
                requiredMode = Schema.RequiredMode.REQUIRED, nullable = true)
        @Nullable
        LocalDate validTo
) {
}