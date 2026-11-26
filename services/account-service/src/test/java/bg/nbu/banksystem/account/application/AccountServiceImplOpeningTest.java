package bg.nbu.banksystem.account.application;

import bg.nbu.banksystem.account.application.dto.AccountOpeningDto;
import bg.nbu.banksystem.account.application.dto.AccountViewDto;
import bg.nbu.banksystem.account.domain.AccountErrorCode;
import bg.nbu.banksystem.account.domain.AccountStatus;
import bg.nbu.banksystem.account.domain.AccountType;
import bg.nbu.banksystem.account.domain.Currency;
import bg.nbu.banksystem.account.integration.customer.CustomerReference;
import bg.nbu.banksystem.account.integration.customer.CustomerServiceClient;
import bg.nbu.banksystem.account.integration.customer.CustomerStatus;
import bg.nbu.banksystem.account.persistence.AccountRepository;
import bg.nbu.banksystem.account.persistence.AccountTransactionQueryRepository;
import bg.nbu.banksystem.starter.error.DomainException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static bg.nbu.banksystem.account.domain.AccountErrorCode.ACCOUNT_OWNER_NOT_ACTIVE;
import static bg.nbu.banksystem.account.domain.AccountErrorCode.ACCOUNT_OWNER_NOT_FOUND;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AccountServiceImpl opening (unit)")
class AccountServiceImplOpeningTest {

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

    @Mock
    private CustomerServiceClient customerServiceClient;

    @Mock
    private AccountCreator accountCreator;

    @InjectMocks
    private AccountServiceImpl accountService;

    @Test
    @DisplayName("Happy Path: Should validate an active Customer before creating the Account")
    void shouldOpenAccountForActiveCustomer() {
        // Arrange
        AccountOpeningDto openingDto = new AccountOpeningDto(OWNER_CUSTOMER_ID, AccountType.CURRENT);
        CustomerReference customer = new CustomerReference(OWNER_CUSTOMER_ID, CustomerStatus.ACTIVE);
        AccountViewDto expectedView = new AccountViewDto(ACCOUNT_ID, "BG75NBUB00010100000042", OWNER_CUSTOMER_ID,
            AccountType.CURRENT, Currency.EUR, new BigDecimal("0.00"), AccountStatus.ACTIVE);
        when(customerServiceClient.findById(OWNER_CUSTOMER_ID)).thenReturn(Optional.of(customer));
        when(accountCreator.create(openingDto)).thenReturn(expectedView);

        // Act
        AccountViewDto result = accountService.open(openingDto);

        // Assert
        assertThat(result).isSameAs(expectedView);
        InOrder order = inOrder(customerServiceClient, accountCreator);
        order.verify(customerServiceClient).findById(OWNER_CUSTOMER_ID);
        order.verify(accountCreator).create(openingDto);
    }

    @Test
    @DisplayName("Error Case: Should reject opening for an unknown Customer")
    void shouldRejectUnknownCustomer() {
        // Arrange
        AccountOpeningDto openingDto = new AccountOpeningDto(OWNER_CUSTOMER_ID, AccountType.CURRENT);
        when(customerServiceClient.findById(OWNER_CUSTOMER_ID)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> accountService.open(openingDto)).isInstanceOfSatisfying(DomainException.class,
            exception -> assertThat(exception.getErrorCode()).isEqualTo(ACCOUNT_OWNER_NOT_FOUND));

        verify(customerServiceClient).findById(OWNER_CUSTOMER_ID);
        verify(accountCreator, never()).create(openingDto);
    }

    @Test
    @DisplayName("Error Case: Should reject opening for a closed Customer")
    void shouldRejectClosedCustomer() {
        // Arrange
        AccountOpeningDto openingDto = new AccountOpeningDto(OWNER_CUSTOMER_ID, AccountType.CURRENT);
        CustomerReference customer = new CustomerReference(OWNER_CUSTOMER_ID, CustomerStatus.CLOSED);
        when(customerServiceClient.findById(OWNER_CUSTOMER_ID)).thenReturn(Optional.of(customer));

        // Act & Assert
        assertThatThrownBy(() -> accountService.open(openingDto)).isInstanceOfSatisfying(DomainException.class,
            exception -> assertThat(exception.getErrorCode()).isEqualTo(ACCOUNT_OWNER_NOT_ACTIVE));

        verify(customerServiceClient).findById(OWNER_CUSTOMER_ID);
        verify(accountCreator, never()).create(openingDto);
    }

    @Test
    @DisplayName("Error Case: Should not create an Account when Customer lookup fails")
    void shouldNotCreateAccountWhenCustomerLookupFails() {
        // Arrange
        AccountOpeningDto openingDto = new AccountOpeningDto(OWNER_CUSTOMER_ID, AccountType.CURRENT);
        DomainException unavailable = new DomainException(AccountErrorCode.CUSTOMER_SERVICE_UNAVAILABLE);
        when(customerServiceClient.findById(OWNER_CUSTOMER_ID)).thenThrow(unavailable);

        // Act & Assert
        assertThatThrownBy(() -> accountService.open(openingDto)).isSameAs(unavailable);

        verify(customerServiceClient).findById(OWNER_CUSTOMER_ID);
        verify(accountCreator, never()).create(openingDto);
    }
}
