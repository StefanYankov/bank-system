package bg.nbu.banksystem.employee.domain;

import bg.nbu.banksystem.common.Identifiers;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents a bank-system employee linked to an identity managed by Keycloak.
 *
 * <p>
 * The entity protects rules concerning its own state. Repository access,
 * transaction boundaries, authorisation, and Keycloak account management remain
 * responsibilities of the application service.
 * </p>
 */
@Getter
@Entity
@Table(name = "employees")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Employee extends BaseEntity {

    @Column(
            name = "employee_number",
            nullable = false,
            updatable = false,
            unique = true,
            length = 20
    )
    private String employeeNumber;

    @Column(
            name = "identity_subject_id",
            nullable = false,
            updatable = false,
            unique = true,
            length = 255
    )
    private String identitySubjectId;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "work_email", nullable = false, unique = true, length = 254)
    private String workEmail;

    @Enumerated(EnumType.STRING)
    @Column(name = "position", nullable = false, length = 30)
    private EmployeePosition position;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private EmployeeStatus status;

    @Column(name = "employment_start_date", nullable = false)
    private LocalDate employmentStartDate;

    @Column(name = "employment_end_date")
    private LocalDate employmentEndDate;

    private Employee(
            UUID id,
            String employeeNumber,
            String identitySubjectId,
            String firstName,
            String lastName,
            String workEmail,
            EmployeePosition position,
            LocalDate employmentStartDate
    ) {
        super(id);

        this.employeeNumber = requireText(employeeNumber, "employeeNumber");
        this.identitySubjectId = requireText(identitySubjectId, "identitySubjectId");
        this.firstName = requireText(firstName, "firstName");
        this.lastName = requireText(lastName, "lastName");
        this.workEmail = normalizeEmail(workEmail);
        this.position = Objects.requireNonNull(
                position,
                "position must not be null"
        );
        this.employmentStartDate = Objects.requireNonNull(
                employmentStartDate,
                "employmentStartDate must not be null"
        );

        this.status = EmployeeStatus.ACTIVE;
    }

    /**
     * Registers a new active employee with an application-assigned UUID.
     *
     * @param employeeNumber      immutable bank employee number
     * @param identitySubjectId   immutable Keycloak subject identifier
     * @param firstName           employee first name
     * @param lastName            employee last name
     * @param workEmail           employee work email
     * @param position            employee position
     * @param employmentStartDate first date of employment
     * @return the registered employee
     */
    public static Employee register(
            String employeeNumber,
            String identitySubjectId,
            String firstName,
            String lastName,
            String workEmail,
            EmployeePosition position,
            LocalDate employmentStartDate
    ) {
        return new Employee(
                Identifiers.newUuid(),
                employeeNumber,
                identitySubjectId,
                firstName,
                lastName,
                workEmail,
                position,
                employmentStartDate
        );
    }

    /**
     * Updates the employee's editable profile information.
     *
     * @param firstName employee first name
     * @param lastName  employee last name
     * @param workEmail employee work email
     */
    public void updateProfile(
            String firstName,
            String lastName,
            String workEmail
    ) {
        this.firstName = requireText(firstName, "firstName");
        this.lastName = requireText(lastName, "lastName");
        this.workEmail = normalizeEmail(workEmail);
    }

    /**
     * Changes the employee's position within the bank system.
     *
     * @param newPosition the new position
     */
    public void changePosition(EmployeePosition newPosition) {
        this.position = Objects.requireNonNull(
                newPosition,
                "newPosition must not be null"
        );
    }

    /**
     * Marks the employee as inactive from the supplied date.
     *
     * <p>
     * The application service is responsible for coordinating the corresponding
     * Keycloak account disablement.
     * </p>
     *
     * @param endDate final date of employment
     * @throws IllegalStateException    if the employee is already inactive
     * @throws IllegalArgumentException if the end date precedes the start date
     */
    public void disable(LocalDate endDate) {
        Objects.requireNonNull(endDate, "endDate must not be null");

        if (status == EmployeeStatus.INACTIVE) {
            throw new IllegalStateException("Employee is already inactive");
        }

        if (endDate.isBefore(employmentStartDate)) {
            throw new IllegalArgumentException(
                    "Employment end date must not precede the start date"
            );
        }

        this.status = EmployeeStatus.INACTIVE;
        this.employmentEndDate = endDate;
    }

    /**
     * Reactivates an inactive employee.
     *
     * <p>
     * The application service is responsible for coordinating the corresponding
     * Keycloak account reactivation.
     * </p>
     *
     * @throws IllegalStateException if the employee is already active
     */
    public void reactivate() {
        if (status == EmployeeStatus.ACTIVE) {
            throw new IllegalStateException("Employee is already active");
        }

        this.status = EmployeeStatus.ACTIVE;
        this.employmentEndDate = null;
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    fieldName + " must not be blank"
            );
        }

        return value.trim();
    }

    private static String normalizeEmail(String workEmail) {
        return requireText(workEmail, "workEmail")
                .toLowerCase(Locale.ROOT);
    }
}
