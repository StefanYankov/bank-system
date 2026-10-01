package bg.nbu.banksystem.employee.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configures employee-service as a stateless OAuth2 resource server.
 *
 * <p>
 *     Requests are authenticated with Keycloak-issued bearer tokens. Issuer,
 * signature, expiry and audience are validated from {@code application.yaml};
 * realm roles are mapped to authorities by {@link KeycloakRealmRoleConverter}.
 * </p>
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    /**
     * Public, unauthenticated system endpoints (API docs + monitoring).
     * Extracted to a constant for readability and reuse.
     */
    private static final String[] PUBLIC_MATCHERS = {
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/actuator/health/**"
    };

    /**
     * Defines the HTTP security rules for the service.
     *
     * @param http the security builder provided by Spring
     * @return the configured filter chain
     * @throws Exception if the security configuration cannot be built
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth ->
                        auth
                                .requestMatchers(PUBLIC_MATCHERS).permitAll()
                                .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                );

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
