package bg.nbu.banksystem.account.application.dto;

import bg.nbu.banksystem.account.domain.AccountStatus;
import bg.nbu.banksystem.account.domain.AccountType;
import bg.nbu.banksystem.account.domain.Currency;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Detailed Account representation returned by Account Service.
 *
 * @param id              technical Account identifier
 * @param iban            externally visible IBAN
 * @param ownerCustomerId Customer Service identifier of the owner
 * @param type            Account product type
 * @param currency        Account currency
 * @param balance         current Account balance
 * @param status          Account lifecycle status
 */
public record AccountViewDto(
    @Schema(description = "Technical Account identifier.", example = "01923b7c-8f6d-7a12-9d1e-4b2f8c3e5a10",
        requiredMode = Schema.RequiredMode.REQUIRED)
    UUID id,
    @Schema(description = "Bulgarian IBAN.", example = "BG00NBUB00010100000001",
        requiredMode = Schema.RequiredMode.REQUIRED)
    String iban,
    @Schema(description = "Customer Service identifier of the Account owner.",
        example = "01923b7c-8f6d-7a12-9d1e-4b2f8c3e5a10", requiredMode = Schema.RequiredMode.REQUIRED)
    UUID ownerCustomerId,
    @Schema(description = "Account product type.", example = "CURRENT", requiredMode = Schema.RequiredMode.REQUIRED)
    AccountType type,
    @Schema(description = "Account currency.", example = "EUR", requiredMode = Schema.RequiredMode.REQUIRED)
    Currency currency,
    @Schema(description = "Current Account balance.", example = "125.50", requiredMode = Schema.RequiredMode.REQUIRED)
    BigDecimal balance,
    @Schema(description = "Account lifecycle status.", example = "ACTIVE", requiredMode = Schema.RequiredMode.REQUIRED)
    AccountStatus status
) {
}
