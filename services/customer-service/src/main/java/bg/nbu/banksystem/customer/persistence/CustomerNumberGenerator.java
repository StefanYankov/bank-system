package bg.nbu.banksystem.customer.persistence;

/**
 * Allocates operational Customer numbers.
 *
 * <p>
 * Callers depend on this contract rather than on the PostgreSQL implementation, so tests can supply a fake.
 * </p>
 */
public interface CustomerNumberGenerator {

    /**
     * Allocates the next Customer number.
     *
     * @return a new, unique Customer number
     */
    String generate();
}