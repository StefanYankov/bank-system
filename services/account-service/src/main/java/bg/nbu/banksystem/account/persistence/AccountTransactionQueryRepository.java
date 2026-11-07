package bg.nbu.banksystem.account.persistence;

import bg.nbu.banksystem.account.domain.AccountTransaction;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.UUID;

/**
 * Read-only persistence access for bounded Account transaction history.
 *
 * <p>
 * Transaction writes remain owned by the Account aggregate and are persisted through
 * {@link AccountRepository}. This repository intentionally exposes no save or delete operations.
 * </p>
 */
public interface AccountTransactionQueryRepository extends Repository<AccountTransaction, UUID> {

    /**
     * Reads one slice of an Account's transaction history within a bounded time interval.
     *
     * @param accountId Account identifier
     * @param from      inclusive creation-time boundary
     * @param to        exclusive creation-time boundary
     * @param pageable  requested slice size; sorting is fixed by this query
     * @return one slice of transaction history
     */
    @Query("""
        select transaction
        from AccountTransaction transaction
        where transaction.account.id = :accountId
          and transaction.createdAt >= :from
          and transaction.createdAt < :to
        order by transaction.createdAt desc, transaction.id desc
        """)
    Slice<AccountTransaction> findHistory(
        @Param("accountId")
        UUID accountId,
        @Param("from")
        Instant from,
        @Param("to")
        Instant to, Pageable pageable);
}
