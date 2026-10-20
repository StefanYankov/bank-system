package bg.nbu.banksystem.customer.domain;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

@DisplayName("Eiks (unit)")
class EiksTest {

    @Nested
    @DisplayName("Valid EIKs")
    class ValidEiks {

        @Test
        @DisplayName("Happy Path: Should trim and accept a valid EIK")
        void shouldTrimAndAcceptValidEik() {
            // Act
            String normalized = Eiks.normalize("  123456786  ");

            // Assert
            assertThat(normalized).isEqualTo("123456786");
        }

        @Test
        @DisplayName("Edge Case: Should use the fallback weights when the first remainder is 10")
        void shouldUseFallbackWeights() {
            // Act & Assert
            assertThat(Eiks.normalize("000000305")).isEqualTo("000000305");
        }

        @Test
        @DisplayName("Edge Case: Should use 0 as the check digit when both remainders are 10")
        void shouldUseZeroCheckDigitWhenBothRemaindersAreTen() {
            // Act & Assert
            assertThat(Eiks.normalize("605000000")).isEqualTo("605000000");
        }
    }

    @Nested
    @DisplayName("Invalid EIKs")
    class InvalidEiks {

        @ParameterizedTest(name = "Error Case: Should reject \"{0}\"")
        @NullSource
        @ValueSource(strings = {"", "   "})
        @DisplayName("Error Case: Should reject null or blank EIKs")
        void shouldRejectNullOrBlank(@Nullable String eik) {
            // Act & Assert
            assertThatIllegalArgumentException().isThrownBy(() -> Eiks.normalize(eik))
                    .withMessage("eik must not be blank");
        }

        @ParameterizedTest(name = "Error Case: Should reject \"{0}\"")
        @ValueSource(strings = {"12345678", "1234567860000", "12345678A", "1234 5678"})
        @DisplayName("Error Case: Should reject values that are not nine digits")
        void shouldRejectMalformedEik(String eik) {
            // Act & Assert
            assertThatIllegalArgumentException().isThrownBy(() -> Eiks.normalize(eik))
                    .withMessage("eik must contain exactly 9 digits");
        }

        @Test
        @DisplayName("Error Case: Should reject an EIK with a wrong check digit")
        void shouldRejectWrongCheckDigit() {
            // Act & Assert
            assertThatIllegalArgumentException().isThrownBy(() -> Eiks.normalize("123456789"))
                    .withMessage("eik has an invalid check digit");
        }
    }
}