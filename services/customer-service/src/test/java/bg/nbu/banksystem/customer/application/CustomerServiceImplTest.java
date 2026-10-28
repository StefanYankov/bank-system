package bg.nbu.banksystem.customer.application;

import bg.nbu.banksystem.customer.application.dto.CustomerSummaryDto;
import bg.nbu.banksystem.customer.domain.Address;
import bg.nbu.banksystem.customer.domain.Customer;
import bg.nbu.banksystem.customer.domain.CustomerErrorCode;
import bg.nbu.banksystem.customer.domain.CustomerType;
import bg.nbu.banksystem.customer.domain.IndividualCustomer;
import bg.nbu.banksystem.customer.domain.LegalEntityCustomer;
import bg.nbu.banksystem.customer.persistence.CustomerRepository;
import bg.nbu.banksystem.starter.error.DomainException;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CustomerServiceImpl (unit)")
class CustomerServiceImplTest {

    @Mock
    private CustomerRepository customerRepository;
    private CustomerServiceImpl customerService;

    @BeforeEach
    void setUp() {
        customerService = new CustomerServiceImpl(customerRepository, Mappers.getMapper(CustomerMapper.class));
    }

    private static Specification<Customer> anySpecification() {
        return any();
    }

    @Nested
    @DisplayName("Summary")
    class Summary {

        @Test
        @DisplayName("Happy Path: Should return the summary of an existing legal-entity Customer")
        void shouldGetSummary() {
            // Arrange
            LegalEntityCustomer customer = legalEntityCustomer();
            when(customerRepository.findById(customer.getId())).thenReturn(Optional.of(customer));

            // Act
            CustomerSummaryDto summary = customerService.getSummary(customer.getId());

            // Assert
            assertThat(summary.id()).isEqualTo(customer.getId());
            assertThat(summary.displayName()).isEqualTo("Starter Financial Services");
            verify(customerRepository).findById(customer.getId());
        }

        @Test
        @DisplayName("Happy Path: Should return the summary of an existing individual Customer")
        void shouldGetIndividualSummary() {
            // Arrange
            IndividualCustomer customer = individualCustomer();
            when(customerRepository.findById(customer.getId())).thenReturn(Optional.of(customer));

            // Act
            CustomerSummaryDto summary = customerService.getSummary(customer.getId());

            // Assert
            assertThat(summary.customerType()).isEqualTo(CustomerType.INDIVIDUAL);
            assertThat(summary.displayName()).isEqualTo("Serafim Ivanov Gerasimoff");
            assertThat(summary.identifier()).isEqualTo("6101047500");
        }

        @Test
        @DisplayName("Error Case: Should report a missing Customer")
        void shouldReportMissingCustomer() {
            // Arrange
            UUID unknownId = UUID.randomUUID();
            when(customerRepository.findById(unknownId)).thenReturn(Optional.empty());

            // Act & Assert
            assertDomainError(() -> customerService.getSummary(unknownId), CustomerErrorCode.CUSTOMER_NOT_FOUND);
        }
    }

    private static void assertDomainError(ThrowingCallable action, CustomerErrorCode expectedCode) {
        assertThatThrownBy(action).isInstanceOfSatisfying(DomainException.class,
                exception -> assertThat(exception.getErrorCode()).isEqualTo(expectedCode));
    }

    private static IndividualCustomer individualCustomer() {
        return IndividualCustomer.register("CUS-000001", "serafim.gerasimoff@example.com", "+359888123456", "Serafim",
                "Ivanov", "Gerasimoff", "6101047500", residentialAddress());
    }

    private static LegalEntityCustomer legalEntityCustomer() {
        return LegalEntityCustomer.register("CUS-000002", "contact@starter.example", "+35929201234",
                "Starter Financial Services", "123456786", registeredAddress(), correspondenceAddress());
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

    @Nested
    @DisplayName("Search")
    class Search {

        @Test
        @DisplayName("Happy Path: Should map matching Customers of both types to summaries")
        void shouldMapResultsToSummaries() {
            // Arrange
            PageRequest pageable = PageRequest.of(0, 10, CustomerServiceImpl.DEFAULT_SORT);
            when(customerRepository.findAll(anySpecification(), any(Pageable.class))).thenReturn(
                    new PageImpl<>(List.of(individualCustomer(), legalEntityCustomer()), pageable, 2));

            // Act
            Page<CustomerSummaryDto> page = customerService.search("Gerasimoff", pageable);

            // Assert
            assertThat(page.getTotalElements()).isEqualTo(2);
            assertThat(page.getContent()).extracting(CustomerSummaryDto::customerType)
                    .containsExactly(CustomerType.INDIVIDUAL, CustomerType.LEGAL_ENTITY);
        }

        @Test
        @DisplayName("Happy Path: Should apply the default sort when the request has none")
        void shouldApplyDefaultSort() {
            // Arrange
            ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
            when(customerRepository.findAll(anySpecification(), pageableCaptor.capture())).thenReturn(Page.empty());

            // Act
            customerService.search(null, PageRequest.of(2, 15));

            // Assert
            Pageable used = pageableCaptor.getValue();
            assertThat(used.getSort()).isEqualTo(CustomerServiceImpl.DEFAULT_SORT);
            assertThat(used.getPageNumber()).isEqualTo(2);
            assertThat(used.getPageSize()).isEqualTo(15);
        }

        @Test
        @DisplayName("Happy Path: Should keep an explicit sort")
        void shouldKeepExplicitSort() {
            // Arrange
            Sort explicitSort = Sort.by(Sort.Direction.DESC, "createdAt");
            ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
            when(customerRepository.findAll(anySpecification(), pageableCaptor.capture())).thenReturn(Page.empty());

            // Act
            customerService.search("Gerasimoff", PageRequest.of(0, 10, explicitSort));

            // Assert
            assertThat(pageableCaptor.getValue().getSort()).isEqualTo(explicitSort);
        }

        @Test
        @DisplayName("Edge Case: Should pass an unpaged request through unchanged")
        void shouldPassUnpagedRequestThrough() {
            // Arrange
            Pageable unpaged = Pageable.unpaged();
            ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
            when(customerRepository.findAll(anySpecification(), pageableCaptor.capture())).thenReturn(Page.empty());

            // Act
            customerService.search("Gerasimoff", unpaged);

            // Assert
            assertThat(pageableCaptor.getValue()).isSameAs(unpaged);
        }

        @Test
        @DisplayName("Error Case: Should reject a query with too many terms before querying the database")
        void shouldRejectTooManyTerms() {
            // Act & Assert
            assertDomainError(() -> customerService.search("one two three four five six", PageRequest.of(0, 10)),
                    CustomerErrorCode.INVALID_SEARCH_QUERY);
            verifyNoInteractions(customerRepository);
        }

        @ParameterizedTest(name = "Happy Path: Should accept sorting by {0}")
        @ValueSource(strings = {"customerNumber", "status", "createdAt"})
        @DisplayName("Happy Path: Should accept every allowed sort property")
        void shouldAcceptAllowedSortProperty(String property) {
            // Arrange
            when(customerRepository.findAll(anySpecification(), any(Pageable.class))).thenReturn(Page.empty());

            // Act
            customerService.search(null, PageRequest.of(0, 10, Sort.by(property)));

            // Assert
            verify(customerRepository).findAll(anySpecification(), any(Pageable.class));
        }

        @Test
        @DisplayName("Error Case: Should reject a sort property outside the allow-list before querying the database")
        void shouldRejectDisallowedSortProperty() {
            // Act & Assert
            assertThatThrownBy(
                    () -> customerService.search(null, PageRequest.of(0, 10, Sort.by("email")))).isInstanceOfSatisfying(
                    DomainException.class, exception -> {
                        assertThat(exception.getErrorCode()).isEqualTo(CustomerErrorCode.INVALID_SORT_PROPERTY);
                        assertThat(exception.getDetail()).isEqualTo(CustomerServiceImpl.SORTABLE_PROPERTIES_DETAIL);
                    });
            verifyNoInteractions(customerRepository);
        }
    }
}