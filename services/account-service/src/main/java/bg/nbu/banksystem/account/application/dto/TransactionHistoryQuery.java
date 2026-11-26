package bg.nbu.banksystem.account.application.dto;

import org.jspecify.annotations.Nullable;

import java.time.Instant;

/**
 * Optional transaction-history boundaries and requested slice coordinates.
 *
 * @param from inclusive boundary; defaults to six calendar months before {@code to}
 * @param to   exclusive boundary; defaults to the current application-clock instant
 * @param page zero-based slice number
 * @param size requested slice size
 */
public record TransactionHistoryQuery(
    @Nullable
    Instant from,
    @Nullable
    Instant to,
    int page,
    int size
) {
}
