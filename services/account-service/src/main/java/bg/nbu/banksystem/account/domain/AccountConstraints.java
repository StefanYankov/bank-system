package bg.nbu.banksystem.account.domain;

/**
 * Length and precision limits shared by Account API validation and JPA mapping.
 *
 * <p>
 * Flyway migrations repeat these values in SQL. Hibernate schema validation
 * detects mismatches between the database schema and entity mappings.
 * </p>
 */
public final class AccountConstraints {

    public static final int IBAN_LENGTH = 22;

    public static final int ACCOUNT_TYPE_MAX_LENGTH = 16;
    public static final int ACCOUNT_STATUS_MAX_LENGTH = 16;
    public static final int TRANSACTION_TYPE_MAX_LENGTH = 16;
    public static final int CURRENCY_LENGTH = 3;

    public static final int TRANSACTION_REFERENCE_MAX_LENGTH = 140;

    public static final int MONEY_PRECISION = 19;
    public static final int MONEY_SCALE = 2;

    private AccountConstraints() {
    }
}
