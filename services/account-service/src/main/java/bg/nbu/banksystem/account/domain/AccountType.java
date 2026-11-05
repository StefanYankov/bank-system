package bg.nbu.banksystem.account.domain;

/**
 * Product type of a bank account.
 */
public enum AccountType {
    CURRENT("01"),
    SAVINGS("02");

    private final String ibanSegment;

    AccountType(String ibanSegment) {
        this.ibanSegment = ibanSegment;
    }

    /**
     * Returns the account-type segment embedded in newly generated IBANs.
     *
     * @return two-digit IBAN account-type segment
     */
    public String ibanSegment() {
        return ibanSegment;
    }
}
