package bg.nbu.banksystem.starter.error;

/**
 * The broad kind of failure an {@link ErrorCode} belongs to.
 *
 * <p>
 * Domain code chooses a category; the web layer decides how each category is reported.
 * Categories carry no transport details such as HTTP status.
 * </p>
 */
public enum ErrorCategory {

    /**
     * The request is malformed or fails validation.
     */
    VALIDATION,

    /**
     * A referenced resource does not exist.
     */
    NOT_FOUND,

    /**
     * The request clashes with the current state, e.g. a duplicate or a concurrent update.
     */
    CONFLICT,

    /**
     * The request is valid but a business rule refuses it.
     */
    RULE_VIOLATION,

    /**
     * The caller is not authenticated.
     */
    UNAUTHENTICATED,

    /**
     * The caller is authenticated but not allowed to perform the action.
     */
    ACCESS_DENIED,

    /**
     * A required downstream dependency is temporarily unavailable.
     */
    UNAVAILABLE,
    
    /**
     * An unexpected server-side failure.
     */
    INTERNAL
}
