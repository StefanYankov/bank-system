package bg.nbu.banksystem.account.application;

import bg.nbu.banksystem.account.application.dto.TransactionHistoryQuery;
import bg.nbu.banksystem.account.domain.AccountErrorCode;
import bg.nbu.banksystem.starter.error.DomainException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static bg.nbu.banksystem.account.domain.AccountErrorCode.INVALID_TRANSACTION_HISTORY_PAGE;
import static bg.nbu.banksystem.account.domain.AccountErrorCode.INVALID_TRANSACTION_HISTORY_PAGE_SIZE;
import static bg.nbu.banksystem.account.domain.AccountErrorCode.INVALID_TRANSACTION_HISTORY_RANGE;
import static bg.nbu.banksystem.account.domain.AccountErrorCode.TRANSACTION_HISTORY_RANGE_TOO_LARGE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("TransactionHistoryPolicy (unit)")
class TransactionHistoryPolicyTest {

    private static final Instant NOW = Instant.parse("2026-11-07T09:00:00Z");
    private static final Instant FROM = Instant.parse("2026-05-01T00:00:00Z");
    private static final Instant TO = Instant.parse("2026-11-01T00:00:00Z");

    private final TransactionHistoryPolicy policy = new TransactionHistoryPolicy(Clock.fixed(NOW, ZoneOffset.UTC));

    private static void assertDomainError(DomainException exception, AccountErrorCode expectedCode) {
        assertThat(exception.getErrorCode()).isEqualTo(expectedCode);
    }

    @Nested
    @DisplayName("Defaults")
    class Defaults {

        @Test
        @DisplayName("Happy Path: Should default to the previous six calendar months")
        void shouldApplyDefaultRange() {
            // Arrange
            TransactionHistoryQuery query = new TransactionHistoryQuery(null, null, 0, 20);

            // Act
            ResolvedTransactionHistoryQuery resolved = policy.resolve(query);

            // Assert
            assertThat(resolved.to()).isEqualTo(NOW);
            assertThat(resolved.from()).isEqualTo(Instant.parse("2026-05-07T08:00:00Z"));
            assertThat(resolved.pageable().getPageNumber()).isZero();
            assertThat(resolved.pageable().getPageSize()).isEqualTo(20);
            assertThat(resolved.pageable().getSort().isUnsorted()).isTrue();
        }

        @Test
        @DisplayName("Happy Path: Should derive the default lower boundary from an explicit upper boundary")
        void shouldDeriveDefaultFromExplicitTo() {
            // Arrange
            TransactionHistoryQuery query = new TransactionHistoryQuery(null, TO, 1, 50);

            // Act
            ResolvedTransactionHistoryQuery resolved = policy.resolve(query);

            // Assert
            assertThat(resolved.to()).isEqualTo(TO);
            assertThat(resolved.from()).isEqualTo(Instant.parse("2026-04-30T23:00:00Z"));
            assertThat(resolved.pageable().getPageNumber()).isEqualTo(1);
            assertThat(resolved.pageable().getPageSize()).isEqualTo(50);
        }
    }

    @Nested
    @DisplayName("Explicit range")
    class ExplicitRange {

        @Test
        @DisplayName("Happy Path: Should retain a valid explicit range")
        void shouldRetainExplicitRange() {
            // Arrange
            TransactionHistoryQuery query = new TransactionHistoryQuery(FROM, TO, 0, 100);

            // Act
            ResolvedTransactionHistoryQuery resolved = policy.resolve(query);

            // Assert
            assertThat(resolved.from()).isEqualTo(FROM);
            assertThat(resolved.to()).isEqualTo(TO);
            assertThat(resolved.pageable().getPageSize()).isEqualTo(100);
        }

        @Test
        @DisplayName("Edge Case: Should accept exactly two calendar years")
        void shouldAcceptExactlyTwoYears() {
            // Arrange
            Instant from = Instant.parse("2024-01-01T00:00:00Z");
            Instant to = Instant.parse("2026-01-01T00:00:00Z");
            TransactionHistoryQuery query = new TransactionHistoryQuery(from, to, 0, 20);

            // Act
            ResolvedTransactionHistoryQuery resolved = policy.resolve(query);

            // Assert
            assertThat(resolved.from()).isEqualTo(from);
            assertThat(resolved.to()).isEqualTo(to);
        }

        @Test
        @DisplayName("Error Case: Should reject equal boundaries")
        void shouldRejectEqualBoundaries() {
            // Arrange
            TransactionHistoryQuery query = new TransactionHistoryQuery(TO, TO, 0, 20);

            // Act & Assert
            assertThatThrownBy(() -> policy.resolve(query)).isInstanceOfSatisfying(DomainException.class,
                exception -> assertDomainError(exception, INVALID_TRANSACTION_HISTORY_RANGE));
        }

        @Test
        @DisplayName("Error Case: Should reject a reversed range")
        void shouldRejectReversedRange() {
            // Arrange
            TransactionHistoryQuery query = new TransactionHistoryQuery(TO, FROM, 0, 20);

            // Act & Assert
            assertThatThrownBy(() -> policy.resolve(query)).isInstanceOfSatisfying(DomainException.class,
                exception -> assertDomainError(exception, INVALID_TRANSACTION_HISTORY_RANGE));
        }

        @Test
        @DisplayName("Error Case: Should reject a range longer than two calendar years")
        void shouldRejectRangeLongerThanTwoYears() {
            // Arrange
            Instant from = Instant.parse("2024-01-01T00:00:00Z");
            Instant to = Instant.parse("2026-01-01T00:00:01Z");
            TransactionHistoryQuery query = new TransactionHistoryQuery(from, to, 0, 20);

            // Act & Assert
            assertThatThrownBy(() -> policy.resolve(query)).isInstanceOfSatisfying(DomainException.class,
                exception -> assertDomainError(exception, TRANSACTION_HISTORY_RANGE_TOO_LARGE));
        }
    }

    @Nested
    @DisplayName("Pagination")
    class Pagination {

        @Test
        @DisplayName("Error Case: Should reject a negative page number")
        void shouldRejectNegativePage() {
            // Arrange
            TransactionHistoryQuery query = new TransactionHistoryQuery(FROM, TO, -1, 20);

            // Act & Assert
            assertThatThrownBy(() -> policy.resolve(query)).isInstanceOfSatisfying(DomainException.class,
                exception -> assertDomainError(exception, INVALID_TRANSACTION_HISTORY_PAGE));
        }

        @ParameterizedTest(name = "Error Case: Should reject page size {0}")
        @ValueSource(ints = {0, 101})
        @DisplayName("Error Case: Should reject a page size outside the supported range")
        void shouldRejectInvalidPageSize(int size) {
            // Arrange
            TransactionHistoryQuery query = new TransactionHistoryQuery(FROM, TO, 0, size);

            // Act & Assert
            assertThatThrownBy(() -> policy.resolve(query)).isInstanceOfSatisfying(DomainException.class,
                exception -> assertDomainError(exception, INVALID_TRANSACTION_HISTORY_PAGE_SIZE));
        }
    }
}
