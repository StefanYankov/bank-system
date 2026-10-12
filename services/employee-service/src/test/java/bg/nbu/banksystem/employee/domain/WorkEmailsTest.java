package bg.nbu.banksystem.employee.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

@DisplayName("WorkEmails (unit)")
class WorkEmailsTest {

    @Test
    @DisplayName("Happy Path: Should trim and lower-case the email")
    void shouldNormalizeEmail() {
        // Act
        String normalized = WorkEmails.normalize("  Serafim.Gerasimoff@Example.COM  ");

        // Assert
        assertThat(normalized).isEqualTo("serafim.gerasimoff@example.com");
    }

    @ParameterizedTest(name = "Error Case: Should reject \"{0}\"")
    @NullSource
    @ValueSource(strings = {"", "   "})
    @DisplayName("Error Case: Should reject null or blank emails")
    void shouldRejectNullOrBlank(String workEmail) {
        // Act & Assert
        assertThatIllegalArgumentException()
                .isThrownBy(() -> WorkEmails.normalize(workEmail))
                .withMessageContaining("workEmail");
    }
}