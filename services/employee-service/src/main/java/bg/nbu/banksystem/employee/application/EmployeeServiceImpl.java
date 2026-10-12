package bg.nbu.banksystem.employee.application;

import bg.nbu.banksystem.employee.application.dto.EmployeeDisableDto;
import bg.nbu.banksystem.employee.application.dto.EmployeePositionChangeDto;
import bg.nbu.banksystem.employee.application.dto.EmployeeRegistrationDto;
import bg.nbu.banksystem.employee.application.dto.EmployeeUpdateDto;
import bg.nbu.banksystem.employee.application.dto.EmployeeViewDto;
import bg.nbu.banksystem.employee.domain.Employee;
import bg.nbu.banksystem.employee.domain.EmployeeErrorCode;
import bg.nbu.banksystem.employee.domain.WorkEmails;
import bg.nbu.banksystem.employee.persistence.EmployeeRepository;
import bg.nbu.banksystem.starter.error.DomainException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Default implementation of the Employee Service use cases.
 *
 * <p>
 * Input is validated by the web layer; the entity and database constraints remain
 * the final guards.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmployeeServiceImpl implements EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final EmployeeNumberGenerator employeeNumberGenerator;
    private final EmployeeMapper employeeMapper;

    @Override
    @Transactional
    public EmployeeViewDto register(EmployeeRegistrationDto registrationDto) {
        String identitySubjectId = registrationDto.identitySubjectId().trim();
        String workEmail = WorkEmails.normalize(registrationDto.workEmail());

        if (employeeRepository.existsByIdentitySubjectId(identitySubjectId)) {
            throw new DomainException(EmployeeErrorCode.EMPLOYEE_IDENTITY_ALREADY_LINKED);
        }
        if (employeeRepository.existsByWorkEmail(workEmail)) {
            throw new DomainException(EmployeeErrorCode.EMPLOYEE_EMAIL_ALREADY_USED);
        }

        Employee employee = Employee.register(
                employeeNumberGenerator.generate(),
                identitySubjectId,
                registrationDto.firstName(),
                registrationDto.lastName(),
                workEmail,
                registrationDto.position(),
                registrationDto.employmentStartDate()
        );

        Employee saved = employeeRepository.save(employee);

        log.info("Registered employee {} ({})", saved.getEmployeeNumber(), saved.getId());
        return employeeMapper.toViewDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeeViewDto getById(UUID id) {
        return employeeMapper.toViewDto(findOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeeViewDto getByEmployeeNumber(String employeeNumber) {
        return employeeRepository.findByEmployeeNumber(employeeNumber)
                .map(employeeMapper::toViewDto)
                .orElseThrow(() -> new DomainException(EmployeeErrorCode.EMPLOYEE_NOT_FOUND));
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeeViewDto getCurrent(String identitySubjectId) {
        return employeeRepository.findByIdentitySubjectId(identitySubjectId)
                .map(employeeMapper::toViewDto)
                .orElseThrow(() -> new DomainException(EmployeeErrorCode.EMPLOYEE_NOT_FOUND));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EmployeeViewDto> getAll(Pageable pageable) {
        return employeeRepository.findAll(pageable).map(employeeMapper::toViewDto);
    }

    @Override
    @Transactional
    public EmployeeViewDto updateProfile(UUID id, EmployeeUpdateDto updateDto) {
        Employee employee = findOrThrow(id);
        String workEmail = WorkEmails.normalize(updateDto.workEmail());

        if (!workEmail.equals(employee.getWorkEmail())
                && employeeRepository.existsByWorkEmail(workEmail)) {
            throw new DomainException(EmployeeErrorCode.EMPLOYEE_EMAIL_ALREADY_USED);
        }

        employee.updateProfile(updateDto.firstName(), updateDto.lastName(), workEmail);
        return employeeMapper.toViewDto(employee);
    }

    @Override
    @Transactional
    public EmployeeViewDto changePosition(UUID id, EmployeePositionChangeDto positionChangeDto) {
        Employee employee = findOrThrow(id);
        employee.changePosition(positionChangeDto.position());

        log.info("Changed position of employee {} to {}", employee.getEmployeeNumber(), employee.getPosition());
        return employeeMapper.toViewDto(employee);
    }

    @Override
    @Transactional
    public EmployeeViewDto disable(UUID id, EmployeeDisableDto disableDto) {
        Employee employee = findOrThrow(id);


        employee.disable(disableDto.employmentEndDate()); // Revoking login access is a separate, explicitly designed step.

        log.info("Disabled employee {}", employee.getEmployeeNumber());
        return employeeMapper.toViewDto(employee);
    }

    @Override
    @Transactional
    public EmployeeViewDto reactivate(UUID id) {
        Employee employee = findOrThrow(id);

        employee.reactivate(); // Similar as with disable(): Keycloak access is not restored here yet.

        log.info("Reactivated employee {}", employee.getEmployeeNumber());
        return employeeMapper.toViewDto(employee);
    }

    private Employee findOrThrow(UUID id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new DomainException(EmployeeErrorCode.EMPLOYEE_NOT_FOUND));
    }
}