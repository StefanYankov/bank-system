import {TestBed} from '@angular/core/testing';
import {of, Subject, throwError} from 'rxjs';
import {provideRouter} from '@angular/router';
import {beforeEach, describe, expect, it, vi,} from 'vitest';
import {EmployeeAPIService, EmployeeViewDto, PagedResponseEmployeeViewDto,} from '../../../../generated/employee-api';
import {EmployeeList} from './employee-list';

describe('EmployeeList', () => {
  const employee: EmployeeViewDto = {
    id: '0199e65a-35b1-7000-8000-000000000001',
    employeeNumber: 'EMP-000001',
    firstName: 'Serafim',
    lastName: 'Gerasimoff',
    workEmail: 'serafim.gerasimoff@bank.test',
    position: EmployeeViewDto.PositionEnum.Clerk,
    status: EmployeeViewDto.StatusEnum.Active,
    employmentStartDate: '2026-10-01',
    employmentEndDate: null,
  };

  const page: PagedResponseEmployeeViewDto = {
    content: [employee],
    page: 0,
    size: 10,
    totalElements: 1,
    totalPages: 1,
    last: true,
  };

  const getEmployeesMock = vi.fn();

  beforeEach(async () => {
    vi.clearAllMocks();
    getEmployeesMock.mockReturnValue(of(page));

    await TestBed.configureTestingModule({
      imports: [EmployeeList],
      providers: [
        provideRouter([]),
        {
          provide: EmployeeAPIService,
          useValue: {
            getEmployees: getEmployeesMock,
          },
        },
      ],
    }).compileComponents();
  });

  it('should request and render the first Employee page', () => {
    const fixture = TestBed.createComponent(EmployeeList);

    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;

    expect(getEmployeesMock).toHaveBeenCalledWith({
      page: 0,
      size: 10,
      sort: ['employeeNumber,asc'],
    });

    expect(element.textContent).toContain('EMP-000001');
    expect(element.textContent).toContain('Serafim Gerasimoff');
    expect(element.textContent).toContain(
      'serafim.gerasimoff@bank.test',
    );
    expect(element.textContent).toContain('CLERK');
    expect(element.textContent).toContain('ACTIVE');
  });

  it('should show a loading state while the request is pending', () => {
    const response = new Subject<PagedResponseEmployeeViewDto>();
    getEmployeesMock.mockReturnValue(response);

    const fixture = TestBed.createComponent(EmployeeList);

    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;

    expect(element.textContent).toContain('Loading employees');
    expect(element.querySelector('table')).toBeNull();
  });

  it('should show an empty state when no employees exist', () => {
    getEmployeesMock.mockReturnValue(of({
      content: [],
      page: 0,
      size: 10,
      totalElements: 0,
      totalPages: 0,
      last: true,
    }));

    const fixture = TestBed.createComponent(EmployeeList);

    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;

    expect(element.textContent).toContain('No employees found');
    expect(element.querySelector('table')).toBeNull();
  });

  it('should show an error and retry the current page', () => {
    getEmployeesMock.mockReturnValueOnce(
      throwError(() => new Error('Employee Service unavailable')),
    );
    getEmployeesMock.mockReturnValueOnce(of(page));

    const fixture = TestBed.createComponent(EmployeeList);

    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;

    expect(element.textContent).toContain(
      'Employees could not be loaded',
    );

    const retryButton = element.querySelector(
      '.button--primary',
    ) as HTMLButtonElement | null;

    retryButton?.click();
    fixture.detectChanges();

    expect(getEmployeesMock).toHaveBeenCalledTimes(2);
    expect(element.textContent).toContain('Serafim Gerasimoff');
  });

  it('should request the next page', () => {
    const firstPage: PagedResponseEmployeeViewDto = {
      ...page,
      totalElements: 2,
      totalPages: 2,
      last: false,
    };

    const secondPage: PagedResponseEmployeeViewDto = {
      ...page,
      page: 1,
      totalElements: 2,
      totalPages: 2,
      last: true,
    };

    getEmployeesMock
      .mockReturnValueOnce(of(firstPage))
      .mockReturnValueOnce(of(secondPage));

    const fixture = TestBed.createComponent(EmployeeList);

    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;
    const buttons = Array.from(
      element.querySelectorAll<HTMLButtonElement>(
        '.pagination__controls button',
      ),
    );

    buttons[1]?.click();
    fixture.detectChanges();

    expect(getEmployeesMock).toHaveBeenLastCalledWith({
      page: 1,
      size: 10,
      sort: ['employeeNumber,asc'],
    });

    expect(element.textContent).toContain('Page 2 of 2');
  });
});
