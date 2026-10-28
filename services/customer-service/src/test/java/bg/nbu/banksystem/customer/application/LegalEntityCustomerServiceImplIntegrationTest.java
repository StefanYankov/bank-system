package bg.nbu.banksystem.customer.application;

import bg.nbu.banksystem.customer.application.dto.AddressDto;
import bg.nbu.banksystem.customer.application.dto.CompanyNameUpdateDto;
import bg.nbu.banksystem.customer.application.dto.EikCorrectionDto;
import bg.nbu.banksystem.customer.application.dto.IndividualCustomerRegistrationDto;
import bg.nbu.banksystem.customer.application.dto.IndividualCustomerViewDto;
import bg.nbu.banksystem.customer.application.dto.LegalEntityCustomerRegistrationDto;
import bg.nbu.banksystem.customer.application.dto.LegalEntityCustomerViewDto;
import bg.nbu.banksystem.customer.application.dto.RepresentationEndDto;
import bg.nbu.banksystem.customer.application.dto.RepresentativeAdditionDto;
import bg.nbu.banksystem.customer.application.dto.RepresentativeCorrectionDto;
import bg.nbu.banksystem.customer.application.dto.RepresentativeViewDto;
import bg.nbu.banksystem.customer.domain.CustomerErrorCode;
import bg.nbu.banksystem.customer.domain.RepresentativeRole;
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

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest(properties = "spring.flyway.locations=classpath:db/migration")
@Testcontainers
@Import({IndividualCustomerServiceImpl.class, LegalEntityCustomerServiceImpl.class, CustomerMapperImpl.class, PostgreSqlCustomerNumberGenerator.class, JpaAuditingConfig.class})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@DisplayName("LegalEntityCustomerServiceImpl (integration)")
class LegalEntityCustomerServiceImplIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(
            DockerImageName.parse("postgres:18.6-alpine3.24"));
    private static final String EIK = "123456786";
    private static final String OTHER_EIK = "000000305";
    private static final LocalDate VALID_FROM = LocalDate.of(2026, 1, 1);
    private static final LocalDate VALID_TO = LocalDate.of(2026, 12, 31);
    @Autowired
    private LegalEntityCustomerService legalEntityCustomerService;

    @Autowired
    private IndividualCustomerService individualCustomerService;

    @Autowired
    private JdbcClient jdbcClient;

    private static LegalEntityCustomerRegistrationDto registrationDto(String eik) {
        return new LegalEntityCustomerRegistrationDto("contact@starter.example", "+35929201234",
                "Starter Financial Services", eik, address(null), address("Office 4"));
    }

    private static IndividualCustomerRegistrationDto individualRegistrationDto() {
        return new IndividualCustomerRegistrationDto("serafim.gerasimoff@example.com", "+359888123456", "Serafim",
                "Ivanov", "Gerasimoff", "6101047500", address(null));
    }

    private static AddressDto address(String addressLine2) {
        return new AddressDto("BG", "Sofia", "1000", "1 Alexander Battenberg Square", addressLine2);
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
        LegalEntityCustomerViewDto registered = legalEntityCustomerService.register(registrationDto(EIK));
        LegalEntityCustomerViewDto reloaded = legalEntityCustomerService.getById(registered.id());

        // Assert
        assertThat(reloaded.customerNumber()).matches("CUS-\\d{6,}");
        assertThat(reloaded.eik()).isEqualTo(EIK);
        assertThat(reloaded.correspondenceAddress().addressLine2()).isEqualTo("Office 4");
        assertThat(reloaded.representatives()).isEmpty();
    }

    @Test
    @DisplayName("Happy Path: Should persist a company name change without an explicit save")
    void shouldPersistCompanyNameChange() {
        // Arrange
        LegalEntityCustomerViewDto registered = legalEntityCustomerService.register(registrationDto(EIK));

        // Act
        legalEntityCustomerService.updateCompanyName(registered.id(), new CompanyNameUpdateDto("Starter Holdings"));

        // Assert
        assertThat(legalEntityCustomerService.getById(registered.id()).companyName()).isEqualTo("Starter Holdings");
    }

    @Test
    @DisplayName("Happy Path: Should persist a linked Representative with names copied from the Customer")
    void shouldPersistLinkedRepresentative() {
        // Arrange
        LegalEntityCustomerViewDto company = legalEntityCustomerService.register(registrationDto(EIK));
        IndividualCustomerViewDto individual = individualCustomerService.register(individualRegistrationDto());

        // Act
        legalEntityCustomerService.addRepresentative(company.id(),
                new RepresentativeAdditionDto(individual.id(), null, null, null,
                        RepresentativeRole.LEGAL_REPRESENTATIVE, VALID_FROM));

        // Assert
        assertThat(legalEntityCustomerService.getById(company.id()).representatives()).singleElement()
                .satisfies(saved -> {
                    assertThat(saved.individualCustomerId()).isEqualTo(individual.id());
                    assertThat(saved.firstName()).isEqualTo("Serafim");
                    assertThat(saved.lastName()).isEqualTo("Gerasimoff");
                });
    }

    @Test
    @DisplayName("Happy Path: Should persist a Representative correction that clears a mistaken end date")
    void shouldPersistRepresentativeCorrection() {
        // Arrange
        LegalEntityCustomerViewDto company = legalEntityCustomerService.register(registrationDto(EIK));
        RepresentativeViewDto added = legalEntityCustomerService.addRepresentative(company.id(),
                new RepresentativeAdditionDto(null, "Jane", null, "Doe", RepresentativeRole.LEGAL_REPRESENTATIVE,
                        VALID_FROM));
        legalEntityCustomerService.endRepresentation(company.id(), added.id(), new RepresentationEndDto(VALID_TO));

        // Act
        legalEntityCustomerService.correctRepresentative(company.id(), added.id(),
                new RepresentativeCorrectionDto(null, "Jane", null, "Doe", RepresentativeRole.AUTHORIZED_REPRESENTATIVE,
                        VALID_FROM, null));

        // Assert
        assertThat(legalEntityCustomerService.getById(company.id()).representatives()).singleElement()
                .satisfies(saved -> {
                    assertThat(saved.role()).isEqualTo(RepresentativeRole.AUTHORIZED_REPRESENTATIVE);
                    assertThat(saved.validTo()).isNull();
                });
    }

    @Test
    @DisplayName("Error Case: Should reject linking an unknown individual Customer and store no Representative")
    void shouldRejectUnknownLinkedCustomer() {
        // Arrange
        LegalEntityCustomerViewDto company = legalEntityCustomerService.register(registrationDto(EIK));

        // Act & Assert
        assertDomainError(() -> legalEntityCustomerService.addRepresentative(company.id(),
                new RepresentativeAdditionDto(UUID.randomUUID(), null, null, null,
                        RepresentativeRole.LEGAL_REPRESENTATIVE, VALID_FROM)), CustomerErrorCode.CUSTOMER_NOT_FOUND);
        assertThat(legalEntityCustomerService.getById(company.id()).representatives()).isEmpty();
    }

    @Test
    @DisplayName("Error Case: Should reject an EIK that belongs to another company and keep the stored EIK")
    void shouldRejectEikOfAnotherCompany() {
        // Arrange
        LegalEntityCustomerViewDto first = legalEntityCustomerService.register(registrationDto(EIK));
        LegalEntityCustomerViewDto second = legalEntityCustomerService.register(registrationDto(OTHER_EIK));

        // Act & Assert
        assertDomainError(() -> legalEntityCustomerService.correctEik(second.id(), new EikCorrectionDto(EIK)),
                CustomerErrorCode.CUSTOMER_EIK_ALREADY_REGISTERED);
        assertThat(legalEntityCustomerService.getById(second.id()).eik()).isEqualTo(OTHER_EIK);
        assertThat(legalEntityCustomerService.getById(first.id()).eik()).isEqualTo(EIK);
    }
}