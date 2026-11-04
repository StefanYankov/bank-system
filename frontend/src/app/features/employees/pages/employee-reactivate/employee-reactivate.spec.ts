import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter, Router } from '@angular/router';
import { of, Subject, throwError } from 'rxjs';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { EmployeeAPIService, EmployeeViewDto } from '../../../../generated/employee-api';
import { EmployeeReactivate } from './employee-reactivate';

describe('EmployeeReactivate', () => {
  const employeeId = '0199e65a-35b1-7000-8000-000000000002';

  const inactiveEmployee: EmployeeViewDto = {
    id: employeeId,
    employeeNumber: 'EMP-000002',
    firstName: 'Jane',
    lastName: 'Doe',
    workEmail: 'jane.doe@bank.test',
    position: EmployeeViewDto.PositionEnum.LoanOfficer,
    status: EmployeeViewDto.StatusEnum.Inactive,
    employmentStartDate: '2026-10-20',
    employmentEndDate: '2026-12-31',
  };

  const activeEmployee: EmployeeViewDto = {
    ...inactiveEmployee,
    status: EmployeeViewDto.StatusEnum.Active,
    employmentEndDate: null,
  };

  const getEmployeeByIdMock = vi.fn();
  const reactivateEmployeeMock = vi.fn();

  const navigateMock = vi.fn<(commands: readonly unknown[]) => Promise<boolean>>();

  beforeEach(async () => {
    getEmployeeByIdMock.mockReset();
    reactivateEmployeeMock.mockReset();
    navigateMock.mockReset();

    getEmployeeByIdMock.mockReturnValue(of(inactiveEmployee));

    reactivateEmployeeMock.mockReturnValue(of(activeEmployee));

    navigateMock.mockResolvedValue(true);

    await TestBed.configureTestingModule({
      imports: [EmployeeReactivate],
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
            reactivateEmployee: reactivateEmployeeMock,
          },
        },
      ],
    }).compileComponents();

    const router = TestBed.inject(Router);

    vi.spyOn(router, 'navigate').mockImplementation((commands) => navigateMock(commands));
  });

  it('should load and render the inactive Employee', () => {
    const fixture = TestBed.createComponent(EmployeeReactivate);

    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;

    expect(getEmployeeByIdMock).toHaveBeenCalledOnce();

    expect(getEmployeeByIdMock).toHaveBeenCalledWith({
      id: employeeId,
    });

    expect(element.textContent).toContain('Reactivate Employee');

    expect(element.textContent).toContain('Jane Doe');

    expect(element.textContent).toContain('EMP-000002');

    expect(element.textContent).toContain('INACTIVE');

    expect(element.textContent).toContain('2026-12-31');
  });

  it('should submit the Employee reactivation request', () => {
    const fixture = TestBed.createComponent(EmployeeReactivate);

    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;

    const reactivateButton = element.querySelector(
      'button.button--primary',
    ) as HTMLButtonElement | null;

    reactivateButton?.click();

    expect(reactivateEmployeeMock).toHaveBeenCalledOnce();

    expect(reactivateEmployeeMock).toHaveBeenCalledWith({
      id: employeeId,
    });
  });

  it('should navigate to the reactivated Employee details page', () => {
    const fixture = TestBed.createComponent(EmployeeReactivate);

    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;

    const reactivateButton = element.querySelector(
      'button.button--primary',
    ) as HTMLButtonElement | null;

    reactivateButton?.click();

    expect(navigateMock).toHaveBeenCalledWith(['/admin/employees', activeEmployee.id]);
  });

  it('should disable reactivation while the request is pending', () => {
    const response = new Subject<EmployeeViewDto>();

    reactivateEmployeeMock.mockReturnValue(response);

    const fixture = TestBed.createComponent(EmployeeReactivate);

    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;

    const reactivateButton = element.querySelector(
      'button.button--primary',
    ) as HTMLButtonElement | null;

    reactivateButton?.click();
    fixture.detectChanges();

    expect(reactivateButton?.disabled).toBe(true);

    expect(reactivateButton?.textContent).toContain('Reactivating');

    expect(navigateMock).not.toHaveBeenCalled();
  });

  it('should prevent repeated reactivation while pending', () => {
    const response = new Subject<EmployeeViewDto>();

    reactivateEmployeeMock.mockReturnValue(response);

    const fixture = TestBed.createComponent(EmployeeReactivate);

    fixture.detectChanges();

    const component = fixture.componentInstance as unknown as {
      reactivate(): void;
    };

    component.reactivate();
    component.reactivate();

    expect(reactivateEmployeeMock).toHaveBeenCalledOnce();
  });

  it('should not offer reactivation for an active Employee', () => {
    getEmployeeByIdMock.mockReturnValue(of(activeEmployee));

    const fixture = TestBed.createComponent(EmployeeReactivate);

    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;

    expect(element.textContent).toContain('Employee already active');

    expect(element.querySelector('button.button--primary')).toBeNull();

    expect(reactivateEmployeeMock).not.toHaveBeenCalled();
  });

  it('should show a loading state while the Employee request is pending', () => {
    const response = new Subject<EmployeeViewDto>();

    getEmployeeByIdMock.mockReturnValue(response);

    const fixture = TestBed.createComponent(EmployeeReactivate);

    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;

    expect(element.textContent).toContain('Loading Employee');

    expect(element.querySelector('.reactivation-card')).toBeNull();
  });

  it('should show a not-found message when the Employee cannot be loaded', () => {
    getEmployeeByIdMock.mockReturnValue(
      throwError(
        () =>
          new HttpErrorResponse({
            status: 404,
          }),
      ),
    );

    const fixture = TestBed.createComponent(EmployeeReactivate);

    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;

    expect(element.textContent).toContain('The requested Employee could not be found.');
  });

  it('should retry loading the Employee', () => {
    getEmployeeByIdMock
      .mockReturnValueOnce(throwError(() => new Error('Employee Service unavailable')))
      .mockReturnValueOnce(of(inactiveEmployee));

    const fixture = TestBed.createComponent(EmployeeReactivate);

    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;

    const retryButton = element.querySelector('.button--primary') as HTMLButtonElement | null;

    retryButton?.click();
    fixture.detectChanges();

    expect(getEmployeeByIdMock).toHaveBeenCalledTimes(2);

    expect(element.textContent).toContain('Reactivate Employee');

    expect(element.querySelector('.reactivation-card')).not.toBeNull();
  });

  it('should show a conflict message for an active Employee', () => {
    reactivateEmployeeMock.mockReturnValue(
      throwError(
        () =>
          new HttpErrorResponse({
            status: 409,
          }),
      ),
    );

    const fixture = TestBed.createComponent(EmployeeReactivate);

    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;

    const reactivateButton = element.querySelector(
      'button.button--primary',
    ) as HTMLButtonElement | null;

    reactivateButton?.click();
    fixture.detectChanges();

    expect(element.textContent).toContain('The Employee is already active.');

    expect(navigateMock).not.toHaveBeenCalled();
  });

  it('should show a forbidden message when reactivation is not permitted', () => {
    reactivateEmployeeMock.mockReturnValue(
      throwError(
        () =>
          new HttpErrorResponse({
            status: 403,
          }),
      ),
    );

    const fixture = TestBed.createComponent(EmployeeReactivate);

    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;

    const reactivateButton = element.querySelector(
      'button.button--primary',
    ) as HTMLButtonElement | null;

    reactivateButton?.click();
    fixture.detectChanges();

    expect(element.textContent).toContain(
      'You do not have permission to reactivate this Employee.',
    );

    expect(navigateMock).not.toHaveBeenCalled();
  });

  it('should show a fallback message for an unexpected failure', () => {
    reactivateEmployeeMock.mockReturnValue(
      throwError(() => new Error('Employee Service unavailable')),
    );

    const fixture = TestBed.createComponent(EmployeeReactivate);

    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;

    const reactivateButton = element.querySelector(
      'button.button--primary',
    ) as HTMLButtonElement | null;

    reactivateButton?.click();
    fixture.detectChanges();

    expect(element.textContent).toContain(
      'The Employee could not be reactivated. Please try again.',
    );

    expect(navigateMock).not.toHaveBeenCalled();
  });
});
