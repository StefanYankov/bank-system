package bg.nbu.banksystem.starter.error.internal;

import bg.nbu.banksystem.starter.error.DomainException;
import bg.nbu.banksystem.starter.error.ErrorCategory;
import bg.nbu.banksystem.starter.error.ErrorCode;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.data.core.TypeInformation;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.net.URI;
import java.util.List;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("GlobalExceptionHandler")
class GlobalExceptionHandlerTest {

    private static final URI BASE = URI.create("https://example.test/problems/");
    private static final CategoryStatusMapper MAPPER = new CategoryStatusMapper();
    private static final String SECRET = "db-password-hunter2";

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ProblemDetailFactory factory = new ProblemDetailFactory(BASE, MAPPER, TraceIdProvider.NONE);
        mockMvc = MockMvcBuilders.standaloneSetup(new StubController())
                .setControllerAdvice(new GlobalExceptionHandler(factory))
                .build();
    }

    // --- Domain exceptions ---

    @ParameterizedTest(name = "{0}")
    @EnumSource(ErrorCategory.class)
    @DisplayName("turns a domain exception into a problem with the category's status")
    void handlesDomainException(ErrorCategory category) throws Exception {
        // Arrange
        int expectedStatus = MAPPER.statusFor(category).value();

        // Act & Assert
        mockMvc.perform(get("/domain/" + category))
                .andExpect(status().is(expectedStatus))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("WIDGET_" + category))
                .andExpect(jsonPath("$.detail").value("Widget 42"))
                .andExpect(jsonPath("$.instance").value("/domain/" + category));
    }

    // --- Validation ---

    @Test
    @DisplayName("keeps every validation error on the same field")
    void reportsAllFieldErrors() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/validate").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors[?(@.field == 'name')]", hasSize(2)));
    }

    @Test
    @DisplayName("never echoes the rejected value")
    void doesNotEchoRejectedValue() throws Exception {
        // Arrange
        String rejected = "this-value-is-far-too-long-to-pass";

        // Act & Assert
        mockMvc.perform(post("/validate").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + rejected + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(not(containsString(rejected))));
    }

    @Test
    @DisplayName("reports an object-level rule without a field")
    void reportsObjectLevelError() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/range").contentType(MediaType.APPLICATION_JSON).content("{\"min\":10,\"max\":5}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].message").value("max must not be less than min"))
                .andExpect(jsonPath("$.errors[0].field").doesNotExist());
    }

    @Test
    @DisplayName("reports an invalid request parameter by name")
    void reportsParameterError() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/params").param("size", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors[0].field").value("size"));
    }

    // --- Spring MVC errors ---

    @ParameterizedTest(name = "{0}")
    @MethodSource("frameworkErrors")
    @DisplayName("enriches problems Spring MVC builds itself")
    void enrichesFrameworkErrors(String scenario, RequestBuilder request, int expectedStatus, String expectedCode)
            throws Exception {
        // Act & Assert
        mockMvc.perform(request)
                .andExpect(status().is(expectedStatus))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value(expectedCode));
    }

    static Stream<Arguments> frameworkErrors() {
        return Stream.of(
                Arguments.of("malformed JSON",
                        post("/validate").contentType(MediaType.APPLICATION_JSON).content("{ not json"),
                        400, "MALFORMED_REQUEST"),
                Arguments.of("unsupported method",
                        delete("/validate"),
                        405, "METHOD_NOT_ALLOWED"),
                Arguments.of("unsupported media type",
                        post("/validate").contentType(MediaType.TEXT_PLAIN).content("name"),
                        415, "UNSUPPORTED_MEDIA_TYPE")
        );
    }

    // --- Other handlers ---

    @Test
    @DisplayName("turns access denied into 403")
    void handlesAccessDenied() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/access-denied"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    @DisplayName("turns an unknown sort property into 400")
    void handlesInvalidSort() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/sort"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_SORT"));
    }

    @Test
    @DisplayName("hides the details of an unexpected exception")
    void hidesUnexpectedExceptionDetails() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/unexpected"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.detail").value("An unexpected error occurred."))
                .andExpect(content().string(not(containsString(SECRET))));
    }

    // --- Stub controller and fixtures ---

    @RestController
    static class StubController {

        @GetMapping("/domain/{category}")
        void throwDomain(@PathVariable ErrorCategory category) {
            throw new DomainException(new StubErrorCode("WIDGET_" + category, category, "Widget problem"), "Widget 42");
        }

        @PostMapping(value = "/validate", consumes = MediaType.APPLICATION_JSON_VALUE)
        void validate(@Valid @RequestBody NameRequest request) {
        }

        @PostMapping(value = "/range", consumes = MediaType.APPLICATION_JSON_VALUE)
        void range(@Valid @RequestBody RangeRequest request) {
        }

        @GetMapping("/params")
        void params(@RequestParam @Min(1) int size) {
        }

        @GetMapping("/access-denied")
        void accessDenied() {
            throw new AccessDeniedException("denied");
        }

        @GetMapping("/sort")
        void sort() {
            throw new PropertyReferenceException("doesNotExist", TypeInformation.of(NameRequest.class), List.of());
        }

        @GetMapping("/unexpected")
        void unexpected() {
            throw new IllegalStateException("Connection failed with " + SECRET);
        }
    }

    private record StubErrorCode(String code, ErrorCategory category, String title) implements ErrorCode {
    }

    public record NameRequest(@NotBlank @Size(min = 3, max = 20) String name) {
    }

    @ValidRange
    public record RangeRequest(int min, int max) {
    }

    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    @Constraint(validatedBy = ValidRangeValidator.class)
    @interface ValidRange {
        String message() default "max must not be less than min";

        Class<?>[] groups() default {};

        Class<? extends Payload>[] payload() default {};
    }

    public static class ValidRangeValidator implements ConstraintValidator<ValidRange, RangeRequest> {
        @Override
        public boolean isValid(RangeRequest value, ConstraintValidatorContext context) {
            return value == null || value.max() >= value.min();
        }
    }
}