package bg.nbu.banksystem.customer.persistence;

import bg.nbu.banksystem.customer.domain.Address;
import bg.nbu.banksystem.customer.domain.LegalEntityCustomer;
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
@DisplayName("LegalEntityCustomerRepository (integration)")
class LegalEntityCustomerRepositoryTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(
        DockerImageName.parse("postgres:18.6-alpine3.24"));
    private static final String EIK = "123456786";
    private static final String OTHER_EIK = "000000305";
    @Autowired
    private LegalEntityCustomerRepository legalEntityCustomerRepository;

    private static LegalEntityCustomer legalEntityCustomer(String customerNumber, String eik) {
        return LegalEntityCustomer.register(customerNumber, "contact@starter.example", "+35929201234",
            "Starter Financial Services", eik, registeredAddress(), correspondenceAddress());
    }

    private static Address registeredAddress() {
        return Address.of("BG", "Sofia", "1000", "1 Alexander Battenberg Square", null);
    }

    private static Address correspondenceAddress() {
        return Address.of("BG", "Sofia", "1000", "10 Business Park Street", "Office 4");
    }

    @Test
    @DisplayName("Happy Path: Should detect an existing EIK")
    void shouldDetectExistingEik() {
        // Arrange
        legalEntityCustomerRepository.saveAndFlush(legalEntityCustomer("CUS-000201", EIK));

        // Act & Assert
        assertThat(legalEntityCustomerRepository.existsByEik(EIK)).isTrue();
    }

    @Test
    @DisplayName("Edge Case: Should exclude the Customer being corrected from the duplicate check")
    void shouldExcludeCurrentCustomerFromDuplicateCheck() {
        // Arrange
        LegalEntityCustomer customer = legalEntityCustomerRepository.saveAndFlush(
            legalEntityCustomer("CUS-000202", EIK));

        // Act & Assert
        assertThat(legalEntityCustomerRepository.existsByEikAndIdNot(EIK, customer.getId())).isFalse();
        assertThat(legalEntityCustomerRepository.existsByEikAndIdNot(EIK, UUID.randomUUID())).isTrue();
    }

    @Test
    @DisplayName("Edge Case: Should not detect an unknown EIK")
    void shouldNotDetectUnknownEik() {
        // Act & Assert
        assertThat(legalEntityCustomerRepository.existsByEik(OTHER_EIK)).isFalse();
        assertThat(legalEntityCustomerRepository.existsByEikAndIdNot(OTHER_EIK, UUID.randomUUID())).isFalse();
    }

    @Test
    @DisplayName("Error Case: Should reject duplicate EIKs")
    void shouldRejectDuplicateEik() {
        // Arrange
        legalEntityCustomerRepository.saveAndFlush(legalEntityCustomer("CUS-000203", EIK));
        LegalEntityCustomer duplicate = legalEntityCustomer("CUS-000204", EIK);

        // Act & Assert
        assertThatThrownBy(() -> legalEntityCustomerRepository.saveAndFlush(duplicate)).isInstanceOf(
            DataIntegrityViolationException.class);
    }
}
