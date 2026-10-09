package bg.nbu.banksystem.employee.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;

/**
 * Configures employee-service as a stateless OAuth2 resource server.
 *
 * <p>
 * Requests are authenticated with Keycloak-issued bearer tokens. Issuer, signature, expiry
 * and audience are validated from {@code application.yaml}; realm roles are mapped to
 * authorities by {@link KeycloakRealmRoleConverter}. 401 and 403 responses are RFC 9457
 * problem details supplied by the error-handling starter.
 * </p>
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    /**
     * Public, unauthenticated system endpoints (API docs + monitoring).
     * Extracted to a constant for readability and reuse.
     */
    private static final String[] PUBLIC_ENDPOINTS = {
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/actuator/health/**"
    };

    /**
     * Defines the HTTP security rules for the service.
     *
     * @param http                the security builder provided by Spring
     * @param entryPoint          writes 401 problem responses
     * @param accessDeniedHandler writes 403 problem responses
     * @return the configured filter chain
     * @throws Exception if the security configuration cannot be built
     */
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
                                            AuthenticationEntryPoint entryPoint,
                                            AccessDeniedHandler accessDeniedHandler) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .oauth2ResourceServer(oauth2 -> oauth2
                        .authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));

        return http.build();
    }

    /**
     * Builds the converter that turns a validated JWT into an {@code Authentication},
     * using realm roles as authorities.
     *
     * @return the configured converter
     */
    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new KeycloakRealmRoleConverter());
        return converter;
    }
}