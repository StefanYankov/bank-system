package bg.nbu.banksystem.customer.persistence;

/**
 * Allocates operational Customer numbers.
 */
public interface CustomerNumberGenerator {

    /**
     * Allocates the next Customer number.
     *
     * @return a new, unique Customer number
     */
    String generate();
}