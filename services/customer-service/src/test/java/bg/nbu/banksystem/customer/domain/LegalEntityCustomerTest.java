package bg.nbu.banksystem.customer.domain;

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

@DisplayName("LegalEntityCustomer (unit)")
class LegalEntityCustomerTest {

    private static final String CUSTOMER_NUMBER = "CUS-000002";
    private static final String EMAIL = "contact@starter.example";
    private static final String PHONE = "+35929201234";
    private static final String COMPANY_NAME = "Starter Financial Services";
    private static final String EIK = "123456789";

    private static LegalEntityCustomer registerLegalEntityCustomer() {
        return LegalEntityCustomer.register(CUSTOMER_NUMBER, EMAIL, PHONE, COMPANY_NAME, EIK,
                registeredAddressFixture(), correspondenceAddressFixture());
    }

    private static Address registeredAddressFixture() {
        return Address.of("BG", "Sofia", "1000", "1 Alexander Battenberg Square", null);
    }

    private static Address correspondenceAddressFixture() {
        return Address.of("BG", "Sofia", "1000", "10 Business Park Street", "Office 4");
    }

    static Stream<Arguments> invalidRegistrationData() {
        return Stream.of(Arguments.of("customerNumber", null, EMAIL, PHONE, COMPANY_NAME, EIK),
                Arguments.of("email", CUSTOMER_NUMBER, "   ", PHONE, COMPANY_NAME, EIK),
                Arguments.of("phone", CUSTOMER_NUMBER, EMAIL, "", COMPANY_NAME, EIK),
                Arguments.of("companyName", CUSTOMER_NUMBER, EMAIL, PHONE, null, EIK),
                Arguments.of("eik", CUSTOMER_NUMBER, EMAIL, PHONE, COMPANY_NAME, "   "));
    }

    static Stream<Arguments> invalidContactUpdateData() {
        return Stream.of(Arguments.of("email", null, PHONE), Arguments.of("phone", EMAIL, "   "));
    }

    @Nested
    @DisplayName("Registration")
    class Registration {

        @Test
        @DisplayName("Happy Path: Should register a legal-entity Customer with an assigned UUIDv7")
        void shouldRegisterLegalEntityCustomer() {
            // Arrange
            Address registeredAddress = registeredAddressFixture();

            Address correspondenceAddress = correspondenceAddressFixture();

            // Act
            LegalEntityCustomer customer = LegalEntityCustomer.register(CUSTOMER_NUMBER, EMAIL, PHONE, COMPANY_NAME,
                    EIK, registeredAddress, correspondenceAddress);

            // Assert
            assertThat(customer.getId()).isNotNull();
            assertThat(customer.getId().version()).isEqualTo(7);
            assertThat(customer.getCustomerNumber()).isEqualTo(CUSTOMER_NUMBER);
            assertThat(customer.getCustomerType()).isEqualTo(CustomerType.LEGAL_ENTITY);
            assertThat(customer.getEmail()).isEqualTo(EMAIL);
            assertThat(customer.getPhone()).isEqualTo(PHONE);
            assertThat(customer.getCompanyName()).isEqualTo(COMPANY_NAME);
            assertThat(customer.getEik()).isEqualTo(EIK);
            assertThat(customer.getRegisteredAddress()).isSameAs(registeredAddress);
            assertThat(customer.getCorrespondenceAddress()).isSameAs(correspondenceAddress);
        }

        @Test
        @DisplayName("Happy Path: Should assign a different UUID to every legal-entity Customer")
        void shouldAssignDifferentUuidToEveryCustomer() {
            // Act
            LegalEntityCustomer firstCustomer = registerLegalEntityCustomer();

            LegalEntityCustomer secondCustomer = registerLegalEntityCustomer();

            // Assert
            assertThat(firstCustomer.getId()).isNotEqualTo(secondCustomer.getId());
        }

        @Test
        @DisplayName("Edge Case: Should trim text and normalize the email")
        void shouldNormalizeRegistrationData() {
            // Arrange
            Address registeredAddress = registeredAddressFixture();

            Address correspondenceAddress = correspondenceAddressFixture();

            // Act
            LegalEntityCustomer customer = LegalEntityCustomer.register("  CUS-000002  ", "  Contact@Starter.Example  ",
                    "  +35929201234  ", "  Starter Financial Services  ", "  123456789  ", registeredAddress,
                    correspondenceAddress);

            // Assert
            assertThat(customer.getCustomerNumber()).isEqualTo(CUSTOMER_NUMBER);
            assertThat(customer.getEmail()).isEqualTo(EMAIL);
            assertThat(customer.getPhone()).isEqualTo(PHONE);
            assertThat(customer.getCompanyName()).isEqualTo(COMPANY_NAME);
            assertThat(customer.getEik()).isEqualTo(EIK);
            assertThat(customer.getRegisteredAddress()).isSameAs(registeredAddress);
            assertThat(customer.getCorrespondenceAddress()).isSameAs(correspondenceAddress);
        }

        @ParameterizedTest(name = "Error Case: Should reject invalid {0}")
        @MethodSource("bg.nbu.banksystem.customer.domain." + "LegalEntityCustomerTest#invalidRegistrationData")
        @DisplayName("Error Case: Should reject missing mandatory text")
        void shouldRejectInvalidMandatoryText(String field, @Nullable String customerNumber, @Nullable String email,
                                              @Nullable String phone, @Nullable String companyName,
                                              @Nullable String eik) {
            // Act & Assert
            assertThatIllegalArgumentException().isThrownBy(
                    () -> LegalEntityCustomer.register(customerNumber, email, phone, companyName, eik,
                            registeredAddressFixture(), correspondenceAddressFixture())).withMessageContaining(field);
        }

        @Test
        @DisplayName("Error Case: Should reject a missing registered address")
        void shouldRejectMissingRegisteredAddress() {
            // Act & Assert
            assertThatNullPointerException().isThrownBy(
                    () -> LegalEntityCustomer.register(CUSTOMER_NUMBER, EMAIL, PHONE, COMPANY_NAME, EIK, null,
                            correspondenceAddressFixture())).withMessage("registeredAddress must not be null");
        }

        @Test
        @DisplayName("Error Case: Should reject a missing correspondence address")
        void shouldRejectMissingCorrespondenceAddress() {
            // Act & Assert
            assertThatNullPointerException().isThrownBy(
                    () -> LegalEntityCustomer.register(CUSTOMER_NUMBER, EMAIL, PHONE, COMPANY_NAME, EIK,
                            registeredAddressFixture(), null)).withMessage("correspondenceAddress must not be null");
        }
    }

    @Nested
    @DisplayName("Profile")
    class Profile {

        @Test
        @DisplayName("Happy Path: Should update and normalize the company name")
        void shouldUpdateCompanyName() {
            // Arrange
            LegalEntityCustomer customer = registerLegalEntityCustomer();

            // Act
            customer.updateCompanyName("  Updated Financial Services  ");

            // Assert
            assertThat(customer.getCompanyName()).isEqualTo("Updated Financial Services");
        }

        @Test
        @DisplayName("Happy Path: Should update and normalize contact information")
        void shouldUpdateContactInformation() {
            // Arrange
            LegalEntityCustomer customer = registerLegalEntityCustomer();

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
            LegalEntityCustomer customer = registerLegalEntityCustomer();

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
            LegalEntityCustomer customer = registerLegalEntityCustomer();

            Address newAddress = Address.of("BG", "Varna", "9000", "1 Independence Square", "Office 2");

            // Act
            customer.changeCorrespondenceAddress(newAddress);

            // Assert
            assertThat(customer.getCorrespondenceAddress()).isSameAs(newAddress);
        }

        @Test
        @DisplayName("Error Case: Should reject a blank company name")
        void shouldRejectBlankCompanyName() {
            // Arrange
            LegalEntityCustomer customer = registerLegalEntityCustomer();

            // Act & Assert
            assertThatIllegalArgumentException().isThrownBy(() -> customer.updateCompanyName("   "))
                    .withMessageContaining("companyName");
        }

        @Test
        @DisplayName("Error Case: Should reject a missing registered address update")
        void shouldRejectMissingRegisteredAddressUpdate() {
            // Arrange
            LegalEntityCustomer customer = registerLegalEntityCustomer();

            // Act & Assert
            assertThatNullPointerException().isThrownBy(() -> customer.changeRegisteredAddress(null))
                    .withMessage("registeredAddress must not be null");
        }

        @Test
        @DisplayName("Error Case: Should reject a missing correspondence address update")
        void shouldRejectMissingCorrespondenceAddressUpdate() {
            // Arrange
            LegalEntityCustomer customer = registerLegalEntityCustomer();

            // Act & Assert
            assertThatNullPointerException().isThrownBy(() -> customer.changeCorrespondenceAddress(null))
                    .withMessage("correspondenceAddress must not be null");
        }

        @ParameterizedTest(name = "Error Case: Should reject invalid {0}")
        @MethodSource("bg.nbu.banksystem.customer.domain." + "LegalEntityCustomerTest#invalidContactUpdateData")
        @DisplayName("Error Case: Should reject invalid contact information")
        void shouldRejectInvalidContactUpdate(String field, @Nullable String email, @Nullable String phone) {
            // Arrange
            LegalEntityCustomer customer = registerLegalEntityCustomer();

            // Act & Assert
            assertThatIllegalArgumentException().isThrownBy(() -> customer.updateContactInformation(email, phone))
                    .withMessageContaining(field);
        }
    }
}