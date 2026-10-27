package bg.nbu.banksystem.customer.application.validation;

import bg.nbu.banksystem.customer.domain.Egns;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.jspecify.annotations.Nullable;

/**
 * Validates {@link ValidEgn} values with the same algorithm the domain uses.
 */
public class EgnValidator implements ConstraintValidator<ValidEgn, String> {

    @Override
    public boolean isValid(@Nullable String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;
        }

        try {
            Egns.normalize(value);
            return true;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }
}