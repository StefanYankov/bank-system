package bg.nbu.banksystem.employee.persistence;

import bg.nbu.banksystem.employee.application.EmployeeNumberGenerator;
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
@Import(PostgreSqlEmployeeNumberGenerator.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("PostgreSqlEmployeeNumberGenerator (integration)")
class PostgreSqlEmployeeNumberGeneratorTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer(
                    DockerImageName.parse("postgres:18.6-alpine3.24")
            );

    @Autowired
    private EmployeeNumberGenerator employeeNumberGenerator;

    @Test
    @DisplayName("Happy Path: Should generate a correctly formatted employee number")
    void shouldGenerateFormattedEmployeeNumber() {
        // Act
        String employeeNumber = employeeNumberGenerator.generate();

        // Assert
        assertThat(employeeNumber)
                .matches("EMP-\\d{6,}");
    }

    @Test
    @DisplayName("Happy Path: Should generate distinct sequential employee numbers")
    void shouldGenerateDistinctSequentialEmployeeNumbers() {
        // Act
        String firstEmployeeNumber =
                employeeNumberGenerator.generate();

        String secondEmployeeNumber =
                employeeNumberGenerator.generate();

        // Assert
        assertThat(secondEmployeeNumber)
                .isNotEqualTo(firstEmployeeNumber);

        long firstSequenceValue =
                numericPart(firstEmployeeNumber);

        long secondSequenceValue =
                numericPart(secondEmployeeNumber);

        assertThat(secondSequenceValue)
                .isEqualTo(firstSequenceValue + 1);
    }

    private static long numericPart(String employeeNumber) {
        return Long.parseLong(
                employeeNumber.substring("EMP-".length())
        );
    }
}