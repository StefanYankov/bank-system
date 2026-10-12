package bg.nbu.banksystem.employee.domain;

/**
 * Length limits shared by Employee API validation and JPA mapping.
 *
 * <p>
 * The Flyway migration repeats these values in SQL. Hibernate schema
 * validation detects a mismatch at startup.
 * </p>
 */
public final class EmployeeConstraints {

    public static final int EMPLOYEE_NUMBER_MAX_LENGTH = 20;
    public static final int IDENTITY_SUBJECT_MAX_LENGTH = 255;
    public static final int NAME_MAX_LENGTH = 100;

    public static final int EMAIL_MAX_LENGTH = 254;

    public static final int POSITION_MAX_LENGTH = 30;
    public static final int STATUS_MAX_LENGTH = 20;

    private EmployeeConstraints() {
    }
}