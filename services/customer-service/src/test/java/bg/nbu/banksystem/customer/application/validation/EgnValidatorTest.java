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

@DisplayName("EgnValidator (unit)")
class EgnValidatorTest {

    private final EgnValidator validator = new EgnValidator();
    private final ConstraintValidatorContext context = mock(ConstraintValidatorContext.class);

    @Test
    @DisplayName("Happy Path: Should accept a valid EGN")
    void shouldAcceptValidEgn() {
        // Act & Assert
        assertThat(validator.isValid("6101047500", context)).isTrue();
    }

    @ParameterizedTest(name = "Error Case: Should reject \"{0}\"")
    @ValueSource(strings = {"6101047501", "610104750", "6113047500"})
    @DisplayName("Error Case: Should reject an invalid EGN")
    void shouldRejectInvalidEgn(String egn) {
        // Act & Assert
        assertThat(validator.isValid(egn, context)).isFalse();
    }

    @ParameterizedTest(name = "Edge Case: Should leave \"{0}\" to @NotBlank")
    @NullSource
    @ValueSource(strings = {"", "   "})
    @DisplayName("Edge Case: Should accept null or blank values")
    void shouldAcceptNullOrBlank(@Nullable String egn) {
        // Act & Assert
        assertThat(validator.isValid(egn, context)).isTrue();
    }
}