package bg.nbu.banksystem.account.persistence;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.Objects;

/**
 * Generates account sequence values from the service-owned PostgreSQL sequence.
 */
@Repository
public class PostgreSqlAccountNumberGenerator implements AccountNumberGenerator {

    private static final String NEXT_SEQUENCE_VALUE_SQL = "SELECT nextval('account_number_seq')";

    private final JdbcClient jdbcClient;

    /**
     * @param jdbcClient JDBC client used to obtain the next sequence value
     */
    public PostgreSqlAccountNumberGenerator(JdbcClient jdbcClient) {
        this.jdbcClient = Objects.requireNonNull(jdbcClient, "jdbcClient must not be null");
    }

    /**
     * Obtains the next value of {@code account_number_seq}.
     *
     * @return the next account sequence value
     */
    @Override
    public long nextAccountSequence() {
        return jdbcClient.sql(NEXT_SEQUENCE_VALUE_SQL).query(Long.class).single();
    }
}
