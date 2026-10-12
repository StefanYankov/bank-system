package bg.nbu.banksystem.employee.application;

import bg.nbu.banksystem.employee.application.dto.EmployeeViewDto;
import bg.nbu.banksystem.employee.domain.Employee;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

/**
 * Maps Employee entities to their API-facing views.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface EmployeeMapper {

    /**
     * Converts an employee to its view representation.
     *
     * @param employee the employee to convert
     * @return the employee view
     */
    EmployeeViewDto toViewDto(Employee employee);
}