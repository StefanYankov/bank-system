package bg.nbu.banksystem.starter.error.internal;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.annotation.Nullable;

/**
 * One validation failure in the {@code errors} member of a problem response.
 *
 * @param field   the field or parameter that failed; absent for object-level rules
 * @param message the validation message
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ValidationError(@Nullable String field, String message) {
}