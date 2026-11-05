package bg.nbu.banksystem.account.persistence;

import bg.nbu.banksystem.account.domain.AccountType;
import bg.nbu.banksystem.account.domain.Iban;
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
@Import(PostgreSqlAccountNumberGenerator.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("PostgreSqlAccountNumberGenerator (integration)")
class PostgreSqlAccountNumberGeneratorTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(
        DockerImageName.parse("postgres:18.6-alpine3.24"));

    @Autowired
    private AccountNumberGenerator accountNumberGenerator;

    @Test
    @DisplayName("Happy Path: Should generate distinct sequential account numbers")
    void shouldGenerateDistinctSequentialAccountNumbers() {
        // Act
        long firstSequence = accountNumberGenerator.nextAccountSequence();
        long secondSequence = accountNumberGenerator.nextAccountSequence();

        // Assert
        assertThat(firstSequence).isPositive();
        assertThat(secondSequence).isEqualTo(firstSequence + 1);
    }

    @Test
    @DisplayName("Happy Path: Should produce a sequence value accepted by IBAN generation")
    void shouldProduceSequenceAcceptedByIbanGeneration() {
        // Arrange
        long sequence = accountNumberGenerator.nextAccountSequence();

        // Act
        Iban iban = Iban.generate(AccountType.CURRENT, sequence);

        // Assert
        assertThat(iban.value()).startsWith("BG").hasSize(22);
    }
}
