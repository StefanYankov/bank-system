package bg.nbu.banksystem.customer.application.dto;

import bg.nbu.banksystem.customer.domain.CustomerConstraints;
import bg.nbu.banksystem.customer.domain.RepresentativeRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.Nullable;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Data required to add a Representative to a legal-entity Customer.
 *
 * <p>
 * The names are stored as a snapshot. When {@code individualCustomerId} is supplied, the Representative is also
 * linked to that individual Customer.
 * </p>
 *
 * @param individualCustomerId optional identifier of an existing individual Customer
 * @param firstName            representative first name
 * @param middleName           optional representative middle name
 * @param lastName             representative last name
 * @param role                 representation role
 * @param validFrom            first valid date of the representation
 */
public record RepresentativeAdditionDto(
        @Schema(description = "Optional identifier of an existing individual Customer.",
                example = "01923b7c-8f6d-7a12-9d1e-4b2f8c3e5a10")
        @Nullable
        UUID individualCustomerId,

        @Schema(description = "Representative first name.", example = "Serafim")
        @NotBlank
        @Size(max = CustomerConstraints.NAME_MAX_LENGTH)
        String firstName,

        @Schema(description = "Optional representative middle name.", example = "Ivanov")
        @Size(max = CustomerConstraints.NAME_MAX_LENGTH)
        @Nullable
        String middleName,

        @Schema(description = "Representative last name.", example = "Gerasimoff")
        @NotBlank
        @Size(max = CustomerConstraints.NAME_MAX_LENGTH)
        String lastName,

        @Schema(description = "Representation role.", example = "LEGAL_REPRESENTATIVE")
        @NotNull
        RepresentativeRole role,

        @Schema(description = "First valid date of the representation.", example = "2026-01-01")
        @NotNull
        LocalDate validFrom
) {
}