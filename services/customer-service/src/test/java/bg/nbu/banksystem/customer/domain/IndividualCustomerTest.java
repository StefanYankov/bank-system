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

@DisplayName("IndividualCustomer (unit)")
class IndividualCustomerTest {

    private static final String CUSTOMER_NUMBER = "CUS-000001";
    private static final String EMAIL = "serafim.gerasimoff@example.com";
    private static final String PHONE = "+359888123456";
    private static final String FIRST_NAME = "Serafim";
    private static final String MIDDLE_NAME = "Ivanov";
    private static final String LAST_NAME = "Gerasimoff";
    private static final String EGN = "9001010000";

    private static IndividualCustomer registerIndividualCustomer() {
        return IndividualCustomer.register(CUSTOMER_NUMBER, EMAIL, PHONE, FIRST_NAME, MIDDLE_NAME, LAST_NAME, EGN,
                residentialAddressFixture());
    }

    private static Address residentialAddressFixture() {
        return Address.of("BG", "Sofia", "1000", "1 Alexander Battenberg Square", null);
    }

    static Stream<Arguments> invalidRegistrationData() {
        return Stream.of(Arguments.of("customerNumber", null, EMAIL, PHONE, FIRST_NAME, LAST_NAME, EGN),
                Arguments.of("email", CUSTOMER_NUMBER, "   ", PHONE, FIRST_NAME, LAST_NAME, EGN),
                Arguments.of("phone", CUSTOMER_NUMBER, EMAIL, "", FIRST_NAME, LAST_NAME, EGN),
                Arguments.of("firstName", CUSTOMER_NUMBER, EMAIL, PHONE, null, LAST_NAME, EGN),
                Arguments.of("lastName", CUSTOMER_NUMBER, EMAIL, PHONE, FIRST_NAME, "   ", EGN),
                Arguments.of("egn", CUSTOMER_NUMBER, EMAIL, PHONE, FIRST_NAME, LAST_NAME, ""));
    }

    static Stream<Arguments> invalidNameUpdateData() {
        return Stream.of(Arguments.of("firstName", null, LAST_NAME), Arguments.of("lastName", FIRST_NAME, "   "));
    }

    static Stream<Arguments> invalidContactUpdateData() {
        return Stream.of(Arguments.of("email", null, PHONE), Arguments.of("phone", EMAIL, "   "));
    }

    @Nested
    @DisplayName("Registration")
    class Registration {

        @Test
        @DisplayName("Happy Path: Should register an individual Customer with an assigned UUIDv7")
        void shouldRegisterIndividualCustomer() {
            // Arrange
            Address residentialAddress = residentialAddressFixture();

            // Act
            IndividualCustomer customer = IndividualCustomer.register(CUSTOMER_NUMBER, EMAIL, PHONE, FIRST_NAME,
                    MIDDLE_NAME, LAST_NAME, EGN, residentialAddress);

            // Assert
            assertThat(customer.getId()).isNotNull();
            assertThat(customer.getId().version()).isEqualTo(7);
            assertThat(customer.getCustomerNumber()).isEqualTo(CUSTOMER_NUMBER);
            assertThat(customer.getCustomerType()).isEqualTo(CustomerType.INDIVIDUAL);
            assertThat(customer.getEmail()).isEqualTo(EMAIL);
            assertThat(customer.getPhone()).isEqualTo(PHONE);
            assertThat(customer.getFirstName()).isEqualTo(FIRST_NAME);
            assertThat(customer.getMiddleName()).isEqualTo(MIDDLE_NAME);
            assertThat(customer.getLastName()).isEqualTo(LAST_NAME);
            assertThat(customer.getEgn()).isEqualTo(EGN);
            assertThat(customer.getResidentialAddress()).isSameAs(residentialAddress);
        }

        @Test
        @DisplayName("Happy Path: Should assign a different UUID to every individual Customer")
        void shouldAssignDifferentUuidToEveryCustomer() {
            // Act
            IndividualCustomer firstCustomer = registerIndividualCustomer();

            IndividualCustomer secondCustomer = registerIndividualCustomer();

            // Assert
            assertThat(firstCustomer.getId()).isNotEqualTo(secondCustomer.getId());
        }

        @Test
        @DisplayName("Edge Case: Should trim text and normalize the email")
        void shouldNormalizeRegistrationData() {
            // Arrange
            Address residentialAddress = residentialAddressFixture();

            // Act
            IndividualCustomer customer = IndividualCustomer.register("  CUS-000001  ",
                    "  Serafim.Gerasimoff@Example.COM  ", "  +359888123456  ", "  Serafim  ", "  Ivanov  ",
                    "  Gerasimoff  ", "  9001010000  ", residentialAddress);

            // Assert
            assertThat(customer.getCustomerNumber()).isEqualTo(CUSTOMER_NUMBER);
            assertThat(customer.getEmail()).isEqualTo(EMAIL);
            assertThat(customer.getPhone()).isEqualTo(PHONE);
            assertThat(customer.getFirstName()).isEqualTo(FIRST_NAME);
            assertThat(customer.getMiddleName()).isEqualTo(MIDDLE_NAME);
            assertThat(customer.getLastName()).isEqualTo(LAST_NAME);
            assertThat(customer.getEgn()).isEqualTo(EGN);
            assertThat(customer.getResidentialAddress()).isSameAs(residentialAddress);
        }

        @Test
        @DisplayName("Edge Case: Should store a blank middle name as null")
        void shouldNormalizeBlankMiddleNameToNull() {
            // Act
            IndividualCustomer customer = IndividualCustomer.register(CUSTOMER_NUMBER, EMAIL, PHONE, FIRST_NAME, "   ",
                    LAST_NAME, EGN, residentialAddressFixture());

            // Assert
            assertThat(customer.getMiddleName()).isNull();
        }

        @Test
        @DisplayName("Edge Case: Should allow a missing middle name")
        void shouldAllowMissingMiddleName() {
            // Act
            IndividualCustomer customer = IndividualCustomer.register(CUSTOMER_NUMBER, EMAIL, PHONE, FIRST_NAME, null,
                    LAST_NAME, EGN, residentialAddressFixture());

            // Assert
            assertThat(customer.getMiddleName()).isNull();
        }

        @ParameterizedTest(name = "Error Case: Should reject invalid {0}")
        @MethodSource("bg.nbu.banksystem.customer.domain." + "IndividualCustomerTest#invalidRegistrationData")
        @DisplayName("Error Case: Should reject missing mandatory text")
        void shouldRejectInvalidMandatoryText(String field, @Nullable String customerNumber, @Nullable String email,
                                              @Nullable String phone, @Nullable String firstName,
                                              @Nullable String lastName, @Nullable String egn) {
            // Act & Assert
            assertThatIllegalArgumentException().isThrownBy(
                    () -> IndividualCustomer.register(customerNumber, email, phone, firstName, MIDDLE_NAME, lastName,
                            egn, residentialAddressFixture())).withMessageContaining(field);
        }

        @Test
        @DisplayName("Error Case: Should reject a missing residential address")
        void shouldRejectMissingResidentialAddress() {
            // Act & Assert
            assertThatNullPointerException().isThrownBy(
                    () -> IndividualCustomer.register(CUSTOMER_NUMBER, EMAIL, PHONE, FIRST_NAME, MIDDLE_NAME, LAST_NAME,
                            EGN, null)).withMessage("residentialAddress must not be null");
        }
    }

    @Nested
    @DisplayName("Profile")
    class Profile {

        @Test
        @DisplayName("Happy Path: Should update and normalize the Customer name")
        void shouldUpdateName() {
            // Arrange
            IndividualCustomer customer = registerIndividualCustomer();

            // Act
            customer.updateName("  Updated  ", "   ", "  Customer  ");

            // Assert
            assertThat(customer.getFirstName()).isEqualTo("Updated");
            assertThat(customer.getMiddleName()).isNull();
            assertThat(customer.getLastName()).isEqualTo("Customer");
        }

        @Test
        @DisplayName("Happy Path: Should update and normalize contact information")
        void shouldUpdateContactInformation() {
            // Arrange
            IndividualCustomer customer = registerIndividualCustomer();

            // Act
            customer.updateContactInformation("  Updated.Customer@Example.COM  ", "  +359888654321  ");

            // Assert
            assertThat(customer.getEmail()).isEqualTo("updated.customer@example.com");
            assertThat(customer.getPhone()).isEqualTo("+359888654321");
        }

        @Test
        @DisplayName("Happy Path: Should replace the residential address")
        void shouldChangeResidentialAddress() {
            // Arrange
            IndividualCustomer customer = registerIndividualCustomer();

            Address newAddress = Address.of("BG", "Plovdiv", "4000", "1 Central Square", null);

            // Act
            customer.changeResidentialAddress(newAddress);

            // Assert
            assertThat(customer.getResidentialAddress()).isSameAs(newAddress);
        }

        @Test
        @DisplayName("Error Case: Should reject a missing residential address update")
        void shouldRejectMissingResidentialAddressUpdate() {
            // Arrange
            IndividualCustomer customer = registerIndividualCustomer();

            // Act & Assert
            assertThatNullPointerException().isThrownBy(() -> customer.changeResidentialAddress(null))
                    .withMessage("residentialAddress must not be null");
        }

        @ParameterizedTest(name = "Error Case: Should reject invalid {0}")
        @MethodSource("bg.nbu.banksystem.customer.domain." + "IndividualCustomerTest#invalidNameUpdateData")
        @DisplayName("Error Case: Should reject invalid mandatory names")
        void shouldRejectInvalidNameUpdate(String field, @Nullable String firstName, @Nullable String lastName) {
            // Arrange
            IndividualCustomer customer = registerIndividualCustomer();

            // Act & Assert
            assertThatIllegalArgumentException().isThrownBy(() -> customer.updateName(firstName, MIDDLE_NAME, lastName))
                    .withMessageContaining(field);
        }

        @ParameterizedTest(name = "Error Case: Should reject invalid {0}")
        @MethodSource("bg.nbu.banksystem.customer.domain." + "IndividualCustomerTest#invalidContactUpdateData")
        @DisplayName("Error Case: Should reject invalid contact information")
        void shouldRejectInvalidContactUpdate(String field, @Nullable String email, @Nullable String phone) {
            // Arrange
            IndividualCustomer customer = registerIndividualCustomer();

            // Act & Assert
            assertThatIllegalArgumentException().isThrownBy(() -> customer.updateContactInformation(email, phone))
                    .withMessageContaining(field);
        }
    }
}