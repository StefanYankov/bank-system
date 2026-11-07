package bg.nbu.banksystem.account.application.dto;

import bg.nbu.banksystem.account.domain.Currency;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/**
 * Data required to deposit funds into an Account.
 *
 * @param amount    positive deposit amount
 * @param currency  transaction currency
 * @param reference client-safe transaction reference
 */
public record AccountDepositDto(
    @Schema(description = "Positive deposit amount with at most two decimal places.", example = "125.50",
        requiredMode = Schema.RequiredMode.REQUIRED)
    BigDecimal amount,
    @Schema(description = "Transaction currency.", example = "EUR", requiredMode = Schema.RequiredMode.REQUIRED)
    Currency currency,
    @Schema(description = "Client-safe transaction reference.", example = "Cash deposit",
        requiredMode = Schema.RequiredMode.REQUIRED)
    String reference
) {
}
