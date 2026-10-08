package bg.nbu.banksystem.starter.error.internal;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.web.AuthenticationEntryPoint;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.util.Objects;

/**
 * Responds to unauthenticated requests rejected by the security filter chain with a 401
 * problem response, keeping the standard {@code WWW-Authenticate} bearer challenge.
 */
public final class ProblemDetailAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final Logger log = LoggerFactory.getLogger(ProblemDetailAuthenticationEntryPoint.class);

    private final AuthenticationEntryPoint bearerEntryPoint = new BearerTokenAuthenticationEntryPoint();
    private final ProblemDetailFactory problemDetailFactory;
    private final ProblemResponseWriter writer;

    /**
     * @param problemDetailFactory builds the problem body
     * @param jsonMapper           serialises the body
     */
    public ProblemDetailAuthenticationEntryPoint(ProblemDetailFactory problemDetailFactory, JsonMapper jsonMapper) {
        this.problemDetailFactory = Objects.requireNonNull(problemDetailFactory, "problemDetailFactory must not be null");
        this.writer = new ProblemResponseWriter(jsonMapper);
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException, ServletException {
        log.debug("Unauthenticated request to {} {}: {}", request.getMethod(), request.getRequestURI(),
                authException.getMessage());
        bearerEntryPoint.commence(request, response, authException);
        writer.write(problemDetailFactory.create(CommonErrorCode.UNAUTHENTICATED, null), request, response);
    }
}