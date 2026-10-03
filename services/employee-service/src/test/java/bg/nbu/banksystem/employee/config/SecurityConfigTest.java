package bg.nbu.banksystem.employee.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
@Import(SecurityConfig.class)
@DisplayName("SecurityConfig")
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    // --- Public endpoints ---

    @ParameterizedTest(name = "{0} is reachable without a token")
    @ValueSource(strings = {"/actuator/health", "/v3/api-docs", "/swagger-ui.html"})
    @DisplayName("permits public endpoints without authentication")
    void permitsPublicEndpointsWithoutAuthentication(String path) throws Exception {
        // Act & Assert
        mockMvc.perform(get(path))
                .andExpect(isNotRejectedBySecurity());
    }

    // --- Protected endpoints ---

    @ParameterizedTest(name = "{0} rejects a request without a token")
    @ValueSource(strings = {"/api/employees", "/actuator/metrics"})
    @DisplayName("rejects unauthenticated requests to protected endpoints")
    void rejectsUnauthenticatedRequests(String path) throws Exception {
        // Act & Assert
        mockMvc.perform(get(path))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("accepts a request with a valid token")
    void acceptsValidToken() throws Exception {
        // Arrange
        when(jwtDecoder.decode(anyString())).thenReturn(clerkToken());

        // Act & Assert
        mockMvc.perform(get("/api/employees").header(HttpHeaders.AUTHORIZATION, "Bearer valid-token"))
                .andExpect(isNotRejectedBySecurity());
    }

    @Test
    @DisplayName("rejects a request with an invalid token")
    void rejectsInvalidToken() throws Exception {
        // Arrange
        when(jwtDecoder.decode(anyString())).thenThrow(new BadJwtException("invalid token"));

        // Act & Assert
        mockMvc.perform(get("/api/employees").header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, containsString("invalid_token")));
    }

    // --- Helpers ---

    private static ResultMatcher isNotRejectedBySecurity() {
        return result -> assertThat(result.getResponse().getStatus()).isNotIn(401, 403);
    }

    private static Jwt clerkToken() {
        return Jwt.withTokenValue("valid-token")
                .header("alg", "none")
                .subject("clerk-subject-id")
                .claim("realm_access", Map.of("roles", List.of("CLERK")))
                .build();
    }
}