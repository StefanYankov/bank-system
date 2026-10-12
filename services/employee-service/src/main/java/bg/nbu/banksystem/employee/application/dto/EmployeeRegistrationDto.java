package bg.nbu.banksystem.employee.application.dto;

import bg.nbu.banksystem.employee.domain.EmployeeConstraints;
import bg.nbu.banksystem.employee.domain.EmployeePosition;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Data required to register an employee linked to an existing Keycloak identity.
 *
 * @param identitySubjectId   immutable Keycloak subject identifier
 * @param firstName           employee first name
 * @param lastName            employee last name
 * @param workEmail           employee work email
 * @param position            employee position
 * @param employmentStartDate first date of employment
 */
public record EmployeeRegistrationDto(

        @Schema(
                description = "Immutable Keycloak subject identifier.",
                example = "f5d8a7d5-6c43-4b16-b8e6-0a85a5a6b2d1"
        )
        @NotBlank
        @Size(max = EmployeeConstraints.IDENTITY_SUBJECT_MAX_LENGTH)
        String identitySubjectId,

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
        String workEmail,

        @Schema(description = "Employee position.", example = "CLERK")
        @NotNull
        EmployeePosition position,

        @Schema(description = "First date of employment.", example = "2026-10-01")
        @NotNull
        LocalDate employmentStartDate
) {
}