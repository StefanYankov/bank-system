package bg.nbu.banksystem.account.application;

import bg.nbu.banksystem.account.application.dto.AccountDepositDto;
import bg.nbu.banksystem.account.application.dto.AccountOpeningDto;
import bg.nbu.banksystem.account.application.dto.AccountSummaryDto;
import bg.nbu.banksystem.account.application.dto.AccountTransactionViewDto;
import bg.nbu.banksystem.account.application.dto.AccountViewDto;
import bg.nbu.banksystem.account.application.dto.TransactionHistoryQuery;
import bg.nbu.banksystem.account.domain.Account;
import bg.nbu.banksystem.account.domain.AccountStatus;
import bg.nbu.banksystem.account.integration.customer.CustomerReference;
import bg.nbu.banksystem.account.integration.customer.CustomerServiceClient;
import bg.nbu.banksystem.account.integration.customer.CustomerStatus;
import bg.nbu.banksystem.account.persistence.AccountRepository;
import bg.nbu.banksystem.account.persistence.AccountTransactionQueryRepository;
import bg.nbu.banksystem.starter.error.DomainException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static bg.nbu.banksystem.account.domain.AccountErrorCode.ACCOUNT_NOT_FOUND;
import static bg.nbu.banksystem.account.domain.AccountErrorCode.ACCOUNT_OWNER_NOT_ACTIVE;
import static bg.nbu.banksystem.account.domain.AccountErrorCode.ACCOUNT_OWNER_NOT_FOUND;

@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;
    private final AccountTransactionQueryRepository transactionQueryRepository;
    private final AccountMapper accountMapper;
    private final TransactionHistoryPolicy transactionHistoryPolicy;
    private final CustomerServiceClient customerServiceClient;
    private final AccountCreator accountCreator;

    @Override
    public AccountViewDto open(AccountOpeningDto openingDto) {
        CustomerReference customer = customerServiceClient.findById(openingDto.ownerCustomerId())
            .orElseThrow(() -> new DomainException(ACCOUNT_OWNER_NOT_FOUND));

        if (customer.status() != CustomerStatus.ACTIVE) {
            throw new DomainException(ACCOUNT_OWNER_NOT_ACTIVE);
        }

        return accountCreator.create(openingDto);
    }

    @Override
    @Transactional(readOnly = true)
    public AccountViewDto getById(UUID accountId) {
        return accountMapper.toView(getAccount(accountId));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AccountSummaryDto> findByOwner(UUID ownerCustomerId, Pageable pageable) {
        return accountRepository.findByOwnerCustomerId(ownerCustomerId, pageable).map(accountMapper::toSummary);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasActiveAccounts(UUID ownerCustomerId) {
        return accountRepository.existsByOwnerCustomerIdAndStatus(ownerCustomerId, AccountStatus.ACTIVE);
    }

    @Override
    @Transactional
    public AccountViewDto deposit(UUID accountId, AccountDepositDto deposit) {
        Account account = getAccount(accountId);
        account.credit(deposit.amount(), deposit.currency(), deposit.reference());
        return accountMapper.toView(account);
    }

    @Override
    @Transactional
    public AccountViewDto close(UUID accountId) {
        Account account = getAccount(accountId);
        account.close();
        return accountMapper.toView(account);
    }

    @Override
    @Transactional(readOnly = true)
    public Slice<AccountTransactionViewDto> getTransactionHistory(UUID accountId, TransactionHistoryQuery query) {
        ResolvedTransactionHistoryQuery resolved = transactionHistoryPolicy.resolve(query);
        ensureAccountExists(accountId);

        return transactionQueryRepository.findHistory(accountId, resolved.from(), resolved.to(), resolved.pageable())
            .map(accountMapper::toTransactionView);
    }

    private Account getAccount(UUID accountId) {
        return accountRepository.findById(accountId).orElseThrow(() -> new DomainException(ACCOUNT_NOT_FOUND));
    }

    private void ensureAccountExists(UUID accountId) {
        if (!accountRepository.existsById(accountId)) {
            throw new DomainException(ACCOUNT_NOT_FOUND);
        }
    }
}
