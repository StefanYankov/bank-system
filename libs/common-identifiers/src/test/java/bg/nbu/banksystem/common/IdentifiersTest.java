package bg.nbu.banksystem.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Identifiers")
class IdentifiersTest {

    @Test
    @DisplayName("Happy Path: Should generate distinct UUIDv7 identifiers")
    void shouldGenerateDistinctUuidV7Identifiers() {
        // Act
        UUID first = Identifiers.newUuid();
        UUID second = Identifiers.newUuid();

        // Assert
        assertThat(first.version()).isEqualTo(7);
        assertThat(second.version()).isEqualTo(7);
        assertThat(first).isNotEqualTo(second);
    }

    @Test
    @DisplayName("Error Case: Should prevent utility class instantiation")
    void shouldPreventInstantiation() throws Exception {
        // Arrange
        Constructor<Identifiers> constructor =
                Identifiers.class.getDeclaredConstructor();

        constructor.setAccessible(true);

        // Act and Assert
        assertThatThrownBy(constructor::newInstance)
                .isInstanceOf(InvocationTargetException.class)
                .hasCauseInstanceOf(IllegalStateException.class)
                .hasRootCauseMessage(
                        "Identifier utility class must not be instantiated"
                );
    }
}