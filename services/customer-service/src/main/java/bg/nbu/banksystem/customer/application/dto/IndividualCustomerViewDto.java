package bg.nbu.banksystem.customer.application.dto;

import bg.nbu.banksystem.customer.domain.CustomerStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

/**
 * Detailed individual Customer representation returned by Customer Service.
 *
 * @param id                 technical Customer identifier
 * @param customerNumber     business Customer number
 * @param status             Customer status
 * @param email              Customer email
 * @param phone              Customer phone
 * @param firstName          first name
 * @param middleName         middle name, if any
 * @param lastName           last name
 * @param egn                Bulgarian personal identifier
 * @param residentialAddress residential postal address
 */
public record IndividualCustomerViewDto(
        @Schema(description = "Technical Customer identifier.", example = "01923b7c-8f6d-7a12-9d1e-4b2f8c3e5a10",
                requiredMode = Schema.RequiredMode.REQUIRED)
        UUID id,

        @Schema(description = "Business Customer number.", example = "CUS-000001",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String customerNumber,

        @Schema(description = "Customer status.", example = "ACTIVE", requiredMode = Schema.RequiredMode.REQUIRED)
        CustomerStatus status,

        @Schema(description = "Customer email.", example = "serafim.gerasimoff@example.com",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String email,

        @Schema(description = "Customer phone.", example = "+359888123456", requiredMode = Schema.RequiredMode.REQUIRED)
        String phone,

        @Schema(description = "First name.", example = "Serafim", requiredMode = Schema.RequiredMode.REQUIRED)
        String firstName,

        @Schema(description = "Middle name, if any.", example = "Ivanov", requiredMode = Schema.RequiredMode.REQUIRED,
                nullable = true)
        @Nullable
        String middleName,

        @Schema(description = "Last name.", example = "Gerasimoff", requiredMode = Schema.RequiredMode.REQUIRED)
        String lastName,

        @Schema(description = "Bulgarian personal identifier (EGN).", example = "6101047500",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String egn,

        @Schema(description = "Residential postal address.", requiredMode = Schema.RequiredMode.REQUIRED)
        AddressViewDto residentialAddress
) {
}