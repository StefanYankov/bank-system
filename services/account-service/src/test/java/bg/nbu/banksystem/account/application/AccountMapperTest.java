package bg.nbu.banksystem.account.application;

import bg.nbu.banksystem.account.application.dto.AccountSummaryDto;
import bg.nbu.banksystem.account.application.dto.AccountTransactionViewDto;
import bg.nbu.banksystem.account.application.dto.AccountViewDto;
import bg.nbu.banksystem.account.domain.Account;
import bg.nbu.banksystem.account.domain.AccountStatus;
import bg.nbu.banksystem.account.domain.AccountTransaction;
import bg.nbu.banksystem.account.domain.AccountType;
import bg.nbu.banksystem.account.domain.Currency;
import bg.nbu.banksystem.account.domain.Iban;
import bg.nbu.banksystem.account.domain.TransactionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AccountMapper (unit)")
class AccountMapperTest {

    private static final UUID OWNER_CUSTOMER_ID = UUID.fromString("01a18828-3a3e-7a5a-91e0-64044866d971");
    private static final Currency CURRENCY = Currency.EUR;

    private final AccountMapper mapper = Mappers.getMapper(AccountMapper.class);

    private static Account account() {
        return Account.open(Iban.generate(AccountType.CURRENT, 1), OWNER_CUSTOMER_ID, AccountType.CURRENT, CURRENCY);
    }

    @Nested
    @DisplayName("Account views")
    class AccountViews {

        @Test
        @DisplayName("Happy Path: Should map an Account to its detailed view")
        void shouldMapAccountToView() {
            // Arrange
            Account account = account();
            account.credit(new BigDecimal("125.50"), CURRENCY, "Cash deposit");

            // Act
            AccountViewDto view = mapper.toView(account);

            // Assert
            assertThat(view.id()).isEqualTo(account.getId());
            assertThat(view.iban()).isEqualTo(account.getIban());
            assertThat(view.ownerCustomerId()).isEqualTo(OWNER_CUSTOMER_ID);
            assertThat(view.type()).isEqualTo(AccountType.CURRENT);
            assertThat(view.currency()).isEqualTo(Currency.EUR);
            assertThat(view.balance()).isEqualByComparingTo("125.50");
            assertThat(view.status()).isEqualTo(AccountStatus.ACTIVE);
        }

        @Test
        @DisplayName("Happy Path: Should map an Account to its summary")
        void shouldMapAccountToSummary() {
            // Arrange
            Account account = account();

            // Act
            AccountSummaryDto summary = mapper.toSummary(account);

            // Assert
            assertThat(summary.id()).isEqualTo(account.getId());
            assertThat(summary.iban()).isEqualTo(account.getIban());
            assertThat(summary.type()).isEqualTo(AccountType.CURRENT);
            assertThat(summary.currency()).isEqualTo(Currency.EUR);
            assertThat(summary.balance()).isEqualByComparingTo("0.00");
            assertThat(summary.status()).isEqualTo(AccountStatus.ACTIVE);
        }
    }

    @Nested
    @DisplayName("Transaction views")
    class TransactionViews {

        @Test
        @DisplayName("Happy Path: Should map an Account transaction to its view")
        void shouldMapTransactionToView() {
            // Arrange
            Account account = account();
            AccountTransaction transaction = account.credit(new BigDecimal("125.50"), CURRENCY, "Cash deposit");

            // Act
            AccountTransactionViewDto view = mapper.toTransactionView(transaction);

            // Assert
            assertThat(view.id()).isEqualTo(transaction.getId());
            assertThat(view.type()).isEqualTo(TransactionType.CREDIT);
            assertThat(view.amount()).isEqualByComparingTo("125.50");
            assertThat(view.currency()).isEqualTo(Currency.EUR);
            assertThat(view.resultingBalance()).isEqualByComparingTo("125.50");
            assertThat(view.reference()).isEqualTo("Cash deposit");
            assertThat(view.createdAt()).isNull();
        }
    }
}
