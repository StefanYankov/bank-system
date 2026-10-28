package bg.nbu.banksystem.customer.application.dto;

import bg.nbu.banksystem.customer.application.validation.ValidEik;
import bg.nbu.banksystem.customer.domain.CustomerConstraints;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Data required to register a legal-entity Customer.
 *
 * @param email                 Customer email
 * @param phone                 Customer phone
 * @param companyName           registered company name
 * @param eik                   9-digit Bulgarian legal-entity identifier
 * @param registeredAddress     registered company address
 * @param correspondenceAddress correspondence address
 */
public record LegalEntityCustomerRegistrationDto(
        @Schema(description = "Customer email.", example = "contact@starter.example")
        @NotBlank
        @Email
        @Size(max = CustomerConstraints.EMAIL_MAX_LENGTH)
        String email,

        @Schema(description = "Customer phone.", example = "+35929201234")
        @NotBlank
        @Size(max = CustomerConstraints.PHONE_MAX_LENGTH)
        String phone,

        @Schema(description = "Registered company name.", example = "Starter Financial Services")
        @NotBlank
        @Size(max = CustomerConstraints.COMPANY_NAME_MAX_LENGTH)
        String companyName,

        @Schema(description = "9-digit Bulgarian legal-entity identifier (EIK).", example = "123456786")
        @NotBlank
        @ValidEik
        String eik,

        @Schema(description = "Registered company address.")
        @NotNull
        @Valid
        AddressDto registeredAddress,

        @Schema(description = "Correspondence address.")
        @NotNull
        @Valid
        AddressDto correspondenceAddress
) {
}