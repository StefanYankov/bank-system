package bg.nbu.banksystem.starter.error.internal;

import bg.nbu.banksystem.starter.error.DomainException;
import bg.nbu.banksystem.starter.error.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.Errors;
import org.springframework.validation.method.ParameterErrors;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Translates exceptions raised while handling a request into RFC 9457 problem responses.
 *
 * <p>
 * Every response is built by {@link ProblemDetailFactory}; this class only decides which
 * {@link ErrorCode} applies, adds request-specific fields and logs the failure.
 * </p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static final String ERRORS_PROPERTY = "errors";
    private static final String INTERNAL_ERROR_DETAIL = "An unexpected error occurred.";
    private static final String DEFAULT_VALIDATION_MESSAGE = "Invalid value";

    private final ProblemDetailFactory problemDetailFactory;

    /**
     * @param problemDetailFactory builds every problem response
     */
    public GlobalExceptionHandler(ProblemDetailFactory problemDetailFactory) {
        this.problemDetailFactory = Objects.requireNonNull(problemDetailFactory, "problemDetailFactory must not be null");
    }

    /**
     * Handles expected failures raised by service code.
     *
     * @param ex      the domain exception
     * @param request the current request
     * @return the problem response, with the status derived from the error category
     */
    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ProblemDetail> handleDomainException(DomainException ex, HttpServletRequest request) {
        logDomainException(ex, request);
        return respond(problemDetailFactory.create(ex.getErrorCode(), ex.getDetail()), request);
    }

    /**
     * Handles authorisation failures raised by method security, e.g. {@code @PreAuthorize}.
     *
     * @param ex      the access-denied exception
     * @param request the current request
     * @return a 403 problem response
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        log.info("Access denied on {} {}", request.getMethod(), request.getRequestURI());
        return respond(problemDetailFactory.create(CommonErrorCode.ACCESS_DENIED, null), request);
    }

    /**
     * Handles an unknown property in a {@code sort} parameter.
     *
     * @param ex      the property reference exception
     * @param request the current request
     * @return a 400 problem response
     */
    @ExceptionHandler(PropertyReferenceException.class)
    public ResponseEntity<ProblemDetail> handleInvalidSort(PropertyReferenceException ex, HttpServletRequest request) {
        log.debug("Invalid sort property '{}' on {} {}", ex.getPropertyName(), request.getMethod(), request.getRequestURI());
        String detail = "Unknown sort property: " + ex.getPropertyName();
        return respond(problemDetailFactory.create(CommonErrorCode.INVALID_SORT, detail), request);
    }

    /**
     * Handles any exception not handled elsewhere.
     *
     * @param ex      the unexpected exception
     * @param request the current request
     * @return a 500 problem response that reveals nothing about the failure
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception on {} {}", request.getMethod(), request.getRequestURI(), ex);
        return respond(problemDetailFactory.create(CommonErrorCode.INTERNAL_ERROR, INTERNAL_ERROR_DETAIL), request);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            @NonNull Exception ex, Object body, @NonNull HttpHeaders headers, @NonNull HttpStatusCode statusCode, @NonNull WebRequest request) {

        ResponseEntity<Object> response = super.handleExceptionInternal(ex, body, headers, statusCode, request);
        if (response != null && response.getBody() instanceof ProblemDetail problem) {
            problemDetailFactory.enrich(problem, codeFor(ex, statusCode));
            addValidationErrors(problem, ex);
            if (request instanceof ServletWebRequest servletRequest) {
                problem.setInstance(URI.create(servletRequest.getRequest().getRequestURI()));
            }
            log.debug("Request failed with {}: {}", statusCode.value(), ex.getMessage());
        }
        return response;
    }

    private ResponseEntity<ProblemDetail> respond(ProblemDetail problem, HttpServletRequest request) {
        problem.setInstance(URI.create(request.getRequestURI()));
        return ResponseEntity.status(problem.getStatus()).body(problem);
    }

    private static ErrorCode codeFor(Exception ex, HttpStatusCode status) {
        if (ex instanceof MethodArgumentNotValidException || ex instanceof HandlerMethodValidationException) {
            return CommonErrorCode.VALIDATION_FAILED;
        }
        return switch (status.value()) {
            case 404 -> CommonErrorCode.RESOURCE_NOT_FOUND;
            case 405 -> CommonErrorCode.METHOD_NOT_ALLOWED;
            case 415 -> CommonErrorCode.UNSUPPORTED_MEDIA_TYPE;
            default -> status.is5xxServerError() ? CommonErrorCode.INTERNAL_ERROR : CommonErrorCode.MALFORMED_REQUEST;
        };
    }

    private static void addValidationErrors(ProblemDetail problem, Exception ex) {
        List<ValidationError> errors = switch (ex) {
            case MethodArgumentNotValidException e -> fromErrors(e.getBindingResult());
            case HandlerMethodValidationException e -> fromMethodValidation(e);
            default -> List.of();
        };
        if (!errors.isEmpty()) {
            problem.setProperty(ERRORS_PROPERTY, errors);
        }
    }

    private static List<ValidationError> fromErrors(Errors errors) {
        List<ValidationError> result = new ArrayList<>();
        errors.getFieldErrors().forEach(error ->
                result.add(new ValidationError(error.getField(), messageOf(error))));
        errors.getGlobalErrors().forEach(error ->
                result.add(new ValidationError(null, messageOf(error))));
        return result;
    }

    private static List<ValidationError> fromMethodValidation(HandlerMethodValidationException ex) {
        List<ValidationError> result = new ArrayList<>();
        for (ParameterValidationResult parameterResult : ex.getParameterValidationResults()) {
            if (parameterResult instanceof ParameterErrors parameterErrors) {
                result.addAll(fromErrors(parameterErrors));
                continue;
            }
            String parameterName = parameterResult.getMethodParameter().getParameterName();
            parameterResult.getResolvableErrors().forEach(error ->
                    result.add(new ValidationError(parameterName, messageOf(error))));
        }
        return result;
    }

    private static String messageOf(MessageSourceResolvable error) {
        return error.getDefaultMessage() != null ? error.getDefaultMessage() : DEFAULT_VALIDATION_MESSAGE;
    }

    private static void logDomainException(DomainException ex, HttpServletRequest request) {
        ErrorCode code = ex.getErrorCode();
        switch (code.category()) {
            case INTERNAL -> log.error("{} on {} {}", code.code(), request.getMethod(), request.getRequestURI(), ex);
            case ACCESS_DENIED -> log.info("{} on {} {}", code.code(), request.getMethod(), request.getRequestURI());
            default ->
                    log.debug("{} on {} {}: {}", code.code(), request.getMethod(), request.getRequestURI(), ex.getMessage());
        }
    }
}