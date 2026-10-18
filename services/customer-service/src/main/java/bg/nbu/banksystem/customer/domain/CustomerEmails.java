package bg.nbu.banksystem.customer.domain;

import org.jspecify.annotations.Nullable;

import java.util.Locale;

/**
 * Canonical form of Customer email addresses.
 *
 * <p>
 * Used by Customer entities when storing an email and by the application
 * service when checking uniqueness, so both always compare the same value.
 * </p>
 */
public final class CustomerEmails {

    private CustomerEmails() {
    }

    /**
     * Normalises a Customer email to its stored form.
     *
     * <p>
     * Normalization removes surrounding whitespace and converts characters
     * to lowercase using a locale-independent rule. Email syntax validation
     * remains the responsibility of API validation.
     * </p>
     *
     * @param email raw Customer email
     * @return trimmed, lowercase email
     * @throws IllegalArgumentException if the email is null or blank
     */
    public static String normalize(@Nullable String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException(
                    "email must not be blank"
            );
        }

        return email.trim()
                .toLowerCase(Locale.ROOT);
    }
}