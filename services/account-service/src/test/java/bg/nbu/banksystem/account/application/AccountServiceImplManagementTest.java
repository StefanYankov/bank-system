package bg.nbu.banksystem.account.application;

import bg.nbu.banksystem.account.application.dto.AccountDepositDto;
import bg.nbu.banksystem.account.application.dto.AccountViewDto;
import bg.nbu.banksystem.account.domain.Account;
import bg.nbu.banksystem.account.domain.AccountStatus;
import bg.nbu.banksystem.account.domain.AccountType;
import bg.nbu.banksystem.account.domain.Currency;
import bg.nbu.banksystem.account.domain.Iban;
import bg.nbu.banksystem.account.domain.TransactionType;
import bg.nbu.banksystem.account.persistence.AccountRepository;
import bg.nbu.banksystem.account.persistence.AccountTransactionQueryRepository;
import bg.nbu.banksystem.starter.error.DomainException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static bg.nbu.banksystem.account.domain.AccountErrorCode.ACCOUNT_NOT_FOUND;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AccountServiceImpl management (unit)")
class AccountServiceImplManagementTest {

    private static final UUID ACCOUNT_ID = UUID.fromString("01923b7c-8f6d-7a12-9d1e-4b2f8c3e5a10");
    private static final UUID OWNER_CUSTOMER_ID = UUID.fromString("01923b7c-8f6d-7a12-9d1e-4b2f8c3e5a11");

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AccountTransactionQueryRepository transactionQueryRepository;

    @Mock
    private AccountMapper accountMapper;

    @Mock
    private TransactionHistoryPolicy transactionHistoryPolicy;

    @InjectMocks
    private AccountServiceImpl accountService;

    private static Account account(long sequence) {
        return Account.open(Iban.generate(AccountType.CURRENT, sequence), OWNER_CUSTOMER_ID,
            AccountType.CURRENT, Currency.EUR);
    }

    private static AccountViewDto accountView(Account account) {
        return new AccountViewDto(account.getId(), account.getIban(), account.getOwnerCustomerId(), account.getType(),
            account.getCurrency(), account.getBalance(), account.getStatus());
    }

    private static void assertAccountNotFound(DomainException exception) {
        assertThat(exception.getErrorCode()).isEqualTo(ACCOUNT_NOT_FOUND);
    }

    @Nested
    @DisplayName("Deposits")
    class Deposits {

        @Test
        @DisplayName("Happy Path: Should credit the Account and return its updated view")
        void shouldDepositFunds() {
            // Arrange
            Account account = account(1);
            AccountDepositDto deposit = new AccountDepositDto(new BigDecimal("125.50"), Currency.EUR, "Cash deposit");
            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));
            when(accountMapper.toView(account)).thenAnswer(invocation -> accountView(account));

            // Act
            AccountViewDto result = accountService.deposit(ACCOUNT_ID, deposit);

            // Assert
            assertThat(account.getBalance()).isEqualByComparingTo("125.50");
            assertThat(account.getTransactions()).singleElement().satisfies(transaction -> {
                assertThat(transaction.getType()).isEqualTo(TransactionType.CREDIT);
                assertThat(transaction.getReference()).isEqualTo("Cash deposit");
            });
            assertThat(result.balance()).isEqualByComparingTo("125.50");
            verify(accountRepository).findById(ACCOUNT_ID);
            verify(accountMapper).toView(account);
            verify(accountRepository, never()).save(account);
            verifyNoMoreInteractions(accountRepository, transactionQueryRepository, accountMapper,
                transactionHistoryPolicy);
        }

        @Test
        @DisplayName("Error Case: Should reject a deposit for a missing Account")
        void shouldRejectDepositForMissingAccount() {
            // Arrange
            AccountDepositDto deposit = new AccountDepositDto(new BigDecimal("125.50"), Currency.EUR, "Cash deposit");
            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> accountService.deposit(ACCOUNT_ID, deposit))
                .isInstanceOfSatisfying(DomainException.class,
                    AccountServiceImplManagementTest::assertAccountNotFound);

            verify(accountRepository).findById(ACCOUNT_ID);
            verifyNoMoreInteractions(accountRepository, transactionQueryRepository, accountMapper,
                transactionHistoryPolicy);
        }
    }

    @Nested
    @DisplayName("Account closure")
    class AccountClosure {

        @Test
        @DisplayName("Happy Path: Should close a zero-balance Account and return its updated view")
        void shouldCloseAccount() {
            // Arrange
            Account account = account(2);
            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));
            when(accountMapper.toView(account)).thenAnswer(invocation -> accountView(account));

            // Act
            AccountViewDto result = accountService.close(ACCOUNT_ID);

            // Assert
            assertThat(account.getStatus()).isEqualTo(AccountStatus.CLOSED);
            assertThat(result.status()).isEqualTo(AccountStatus.CLOSED);
            verify(accountRepository).findById(ACCOUNT_ID);
            verify(accountMapper).toView(account);
            verify(accountRepository, never()).save(account);
            verifyNoMoreInteractions(accountRepository, transactionQueryRepository, accountMapper,
                transactionHistoryPolicy);
        }

        @Test
        @DisplayName("Error Case: Should reject closure for a missing Account")
        void shouldRejectClosureForMissingAccount() {
            // Arrange
            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> accountService.close(ACCOUNT_ID))
                .isInstanceOfSatisfying(DomainException.class,
                    AccountServiceImplManagementTest::assertAccountNotFound);

            verify(accountRepository).findById(ACCOUNT_ID);
            verifyNoMoreInteractions(accountRepository, transactionQueryRepository, accountMapper,
                transactionHistoryPolicy);
        }
    }
}
