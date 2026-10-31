package bg.nbu.banksystem.customer.application.dto;

import bg.nbu.banksystem.customer.application.validation.ValidEgn;
import bg.nbu.banksystem.customer.domain.CustomerConstraints;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.Nullable;

/**
 * Data required to register an individual Customer.
 *
 * @param email              Customer email
 * @param phone              Customer phone
 * @param firstName          first name
 * @param middleName         optional middle name
 * @param lastName           last name
 * @param egn                Bulgarian personal identifier
 * @param residentialAddress residential postal address
 */
public record IndividualCustomerRegistrationDto(
        @Schema(description = "Customer email.", example = "serafim.gerasimoff@example.com")
        @NotBlank
        @Email
        @Size(max = CustomerConstraints.EMAIL_MAX_LENGTH)
        String email,

        @Schema(description = "Customer phone.", example = "+359888123456")
        @NotBlank
        @Size(max = CustomerConstraints.PHONE_MAX_LENGTH)
        String phone,

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
        String lastName,

        @Schema(description = "Bulgarian personal identifier (EGN).", example = "6101047500", minLength = 10,
                maxLength = 10, pattern = "\\d{10}")
        @NotBlank
        @ValidEgn
        String egn,

        @Schema(description = "Residential postal address.")
        @NotNull
        @Valid
        AddressDto residentialAddress
) {
}