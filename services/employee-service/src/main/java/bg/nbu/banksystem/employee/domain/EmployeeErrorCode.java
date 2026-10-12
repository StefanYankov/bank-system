package bg.nbu.banksystem.employee.domain;

import bg.nbu.banksystem.starter.error.ErrorCategory;
import bg.nbu.banksystem.starter.error.ErrorCode;

/**
 * Stable error codes reported by Employee Service.
 *
 * <p>
 * Codes are part of the public API contract and must not be renamed once published.
 * They live in the domain package because both the {@link Employee} entity and the
 * application service throw them.
 * </p>
 */
public enum EmployeeErrorCode implements ErrorCode {

    EMPLOYEE_NOT_FOUND(ErrorCategory.NOT_FOUND, "Employee not found"),
    EMPLOYEE_IDENTITY_ALREADY_LINKED(ErrorCategory.CONFLICT, "Employee identity is already linked"),
    EMPLOYEE_EMAIL_ALREADY_USED(ErrorCategory.CONFLICT, "Employee work email is already used"),

    EMPLOYEE_ALREADY_INACTIVE(ErrorCategory.CONFLICT, "Employee is already inactive"),
    EMPLOYEE_ALREADY_ACTIVE(ErrorCategory.CONFLICT, "Employee is already active"),
    INVALID_EMPLOYMENT_PERIOD(ErrorCategory.RULE_VIOLATION, "Employment end date precedes the start date");

    private final ErrorCategory category;
    private final String title;

    EmployeeErrorCode(ErrorCategory category, String title) {
        this.category = category;
        this.title = title;
    }

    @Override
    public String code() {
        return name();
    }

    @Override
    public ErrorCategory category() {
        return category;
    }

    @Override
    public String title() {
        return title;
    }
}