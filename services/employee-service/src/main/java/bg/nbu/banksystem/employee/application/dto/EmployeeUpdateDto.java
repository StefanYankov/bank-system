package bg.nbu.banksystem.employee.application.dto;

import bg.nbu.banksystem.employee.domain.EmployeeConstraints;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Editable employee profile information.
 *
 * @param firstName employee first name
 * @param lastName  employee last name
 * @param workEmail employee work email
 */
public record EmployeeUpdateDto(

        @Schema(description = "Employee first name.", example = "Serafim")
        @NotBlank
        @Size(max = EmployeeConstraints.NAME_MAX_LENGTH)
        String firstName,

        @Schema(description = "Employee last name.", example = "Gerasimoff")
        @NotBlank
        @Size(max = EmployeeConstraints.NAME_MAX_LENGTH)
        String lastName,

        @Schema(
                description = "Unique employee work email.",
                example = "serafim.gerasimoff@example.com"
        )
        @NotBlank
        @Email
        @Size(max = EmployeeConstraints.EMAIL_MAX_LENGTH)
        String workEmail
) {
}