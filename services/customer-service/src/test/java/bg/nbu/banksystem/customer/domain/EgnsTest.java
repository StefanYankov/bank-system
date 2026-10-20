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

@DisplayName("Egns (unit)")
class EgnsTest {

    @Nested
    @DisplayName("Valid EGNs")
    class ValidEgns {

        @Test
        @DisplayName("Happy Path: Should trim and accept a valid EGN")
        void shouldTrimAndAcceptValidEgn() {
            // Act
            String normalized = Egns.normalize("  6101047500  ");

            // Assert
            assertThat(normalized).isEqualTo("6101047500");
        }

        @ParameterizedTest(name = "Happy Path: Should accept {0}")
        @ValueSource(strings = {"6101047500", "0041010002", "9921010007", "0042290000"})
        @DisplayName("Happy Path: Should accept every supported century encoding")
        void shouldAcceptSupportedCenturyEncodings(String egn) {
            // Act & Assert
            assertThat(Egns.normalize(egn)).isEqualTo(egn);
        }

        @Test
        @DisplayName("Edge Case: Should use 0 as the check digit when the remainder is 10")
        void shouldUseZeroCheckDigitForRemainderTen() {
            // Act & Assert
            assertThat(Egns.normalize("0041019000")).isEqualTo("0041019000");
        }
    }

    @Nested
    @DisplayName("Invalid EGNs")
    class InvalidEgns {

        @ParameterizedTest(name = "Error Case: Should reject \"{0}\"")
        @NullSource
        @ValueSource(strings = {"", "   "})
        @DisplayName("Error Case: Should reject null or blank EGNs")
        void shouldRejectNullOrBlank(@Nullable String egn) {
            // Act & Assert
            assertThatIllegalArgumentException().isThrownBy(() -> Egns.normalize(egn))
                    .withMessage("egn must not be blank");
        }

        @ParameterizedTest(name = "Error Case: Should reject \"{0}\"")
        @ValueSource(strings = {"610104750", "61010475000", "61010475A0", "6101 47500"})
        @DisplayName("Error Case: Should reject values that are not ten digits")
        void shouldRejectMalformedEgn(String egn) {
            // Act & Assert
            assertThatIllegalArgumentException().isThrownBy(() -> Egns.normalize(egn))
                    .withMessage("egn must contain exactly 10 digits");
        }

        @ParameterizedTest(name = "Error Case: Should reject \"{0}\"")
        @ValueSource(strings = {"6100047500", // month 00
                "6113047500", // month 13
                "6120047500", // month 20, between the encoded ranges
                "6101007500", // day 00
                "6102297500"  // 29 February 1961, not a leap year
        })
        @DisplayName("Error Case: Should reject EGNs encoding an impossible birth date")
        void shouldRejectImpossibleBirthDate(String egn) {
            // Act & Assert
            assertThatIllegalArgumentException().isThrownBy(() -> Egns.normalize(egn))
                    .withMessage("egn must encode a valid birth date");
        }

        @Test
        @DisplayName("Error Case: Should reject an EGN with a wrong check digit")
        void shouldRejectWrongCheckDigit() {
            // Act & Assert
            assertThatIllegalArgumentException().isThrownBy(() -> Egns.normalize("6101047501"))
                    .withMessage("egn has an invalid check digit");
        }
    }
}