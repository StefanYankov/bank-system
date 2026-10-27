package bg.nbu.banksystem.customer.application.dto;

import bg.nbu.banksystem.customer.domain.CustomerStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

/**
 * Detailed legal-entity Customer representation returned by Customer Service.
 *
 * @param id                    technical Customer identifier
 * @param customerNumber        business Customer number
 * @param status                Customer status
 * @param email                 Customer email
 * @param phone                 Customer phone
 * @param companyName           registered company name
 * @param eik                   9-digit Bulgarian legal-entity identifier
 * @param registeredAddress     registered company address
 * @param correspondenceAddress correspondence address
 * @param representatives       current and ended Representatives
 */
public record LegalEntityCustomerViewDto(
        @Schema(description = "Technical Customer identifier.", example = "01923b7c-8f6d-7a12-9d1e-4b2f8c3e5a12",
                requiredMode = Schema.RequiredMode.REQUIRED)
        UUID id,

        @Schema(description = "Business Customer number.", example = "CUS-000002",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String customerNumber,

        @Schema(description = "Customer status.", example = "ACTIVE", requiredMode = Schema.RequiredMode.REQUIRED)
        CustomerStatus status,

        @Schema(description = "Customer email.", example = "contact@starter.example",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String email,

        @Schema(description = "Customer phone.", example = "+35929201234", requiredMode = Schema.RequiredMode.REQUIRED)
        String phone,

        @Schema(description = "Registered company name.", example = "Starter Financial Services",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String companyName,

        @Schema(description = "9-digit Bulgarian legal-entity identifier (EIK).", example = "123456786",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String eik,

        @Schema(description = "Registered company address.", requiredMode = Schema.RequiredMode.REQUIRED)
        AddressViewDto registeredAddress,

        @Schema(description = "Correspondence address.", requiredMode = Schema.RequiredMode.REQUIRED)
        AddressViewDto correspondenceAddress,

        @Schema(description = "Current and ended Representatives.", requiredMode = Schema.RequiredMode.REQUIRED)
        List<RepresentativeViewDto> representatives
) {
}