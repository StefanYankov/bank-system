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

import java.time.LocalDate;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Representative (unit)")
class RepresentativeTest {

    private static final String FIRST_NAME = "Serafim";
    private static final String MIDDLE_NAME = "Ivanov";
    private static final String LAST_NAME = "Gerasimoff";
    private static final LocalDate VALID_FROM = LocalDate.of(2026, 1, 1);
    private static final LocalDate VALID_TO = LocalDate.of(2026, 12, 31);

    private static void assertDomainError(ThrowingCallable action, CustomerErrorCode expectedCode) {
        assertThatThrownBy(action)
                .isInstanceOfSatisfying(DomainException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(expectedCode));
    }

    private static Representative representative() {
        return legalEntityCustomer().addRepresentative(null, FIRST_NAME, MIDDLE_NAME, LAST_NAME,
                RepresentativeRole.LEGAL_REPRESENTATIVE, VALID_FROM);
    }

    private static LegalEntityCustomer legalEntityCustomer() {
        return LegalEntityCustomer.register("CUS-000002", "contact@starter.example", "+35929201234", "Starter Ltd",
                "123456786", address(), address());
    }

    private static IndividualCustomer individualCustomer() {
        return IndividualCustomer.register("CUS-000001", "serafim.gerasimoff@example.com", "+359888123456",
                FIRST_NAME, MIDDLE_NAME, LAST_NAME, "6101047500", address());
    }

    private static Address address() {
        return Address.of("BG", "Sofia", "1000", "1 Alexander Battenberg Square", null);
    }

    static Stream<Arguments> invalidNameData() {
        return Stream.of(
                Arguments.of("firstName", null, LAST_NAME),
                Arguments.of("firstName", "   ", LAST_NAME),
                Arguments.of("lastName", FIRST_NAME, null),
                Arguments.of("lastName", FIRST_NAME, "   "));
    }

    @Nested
    @DisplayName("Creation")
    class Creation {

        @Test
        @DisplayName("Happy Path: Should create a Representative with an assigned UUIDv7")
        void shouldCreateRepresentative() {
            // Arrange
            LegalEntityCustomer legalEntity = legalEntityCustomer();

            // Act
            Representative representative = legalEntity.addRepresentative(null, FIRST_NAME, MIDDLE_NAME, LAST_NAME,
                    RepresentativeRole.LEGAL_REPRESENTATIVE, VALID_FROM);

            // Assert
            assertThat(representative.getId()).isNotNull();
            assertThat(representative.getId().version()).isEqualTo(7);
            assertThat(representative.getLegalEntityCustomer()).isSameAs(legalEntity);
            assertThat(representative.getIndividualCustomer()).isNull();
            assertThat(representative.getFirstName()).isEqualTo(FIRST_NAME);
            assertThat(representative.getMiddleName()).isEqualTo(MIDDLE_NAME);
            assertThat(representative.getLastName()).isEqualTo(LAST_NAME);
            assertThat(representative.getRole()).isEqualTo(RepresentativeRole.LEGAL_REPRESENTATIVE);
            assertThat(representative.getValidFrom()).isEqualTo(VALID_FROM);
            assertThat(representative.getValidTo()).isNull();
        }

        @Test
        @DisplayName("Happy Path: Should link an existing individual Customer")
        void shouldLinkIndividualCustomer() {
            // Arrange
            IndividualCustomer individualCustomer = individualCustomer();

            // Act
            Representative representative = legalEntityCustomer().addRepresentative(individualCustomer, FIRST_NAME,
                    MIDDLE_NAME, LAST_NAME, RepresentativeRole.AUTHORIZED_REPRESENTATIVE, VALID_FROM);

            // Assert
            assertThat(representative.getIndividualCustomer()).isSameAs(individualCustomer);
        }

        @Test
        @DisplayName("Happy Path: Should keep the historical name snapshot when the linked Customer is renamed")
        void shouldKeepHistoricalNameSnapshot() {
            // Arrange
            IndividualCustomer individualCustomer = individualCustomer();
            Representative representative = legalEntityCustomer().addRepresentative(individualCustomer, FIRST_NAME,
                    MIDDLE_NAME, LAST_NAME, RepresentativeRole.LEGAL_REPRESENTATIVE, VALID_FROM);

            // Act
            individualCustomer.updateName("Serafim", null, "Petrov");

            // Assert
            assertThat(representative.getLastName()).isEqualTo(LAST_NAME);
            assertThat(representative.getIndividualCustomer().getLastName()).isEqualTo("Petrov");
        }

        @Test
        @DisplayName("Edge Case: Should normalize names and store a blank middle name as null")
        void shouldNormalizeNames() {
            // Act
            Representative representative = legalEntityCustomer().addRepresentative(null, "  Serafim  ", "   ",
                    "  Gerasimoff  ", RepresentativeRole.LEGAL_REPRESENTATIVE, VALID_FROM);

            // Assert
            assertThat(representative.getFirstName()).isEqualTo(FIRST_NAME);
            assertThat(representative.getMiddleName()).isNull();
            assertThat(representative.getLastName()).isEqualTo(LAST_NAME);
        }

        @ParameterizedTest(name = "Error Case: Should reject invalid {0}")
        @MethodSource("bg.nbu.banksystem.customer.domain.RepresentativeTest#invalidNameData")
        @DisplayName("Error Case: Should reject missing mandatory names")
        void shouldRejectInvalidMandatoryName(String field, @Nullable String firstName, @Nullable String lastName) {
            // Arrange
            LegalEntityCustomer legalEntity = legalEntityCustomer();

            // Act & Assert
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> legalEntity.addRepresentative(null, firstName, MIDDLE_NAME, lastName,
                            RepresentativeRole.LEGAL_REPRESENTATIVE, VALID_FROM))
                    .withMessageContaining(field);
        }

        @Test
        @DisplayName("Error Case: Should reject a missing role or valid-from date")
        void shouldRejectMissingRoleOrValidFrom() {
            // Arrange
            LegalEntityCustomer legalEntity = legalEntityCustomer();

            // Act & Assert
            assertThatNullPointerException()
                    .isThrownBy(() -> legalEntity.addRepresentative(null, FIRST_NAME, MIDDLE_NAME, LAST_NAME, null,
                            VALID_FROM))
                    .withMessage("role must not be null");

            assertThatNullPointerException()
                    .isThrownBy(() -> legalEntity.addRepresentative(null, FIRST_NAME, MIDDLE_NAME, LAST_NAME,
                            RepresentativeRole.LEGAL_REPRESENTATIVE, null))
                    .withMessage("validFrom must not be null");
        }
    }

    @Nested
    @DisplayName("Ending representation")
    class EndingRepresentation {

        @Test
        @DisplayName("Happy Path: Should end an active representation")
        void shouldEndRepresentation() {
            // Arrange
            Representative representative = representative();

            // Act
            representative.endRepresentation(VALID_TO);

            // Assert
            assertThat(representative.getValidTo()).isEqualTo(VALID_TO);
        }

        @Test
        @DisplayName("Edge Case: Should allow the end date to equal the start date")
        void shouldAllowSingleDayRepresentation() {
            // Arrange
            Representative representative = representative();

            // Act
            representative.endRepresentation(VALID_FROM);

            // Assert
            assertThat(representative.getValidTo()).isEqualTo(VALID_FROM);
        }

        @Test
        @DisplayName("Error Case: Should reject a missing end date")
        void shouldRejectMissingEndDate() {
            // Arrange
            Representative representative = representative();

            // Act & Assert
            assertThatNullPointerException()
                    .isThrownBy(() -> representative.endRepresentation(null))
                    .withMessage("validTo must not be null");
        }

        @Test
        @DisplayName("Error Case: Should reject an end date before the start date")
        void shouldRejectEndDateBeforeStartDate() {
            // Arrange
            Representative representative = representative();

            // Act & Assert
            assertDomainError(() -> representative.endRepresentation(VALID_FROM.minusDays(1)),
                    CustomerErrorCode.INVALID_REPRESENTATION_PERIOD);
        }

        @Test
        @DisplayName("Error Case: Should reject ending an already ended representation")
        void shouldRejectEndingTwice() {
            // Arrange
            Representative representative = representative();
            representative.endRepresentation(VALID_TO);

            // Act & Assert
            assertDomainError(() -> representative.endRepresentation(VALID_TO.plusDays(1)),
                    CustomerErrorCode.REPRESENTATION_ALREADY_ENDED);
        }

        @Test
        @DisplayName("Edge Case: Should report an ended representation before an invalid end date")
        void shouldReportAlreadyEndedBeforeInvalidPeriod() {
            // Arrange
            Representative representative = representative();
            representative.endRepresentation(VALID_TO);

            // Act & Assert
            assertDomainError(() -> representative.endRepresentation(VALID_FROM.minusDays(1)),
                    CustomerErrorCode.REPRESENTATION_ALREADY_ENDED);
        }
    }

    @Nested
    @DisplayName("Validity period")
    class ValidityPeriod {

        @Test
        @DisplayName("Edge Case: Should be inactive before the start date")
        void shouldBeInactiveBeforeValidFrom() {
            // Assert
            assertThat(representative().isActiveOn(VALID_FROM.minusDays(1))).isFalse();
        }

        @Test
        @DisplayName("Happy Path: Should be active on the start date and without an end date")
        void shouldBeActiveFromValidFromWithoutEndDate() {
            // Arrange
            Representative representative = representative();

            // Assert
            assertThat(representative.isActiveOn(VALID_FROM)).isTrue();
            assertThat(representative.isActiveOn(VALID_FROM.plusYears(5))).isTrue();
        }

        @Test
        @DisplayName("Edge Case: Should be active on the end date and inactive after it")
        void shouldTreatEndDateAsInclusive() {
            // Arrange
            Representative representative = representative();
            representative.endRepresentation(VALID_TO);

            // Assert
            assertThat(representative.isActiveOn(VALID_TO)).isTrue();
            assertThat(representative.isActiveOn(VALID_TO.plusDays(1))).isFalse();
        }

        @Test
        @DisplayName("Error Case: Should reject a missing evaluation date")
        void shouldRejectMissingEvaluationDate() {
            // Act & Assert
            assertThatNullPointerException()
                    .isThrownBy(() -> representative().isActiveOn(null))
                    .withMessage("date must not be null");
        }
    }
}