package bg.nbu.banksystem.employee.persistence;

import bg.nbu.banksystem.employee.domain.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for managing {@link Employee} persistence operations.
 *
 * <p>
 * Provides access to employees through both technical and business identifiers.
 * Additional query methods should be introduced when concrete application use cases
 * require them rather than preemptively exposing every possible search variation.
 * </p>
 */
@Repository
public interface EmployeeRepository extends JpaRepository<Employee, UUID> {

    /**
     * Finds an employee by the externally visible employee number.
     *
     * @param employeeNumber the employee number (for example, EMP-000001)
     * @return the matching employee, or an empty result when no employee exists
     */
    Optional<Employee> findByEmployeeNumber(String employeeNumber);

    /**
     * Finds an employee by the immutable Keycloak subject identifier.
     *
     * <p>
     * This lookup is expected to be used when resolving the employee
     * represented by an authenticated JWT principal.
     * </p>
     *
     * @param identitySubjectId the Keycloak subject identifier
     * @return the matching employee, or an empty result when no employee exists
     */
    Optional<Employee> findByIdentitySubjectId(String identitySubjectId);

    /**
     * Checks whether an employee already exists with the given work email.
     *
     * <p>
     * Primarily intended for uniqueness validation during employee registration
     * and profile updates.
     * </p>
     *
     * @param workEmail the work email address to check
     * @return {@code true} if an employee already uses the email;
     * {@code false} otherwise
     */
    boolean existsByWorkEmail(String workEmail);

    /**
     * Checks whether an employee is already linked to the given Keycloak subject.
     *
     * <p>
     * Used during registration to report a duplicate identity before inserting.
     * </p>
     *
     * @param identitySubjectId the Keycloak subject identifier
     * @return {@code true} if an employee is already linked to the subject
     */
    boolean existsByIdentitySubjectId(String identitySubjectId);
}