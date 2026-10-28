package bg.nbu.banksystem.customer.application.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The annotated value must be a structurally valid EGN: ten digits, a real encoded birthdate and a correct check
 * digit.
 */
@Documented
@Constraint(validatedBy = EgnValidator.class)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidEgn {

    String message() default "must be a valid EGN";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}