package bg.nbu.banksystem.customer.application.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The annotated value must be a structurally valid 9-digit EIK with a correct check digit.
 */
@Documented
@Constraint(validatedBy = EikValidator.class)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidEik {

    String message() default "must be a valid 9-digit EIK";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}