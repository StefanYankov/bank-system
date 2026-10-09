package bg.nbu.banksystem.starter.error;

/**
 * A specific kind of problem a service can report, usually implemented as one enum per service.
 *
 * <p>The problem {@code type} URI is derived from {@link #code()}, so a code must never be
 * renamed once published.
 */
public interface ErrorCode {

    /**
     * @return a stable, domain-prefixed identifier, e.g. {@code EMPLOYEE_NOT_FOUND}
     */
    String code();

    /**
     * @return the category that determines how the problem is reported
     */
    ErrorCategory category();

    /**
     * @return a short summary that is the same for every occurrence of this problem
     */
    String title();
}