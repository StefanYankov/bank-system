package bg.nbu.banksystem.account.persistence;

import bg.nbu.banksystem.account.domain.Account;
import bg.nbu.banksystem.account.domain.AccountStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Persistence access for the {@link Account} aggregate.
 *
 * <p>
 * Account transactions are written only through the Account aggregate, so a balance change and its
 * history entry cannot be persisted separately. Bounded transaction-history reads are exposed by
 * {@link AccountTransactionQueryRepository}, which intentionally provides no write operations.
 * </p>
 */
public interface AccountRepository extends JpaRepository<Account, UUID> {

    /**
     * Finds an account by its normalized IBAN.
     *
     * @param iban compact, upper-case IBAN
     * @return the matching account, if any
     */
    Optional<Account> findByIban(String iban);

    /**
     * Lists the accounts owned by a Customer.
     *
     * @param ownerCustomerId Customer Service identifier
     * @param pageable        paging and sorting information
     * @return a page of the Customer's accounts
     */
    Page<Account> findByOwnerCustomerId(UUID ownerCustomerId, Pageable pageable);

    /**
     * Checks whether a Customer owns at least one Account in the requested lifecycle status.
     *
     * @param ownerCustomerId Customer Service identifier
     * @param status          lifecycle status to check
     * @return {@code true} when a matching Account exists
     */
    boolean existsByOwnerCustomerIdAndStatus(UUID ownerCustomerId, AccountStatus status);
}
