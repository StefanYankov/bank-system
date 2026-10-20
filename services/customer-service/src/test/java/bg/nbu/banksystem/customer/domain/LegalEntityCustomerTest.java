package bg.nbu.banksystem.customer.domain;

import bg.nbu.banksystem.starter.error.DomainException;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("LegalEntityCustomer (unit)")
class LegalEntityCustomerTest {

    private static final String CUSTOMER_NUMBER = "CUS-000002";
    private static final String EMAIL = "contact@starter.example";
    private static final String PHONE = "+35929201234";
    private static final String COMPANY_NAME = "Starter Financial Services";
    private static final String EIK = "123456786";
    private static final String CORRECTED_EIK = "000000305";

    private static void assertDomainError(ThrowingCallable action, CustomerErrorCode expectedCode) {
        assertThatThrownBy(action)
                .isInstanceOfSatisfying(DomainException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(expectedCode));
    }

    private static LegalEntityCustomer registerCustomer() {
        return LegalEntityCustomer.register(CUSTOMER_NUMBER, EMAIL, PHONE, COMPANY_NAME, EIK,
                registeredAddress(),
                correspondenceAddress());
    }

    private static LegalEntityCustomer closedCustomer() {
        LegalEntityCustomer customer = registerCustomer();
        customer.close();

        return customer;
    }

    private static Address registeredAddress() {
        return Address.of("BG", "Sofia", "1000", "1 Alexander Battenberg Square", null);
    }

    private static Address correspondenceAddress() {
        return Address.of("BG", "Sofia", "1000", "10 Business Park Street", "Office 4");
    }

    static Stream<Arguments> invalidRegistrationData() {
        return Stream.of(
                Arguments.of("customerNumber", null, EMAIL, PHONE, COMPANY_NAME, EIK),
                Arguments.of("email", CUSTOMER_NUMBER, "   ", PHONE, COMPANY_NAME, EIK),
                Arguments.of("phone", CUSTOMER_NUMBER, EMAIL, "", COMPANY_NAME, EIK),
                Arguments.of("companyName", CUSTOMER_NUMBER, EMAIL, PHONE, null, EIK),
                Arguments.of("eik", CUSTOMER_NUMBER, EMAIL, PHONE, COMPANY_NAME, "   "));
    }

    @Nested
    @DisplayName("Registration")
    class Registration {

        @Test
        @DisplayName("Happy Path: Should register an active legal-entity Customer with an assigned UUIDv7")
        void shouldRegisterLegalEntityCustomer() {
            // Arrange
            Address registeredAddress = registeredAddress();
            Address correspondenceAddress = correspondenceAddress();

            // Act
            LegalEntityCustomer customer = LegalEntityCustomer.register(CUSTOMER_NUMBER, EMAIL, PHONE, COMPANY_NAME,
                    EIK, registeredAddress, correspondenceAddress);

            // Assert
            assertThat(customer.getId()).isNotNull();
            assertThat(customer.getId().version()).isEqualTo(7);
            assertThat(customer.getCustomerNumber()).isEqualTo(CUSTOMER_NUMBER);
            assertThat(customer.getCustomerType()).isEqualTo(CustomerType.LEGAL_ENTITY);
            assertThat(customer.getStatus()).isEqualTo(CustomerStatus.ACTIVE);
            assertThat(customer.getEmail()).isEqualTo(EMAIL);
            assertThat(customer.getPhone()).isEqualTo(PHONE);
            assertThat(customer.getCompanyName()).isEqualTo(COMPANY_NAME);
            assertThat(customer.getEik()).isEqualTo(EIK);
            assertThat(customer.getRegisteredAddress()).isSameAs(registeredAddress);
            assertThat(customer.getCorrespondenceAddress()).isSameAs(correspondenceAddress);
            assertThat(customer.getRepresentatives()).isEmpty();
        }

        @Test
        @DisplayName("Happy Path: Should assign a different UUID to every legal-entity Customer")
        void shouldAssignDifferentUuidToEveryCustomer() {
            // Act
            LegalEntityCustomer firstCustomer = registerCustomer();
            LegalEntityCustomer secondCustomer = registerCustomer();

            // Assert
            assertThat(firstCustomer.getId()).isNotEqualTo(secondCustomer.getId());
        }

        @Test
        @DisplayName("Edge Case: Should trim text and normalize the email")
        void shouldNormalizeRegistrationData() {
            // Act
            LegalEntityCustomer customer = LegalEntityCustomer.register("  CUS-000002  ",
                    "  Contact@Starter.Example  ", "  +35929201234  ", "  Starter Financial Services  ",
                    "  123456786  ",
                    registeredAddress(), correspondenceAddress());

            // Assert
            assertThat(customer.getCustomerNumber()).isEqualTo(CUSTOMER_NUMBER);
            assertThat(customer.getEmail()).isEqualTo(EMAIL);
            assertThat(customer.getPhone()).isEqualTo(PHONE);
            assertThat(customer.getCompanyName()).isEqualTo(COMPANY_NAME);
            assertThat(customer.getEik()).isEqualTo(EIK);
        }

        @ParameterizedTest(name = "Error Case: Should reject invalid {0}")
        @MethodSource("bg.nbu.banksystem.customer.domain.LegalEntityCustomerTest#invalidRegistrationData")
        @DisplayName("Error Case: Should reject missing mandatory text")
        void shouldRejectInvalidMandatoryText(String field, @Nullable String customerNumber, @Nullable String email,
                                              @Nullable String phone, @Nullable String companyName,
                                              @Nullable String eik) {
            // Act & Assert
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> LegalEntityCustomer.register(customerNumber, email, phone, companyName, eik,
                            registeredAddress(), correspondenceAddress()))
                    .withMessageContaining(field);
        }

        @Test
        @DisplayName("Error Case: Should reject an EIK with a wrong check digit")
        void shouldRejectInvalidEik() {
            // Act & Assert
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> LegalEntityCustomer.register(CUSTOMER_NUMBER, EMAIL, PHONE, COMPANY_NAME,
                            "123456789", registeredAddress(), correspondenceAddress()))
                    .withMessage("eik has an invalid check digit");
        }

        @Test
        @DisplayName("Error Case: Should reject a missing registered address")
        void shouldRejectMissingRegisteredAddress() {
            // Act & Assert
            assertThatNullPointerException()
                    .isThrownBy(() -> LegalEntityCustomer.register(CUSTOMER_NUMBER, EMAIL, PHONE, COMPANY_NAME, EIK,
                            null, correspondenceAddress()))
                    .withMessage("registeredAddress must not be null");
        }

        @Test
        @DisplayName("Error Case: Should reject a missing correspondence address")
        void shouldRejectMissingCorrespondenceAddress() {
            // Act & Assert
            assertThatNullPointerException()
                    .isThrownBy(() -> LegalEntityCustomer.register(CUSTOMER_NUMBER, EMAIL, PHONE, COMPANY_NAME, EIK,
                            registeredAddress(), null))
                    .withMessage("correspondenceAddress must not be null");
        }
    }

    @Nested
    @DisplayName("Profile")
    class Profile {

        @Test
        @DisplayName("Happy Path: Should update and normalize the company name")
        void shouldUpdateCompanyName() {
            // Arrange
            LegalEntityCustomer customer = registerCustomer();

            // Act
            customer.updateCompanyName("  Starter Holdings Ltd  ");

            // Assert
            assertThat(customer.getCompanyName()).isEqualTo("Starter Holdings Ltd");
        }

        @Test
        @DisplayName("Happy Path: Should update and normalize contact information")
        void shouldUpdateContactInformation() {
            // Arrange
            LegalEntityCustomer customer = registerCustomer();

            // Act
            customer.updateContactInformation("  Office@Starter.Example  ", "  +35929204321  ");

            // Assert
            assertThat(customer.getEmail()).isEqualTo("office@starter.example");
            assertThat(customer.getPhone()).isEqualTo("+35929204321");
        }

        @Test
        @DisplayName("Happy Path: Should replace the registered address")
        void shouldChangeRegisteredAddress() {
            // Arrange
            LegalEntityCustomer customer = registerCustomer();
            Address newAddress = Address.of("BG", "Plovdiv", "4000", "1 Central Square", null);

            // Act
            customer.changeRegisteredAddress(newAddress);

            // Assert
            assertThat(customer.getRegisteredAddress()).isSameAs(newAddress);
        }

        @Test
        @DisplayName("Happy Path: Should replace the correspondence address")
        void shouldChangeCorrespondenceAddress() {
            // Arrange
            LegalEntityCustomer customer = registerCustomer();
            Address newAddress = Address.of("BG", "Varna", "9000", "1 Independence Square", "Office 2");

            // Act
            customer.changeCorrespondenceAddress(newAddress);

            // Assert
            assertThat(customer.getCorrespondenceAddress()).isSameAs(newAddress);
        }

        @Test
        @DisplayName("Happy Path: Should correct a wrongly entered EIK")
        void shouldCorrectEik() {
            // Arrange
            LegalEntityCustomer customer = registerCustomer();

            // Act
            customer.correctEik("  000000305  ");

            // Assert
            assertThat(customer.getEik()).isEqualTo(CORRECTED_EIK);
        }

        @Test
        @DisplayName("Error Case: Should reject an invalid EIK correction and keep the current EIK")
        void shouldRejectInvalidEikCorrection() {
            // Arrange
            LegalEntityCustomer customer = registerCustomer();

            // Act & Assert
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> customer.correctEik("1234567860000"))
                    .withMessage("eik must contain exactly 9 digits");

            assertThat(customer.getEik()).isEqualTo(EIK);
        }

        @Test
        @DisplayName("Error Case: Should reject a blank company name")
        void shouldRejectBlankCompanyName() {
            // Arrange
            LegalEntityCustomer customer = registerCustomer();

            // Act & Assert
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> customer.updateCompanyName("   "))
                    .withMessageContaining("companyName");
        }

        @Test
        @DisplayName("Error Case: Should reject missing address updates")
        void shouldRejectMissingAddressUpdates() {
            // Arrange
            LegalEntityCustomer customer = registerCustomer();

            // Act & Assert
            assertThatNullPointerException()
                    .isThrownBy(() -> customer.changeRegisteredAddress(null))
                    .withMessage("registeredAddress must not be null");

            assertThatNullPointerException()
                    .isThrownBy(() -> customer.changeCorrespondenceAddress(null))
                    .withMessage("correspondenceAddress must not be null");
        }
    }

    @Nested
    @DisplayName("Closed Customer")
    class ClosedCustomer {

        @Test
        @DisplayName("Error Case: Should reject a company name update on a closed Customer")
        void shouldRejectCompanyNameUpdate() {
            // Arrange
            LegalEntityCustomer customer = closedCustomer();

            // Act & Assert
            assertDomainError(() -> customer.updateCompanyName("Starter Holdings Ltd"),
                    CustomerErrorCode.CUSTOMER_CLOSED);
        }

        @Test
        @DisplayName("Error Case: Should reject a contact update on a closed Customer")
        void shouldRejectContactUpdate() {
            // Arrange
            LegalEntityCustomer customer = closedCustomer();

            // Act & Assert
            assertDomainError(() -> customer.updateContactInformation("office@starter.example", "+35929204321"),
                    CustomerErrorCode.CUSTOMER_CLOSED);
        }

        @Test
        @DisplayName("Error Case: Should reject an EIK correction on a closed Customer")
        void shouldRejectEikCorrection() {
            // Arrange
            LegalEntityCustomer customer = closedCustomer();

            // Act & Assert
            assertDomainError(() -> customer.correctEik(CORRECTED_EIK), CustomerErrorCode.CUSTOMER_CLOSED);
        }

        @Test
        @DisplayName("Error Case: Should reject address changes on a closed Customer")
        void shouldRejectAddressChanges() {
            // Arrange
            LegalEntityCustomer customer = closedCustomer();
            Address newAddress = Address.of("BG", "Plovdiv", "4000", "1 Central Square", null);

            // Act & Assert
            assertDomainError(() -> customer.changeRegisteredAddress(newAddress), CustomerErrorCode.CUSTOMER_CLOSED);
            assertDomainError(() -> customer.changeCorrespondenceAddress(newAddress),
                    CustomerErrorCode.CUSTOMER_CLOSED);
        }
    }
}