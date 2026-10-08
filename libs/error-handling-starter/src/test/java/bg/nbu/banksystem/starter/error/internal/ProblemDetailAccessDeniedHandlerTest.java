package bg.nbu.banksystem.starter.error.internal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ProblemDetailAccessDeniedHandler")
class ProblemDetailAccessDeniedHandlerTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final ProblemDetailAccessDeniedHandler handler = new ProblemDetailAccessDeniedHandler(
            new ProblemDetailFactory(URI.create("https://example.test/problems/"), new CategoryStatusMapper(),
                    TraceIdProvider.NONE),
            JSON);

    @Test
    @DisplayName("writes a 403 problem response")
    void writesForbiddenProblem() throws Exception {
        // Arrange
        MockHttpServletRequest request = new MockHttpServletRequest("DELETE", "/api/employees/42");
        MockHttpServletResponse response = new MockHttpServletResponse();

        // Act
        handler.handle(request, response, new AccessDeniedException("denied"));

        // Assert
        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        @SuppressWarnings("unchecked")
        Map<String, Object> body = JSON.readValue(response.getContentAsString(), Map.class);
        assertThat(body).containsEntry("code", "ACCESS_DENIED")
                .containsEntry("instance", "/api/employees/42");
    }
}