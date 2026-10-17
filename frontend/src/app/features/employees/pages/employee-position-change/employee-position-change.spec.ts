import {HttpErrorResponse} from '@angular/common/http';
import {TestBed} from '@angular/core/testing';
import {ActivatedRoute, convertToParamMap, provideRouter, Router,} from '@angular/router';
import {of, Subject, throwError,} from 'rxjs';
import {beforeEach, describe, expect, it, vi,} from 'vitest';

import {EmployeeAPIService, EmployeePositionChangeDto, EmployeeViewDto} from '../../../../generated/employee-api';
import {EmployeePositionChange} from './employee-position-change';

describe('EmployeePositionChange', () => {
  const employeeId = '0199e65a-35b1-7000-8000-000000000002';

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
    position: EmployeeViewDto.PositionEnum.Administrator,
  };

  const getEmployeeByIdMock = vi.fn();
  const changeEmployeePositionMock = vi.fn();

  const navigateMock = vi.fn<
    (commands: readonly unknown[]) => Promise<boolean>
  >();

  beforeEach(async () => {
    getEmployeeByIdMock.mockReset();
    changeEmployeePositionMock.mockReset();
    navigateMock.mockReset();

    getEmployeeByIdMock.mockReturnValue(
      of(employee),
    );

    changeEmployeePositionMock.mockReturnValue(
      of(updatedEmployee),
    );

    navigateMock.mockResolvedValue(true);

    await TestBed.configureTestingModule({
      imports: [EmployeePositionChange],
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
            changeEmployeePosition:
            changeEmployeePositionMock,
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

  it('should load the Employee and select the current position', () => {
    const fixture =
      TestBed.createComponent(EmployeePositionChange);

    fixture.detectChanges();

    const form =
      fixture.componentInstance['positionForm'];

    expect(getEmployeeByIdMock).toHaveBeenCalledOnce();

    expect(getEmployeeByIdMock).toHaveBeenCalledWith({
      id: employeeId,
    });

    expect(form.controls.position.value).toBe(
      EmployeePositionChangeDto.PositionEnum.LoanOfficer,
    );
  });

  it('should render the Employee and current position', () => {
    const fixture =
      TestBed.createComponent(EmployeePositionChange);

    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;

    expect(element.textContent).toContain(
      'Change Employee position',
    );

    expect(element.textContent).toContain(
      'Jane Doe',
    );

    expect(element.textContent).toContain(
      'LOAN_OFFICER',
    );
  });

  it('should submit the selected Employee position', () => {
    const fixture =
      TestBed.createComponent(EmployeePositionChange);

    fixture.detectChanges();

    const form =
      fixture.componentInstance['positionForm'];

    form.controls.position.setValue(
      EmployeePositionChangeDto.PositionEnum.Administrator,
    );

    const element = fixture.nativeElement as HTMLElement;
    const formElement = element.querySelector('form');

    formElement?.dispatchEvent(
      new Event('submit'),
    );

    expect(
      changeEmployeePositionMock,
    ).toHaveBeenCalledWith({
      id: employeeId,
      employeePositionChangeDto: {
        position:
        EmployeePositionChangeDto.PositionEnum.Administrator,
      },
    });
  });

  it('should navigate to the updated Employee details page', () => {
    const fixture =
      TestBed.createComponent(EmployeePositionChange);

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

  it('should disable submission while the request is pending', () => {
    const response =
      new Subject<EmployeeViewDto>();

    changeEmployeePositionMock.mockReturnValue(
      response,
    );

    const fixture =
      TestBed.createComponent(EmployeePositionChange);

    fixture.detectChanges();

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
    expect(submitButton?.textContent).toContain('Saving');
    expect(navigateMock).not.toHaveBeenCalled();
  });

  it('should show a loading state while the Employee request is pending', () => {
    const response =
      new Subject<EmployeeViewDto>();

    getEmployeeByIdMock.mockReturnValue(
      response,
    );

    const fixture =
      TestBed.createComponent(EmployeePositionChange);

    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;

    expect(element.textContent).toContain(
      'Loading Employee',
    );

    expect(
      element.querySelector('.position-form'),
    ).toBeNull();
  });

  it('should show a not-found message when the Employee cannot be loaded', () => {
    getEmployeeByIdMock.mockReturnValue(
      throwError(() => new HttpErrorResponse({
        status: 404,
      })),
    );

    const fixture =
      TestBed.createComponent(EmployeePositionChange);

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
      TestBed.createComponent(EmployeePositionChange);

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
      'Change Employee position',
    );
  });

  it('should show a forbidden message when the update is not permitted', () => {
    changeEmployeePositionMock.mockReturnValue(
      throwError(() => new HttpErrorResponse({
        status: 403,
      })),
    );

    const fixture =
      TestBed.createComponent(EmployeePositionChange);

    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;
    const formElement = element.querySelector('form');

    formElement?.dispatchEvent(
      new Event('submit'),
    );

    fixture.detectChanges();

    expect(element.textContent).toContain(
      'You do not have permission to change this Employee position.',
    );

    expect(navigateMock).not.toHaveBeenCalled();
  });

  it('should show a fallback message for an unexpected update failure', () => {
    changeEmployeePositionMock.mockReturnValue(
      throwError(() =>
        new Error('Employee Service unavailable'),
      ),
    );

    const fixture =
      TestBed.createComponent(EmployeePositionChange);

    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;
    const formElement = element.querySelector('form');

    formElement?.dispatchEvent(
      new Event('submit'),
    );

    fixture.detectChanges();

    expect(element.textContent).toContain(
      'The Employee position could not be changed. Please try again.',
    );

    expect(navigateMock).not.toHaveBeenCalled();
  });
});
