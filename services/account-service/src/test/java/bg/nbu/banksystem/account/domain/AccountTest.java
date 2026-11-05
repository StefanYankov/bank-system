package bg.nbu.banksystem.account.domain;

import bg.nbu.banksystem.starter.error.DomainException;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static bg.nbu.banksystem.account.domain.AccountConstraints.MONEY_SCALE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Account (unit)")
class AccountTest {

    private static final UUID OWNER_CUSTOMER_ID = UUID.fromString("01a18828-3a3e-7a5a-91e0-64044866d971");
    private static final Currency CURRENCY = Currency.EUR;

    private static Account account() {
        return Account.open(Iban.generate(AccountType.CURRENT, 1), OWNER_CUSTOMER_ID, AccountType.CURRENT, CURRENCY);
    }

    private static void assertDomainError(ThrowingCallable action, AccountErrorCode expectedCode) {
        assertThatThrownBy(action).isInstanceOfSatisfying(DomainException.class,
            exception -> assertThat(exception.getErrorCode()).isEqualTo(expectedCode));
    }

    @Nested
    @DisplayName("Opening")
    class Opening {

        @Test
        @DisplayName("Happy Path: Should open an active zero-balance account with UUIDv7")
        void shouldOpenAccount() {
            // Arrange
            Iban iban = Iban.generate(AccountType.CURRENT, 1);

            // Act
            Account account = Account.open(iban, OWNER_CUSTOMER_ID, AccountType.CURRENT, CURRENCY);

            // Assert
            assertThat(account.getId().version()).isEqualTo(7);
            assertThat(account.getIban()).isEqualTo(iban.value());
            assertThat(account.getOwnerCustomerId()).isEqualTo(OWNER_CUSTOMER_ID);
            assertThat(account.getType()).isEqualTo(AccountType.CURRENT);
            assertThat(account.getCurrency()).isEqualTo(Currency.EUR);
            assertThat(account.getBalance()).isEqualByComparingTo("0.00");
            assertThat(account.getStatus()).isEqualTo(AccountStatus.ACTIVE);
            assertThat(account.getTransactions()).isEmpty();
        }

        @Test
        @DisplayName("Happy Path: Should assign a different UUID to every account")
        void shouldAssignDifferentIdentifiers() {
            // Act
            Account firstAccount = account();
            Account secondAccount = Account.open(Iban.generate(AccountType.CURRENT, 2), OWNER_CUSTOMER_ID,
                AccountType.CURRENT, CURRENCY);

            // Assert
            assertThat(firstAccount.getId()).isNotEqualTo(secondAccount.getId());
        }

        @Test
        @DisplayName("Error Case: Should reject a missing owner")
        void shouldRejectMissingOwner() {
            // Act & Assert
            assertThatNullPointerException().isThrownBy(
                    () -> Account.open(Iban.generate(AccountType.CURRENT, 1), null, AccountType.CURRENT, CURRENCY))
                .withMessage("ownerCustomerId must not be null");
        }
    }

    @Nested
    @DisplayName("Credits")
    class Credits {

        @Test
        @DisplayName("Happy Path: Should credit the account and record the resulting balance")
        void shouldCreditAccount() {
            // Arrange
            Account account = account();

            // Act
            AccountTransaction transaction = account.credit(new BigDecimal("125.50"), CURRENCY, "Cash deposit");

            // Assert
            assertThat(account.getBalance()).isEqualByComparingTo("125.50");
            assertThat(account.getTransactions()).containsExactly(transaction);
            assertThat(transaction.getType()).isEqualTo(TransactionType.CREDIT);
            assertThat(transaction.getAmount()).isEqualByComparingTo("125.50");
            assertThat(transaction.getCurrency()).isEqualTo(Currency.EUR);
            assertThat(transaction.getResultingBalance()).isEqualByComparingTo("125.50");
            assertThat(transaction.getReference()).isEqualTo("Cash deposit");
            assertThat(transaction.getId().version()).isEqualTo(7);
        }

        @Test
        @DisplayName("Error Case: Should reject a non-positive credit")
        void shouldRejectNonPositiveCredit() {
            // Arrange
            Account account = account();

            // Act & Assert
            assertThatIllegalArgumentException().isThrownBy(
                    () -> account.credit(BigDecimal.ZERO, CURRENCY, "Invalid deposit"))
                .withMessage("amount must be positive");
            assertThat(account.getBalance()).isEqualByComparingTo("0.00");
            assertThat(account.getTransactions()).isEmpty();
        }

        @Test
        @DisplayName("Error Case: Should reject a different transaction currency")
        void shouldRejectCurrencyMismatch() {
            // Arrange
            Account account = account();

            // Act & Assert
            assertThatNullPointerException().isThrownBy(() -> account.credit(new BigDecimal("1.00"), null, "Deposit"))
                .withMessage("transactionCurrency must not be null");
        }
    }

    @Nested
    @DisplayName("Debits")
    class Debits {

        @Test
        @DisplayName("Happy Path: Should debit available funds and record the resulting balance")
        void shouldDebitAvailableFunds() {
            // Arrange
            Account account = account();
            account.credit(new BigDecimal("100.00"), CURRENCY, "Initial deposit");

            // Act
            AccountTransaction transaction = account.debit(new BigDecimal("40.25"), CURRENCY, "Card purchase");

            // Assert
            assertThat(account.getBalance()).isEqualByComparingTo("59.75");
            assertThat(account.getTransactions()).hasSize(2);
            assertThat(transaction.getType()).isEqualTo(TransactionType.DEBIT);
            assertThat(transaction.getResultingBalance()).isEqualByComparingTo("59.75");
        }

        @Test
        @DisplayName("Error Case: Should reject a debit exceeding the balance")
        void shouldRejectInsufficientFunds() {
            // Arrange
            Account account = account();
            account.credit(new BigDecimal("50.00"), CURRENCY, "Initial deposit");

            // Act & Assert
            assertDomainError(() -> account.debit(new BigDecimal("50.01"), CURRENCY, "Purchase"),
                AccountErrorCode.ACCOUNT_INSUFFICIENT_FUNDS);
            assertThat(account.getBalance()).isEqualByComparingTo("50.00");
            assertThat(account.getTransactions()).hasSize(1);
        }

        @Test
        @DisplayName("Error Case: Should reject more than two decimal places")
        void shouldRejectExcessiveScale() {
            // Arrange
            Account account = account();

            // Act & Assert
            assertThatIllegalArgumentException().isThrownBy(
                    () -> account.credit(new BigDecimal("10.001"), CURRENCY, "Invalid precision"))
                .withMessage("amount must have at most " + MONEY_SCALE + " decimal places");
        }
    }

    @Nested
    @DisplayName("Lifecycle")
    class Lifecycle {

        @Test
        @DisplayName("Happy Path: Should close an active zero-balance account")
        void shouldCloseZeroBalanceAccount() {
            // Arrange
            Account account = account();

            // Act
            account.close();

            // Assert
            assertThat(account.getStatus()).isEqualTo(AccountStatus.CLOSED);
        }

        @Test
        @DisplayName("Error Case: Should reject closing an account with a non-zero balance")
        void shouldRejectClosingNonZeroBalanceAccount() {
            // Arrange
            Account account = account();
            account.credit(new BigDecimal("1.00"), CURRENCY, "Initial deposit");

            // Act & Assert
            assertDomainError(account::close, AccountErrorCode.ACCOUNT_BALANCE_NOT_ZERO);
            assertThat(account.getStatus()).isEqualTo(AccountStatus.ACTIVE);
        }

        @Test
        @DisplayName("Error Case: Should reject closing an already closed account")
        void shouldRejectClosingTwice() {
            // Arrange
            Account account = account();
            account.close();

            // Act & Assert
            assertDomainError(account::close, AccountErrorCode.ACCOUNT_ALREADY_CLOSED);
        }

        @Test
        @DisplayName("Error Case: Should reject transactions after closure")
        void shouldRejectTransactionsAfterClosure() {
            // Arrange
            Account account = account();
            account.close();

            // Act & Assert
            assertDomainError(() -> account.credit(new BigDecimal("1.00"), CURRENCY, "Deposit"),
                AccountErrorCode.ACCOUNT_CLOSED);
            assertDomainError(() -> account.debit(new BigDecimal("1.00"), CURRENCY, "Purchase"),
                AccountErrorCode.ACCOUNT_CLOSED);
        }
    }
}
