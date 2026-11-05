package bg.nbu.banksystem.starter.error.internal;

import bg.nbu.banksystem.starter.error.ErrorCategory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

@DisplayName("CategoryStatusMapper (unit)")
class CategoryStatusMapperTest {

    private final CategoryStatusMapper mapper = new CategoryStatusMapper();

    @ParameterizedTest(name = "Happy Path: Should map {0} to HTTP {1}")
    @CsvSource(
        {"VALIDATION, 400", "NOT_FOUND, 404", "CONFLICT, 409", "RULE_VIOLATION, 422", "UNAUTHENTICATED, 401", "ACCESS_DENIED, 403", "UNAVAILABLE, 503", "INTERNAL, 500"})
    @DisplayName("Happy Path: Should map each ErrorCategory to its HTTP status")
    void shouldMapCategoryToHttpStatus(ErrorCategory category, int expectedStatus) {
        // Act
        int actualStatus = mapper.statusFor(category).value();

        // Assert
        assertThat(actualStatus).isEqualTo(expectedStatus);
    }

    @Test
    @DisplayName("Error Case: Should reject a missing ErrorCategory")
    void shouldRejectMissingCategory() {
        // Act & Assert
        assertThatNullPointerException().isThrownBy(() -> mapper.statusFor(null));
    }
}
