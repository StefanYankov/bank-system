package bg.nbu.banksystem.starter.error.internal;

import bg.nbu.banksystem.starter.error.ErrorCategory;
import bg.nbu.banksystem.starter.error.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import java.net.URI;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

@DisplayName("ProblemDetailFactory")
class ProblemDetailFactoryTest {

    private static final URI BASE = URI.create("https://example.test/problems/");
    private static final CategoryStatusMapper MAPPER = new CategoryStatusMapper();

    private record StubErrorCode(String code, ErrorCategory category, String title) implements ErrorCode {
    }

    private static final ErrorCode WIDGET_NOT_FOUND =
            new StubErrorCode("WIDGET_NOT_FOUND", ErrorCategory.NOT_FOUND, "Widget not found");

    private final ProblemDetailFactory factory = new ProblemDetailFactory(BASE, MAPPER, TraceIdProvider.NONE);

    // --- Happy path ---

    @Test
    @DisplayName("creates a problem with status, type, title, detail and code")
    void createsProblemFromErrorCode() {
        // Act
        ProblemDetail problem = factory.create(WIDGET_NOT_FOUND, "No widget with id 42");

        // Assert
        assertThat(problem.getStatus()).isEqualTo(404);
        assertThat(problem.getType()).isEqualTo(URI.create("https://example.test/problems/widget-not-found"));
        assertThat(problem.getTitle()).isEqualTo("Widget not found");
        assertThat(problem.getDetail()).isEqualTo("No widget with id 42");
        assertThat(problem.getProperties()).containsEntry("code", "WIDGET_NOT_FOUND");
    }

    @Test
    @DisplayName("adds traceId when a trace is active")
    void addsTraceIdWhenAvailable() {
        // Arrange
        ProblemDetailFactory tracingFactory = new ProblemDetailFactory(BASE, MAPPER, () -> Optional.of("abc123"));

        // Act
        ProblemDetail problem = tracingFactory.create(WIDGET_NOT_FOUND, null);

        // Assert
        assertThat(problem.getProperties()).containsEntry("traceId", "abc123");
    }

    @Test
    @DisplayName("enriches a Spring-built problem without changing its status or detail")
    void enrichesExistingProblem() {
        // Arrange
        ProblemDetail springBuilt = ProblemDetail.forStatusAndDetail(HttpStatus.METHOD_NOT_ALLOWED, "POST not supported");

        // Act
        ProblemDetail problem = factory.enrich(springBuilt, WIDGET_NOT_FOUND);

        // Assert
        assertThat(problem.getStatus()).isEqualTo(405);
        assertThat(problem.getDetail()).isEqualTo("POST not supported");
        assertThat(problem.getProperties()).containsEntry("code", "WIDGET_NOT_FOUND");
    }

    // --- Edge cases ---

    @Test
    @DisplayName("omits detail and traceId when they are absent")
    void omitsAbsentOptionalFields() {
        // Act
        ProblemDetail problem = factory.create(WIDGET_NOT_FOUND, null);

        // Assert
        assertThat(problem.getDetail()).isNull();
        assertThat(problem.getProperties()).doesNotContainKey("traceId");
    }

    @ParameterizedTest(name = "base \"{0}\"")
    @ValueSource(strings = {"https://example.test/problems/", "https://example.test/problems"})
    @DisplayName("builds the type from the base URI with or without a trailing slash")
    void buildsTypeRegardlessOfTrailingSlash(String base) {
        // Arrange
        ProblemDetailFactory customFactory = new ProblemDetailFactory(URI.create(base), MAPPER, TraceIdProvider.NONE);

        // Act
        URI type = customFactory.create(WIDGET_NOT_FOUND, null).getType();

        // Assert
        assertThat(type).isEqualTo(URI.create("https://example.test/problems/widget-not-found"));
    }

    // --- Error case ---

    @Test
    @DisplayName("rejects missing constructor arguments")
    void rejectsNullConstructorArguments() {
        // Act & Assert
        assertThatNullPointerException().isThrownBy(() -> new ProblemDetailFactory(null, MAPPER, TraceIdProvider.NONE));
        assertThatNullPointerException().isThrownBy(() -> new ProblemDetailFactory(BASE, null, TraceIdProvider.NONE));
        assertThatNullPointerException().isThrownBy(() -> new ProblemDetailFactory(BASE, MAPPER, null));
    }
}