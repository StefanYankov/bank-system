package bg.nbu.banksystem.customer.persistence;

import bg.nbu.banksystem.customer.domain.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for polymorphic {@link Customer} persistence operations.
 *
 * <p>
 * Lookups return the concrete subtype resolved through the inheritance discriminator. Subtype-specific queries
 * live in the subtype repositories. Representatives have no repository: they are persisted through their legal
 * entity, so the closed-Customer rule cannot be bypassed.
 * </p>
 */
@Repository
public interface CustomerRepository extends JpaRepository<Customer, UUID> {

    /**
     * Finds a Customer of either type by the externally visible Customer number.
     *
     * @param customerNumber the Customer number (for example, CUS-000001)
     * @return the matching Customer, or an empty result when no Customer exists
     */
    Optional<Customer> findByCustomerNumber(String customerNumber);
}