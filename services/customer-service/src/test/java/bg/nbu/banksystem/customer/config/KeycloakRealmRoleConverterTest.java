package bg.nbu.banksystem.customer.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("KeycloakRealmRoleConverter")
class KeycloakRealmRoleConverterTest {

    private final KeycloakRealmRoleConverter converter = new KeycloakRealmRoleConverter();

    // --- Happy path ---

    static Stream<Arguments> tokensWithoutUsableRoles() {
        return Stream.of(Arguments.of("no realm_access claim", Map.of()),
                Arguments.of("realm_access is not a map", Map.of("realm_access", "not-a-map")),
                Arguments.of("realm_access has no roles key", Map.of("realm_access", Map.of())),
                Arguments.of("roles is not a collection", Map.of("realm_access", Map.of("roles", "CLERK"))),
                Arguments.of("roles is empty", Map.of("realm_access", Map.of("roles", List.of()))));
    }

    // --- Error cases ---

    private static Jwt tokenWithClaims(Map<String, ?> claims) {
        return Jwt.withTokenValue("test-token").header("alg", "none").subject("test-user")
                .claims(existing -> existing.putAll(claims)).build();
    }

    @Test
    @DisplayName("maps realm roles to ROLE_-prefixed authorities")
    void mapsRealmRolesToAuthorities() {
        // Arrange
        Jwt jwt = tokenWithClaims(
                Map.of("realm_access", Map.of("roles", List.of("CLERK", "LOAN_OFFICER", "default-roles-bank-system"))));

        // Act
        Collection<GrantedAuthority> authorities = converter.convert(jwt);

        // Assert
        assertThat(authorities).extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_CLERK", "ROLE_LOAN_OFFICER", "ROLE_default-roles-bank-system");
    }

    // --- Edge case ---

    @ParameterizedTest(name = "{0}")
    @MethodSource("tokensWithoutUsableRoles")
    @DisplayName("returns no authorities when realm roles are missing or malformed")
    void returnsNoAuthoritiesWhenRolesAreMissingOrMalformed(String scenario, Map<String, ?> claims) {
        // Arrange
        Jwt jwt = tokenWithClaims(claims);

        // Act
        Collection<GrantedAuthority> authorities = converter.convert(jwt);

        // Assert
        assertThat(authorities).isEmpty();
    }

    // --- Helper ----

    @Test
    @DisplayName("ignores role entries that are not strings")
    void ignoresNonStringRoleEntries() {
        // Arrange
        Jwt jwt = tokenWithClaims(Map.of("realm_access", Map.of("roles", List.of("CLERK", 42, true))));

        // Act
        Collection<GrantedAuthority> authorities = converter.convert(jwt);

        // Assert
        assertThat(authorities).extracting(GrantedAuthority::getAuthority).containsExactly("ROLE_CLERK");
    }
}