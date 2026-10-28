package bg.nbu.banksystem.customer.application;

import bg.nbu.banksystem.customer.application.dto.AddressDto;
import bg.nbu.banksystem.customer.application.dto.CompanyNameUpdateDto;
import bg.nbu.banksystem.customer.application.dto.CustomerContactUpdateDto;
import bg.nbu.banksystem.customer.application.dto.EikCorrectionDto;
import bg.nbu.banksystem.customer.application.dto.LegalEntityCustomerRegistrationDto;
import bg.nbu.banksystem.customer.application.dto.LegalEntityCustomerViewDto;
import bg.nbu.banksystem.customer.application.dto.RepresentationEndDto;
import bg.nbu.banksystem.customer.application.dto.RepresentativeAdditionDto;
import bg.nbu.banksystem.customer.application.dto.RepresentativeCorrectionDto;
import bg.nbu.banksystem.customer.application.dto.RepresentativeViewDto;
import bg.nbu.banksystem.customer.domain.Address;
import bg.nbu.banksystem.customer.domain.CustomerErrorCode;
import bg.nbu.banksystem.customer.domain.CustomerStatus;
import bg.nbu.banksystem.customer.domain.IndividualCustomer;
import bg.nbu.banksystem.customer.domain.LegalEntityCustomer;
import bg.nbu.banksystem.customer.domain.Representative;
import bg.nbu.banksystem.customer.domain.RepresentativeRole;
import bg.nbu.banksystem.customer.persistence.CustomerNumberGenerator;
import bg.nbu.banksystem.customer.persistence.IndividualCustomerRepository;
import bg.nbu.banksystem.customer.persistence.LegalEntityCustomerRepository;
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

import java.time.LocalDate;
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
@DisplayName("LegalEntityCustomerServiceImpl (unit)")
class LegalEntityCustomerServiceImplTest {

    private static final String CUSTOMER_NUMBER = "CUS-000002";
    private static final String COMPANY_NAME = "Starter Financial Services";
    private static final String EIK = "123456786";
    private static final String OTHER_EIK = "000000305";
    private static final LocalDate VALID_FROM = LocalDate.of(2026, 1, 1);
    private static final LocalDate VALID_TO = LocalDate.of(2026, 12, 31);

    @Mock
    private LegalEntityCustomerRepository legalEntityCustomerRepository;

    @Mock
    private IndividualCustomerRepository individualCustomerRepository;

    @Mock
    private CustomerNumberGenerator customerNumberGenerator;

    private LegalEntityCustomerServiceImpl legalEntityCustomerService;

    private static LegalEntityCustomerRegistrationDto registrationDto(String eik) {
        return new LegalEntityCustomerRegistrationDto("contact@starter.example", "+35929201234", COMPANY_NAME, eik,
                new AddressDto("bg", "Sofia", "1000", "1 Alexander Battenberg Square", null),
                new AddressDto("BG", "Sofia", "1000", "10 Business Park Street", "Office 4"));
    }

    private static RepresentativeAdditionDto linkedAddition(UUID individualCustomerId) {
        return new RepresentativeAdditionDto(individualCustomerId, null, null, null,
                RepresentativeRole.LEGAL_REPRESENTATIVE, VALID_FROM);
    }

    private static Representative addUnlinkedRepresentative(LegalEntityCustomer customer) {
        return customer.addRepresentative(null, "Serafim", "Ivanov", "Gerasimoff",
                RepresentativeRole.LEGAL_REPRESENTATIVE, VALID_FROM);
    }

    private static LegalEntityCustomer existingCustomer() {
        return LegalEntityCustomer.register(CUSTOMER_NUMBER, "contact@starter.example", "+35929201234", COMPANY_NAME,
                EIK, registeredAddress(), correspondenceAddress());
    }

    private static IndividualCustomer individualCustomer() {
        return IndividualCustomer.register("CUS-000001", "serafim.gerasimoff@example.com", "+359888123456", "Serafim",
                "Ivanov", "Gerasimoff", "6101047500", registeredAddress());
    }

    private static Address registeredAddress() {
        return Address.of("BG", "Sofia", "1000", "1 Alexander Battenberg Square", null);
    }

    private static Address correspondenceAddress() {
        return Address.of("BG", "Sofia", "1000", "10 Business Park Street", "Office 4");
    }

    private static void assertDomainError(ThrowingCallable action, CustomerErrorCode expectedCode) {
        assertThatThrownBy(action).isInstanceOfSatisfying(DomainException.class,
                exception -> assertThat(exception.getErrorCode()).isEqualTo(expectedCode));
    }

    @BeforeEach
    void setUp() {
        legalEntityCustomerService = new LegalEntityCustomerServiceImpl(legalEntityCustomerRepository,
                individualCustomerRepository, customerNumberGenerator, Mappers.getMapper(CustomerMapper.class));
    }

    @Nested
    @DisplayName("Register")
    class Register {

        @Test
        @DisplayName("Happy Path: Should register an active Customer with a generated number and both addresses")
        void shouldRegisterCustomer() {
            // Arrange
            when(customerNumberGenerator.generate()).thenReturn(CUSTOMER_NUMBER);
            when(legalEntityCustomerRepository.save(any(LegalEntityCustomer.class))).thenAnswer(
                    invocation -> invocation.getArgument(0));

            // Act
            LegalEntityCustomerViewDto view = legalEntityCustomerService.register(registrationDto(EIK));

            // Assert
            assertThat(view.customerNumber()).isEqualTo(CUSTOMER_NUMBER);
            assertThat(view.status()).isEqualTo(CustomerStatus.ACTIVE);
            assertThat(view.eik()).isEqualTo(EIK);
            assertThat(view.registeredAddress().countryCode()).isEqualTo("BG");
            assertThat(view.correspondenceAddress().addressLine2()).isEqualTo("Office 4");
            assertThat(view.representatives()).isEmpty();
            verify(legalEntityCustomerRepository).save(any(LegalEntityCustomer.class));
        }

        @Test
        @DisplayName("Edge Case: Should check uniqueness against the normalized EIK")
        void shouldCheckNormalizedEik() {
            // Arrange
            when(customerNumberGenerator.generate()).thenReturn(CUSTOMER_NUMBER);
            when(legalEntityCustomerRepository.save(any(LegalEntityCustomer.class))).thenAnswer(
                    invocation -> invocation.getArgument(0));

            // Act
            legalEntityCustomerService.register(registrationDto("  " + EIK + "  "));

            // Assert
            verify(legalEntityCustomerRepository).existsByEik(EIK);
        }

        @Test
        @DisplayName("Error Case: Should reject an EIK that is already registered without using a number")
        void shouldRejectRegisteredEik() {
            // Arrange
            when(legalEntityCustomerRepository.existsByEik(EIK)).thenReturn(true);

            // Act & Assert
            assertDomainError(() -> legalEntityCustomerService.register(registrationDto(EIK)),
                    CustomerErrorCode.CUSTOMER_EIK_ALREADY_REGISTERED);
            verifyNoInteractions(customerNumberGenerator);
            verify(legalEntityCustomerRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Queries")
    class Queries {

        @Test
        @DisplayName("Happy Path: Should return a legal-entity Customer with its Representatives")
        void shouldGetById() {
            // Arrange
            LegalEntityCustomer customer = existingCustomer();
            addUnlinkedRepresentative(customer);
            when(legalEntityCustomerRepository.findById(customer.getId())).thenReturn(Optional.of(customer));

            // Act
            LegalEntityCustomerViewDto view = legalEntityCustomerService.getById(customer.getId());

            // Assert
            assertThat(view.id()).isEqualTo(customer.getId());
            assertThat(view.companyName()).isEqualTo(COMPANY_NAME);
            assertThat(view.representatives()).hasSize(1);
        }

        @Test
        @DisplayName("Error Case: Should report a missing Customer")
        void shouldReportMissingCustomer() {
            // Arrange
            UUID unknownId = UUID.randomUUID();
            when(legalEntityCustomerRepository.findById(unknownId)).thenReturn(Optional.empty());

            // Act & Assert
            assertDomainError(() -> legalEntityCustomerService.getById(unknownId),
                    CustomerErrorCode.CUSTOMER_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("Profile changes")
    class ProfileChanges {

        @Test
        @DisplayName("Happy Path: Should update the company name without an explicit save")
        void shouldUpdateCompanyName() {
            // Arrange
            LegalEntityCustomer customer = existingCustomer();
            when(legalEntityCustomerRepository.findById(customer.getId())).thenReturn(Optional.of(customer));

            // Act
            LegalEntityCustomerViewDto view = legalEntityCustomerService.updateCompanyName(customer.getId(),
                    new CompanyNameUpdateDto("Starter Holdings"));

            // Assert
            assertThat(view.companyName()).isEqualTo("Starter Holdings");
            verify(legalEntityCustomerRepository, never()).save(any());
        }

        @Test
        @DisplayName("Happy Path: Should replace the registered address")
        void shouldChangeRegisteredAddress() {
            // Arrange
            LegalEntityCustomer customer = existingCustomer();
            when(legalEntityCustomerRepository.findById(customer.getId())).thenReturn(Optional.of(customer));

            // Act
            LegalEntityCustomerViewDto view = legalEntityCustomerService.changeRegisteredAddress(customer.getId(),
                    new AddressDto("BG", "Plovdiv", "4000", "1 Central Square", null));

            // Assert
            assertThat(view.registeredAddress().city()).isEqualTo("Plovdiv");
            assertThat(view.correspondenceAddress().city()).isEqualTo("Sofia");
        }

        @Test
        @DisplayName("Happy Path: Should replace the correspondence address")
        void shouldChangeCorrespondenceAddress() {
            // Arrange
            LegalEntityCustomer customer = existingCustomer();
            when(legalEntityCustomerRepository.findById(customer.getId())).thenReturn(Optional.of(customer));

            // Act
            LegalEntityCustomerViewDto view = legalEntityCustomerService.changeCorrespondenceAddress(customer.getId(),
                    new AddressDto("BG", "Varna", "9000", "1 Independence Square", "Office 2"));

            // Assert
            assertThat(view.correspondenceAddress().city()).isEqualTo("Varna");
            assertThat(view.registeredAddress().city()).isEqualTo("Sofia");
        }

        @Test
        @DisplayName("Happy Path: Should update the contact information")
        void shouldUpdateContactInformation() {
            // Arrange
            LegalEntityCustomer customer = existingCustomer();
            when(legalEntityCustomerRepository.findById(customer.getId())).thenReturn(Optional.of(customer));

            // Act
            LegalEntityCustomerViewDto view = legalEntityCustomerService.updateContactInformation(customer.getId(),
                    new CustomerContactUpdateDto("Office@Starter.Example", "+35929204321"));

            // Assert
            assertThat(view.email()).isEqualTo("office@starter.example");
            assertThat(view.phone()).isEqualTo("+35929204321");
        }

        @Test
        @DisplayName("Error Case: Should pass the closed-customer error through unchanged")
        void shouldPassThroughClosedCustomerError() {
            // Arrange
            LegalEntityCustomer customer = existingCustomer();
            customer.close();
            when(legalEntityCustomerRepository.findById(customer.getId())).thenReturn(Optional.of(customer));

            // Act & Assert
            assertDomainError(() -> legalEntityCustomerService.updateCompanyName(customer.getId(),
                    new CompanyNameUpdateDto("Starter Holdings")), CustomerErrorCode.CUSTOMER_CLOSED);
        }
    }

    @Nested
    @DisplayName("EIK correction")
    class EikCorrection {

        @Test
        @DisplayName("Happy Path: Should correct the EIK when it is not used by another Customer")
        void shouldCorrectEik() {
            // Arrange
            LegalEntityCustomer customer = existingCustomer();
            when(legalEntityCustomerRepository.findById(customer.getId())).thenReturn(Optional.of(customer));

            // Act
            LegalEntityCustomerViewDto view = legalEntityCustomerService.correctEik(customer.getId(),
                    new EikCorrectionDto(OTHER_EIK));

            // Assert
            assertThat(view.eik()).isEqualTo(OTHER_EIK);
            verify(legalEntityCustomerRepository).existsByEikAndIdNot(OTHER_EIK, customer.getId());
        }

        @Test
        @DisplayName("Error Case: Should reject an EIK that belongs to another Customer and keep the current EIK")
        void shouldRejectEikOfAnotherCustomer() {
            // Arrange
            LegalEntityCustomer customer = existingCustomer();
            when(legalEntityCustomerRepository.findById(customer.getId())).thenReturn(Optional.of(customer));
            when(legalEntityCustomerRepository.existsByEikAndIdNot(OTHER_EIK, customer.getId())).thenReturn(true);

            // Act & Assert
            assertDomainError(
                    () -> legalEntityCustomerService.correctEik(customer.getId(), new EikCorrectionDto(OTHER_EIK)),
                    CustomerErrorCode.CUSTOMER_EIK_ALREADY_REGISTERED);
            assertThat(customer.getEik()).isEqualTo(EIK);
        }

        @Test
        @DisplayName("Error Case: Should report a missing Customer")
        void shouldReportMissingCustomer() {
            // Arrange
            UUID unknownId = UUID.randomUUID();
            when(legalEntityCustomerRepository.findById(unknownId)).thenReturn(Optional.empty());

            // Act & Assert
            assertDomainError(() -> legalEntityCustomerService.correctEik(unknownId, new EikCorrectionDto(OTHER_EIK)),
                    CustomerErrorCode.CUSTOMER_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("Lifecycle")
    class Lifecycle {

        @Test
        @DisplayName("Happy Path: Should close an active Customer and keep its Representatives")
        void shouldCloseCustomer() {
            // Arrange
            LegalEntityCustomer customer = existingCustomer();
            addUnlinkedRepresentative(customer);
            when(legalEntityCustomerRepository.findById(customer.getId())).thenReturn(Optional.of(customer));

            // Act
            LegalEntityCustomerViewDto view = legalEntityCustomerService.close(customer.getId());

            // Assert
            assertThat(view.status()).isEqualTo(CustomerStatus.CLOSED);
            assertThat(view.representatives()).singleElement()
                    .satisfies(representative -> assertThat(representative.validTo()).isNull());
        }

        @Test
        @DisplayName("Happy Path: Should reopen a closed Customer")
        void shouldReopenCustomer() {
            // Arrange
            LegalEntityCustomer customer = existingCustomer();
            customer.close();
            when(legalEntityCustomerRepository.findById(customer.getId())).thenReturn(Optional.of(customer));

            // Act
            LegalEntityCustomerViewDto view = legalEntityCustomerService.reopen(customer.getId());

            // Assert
            assertThat(view.status()).isEqualTo(CustomerStatus.ACTIVE);
        }

        @Test
        @DisplayName("Error Case: Should pass the already-closed error through unchanged")
        void shouldPassThroughAlreadyClosedError() {
            // Arrange
            LegalEntityCustomer customer = existingCustomer();
            customer.close();
            when(legalEntityCustomerRepository.findById(customer.getId())).thenReturn(Optional.of(customer));

            // Act & Assert
            assertDomainError(() -> legalEntityCustomerService.close(customer.getId()),
                    CustomerErrorCode.CUSTOMER_ALREADY_CLOSED);
        }
    }

    @Nested
    @DisplayName("Add representative")
    class AddRepresentative {

        @Test
        @DisplayName("Happy Path: Should add an unlinked Representative with the supplied names")
        void shouldAddUnlinkedRepresentative() {
            // Arrange
            LegalEntityCustomer customer = existingCustomer();
            when(legalEntityCustomerRepository.findById(customer.getId())).thenReturn(Optional.of(customer));

            // Act
            RepresentativeViewDto view = legalEntityCustomerService.addRepresentative(customer.getId(),
                    new RepresentativeAdditionDto(null, "Jane", null, "Doe",
                            RepresentativeRole.AUTHORIZED_REPRESENTATIVE, VALID_FROM));

            // Assert
            assertThat(view.individualCustomerId()).isNull();
            assertThat(view.firstName()).isEqualTo("Jane");
            assertThat(view.lastName()).isEqualTo("Doe");
            assertThat(view.role()).isEqualTo(RepresentativeRole.AUTHORIZED_REPRESENTATIVE);
            assertThat(customer.getRepresentatives()).hasSize(1);
            verifyNoInteractions(individualCustomerRepository);
        }

        @Test
        @DisplayName("Happy Path: Should copy the names from a linked individual Customer")
        void shouldCopyNamesFromLinkedCustomer() {
            // Arrange
            LegalEntityCustomer customer = existingCustomer();
            IndividualCustomer individual = individualCustomer();
            when(individualCustomerRepository.findById(individual.getId())).thenReturn(Optional.of(individual));
            when(legalEntityCustomerRepository.findById(customer.getId())).thenReturn(Optional.of(customer));

            // Act
            RepresentativeViewDto view = legalEntityCustomerService.addRepresentative(customer.getId(),
                    linkedAddition(individual.getId()));

            // Assert
            assertThat(view.individualCustomerId()).isEqualTo(individual.getId());
            assertThat(view.firstName()).isEqualTo("Serafim");
            assertThat(view.middleName()).isEqualTo("Ivanov");
            assertThat(view.lastName()).isEqualTo("Gerasimoff");
        }

        @Test
        @DisplayName("Edge Case: Should allow linking a closed individual Customer")
        void shouldAllowLinkingClosedIndividual() {
            // Arrange
            LegalEntityCustomer customer = existingCustomer();
            IndividualCustomer individual = individualCustomer();
            individual.close();
            when(individualCustomerRepository.findById(individual.getId())).thenReturn(Optional.of(individual));
            when(legalEntityCustomerRepository.findById(customer.getId())).thenReturn(Optional.of(customer));

            // Act
            RepresentativeViewDto view = legalEntityCustomerService.addRepresentative(customer.getId(),
                    linkedAddition(individual.getId()));

            // Assert
            assertThat(view.individualCustomerId()).isEqualTo(individual.getId());
        }

        @Test
        @DisplayName("Error Case: Should reject names supplied together with a link before any lookup")
        void shouldRejectNamesWithLink() {
            // Act & Assert
            assertThatThrownBy(() -> legalEntityCustomerService.addRepresentative(UUID.randomUUID(),
                    new RepresentativeAdditionDto(UUID.randomUUID(), "Jane", null, null,
                            RepresentativeRole.LEGAL_REPRESENTATIVE, VALID_FROM))).isInstanceOfSatisfying(
                    DomainException.class, exception -> {
                        assertThat(exception.getErrorCode()).isEqualTo(CustomerErrorCode.INVALID_REPRESENTATIVE_NAMES);
                        assertThat(exception.getDetail()).isEqualTo(LegalEntityCustomerServiceImpl.NAMES_NOT_ALLOWED);
                    });
            verifyNoInteractions(individualCustomerRepository, legalEntityCustomerRepository);
        }

        @Test
        @DisplayName("Error Case: Should reject a middle name supplied together with a link")
        void shouldRejectMiddleNameWithLink() {
            // Act & Assert
            assertDomainError(() -> legalEntityCustomerService.addRepresentative(UUID.randomUUID(),
                            new RepresentativeAdditionDto(UUID.randomUUID(), null, "Ivanov", null,
                                    RepresentativeRole.LEGAL_REPRESENTATIVE, VALID_FROM)),
                    CustomerErrorCode.INVALID_REPRESENTATIVE_NAMES);
        }

        @Test
        @DisplayName("Error Case: Should reject a missing last name without a link")
        void shouldRejectMissingNamesWithoutLink() {
            // Act & Assert
            assertThatThrownBy(() -> legalEntityCustomerService.addRepresentative(UUID.randomUUID(),
                    new RepresentativeAdditionDto(null, "Jane", null, null, RepresentativeRole.LEGAL_REPRESENTATIVE,
                            VALID_FROM))).isInstanceOfSatisfying(DomainException.class, exception -> {
                assertThat(exception.getErrorCode()).isEqualTo(CustomerErrorCode.INVALID_REPRESENTATIVE_NAMES);
                assertThat(exception.getDetail()).isEqualTo(LegalEntityCustomerServiceImpl.NAMES_REQUIRED);
            });
            verifyNoInteractions(legalEntityCustomerRepository);
        }

        @Test
        @DisplayName("Edge Case: Should treat a blank first name as missing without a link")
        void shouldRejectBlankFirstNameWithoutLink() {
            // Act & Assert
            assertDomainError(() -> legalEntityCustomerService.addRepresentative(UUID.randomUUID(),
                    new RepresentativeAdditionDto(null, "   ", null, "Doe", RepresentativeRole.LEGAL_REPRESENTATIVE,
                            VALID_FROM)), CustomerErrorCode.INVALID_REPRESENTATIVE_NAMES);
        }

        @Test
        @DisplayName("Error Case: Should report a missing linked individual Customer")
        void shouldReportMissingLinkedCustomer() {
            // Arrange
            UUID unknownIndividualId = UUID.randomUUID();
            when(individualCustomerRepository.findById(unknownIndividualId)).thenReturn(Optional.empty());

            // Act & Assert
            assertDomainError(() -> legalEntityCustomerService.addRepresentative(UUID.randomUUID(),
                    linkedAddition(unknownIndividualId)), CustomerErrorCode.CUSTOMER_NOT_FOUND);
            verifyNoInteractions(legalEntityCustomerRepository);
        }

        @Test
        @DisplayName("Error Case: Should report a missing legal-entity Customer")
        void shouldReportMissingCompany() {
            // Arrange
            UUID unknownId = UUID.randomUUID();
            when(legalEntityCustomerRepository.findById(unknownId)).thenReturn(Optional.empty());

            // Act & Assert
            assertDomainError(() -> legalEntityCustomerService.addRepresentative(unknownId,
                    new RepresentativeAdditionDto(null, "Jane", null, "Doe", RepresentativeRole.LEGAL_REPRESENTATIVE,
                            VALID_FROM)), CustomerErrorCode.CUSTOMER_NOT_FOUND);
        }

        @Test
        @DisplayName("Error Case: Should pass the closed-customer error through unchanged")
        void shouldPassThroughClosedCompanyError() {
            // Arrange
            LegalEntityCustomer customer = existingCustomer();
            customer.close();
            when(legalEntityCustomerRepository.findById(customer.getId())).thenReturn(Optional.of(customer));

            // Act & Assert
            assertDomainError(() -> legalEntityCustomerService.addRepresentative(customer.getId(),
                    new RepresentativeAdditionDto(null, "Jane", null, "Doe", RepresentativeRole.LEGAL_REPRESENTATIVE,
                            VALID_FROM)), CustomerErrorCode.CUSTOMER_CLOSED);
        }
    }

    @Nested
    @DisplayName("End representation")
    class EndRepresentation {

        @Test
        @DisplayName("Happy Path: Should end a representation and return only that Representative")
        void shouldEndRepresentation() {
            // Arrange
            LegalEntityCustomer customer = existingCustomer();
            Representative representative = addUnlinkedRepresentative(customer);
            when(legalEntityCustomerRepository.findById(customer.getId())).thenReturn(Optional.of(customer));

            // Act
            RepresentativeViewDto view = legalEntityCustomerService.endRepresentation(customer.getId(),
                    representative.getId(), new RepresentationEndDto(VALID_TO));

            // Assert
            assertThat(view.id()).isEqualTo(representative.getId());
            assertThat(view.validTo()).isEqualTo(VALID_TO);
        }

        @Test
        @DisplayName("Error Case: Should pass the unknown-representative error through unchanged")
        void shouldPassThroughUnknownRepresentativeError() {
            // Arrange
            LegalEntityCustomer customer = existingCustomer();
            when(legalEntityCustomerRepository.findById(customer.getId())).thenReturn(Optional.of(customer));

            // Act & Assert
            assertDomainError(() -> legalEntityCustomerService.endRepresentation(customer.getId(), UUID.randomUUID(),
                    new RepresentationEndDto(VALID_TO)), CustomerErrorCode.REPRESENTATIVE_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("Correct representative")
    class CorrectRepresentative {

        @Test
        @DisplayName("Happy Path: Should correct every value and clear a mistaken end date")
        void shouldCorrectUnlinkedRepresentative() {
            // Arrange
            LegalEntityCustomer customer = existingCustomer();
            Representative representative = addUnlinkedRepresentative(customer);
            customer.endRepresentation(representative.getId(), VALID_TO);
            when(legalEntityCustomerRepository.findById(customer.getId())).thenReturn(Optional.of(customer));

            // Act
            RepresentativeViewDto view = legalEntityCustomerService.correctRepresentative(customer.getId(),
                    representative.getId(), new RepresentativeCorrectionDto(null, "Jane", null, "Doe",
                            RepresentativeRole.AUTHORIZED_REPRESENTATIVE, VALID_FROM.plusMonths(1), null));

            // Assert
            assertThat(view.firstName()).isEqualTo("Jane");
            assertThat(view.role()).isEqualTo(RepresentativeRole.AUTHORIZED_REPRESENTATIVE);
            assertThat(view.validFrom()).isEqualTo(VALID_FROM.plusMonths(1));
            assertThat(view.validTo()).isNull();
        }

        @Test
        @DisplayName("Happy Path: Should link an individual Customer and copy the names")
        void shouldCorrectToLinkedRepresentative() {
            // Arrange
            LegalEntityCustomer customer = existingCustomer();
            Representative representative = addUnlinkedRepresentative(customer);
            IndividualCustomer individual = individualCustomer();
            when(individualCustomerRepository.findById(individual.getId())).thenReturn(Optional.of(individual));
            when(legalEntityCustomerRepository.findById(customer.getId())).thenReturn(Optional.of(customer));

            // Act
            RepresentativeViewDto view = legalEntityCustomerService.correctRepresentative(customer.getId(),
                    representative.getId(), new RepresentativeCorrectionDto(individual.getId(), null, null, null,
                            RepresentativeRole.LEGAL_REPRESENTATIVE, VALID_FROM, null));

            // Assert
            assertThat(view.individualCustomerId()).isEqualTo(individual.getId());
            assertThat(view.firstName()).isEqualTo("Serafim");
            assertThat(view.lastName()).isEqualTo("Gerasimoff");
        }

        @Test
        @DisplayName("Error Case: Should reject names supplied together with a link")
        void shouldRejectNamesWithLink() {
            // Act & Assert
            assertDomainError(
                    () -> legalEntityCustomerService.correctRepresentative(UUID.randomUUID(), UUID.randomUUID(),
                            new RepresentativeCorrectionDto(UUID.randomUUID(), "Jane", null, "Doe",
                                    RepresentativeRole.LEGAL_REPRESENTATIVE, VALID_FROM, null)),
                    CustomerErrorCode.INVALID_REPRESENTATIVE_NAMES);
            verifyNoInteractions(individualCustomerRepository, legalEntityCustomerRepository);
        }

        @Test
        @DisplayName("Error Case: Should pass the period error through unchanged")
        void shouldPassThroughPeriodError() {
            // Arrange
            LegalEntityCustomer customer = existingCustomer();
            Representative representative = addUnlinkedRepresentative(customer);
            when(legalEntityCustomerRepository.findById(customer.getId())).thenReturn(Optional.of(customer));

            // Act & Assert
            assertDomainError(
                    () -> legalEntityCustomerService.correctRepresentative(customer.getId(), representative.getId(),
                            new RepresentativeCorrectionDto(null, "Jane", null, "Doe",
                                    RepresentativeRole.LEGAL_REPRESENTATIVE, VALID_TO, VALID_FROM)),
                    CustomerErrorCode.INVALID_REPRESENTATION_PERIOD);
        }
    }
}