package bg.nbu.banksystem.starter.error.internal;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.util.Objects;

/**
 * Responds to requests refused by URL authorisation rules in the security filter chain
 * with a 403 problem response.
 */
public final class ProblemDetailAccessDeniedHandler implements AccessDeniedHandler {

    private static final Logger log = LoggerFactory.getLogger(ProblemDetailAccessDeniedHandler.class);

    private final ProblemDetailFactory problemDetailFactory;
    private final ProblemResponseWriter writer;

    /**
     * @param problemDetailFactory builds the problem body
     * @param jsonMapper           serialises the body
     */
    public ProblemDetailAccessDeniedHandler(ProblemDetailFactory problemDetailFactory, JsonMapper jsonMapper) {
        this.problemDetailFactory = Objects.requireNonNull(problemDetailFactory, "problemDetailFactory must not be null");
        this.writer = new ProblemResponseWriter(jsonMapper);
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        log.info("Access denied on {} {}", request.getMethod(), request.getRequestURI());
        writer.write(problemDetailFactory.create(CommonErrorCode.ACCESS_DENIED, null), request, response);
    }
}