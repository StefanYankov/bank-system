package bg.nbu.banksystem.starter.error.internal;

import bg.nbu.banksystem.starter.error.ErrorCategory;
import bg.nbu.banksystem.starter.error.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.http.ProblemDetail;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ErrorHandlingAutoConfiguration (integration)")
class ErrorHandlingAutoConfigurationTest {

    private final WebApplicationContextRunner webRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ErrorHandlingAutoConfiguration.class))
            .withBean(JsonMapper.class, () -> JsonMapper.builder().build());

    private record StubErrorCode(String code, ErrorCategory category, String title) implements ErrorCode {
    }

    @Nested
    @DisplayName("Bean registration")
    class BeanRegistration {

        @Test
        @DisplayName("Happy Path: Should register all error-handling beans in a servlet web application")
        void shouldRegisterAllBeans() {
            webRunner.run(context -> {
                // Assert
                assertThat(context).hasSingleBean(CategoryStatusMapper.class)
                        .hasSingleBean(TraceIdProvider.class)
                        .hasSingleBean(ProblemDetailFactory.class)
                        .hasSingleBean(GlobalExceptionHandler.class)
                        .hasSingleBean(AuthenticationEntryPoint.class)
                        .hasSingleBean(AccessDeniedHandler.class);
                assertThat(context.getBean(AuthenticationEntryPoint.class))
                        .isInstanceOf(ProblemDetailAuthenticationEntryPoint.class);
            });
        }

        @Test
        @DisplayName("Edge Case: Should back off when the service defines its own entry point")
        void shouldBackOffForCustomEntryPoint() {
            // Arrange
            AuthenticationEntryPoint custom = (request, response, ex) -> response.setStatus(401);

            webRunner.withBean(AuthenticationEntryPoint.class, () -> custom)
                    .run(context -> {
                        // Assert
                        assertThat(context).hasSingleBean(AuthenticationEntryPoint.class);
                        assertThat(context.getBean(AuthenticationEntryPoint.class)).isSameAs(custom);
                    });
        }
    }

    @Nested
    @DisplayName("Problem type")
    class ProblemType {

        @Test
        @DisplayName("Happy Path: Should build problem types from the published base URI")
        void shouldUsePublishedBaseUri() {
            webRunner.run(context -> {
                // Arrange
                ProblemDetailFactory factory = context.getBean(ProblemDetailFactory.class);
                ErrorCode code = new StubErrorCode("WIDGET_NOT_FOUND", ErrorCategory.NOT_FOUND, "Widget not found");

                // Act
                ProblemDetail problem = factory.create(code, null);

                // Assert
                assertThat(problem.getType()).hasToString(
                        "https://stefanyankov.github.io/bank-system/problems/widget-not-found");
            });
        }
    }

    @Nested
    @DisplayName("Application type")
    class ApplicationType {

        @Test
        @DisplayName("Error Case: Should stay inactive outside a servlet web application")
        void shouldStayInactiveOutsideWebApplication() {
            // [GUIDE] A plain (non-web) context: @ConditionalOWebApplication fails, so nothing is registered.
            new ApplicationContextRunner()
                    .withConfiguration(AutoConfigurations.of(ErrorHandlingAutoConfiguration.class))
                    .run(context -> assertThat(context).doesNotHaveBean(ProblemDetailFactory.class));
        }
    }
}