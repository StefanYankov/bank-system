package bg.nbu.banksystem.customer.application;

import bg.nbu.banksystem.customer.application.dto.AddressDto;
import bg.nbu.banksystem.customer.application.dto.EgnCorrectionDto;
import bg.nbu.banksystem.customer.application.dto.IndividualCustomerNameUpdateDto;
import bg.nbu.banksystem.customer.application.dto.IndividualCustomerRegistrationDto;
import bg.nbu.banksystem.customer.application.dto.IndividualCustomerViewDto;
import bg.nbu.banksystem.customer.application.dto.LegalEntityCustomerRegistrationDto;
import bg.nbu.banksystem.customer.application.dto.LegalEntityCustomerViewDto;
import bg.nbu.banksystem.customer.domain.CustomerErrorCode;
import bg.nbu.banksystem.customer.domain.CustomerStatus;
import bg.nbu.banksystem.customer.persistence.JpaAuditingConfig;
import bg.nbu.banksystem.customer.persistence.PostgreSqlCustomerNumberGenerator;
import bg.nbu.banksystem.starter.error.DomainException;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest(properties = "spring.flyway.locations=classpath:db/migration")
@Testcontainers
@Import({IndividualCustomerServiceImpl.class, LegalEntityCustomerServiceImpl.class, CustomerMapperImpl.class, PostgreSqlCustomerNumberGenerator.class, JpaAuditingConfig.class})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@DisplayName("IndividualCustomerServiceImpl (integration)")
class IndividualCustomerServiceImplIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(
            DockerImageName.parse("postgres:18.6-alpine3.24"));
    private static final String EGN = "6101047500";
    private static final String OTHER_EGN = "0041010002";
    @Autowired
    private IndividualCustomerService individualCustomerService;

    @Autowired
    private LegalEntityCustomerService legalEntityCustomerService;

    @Autowired
    private JdbcClient jdbcClient;

    private static IndividualCustomerRegistrationDto registrationDto(String egn) {
        return new IndividualCustomerRegistrationDto("serafim.gerasimoff@example.com", "+359888123456", "Serafim",
                "Ivanov", "Gerasimoff", egn, address());
    }

    private static AddressDto address() {
        return new AddressDto("BG", "Sofia", "1000", "1 Alexander Battenberg Square", null);
    }

    private static void assertDomainError(ThrowingCallable action, CustomerErrorCode expectedCode) {
        assertThatThrownBy(action).isInstanceOfSatisfying(DomainException.class,
                exception -> assertThat(exception.getErrorCode()).isEqualTo(expectedCode));
    }

    @AfterEach
    void deleteCustomers() {
        jdbcClient.sql("TRUNCATE representatives, individual_customers, legal_entity_customers, customers").update();
    }

    @Test
    @DisplayName("Happy Path: Should register a Customer and load it in a separate transaction")
    void shouldRegisterAndReload() {
        // Act
        IndividualCustomerViewDto registered = individualCustomerService.register(registrationDto(EGN));
        IndividualCustomerViewDto reloaded = individualCustomerService.getById(registered.id());

        // Assert
        assertThat(reloaded.customerNumber()).matches("CUS-\\d{6,}");
        assertThat(reloaded.status()).isEqualTo(CustomerStatus.ACTIVE);
        assertThat(reloaded.egn()).isEqualTo(EGN);
        assertThat(reloaded.residentialAddress().city()).isEqualTo("Sofia");
    }

    @Test
    @DisplayName("Happy Path: Should persist an EGN correction without an explicit save")
    void shouldPersistEgnCorrection() {
        // Arrange
        IndividualCustomerViewDto registered = individualCustomerService.register(registrationDto(EGN));

        // Act
        individualCustomerService.correctEgn(registered.id(), new EgnCorrectionDto(OTHER_EGN));

        // Assert
        assertThat(individualCustomerService.getById(registered.id()).egn()).isEqualTo(OTHER_EGN);
    }

    @Test
    @DisplayName("Happy Path: Should persist closing a Customer")
    void shouldPersistClose() {
        // Arrange
        IndividualCustomerViewDto registered = individualCustomerService.register(registrationDto(EGN));

        // Act
        individualCustomerService.close(registered.id());

        // Assert
        assertThat(individualCustomerService.getById(registered.id()).status()).isEqualTo(CustomerStatus.CLOSED);
    }

    @Test
    @DisplayName("Error Case: Should reject a duplicate EGN and keep a single Customer")
    void shouldRejectDuplicateEgn() {
        // Arrange
        individualCustomerService.register(registrationDto(EGN));

        // Act & Assert
        assertDomainError(() -> individualCustomerService.register(registrationDto(EGN)),
                CustomerErrorCode.CUSTOMER_EGN_ALREADY_REGISTERED);
        assertThat(customerCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("Error Case: Should report a legal entity's id as not found")
    void shouldReportLegalEntityIdAsNotFound() {
        // Arrange
        LegalEntityCustomerViewDto legalEntity = legalEntityCustomerService.register(
                new LegalEntityCustomerRegistrationDto("contact@starter.example", "+35929201234",
                        "Starter Financial Services", "123456786", address(), address()));

        // Act & Assert
        assertDomainError(() -> individualCustomerService.getById(legalEntity.id()),
                CustomerErrorCode.CUSTOMER_NOT_FOUND);
    }

    @Test
    @DisplayName("Error Case: Should reject a change to a closed Customer and keep the stored name")
    void shouldRejectChangeToClosedCustomer() {
        // Arrange
        IndividualCustomerViewDto registered = individualCustomerService.register(registrationDto(EGN));
        individualCustomerService.close(registered.id());

        // Act & Assert
        assertDomainError(() -> individualCustomerService.updateName(registered.id(),
                new IndividualCustomerNameUpdateDto("Serafim", null, "Petrov")), CustomerErrorCode.CUSTOMER_CLOSED);
        assertThat(individualCustomerService.getById(registered.id()).lastName()).isEqualTo("Gerasimoff");
    }

    private long customerCount() {
        return jdbcClient.sql("SELECT COUNT(*) FROM customers").query(Long.class).single();
    }
}