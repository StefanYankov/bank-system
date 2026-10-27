package bg.nbu.banksystem.customer.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * Final date of a representation.
 *
 * @param validTo final valid date, inclusive
 */
public record RepresentationEndDto(
        @Schema(description = "Final valid date of the representation, inclusive.", example = "2026-12-31")
        @NotNull
        LocalDate validTo
) {
}