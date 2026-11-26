package bg.nbu.banksystem.account.application;

import bg.nbu.banksystem.account.application.dto.AccountSummaryDto;
import bg.nbu.banksystem.account.application.dto.AccountViewDto;
import bg.nbu.banksystem.account.domain.Account;
import bg.nbu.banksystem.account.domain.AccountStatus;
import bg.nbu.banksystem.account.domain.AccountType;
import bg.nbu.banksystem.account.domain.Currency;
import bg.nbu.banksystem.account.domain.Iban;
import bg.nbu.banksystem.account.integration.customer.CustomerServiceClient;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static bg.nbu.banksystem.account.domain.AccountErrorCode.ACCOUNT_NOT_FOUND;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AccountServiceImpl queries (unit)")
class AccountServiceImplQueryTest {

    private static final UUID ACCOUNT_ID = UUID.fromString("01923b7c-8f6d-7a12-9d1e-4b2f8c3e5a10");
    private static final UUID OWNER_CUSTOMER_ID = UUID.fromString("01923b7c-8f6d-7a12-9d1e-4b2f8c3e5a11");

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AccountTransactionQueryRepository transactionQueryRepository;

    @Mock
    private AccountMapper accountMapper;

    @Mock
    private CustomerServiceClient customerServiceClient;

    @Mock
    private AccountCreator accountCreator;

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

    private static AccountSummaryDto accountSummary(Account account) {
        return new AccountSummaryDto(account.getId(), account.getIban(), account.getType(), account.getCurrency(),
            account.getBalance(), account.getStatus());
    }

    private static void assertAccountNotFound(DomainException exception) {
        assertThat(exception.getErrorCode()).isEqualTo(ACCOUNT_NOT_FOUND);
    }

    @Nested
    @DisplayName("Account retrieval")
    class AccountRetrieval {

        @Test
        @DisplayName("Happy Path: Should return the detailed Account view")
        void shouldReturnAccountView() {
            // Arrange
            Account account = account(1);
            AccountViewDto expectedView = accountView(account);
            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));
            when(accountMapper.toView(account)).thenReturn(expectedView);

            // Act
            AccountViewDto actualView = accountService.getById(ACCOUNT_ID);

            // Assert
            assertThat(actualView).isSameAs(expectedView);
            verify(accountRepository).findById(ACCOUNT_ID);
            verify(accountMapper).toView(account);
            verifyNoMoreInteractions(accountRepository, transactionQueryRepository, accountMapper,
                transactionHistoryPolicy);
        }

        @Test
        @DisplayName("Error Case: Should report a missing Account")
        void shouldReportMissingAccount() {
            // Arrange
            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> accountService.getById(ACCOUNT_ID))
                .isInstanceOfSatisfying(DomainException.class, AccountServiceImplQueryTest::assertAccountNotFound);

            verify(accountRepository).findById(ACCOUNT_ID);
            verifyNoMoreInteractions(accountRepository, transactionQueryRepository, accountMapper,
                transactionHistoryPolicy);
        }
    }

    @Nested
    @DisplayName("Owner Accounts")
    class OwnerAccounts {

        @Test
        @DisplayName("Happy Path: Should map a page of the Customer's Accounts")
        void shouldMapOwnerAccounts() {
            // Arrange
            Pageable pageable = PageRequest.of(1, 2);
            Account firstAccount = account(2);
            Account secondAccount = account(3);
            AccountSummaryDto firstSummary = accountSummary(firstAccount);
            AccountSummaryDto secondSummary = accountSummary(secondAccount);
            Page<Account> accounts = new PageImpl<>(List.of(firstAccount, secondAccount), pageable, 5);
            when(accountRepository.findByOwnerCustomerId(OWNER_CUSTOMER_ID, pageable)).thenReturn(accounts);
            when(accountMapper.toSummary(firstAccount)).thenReturn(firstSummary);
            when(accountMapper.toSummary(secondAccount)).thenReturn(secondSummary);

            // Act
            Page<AccountSummaryDto> result = accountService.findByOwner(OWNER_CUSTOMER_ID, pageable);

            // Assert
            assertThat(result.getContent()).containsExactly(firstSummary, secondSummary);
            assertThat(result.getNumber()).isEqualTo(1);
            assertThat(result.getSize()).isEqualTo(2);
            assertThat(result.getTotalElements()).isEqualTo(5);
            assertThat(result.getTotalPages()).isEqualTo(3);
            verify(accountRepository).findByOwnerCustomerId(OWNER_CUSTOMER_ID, pageable);
            verify(accountMapper).toSummary(firstAccount);
            verify(accountMapper).toSummary(secondAccount);
            verifyNoMoreInteractions(accountRepository, transactionQueryRepository, accountMapper,
                transactionHistoryPolicy);
        }

        @Test
        @DisplayName("Happy Path: Should report whether the Customer has an active Account")
        void shouldReportActiveAccountExistence() {
            // Arrange
            when(accountRepository.existsByOwnerCustomerIdAndStatus(OWNER_CUSTOMER_ID, AccountStatus.ACTIVE))
                .thenReturn(true);

            // Act
            boolean activeAccountExists = accountService.hasActiveAccounts(OWNER_CUSTOMER_ID);

            // Assert
            assertThat(activeAccountExists).isTrue();
            verify(accountRepository).existsByOwnerCustomerIdAndStatus(OWNER_CUSTOMER_ID, AccountStatus.ACTIVE);
            verifyNoMoreInteractions(accountRepository, transactionQueryRepository, accountMapper,
                transactionHistoryPolicy);
        }
    }
}
