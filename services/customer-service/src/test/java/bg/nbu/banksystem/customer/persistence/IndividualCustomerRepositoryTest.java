package bg.nbu.banksystem.customer.persistence;

import bg.nbu.banksystem.customer.domain.Address;
import bg.nbu.banksystem.customer.domain.IndividualCustomer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest(properties = "spring.flyway.locations=classpath:db/migration")
@Testcontainers
@Import(JpaAuditingConfig.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("IndividualCustomerRepository (integration)")
class IndividualCustomerRepositoryTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(
        DockerImageName.parse("postgres:18.6-alpine3.24"));
    private static final String EGN = "6101047500";
    private static final String OTHER_EGN = "0041010002";
    @Autowired
    private IndividualCustomerRepository individualCustomerRepository;

    private static IndividualCustomer individualCustomer(String customerNumber, String egn) {
        return IndividualCustomer.register(customerNumber, "serafim.gerasimoff@example.com", "+359888123456", "Serafim",
            "Ivanov", "Gerasimoff", egn, residentialAddress());
    }

    private static Address residentialAddress() {
        return Address.of("BG", "Sofia", "1000", "1 Alexander Battenberg Square", null);
    }

    @Test
    @DisplayName("Happy Path: Should detect an existing EGN")
    void shouldDetectExistingEgn() {
        // Arrange
        individualCustomerRepository.saveAndFlush(individualCustomer("CUS-000101", EGN));

        // Act & Assert
        assertThat(individualCustomerRepository.existsByEgn(EGN)).isTrue();
    }

    @Test
    @DisplayName("Edge Case: Should exclude the Customer being corrected from the duplicate check")
    void shouldExcludeCurrentCustomerFromDuplicateCheck() {
        // Arrange
        IndividualCustomer customer = individualCustomerRepository.saveAndFlush(individualCustomer("CUS-000102", EGN));

        // Act & Assert
        assertThat(individualCustomerRepository.existsByEgnAndIdNot(EGN, customer.getId())).isFalse();
        assertThat(individualCustomerRepository.existsByEgnAndIdNot(EGN, UUID.randomUUID())).isTrue();
    }

    @Test
    @DisplayName("Edge Case: Should not detect an unknown EGN")
    void shouldNotDetectUnknownEgn() {
        // Act & Assert
        assertThat(individualCustomerRepository.existsByEgn(OTHER_EGN)).isFalse();
        assertThat(individualCustomerRepository.existsByEgnAndIdNot(OTHER_EGN, UUID.randomUUID())).isFalse();
    }

    @Test
    @DisplayName("Error Case: Should reject duplicate EGNs")
    void shouldRejectDuplicateEgn() {
        // Arrange
        individualCustomerRepository.saveAndFlush(individualCustomer("CUS-000103", EGN));
        IndividualCustomer duplicate = individualCustomer("CUS-000104", EGN);

        // Act & Assert
        assertThatThrownBy(() -> individualCustomerRepository.saveAndFlush(duplicate)).isInstanceOf(
            DataIntegrityViolationException.class);
    }
}
