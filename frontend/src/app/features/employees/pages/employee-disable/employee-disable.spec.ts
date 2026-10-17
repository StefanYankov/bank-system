import {HttpErrorResponse} from '@angular/common/http';
import {TestBed} from '@angular/core/testing';
import {ActivatedRoute, convertToParamMap, provideRouter, Router,} from '@angular/router';
import {of, Subject, throwError,} from 'rxjs';
import {beforeEach, describe, expect, it, vi,} from 'vitest';

import {EmployeeAPIService, EmployeeDisableDto, EmployeeViewDto,} from '../../../../generated/employee-api';
import {EmployeeDisable} from './employee-disable';

describe('EmployeeDisable', () => {
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

  const disabledEmployee: EmployeeViewDto = {
    ...employee,
    status: EmployeeViewDto.StatusEnum.Inactive,
    employmentEndDate: '2026-12-31',
  };

  const getEmployeeByIdMock = vi.fn();
  const disableEmployeeMock = vi.fn();

  const navigateMock = vi.fn<
    (commands: readonly unknown[]) => Promise<boolean>
  >();

  beforeEach(async () => {
    getEmployeeByIdMock.mockReset();
    disableEmployeeMock.mockReset();
    navigateMock.mockReset();

    getEmployeeByIdMock.mockReturnValue(
      of(employee),
    );

    disableEmployeeMock.mockReturnValue(
      of(disabledEmployee),
    );

    navigateMock.mockResolvedValue(true);

    await TestBed.configureTestingModule({
      imports: [EmployeeDisable],
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
            disableEmployee: disableEmployeeMock,
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

  it('should load and render the active Employee', () => {
    const fixture =
      TestBed.createComponent(EmployeeDisable);

    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;

    expect(getEmployeeByIdMock).toHaveBeenCalledOnce();

    expect(getEmployeeByIdMock).toHaveBeenCalledWith({
      id: employeeId,
    });

    expect(element.textContent).toContain(
      'Disable Employee',
    );

    expect(element.textContent).toContain(
      'Jane Doe',
    );

    expect(element.textContent).toContain(
      'EMP-000002',
    );

    expect(element.textContent).toContain(
      'ACTIVE',
    );

    expect(
      element.querySelector('.disable-form'),
    ).not.toBeNull();
  });

  it('should reject an empty employment end date', () => {
    const fixture =
      TestBed.createComponent(EmployeeDisable);

    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;
    const formElement = element.querySelector('form');

    formElement?.dispatchEvent(
      new Event('submit'),
    );

    fixture.detectChanges();

    expect(disableEmployeeMock).not.toHaveBeenCalled();

    expect(element.textContent).toContain(
      'Employment end date is required.',
    );
  });

  it('should submit the Employee disable request', () => {
    const fixture =
      TestBed.createComponent(EmployeeDisable);

    fixture.detectChanges();

    const form =
      fixture.componentInstance['disableForm'];

    const request: EmployeeDisableDto = {
      employmentEndDate: '2026-12-31',
    };

    form.setValue(request);

    const element = fixture.nativeElement as HTMLElement;
    const formElement = element.querySelector('form');

    formElement?.dispatchEvent(
      new Event('submit'),
    );

    expect(disableEmployeeMock).toHaveBeenCalledWith({
      id: employeeId,
      employeeDisableDto: request,
    });
  });

  it('should navigate to the disabled Employee details page', () => {
    const fixture =
      TestBed.createComponent(EmployeeDisable);

    fixture.detectChanges();

    const form =
      fixture.componentInstance['disableForm'];

    form.setValue({
      employmentEndDate: '2026-12-31',
    });

    const element = fixture.nativeElement as HTMLElement;
    const formElement = element.querySelector('form');

    formElement?.dispatchEvent(
      new Event('submit'),
    );

    expect(navigateMock).toHaveBeenCalledWith([
      '/admin/employees',
      disabledEmployee.id,
    ]);
  });

  it('should disable submission while the request is pending', () => {
    const response =
      new Subject<EmployeeViewDto>();

    disableEmployeeMock.mockReturnValue(
      response,
    );

    const fixture =
      TestBed.createComponent(EmployeeDisable);

    fixture.detectChanges();

    const form =
      fixture.componentInstance['disableForm'];

    form.setValue({
      employmentEndDate: '2026-12-31',
    });

    const element = fixture.nativeElement as HTMLElement;
    const formElement = element.querySelector('form');

    formElement?.dispatchEvent(
      new Event('submit'),
    );

    fixture.detectChanges();

    const submitButton = element.querySelector(
      'button[type="submit"]',
    ) as HTMLButtonElement | null;

    expect(submitButton?.disabled).toBe(true);

    expect(submitButton?.textContent).toContain(
      'Disabling',
    );

    expect(navigateMock).not.toHaveBeenCalled();
  });

  it('should not display the disable form for an inactive Employee', () => {
    getEmployeeByIdMock.mockReturnValue(
      of(disabledEmployee),
    );

    const fixture =
      TestBed.createComponent(EmployeeDisable);

    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;

    expect(element.textContent).toContain(
      'Employee already inactive',
    );

    expect(
      element.querySelector('.disable-form'),
    ).toBeNull();
  });

  it('should show a loading state while the Employee request is pending', () => {
    const response =
      new Subject<EmployeeViewDto>();

    getEmployeeByIdMock.mockReturnValue(
      response,
    );

    const fixture =
      TestBed.createComponent(EmployeeDisable);

    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;

    expect(element.textContent).toContain(
      'Loading Employee',
    );

    expect(
      element.querySelector('.disable-form'),
    ).toBeNull();
  });

  it('should show a not-found message when the Employee cannot be loaded', () => {
    getEmployeeByIdMock.mockReturnValue(
      throwError(() => new HttpErrorResponse({
        status: 404,
      })),
    );

    const fixture =
      TestBed.createComponent(EmployeeDisable);

    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;

    expect(element.textContent).toContain(
      'The requested Employee could not be found.',
    );
  });

  it('should retry loading the Employee', () => {
    getEmployeeByIdMock
      .mockReturnValueOnce(
        throwError(() =>
          new Error('Employee Service unavailable'),
        ),
      )
      .mockReturnValueOnce(
        of(employee),
      );

    const fixture =
      TestBed.createComponent(EmployeeDisable);

    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;

    const retryButton = element.querySelector(
      '.button--primary',
    ) as HTMLButtonElement | null;

    retryButton?.click();
    fixture.detectChanges();

    expect(
      getEmployeeByIdMock,
    ).toHaveBeenCalledTimes(2);

    expect(element.textContent).toContain(
      'Disable Employee',
    );

    expect(
      element.querySelector('.disable-form'),
    ).not.toBeNull();
  });

  it('should show a conflict message for an inactive Employee', () => {
    disableEmployeeMock.mockReturnValue(
      throwError(() => new HttpErrorResponse({
        status: 409,
      })),
    );

    const fixture =
      TestBed.createComponent(EmployeeDisable);

    fixture.detectChanges();

    const form =
      fixture.componentInstance['disableForm'];

    form.setValue({
      employmentEndDate: '2026-12-31',
    });

    const element = fixture.nativeElement as HTMLElement;
    const formElement = element.querySelector('form');

    formElement?.dispatchEvent(
      new Event('submit'),
    );

    fixture.detectChanges();

    expect(element.textContent).toContain(
      'The Employee is already inactive.',
    );

    expect(navigateMock).not.toHaveBeenCalled();
  });

  it('should show a forbidden message when disabling is not permitted', () => {
    disableEmployeeMock.mockReturnValue(
      throwError(() => new HttpErrorResponse({
        status: 403,
      })),
    );

    const fixture =
      TestBed.createComponent(EmployeeDisable);

    fixture.detectChanges();

    const form =
      fixture.componentInstance['disableForm'];

    form.setValue({
      employmentEndDate: '2026-12-31',
    });

    const element = fixture.nativeElement as HTMLElement;
    const formElement = element.querySelector('form');

    formElement?.dispatchEvent(
      new Event('submit'),
    );

    fixture.detectChanges();

    expect(element.textContent).toContain(
      'You do not have permission to disable this Employee.',
    );

    expect(navigateMock).not.toHaveBeenCalled();
  });

  it('should show a fallback message for an unexpected failure', () => {
    disableEmployeeMock.mockReturnValue(
      throwError(() =>
        new Error('Employee Service unavailable'),
      ),
    );

    const fixture =
      TestBed.createComponent(EmployeeDisable);

    fixture.detectChanges();

    const form =
      fixture.componentInstance['disableForm'];

    form.setValue({
      employmentEndDate: '2026-12-31',
    });

    const element = fixture.nativeElement as HTMLElement;
    const formElement = element.querySelector('form');

    formElement?.dispatchEvent(
      new Event('submit'),
    );

    fixture.detectChanges();

    expect(element.textContent).toContain(
      'The Employee could not be disabled. Please try again.',
    );

    expect(navigateMock).not.toHaveBeenCalled();
  });
});
