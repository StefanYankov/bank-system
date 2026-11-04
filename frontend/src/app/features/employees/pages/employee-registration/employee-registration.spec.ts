import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { of, Subject, throwError } from 'rxjs';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import {
  EmployeeAPIService,
  EmployeeRegistrationDto,
  EmployeeViewDto,
} from '../../../../generated/employee-api';
import { employeeSchemaConstraints } from '../../../../generated/employee-schema-constraints.generated';
import { EmployeeRegistration } from './employee-registration';

describe('EmployeeRegistration', () => {
  const employee: EmployeeViewDto = {
    id: '0199e65a-35b1-7000-8000-000000000002',
    employeeNumber: 'EMP-000002',
    firstName: 'Jane',
    lastName: 'Doe',
    workEmail: 'Jane.Doe@bank.test',
    position: EmployeeViewDto.PositionEnum.LoanOfficer,
    status: EmployeeViewDto.StatusEnum.Active,
    employmentStartDate: '2026-10-20',
    employmentEndDate: null,
  };

  const validRegistration: EmployeeRegistrationDto = {
    identitySubjectId: 'keycloak-subject-002',
    firstName: 'Jane',
    lastName: 'Doe',
    workEmail: 'Jane.Doe@bank.test',
    position: EmployeeRegistrationDto.PositionEnum.LoanOfficer,
    employmentStartDate: '2026-10-20',
  };

  const registerEmployeeMock = vi.fn();

  const navigateMock = vi.fn<(commands: readonly unknown[]) => Promise<boolean>>();

  beforeEach(async () => {
    vi.clearAllMocks();

    registerEmployeeMock.mockReturnValue(of(employee));
    navigateMock.mockResolvedValue(true);

    await TestBed.configureTestingModule({
      imports: [EmployeeRegistration],
      providers: [
        provideRouter([]),
        {
          provide: EmployeeAPIService,
          useValue: {
            registerEmployee: registerEmployeeMock,
          },
        },
      ],
    }).compileComponents();

    const router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockImplementation((commands) => navigateMock(commands));
  });

  it('should render the registration fields', () => {
    const fixture = TestBed.createComponent(EmployeeRegistration);

    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;

    expect(element.textContent).toContain('Register Employee');

    expect(element.textContent).toContain('Keycloak subject ID');

    expect(element.textContent).toContain('First name');

    expect(element.textContent).toContain('Last name');

    expect(element.textContent).toContain('Work email');

    expect(element.textContent).toContain('Position');

    expect(element.textContent).toContain('Employment start date');
  });

  it('should use maximum lengths generated from OpenAPI', () => {
    const fixture = TestBed.createComponent(EmployeeRegistration);

    const form = fixture.componentInstance['registrationForm'];

    const constraints = employeeSchemaConstraints.EmployeeRegistrationDto;

    form.controls.identitySubjectId.setValue(
      'a'.repeat(constraints.identitySubjectId.maxLength + 1),
    );

    form.controls.firstName.setValue('a'.repeat(constraints.firstName.maxLength + 1));

    form.controls.lastName.setValue('a'.repeat(constraints.lastName.maxLength + 1));

    form.controls.workEmail.setValue('a'.repeat(constraints.workEmail.maxLength + 1));

    expect(form.controls.identitySubjectId.hasError('maxlength')).toBe(true);

    expect(form.controls.firstName.hasError('maxlength')).toBe(true);

    expect(form.controls.lastName.hasError('maxlength')).toBe(true);

    expect(form.controls.workEmail.hasError('maxlength')).toBe(true);
  });

  it('should apply email validation to work email', () => {
    const fixture = TestBed.createComponent(EmployeeRegistration);

    const form = fixture.componentInstance['registrationForm'];

    form.controls.workEmail.setValue('not-an-email');

    expect(form.controls.workEmail.hasError('email')).toBe(true);
  });

  it('should reject an invalid form without calling the API', () => {
    const fixture = TestBed.createComponent(EmployeeRegistration);

    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;
    const formElement = element.querySelector('form');

    formElement?.dispatchEvent(new Event('submit'));

    fixture.detectChanges();

    expect(registerEmployeeMock).not.toHaveBeenCalled();

    expect(element.textContent).toContain('Keycloak subject ID is required.');

    expect(element.textContent).toContain('First name is required.');

    expect(element.textContent).toContain('Last name is required.');

    expect(element.textContent).toContain('Work email is required.');

    expect(element.textContent).toContain('Employment start date is required.');
  });

  it('should reject whitespace-only values', () => {
    const fixture = TestBed.createComponent(EmployeeRegistration);

    const form = fixture.componentInstance['registrationForm'];

    form.patchValue({
      identitySubjectId: '   ',
      firstName: '   ',
      lastName: '\t',
      workEmail: '   ',
      employmentStartDate: '2026-10-20',
    });

    expect(form.controls.identitySubjectId.hasError('notBlank')).toBe(true);

    expect(form.controls.firstName.hasError('notBlank')).toBe(true);

    expect(form.controls.lastName.hasError('notBlank')).toBe(true);

    expect(form.controls.workEmail.hasError('notBlank')).toBe(true);

    expect(form.invalid).toBe(true);
  });

  it('should submit a valid Employee registration request', () => {
    const fixture = TestBed.createComponent(EmployeeRegistration);

    fixture.componentInstance['registrationForm'].setValue(validRegistration);

    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;
    const formElement = element.querySelector('form');

    formElement?.dispatchEvent(new Event('submit'));

    expect(registerEmployeeMock).toHaveBeenCalledWith({
      employeeRegistrationDto: validRegistration,
    });
  });

  it('should trim accepted text values before submission', () => {
    const fixture = TestBed.createComponent(EmployeeRegistration);

    fixture.componentInstance['registrationForm'].setValue({
      ...validRegistration,
      identitySubjectId: '  keycloak-subject-002  ',
      firstName: '  Jane  ',
      lastName: '  Doe  ',
    });

    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;
    const formElement = element.querySelector('form');

    formElement?.dispatchEvent(new Event('submit'));

    expect(registerEmployeeMock).toHaveBeenCalledWith({
      employeeRegistrationDto: validRegistration,
    });
  });

  it('should navigate to the created Employee details page', () => {
    const fixture = TestBed.createComponent(EmployeeRegistration);

    fixture.componentInstance['registrationForm'].setValue(validRegistration);

    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;
    const formElement = element.querySelector('form');

    formElement?.dispatchEvent(new Event('submit'));

    expect(navigateMock).toHaveBeenCalledWith(['/admin/employees', employee.id]);
  });

  it('should disable submission while registration is pending', () => {
    const response = new Subject<EmployeeViewDto>();

    registerEmployeeMock.mockReturnValue(response);

    const fixture = TestBed.createComponent(EmployeeRegistration);

    fixture.componentInstance['registrationForm'].setValue(validRegistration);

    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;
    const formElement = element.querySelector('form');

    formElement?.dispatchEvent(new Event('submit'));

    fixture.detectChanges();

    const submitButton = element.querySelector('button[type="submit"]') as HTMLButtonElement | null;

    expect(submitButton?.disabled).toBe(true);

    expect(submitButton?.textContent).toContain('Registering');

    expect(navigateMock).not.toHaveBeenCalled();
  });

  it('should prevent repeated submission while registration is pending', () => {
    const response = new Subject<EmployeeViewDto>();

    registerEmployeeMock.mockReturnValue(response);

    const fixture = TestBed.createComponent(EmployeeRegistration);

    fixture.componentInstance['registrationForm'].setValue(validRegistration);

    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;
    const formElement = element.querySelector('form');

    formElement?.dispatchEvent(new Event('submit'));

    formElement?.dispatchEvent(new Event('submit'));

    expect(registerEmployeeMock).toHaveBeenCalledTimes(1);
  });

  it('should show a conflict message for an existing identity or email', () => {
    registerEmployeeMock.mockReturnValue(
      throwError(
        () =>
          new HttpErrorResponse({
            status: 409,
          }),
      ),
    );

    const fixture = TestBed.createComponent(EmployeeRegistration);

    fixture.componentInstance['registrationForm'].setValue(validRegistration);

    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;
    const formElement = element.querySelector('form');

    formElement?.dispatchEvent(new Event('submit'));

    fixture.detectChanges();

    expect(element.textContent).toContain(
      'The Keycloak subject or work email ' + 'is already linked to an Employee.',
    );

    expect(navigateMock).not.toHaveBeenCalled();
  });

  it('should show a forbidden message when registration is not permitted', () => {
    registerEmployeeMock.mockReturnValue(
      throwError(
        () =>
          new HttpErrorResponse({
            status: 403,
          }),
      ),
    );

    const fixture = TestBed.createComponent(EmployeeRegistration);

    fixture.componentInstance['registrationForm'].setValue(validRegistration);

    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;
    const formElement = element.querySelector('form');

    formElement?.dispatchEvent(new Event('submit'));

    fixture.detectChanges();

    expect(element.textContent).toContain('You do not have permission to register Employees.');

    expect(navigateMock).not.toHaveBeenCalled();
  });

  it('should show a validation message for a rejected request', () => {
    registerEmployeeMock.mockReturnValue(
      throwError(
        () =>
          new HttpErrorResponse({
            status: 400,
          }),
      ),
    );

    const fixture = TestBed.createComponent(EmployeeRegistration);

    fixture.componentInstance['registrationForm'].setValue(validRegistration);

    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;
    const formElement = element.querySelector('form');

    formElement?.dispatchEvent(new Event('submit'));

    fixture.detectChanges();

    expect(element.textContent).toContain(
      'The Employee information is invalid. ' + 'Review the form and try again.',
    );

    expect(navigateMock).not.toHaveBeenCalled();
  });

  it('should show a fallback message for an unexpected failure', () => {
    registerEmployeeMock.mockReturnValue(
      throwError(() => new Error('Employee Service unavailable')),
    );

    const fixture = TestBed.createComponent(EmployeeRegistration);

    fixture.componentInstance['registrationForm'].setValue(validRegistration);

    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;
    const formElement = element.querySelector('form');

    formElement?.dispatchEvent(new Event('submit'));

    fixture.detectChanges();

    expect(element.textContent).toContain(
      'The Employee could not be registered. ' + 'Please try again.',
    );

    expect(navigateMock).not.toHaveBeenCalled();
  });
});
