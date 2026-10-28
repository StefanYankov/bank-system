package bg.nbu.banksystem.customer.application.dto;

import bg.nbu.banksystem.customer.domain.CustomerConstraints;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.Nullable;

/**
 * Editable personal-name information of an individual Customer.
 *
 * @param firstName  first name
 * @param middleName optional middle name
 * @param lastName   last name
 */
public record IndividualCustomerNameUpdateDto(
        @Schema(description = "First name.", example = "Serafim")
        @NotBlank
        @Size(max = CustomerConstraints.NAME_MAX_LENGTH)
        String firstName,

        @Schema(description = "Optional middle name.", example = "Ivanov")
        @Size(max = CustomerConstraints.NAME_MAX_LENGTH)
        @Nullable
        String middleName,

        @Schema(description = "Last name.", example = "Gerasimoff")
        @NotBlank
        @Size(max = CustomerConstraints.NAME_MAX_LENGTH)
        String lastName
) {
}