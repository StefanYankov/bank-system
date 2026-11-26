package bg.nbu.banksystem.account.application;

import bg.nbu.banksystem.account.application.dto.AccountTransactionViewDto;
import bg.nbu.banksystem.account.application.dto.TransactionHistoryQuery;
import bg.nbu.banksystem.account.domain.Account;
import bg.nbu.banksystem.account.domain.AccountErrorCode;
import bg.nbu.banksystem.account.domain.AccountTransaction;
import bg.nbu.banksystem.account.domain.AccountType;
import bg.nbu.banksystem.account.domain.Currency;
import bg.nbu.banksystem.account.domain.Iban;
import bg.nbu.banksystem.account.persistence.AccountRepository;
import bg.nbu.banksystem.account.persistence.AccountTransactionQueryRepository;
import bg.nbu.banksystem.starter.error.DomainException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static bg.nbu.banksystem.account.domain.AccountErrorCode.ACCOUNT_NOT_FOUND;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AccountServiceImpl transaction history (unit)")
class AccountServiceImplTransactionHistoryTest {

    private static final UUID ACCOUNT_ID = UUID.fromString("01923b7c-8f6d-7a12-9d1e-4b2f8c3e5a10");
    private static final UUID OWNER_CUSTOMER_ID = UUID.fromString("01923b7c-8f6d-7a12-9d1e-4b2f8c3e5a11");
    private static final Instant HISTORY_FROM = Instant.parse("2026-05-01T00:00:00Z");
    private static final Instant HISTORY_TO = Instant.parse("2026-11-01T00:00:00Z");

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

    private static Account account() {
        return Account.open(Iban.generate(AccountType.CURRENT, 1), OWNER_CUSTOMER_ID,
            AccountType.CURRENT, Currency.EUR);
    }

    private static AccountTransactionViewDto transactionView(AccountTransaction transaction) {
        return new AccountTransactionViewDto(transaction.getId(), transaction.getType(), transaction.getAmount(),
            transaction.getCurrency(), transaction.getResultingBalance(), transaction.getReference(),
            transaction.getCreatedAt());
    }

    @Test
    @DisplayName("Happy Path: Should map a transaction-history slice and preserve navigation metadata")
    void shouldReturnTransactionHistory() {
        // Arrange
        TransactionHistoryQuery query = new TransactionHistoryQuery(HISTORY_FROM, HISTORY_TO, 0, 2);
        Pageable pageable = PageRequest.of(0, 2);
        ResolvedTransactionHistoryQuery resolved = new ResolvedTransactionHistoryQuery(
            HISTORY_FROM, HISTORY_TO, pageable);
        Account account = account();
        AccountTransaction first = account.credit(new BigDecimal("10.00"), Currency.EUR, "First deposit");
        AccountTransaction second = account.credit(new BigDecimal("20.00"), Currency.EUR, "Second deposit");
        AccountTransactionViewDto firstView = transactionView(first);
        AccountTransactionViewDto secondView = transactionView(second);
        Slice<AccountTransaction> history = new SliceImpl<>(List.of(second, first), pageable, true);
        when(transactionHistoryPolicy.resolve(query)).thenReturn(resolved);
        when(accountRepository.existsById(ACCOUNT_ID)).thenReturn(true);
        when(transactionQueryRepository.findHistory(ACCOUNT_ID, HISTORY_FROM, HISTORY_TO, pageable))
            .thenReturn(history);
        when(accountMapper.toTransactionView(second)).thenReturn(secondView);
        when(accountMapper.toTransactionView(first)).thenReturn(firstView);

        // Act
        Slice<AccountTransactionViewDto> result = accountService.getTransactionHistory(ACCOUNT_ID, query);

        // Assert
        assertThat(result.getContent()).containsExactly(secondView, firstView);
        assertThat(result.getNumber()).isZero();
        assertThat(result.getSize()).isEqualTo(2);
        assertThat(result.hasNext()).isTrue();
        verify(transactionHistoryPolicy).resolve(query);
        verify(accountRepository).existsById(ACCOUNT_ID);
        verify(transactionQueryRepository).findHistory(ACCOUNT_ID, HISTORY_FROM, HISTORY_TO, pageable);
        verify(accountMapper).toTransactionView(second);
        verify(accountMapper).toTransactionView(first);
        verifyNoMoreInteractions(accountRepository, transactionQueryRepository, accountMapper,
            transactionHistoryPolicy);
    }

    @Test
    @DisplayName("Error Case: Should not query history for a missing Account")
    void shouldRejectHistoryForMissingAccount() {
        // Arrange
        TransactionHistoryQuery query = new TransactionHistoryQuery(HISTORY_FROM, HISTORY_TO, 0, 20);
        Pageable pageable = PageRequest.of(0, 20);
        ResolvedTransactionHistoryQuery resolved = new ResolvedTransactionHistoryQuery(
            HISTORY_FROM, HISTORY_TO, pageable);
        when(transactionHistoryPolicy.resolve(query)).thenReturn(resolved);
        when(accountRepository.existsById(ACCOUNT_ID)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> accountService.getTransactionHistory(ACCOUNT_ID, query))
            .isInstanceOfSatisfying(DomainException.class,
                exception -> assertThat(exception.getErrorCode()).isEqualTo(ACCOUNT_NOT_FOUND));

        verify(transactionHistoryPolicy).resolve(query);
        verify(accountRepository).existsById(ACCOUNT_ID);
        verifyNoMoreInteractions(accountRepository, transactionQueryRepository, accountMapper,
            transactionHistoryPolicy);
    }

    @Test
    @DisplayName("Error Case: Should reject invalid history input before accessing persistence")
    void shouldRejectInvalidHistoryBeforeAccessingPersistence() {
        // Arrange
        TransactionHistoryQuery query = new TransactionHistoryQuery(HISTORY_FROM, HISTORY_TO, 0, 101);
        DomainException validationFailure = new DomainException(
            AccountErrorCode.INVALID_TRANSACTION_HISTORY_PAGE_SIZE);
        when(transactionHistoryPolicy.resolve(query)).thenThrow(validationFailure);

        // Act & Assert
        assertThatThrownBy(() -> accountService.getTransactionHistory(ACCOUNT_ID, query))
            .isSameAs(validationFailure);

        verify(transactionHistoryPolicy).resolve(query);
        verifyNoMoreInteractions(accountRepository, transactionQueryRepository, accountMapper,
            transactionHistoryPolicy);
    }
}
