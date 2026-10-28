package bg.nbu.banksystem.customer.application;

import bg.nbu.banksystem.customer.application.dto.AddressDto;
import bg.nbu.banksystem.customer.application.dto.CustomerContactUpdateDto;
import bg.nbu.banksystem.customer.application.dto.EgnCorrectionDto;
import bg.nbu.banksystem.customer.application.dto.IndividualCustomerNameUpdateDto;
import bg.nbu.banksystem.customer.application.dto.IndividualCustomerRegistrationDto;
import bg.nbu.banksystem.customer.application.dto.IndividualCustomerViewDto;
import bg.nbu.banksystem.customer.domain.Address;
import bg.nbu.banksystem.customer.domain.CustomerErrorCode;
import bg.nbu.banksystem.customer.domain.CustomerStatus;
import bg.nbu.banksystem.customer.domain.IndividualCustomer;
import bg.nbu.banksystem.customer.persistence.CustomerNumberGenerator;
import bg.nbu.banksystem.customer.persistence.IndividualCustomerRepository;
import bg.nbu.banksystem.starter.error.DomainException;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("IndividualCustomerServiceImpl (unit)")
class IndividualCustomerServiceImplTest {

    private static final String CUSTOMER_NUMBER = "CUS-000001";
    private static final String EGN = "6101047500";
    private static final String OTHER_EGN = "0041010002";

    @Mock
    private IndividualCustomerRepository individualCustomerRepository;

    @Mock
    private CustomerNumberGenerator customerNumberGenerator;

    private IndividualCustomerServiceImpl individualCustomerService;

    private static IndividualCustomerRegistrationDto registrationDto(String egn) {
        return new IndividualCustomerRegistrationDto("serafim.gerasimoff@example.com", "+359888123456", "Serafim",
                "Ivanov", "Gerasimoff", egn,
                new AddressDto("bg", "Sofia", "1000", "1 Alexander Battenberg Square", "Floor 2"));
    }

    private static IndividualCustomer existingCustomer() {
        return IndividualCustomer.register(CUSTOMER_NUMBER, "serafim.gerasimoff@example.com", "+359888123456",
                "Serafim", "Ivanov", "Gerasimoff", EGN, residentialAddress());
    }

    private static Address residentialAddress() {
        return Address.of("BG", "Sofia", "1000", "1 Alexander Battenberg Square", null);
    }

    private static void assertDomainError(ThrowingCallable action, CustomerErrorCode expectedCode) {
        assertThatThrownBy(action).isInstanceOfSatisfying(DomainException.class,
                exception -> assertThat(exception.getErrorCode()).isEqualTo(expectedCode));
    }

    @BeforeEach
    void setUp() {
        individualCustomerService = new IndividualCustomerServiceImpl(individualCustomerRepository,
                customerNumberGenerator, Mappers.getMapper(CustomerMapper.class));
    }

    @Nested
    @DisplayName("Register")
    class Register {

        @Test
        @DisplayName("Happy Path: Should register an active Customer with a generated number and address")
        void shouldRegisterCustomer() {
            // Arrange
            when(customerNumberGenerator.generate()).thenReturn(CUSTOMER_NUMBER);
            when(individualCustomerRepository.save(any(IndividualCustomer.class))).thenAnswer(
                    invocation -> invocation.getArgument(0));

            // Act
            IndividualCustomerViewDto view = individualCustomerService.register(registrationDto(EGN));

            // Assert
            assertThat(view.customerNumber()).isEqualTo(CUSTOMER_NUMBER);
            assertThat(view.status()).isEqualTo(CustomerStatus.ACTIVE);
            assertThat(view.egn()).isEqualTo(EGN);
            assertThat(view.residentialAddress().countryCode()).isEqualTo("BG");
            assertThat(view.residentialAddress().city()).isEqualTo("Sofia");
            assertThat(view.residentialAddress().addressLine2()).isEqualTo("Floor 2");
            verify(individualCustomerRepository).save(any(IndividualCustomer.class));
        }

        @Test
        @DisplayName("Edge Case: Should check uniqueness against the normalized EGN")
        void shouldCheckNormalizedEgn() {
            // Arrange
            when(customerNumberGenerator.generate()).thenReturn(CUSTOMER_NUMBER);
            when(individualCustomerRepository.save(any(IndividualCustomer.class))).thenAnswer(
                    invocation -> invocation.getArgument(0));

            // Act
            individualCustomerService.register(registrationDto("  " + EGN + "  "));

            // Assert
            verify(individualCustomerRepository).existsByEgn(EGN);
        }

        @Test
        @DisplayName("Error Case: Should reject an EGN that is already registered without using a number")
        void shouldRejectRegisteredEgn() {
            // Arrange
            when(individualCustomerRepository.existsByEgn(EGN)).thenReturn(true);

            // Act & Assert
            assertDomainError(() -> individualCustomerService.register(registrationDto(EGN)),
                    CustomerErrorCode.CUSTOMER_EGN_ALREADY_REGISTERED);
            verifyNoInteractions(customerNumberGenerator);
            verify(individualCustomerRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Queries")
    class Queries {

        @Test
        @DisplayName("Happy Path: Should return an individual Customer by id")
        void shouldGetById() {
            // Arrange
            IndividualCustomer customer = existingCustomer();
            when(individualCustomerRepository.findById(customer.getId())).thenReturn(Optional.of(customer));

            // Act
            IndividualCustomerViewDto view = individualCustomerService.getById(customer.getId());

            // Assert
            assertThat(view.id()).isEqualTo(customer.getId());
            assertThat(view.firstName()).isEqualTo("Serafim");
        }

        @Test
        @DisplayName("Error Case: Should report a missing Customer")
        void shouldReportMissingCustomer() {
            // Arrange
            UUID unknownId = UUID.randomUUID();
            when(individualCustomerRepository.findById(unknownId)).thenReturn(Optional.empty());

            // Act & Assert
            assertDomainError(() -> individualCustomerService.getById(unknownId), CustomerErrorCode.CUSTOMER_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("Profile changes")
    class ProfileChanges {

        @Test
        @DisplayName("Happy Path: Should update the name without an explicit save")
        void shouldUpdateName() {
            // Arrange
            IndividualCustomer customer = existingCustomer();
            when(individualCustomerRepository.findById(customer.getId())).thenReturn(Optional.of(customer));

            // Act
            IndividualCustomerViewDto view = individualCustomerService.updateName(customer.getId(),
                    new IndividualCustomerNameUpdateDto("Serafim", null, "Petrov"));

            // Assert
            assertThat(view.lastName()).isEqualTo("Petrov");
            assertThat(view.middleName()).isNull();
            verify(individualCustomerRepository, never()).save(any());
        }

        @Test
        @DisplayName("Happy Path: Should replace the residential address")
        void shouldChangeResidentialAddress() {
            // Arrange
            IndividualCustomer customer = existingCustomer();
            when(individualCustomerRepository.findById(customer.getId())).thenReturn(Optional.of(customer));

            // Act
            IndividualCustomerViewDto view = individualCustomerService.changeResidentialAddress(customer.getId(),
                    new AddressDto("BG", "Plovdiv", "4000", "1 Central Square", null));

            // Assert
            assertThat(view.residentialAddress().city()).isEqualTo("Plovdiv");
            assertThat(view.residentialAddress().addressLine2()).isNull();
        }

        @Test
        @DisplayName("Happy Path: Should update the contact information")
        void shouldUpdateContactInformation() {
            // Arrange
            IndividualCustomer customer = existingCustomer();
            when(individualCustomerRepository.findById(customer.getId())).thenReturn(Optional.of(customer));

            // Act
            IndividualCustomerViewDto view = individualCustomerService.updateContactInformation(customer.getId(),
                    new CustomerContactUpdateDto("Serafim.New@Example.COM", "+359888654321"));

            // Assert
            assertThat(view.email()).isEqualTo("serafim.new@example.com");
            assertThat(view.phone()).isEqualTo("+359888654321");
        }

        @Test
        @DisplayName("Error Case: Should pass the closed-customer error through unchanged")
        void shouldPassThroughClosedCustomerError() {
            // Arrange
            IndividualCustomer customer = existingCustomer();
            customer.close();
            when(individualCustomerRepository.findById(customer.getId())).thenReturn(Optional.of(customer));

            // Act & Assert
            assertDomainError(() -> individualCustomerService.updateName(customer.getId(),
                    new IndividualCustomerNameUpdateDto("Serafim", null, "Petrov")), CustomerErrorCode.CUSTOMER_CLOSED);
        }
    }

    @Nested
    @DisplayName("EGN correction")
    class EgnCorrection {

        @Test
        @DisplayName("Happy Path: Should correct the EGN when it is not used by another Customer")
        void shouldCorrectEgn() {
            // Arrange
            IndividualCustomer customer = existingCustomer();
            when(individualCustomerRepository.findById(customer.getId())).thenReturn(Optional.of(customer));

            // Act
            IndividualCustomerViewDto view = individualCustomerService.correctEgn(customer.getId(),
                    new EgnCorrectionDto(OTHER_EGN));

            // Assert
            assertThat(view.egn()).isEqualTo(OTHER_EGN);
            verify(individualCustomerRepository).existsByEgnAndIdNot(OTHER_EGN, customer.getId());
        }

        @Test
        @DisplayName("Error Case: Should reject an EGN that belongs to another Customer")
        void shouldRejectEgnOfAnotherCustomer() {
            // Arrange
            IndividualCustomer customer = existingCustomer();
            when(individualCustomerRepository.findById(customer.getId())).thenReturn(Optional.of(customer));
            when(individualCustomerRepository.existsByEgnAndIdNot(OTHER_EGN, customer.getId())).thenReturn(true);

            // Act & Assert
            assertDomainError(
                    () -> individualCustomerService.correctEgn(customer.getId(), new EgnCorrectionDto(OTHER_EGN)),
                    CustomerErrorCode.CUSTOMER_EGN_ALREADY_REGISTERED);
            assertThat(customer.getEgn()).isEqualTo(EGN);
        }

        @Test
        @DisplayName("Error Case: Should report a missing Customer")
        void shouldReportMissingCustomer() {
            // Arrange
            UUID unknownId = UUID.randomUUID();
            when(individualCustomerRepository.findById(unknownId)).thenReturn(Optional.empty());

            // Act & Assert
            assertDomainError(() -> individualCustomerService.correctEgn(unknownId, new EgnCorrectionDto(OTHER_EGN)),
                    CustomerErrorCode.CUSTOMER_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("Lifecycle")
    class Lifecycle {

        @Test
        @DisplayName("Happy Path: Should close an active Customer")
        void shouldCloseCustomer() {
            // Arrange
            IndividualCustomer customer = existingCustomer();
            when(individualCustomerRepository.findById(customer.getId())).thenReturn(Optional.of(customer));

            // Act
            IndividualCustomerViewDto view = individualCustomerService.close(customer.getId());

            // Assert
            assertThat(view.status()).isEqualTo(CustomerStatus.CLOSED);
        }

        @Test
        @DisplayName("Happy Path: Should reopen a closed Customer")
        void shouldReopenCustomer() {
            // Arrange
            IndividualCustomer customer = existingCustomer();
            customer.close();
            when(individualCustomerRepository.findById(customer.getId())).thenReturn(Optional.of(customer));

            // Act
            IndividualCustomerViewDto view = individualCustomerService.reopen(customer.getId());

            // Assert
            assertThat(view.status()).isEqualTo(CustomerStatus.ACTIVE);
        }

        @Test
        @DisplayName("Error Case: Should pass the already-closed error through unchanged")
        void shouldPassThroughAlreadyClosedError() {
            // Arrange
            IndividualCustomer customer = existingCustomer();
            customer.close();
            when(individualCustomerRepository.findById(customer.getId())).thenReturn(Optional.of(customer));

            // Act & Assert
            assertDomainError(() -> individualCustomerService.close(customer.getId()),
                    CustomerErrorCode.CUSTOMER_ALREADY_CLOSED);
        }

        @Test
        @DisplayName("Error Case: Should report a missing Customer")
        void shouldReportMissingCustomer() {
            // Arrange
            UUID unknownId = UUID.randomUUID();
            when(individualCustomerRepository.findById(unknownId)).thenReturn(Optional.empty());

            // Act & Assert
            assertDomainError(() -> individualCustomerService.reopen(unknownId), CustomerErrorCode.CUSTOMER_NOT_FOUND);
        }
    }
}