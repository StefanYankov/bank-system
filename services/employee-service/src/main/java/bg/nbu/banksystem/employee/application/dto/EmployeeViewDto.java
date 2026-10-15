package bg.nbu.banksystem.employee.application.dto;

import bg.nbu.banksystem.employee.domain.EmployeePosition;
import bg.nbu.banksystem.employee.domain.EmployeeStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Employee representation returned by Employee Service.
 *
 * @param id                  technical employee identifier
 * @param employeeNumber      business employee number
 * @param firstName           employee first name
 * @param lastName            employee last name
 * @param workEmail           employee work email
 * @param position            employee position
 * @param status              employee status
 * @param employmentStartDate first date of employment
 * @param employmentEndDate   final date of employment, if any
 */
public record EmployeeViewDto(

        @Schema(
                description = "Technical employee identifier.",
                example = "01923b7c-8f6d-7a12-9d1e-4b2f8c3e5a10",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        UUID id,

        @Schema(
                description = "Business employee number.",
                example = "EMP-000001",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String employeeNumber,

        @Schema(
                description = "Employee first name.",
                example = "Serafim",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String firstName,

        @Schema(
                description = "Employee last name.",
                example = "Gerasimoff",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String lastName,

        @Schema(
                description = "Employee work email.",
                example = "serafim.gerasimoff@example.com",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String workEmail,

        @Schema(
                description = "Employee position.",
                example = "CLERK",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        EmployeePosition position,

        @Schema(
                description = "Employee status.",
                example = "ACTIVE",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        EmployeeStatus status,

        @Schema(
                description = "First date of employment.",
                example = "2026-10-01",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        LocalDate employmentStartDate,

        @Schema(
                description = "Final date of employment, if any.",
                example = "2026-12-31",
                requiredMode = Schema.RequiredMode.REQUIRED,
                nullable = true
        )
        LocalDate employmentEndDate
) {
}