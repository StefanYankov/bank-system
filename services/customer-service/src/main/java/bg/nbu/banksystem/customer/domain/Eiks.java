package bg.nbu.banksystem.customer.domain;

import org.jspecify.annotations.Nullable;

import java.util.regex.Pattern;

/**
 * Validation and canonical form of Bulgarian legal-entity identifiers (EIK).
 *
 * <p>
 * Only the 9-digit EIK of the legal entity itself is accepted. 13-digit codes
 * identify branches or subunits and are not supported.
 * </p>
 *
 * <p>
 * Validation is structural only: nine digits and a correct check digit. It does
 * not confirm that the EIK is registered in the Commercial Register.
 * </p>
 */
public final class Eiks {

    private static final Pattern NINE_DIGITS = Pattern.compile("[0-9]{" + CustomerConstraints.EIK_LENGTH + "}");

    private static final int[] FIRST_PASS_WEIGHTS = {1, 2, 3, 4, 5, 6, 7, 8};
    private static final int[] SECOND_PASS_WEIGHTS = {3, 4, 5, 6, 7, 8, 9, 10};

    private Eiks() {
    }

    /**
     * Validates an EIK and returns its canonical form.
     *
     * @param eik the raw EIK
     * @return the trimmed EIK
     * @throws IllegalArgumentException if the EIK is blank, not nine digits,
     *                                  or has a wrong check digit
     */
    public static String normalize(@Nullable String eik) {
        if (eik == null || eik.isBlank()) {
            throw new IllegalArgumentException("eik must not be blank");
        }

        String normalized = eik.trim();

        if (!NINE_DIGITS.matcher(normalized).matches()) {
            throw new IllegalArgumentException(
                    "eik must contain exactly " + CustomerConstraints.EIK_LENGTH + " digits");
        }

        if (!hasValidCheckDigit(normalized)) {
            throw new IllegalArgumentException("eik has an invalid check digit");
        }

        return normalized;
    }

    private static boolean hasValidCheckDigit(String eik) {
        int remainder = weightedSum(eik, FIRST_PASS_WEIGHTS) % 11;

        if (remainder == 10) {
            remainder = weightedSum(eik, SECOND_PASS_WEIGHTS) % 11;
        }

        int expectedCheckDigit;

        if (remainder == 10) {
            expectedCheckDigit = 0;
        } else {
            expectedCheckDigit = remainder;
        }

        return digitAt(eik, CustomerConstraints.EIK_LENGTH - 1) == expectedCheckDigit;
    }

    private static int weightedSum(String eik, int[] weights) {
        int sum = 0;

        for (int i = 0; i < weights.length; i++) {
            sum += digitAt(eik, i) * weights[i];
        }

        return sum;
    }

    private static int digitAt(String value, int index) {
        return value.charAt(index) - '0';
    }
}