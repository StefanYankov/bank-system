package bg.nbu.banksystem.employee.config;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Maps Keycloak realm roles from the {@code realm_access.roles} claim
 * to Spring Security authorities prefixed with {@code ROLE_}.
 *
 * <p>
 * Missing or malformed claims produce an empty authority set rather than
 * an exception, so a token without realm roles authenticates but is not
 * authorized for role-protected operations.
 * </p>
 */
public class KeycloakRealmRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    private static final String REALM_ACCESS_CLAIM = "realm_access";
    private static final String ROLES_KEY = "roles";
    private static final String ROLE_PREFIX = "ROLE_";

    /**
     * Extracts realm roles from the given token.
     *
     * @param jwt the validated access token
     * @return the realm roles as {@code ROLE_}-prefixed authorities; empty if none are present
     */
    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        Object realmAccess = jwt.getClaim(REALM_ACCESS_CLAIM);
        if (!(realmAccess instanceof Map<?, ?> realmAccessMap)) {
            return List.of();
        }

        if (!(realmAccessMap.get(ROLES_KEY) instanceof Collection<?> roles)) {
            return List.of();
        }

        return roles
                .stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .<GrantedAuthority>map(role -> new SimpleGrantedAuthority(ROLE_PREFIX + role))
                .toList();
    }
}