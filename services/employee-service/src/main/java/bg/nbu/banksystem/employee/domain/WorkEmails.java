package bg.nbu.banksystem.employee.domain;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Locale;

/**
 * Canonical form of employee work emails.
 *
 * <p>
 * Used by {@link Employee} when storing an email and by the application service
 * when checking uniqueness, so both always compare the same value.
 * </p>
 */
@NullMarked
public final class WorkEmails {

    private WorkEmails() {
    }

    /**
     * Normalises a work email to its stored form.
     *
     * @param workEmail the raw work email
     * @return the trimmed, lower-cased email
     * @throws IllegalArgumentException if the email is null or blank
     */
    public static String normalize(@Nullable String workEmail) {
        if (workEmail == null || workEmail.isBlank()) {
            throw new IllegalArgumentException("workEmail must not be blank");
        }

        return workEmail.trim().toLowerCase(Locale.ROOT);
    }
}