package bg.nbu.banksystem.account.application;

import bg.nbu.banksystem.account.application.dto.AccountOpeningDto;
import bg.nbu.banksystem.account.application.dto.AccountViewDto;
import bg.nbu.banksystem.account.domain.Account;
import bg.nbu.banksystem.account.domain.AccountStatus;
import bg.nbu.banksystem.account.domain.AccountType;
import bg.nbu.banksystem.account.domain.Currency;
import bg.nbu.banksystem.account.domain.Iban;
import bg.nbu.banksystem.account.persistence.AccountNumberGenerator;
import bg.nbu.banksystem.account.persistence.AccountRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AccountCreator (unit)")
class AccountCreatorTest {

    private static final UUID OWNER_CUSTOMER_ID = UUID.fromString("01923b7c-8f6d-7a12-9d1e-4b2f8c3e5a11");
    private static final long ACCOUNT_SEQUENCE = 42L;

    @Mock
    private AccountNumberGenerator accountNumberGenerator;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AccountMapper accountMapper;

    @InjectMocks
    private AccountCreator accountCreator;

    @Test
    @DisplayName("Happy Path: Should generate, persist and map a new EUR Account")
    void shouldCreateAccount() {
        // Arrange
        AccountOpeningDto openingDto = new AccountOpeningDto(OWNER_CUSTOMER_ID, AccountType.CURRENT);
        Iban expectedIban = Iban.generate(AccountType.CURRENT, ACCOUNT_SEQUENCE);
        AccountViewDto[] mappedView = new AccountViewDto[1];
        when(accountNumberGenerator.nextAccountSequence()).thenReturn(ACCOUNT_SEQUENCE);
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(accountMapper.toView(any(Account.class))).thenAnswer(invocation -> {
            Account account = invocation.getArgument(0);
            AccountViewDto view = new AccountViewDto(account.getId(), account.getIban(), account.getOwnerCustomerId(),
                account.getType(), account.getCurrency(), account.getBalance(), account.getStatus());
            mappedView[0] = view;
            return view;
        });

        // Act
        AccountViewDto result = accountCreator.create(openingDto);

        // Assert
        ArgumentCaptor<Account> accountCaptor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).save(accountCaptor.capture());
        Account savedAccount = accountCaptor.getValue();
        assertThat(savedAccount.getId()).isNotNull();
        assertThat(savedAccount.getId().version()).isEqualTo(7);
        assertThat(savedAccount.getIban()).isEqualTo(expectedIban.value());
        assertThat(savedAccount.getOwnerCustomerId()).isEqualTo(OWNER_CUSTOMER_ID);
        assertThat(savedAccount.getType()).isEqualTo(AccountType.CURRENT);
        assertThat(savedAccount.getCurrency()).isEqualTo(Currency.EUR);
        assertThat(savedAccount.getBalance()).isEqualByComparingTo("0.00");
        assertThat(savedAccount.getStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(savedAccount.getTransactions()).isEmpty();
        assertThat(result).isSameAs(mappedView[0]);
        verify(accountNumberGenerator).nextAccountSequence();
        verify(accountMapper).toView(savedAccount);
        verifyNoMoreInteractions(accountNumberGenerator, accountRepository, accountMapper);
    }
}
