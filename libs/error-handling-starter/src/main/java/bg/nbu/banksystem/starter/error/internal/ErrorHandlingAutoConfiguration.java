package bg.nbu.banksystem.starter.error.internal;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;

/**
 * Registers RFC 9457 error handling for servlet-based bank-system services.
 *
 * <p>Every bean backs off when the application defines its own bean of the same type.</p>
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class ErrorHandlingAutoConfiguration {

    static final URI PROBLEM_TYPE_BASE_URI = URI.create("https://stefanyankov.github.io/bank-system/problems/");

    /**
     * @return the category to HTTP status mapping
     */
    @Bean
    @ConditionalOnMissingBean
    public CategoryStatusMapper categoryStatusMapper() {
        return new CategoryStatusMapper();
    }

    /**
     * @return a provider that reports no trace id until tracing is configured
     */
    @Bean
    @ConditionalOnMissingBean
    public TraceIdProvider traceIdProvider() {
        return TraceIdProvider.NONE;
    }

    /**
     * @param statusMapper    maps categories to statuses
     * @param traceIdProvider supplies the current trace id
     * @return the factory that builds every problem response
     */
    @Bean
    @ConditionalOnMissingBean
    public ProblemDetailFactory problemDetailFactory(CategoryStatusMapper statusMapper,
                                                     TraceIdProvider traceIdProvider) {
        return new ProblemDetailFactory(PROBLEM_TYPE_BASE_URI, statusMapper, traceIdProvider);
    }

    /**
     * @param problemDetailFactory builds the problem responses
     * @return the advice handling exceptions raised by controllers
     */
    @Bean
    @ConditionalOnMissingBean(ResponseEntityExceptionHandler.class)
    public GlobalExceptionHandler globalExceptionHandler(ProblemDetailFactory problemDetailFactory) {
        return new GlobalExceptionHandler(problemDetailFactory);
    }

    /**
     * @param problemDetailFactory builds the problem body
     * @param jsonMapper           Spring Boot's JSON mapper
     * @return the entry point writing 401 problem responses
     */
    @Bean
    @ConditionalOnMissingBean
    public AuthenticationEntryPoint problemDetailAuthenticationEntryPoint(ProblemDetailFactory problemDetailFactory,
                                                                          JsonMapper jsonMapper) {
        return new ProblemDetailAuthenticationEntryPoint(problemDetailFactory, jsonMapper);
    }

    /**
     * @param problemDetailFactory builds the problem body
     * @param jsonMapper           Spring Boot's JSON mapper
     * @return the handler writing 403 problem responses
     */
    @Bean
    @ConditionalOnMissingBean
    public AccessDeniedHandler problemDetailAccessDeniedHandler(ProblemDetailFactory problemDetailFactory,
                                                                JsonMapper jsonMapper) {
        return new ProblemDetailAccessDeniedHandler(problemDetailFactory, jsonMapper);
    }
}