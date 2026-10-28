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
 * Data required to add a Representative to a legal-entity Customer.
 *
 * <p>
 * The names are stored as a snapshot. When {@code individualCustomerId} is supplied, the Representative is also
 * linked to that individual Customer.
 * </p>
 *
 * @param individualCustomerId identifier of an existing individual Customer, if linked
 * @param firstName            first name, when no Customer is linked
 * @param middleName           optional middle name, when no Customer is linked
 * @param lastName             last name, when no Customer is linked
 * @param role                 representation role
 * @param validFrom            first valid date of the representation
 */
public record RepresentativeAdditionDto(
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

        @Schema(description = "Representation role.", example = "LEGAL_REPRESENTATIVE")
        @NotNull
        RepresentativeRole role,

        @Schema(description = "First valid date of the representation.", example = "2026-01-01")
        @NotNull
        LocalDate validFrom
) {
}