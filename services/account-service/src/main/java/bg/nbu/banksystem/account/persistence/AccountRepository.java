package bg.nbu.banksystem.account.persistence;

import bg.nbu.banksystem.account.domain.Account;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Persistence access for the {@link Account} aggregate.
 *
 * <p>
 * Account transactions have no repository of their own. They are written only through the
 * Account aggregate, so a balance change and its history entry cannot be persisted separately.
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
}
