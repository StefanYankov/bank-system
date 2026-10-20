package bg.nbu.banksystem.customer.persistence;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

/**
 * Enables Spring Data JPA auditing for persistence entities.
 *
 * <p>
 * Audit timestamps are produced from a shared UTC clock. Audit actors use the
 * authenticated JWT subject when available and {@code SYSTEM} for operations without
 * an authenticated end-user.
 * </p>
 */
@Configuration
@EnableJpaAuditing(
        auditorAwareRef = "auditorAware",
        dateTimeProviderRef = "auditingDateTimeProvider"
)
public class JpaAuditingConfig {

    /**
     * Reserved actor used when an operation is not performed by an authenticated user.
     */
    public static final String SYSTEM_ACTOR = "SYSTEM";

    private static String resolveCurrentAuditor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (!(authentication instanceof JwtAuthenticationToken jwtAuthentication)
                || !jwtAuthentication.isAuthenticated()) {
            return SYSTEM_ACTOR;
        }

        String subject = jwtAuthentication.getToken().getSubject();

        if (subject == null || subject.isBlank()) {
            return SYSTEM_ACTOR;
        }

        return subject;
    }

    /**
     * Provides the application clock used for persistence auditing.
     *
     * @return the system clock in UTC
     */
    @Bean
    public Clock applicationClock() {
        return Clock.systemUTC();
    }

    /**
     * Provides the current time for {@code @CreatedDate} and
     * {@code @LastModifiedDate} fields.
     *
     * @param applicationClock the application clock
     * @return the auditing time provider
     */
    @Bean
    public DateTimeProvider auditingDateTimeProvider(Clock applicationClock) {
        return () -> Optional.of(Instant.now(applicationClock));
    }

    /**
     * Resolves the actor stored in {@code @CreatedBy} and
     * {@code @LastModifiedBy} fields.
     *
     * @return an auditor provider using the JWT subject or {@code SYSTEM}
     */
    @Bean
    public AuditorAware<String> auditorAware() {
        return () -> Optional.of(resolveCurrentAuditor());
    }
}