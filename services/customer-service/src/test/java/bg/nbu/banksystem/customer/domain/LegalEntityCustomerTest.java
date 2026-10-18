package bg.nbu.banksystem.customer.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

@DisplayName("LegalEntityCustomer (unit)")
class LegalEntityCustomerTest {

    private static final String CUSTOMER_NUMBER = "CUS-000002";
    private static final String EMAIL = "contact@starter.example";
    private static final String PHONE = "+35929201234";
    private static final String COMPANY_NAME = "Starter Financial Services";
    private static final String EIK = "123456789";

    private static LegalEntityCustomer registerLegalEntityCustomer() {
        return LegalEntityCustomer.register(CUSTOMER_NUMBER, EMAIL, PHONE, COMPANY_NAME, EIK);
    }

    static Stream<Arguments> invalidRegistrationData() {
        return Stream.of(Arguments.of("customerNumber", null, EMAIL, PHONE, COMPANY_NAME, EIK), Arguments.of("email", CUSTOMER_NUMBER, "   ", PHONE, COMPANY_NAME, EIK), Arguments.of("phone", CUSTOMER_NUMBER, EMAIL, "", COMPANY_NAME, EIK), Arguments.of("companyName", CUSTOMER_NUMBER, EMAIL, PHONE, null, EIK), Arguments.of("eik", CUSTOMER_NUMBER, EMAIL, PHONE, COMPANY_NAME, "   "));
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
            // Act
            LegalEntityCustomer customer = registerLegalEntityCustomer();

            // Assert
            assertThat(customer.getId()).isNotNull();
            assertThat(customer.getId().version()).isEqualTo(7);
            assertThat(customer.getCustomerNumber()).isEqualTo(CUSTOMER_NUMBER);
            assertThat(customer.getCustomerType()).isEqualTo(CustomerType.LEGAL_ENTITY);
            assertThat(customer.getEmail()).isEqualTo(EMAIL);
            assertThat(customer.getPhone()).isEqualTo(PHONE);
            assertThat(customer.getCompanyName()).isEqualTo(COMPANY_NAME);
            assertThat(customer.getEik()).isEqualTo(EIK);
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
            // Act
            LegalEntityCustomer customer = LegalEntityCustomer.register("  CUS-000002  ", "  Contact@Starter.Example  ", "  +35929201234  ", "  Starter Financial Services  ", "  123456789  ");

            // Assert
            assertThat(customer.getCustomerNumber()).isEqualTo(CUSTOMER_NUMBER);
            assertThat(customer.getEmail()).isEqualTo(EMAIL);
            assertThat(customer.getPhone()).isEqualTo(PHONE);
            assertThat(customer.getCompanyName()).isEqualTo(COMPANY_NAME);
            assertThat(customer.getEik()).isEqualTo(EIK);
        }

        @ParameterizedTest(name = "Error Case: Should reject invalid {0}")
        @MethodSource("bg.nbu.banksystem.customer.domain." + "LegalEntityCustomerTest#invalidRegistrationData")
        @DisplayName("Error Case: Should reject missing mandatory text")
        void shouldRejectInvalidMandatoryText(String field, String customerNumber, String email, String phone, String companyName, String eik) {
            // Act & Assert
            assertThatIllegalArgumentException().isThrownBy(() -> LegalEntityCustomer.register(customerNumber, email, phone, companyName, eik)).withMessageContaining(field);
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
        @DisplayName("Error Case: Should reject a blank company name")
        void shouldRejectBlankCompanyName() {
            // Arrange
            LegalEntityCustomer customer = registerLegalEntityCustomer();

            // Act & Assert
            assertThatIllegalArgumentException().isThrownBy(() -> customer.updateCompanyName("   ")).withMessageContaining("companyName");
        }

        @ParameterizedTest(name = "Error Case: Should reject invalid {0}")
        @MethodSource("bg.nbu.banksystem.customer.domain." + "LegalEntityCustomerTest#invalidContactUpdateData")
        @DisplayName("Error Case: Should reject invalid contact information")
        void shouldRejectInvalidContactUpdate(String field, String email, String phone) {
            // Arrange
            LegalEntityCustomer customer = registerLegalEntityCustomer();

            // Act & Assert
            assertThatIllegalArgumentException().isThrownBy(() -> customer.updateContactInformation(email, phone)).withMessageContaining(field);
        }
    }
}