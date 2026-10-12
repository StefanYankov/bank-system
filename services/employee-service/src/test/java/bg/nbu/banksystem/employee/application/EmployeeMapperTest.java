package bg.nbu.banksystem.employee.application;

import bg.nbu.banksystem.employee.application.dto.EmployeeViewDto;
import bg.nbu.banksystem.employee.domain.Employee;
import bg.nbu.banksystem.employee.domain.EmployeePosition;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("EmployeeMapper (unit)")
class EmployeeMapperTest {

    private final EmployeeMapper mapper = Mappers.getMapper(EmployeeMapper.class);

    @Test
    @DisplayName("Happy Path: Should map an employee to its view")
    void shouldMapEmployeeToView() {
        // Arrange
        Employee employee = Employee.register(
                "EMP-000001",
                "keycloak-subject-id",
                "Serafim",
                "Gerasimoff",
                "serafim.gerasimoff@example.com",
                EmployeePosition.CLERK,
                LocalDate.of(2026, 10, 1)
        );
        employee.disable(LocalDate.of(2026, 12, 31));

        // Act
        EmployeeViewDto view = mapper.toViewDto(employee);

        // Assert
        assertThat(view.id()).isEqualTo(employee.getId());
        assertThat(view.employeeNumber()).isEqualTo("EMP-000001");
        assertThat(view.firstName()).isEqualTo("Serafim");
        assertThat(view.lastName()).isEqualTo("Gerasimoff");
        assertThat(view.workEmail()).isEqualTo("serafim.gerasimoff@example.com");
        assertThat(view.position()).isEqualTo(EmployeePosition.CLERK);
        assertThat(view.status()).isEqualTo(employee.getStatus());
        assertThat(view.employmentStartDate()).isEqualTo(LocalDate.of(2026, 10, 1));
        assertThat(view.employmentEndDate()).isEqualTo(LocalDate.of(2026, 12, 31));
    }
}