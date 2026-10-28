package bg.nbu.banksystem.customer.application.validation;

import bg.nbu.banksystem.customer.domain.Eiks;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.jspecify.annotations.Nullable;

/**
 * Validates {@link ValidEik} values with the same algorithm the domain uses.
 */
public class EikValidator implements ConstraintValidator<ValidEik, String> {

    @Override
    public boolean isValid(@Nullable String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;
        }

        try {
            Eiks.normalize(value);
            return true;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }
}