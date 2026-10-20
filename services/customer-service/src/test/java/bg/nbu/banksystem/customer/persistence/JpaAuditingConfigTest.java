package bg.nbu.banksystem.customer.persistence;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAccessor;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JpaAuditingConfig (unit)")
class JpaAuditingConfigTest {

    private static final Instant FIXED_INSTANT =
            Instant.parse("2026-10-10T08:00:00Z");

    private static final String SUBJECT = "keycloak-user-subject";

    private final JpaAuditingConfig config = new JpaAuditingConfig();

    private static Jwt jwtWithSubject(String subject) {
        return Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .subject(subject)
                .build();
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("Audit time")
    class AuditTime {

        @Test
        @DisplayName("Happy Path: Should return the current instant from the supplied clock")
        void shouldReturnInstantFromSuppliedClock() {
            // Arrange
            Clock fixedClock = Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);
            DateTimeProvider provider =
                    config.auditingDateTimeProvider(fixedClock);

            // Act
            Optional<TemporalAccessor> currentTime = provider.getNow();

            // Assert
            assertThat(currentTime).contains(FIXED_INSTANT);
        }
    }

    @Nested
    @DisplayName("Audit actor")
    class AuditActor {

        @Test
        @DisplayName("Happy Path: Should use the JWT subject for an authenticated request")
        void shouldUseJwtSubject() {
            // Arrange
            JwtAuthenticationToken authentication =
                    new JwtAuthenticationToken(
                            jwtWithSubject(SUBJECT),
                            List.of()
                    );

            SecurityContextHolder.getContext()
                    .setAuthentication(authentication);

            AuditorAware<String> auditorAware = config.auditorAware();

            // Act
            Optional<String> auditor = auditorAware.getCurrentAuditor();

            // Assert
            assertThat(auditor).contains(SUBJECT);
        }

        @Test
        @DisplayName("Edge Case: Should use SYSTEM when no authentication exists")
        void shouldUseSystemWithoutAuthentication() {
            // Arrange
            SecurityContextHolder.clearContext();
            AuditorAware<String> auditorAware = config.auditorAware();

            // Act
            Optional<String> auditor = auditorAware.getCurrentAuditor();

            // Assert
            assertThat(auditor)
                    .contains(JpaAuditingConfig.SYSTEM_ACTOR);
        }

        @Test
        @DisplayName("Edge Case: Should use SYSTEM for an unauthenticated JWT")
        void shouldUseSystemForUnauthenticatedJwt() {
            // Arrange
            JwtAuthenticationToken authentication =
                    new JwtAuthenticationToken(jwtWithSubject(SUBJECT));

            SecurityContextHolder.getContext()
                    .setAuthentication(authentication);

            AuditorAware<String> auditorAware = config.auditorAware();

            // Act
            Optional<String> auditor = auditorAware.getCurrentAuditor();

            // Assert
            assertThat(auditor)
                    .contains(JpaAuditingConfig.SYSTEM_ACTOR);
        }

        @Test
        @DisplayName("Edge Case: Should use SYSTEM for anonymous authentication")
        void shouldUseSystemForAnonymousAuthentication() {
            // Arrange
            AnonymousAuthenticationToken authentication =
                    new AnonymousAuthenticationToken(
                            "test-key",
                            "anonymousUser",
                            List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))
                    );

            SecurityContextHolder.getContext()
                    .setAuthentication(authentication);

            AuditorAware<String> auditorAware = config.auditorAware();

            // Act
            Optional<String> auditor = auditorAware.getCurrentAuditor();

            // Assert
            assertThat(auditor)
                    .contains(JpaAuditingConfig.SYSTEM_ACTOR);
        }

        @Test
        @DisplayName("Edge Case: Should use SYSTEM when the authenticated JWT subject is blank")
        void shouldUseSystemForBlankJwtSubject() {
            // Arrange
            JwtAuthenticationToken authentication =
                    new JwtAuthenticationToken(
                            jwtWithSubject("   "),
                            List.of()
                    );

            SecurityContextHolder.getContext()
                    .setAuthentication(authentication);

            AuditorAware<String> auditorAware = config.auditorAware();

            // Act
            Optional<String> auditor = auditorAware.getCurrentAuditor();

            // Assert
            assertThat(auditor)
                    .contains(JpaAuditingConfig.SYSTEM_ACTOR);
        }
    }
}