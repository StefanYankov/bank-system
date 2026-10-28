package bg.nbu.banksystem.customer.application.validation;

import jakarta.validation.ConstraintValidatorContext;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@DisplayName("EikValidator (unit)")
class EikValidatorTest {

    private final EikValidator validator = new EikValidator();
    private final ConstraintValidatorContext context = mock(ConstraintValidatorContext.class);

    @Test
    @DisplayName("Happy Path: Should accept a valid EIK")
    void shouldAcceptValidEik() {
        // Act & Assert
        assertThat(validator.isValid("123456786", context)).isTrue();
    }

    @ParameterizedTest(name = "Error Case: Should reject \"{0}\"")
    @ValueSource(strings = {"123456789", "12345678", "1234567860000"})
    @DisplayName("Error Case: Should reject an invalid EIK")
    void shouldRejectInvalidEik(String eik) {
        // Act & Assert
        assertThat(validator.isValid(eik, context)).isFalse();
    }

    @ParameterizedTest(name = "Edge Case: Should leave \"{0}\" to @NotBlank")
    @NullSource
    @ValueSource(strings = {"", "   "})
    @DisplayName("Edge Case: Should accept null or blank values")
    void shouldAcceptNullOrBlank(@Nullable String eik) {
        // Act & Assert
        assertThat(validator.isValid(eik, context)).isTrue();
    }
}