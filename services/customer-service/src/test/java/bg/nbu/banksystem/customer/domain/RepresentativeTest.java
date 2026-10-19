package bg.nbu.banksystem.customer.domain;

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
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

@DisplayName("Representative (unit)")
class RepresentativeTest {

    private static final String FIRST_NAME = "Serafim";
    private static final String MIDDLE_NAME = "Ivanov";
    private static final String LAST_NAME = "Gerasimoff";

    private static final LocalDate VALID_FROM = LocalDate.of(2026, 1, 1);

    private static Representative representativeFixture() {
        return legalEntityFixture().addRepresentative(null, FIRST_NAME, MIDDLE_NAME, LAST_NAME,
                RepresentativeRole.LEGAL_REPRESENTATIVE, VALID_FROM);
    }

    private static LegalEntityCustomer legalEntityFixture() {
        return LegalEntityCustomer.register("CUS-000002", "contact@starter.example", "+35929201234",
                "Starter Financial Services", "123456789", registeredAddressFixture(),
                correspondenceAddressFixture());
    }

    private static IndividualCustomer individualCustomerFixture() {
        return IndividualCustomer.register("CUS-000001", "serafim.gerasimoff@example.com", "+359888123456", FIRST_NAME,
                MIDDLE_NAME, LAST_NAME, "9001010000", residentialAddressFixture());
    }

    private static Address residentialAddressFixture() {
        return Address.of("BG", "Sofia", "1000", "1 Alexander Battenberg Square", null);
    }

    private static Address registeredAddressFixture() {
        return Address.of("BG", "Sofia", "1000", "1 Alexander Battenberg Square", null);
    }

    private static Address correspondenceAddressFixture() {
        return Address.of("BG", "Sofia", "1000", "10 Business Park Street", "Office 4");
    }

    static Stream<Arguments> invalidNameData() {
        return Stream.of(Arguments.of("firstName", null, LAST_NAME), Arguments.of("firstName", "   ", LAST_NAME),
                Arguments.of("lastName", FIRST_NAME, null), Arguments.of("lastName", FIRST_NAME, "   "));
    }

    @Nested
    @DisplayName("Creation")
    class Creation {

        @Test
        @DisplayName("Happy Path: Should create a Representative with an assigned UUIDv7")
        void shouldCreateRepresentative() {
            // Arrange
            LegalEntityCustomer legalEntity = legalEntityFixture();

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
        @DisplayName("Happy Path: Should preserve the historical name snapshot")
        void shouldPreserveHistoricalNameSnapshot() {
            // Arrange
            LegalEntityCustomer legalEntity = legalEntityFixture();
            IndividualCustomer individualCustomer = individualCustomerFixture();
            Representative representative = legalEntity.addRepresentative(individualCustomer, FIRST_NAME, MIDDLE_NAME,
                    LAST_NAME, RepresentativeRole.LEGAL_REPRESENTATIVE, VALID_FROM);

            // Act
            individualCustomer.updateName("Updated", null, "Customer");

            // Assert
            assertThat(representative.getFirstName()).isEqualTo(FIRST_NAME);
            assertThat(representative.getMiddleName()).isEqualTo(MIDDLE_NAME);
            assertThat(representative.getLastName()).isEqualTo(LAST_NAME);
            assertThat(representative.getIndividualCustomer().getFirstName()).isEqualTo("Updated");
            assertThat(representative.getIndividualCustomer().getLastName()).isEqualTo("Customer");
        }

        @Test
        @DisplayName("Happy Path: Should link an existing individual Customer")
        void shouldLinkIndividualCustomer() {
            // Arrange
            LegalEntityCustomer legalEntity = legalEntityFixture();
            IndividualCustomer individualCustomer = individualCustomerFixture();

            // Act
            Representative representative = legalEntity.addRepresentative(individualCustomer, FIRST_NAME, MIDDLE_NAME,
                    LAST_NAME, RepresentativeRole.AUTHORIZED_REPRESENTATIVE, VALID_FROM);

            // Assert
            assertThat(representative.getIndividualCustomer()).isSameAs(individualCustomer);
        }

        @Test
        @DisplayName("Edge Case: Should allow a Representative without a Customer link")
        void shouldAllowRepresentativeWithoutCustomerLink() {
            // Arrange
            LegalEntityCustomer legalEntity = legalEntityFixture();

            // Act
            Representative representative = legalEntity.addRepresentative(null, FIRST_NAME, null, LAST_NAME,
                    RepresentativeRole.AUTHORIZED_REPRESENTATIVE, VALID_FROM);

            // Assert
            assertThat(representative.getIndividualCustomer()).isNull();
            assertThat(representative.getMiddleName()).isNull();
        }

        @Test
        @DisplayName("Edge Case: Should normalize Representative names")
        void shouldNormalizeNames() {
            // Arrange
            LegalEntityCustomer legalEntity = legalEntityFixture();

            // Act
            Representative representative = legalEntity.addRepresentative(null, "  Serafim  ", "  Ivanov  ",
                    "  Gerasimoff  ", RepresentativeRole.LEGAL_REPRESENTATIVE, VALID_FROM);

            // Assert
            assertThat(representative.getFirstName()).isEqualTo(FIRST_NAME);
            assertThat(representative.getMiddleName()).isEqualTo(MIDDLE_NAME);
            assertThat(representative.getLastName()).isEqualTo(LAST_NAME);
        }

        @Test
        @DisplayName("Edge Case: Should store a blank middle name as null")
        void shouldNormalizeBlankMiddleNameToNull() {
            // Arrange
            LegalEntityCustomer legalEntity = legalEntityFixture();

            // Act
            Representative representative = legalEntity.addRepresentative(null, FIRST_NAME, "   ", LAST_NAME,
                    RepresentativeRole.LEGAL_REPRESENTATIVE, VALID_FROM);

            // Assert
            assertThat(representative.getMiddleName()).isNull();
        }

        @ParameterizedTest(name = "Error Case: Should reject invalid {0}")
        @MethodSource(
                "bg.nbu.banksystem.customer.domain."
                        + "RepresentativeTest#invalidNameData"
        )
        @DisplayName("Error Case: Should reject missing mandatory names")
        void shouldRejectInvalidMandatoryName(String field, @Nullable String firstName, @Nullable String lastName) {
            // Arrange
            LegalEntityCustomer legalEntity = legalEntityFixture();

            // Act & Assert
            assertThatIllegalArgumentException().isThrownBy(
                    () -> legalEntity.addRepresentative(null, firstName, MIDDLE_NAME, lastName,
                            RepresentativeRole.LEGAL_REPRESENTATIVE, VALID_FROM)).withMessageContaining(field);
        }

        @Test
        @DisplayName("Error Case: Should reject a missing role")
        void shouldRejectMissingRole() {
            // Arrange
            LegalEntityCustomer legalEntity = legalEntityFixture();

            // Act & Assert
            assertThatNullPointerException().isThrownBy(
                            () -> legalEntity.addRepresentative(null, FIRST_NAME, MIDDLE_NAME, LAST_NAME, null, VALID_FROM))
                    .withMessage("role must not be null");
        }

        @Test
        @DisplayName("Error Case: Should reject a missing valid-from date")
        void shouldRejectMissingValidFrom() {
            // Arrange
            LegalEntityCustomer legalEntity = legalEntityFixture();

            // Act & Assert
            assertThatNullPointerException().isThrownBy(
                    () -> legalEntity.addRepresentative(null, FIRST_NAME, MIDDLE_NAME, LAST_NAME,
                            RepresentativeRole.LEGAL_REPRESENTATIVE, null)).withMessage("validFrom must not be null");
        }
    }

    @Nested
    @DisplayName("Validity period")
    class ValidityPeriod {

        @Test
        @DisplayName("Happy Path: Should end an active representation")
        void shouldEndRepresentation() {
            // Arrange
            Representative representative = representativeFixture();

            LocalDate validTo = LocalDate.of(2026, 12, 31);

            // Act
            representative.endRepresentation(validTo);

            // Assert
            assertThat(representative.getValidTo()).isEqualTo(validTo);
        }

        @Test
        @DisplayName("Edge Case: Should allow the end date to equal the start date")
        void shouldAllowSingleDayRepresentation() {
            // Arrange
            Representative representative = representativeFixture();

            // Act
            representative.endRepresentation(VALID_FROM);

            // Assert
            assertThat(representative.getValidTo()).isEqualTo(VALID_FROM);
            assertThat(representative.isActiveOn(VALID_FROM)).isTrue();
        }

        @Test
        @DisplayName("Error Case: Should reject a missing end date")
        void shouldRejectMissingEndDate() {
            // Arrange
            Representative representative = representativeFixture();

            // Act & Assert
            assertThatNullPointerException().isThrownBy(() -> representative.endRepresentation(null))
                    .withMessage("validTo must not be null");
        }

        @Test
        @DisplayName("Error Case: Should reject an end date before the start date")
        void shouldRejectEndDateBeforeStartDate() {
            // Arrange
            Representative representative = representativeFixture();
            LocalDate invalidValidTo = VALID_FROM.minusDays(1);

            // Act & Assert
            assertThatIllegalArgumentException().isThrownBy(() -> representative.endRepresentation(invalidValidTo))
                    .withMessage("validTo must not be before validFrom");
        }

        @Test
        @DisplayName("Error Case: Should reject ending an already ended representation")
        void shouldRejectEndingRepresentationTwice() {
            // Arrange
            Representative representative = representativeFixture();
            representative.endRepresentation(LocalDate.of(2026, 12, 31));

            // Act & Assert
            assertThatIllegalStateException().isThrownBy(
                            () -> representative.endRepresentation(LocalDate.of(2027, 1, 31)))
                    .withMessage("representation has already ended");
        }

        @Test
        @DisplayName("Edge Case: Should be inactive before the start date")
        void shouldBeInactiveBeforeValidFrom() {
            // Arrange
            Representative representative = representativeFixture();

            // Act
            boolean active = representative.isActiveOn(VALID_FROM.minusDays(1));

            // Assert
            assertThat(active).isFalse();
        }

        @Test
        @DisplayName("Happy Path: Should be active on the start date")
        void shouldBeActiveOnValidFrom() {
            // Arrange
            Representative representative = representativeFixture();

            // Act
            boolean active = representative.isActiveOn(VALID_FROM);

            // Assert
            assertThat(active).isTrue();
        }

        @Test
        @DisplayName("Happy Path: Should remain active without an end date")
        void shouldRemainActiveWithoutValidTo() {
            // Arrange
            Representative representative = representativeFixture();

            // Act
            boolean active = representative.isActiveOn(VALID_FROM.plusYears(5));

            // Assert
            assertThat(active).isTrue();
        }

        @Test
        @DisplayName("Happy Path: Should be active on the end date")
        void shouldBeActiveOnValidTo() {
            // Arrange
            Representative representative = representativeFixture();

            LocalDate validTo = LocalDate.of(2026, 12, 31);

            representative.endRepresentation(validTo);

            // Act
            boolean active = representative.isActiveOn(validTo);

            // Assert
            assertThat(active).isTrue();
        }

        @Test
        @DisplayName("Edge Case: Should be inactive after the end date")
        void shouldBeInactiveAfterValidTo() {
            // Arrange
            Representative representative = representativeFixture();

            LocalDate validTo = LocalDate.of(2026, 12, 31);

            representative.endRepresentation(validTo);

            // Act
            boolean active = representative.isActiveOn(validTo.plusDays(1));

            // Assert
            assertThat(active).isFalse();
        }

        @Test
        @DisplayName("Error Case: Should reject a missing evaluation date")
        void shouldRejectMissingEvaluationDate() {
            // Arrange
            Representative representative = representativeFixture();

            // Act & Assert
            assertThatNullPointerException().isThrownBy(() -> representative.isActiveOn(null))
                    .withMessage("date must not be null");
        }
    }
}