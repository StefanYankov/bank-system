package bg.nbu.banksystem.account.application;

import bg.nbu.banksystem.account.application.dto.AccountOpeningDto;
import bg.nbu.banksystem.account.application.dto.AccountViewDto;
import bg.nbu.banksystem.account.domain.Account;
import bg.nbu.banksystem.account.domain.Currency;
import bg.nbu.banksystem.account.domain.Iban;
import bg.nbu.banksystem.account.persistence.AccountNumberGenerator;
import bg.nbu.banksystem.account.persistence.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Performs the local atomic write required to open an Account.
 */
@Component
@RequiredArgsConstructor
class AccountCreator {

    private final AccountNumberGenerator accountNumberGenerator;
    private final AccountRepository accountRepository;
    private final AccountMapper accountMapper;

    /**
     * Creates and persists one EUR Account after external owner validation has succeeded.
     *
     * @param openingDto validated Account-opening data
     * @return the persisted Account view
     */
    @Transactional
    AccountViewDto create(AccountOpeningDto openingDto) {
        long accountSequence = accountNumberGenerator.nextAccountSequence();
        Iban iban = Iban.generate(openingDto.type(), accountSequence);
        Account account = Account.open(iban, openingDto.ownerCustomerId(), openingDto.type(), Currency.EUR);
        Account savedAccount = accountRepository.save(account);

        return accountMapper.toView(savedAccount);
    }
}
