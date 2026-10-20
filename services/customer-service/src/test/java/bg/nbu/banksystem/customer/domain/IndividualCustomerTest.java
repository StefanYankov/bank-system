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

import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("IndividualCustomer (unit)")
class IndividualCustomerTest {

    private static final String CUSTOMER_NUMBER = "CUS-000001";
    private static final String EMAIL = "serafim.gerasimoff@example.com";
    private static final String PHONE = "+359888123456";
    private static final String FIRST_NAME = "Serafim";
    private static final String MIDDLE_NAME = "Ivanov";
    private static final String LAST_NAME = "Gerasimoff";
    private static final String EGN = "6101047500";
    private static final String CORRECTED_EGN = "0041010002";

    private static void assertDomainError(ThrowingCallable action, CustomerErrorCode expectedCode) {
        assertThatThrownBy(action).isInstanceOfSatisfying(DomainException.class,
                exception -> assertThat(exception.getErrorCode()).isEqualTo(expectedCode));
    }

    private static IndividualCustomer registerCustomer() {
        return IndividualCustomer.register(CUSTOMER_NUMBER, EMAIL, PHONE, FIRST_NAME, MIDDLE_NAME, LAST_NAME, EGN,
                registeredAddress());
    }

    private static IndividualCustomer closedCustomer() {
        IndividualCustomer customer = registerCustomer();
        customer.close();

        return customer;
    }

    private static Address registeredAddress() {
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
        @DisplayName("Happy Path: Should register an active individual Customer with an assigned UUIDv7")
        void shouldRegisterIndividualCustomer() {
            // Arrange
            Address residentialAddress = registeredAddress();

            // Act
            IndividualCustomer customer = IndividualCustomer.register(CUSTOMER_NUMBER, EMAIL, PHONE, FIRST_NAME,
                    MIDDLE_NAME, LAST_NAME, EGN, residentialAddress);

            // Assert
            assertThat(customer.getId()).isNotNull();
            assertThat(customer.getId().version()).isEqualTo(7);
            assertThat(customer.getCustomerNumber()).isEqualTo(CUSTOMER_NUMBER);
            assertThat(customer.getCustomerType()).isEqualTo(CustomerType.INDIVIDUAL);
            assertThat(customer.getStatus()).isEqualTo(CustomerStatus.ACTIVE);
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
            IndividualCustomer firstCustomer = registerCustomer();
            IndividualCustomer secondCustomer = registerCustomer();

            // Assert
            assertThat(firstCustomer.getId()).isNotEqualTo(secondCustomer.getId());
        }

        @Test
        @DisplayName("Edge Case: Should trim text and normalize the email")
        void shouldNormalizeRegistrationData() {
            // Act
            IndividualCustomer customer = IndividualCustomer.register("  CUS-000001  ",
                    "  Serafim.Gerasimoff@Example.COM  ", "  +359888123456  ", "  Serafim  ", "  Ivanov  ",
                    "  Gerasimoff  ", "  6101047500  ", registeredAddress());

            // Assert
            assertThat(customer.getCustomerNumber()).isEqualTo(CUSTOMER_NUMBER);
            assertThat(customer.getEmail()).isEqualTo(EMAIL);
            assertThat(customer.getPhone()).isEqualTo(PHONE);
            assertThat(customer.getFirstName()).isEqualTo(FIRST_NAME);
            assertThat(customer.getMiddleName()).isEqualTo(MIDDLE_NAME);
            assertThat(customer.getLastName()).isEqualTo(LAST_NAME);
            assertThat(customer.getEgn()).isEqualTo(EGN);
        }

        @Test
        @DisplayName("Edge Case: Should store a blank middle name as null")
        void shouldNormalizeBlankMiddleNameToNull() {
            // Act
            IndividualCustomer customer = IndividualCustomer.register(CUSTOMER_NUMBER, EMAIL, PHONE, FIRST_NAME, "   ",
                    LAST_NAME, EGN, registeredAddress());

            // Assert
            assertThat(customer.getMiddleName()).isNull();
        }

        @Test
        @DisplayName("Edge Case: Should allow a missing middle name")
        void shouldAllowMissingMiddleName() {
            // Act
            IndividualCustomer customer = IndividualCustomer.register(CUSTOMER_NUMBER, EMAIL, PHONE, FIRST_NAME, null,
                    LAST_NAME, EGN, registeredAddress());

            // Assert
            assertThat(customer.getMiddleName()).isNull();
        }

        @ParameterizedTest(name = "Error Case: Should reject invalid {0}")
        @MethodSource("bg.nbu.banksystem.customer.domain.IndividualCustomerTest#invalidRegistrationData")
        @DisplayName("Error Case: Should reject missing mandatory text")
        void shouldRejectInvalidMandatoryText(String field, @Nullable String customerNumber, @Nullable String email,
                                              @Nullable String phone, @Nullable String firstName,
                                              @Nullable String lastName, @Nullable String egn) {
            // Act & Assert
            assertThatIllegalArgumentException().isThrownBy(
                    () -> IndividualCustomer.register(customerNumber, email, phone, firstName, MIDDLE_NAME, lastName,
                            egn, registeredAddress())).withMessageContaining(field);
        }

        @Test
        @DisplayName("Error Case: Should reject an EGN with a wrong check digit")
        void shouldRejectInvalidEgn() {
            // Act & Assert
            assertThatIllegalArgumentException().isThrownBy(
                    () -> IndividualCustomer.register(CUSTOMER_NUMBER, EMAIL, PHONE, FIRST_NAME, MIDDLE_NAME, LAST_NAME,
                            "6101047501", registeredAddress())).withMessage("egn has an invalid check digit");
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
            IndividualCustomer customer = registerCustomer();

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
            IndividualCustomer customer = registerCustomer();

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
            IndividualCustomer customer = registerCustomer();
            Address newAddress = Address.of("BG", "Plovdiv", "4000", "1 Central Square", null);

            // Act
            customer.changeResidentialAddress(newAddress);

            // Assert
            assertThat(customer.getResidentialAddress()).isSameAs(newAddress);
        }

        @Test
        @DisplayName("Happy Path: Should correct a wrongly entered EGN")
        void shouldCorrectEgn() {
            // Arrange
            IndividualCustomer customer = registerCustomer();

            // Act
            customer.correctEgn("  0041010002  ");

            // Assert
            assertThat(customer.getEgn()).isEqualTo(CORRECTED_EGN);
        }

        @Test
        @DisplayName("Error Case: Should reject an invalid EGN correction")
        void shouldRejectInvalidEgnCorrection() {
            // Arrange
            IndividualCustomer customer = registerCustomer();

            // Act & Assert
            assertThatIllegalArgumentException().isThrownBy(() -> customer.correctEgn("6101047501"))
                    .withMessage("egn has an invalid check digit");

            assertThat(customer.getEgn()).isEqualTo(EGN);
        }

        @Test
        @DisplayName("Error Case: Should reject a missing residential address update")
        void shouldRejectMissingResidentialAddressUpdate() {
            // Arrange
            IndividualCustomer customer = registerCustomer();

            // Act & Assert
            assertThatNullPointerException().isThrownBy(() -> customer.changeResidentialAddress(null))
                    .withMessage("residentialAddress must not be null");
        }

        @ParameterizedTest(name = "Error Case: Should reject invalid {0}")
        @MethodSource("bg.nbu.banksystem.customer.domain.IndividualCustomerTest#invalidNameUpdateData")
        @DisplayName("Error Case: Should reject invalid mandatory names")
        void shouldRejectInvalidNameUpdate(String field, @Nullable String firstName, @Nullable String lastName) {
            // Arrange
            IndividualCustomer customer = registerCustomer();

            // Act & Assert
            assertThatIllegalArgumentException().isThrownBy(() -> customer.updateName(firstName, MIDDLE_NAME, lastName))
                    .withMessageContaining(field);
        }

        @ParameterizedTest(name = "Error Case: Should reject invalid {0}")
        @MethodSource("bg.nbu.banksystem.customer.domain.IndividualCustomerTest#invalidContactUpdateData")
        @DisplayName("Error Case: Should reject invalid contact information")
        void shouldRejectInvalidContactUpdate(String field, @Nullable String email, @Nullable String phone) {
            // Arrange
            IndividualCustomer customer = registerCustomer();

            // Act & Assert
            assertThatIllegalArgumentException().isThrownBy(() -> customer.updateContactInformation(email, phone))
                    .withMessageContaining(field);
        }
    }

    @Nested
    @DisplayName("Customer lifecycle")
    class CustomerLifecycle {

        @Test
        @DisplayName("Happy Path: Should close an active Customer")
        void shouldCloseActiveCustomer() {
            // Arrange
            IndividualCustomer customer = registerCustomer();

            // Act
            customer.close();

            // Assert
            assertThat(customer.getStatus()).isEqualTo(CustomerStatus.CLOSED);
        }

        @Test
        @DisplayName("Happy Path: Should reopen a closed Customer and keep its identity")
        void shouldReopenClosedCustomer() {
            // Arrange
            IndividualCustomer customer = registerCustomer();
            UUID originalId = customer.getId();
            customer.close();

            // Act
            customer.reopen();

            // Assert
            assertThat(customer.getStatus()).isEqualTo(CustomerStatus.ACTIVE);
            assertThat(customer.getId()).isEqualTo(originalId);
            assertThat(customer.getCustomerNumber()).isEqualTo(CUSTOMER_NUMBER);
            assertThat(customer.getEgn()).isEqualTo(EGN);
        }

        @Test
        @DisplayName("Happy Path: Should accept changes again after reopening")
        void shouldAcceptChangesAfterReopening() {
            // Arrange
            IndividualCustomer customer = registerCustomer();
            customer.close();
            customer.reopen();

            // Act
            customer.updateName("Updated", null, "Customer");

            // Assert
            assertThat(customer.getFirstName()).isEqualTo("Updated");
        }

        @Test
        @DisplayName("Error Case: Should reject closing an already closed Customer")
        void shouldRejectClosingClosedCustomer() {
            // Arrange
            IndividualCustomer customer = registerCustomer();
            customer.close();

            // Act & Assert
            assertDomainError(customer::close, CustomerErrorCode.CUSTOMER_ALREADY_CLOSED);
        }

        @Test
        @DisplayName("Error Case: Should reject reopening an active Customer")
        void shouldRejectReopeningActiveCustomer() {
            // Arrange
            IndividualCustomer customer = registerCustomer();

            // Act & Assert
            assertDomainError(customer::reopen, CustomerErrorCode.CUSTOMER_ALREADY_ACTIVE);
        }
    }

    @Nested
    @DisplayName("Closed Customer")
    class ClosedCustomer {

        @Test
        @DisplayName("Error Case: Should reject a name update on a closed Customer")
        void shouldRejectNameUpdate() {
            // Arrange
            IndividualCustomer customer = closedCustomer();

            // Act & Assert
            assertDomainError(() -> customer.updateName("Updated", null, "Customer"),
                    CustomerErrorCode.CUSTOMER_CLOSED);
        }

        @Test
        @DisplayName("Error Case: Should reject a contact update on a closed Customer")
        void shouldRejectContactUpdate() {
            // Arrange
            IndividualCustomer customer = closedCustomer();

            // Act & Assert
            assertDomainError(() -> customer.updateContactInformation("updated@example.com", "+359888654321"),
                    CustomerErrorCode.CUSTOMER_CLOSED);
        }

        @Test
        @DisplayName("Error Case: Should reject an address change on a closed Customer")
        void shouldRejectAddressChange() {
            // Arrange
            IndividualCustomer customer = closedCustomer();
            Address newAddress = Address.of("BG", "Plovdiv", "4000", "1 Central Square", null);

            // Act & Assert
            assertDomainError(() -> customer.changeResidentialAddress(newAddress), CustomerErrorCode.CUSTOMER_CLOSED);
        }

        @Test
        @DisplayName("Error Case: Should reject an EGN correction on a closed Customer")
        void shouldRejectEgnCorrection() {
            // Arrange
            IndividualCustomer customer = closedCustomer();

            // Act & Assert
            assertDomainError(() -> customer.correctEgn(CORRECTED_EGN), CustomerErrorCode.CUSTOMER_CLOSED);
        }

        @Test
        @DisplayName("Edge Case: Should report the closed status before invalid input")
        void shouldReportClosedStatusBeforeInvalidInput() {
            // Arrange
            IndividualCustomer customer = closedCustomer();

            // Act & Assert
            assertDomainError(() -> customer.updateName("   ", null, "   "), CustomerErrorCode.CUSTOMER_CLOSED);
        }
    }
}