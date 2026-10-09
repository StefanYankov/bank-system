package bg.nbu.banksystem.starter.error.internal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ProblemDetailAuthenticationEntryPoint")
class ProblemDetailAuthenticationEntryPointTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final ProblemDetailAuthenticationEntryPoint entryPoint = new ProblemDetailAuthenticationEntryPoint(
            new ProblemDetailFactory(URI.create("https://example.test/problems/"), new CategoryStatusMapper(),
                    TraceIdProvider.NONE),
            JSON);

    // --- Happy path ---

    @Test
    @DisplayName("writes a 401 problem when no token is sent")
    void writesProblemWhenTokenMissing() throws Exception {
        // Arrange
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/employees");
        MockHttpServletResponse response = new MockHttpServletResponse();

        // Act
        entryPoint.commence(request, response, new InsufficientAuthenticationException("Full authentication required"));

        // Assert
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        assertThat(response.getHeader(HttpHeaders.WWW_AUTHENTICATE)).startsWith("Bearer");
        Map<String, Object> body = bodyOf(response);
        assertThat(body).containsEntry("code", "UNAUTHENTICATED")
                .containsEntry("instance", "/api/employees")
                .containsEntry("status", 401);
    }

    // --- Error case ---

    @Test
    @DisplayName("keeps the invalid_token challenge for a rejected token")
    void keepsInvalidTokenChallenge() throws Exception {
        // Arrange
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/employees");
        MockHttpServletResponse response = new MockHttpServletResponse();

        // Act
        entryPoint.commence(request, response, new InvalidBearerTokenException("Jwt expired at 2026-10-07"));

        // Assert
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getHeader(HttpHeaders.WWW_AUTHENTICATE)).contains("invalid_token");
        assertThat(bodyOf(response)).containsEntry("code", "UNAUTHENTICATED");
    }

    // --- Edge case ---

    @Test
    @DisplayName("does not expose token parser details in the body")
    void doesNotLeakExceptionMessage() throws Exception {
        // Arrange
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/employees");
        MockHttpServletResponse response = new MockHttpServletResponse();

        // Act
        entryPoint.commence(request, response, new InvalidBearerTokenException("Jwt expired at 2026-10-07"));

        // Assert
        assertThat(response.getContentAsString()).doesNotContain("Jwt expired");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> bodyOf(MockHttpServletResponse response) throws Exception {
        return JSON.readValue(response.getContentAsString(), Map.class);
    }
}