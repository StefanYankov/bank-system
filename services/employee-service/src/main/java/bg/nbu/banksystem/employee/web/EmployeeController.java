package bg.nbu.banksystem.employee.web;

import bg.nbu.banksystem.employee.application.EmployeeService;
import bg.nbu.banksystem.employee.application.dto.EmployeeDisableDto;
import bg.nbu.banksystem.employee.application.dto.EmployeePositionChangeDto;
import bg.nbu.banksystem.employee.application.dto.EmployeeRegistrationDto;
import bg.nbu.banksystem.employee.application.dto.EmployeeUpdateDto;
import bg.nbu.banksystem.employee.application.dto.EmployeeViewDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

/**
 * REST API for registering, retrieving and managing bank employees.
 *
 * <p>
 * Employee master data is administered through this API. Authenticated staff may
 * retrieve Employee contact information for operational collaboration, while
 * registration and state-changing operations remain restricted to administrators.
 * </p>
 */
@RestController
@RequestMapping(
        value = "/api/employees",
        produces = MediaType.APPLICATION_JSON_VALUE
)
@RequiredArgsConstructor
@ApiStandardResponses
@SecurityRequirement(name = "bearerAuth")
@Tag(
        name = "Employee API",
        description = "Endpoints for registering, retrieving and managing bank employees."
)
public class EmployeeController {

    private final EmployeeService employeeService;

    /**
     * Registers an employee linked to an existing Keycloak identity.
     *
     * @param registrationDto employee registration data
     * @return the registered employee with a location header
     */
    @Operation(
            operationId = "registerEmployee",
            summary = "Register an employee",
            description = "Registers an active employee linked to an existing Keycloak identity."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Employee registered"
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "The identity or work email is already linked",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)
                    )
            )
    })
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EmployeeViewDto> registerEmployee(
            @Valid @RequestBody EmployeeRegistrationDto registrationDto
    ) {
        EmployeeViewDto registeredEmployee =
                employeeService.register(registrationDto);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(registeredEmployee.id())
                .toUri();

        return ResponseEntity
                .created(location)
                .body(registeredEmployee);
    }

    /**
     * Retrieves a page of employees.
     *
     * @param pageable paging and sorting information
     * @return a stable paged representation of employees
     */
    @Operation(
            operationId = "getEmployees",
            summary = "Get employees",
            description = "Retrieves a paginated list of employees."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Employees retrieved"
    )
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PagedResponse<EmployeeViewDto>> getEmployees(
            @ParameterObject Pageable pageable
    ) {
        Page<EmployeeViewDto> employeePage =
                employeeService.getAll(pageable);

        return ResponseEntity
                .ok(
                PagedResponse.from(employeePage)
        );
    }

    /**
     * Retrieves the employee linked to the authenticated Keycloak user.
     *
     * @param jwt authenticated JWT
     * @return the current employee
     */
    @Operation(
            operationId = "getCurrentEmployee",
            summary = "Get the current employee",
            description = "Retrieves the employee linked to the authenticated Keycloak subject."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Current employee retrieved"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "No employee is linked to the authenticated identity",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)
                    )
            )
    })
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<EmployeeViewDto> getCurrentEmployee(
            @Parameter(hidden = true)
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity
                .ok(
                employeeService.getCurrent(jwt.getSubject())
        );
    }

    /**
     * Retrieves an employee by technical identifier.
     *
     * @param id employee identifier
     * @return the employee
     */
    @Operation(
            operationId = "getEmployeeById",
            summary = "Get an employee by ID",
            description = "Retrieves an employee by the technical UUID identifier."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Employee retrieved"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Employee not found",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)
                    )
            )
    })
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CLERK', 'LOAN_OFFICER', 'ADMIN')")
    public ResponseEntity<EmployeeViewDto> getEmployeeById(
            @Parameter(description = "Technical employee UUID")
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(
                employeeService.getById(id)
        );
    }

    /**
     * Retrieves an employee by business employee number.
     *
     * @param employeeNumber business employee number
     * @return the employee
     */
    @Operation(
            operationId = "getEmployeeByNumber",
            summary = "Get an employee by employee number",
            description = "Retrieves an employee by the business employee number."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Employee retrieved"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Employee not found",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)
                    )
            )
    })
    @GetMapping("/by-number/{employeeNumber}")
    @PreAuthorize("hasAnyRole('CLERK', 'LOAN_OFFICER', 'ADMIN')")
    public ResponseEntity<EmployeeViewDto> getEmployeeByNumber(
            @Parameter(
                    description = "Business employee number",
                    example = "EMP-000001"
            )
            @PathVariable String employeeNumber
    ) {
        return ResponseEntity.ok(
                employeeService.getByEmployeeNumber(employeeNumber)
        );
    }

    /**
     * Replaces an employee's editable profile information.
     *
     * @param id        employee identifier
     * @param updateDto updated profile information
     * @return the updated employee
     */
    @Operation(
            operationId = "updateEmployeeProfile",
            summary = "Update an employee profile",
            description = "Replaces the editable name and work-email fields of an employee."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Employee profile updated"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Employee not found",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "The work email is already assigned to another employee",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)
                    )
            )
    })
    @PutMapping(
            value = "/{id}/profile",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EmployeeViewDto> updateEmployeeProfile(
            @Parameter(description = "Technical employee UUID")
            @PathVariable UUID id,
            @Valid @RequestBody EmployeeUpdateDto updateDto
    ) {
        return ResponseEntity.ok(
                employeeService.updateProfile(id, updateDto)
        );
    }

    /**
     * Changes an employee's position.
     *
     * @param id                employee identifier
     * @param positionChangeDto new employee position
     * @return the updated employee
     */
    @Operation(
            operationId = "changeEmployeePosition",
            summary = "Change an employee position",
            description = "Changes the employee's position within the bank system."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Employee position changed"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Employee not found",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)
                    )
            )
    })
    @PutMapping(
            value = "/{id}/position",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EmployeeViewDto> changeEmployeePosition(
            @Parameter(description = "Technical employee UUID")
            @PathVariable UUID id,
            @Valid @RequestBody EmployeePositionChangeDto positionChangeDto
    ) {
        return ResponseEntity.ok(
                employeeService.changePosition(id, positionChangeDto)
        );
    }

    /**
     * Disables an active employee.
     *
     * @param id         employee identifier
     * @param disableDto final employment date
     * @return the disabled employee
     */
    @Operation(
            operationId = "disableEmployee",
            summary = "Disable an employee",
            description = "Marks an active employee as inactive from the supplied employment end date."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Employee disabled"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Employee not found",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Employee is already inactive",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "422",
                    description = "Employment end date precedes the start date",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)
                    )
            )
    })
    @PostMapping(
            value = "/{id}/disable",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EmployeeViewDto> disableEmployee(
            @Parameter(description = "Technical employee UUID")
            @PathVariable UUID id,
            @Valid @RequestBody EmployeeDisableDto disableDto
    ) {
        return ResponseEntity.ok(
                employeeService.disable(id, disableDto)
        );
    }

    /**
     * Reactivates an inactive employee.
     *
     * @param id employee identifier
     * @return the reactivated employee
     */
    @Operation(
            operationId = "reactivateEmployee",
            summary = "Reactivate an employee",
            description = "Marks an inactive employee as active and clears the employment end date."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Employee reactivated"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Employee not found",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Employee is already active",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)
                    )
            )
    })
    @PostMapping("/{id}/reactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EmployeeViewDto> reactivateEmployee(
            @Parameter(description = "Technical employee UUID")
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(
                employeeService.reactivate(id)
        );
    }
}