package bg.nbu.banksystem.starter.error;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

@DisplayName("DomainException")
class DomainExceptionTest {

    // --- Happy path ---

    @Test
    @DisplayName("keeps the error code and uses the detail as its message")
    void keepsErrorCodeAndDetail() {
        // Act
        DomainException exception = new DomainException(TestErrorCode.WIDGET_NOT_FOUND, "No widget with id 42");

        // Assert
        assertThat(exception.getErrorCode()).isEqualTo(TestErrorCode.WIDGET_NOT_FOUND);
        assertThat(exception.getDetail()).isEqualTo("No widget with id 42");
        assertThat(exception.getMessage()).isEqualTo("No widget with id 42");
    }

    @Test
    @DisplayName("keeps the cause for logging")
    void keepsCause() {
        // Arrange
        IllegalStateException cause = new IllegalStateException("constraint violated");

        // Act
        DomainException exception = new DomainException(TestErrorCode.WIDGET_NOT_FOUND, "Duplicate", cause);

        // Assert
        assertThat(exception.getCause()).isSameAs(cause);
    }

    // --- Edge case ---

    @Test
    @DisplayName("falls back to the title when no detail is given")
    void fallsBackToTitle() {
        // Act
        DomainException exception = new DomainException(TestErrorCode.WIDGET_NOT_FOUND);

        // Assert
        assertThat(exception.getDetail()).isNull();
        assertThat(exception.getMessage()).isEqualTo("Widget not found");
    }

    // --- Error case ---

    @Test
    @DisplayName("rejects a missing error code")
    void rejectsNullErrorCode() {
        // Act & Assert
        assertThatNullPointerException()
                .isThrownBy(() -> new DomainException(null, "detail"));
    }
}