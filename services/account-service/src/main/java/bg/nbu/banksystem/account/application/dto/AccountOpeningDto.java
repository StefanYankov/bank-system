package bg.nbu.banksystem.account.application.dto;

import bg.nbu.banksystem.account.domain.AccountType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

/**
 * Data required to open an Account for an existing Customer.
 *
 * @param ownerCustomerId Customer Service identifier of the owner
 * @param type            requested Account product type
 */
public record AccountOpeningDto(
    @Schema(description = "Customer Service identifier of the Account owner.",
        example = "01923b7c-8f6d-7a12-9d1e-4b2f8c3e5a10", requiredMode = Schema.RequiredMode.REQUIRED)
    UUID ownerCustomerId,
    @Schema(description = "Account product type.", example = "CURRENT", requiredMode = Schema.RequiredMode.REQUIRED)
    AccountType type
) {
}
