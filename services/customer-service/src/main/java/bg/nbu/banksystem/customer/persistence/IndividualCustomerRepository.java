package bg.nbu.banksystem.customer.persistence;

import bg.nbu.banksystem.customer.domain.IndividualCustomer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Repository for {@link IndividualCustomer}-specific queries.
 *
 * <p>
 * Callers must normalize the EGN with {@code Egns.normalize(...)} before querying, so the comparison uses the same
 * canonical value that the entity stores.
 * </p>
 */
@Repository
public interface IndividualCustomerRepository extends JpaRepository<IndividualCustomer, UUID> {

    /**
     * Checks whether an individual Customer already exists with the given EGN.
     *
     * @param egn the normalized EGN
     * @return {@code true} if the EGN is already registered
     */
    boolean existsByEgn(String egn);

    /**
     * Checks whether a different individual Customer already exists with the given EGN.
     *
     * @param egn the normalized EGN
     * @param id  identifier of the Customer being corrected
     * @return {@code true} if another Customer already uses the EGN
     */
    boolean existsByEgnAndIdNot(String egn, UUID id);
}