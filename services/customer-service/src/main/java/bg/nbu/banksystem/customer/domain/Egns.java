package bg.nbu.banksystem.customer.domain;

import org.jspecify.annotations.Nullable;

import java.time.YearMonth;
import java.util.regex.Pattern;

/**
 * Validation and canonical form of Bulgarian personal identifiers (EGN).
 *
 * <p>
 * Used by {@link IndividualCustomer} when storing an EGN and by the application
 * service before uniqueness checks, so both always work with the same value.
 * </p>
 *
 * <p>
 * Validation is structural only: ten digits, an encoded birthdate that exists,
 * and a correct check digit. It does not confirm that the EGN was actually issued.
 * </p>
 */
public final class Egns {

    private static final Pattern TEN_DIGITS = Pattern.compile("[0-9]{" + CustomerConstraints.EGN_LENGTH + "}");

    private static final int[] CHECK_DIGIT_WEIGHTS = {2, 4, 8, 5, 10, 9, 7, 3, 6};

    private Egns() {
    }

    /**
     * Validates an EGN and returns its canonical form.
     *
     * @param egn the raw EGN
     * @return the trimmed EGN
     * @throws IllegalArgumentException if the EGN is blank, malformed, encodes an
     *                                  impossible birth date, or has a wrong check digit
     */
    public static String normalize(@Nullable String egn) {
        if (egn == null || egn.isBlank()) {
            throw new IllegalArgumentException("egn must not be blank");
        }

        String normalized = egn.trim();

        if (!TEN_DIGITS.matcher(normalized).matches()) {
            throw new IllegalArgumentException(
                    "egn must contain exactly " + CustomerConstraints.EGN_LENGTH + " digits");
        }

        if (!hasValidBirthDate(normalized)) {
            throw new IllegalArgumentException("egn must encode a valid birth date");
        }

        if (!hasValidCheckDigit(normalized)) {
            throw new IllegalArgumentException("egn has an invalid check digit");
        }

        return normalized;
    }

    private static boolean hasValidBirthDate(String egn) {
        int yearOfCentury = twoDigitsAt(egn, 0);
        int encodedMonth = twoDigitsAt(egn, 2);
        int day = twoDigitsAt(egn, 4);

        int century;
        int month;

        if (encodedMonth >= 1 && encodedMonth <= 12) {
            century = 1900;
            month = encodedMonth;
        } else if (encodedMonth >= 21 && encodedMonth <= 32) {
            century = 1800;
            month = encodedMonth - 20;
        } else if (encodedMonth >= 41 && encodedMonth <= 52) {
            century = 2000;
            month = encodedMonth - 40;
        } else {
            return false;
        }

        return YearMonth.of(century + yearOfCentury, month).isValidDay(day);
    }

    private static boolean hasValidCheckDigit(String egn) {
        int sum = 0;

        for (int i = 0; i < CHECK_DIGIT_WEIGHTS.length; i++) {
            sum += digitAt(egn, i) * CHECK_DIGIT_WEIGHTS[i];
        }

        int remainder = sum % 11;
        int expectedCheckDigit;

        if (remainder == 10) {
            expectedCheckDigit = 0;
        } else {
            expectedCheckDigit = remainder;
        }

        return digitAt(egn, CustomerConstraints.EGN_LENGTH - 1) == expectedCheckDigit;
    }

    private static int twoDigitsAt(String value, int index) {
        return digitAt(value, index) * 10 + digitAt(value, index + 1);
    }

    private static int digitAt(String value, int index) {
        return value.charAt(index) - '0';
    }
}