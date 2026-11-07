package bg.nbu.banksystem.account.application.dto;

import bg.nbu.banksystem.account.domain.Currency;
import bg.nbu.banksystem.account.domain.TransactionType;
import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Immutable view of a successful Account balance movement.
 *
 * @param id               technical transaction identifier
 * @param type             direction of the balance movement
 * @param amount           transaction amount
 * @param currency         transaction currency
 * @param resultingBalance Account balance after the transaction
 * @param reference        client-safe transaction reference
 * @param createdAt        persistence timestamp, absent before the entry is persisted
 */
public record AccountTransactionViewDto(
    @Schema(description = "Technical transaction identifier.", example = "01923b7c-8f6d-7a12-9d1e-4b2f8c3e5a10",
        requiredMode = Schema.RequiredMode.REQUIRED)
    UUID id,
    @Schema(description = "Direction of the balance movement.", example = "CREDIT",
        requiredMode = Schema.RequiredMode.REQUIRED)
    TransactionType type,
    @Schema(description = "Transaction amount.", example = "125.50", requiredMode = Schema.RequiredMode.REQUIRED)
    BigDecimal amount,
    @Schema(description = "Transaction currency.", example = "EUR", requiredMode = Schema.RequiredMode.REQUIRED)
    Currency currency,
    @Schema(description = "Account balance after the transaction.", example = "125.50",
        requiredMode = Schema.RequiredMode.REQUIRED)
    BigDecimal resultingBalance,
    @Schema(description = "Client-safe transaction reference.", example = "Cash deposit",
        requiredMode = Schema.RequiredMode.REQUIRED)
    String reference,
    @Schema(description = "Time at which the transaction was persisted.", example = "2026-11-05T18:30:00Z",
        nullable = true)
    @Nullable
    Instant createdAt
) {
}
