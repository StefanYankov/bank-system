package bg.nbu.banksystem.customer.application.dto;

import bg.nbu.banksystem.customer.domain.CustomerStatus;
import bg.nbu.banksystem.customer.domain.CustomerType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

/**
 * Minimal Customer representation used in lists and search results.
 *
 * @param id             technical Customer identifier
 * @param customerNumber business Customer number
 * @param customerType   Customer type, used to open the matching detail view
 * @param displayName    full name for individuals, company name for legal entities
 * @param identifier     EGN for individuals, EIK for legal entities
 * @param status         Customer status
 */
public record CustomerSummaryDto(
        @Schema(description = "Technical Customer identifier.", example = "01923b7c-8f6d-7a12-9d1e-4b2f8c3e5a10",
                requiredMode = Schema.RequiredMode.REQUIRED)
        UUID id,

        @Schema(description = "Business Customer number.", example = "CUS-000001",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String customerNumber,

        @Schema(description = "Customer type.", example = "INDIVIDUAL", requiredMode = Schema.RequiredMode.REQUIRED)
        CustomerType customerType,

        @Schema(description = "Full name for individuals, company name for legal entities.",
                example = "Serafim Ivanov Gerasimoff", requiredMode = Schema.RequiredMode.REQUIRED)
        String displayName,

        @Schema(description = "EGN for individuals, EIK for legal entities.", example = "6101047500",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String identifier,

        @Schema(description = "Customer status.", example = "ACTIVE", requiredMode = Schema.RequiredMode.REQUIRED)
        CustomerStatus status
) {
}