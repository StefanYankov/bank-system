package bg.nbu.banksystem.starter.error.internal;

import bg.nbu.banksystem.starter.error.ErrorCategory;
import org.springframework.http.HttpStatus;

/**
 * Maps an {@link ErrorCategory} to the HTTP status reported to clients.
 *
 * <p>
 * This is the only place in the system where categories are translated to HTTP.
 * </p
 */
public final class CategoryStatusMapper {

    /**
     * @param category the error category
     * @return the HTTP status for that category
     */
    public HttpStatus statusFor(ErrorCategory category) {
        return switch (category) {
            case VALIDATION -> HttpStatus.BAD_REQUEST;
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case CONFLICT -> HttpStatus.CONFLICT;
            case RULE_VIOLATION -> HttpStatus.UNPROCESSABLE_CONTENT;
            case UNAUTHENTICATED -> HttpStatus.UNAUTHORIZED;
            case ACCESS_DENIED -> HttpStatus.FORBIDDEN;
            case INTERNAL -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
}