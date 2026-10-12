package bg.nbu.banksystem.employee.application.dto;

import bg.nbu.banksystem.employee.domain.EmployeePosition;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * Request to change an employee's position.
 *
 * @param position the new employee position
 */
public record EmployeePositionChangeDto(

        @Schema(description = "New employee position.", example = "LOAN_OFFICER")
        @NotNull
        EmployeePosition position
) {
}