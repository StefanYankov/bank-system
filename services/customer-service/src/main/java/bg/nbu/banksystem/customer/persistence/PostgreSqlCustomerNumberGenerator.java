package bg.nbu.banksystem.customer.persistence;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.Locale;
import java.util.Objects;

/**
 * Generates Customer numbers from the service-owned PostgreSQL sequence.
 */
@Repository
public class PostgreSqlCustomerNumberGenerator implements CustomerNumberGenerator {

    private static final String NEXT_SEQUENCE_VALUE_SQL = "SELECT nextval('customer_number_seq')";
    private static final String CUSTOMER_NUMBER_FORMAT = "CUS-%06d";

    private final JdbcClient jdbcClient;

    /**
     * @param jdbcClient JDBC client used to obtain the next sequence value
     */
    public PostgreSqlCustomerNumberGenerator(JdbcClient jdbcClient) {
        this.jdbcClient = Objects.requireNonNull(jdbcClient, "jdbcClient must not be null");
    }

    /**
     * Obtains the next sequence value and formats it as a Customer number.
     *
     * @return the next Customer number
     */
    @Override
    public String generate() {
        Long sequenceValue = jdbcClient.sql(NEXT_SEQUENCE_VALUE_SQL).query(Long.class).single();

        return String.format(Locale.ROOT, CUSTOMER_NUMBER_FORMAT, sequenceValue);
    }
}