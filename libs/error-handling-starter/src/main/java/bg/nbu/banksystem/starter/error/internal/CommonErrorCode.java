package bg.nbu.banksystem.starter.error.internal;

import bg.nbu.banksystem.starter.error.ErrorCategory;
import bg.nbu.banksystem.starter.error.ErrorCode;

/**
 * Problems raised by the error-handling starter itself, independent of any domain.
 *
 * <p>
 * For problems Spring MVC has already built (e.g. 405, 415), the factory's {@code enrich}
 * keeps Spring's status, so the category here only affects problems created from scratch.
 * </p>
 */
public enum CommonErrorCode implements ErrorCode {

    VALIDATION_FAILED(ErrorCategory.VALIDATION, "Validation failed"),
    MALFORMED_REQUEST(ErrorCategory.VALIDATION, "Malformed request"),
    METHOD_NOT_ALLOWED(ErrorCategory.VALIDATION, "Method not allowed"),
    UNSUPPORTED_MEDIA_TYPE(ErrorCategory.VALIDATION, "Unsupported media type"),
    RESOURCE_NOT_FOUND(ErrorCategory.NOT_FOUND, "Resource not found"),
    INVALID_SORT(ErrorCategory.VALIDATION, "Invalid sort property"),
    UNAUTHENTICATED(ErrorCategory.UNAUTHENTICATED, "Authentication required"),
    ACCESS_DENIED(ErrorCategory.ACCESS_DENIED, "Access denied"),
    INTERNAL_ERROR(ErrorCategory.INTERNAL, "Internal server error");

    private final ErrorCategory category;
    private final String title;

    CommonErrorCode(ErrorCategory category, String title) {
        this.category = category;
        this.title = title;
    }

    @Override
    public String code() {
        return name();
    }

    @Override
    public ErrorCategory category() {
        return category;
    }

    @Override
    public String title() {
        return title;
    }
}