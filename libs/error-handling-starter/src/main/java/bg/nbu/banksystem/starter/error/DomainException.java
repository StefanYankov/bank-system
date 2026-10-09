package bg.nbu.banksystem.starter.error;

import jakarta.annotation.Nullable;

import java.util.Objects;

/**
 * An expected failure reported to the client as a problem response.
 *
 * <p>
 * The {@code detail} is shown to clients and must not contain personal or sensitive data.
 * Context needed only for diagnosis belongs in the {@code cause} or in logs.
 * </p>
 */
public class DomainException extends RuntimeException {

    private final transient ErrorCode errorCode;
    private final String detail;

    /**
     * @param errorCode the kind of problem
     */
    public DomainException(ErrorCode errorCode) {
        this(errorCode, null, null);
    }

    /**
     * @param errorCode the kind of problem
     * @param detail    client-safe explanation of this occurrence; may be {@code null}
     */
    public DomainException(ErrorCode errorCode, String detail) {
        this(errorCode, detail, null);
    }

    /**
     * @param errorCode the kind of problem
     * @param detail    client-safe explanation of this occurrence; may be {@code null}
     * @param cause     the underlying exception, kept for logs only
     */
    public DomainException(ErrorCode errorCode, String detail, Throwable cause) {
        super(messageOf(errorCode, detail), cause);
        this.errorCode = errorCode;
        this.detail = detail;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    /**
     * @return the client-safe detail, or {@code null} if none was given
     */
    public String getDetail() {
        return detail;
    }

    private static String messageOf(ErrorCode errorCode, @Nullable String detail){
        Objects.requireNonNull(errorCode, "errorCode must not be null");
        return detail != null ? detail : errorCode.title();
    }
}