import {HttpErrorResponse} from '@angular/common/http';
import {TestBed} from '@angular/core/testing';
import {ActivatedRoute, convertToParamMap, provideRouter, Router,} from '@angular/router';
import {of, Subject, throwError,} from 'rxjs';
import {beforeEach, describe, expect, it, vi,} from 'vitest';
import {EmployeeAPIService, EmployeeUpdateDto, EmployeeViewDto,} from '../../../../generated/employee-api';
import {employeeSchemaConstraints,} from '../../../../generated/employee-schema-constraints.generated';
import {EmployeeProfileEdit} from './employee-profile-edit';

describe('EmployeeProfileEdit', () => {
  const employeeId =
    '0199e65a-35b1-7000-8000-000000000002';

  const employee: EmployeeViewDto = {
    id: employeeId,
    employeeNumber: 'EMP-000002',
    firstName: 'Jane',
    lastName: 'Doe',
    workEmail: 'jane.doe@bank.test',
    position: EmployeeViewDto.PositionEnum.LoanOfficer,
    status: EmployeeViewDto.StatusEnum.Active,
    employmentStartDate: '2026-10-20',
    employmentEndDate: null,
  };

  const updatedEmployee: EmployeeViewDto = {
    ...employee,
    firstName: 'Janet',
    workEmail: 'janet.doe@bank.test',
  };

  const getEmployeeByIdMock = vi.fn();
  const updateEmployeeProfileMock = vi.fn();

  const navigateMock = vi.fn<
    (commands: readonly unknown[]) => Promise<boolean>
  >();

  beforeEach(async () => {
    vi.clearAllMocks();

    getEmployeeByIdMock.mockReturnValue(
      of(employee),
    );

    updateEmployeeProfileMock.mockReturnValue(
      of(updatedEmployee),
    );

    navigateMock.mockResolvedValue(true);

    await TestBed.configureTestingModule({
      imports: [EmployeeProfileEdit],
      providers: [
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              paramMap: convertToParamMap({
                id: employeeId,
              }),
            },
          },
        },
        {
          provide: EmployeeAPIService,
          useValue: {
            getEmployeeById: getEmployeeByIdMock,
            updateEmployeeProfile:
            updateEmployeeProfileMock,
          },
        },
      ],
    }).compileComponents();

    const router = TestBed.inject(Router);

    vi.spyOn(router, 'navigate')
      .mockImplementation(
        (commands) => navigateMock(commands),
      );
  });

  it('should load the Employee and populate the form', () => {
    const fixture =
      TestBed.createComponent(EmployeeProfileEdit);

    fixture.detectChanges();

    const form =
      fixture.componentInstance['profileForm'];

    expect(getEmployeeByIdMock).toHaveBeenCalledWith({
      id: employeeId,
    });

    expect(form.getRawValue()).toEqual({
      firstName: 'Jane',
      lastName: 'Doe',
      workEmail: 'jane.doe@bank.test'
    });
  });

  it('should render the loaded Employee profile values', () => {
    const fixture =
      TestBed.createComponent(EmployeeProfileEdit);

    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;

    const firstNameInput = element.querySelector(
      '#first-name',
    ) as HTMLInputElement | null;

    const lastNameInput = element.querySelector(
      '#last-name',
    ) as HTMLInputElement | null;

    const workEmailInput = element.querySelector(
      '#work-email',
    ) as HTMLInputElement | null;

    expect(firstNameInput?.value).toBe('Jane');
    expect(lastNameInput?.value).toBe('Doe');
    expect(workEmailInput?.value).toBe('jane.doe@bank.test');
  });

  it('should use maximum lengths generated from OpenAPI', () => {
    const fixture =
      TestBed.createComponent(EmployeeProfileEdit);

    fixture.detectChanges();

    const form =
      fixture.componentInstance['profileForm'];

    const constraints =
      employeeSchemaConstraints.EmployeeUpdateDto;

    form.controls.firstName.setValue(
      'a'.repeat(
        constraints.firstName.maxLength + 1,
      ),
    );

    form.controls.lastName.setValue(
      'a'.repeat(
        constraints.lastName.maxLength + 1,
      ),
    );

    form.controls.workEmail.setValue(
      'a'.repeat(
        constraints.workEmail.maxLength + 1,
      ),
    );

    expect(form.controls.firstName.hasError('maxlength')).toBe(true);
    expect(form.controls.lastName.hasError('maxlength')).toBe(true);
    expect(form.controls.workEmail.hasError('maxlength')).toBe(true);
  });

  it('should reject whitespace-only profile values', () => {
    const fixture =
      TestBed.createComponent(EmployeeProfileEdit);

    fixture.detectChanges();

    const form =
      fixture.componentInstance['profileForm'];

    form.setValue({
      firstName: '   ',
      lastName: '\t',
      workEmail: '   ',
    });

    expect(form.controls.firstName.hasError('notBlank')).toBe(true);
    expect(form.controls.lastName.hasError('notBlank')).toBe(true);
    expect(form.controls.workEmail.hasError('notBlank')).toBe(true);
    expect(form.invalid).toBe(true);
  });

  it('should reject an invalid work email', () => {
    const fixture =
      TestBed.createComponent(EmployeeProfileEdit);

    fixture.detectChanges();

    const form =
      fixture.componentInstance['profileForm'];

    form.controls.workEmail.setValue(
      'not-an-email',
    );

    expect(form.controls.workEmail.hasError('email')).toBe(true);
  });

  it('should submit the updated Employee profile', () => {
    const fixture =
      TestBed.createComponent(EmployeeProfileEdit);

    fixture.detectChanges();

    const form =
      fixture.componentInstance['profileForm'];

    const request: EmployeeUpdateDto = {
      firstName: 'Janet',
      lastName: 'Doe',
      workEmail: 'janet.doe@bank.test',
    };

    form.setValue(request);

    const element = fixture.nativeElement as HTMLElement;
    const formElement = element.querySelector('form');

    formElement?.dispatchEvent(new Event('submit'));

    expect(
      updateEmployeeProfileMock,
    ).toHaveBeenCalledWith({
      id: employeeId,
      employeeUpdateDto: request,
    });
  });

  it('should trim accepted names before submission', () => {
    const fixture =
      TestBed.createComponent(EmployeeProfileEdit);

    fixture.detectChanges();

    const form =
      fixture.componentInstance['profileForm'];

    form.setValue({
      firstName: '  Janet  ',
      lastName: '  Doe  ',
      workEmail: 'janet.doe@bank.test',
    });

    const element = fixture.nativeElement as HTMLElement;
    const formElement = element.querySelector('form');

    formElement?.dispatchEvent(
      new Event('submit'),
    );

    expect(
      updateEmployeeProfileMock,
    ).toHaveBeenCalledWith({
      id: employeeId,
      employeeUpdateDto: {
        firstName: 'Janet',
        lastName: 'Doe',
        workEmail: 'janet.doe@bank.test',
      },
    });
  });

  it('should navigate to the updated Employee details page', () => {
    const fixture =
      TestBed.createComponent(EmployeeProfileEdit);

    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;
    const formElement = element.querySelector('form');

    formElement?.dispatchEvent(
      new Event('submit'),
    );

    expect(navigateMock).toHaveBeenCalledWith([
      '/admin/employees',
      updatedEmployee.id,
    ]);
  });

  it('should disable submission while the update is pending', () => {
    const response =
      new Subject<EmployeeViewDto>();

    updateEmployeeProfileMock.mockReturnValue(
      response,
    );

    const fixture =
      TestBed.createComponent(EmployeeProfileEdit);

    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;
    const formElement = element.querySelector('form');

    formElement?.dispatchEvent(
      new Event('submit'));

    fixture.detectChanges();

    const submitButton = element.querySelector(
      'button[type="submit"]',
    ) as HTMLButtonElement | null;

    expect(submitButton?.disabled).toBe(true);
    expect(submitButton?.textContent).toContain('Saving');
    expect(navigateMock).not.toHaveBeenCalled();
  });

  it('should show a not-found message when the Employee cannot be loaded', () => {
    getEmployeeByIdMock.mockReturnValue(
      throwError(() => new HttpErrorResponse({
        status: 404,
      })),
    );

    const fixture =
      TestBed.createComponent(EmployeeProfileEdit);

    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;

    expect(element.textContent).toContain('The requested Employee could not be found.');
  });

  it('should show a conflict message for an existing work email', () => {
    updateEmployeeProfileMock.mockReturnValue(
      throwError(() => new HttpErrorResponse({
        status: 409,
      })),
    );

    const fixture =
      TestBed.createComponent(EmployeeProfileEdit);

    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;
    const formElement = element.querySelector('form');

    formElement?.dispatchEvent(new Event('submit'));

    fixture.detectChanges();

    expect(element.textContent).toContain('The work email is already assigned to another Employee.');
    expect(navigateMock).not.toHaveBeenCalled();
  });

  it('should show a fallback message for an unexpected update failure', () => {
    updateEmployeeProfileMock.mockReturnValue(
      throwError(() =>
        new Error('Employee Service unavailable')));

    const fixture =
      TestBed.createComponent(EmployeeProfileEdit);

    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;
    const formElement = element.querySelector('form');

    formElement?.dispatchEvent(
      new Event('submit'),
    );

    fixture.detectChanges();

    expect(element.textContent).toContain('The Employee profile could not be updated. Please try again.');
    expect(navigateMock).not.toHaveBeenCalled();
  });
});
