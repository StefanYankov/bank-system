package bg.nbu.banksystem.account.application;

import bg.nbu.banksystem.account.application.dto.AccountDepositDto;
import bg.nbu.banksystem.account.application.dto.AccountOpeningDto;
import bg.nbu.banksystem.account.application.dto.AccountSummaryDto;
import bg.nbu.banksystem.account.application.dto.AccountTransactionViewDto;
import bg.nbu.banksystem.account.application.dto.AccountViewDto;
import bg.nbu.banksystem.account.application.dto.TransactionHistoryQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.util.UUID;

/**
 * Application use cases for Account retrieval and management.
 */
public interface AccountService {

    /**
     * Opens an Account for an existing active Customer.
     *
     * @param openingDto Account-opening data
     * @return newly opened Account view
     */
    AccountViewDto open(AccountOpeningDto openingDto);

    /**
     * Retrieves one Account by its technical identifier.
     *
     * @param accountId Account identifier
     * @return detailed Account view
     */
    AccountViewDto getById(UUID accountId);

    /**
     * Lists the Accounts owned by one Customer.
     *
     * @param ownerCustomerId Customer Service identifier
     * @param pageable        paging and sorting information
     * @return one page of Account summaries
     */
    Page<AccountSummaryDto> findByOwner(UUID ownerCustomerId, Pageable pageable);

    /**
     * Checks whether a Customer owns at least one ACTIVE Account.
     *
     * @param ownerCustomerId Customer Service identifier
     * @return {@code true} when an ACTIVE Account exists
     */
    boolean hasActiveAccounts(UUID ownerCustomerId);

    /**
     * Deposits funds into an active Account.
     *
     * @param accountId Account identifier
     * @param deposit   deposit data
     * @return updated Account view
     */
    AccountViewDto deposit(UUID accountId, AccountDepositDto deposit);

    /**
     * Permanently closes an active Account with a zero balance.
     *
     * @param accountId Account identifier
     * @return closed Account view
     */
    AccountViewDto close(UUID accountId);

    /**
     * Reads one validated slice of Account transaction history.
     *
     * @param accountId Account identifier
     * @param query     optional date boundaries and requested slice coordinates
     * @return one transaction-history slice
     */
    Slice<AccountTransactionViewDto> getTransactionHistory(UUID accountId, TransactionHistoryQuery query);
}
