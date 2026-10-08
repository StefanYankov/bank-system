package bg.nbu.banksystem.starter.error.internal;

import bg.nbu.banksystem.starter.error.ErrorCategory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

@DisplayName("CategoryStatusMapper")
class CategoryStatusMapperTest {

    private final CategoryStatusMapper mapper = new CategoryStatusMapper();

    // --- Happy path ----

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({
            "VALIDATION, 400",
            "NOT_FOUND, 404",
            "CONFLICT, 409",
            "RULE_VIOLATION, 422",
            "UNAUTHENTICATED, 401",
            "ACCESS_DENIED, 403",
            "INTERNAL, 500"
    })
    @DisplayName("maps each category to its HTTP status")
    void mapsCategoryToStatus(ErrorCategory category, int expectedStatus) {
        // Act
        int status = mapper.statusFor(category).value();

        // Assert
        assertThat(status).isEqualTo(expectedStatus);
    }

    // --- Error case ---

    @Test
    @DisplayName("rejects a missing category")
    void rejectsNullCategory() {
        // Act & Assert
        assertThatNullPointerException().isThrownBy(() -> mapper.statusFor(null));
    }
}