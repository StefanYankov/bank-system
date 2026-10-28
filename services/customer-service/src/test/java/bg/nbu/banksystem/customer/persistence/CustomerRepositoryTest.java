package bg.nbu.banksystem.customer.persistence;

import bg.nbu.banksystem.customer.domain.Address;
import bg.nbu.banksystem.customer.domain.Customer;
import bg.nbu.banksystem.customer.domain.CustomerStatus;
import bg.nbu.banksystem.customer.domain.CustomerType;
import bg.nbu.banksystem.customer.domain.IndividualCustomer;
import bg.nbu.banksystem.customer.domain.LegalEntityCustomer;
import bg.nbu.banksystem.customer.domain.Representative;
import bg.nbu.banksystem.customer.domain.RepresentativeRole;
import jakarta.persistence.EntityManager;
import org.hibernate.proxy.HibernateProxy;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest(properties = "spring.flyway.locations=classpath:db/migration")
@Testcontainers
@Import(JpaAuditingConfig.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("CustomerRepository (integration)")
class CustomerRepositoryTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(
            DockerImageName.parse("postgres:18.6-alpine3.24"));
    private static final String EGN = "6101047500";
    private static final String OTHER_EGN = "0041010002";
    private static final String EIK = "123456786";
    private static final String COMPANY_NAME = "Starter Financial Services";
    private static final LocalDate VALID_FROM = LocalDate.of(2026, 1, 1);
    private static final LocalDate VALID_TO = LocalDate.of(2026, 12, 31);
    private static final String AUTHENTICATED_SUBJECT = "authenticated-keycloak-subject";
    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcClient jdbcClient;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private LegalEntityCustomer reloadLegalEntity(UUID id) {
        return (LegalEntityCustomer) customerRepository.findById(id).orElseThrow();
    }

    private String customerTypeColumn(UUID id) {
        return jdbcClient.sql("SELECT customer_type FROM customers WHERE id = :id").param("id", id).query(String.class)
                .single();
    }

    @Nested
    @DisplayName("Persistence and inheritance")
    class PersistenceAndInheritance {

        @Test
        @DisplayName("Happy Path: Should persist and reload an individual Customer with its address")
        void shouldPersistAndReloadIndividualCustomer() {
            // Arrange
            IndividualCustomer customer = individualCustomer("CUS-000001", EGN);
            customerRepository.saveAndFlush(customer);
            entityManager.clear();

            // Act
            Customer reloaded = customerRepository.findById(customer.getId()).orElseThrow();

            // Assert
            assertThat(reloaded).isInstanceOf(IndividualCustomer.class);
            IndividualCustomer individual = (IndividualCustomer) reloaded;
            assertThat(individual.getCustomerType()).isEqualTo(CustomerType.INDIVIDUAL);
            assertThat(individual.getStatus()).isEqualTo(CustomerStatus.ACTIVE);
            assertThat(individual.getFirstName()).isEqualTo("Serafim");
            assertThat(individual.getEgn()).isEqualTo(EGN);
            assertThat(individual.getResidentialAddress().getCity()).isEqualTo("Sofia");
            assertThat(individual.getResidentialAddress().getAddressLine2()).isNull();
        }

        @Test
        @DisplayName("Happy Path: Should persist and reload a legal-entity Customer with both addresses")
        void shouldPersistAndReloadLegalEntityCustomer() {
            // Arrange
            LegalEntityCustomer customer = legalEntityCustomer("CUS-000002", EIK);
            customerRepository.saveAndFlush(customer);
            entityManager.clear();

            // Act
            Customer reloaded = customerRepository.findById(customer.getId()).orElseThrow();

            // Assert
            assertThat(reloaded).isInstanceOf(LegalEntityCustomer.class);
            LegalEntityCustomer legalEntity = (LegalEntityCustomer) reloaded;
            assertThat(legalEntity.getCompanyName()).isEqualTo(COMPANY_NAME);
            assertThat(legalEntity.getEik()).isEqualTo(EIK);
            assertThat(legalEntity.getRegisteredAddress().getAddressLine1()).isEqualTo("1 Alexander Battenberg Square");
            assertThat(legalEntity.getCorrespondenceAddress().getAddressLine1()).isEqualTo("10 Business Park Street");
            assertThat(legalEntity.getCorrespondenceAddress().getAddressLine2()).isEqualTo("Office 4");
        }

        @Test
        @DisplayName("Happy Path: Should store the discriminator value for each subtype")
        void shouldStoreDiscriminatorValues() {
            // Arrange
            IndividualCustomer individual = individualCustomer("CUS-000003", EGN);
            LegalEntityCustomer legalEntity = legalEntityCustomer("CUS-000004", EIK);
            customerRepository.saveAndFlush(individual);
            customerRepository.saveAndFlush(legalEntity);

            // Act
            String individualType = customerTypeColumn(individual.getId());
            String legalEntityType = customerTypeColumn(legalEntity.getId());

            // Assert
            assertThat(individualType).isEqualTo("INDIVIDUAL");
            assertThat(legalEntityType).isEqualTo("LEGAL_ENTITY");
        }

        @Test
        @DisplayName("Happy Path: Should page both Customer subtypes through the polymorphic repository")
        void shouldPageBothSubtypes() {
            // Arrange
            customerRepository.saveAndFlush(individualCustomer("CUS-000005", EGN));
            customerRepository.saveAndFlush(legalEntityCustomer("CUS-000006", EIK));
            entityManager.clear();

            // Act
            Page<Customer> page = customerRepository.findAll(PageRequest.of(0, 10));

            // Assert
            assertThat(page.getContent()).hasSize(2).hasAtLeastOneElementOfType(IndividualCustomer.class)
                    .hasAtLeastOneElementOfType(LegalEntityCustomer.class);
        }
    }

    private static IndividualCustomer individualCustomer(String customerNumber, String egn) {
        return IndividualCustomer.register(customerNumber, "serafim.gerasimoff@example.com", "+359888123456", "Serafim",
                "Ivanov", "Gerasimoff", egn, residentialAddress());
    }

    @Nested
    @DisplayName("Auditing and versioning")
    class AuditingAndVersioning {

        @Test
        @DisplayName("Happy Path: Should populate audit fields with the SYSTEM actor and initialize the version")
        void shouldPopulateSystemAuditFields() {
            // Arrange
            IndividualCustomer customer = individualCustomer("CUS-000011", EGN);

            // Act
            customerRepository.saveAndFlush(customer);

            // Assert
            assertThat(customer.getCreatedAt()).isNotNull();
            assertThat(customer.getUpdatedAt()).isNotNull();
            assertThat(customer.getCreatedBy()).isEqualTo(JpaAuditingConfig.SYSTEM_ACTOR);
            assertThat(customer.getUpdatedBy()).isEqualTo(JpaAuditingConfig.SYSTEM_ACTOR);
            assertThat(customer.getVersion()).isZero();
        }

        @Test
        @DisplayName("Happy Path: Should update modification metadata without changing creation metadata")
        void shouldUpdateModificationMetadata() {
            // Arrange
            IndividualCustomer customer = individualCustomer("CUS-000012", EGN);
            customerRepository.saveAndFlush(customer);
            var initialCreatedAt = customer.getCreatedAt();
            var initialCreatedBy = customer.getCreatedBy();
            var initialVersion = customer.getVersion();
            SecurityContextHolder.getContext().setAuthentication(authenticatedJwt(AUTHENTICATED_SUBJECT));

            // Act
            customer.updateName("Serafim", null, "Petrov");
            customerRepository.flush();

            // Assert
            assertThat(customer.getCreatedAt()).isEqualTo(initialCreatedAt);
            assertThat(customer.getCreatedBy()).isEqualTo(initialCreatedBy);
            assertThat(customer.getUpdatedBy()).isEqualTo(AUTHENTICATED_SUBJECT);
            assertThat(customer.getVersion()).isGreaterThan(initialVersion);
        }

        @Test
        @DisplayName("Happy Path: Should persist an EGN correction")
        void shouldPersistEgnCorrection() {
            // Arrange
            IndividualCustomer customer = individualCustomer("CUS-000013", EGN);
            customerRepository.saveAndFlush(customer);

            // Act
            customer.correctEgn(OTHER_EGN);
            customerRepository.flush();
            entityManager.clear();

            // Assert
            IndividualCustomer reloaded = (IndividualCustomer) customerRepository.findById(customer.getId())
                    .orElseThrow();
            assertThat(reloaded.getEgn()).isEqualTo(OTHER_EGN);
        }
    }

    @Nested
    @DisplayName("Business identifier queries")
    class BusinessIdentifierQueries {

        @Test
        @DisplayName("Happy Path: Should find a Customer of either type by Customer number")
        void shouldFindByCustomerNumber() {
            // Arrange
            customerRepository.saveAndFlush(legalEntityCustomer("CUS-000014", EIK));
            entityManager.clear();

            // Act
            var result = customerRepository.findByCustomerNumber("CUS-000014");

            // Assert
            assertThat(result).get().isInstanceOf(LegalEntityCustomer.class);
        }

        @Test
        @DisplayName("Edge Case: Should return an empty result for an unknown Customer number")
        void shouldReturnEmptyResultForUnknownCustomerNumber() {
            // Act & Assert
            assertThat(customerRepository.findByCustomerNumber("CUS-999999")).isEmpty();
        }
    }

    @Nested
    @DisplayName("Database constraints")
    class DatabaseConstraints {

        @Test
        @DisplayName("Error Case: Should reject duplicate Customer numbers across subtypes")
        void shouldRejectDuplicateCustomerNumber() {
            // Arrange
            customerRepository.saveAndFlush(individualCustomer("CUS-000019", EGN));
            LegalEntityCustomer duplicate = legalEntityCustomer("CUS-000019", EIK);

            // Act & Assert
            assertThatThrownBy(() -> customerRepository.saveAndFlush(duplicate)).isInstanceOf(
                    DataIntegrityViolationException.class);
        }

        @Test
        @DisplayName("Error Case: Should reject an unknown status at the database level")
        void shouldRejectUnknownStatus() {
            // Arrange
            IndividualCustomer customer = individualCustomer("CUS-000024", EGN);
            customerRepository.saveAndFlush(customer);

            // Act & Assert
            assertThatThrownBy(() -> jdbcClient.sql("UPDATE customers SET status = 'PENDING' WHERE id = :id")
                    .param("id", customer.getId()).update()).isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        @DisplayName("Error Case: Should reject a representation end date before its start date")
        void shouldRejectInvalidRepresentationPeriod() {
            // Arrange
            LegalEntityCustomer company = legalEntityCustomer("CUS-000025", EIK);
            Representative representative = addLegalRepresentative(company, null);
            customerRepository.saveAndFlush(company);

            // Act & Assert
            assertThatThrownBy(() -> jdbcClient.sql("UPDATE representatives SET valid_to = :validTo WHERE id = :id")
                    .param("validTo", VALID_FROM.minusDays(1)).param("id", representative.getId())
                    .update()).isInstanceOf(DataIntegrityViolationException.class);
        }
    }

    @Nested
    @DisplayName("Entity identity")
    class EntityIdentity {

        @Test
        @DisplayName("Happy Path: Should treat a Customer and its Hibernate proxy as equal")
        void shouldTreatEntityAndProxyAsEqual() {
            // Arrange
            IndividualCustomer customer = customerRepository.saveAndFlush(individualCustomer("CUS-000026", EGN));
            entityManager.clear();

            // Act
            IndividualCustomer proxy = entityManager.getReference(IndividualCustomer.class, customer.getId());

            // Assert
            assertThat(proxy).isInstanceOf(HibernateProxy.class);
            assertThat(customer).isEqualTo(proxy);
            assertThat(proxy).isEqualTo(customer);
            assertThat(customer.hashCode()).isEqualTo(proxy.hashCode());
        }
    }

    private static LegalEntityCustomer legalEntityCustomer(String customerNumber, String eik) {
        return LegalEntityCustomer.register(customerNumber, "contact@starter.example", "+35929201234", COMPANY_NAME,
                eik, registeredAddress(), correspondenceAddress());
    }

    private static Representative addLegalRepresentative(LegalEntityCustomer company,
                                                         @Nullable IndividualCustomer individualCustomer) {
        return company.addRepresentative(individualCustomer, "Serafim", "Ivanov", "Gerasimoff",
                RepresentativeRole.LEGAL_REPRESENTATIVE, VALID_FROM);
    }

    private static Address residentialAddress() {
        return Address.of("BG", "Sofia", "1000", "1 Alexander Battenberg Square", null);
    }

    private static Address registeredAddress() {
        return Address.of("BG", "Sofia", "1000", "1 Alexander Battenberg Square", null);
    }

    private static Address correspondenceAddress() {
        return Address.of("BG", "Sofia", "1000", "10 Business Park Street", "Office 4");
    }

    private static JwtAuthenticationToken authenticatedJwt(String subject) {
        Jwt jwt = Jwt.withTokenValue("test-token").header("alg", "none").subject(subject).build();

        return new JwtAuthenticationToken(jwt, List.of());
    }

    @Nested
    @DisplayName("Representatives")
    class Representatives {

        @Test
        @DisplayName("Happy Path: Should persist a Representative through the legal-entity cascade")
        void shouldPersistRepresentativeThroughCascade() {
            // Arrange
            LegalEntityCustomer company = legalEntityCustomer("CUS-000007", EIK);
            Representative representative = addLegalRepresentative(company, null);

            // Act
            customerRepository.saveAndFlush(company);
            entityManager.clear();

            // Assert
            LegalEntityCustomer reloaded = reloadLegalEntity(company.getId());
            assertThat(reloaded.getRepresentatives()).singleElement().satisfies(saved -> {
                assertThat(saved.getId()).isEqualTo(representative.getId());
                assertThat(saved.getRole()).isEqualTo(RepresentativeRole.LEGAL_REPRESENTATIVE);
                assertThat(saved.getValidFrom()).isEqualTo(VALID_FROM);
                assertThat(saved.getIndividualCustomer()).isNull();
            });
        }

        @Test
        @DisplayName("Happy Path: Should persist the optional link to an individual Customer")
        void shouldPersistLinkToIndividualCustomer() {
            // Arrange
            IndividualCustomer individual = individualCustomer("CUS-000008", EGN);
            customerRepository.saveAndFlush(individual);
            LegalEntityCustomer company = legalEntityCustomer("CUS-000009", EIK);
            addLegalRepresentative(company, individual);

            // Act
            customerRepository.saveAndFlush(company);
            entityManager.clear();

            // Assert
            LegalEntityCustomer reloaded = reloadLegalEntity(company.getId());
            assertThat(reloaded.getRepresentatives().getFirst().getIndividualCustomer().getId()).isEqualTo(
                    individual.getId());
        }

        @Test
        @DisplayName("Happy Path: Should keep an ended Representative in the history")
        void shouldKeepEndedRepresentative() {
            // Arrange
            LegalEntityCustomer company = legalEntityCustomer("CUS-000010", EIK);
            Representative representative = addLegalRepresentative(company, null);
            customerRepository.saveAndFlush(company);

            // Act
            company.endRepresentation(representative.getId(), VALID_TO);
            customerRepository.flush();
            entityManager.clear();

            // Assert
            LegalEntityCustomer reloaded = reloadLegalEntity(company.getId());
            assertThat(reloaded.getRepresentatives()).singleElement()
                    .satisfies(saved -> assertThat(saved.getValidTo()).isEqualTo(VALID_TO));
        }

        @Test
        @DisplayName("Happy Path: Should persist a Representative correction")
        void shouldPersistRepresentativeCorrection() {
            // Arrange
            LegalEntityCustomer company = legalEntityCustomer("CUS-000027", EIK);
            Representative representative = addLegalRepresentative(company, null);
            customerRepository.saveAndFlush(company);
            company.endRepresentation(representative.getId(), VALID_TO);
            customerRepository.flush();
            LocalDate correctedValidFrom = LocalDate.of(2025, 6, 1);

            // Act
            company.correctRepresentative(representative.getId(), null, "Jane", null, "Doe",
                    RepresentativeRole.AUTHORIZED_REPRESENTATIVE, correctedValidFrom, null);
            customerRepository.flush();
            entityManager.clear();

            // Assert
            LegalEntityCustomer reloaded = reloadLegalEntity(company.getId());
            assertThat(reloaded.getRepresentatives()).singleElement().satisfies(saved -> {
                assertThat(saved.getFirstName()).isEqualTo("Jane");
                assertThat(saved.getLastName()).isEqualTo("Doe");
                assertThat(saved.getRole()).isEqualTo(RepresentativeRole.AUTHORIZED_REPRESENTATIVE);
                assertThat(saved.getValidFrom()).isEqualTo(correctedValidFrom);
                assertThat(saved.getValidTo()).isNull();
            });
        }
    }
}