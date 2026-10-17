import {HttpErrorResponse} from '@angular/common/http';
import {TestBed} from '@angular/core/testing';
import {ActivatedRoute, convertToParamMap, provideRouter,} from '@angular/router';
import {of, Subject, throwError,} from 'rxjs';
import {beforeEach, describe, expect, it, vi,} from 'vitest';

import {EmployeeAPIService, EmployeeViewDto,} from '../../../../generated/employee-api';
import {EmployeeDetails} from './employee-details';

describe('EmployeeDetails', () => {
  const employeeId = '0199e65a-35b1-7000-8000-000000000001';

  const activeEmployee: EmployeeViewDto = {
    id: employeeId,
    employeeNumber: 'EMP-000001',
    firstName: 'Serafim',
    lastName: 'Gerasimoff',
    workEmail: 'serafim.gerasimoff@bank.test',
    position: EmployeeViewDto.PositionEnum.Clerk,
    status: EmployeeViewDto.StatusEnum.Active,
    employmentStartDate: '2026-10-01',
    employmentEndDate: null,
  };

  const inactiveEmployee: EmployeeViewDto = {
    ...activeEmployee,
    status: EmployeeViewDto.StatusEnum.Inactive,
    employmentEndDate: '2026-10-18',
  };

  const getEmployeeByIdMock = vi.fn();

  beforeEach(async () => {
    vi.clearAllMocks();
    getEmployeeByIdMock.mockReturnValue(
      of(activeEmployee),
    );

    await TestBed.configureTestingModule({
      imports: [EmployeeDetails],
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
          },
        },
      ],
    }).compileComponents();
  });

  it('should request and render the Employee details', () => {
    const fixture = TestBed.createComponent(EmployeeDetails);

    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;

    expect(getEmployeeByIdMock).toHaveBeenCalledOnce();
    expect(getEmployeeByIdMock).toHaveBeenCalledWith({
      id: employeeId,
    });

    expect(element.textContent).toContain('Employee name');
    expect(element.textContent).toContain('Serafim Gerasimoff');
    expect(element.textContent).toContain('Employee number');
    expect(element.textContent).toContain('EMP-000001');
    expect(element.textContent).toContain('serafim.gerasimoff@bank.test');
    expect(element.textContent).toContain('CLERK');
    expect(element.textContent).toContain('ACTIVE');
    expect(element.textContent).toContain('2026-10-01');
    expect(element.textContent).toContain('Not applicable');
  });

  it('should render active Employee actions', () => {
    const fixture = TestBed.createComponent(EmployeeDetails);

    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;

    expectActionLink(element, 'Edit profile', `/admin/employees/${employeeId}/edit`);
    expectActionLink(element, 'Change position', `/admin/employees/${employeeId}/position`);
    expectActionLink(element, 'Disable Employee', `/admin/employees/${employeeId}/disable`);
    expect(findActionLink(element, 'Reactivate Employee')).toBeUndefined();
    expect(element.querySelectorAll('.details-action')).toHaveLength(3);
  });

  it('should render only reactivation for an inactive Employee', () => {
    getEmployeeByIdMock.mockReturnValue(of(inactiveEmployee));

    const fixture = TestBed.createComponent(EmployeeDetails);

    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;

    expect(element.textContent).toContain('INACTIVE');
    expect(element.textContent).toContain('2026-10-18');
    expectActionLink(element, 'Reactivate Employee', `/admin/employees/${employeeId}/reactivate`);
    expect(findActionLink(element, 'Edit profile')).toBeUndefined();
    expect(findActionLink(element, 'Change position')).toBeUndefined();
    expect(findActionLink(element, 'Disable Employee')).toBeUndefined();
    expect(element.querySelectorAll('.details-action')).toHaveLength(1);
  });

  it('should not display the technical Employee identifier', () => {
    const fixture = TestBed.createComponent(EmployeeDetails);

    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;

    expect(element.textContent).not.toContain(employeeId);
    expect(element.textContent).not.toContain('Employee ID');
  });

  it('should show a loading state while the request is pending', () => {
    const response = new Subject<EmployeeViewDto>();
    getEmployeeByIdMock.mockReturnValue(response);

    getEmployeeByIdMock.mockReturnValue(response);
    const fixture = TestBed.createComponent(EmployeeDetails);

    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;

    expect(element.textContent).toContain('Loading Employee');
    expect(element.querySelector('.details-card')).toBeNull();
    expect(element.querySelector('.employee-actions')).toBeNull();
    expect(getEmployeeByIdMock).toHaveBeenCalledOnce();
  });

  it('should show a not-found message for a missing Employee', () => {
    getEmployeeByIdMock.mockReturnValue(
      throwError(() => new HttpErrorResponse({
        status: 404,
      })),
    );

    const fixture = TestBed.createComponent(EmployeeDetails);

    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;

    expect(element.textContent).toContain('Employee unavailable');
    expect(element.textContent).toContain('The requested Employee could not be found.');
    expect(element.querySelector('.employee-actions')).toBeNull();
    expect(getEmployeeByIdMock).toHaveBeenCalledOnce();
  });

  it('should show an authorization message for a forbidden response', () => {
    getEmployeeByIdMock.mockReturnValue(
      throwError(() => new HttpErrorResponse({
        status: 403,
      })),
    );

    const fixture = TestBed.createComponent(EmployeeDetails);

    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;
    expect(element.textContent).toContain('You do not have permission to view this Employee.');
    expect(element.querySelector('.employee-actions')).toBeNull();
    expect(getEmployeeByIdMock).toHaveBeenCalledOnce();
  });

  it('should retry loading the Employee', () => {
    getEmployeeByIdMock
      .mockReturnValueOnce(
        throwError(() =>
          new Error('Employee Service unavailable'),
        ),
      )
      .mockReturnValueOnce(of(activeEmployee));

    const fixture =
      TestBed.createComponent(EmployeeDetails);

    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;

    expect(getEmployeeByIdMock).toHaveBeenCalledOnce();
    expect(element.textContent).toContain('The Employee could not be loaded. Please try again.');

    const retryButton = element.querySelector(
      '.button--primary',
    ) as HTMLButtonElement | null;

    expect(retryButton).not.toBeNull();

    retryButton?.click();
    fixture.detectChanges();

    expect(
      getEmployeeByIdMock,
    ).toHaveBeenCalledTimes(2);

    expect(element.textContent).toContain(
      'Serafim Gerasimoff',
    );

    expect(element.querySelectorAll('.details-action')).toHaveLength(3);
    expect(element.querySelector('.state-panel--error')).toBeNull();
  });

  /**
   * Finds an Employee action by its visible label.
   *
   * @param element rendered component root
   * @param label visible action label
   * @returns matching action link, if present
   */
  function findActionLink(
    element: HTMLElement,
    label: string,
  ): HTMLAnchorElement | undefined {
    return Array.from(
      element.querySelectorAll<HTMLAnchorElement>(
        '.details-action',
      ),
    ).find(
      (link) =>
        link.textContent?.trim() === label,
    );
  }

  /**
   * Verifies one Employee action and its destination.
   *
   * @param element rendered component root
   * @param label visible action label
   * @param expectedHref expected Angular route
   */
  function expectActionLink(
    element: HTMLElement,
    label: string,
    expectedHref: string,
  ): void {
    const actionLink = findActionLink(
      element,
      label,
    );

    expect(actionLink).toBeDefined();

    expect(
      actionLink?.getAttribute('href'),
    ).toBe(expectedHref);
  }
});
