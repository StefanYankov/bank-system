package bg.nbu.banksystem.account.persistence;

/**
 * Supplies the account sequence segment used to build new IBANs.
 */
public interface AccountNumberGenerator {

    /**
     * Returns the next unused account sequence value.
     *
     * @return positive sequence value within the IBAN account segment range
     */
    long nextAccountSequence();
}
