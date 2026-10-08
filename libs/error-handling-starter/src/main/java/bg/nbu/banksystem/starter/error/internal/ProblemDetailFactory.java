package bg.nbu.banksystem.starter.error.internal;

import bg.nbu.banksystem.starter.error.ErrorCode;
import org.springframework.http.ProblemDetail;

import java.net.URI;
import java.util.Locale;
import java.util.Objects;

/**
 * Builds RFC 9457 problem details with the fields shared by every bank-system service.
 */
public final class ProblemDetailFactory {

    private static final String CODE_PROPERTY = "code";
    private static final String TRACE_ID_PROPERTY = "traceId";

    private final URI typeBaseUri;
    private final CategoryStatusMapper statusMapper;
    private final TraceIdProvider traceIdProvider;

    /**
     * @param typeBaseUri     base for problem {@code type} URIs,
     *                        e.g. {@code https://stefanyankov.github.io/bank-system/problems/}
     * @param statusMapper    maps error categories to HTTP statuses
     * @param traceIdProvider source of the current trace id
     */
    public ProblemDetailFactory(URI typeBaseUri, CategoryStatusMapper statusMapper, TraceIdProvider traceIdProvider) {
        Objects.requireNonNull(typeBaseUri, "typeBaseUri must not be null");
        this.statusMapper = Objects.requireNonNull(statusMapper, "statusMapper must not be null");
        this.traceIdProvider = Objects.requireNonNull(traceIdProvider, "traceIdProvider must not be null");
        String base = typeBaseUri.toString();
        this.typeBaseUri = URI.create(base.endsWith("/") ? base : base + "/");
    }

    /**
     * Creates a new problem for the given error code.
     *
     * @param errorCode the kind of problem
     * @param detail    client-safe explanation of this occurrence; omitted when {@code null}
     * @return the problem detail
     */
    public ProblemDetail create(ErrorCode errorCode, String detail) {
        ProblemDetail problem = ProblemDetail.forStatus(statusMapper.statusFor(errorCode.category()));
        problem.setDetail(detail);
        return enrich(problem, errorCode);
    }

    /**
     * Adds the shared fields to a problem detail built elsewhere, such as one Spring MVC
     * created for a framework exception. The existing status and detail are kept.
     *
     * @param problem   the problem detail to enrich
     * @param errorCode the kind of problem
     * @return the same problem detail, for chaining
     */
    public ProblemDetail enrich(ProblemDetail problem, ErrorCode errorCode) {
        problem.setType(typeFor(errorCode));
        problem.setTitle(errorCode.title());
        problem.setProperty(CODE_PROPERTY, errorCode.code());
        traceIdProvider.currentTraceId()
                .ifPresent(traceId -> problem.setProperty(TRACE_ID_PROPERTY, traceId));
        return problem;
    }

    private URI typeFor(ErrorCode errorCode) {
        String slug = errorCode.code().toLowerCase(Locale.ROOT).replace('_', '-');
        return typeBaseUri.resolve(slug);
    }
}