package bg.nbu.banksystem.employee.persistence;

import bg.nbu.banksystem.employee.application.EmployeeNumberGenerator;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.Locale;
import java.util.Objects;

/**
 * Generates employee numbers from the service-owned PostgreSQL sequence.
 *
 * <p>
 * The database sequence provides atomic value allocation across concurrent
 * requests and multiple Employee Service instances. Sequence gaps are accepted.
 * </p>
 */
@Repository
public class PostgreSqlEmployeeNumberGenerator implements EmployeeNumberGenerator {

    private static final String NEXT_SEQUENCE_VALUE_SQL =
            "SELECT nextval('employee_number_seq')";

    private static final String EMPLOYEE_NUMBER_FORMAT = "EMP-%06d";

    private final JdbcClient jdbcClient;

    /**
     * @param jdbcClient JDBC client used to obtain the next sequence value
     */
    public PostgreSqlEmployeeNumberGenerator(JdbcClient jdbcClient) {
        this.jdbcClient = Objects.requireNonNull(
                jdbcClient,
                "jdbcClient must not be null"
        );
    }

    /**
     * Obtains the next sequence value and formats it as an employee number.
     *
     * @return the next employee number
     */
    @Override
    public String generate() {
        Long sequenceValue = jdbcClient.sql(NEXT_SEQUENCE_VALUE_SQL)
                .query(Long.class)
                .single();

        return String.format(
                Locale.ROOT,
                EMPLOYEE_NUMBER_FORMAT,
                sequenceValue
        );
    }
}