import {HttpErrorResponse} from '@angular/common/http';
import {TestBed} from '@angular/core/testing';
import {of, throwError,} from 'rxjs';
import {beforeEach, describe, expect, it, vi,} from 'vitest';

import {EmployeeAPIService, EmployeeViewDto,} from '../../../../generated/employee-api';
import {CurrentEmployee} from './current-employee';

describe('CurrentEmployee', () => {
  const getCurrentEmployeeMock = vi.fn();

  const employee: EmployeeViewDto = {
    id: '01923b7c-8f6d-7a12-9d1e-4b2f8c3e5a10',
    employeeNumber: 'EMP-000001',
    firstName: 'Serafim',
    lastName: 'Gerasimoff',
    workEmail: 'serafim.gerasimoff@example.com',
    position: 'CLERK',
    status: 'ACTIVE',
    employmentStartDate: '2026-10-01',
    employmentEndDate: null,
  };

  beforeEach(async () => {
    vi.clearAllMocks();

    await TestBed.configureTestingModule({
      imports: [CurrentEmployee],
      providers: [
        {
          provide: EmployeeAPIService,
          useValue: {
            getCurrentEmployee:
            getCurrentEmployeeMock,
          },
        },
      ],
    }).compileComponents();
  });

  it('should display the current employee', () => {
    // Arrange
    getCurrentEmployeeMock.mockReturnValue(
      of(employee),
    );

    const fixture =
      TestBed.createComponent(CurrentEmployee);

    // Act
    fixture.detectChanges();

    // Assert
    const element =
      fixture.nativeElement as HTMLElement;

    expect(
      getCurrentEmployeeMock,
    ).toHaveBeenCalledOnce();

    expect(element.textContent)
      .toContain('EMP-000001');

    expect(element.textContent)
      .toContain('Serafim Gerasimoff');

    expect(element.textContent)
      .toContain(
        'serafim.gerasimoff@example.com',
      );

    expect(element.textContent)
      .toContain('Not applicable');
  });

  it('should explain when no employee profile is linked', () => {
    // Arrange
    getCurrentEmployeeMock.mockReturnValue(
      throwError(
        () =>
          new HttpErrorResponse({
            status: 404,
            statusText: 'Not Found',
          }),
      ),
    );

    const fixture =
      TestBed.createComponent(CurrentEmployee);

    // Act
    fixture.detectChanges();

    // Assert
    const element =
      fixture.nativeElement as HTMLElement;

    expect(
      element.querySelector('[role="alert"]')
        ?.textContent,
    ).toContain(
      'No employee profile is linked to this Keycloak account.',
    );
  });

  it('should explain when Employee Service is unavailable', () => {
    // Arrange
    getCurrentEmployeeMock.mockReturnValue(
      throwError(
        () =>
          new HttpErrorResponse({
            status: 0,
            statusText: 'Unknown Error',
          }),
      ),
    );

    const fixture =
      TestBed.createComponent(CurrentEmployee);

    // Act
    fixture.detectChanges();

    // Assert
    const element =
      fixture.nativeElement as HTMLElement;

    expect(
      element.querySelector('[role="alert"]')
        ?.textContent,
    ).toContain(
      'Employee Service is currently unavailable.',
    );
  });
});
