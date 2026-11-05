package bg.nbu.banksystem.account.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

@DisplayName("Iban (unit)")
class IbanTest {

    @Test
    @DisplayName("Happy Path: Should generate a valid current-account IBAN")
    void shouldGenerateCurrentAccountIban() {
        // Act
        Iban iban = Iban.generate(AccountType.CURRENT, 1);

        // Assert
        assertThat(iban.value()).hasSize(22);
        assertThat(iban.value()).startsWith("BG");
        assertThat(iban.value()).contains("NBUB00010100000001");
        assertThat(new Iban(iban.value())).isEqualTo(iban);
    }

    @Test
    @DisplayName("Happy Path: Should generate a valid savings-account IBAN")
    void shouldGenerateSavingsAccountIban() {
        // Act
        Iban iban = Iban.generate(AccountType.SAVINGS, 42);

        // Assert
        assertThat(iban.value()).contains("NBUB00010200000042");
        assertThat(new Iban(iban.value())).isEqualTo(iban);
    }

    @Test
    @DisplayName("Edge Case: Should remove spaces and normalize letters")
    void shouldNormalizeIban() {
        // Arrange
        Iban generated = Iban.generate(AccountType.CURRENT, 1);
        String spacedIban = generated.value().toLowerCase().replaceAll("(.{4})", "$1 ").trim();

        // Act
        Iban normalizedIban = new Iban(spacedIban);

        // Assert
        assertThat(normalizedIban).isEqualTo(generated);
    }

    @ParameterizedTest
    @ValueSource(longs = {0, -1, 100_000_000L})
    @DisplayName("Error Case: Should reject an out-of-range account sequence")
    void shouldRejectInvalidSequence(long sequence) {
        // Act & Assert
        assertThatIllegalArgumentException().isThrownBy(() -> Iban.generate(AccountType.CURRENT, sequence))
            .withMessageContaining("accountSequence");
    }

    @Test
    @DisplayName("Error Case: Should reject invalid check digits")
    void shouldRejectInvalidCheckDigits() {
        // Arrange
        String validIban = Iban.generate(AccountType.CURRENT, 1).value();
        String invalidIban = "BG00" + validIban.substring(4);

        // Act & Assert
        assertThatIllegalArgumentException().isThrownBy(() -> new Iban(invalidIban))
            .withMessage("iban has invalid check digits");
    }

    @Test
    @DisplayName("Error Case: Should reject a missing IBAN")
    void shouldRejectMissingIban() {
        // Act & Assert
        assertThatNullPointerException().isThrownBy(() -> new Iban(null)).withMessage("value must not be null");
    }
}
