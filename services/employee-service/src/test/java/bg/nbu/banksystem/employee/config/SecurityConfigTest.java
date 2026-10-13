package bg.nbu.banksystem.employee.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies the service's security infrastructure: token decoding, Keycloak realm-role
 * mapping, the filter chain and the 401/403 problem responses.
 *
 * <p>
 * Uses a test-only probe controller, so it is independent of application controllers.
 * Endpoint-specific role policies are tested next to each controller.
 * </p>
 */
@WebMvcTest(controllers = SecurityConfigTest.SecurityProbeController.class)
@Import({SecurityConfig.class, SecurityConfigTest.SecurityProbeController.class})
@DisplayName("SecurityConfig (web slice)")
class SecurityConfigTest {

    private static final String PROTECTED_PATH = "/test/protected";
    private static final String ADMIN_PATH = "/test/admin";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Nested
    @DisplayName("Public endpoints")
    class PublicEndpoints {

        @ParameterizedTest(name = "Happy Path: Should permit {0} without a token")
        @ValueSource(strings = {"/actuator/health", "/v3/api-docs", "/swagger-ui.html"})
        @DisplayName("Happy Path: Should permit public endpoints without a token")
        void shouldPermitPublicEndpoints(String path) throws Exception {
            // Act & Assert
            mockMvc.perform(get(path))
                    .andExpect(isNotRejectedBySecurity());
        }
    }

    @Nested
    @DisplayName("Missing or invalid token")
    class MissingOrInvalidToken {

        @ParameterizedTest(name = "Error Case: Should return a 401 problem for {0} without a token")
        @ValueSource(strings = {PROTECTED_PATH, "/actuator/metrics"})
        @DisplayName("Error Case: Should return a 401 problem without a token")
        void shouldReturnProblemWithoutToken(String path) throws Exception {
            // Act & Assert
            mockMvc.perform(get(path))
                    .andExpect(status().isUnauthorized())
                    .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, startsWith("Bearer")))
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"))
                    .andExpect(jsonPath("$.instance").value(path));
        }

        @Test
        @DisplayName("Error Case: Should keep the invalid_token challenge and hide parser details")
        void shouldReturnProblemForInvalidToken() throws Exception {
            // Arrange
            when(jwtDecoder.decode(anyString())).thenThrow(new BadJwtException("parser-detail-xyz"));

            // Act & Assert
            mockMvc.perform(get(PROTECTED_PATH).header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, containsString("invalid_token")))
                    .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"))
                    .andExpect(content().string(not(containsString("parser-detail-xyz"))));
        }
    }

    @Nested
    @DisplayName("Realm role mapping")
    class RealmRoleMapping {

        @Test
        @DisplayName("Happy Path: Should authenticate a valid token")
        void shouldAuthenticateValidToken() throws Exception {
            // Arrange
            when(jwtDecoder.decode(anyString())).thenReturn(tokenWithRealmRole("CLERK"));

            // Act & Assert
            mockMvc.perform(get(PROTECTED_PATH).header(HttpHeaders.AUTHORIZATION, "Bearer valid-token"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Happy Path: Should grant ROLE_ADMIN from the ADMIN realm role")
        void shouldGrantAdminFromRealmRole() throws Exception {
            // Arrange
            when(jwtDecoder.decode(anyString())).thenReturn(tokenWithRealmRole("ADMIN"));

            // Act & Assert
            mockMvc.perform(get(ADMIN_PATH).header(HttpHeaders.AUTHORIZATION, "Bearer admin-token"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Error Case: Should return a 403 problem when the realm role is insufficient")
        void shouldReturnProblemForInsufficientRole() throws Exception {
            // Arrange
            when(jwtDecoder.decode(anyString())).thenReturn(tokenWithRealmRole("CLERK"));

            // Act & Assert
            mockMvc.perform(get(ADMIN_PATH).header(HttpHeaders.AUTHORIZATION, "Bearer clerk-token"))
                    .andExpect(status().isForbidden())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
        }
    }

    private static ResultMatcher isNotRejectedBySecurity() {
        return result -> assertThat(result.getResponse().getStatus()).isNotIn(401, 403);
    }

    private static Jwt tokenWithRealmRole(String role) {
        return Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("probe-subject")
                .claim("realm_access", Map.of("roles", List.of(role)))
                .build();
    }

    /**
     * Test-only endpoints that exercise the security configuration without application logic.
     */
    @RestController
    static class SecurityProbeController {

        @GetMapping(PROTECTED_PATH)
        String protectedEndpoint() {
            return "ok";
        }

        @GetMapping(ADMIN_PATH)
        @PreAuthorize("hasRole('ADMIN')")
        String adminEndpoint() {
            return "ok";
        }
    }
}