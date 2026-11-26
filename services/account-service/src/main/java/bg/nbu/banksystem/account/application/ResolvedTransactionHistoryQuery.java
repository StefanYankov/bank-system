package bg.nbu.banksystem.account.application;

import org.springframework.data.domain.Pageable;

import java.time.Instant;

/**
 * Validated transaction-history boundaries and repository pagination.
 *
 * @param from     inclusive boundary
 * @param to       exclusive boundary
 * @param pageable validated slice coordinates
 */
record ResolvedTransactionHistoryQuery(
    Instant from,
    Instant to,
    Pageable pageable
) {
}
