package bg.nbu.banksystem.account.domain;

import java.math.BigInteger;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

import static bg.nbu.banksystem.account.domain.AccountConstraints.IBAN_LENGTH;

/**
 * Bulgarian IBAN value object used by Account Service.
 */
public record Iban(String value) {

    private static final Pattern BULGARIAN_IBAN_PATTERN = Pattern.compile("BG\\d{2}[A-Z]{4}\\d{14}");
    private static final BigInteger MODULUS = BigInteger.valueOf(97);

    /**
     * Creates and validates a Bulgarian IBAN.
     *
     * @param value IBAN text, with or without spaces
     */
    public Iban {
        Objects.requireNonNull(value, "value must not be null");

        value = value.replace(" ", "").toUpperCase(Locale.ROOT);

        if (value.length() != IBAN_LENGTH) {
            throw new IllegalArgumentException("iban must contain exactly " + IBAN_LENGTH + " characters");
        }

        if (!BULGARIAN_IBAN_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("iban has an invalid Bulgarian format");
        }

        if (!hasValidCheckDigits(value)) {
            throw new IllegalArgumentException("iban has invalid check digits");
        }
    }

    /**
     * Generates a Bulgarian IBAN for the fictional NBU Bank used by the capstone.
     *
     * @param type            account product type
     * @param accountSequence positive account sequence value
     * @return generated valid IBAN
     */
    public static Iban generate(AccountType type, long accountSequence) {
        Objects.requireNonNull(type, "type must not be null");

        if (accountSequence <= 0 || accountSequence > 99_999_999L) {
            throw new IllegalArgumentException("accountSequence must be between 1 and 99999999");
        }

        String accountNumber = "%08d".formatted(accountSequence);
        String bban = "NBUB" + "0001" + type.ibanSegment() + accountNumber;
        String checkDigits = calculateCheckDigits(bban);

        return new Iban("BG" + checkDigits + bban);
    }

    private static String calculateCheckDigits(String bban) {
        String rearranged = bban + "BG" + "00";
        BigInteger numericValue = new BigInteger(toNumericRepresentation(rearranged));
        int remainder = numericValue.mod(MODULUS).intValue();
        int checkDigits = 98 - remainder;

        return "%02d".formatted(checkDigits);
    }

    private static boolean hasValidCheckDigits(String iban) {
        String rearranged = iban.substring(4) + iban.substring(0, 4);
        BigInteger numericValue = new BigInteger(toNumericRepresentation(rearranged));

        return numericValue.mod(MODULUS).intValue() == 1;
    }

    private static String toNumericRepresentation(String value) {
        StringBuilder numericValue = new StringBuilder();

        for (char character : value.toCharArray()) {
            if (Character.isDigit(character)) {
                numericValue.append(character);
            } else {
                int numericCharacter = character - 'A' + 10;
                numericValue.append(numericCharacter);
            }
        }

        return numericValue.toString();
    }
}
