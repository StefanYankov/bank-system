package bg.nbu.banksystem.employee.application;

/**
 * Generates the externally visible business number assigned to a new employee.
 */
@FunctionalInterface
public interface EmployeeNumberGenerator {

    /**
     * Generates the next available employee number.
     *
     * @return an employee number in the format {@code EMP-000001}
     */
    String generate();
}