package bg.nbu.banksystem.customer.persistence;

import bg.nbu.banksystem.customer.domain.LegalEntityCustomer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Repository for {@link LegalEntityCustomer}-specific queries.
 *
 * <p>
 * Callers must normalize the EIK with {@code Eiks.normalize(...)} before querying.
 * </p>
 */
@Repository
public interface LegalEntityCustomerRepository extends JpaRepository<LegalEntityCustomer, UUID> {

    /**
     * Checks whether a legal-entity Customer already exists with the given EIK.
     *
     * @param eik the normalized EIK
     * @return {@code true} if the EIK is already registered
     */
    boolean existsByEik(String eik);

    /**
     * Checks whether a different legal-entity Customer already exists with the given EIK.
     *
     * @param eik the normalized EIK
     * @param id  identifier of the Customer being corrected
     * @return {@code true} if another Customer already uses the EIK
     */
    boolean existsByEikAndIdNot(String eik, UUID id);
}