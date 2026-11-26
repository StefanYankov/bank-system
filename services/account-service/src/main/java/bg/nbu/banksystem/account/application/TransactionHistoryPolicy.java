package bg.nbu.banksystem.account.application;

import bg.nbu.banksystem.account.application.dto.TransactionHistoryQuery;
import bg.nbu.banksystem.starter.error.DomainException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static bg.nbu.banksystem.account.domain.AccountErrorCode.INVALID_TRANSACTION_HISTORY_PAGE;
import static bg.nbu.banksystem.account.domain.AccountErrorCode.INVALID_TRANSACTION_HISTORY_PAGE_SIZE;
import static bg.nbu.banksystem.account.domain.AccountErrorCode.INVALID_TRANSACTION_HISTORY_RANGE;
import static bg.nbu.banksystem.account.domain.AccountErrorCode.TRANSACTION_HISTORY_RANGE_TOO_LARGE;

/**
 * Applies Account transaction-history defaults and validation rules.
 */
@Component
@RequiredArgsConstructor
class TransactionHistoryPolicy {

    static final int MAXIMUM_PAGE_SIZE = 100;

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Europe/Sofia");

    private final Clock clock;

    private static void validatePage(int page, int size) {
        if (page < 0) {
            throw new DomainException(INVALID_TRANSACTION_HISTORY_PAGE);
        }

        if (size < 1 || size > MAXIMUM_PAGE_SIZE) {
            throw new DomainException(INVALID_TRANSACTION_HISTORY_PAGE_SIZE);
        }
    }

    private static void validateRange(Instant from, Instant to) {
        if (!from.isBefore(to)) {
            throw new DomainException(INVALID_TRANSACTION_HISTORY_RANGE);
        }

        Instant maximumTo = from.atZone(BUSINESS_ZONE).plusYears(2).toInstant();
        if (to.isAfter(maximumTo)) {
            throw new DomainException(TRANSACTION_HISTORY_RANGE_TOO_LARGE);
        }
    }

    ResolvedTransactionHistoryQuery resolve(TransactionHistoryQuery query) {
        validatePage(query.page(), query.size());

        Instant to = query.to() != null ? query.to() : clock.instant();
        Instant from = query.from() != null ? query.from() : to.atZone(BUSINESS_ZONE).minusMonths(6).toInstant();

        validateRange(from, to);

        PageRequest pageable = PageRequest.of(query.page(), query.size());
        return new ResolvedTransactionHistoryQuery(from, to, pageable);
    }
}
