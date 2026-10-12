package bg.nbu.banksystem.employee.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * Request to disable an employee from a specific employment end date.
 *
 * @param employmentEndDate final date of employment
 */
public record EmployeeDisableDto(

        @Schema(description = "Final date of employment.", example = "2026-12-31")
        @NotNull
        LocalDate employmentEndDate
) {
}
