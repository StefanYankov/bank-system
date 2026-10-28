package bg.nbu.banksystem.customer.application.dto;

import bg.nbu.banksystem.customer.domain.CustomerConstraints;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * New registered name of a legal-entity Customer.
 *
 * @param companyName registered company name
 */
public record CompanyNameUpdateDto(
        @Schema(description = "Registered company name.", example = "Starter Financial Services")
        @NotBlank
        @Size(max = CustomerConstraints.COMPANY_NAME_MAX_LENGTH)
        String companyName
) {
}