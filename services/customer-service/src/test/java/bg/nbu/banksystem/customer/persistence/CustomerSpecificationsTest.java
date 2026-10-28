package bg.nbu.banksystem.customer.persistence;

import bg.nbu.banksystem.customer.domain.Address;
import bg.nbu.banksystem.customer.domain.Customer;
import bg.nbu.banksystem.customer.domain.CustomerStatus;
import bg.nbu.banksystem.customer.domain.IndividualCustomer;
import bg.nbu.banksystem.customer.domain.LegalEntityCustomer;
import jakarta.persistence.EntityManager;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = "spring.flyway.locations=classpath:db/migration")
@Testcontainers
@Import(JpaAuditingConfig.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("CustomerSpecifications (integration)")
class CustomerSpecificationsTest {

    private static final String EGN = "6101047500";
    private static final String OTHER_EGN = "0041010002";
    private static final String CYRILLIC_NAME_EGN = "9921010007";
    private static final String EIK = "123456786";
    private static final String EIK_SHARING_EGN_PREFIX = "610100001";
    private static final String BACKSLASH_COMPANY_EIK = "000000305";

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(
            DockerImageName.parse("postgres:18.6-alpine3.24"));

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void saveCustomers() {
        customerRepository.saveAndFlush(individualCustomer("CUS-000301", "Serafim", "Ivanov", "Gerasimoff", EGN));
        customerRepository.saveAndFlush(individualCustomer("CUS-000302", "Jane", null, "Doe", OTHER_EGN));
        customerRepository.saveAndFlush(legalEntityCustomer("CUS-000303", "Starter Financial Services", EIK));
        customerRepository.saveAndFlush(
                legalEntityCustomer("CUS-000304", "Gerasimoff Holdings", EIK_SHARING_EGN_PREFIX));
        entityManager.clear();
    }

    @Nested
    @DisplayName("Identifiers")
    class Identifiers {

        @Test
        @DisplayName("Happy Path: Should match an EGN by prefix")
        void shouldMatchEgnPrefix() {
            // Act & Assert
            assertThat(customerNumbers("0041")).containsExactly("CUS-000302");
        }

        @Test
        @DisplayName("Happy Path: Should match an EIK by prefix")
        void shouldMatchEikPrefix() {
            // Act & Assert
            assertThat(customerNumbers("1234")).containsExactly("CUS-000303");
        }

        @Test
        @DisplayName("Happy Path: Should find both Customer types when one term matches an EGN and an EIK")
        void shouldMatchEgnAndEikWithSamePrefix() {
            // Act & Assert
            assertThat(customerNumbers("6101")).containsExactly("CUS-000301", "CUS-000304");
        }

        @Test
        @DisplayName("Happy Path: Should match a Customer number partially, ignoring case")
        void shouldMatchCustomerNumberIgnoringCase() {
            // Act & Assert
            assertThat(customerNumbers("cus-00030")).containsExactly("CUS-000301", "CUS-000302", "CUS-000303",
                    "CUS-000304");
        }

        @Test
        @DisplayName("Edge Case: Should not match an EGN by its middle digits")
        void shouldNotMatchEgnMiddleDigits() {
            // Act & Assert
            assertThat(customerNumbers("0475")).isEmpty();
        }
    }

    private static IndividualCustomer individualCustomer(String customerNumber, String firstName,
                                                         @Nullable String middleName, String lastName, String egn) {
        return IndividualCustomer.register(customerNumber, "serafim.gerasimoff@example.com", "+359888123456", firstName,
                middleName, lastName, egn, residentialAddress());
    }

    @Nested
    @DisplayName("Multiple terms")
    class MultipleTerms {

        @Test
        @DisplayName("Happy Path: Should require every term to match, in any order")
        void shouldRequireEveryTerm() {
            // Act & Assert
            assertThat(customerNumbers("Serafim", "Gerasimoff")).containsExactly("CUS-000301");
            assertThat(customerNumbers("Gerasimoff", "Serafim")).containsExactly("CUS-000301");
        }

        @Test
        @DisplayName("Happy Path: Should combine a name and an identifier prefix")
        void shouldCombineNameAndIdentifier() {
            // Act & Assert
            assertThat(customerNumbers("Gerasimoff", "6101")).containsExactly("CUS-000301", "CUS-000304");
            assertThat(customerNumbers("Serafim", "0041")).isEmpty();
        }

        @Test
        @DisplayName("Edge Case: Should match every Customer when there are no terms")
        void shouldMatchEveryCustomerWithoutTerms() {
            // Act & Assert
            assertThat(customerNumbers()).hasSize(4);
        }
    }

    private static LegalEntityCustomer legalEntityCustomer(String customerNumber, String companyName, String eik) {
        return LegalEntityCustomer.register(customerNumber, "contact@starter.example", "+35929201234", companyName, eik,
                registeredAddress(), correspondenceAddress());
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

    private List<String> customerNumbers(String... terms) {
        return customerRepository.findAll(CustomerSpecifications.matchesAllTerms(List.of(terms)),
                PageRequest.of(0, 10, Sort.by("customerNumber"))).map(Customer::getCustomerNumber).getContent();
    }

    @Nested
    @DisplayName("Names")
    class Names {

        @Test
        @DisplayName("Happy Path: Should match first, middle and last names partially, ignoring case")
        void shouldMatchPersonNameParts() {
            // Act & Assert
            assertThat(customerNumbers("serAF")).containsExactly("CUS-000301");
            assertThat(customerNumbers("ivanov")).containsExactly("CUS-000301");
            assertThat(customerNumbers("DOE")).containsExactly("CUS-000302");
        }

        @Test
        @DisplayName("Happy Path: Should match a company name partially")
        void shouldMatchCompanyName() {
            // Act & Assert
            assertThat(customerNumbers("financial")).containsExactly("CUS-000303");
        }

        @Test
        @DisplayName("Happy Path: Should match the same word in a person name and a company name")
        void shouldMatchWordAcrossBothTypes() {
            // Act & Assert
            assertThat(customerNumbers("gerasimoff")).containsExactly("CUS-000301", "CUS-000304");
        }

        @Test
        @DisplayName("Happy Path: Should match Cyrillic names, ignoring case")
        void shouldMatchCyrillicNamesIgnoringCase() {
            // Arrange
            customerRepository.saveAndFlush(
                    individualCustomer("CUS-000305", "Серафим", null, "Герасимов", CYRILLIC_NAME_EGN));
            entityManager.clear();

            // Act & Assert
            assertThat(customerNumbers("серафим")).containsExactly("CUS-000305");
            assertThat(customerNumbers("ГЕРАСИМОВ")).containsExactly("CUS-000305");
            assertThat(customerNumbers("Сераф", "герас")).containsExactly("CUS-000305");
        }
    }

    @Nested
    @DisplayName("Wildcards")
    class Wildcards {

        @Test
        @DisplayName("Edge Case: Should treat % and _ literally instead of as wildcards")
        void shouldEscapeWildcards() {
            // Act & Assert
            assertThat(customerNumbers("%")).isEmpty();
            assertThat(customerNumbers("_")).isEmpty();
            assertThat(customerNumbers("Ser%")).isEmpty();
        }

        @Test
        @DisplayName("Edge Case: Should treat a backslash literally")
        void shouldEscapeBackslash() {
            // Arrange
            customerRepository.saveAndFlush(
                    legalEntityCustomer("CUS-000306", "Back\\Slash Ltd", BACKSLASH_COMPANY_EIK));
            entityManager.clear();

            // Act & Assert
            assertThat(customerNumbers("\\")).containsExactly("CUS-000306");
            assertThat(customerNumbers("k\\s")).containsExactly("CUS-000306");
        }
    }

    @Nested
    @DisplayName("Status")
    class Status {

        @Test
        @DisplayName("Happy Path: Should include closed Customers in the results")
        void shouldIncludeClosedCustomers() {
            // Arrange
            Customer customer = customerRepository.findByCustomerNumber("CUS-000303").orElseThrow();
            customer.close();
            customerRepository.saveAndFlush(customer);
            entityManager.clear();

            // Act
            List<Customer> results = customerRepository.findAll(
                    CustomerSpecifications.matchesAllTerms(List.of("financial")), PageRequest.of(0, 10)).getContent();

            // Assert
            assertThat(results).singleElement()
                    .satisfies(found -> assertThat(found.getStatus()).isEqualTo(CustomerStatus.CLOSED));
        }
    }

    @Nested
    @DisplayName("Paging and sorting")
    class PagingAndSorting {

        @Test
        @DisplayName("Happy Path: Should page and count matches across both Customer types")
        void shouldPageAndCount() {
            // Act
            Page<Customer> page = customerRepository.findAll(CustomerSpecifications.matchesAllTerms(List.of("6101")),
                    PageRequest.of(0, 1, Sort.by("customerNumber")));

            // Assert
            assertThat(page.getTotalElements()).isEqualTo(2);
            assertThat(page.getContent()).extracting(Customer::getCustomerNumber).containsExactly("CUS-000301");
        }

        @Test
        @DisplayName("Happy Path: Should sort matches by Customer number descending")
        void shouldSortDescending() {
            // Act
            Page<Customer> page = customerRepository.findAll(CustomerSpecifications.matchesAllTerms(List.of("6101")),
                    PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "customerNumber")));

            // Assert
            assertThat(page.getContent()).extracting(Customer::getCustomerNumber)
                    .containsExactly("CUS-000304", "CUS-000301");
        }

        @Test
        @DisplayName("Edge Case: Should return an empty page with the correct total beyond the last page")
        void shouldReturnEmptyPageBeyondLastPage() {
            // Act
            Page<Customer> page = customerRepository.findAll(CustomerSpecifications.matchesAllTerms(List.of("6101")),
                    PageRequest.of(5, 10, Sort.by("customerNumber")));

            // Assert
            assertThat(page.getContent()).isEmpty();
            assertThat(page.getTotalElements()).isEqualTo(2);
        }
    }
}