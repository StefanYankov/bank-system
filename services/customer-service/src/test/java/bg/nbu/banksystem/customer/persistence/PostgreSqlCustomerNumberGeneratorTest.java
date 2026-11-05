package bg.nbu.banksystem.customer.persistence;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@Import(PostgreSqlCustomerNumberGenerator.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("PostgreSqlCustomerNumberGenerator (integration)")
class PostgreSqlCustomerNumberGeneratorTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(
        DockerImageName.parse("postgres:18.6-alpine3.24"));

    @Autowired
    private CustomerNumberGenerator customerNumberGenerator;

    private static long numericPart(String customerNumber) {
        return Long.parseLong(customerNumber.substring("CUS-".length()));
    }

    @Test
    @DisplayName("Happy Path: Should generate a correctly formatted Customer number")
    void shouldGenerateFormattedCustomerNumber() {
        // Act
        String customerNumber = customerNumberGenerator.generate();

        // Assert
        assertThat(customerNumber).matches("CUS-\\d{6,}");
    }

    @Test
    @DisplayName("Happy Path: Should generate distinct sequential Customer numbers")
    void shouldGenerateDistinctSequentialCustomerNumbers() {
        // Act
        String firstCustomerNumber = customerNumberGenerator.generate();
        String secondCustomerNumber = customerNumberGenerator.generate();

        // Assert
        assertThat(secondCustomerNumber).isNotEqualTo(firstCustomerNumber);
        assertThat(numericPart(secondCustomerNumber)).isEqualTo(numericPart(firstCustomerNumber) + 1);
    }
}
