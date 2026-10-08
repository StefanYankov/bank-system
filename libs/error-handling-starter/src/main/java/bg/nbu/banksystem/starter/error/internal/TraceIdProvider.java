package bg.nbu.banksystem.starter.error.internal;

import java.util.Optional;

/**
 * Supplies the trace id of the current request, when tracing is active.
 */
@FunctionalInterface
public interface TraceIdProvider {

    /** Provider used when no tracing is configured. */
    TraceIdProvider NONE = Optional::empty;

    /**
     * @return the current trace id, or empty if there is none
     */
    Optional<String> currentTraceId();
}