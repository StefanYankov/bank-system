import {HttpErrorResponse} from '@angular/common/http';
import {TestBed} from '@angular/core/testing';
import {ReactiveFormsModule} from '@angular/forms';
import {of, Subject, throwError} from 'rxjs';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';
import {provideRouter} from '@angular/router';

import {
  CustomerAPIService,
  CustomerSummaryDto,
  PagedResponseCustomerSummaryDto,
} from '../../../../generated/customer-api';
import {CustomerSearch} from './customer-search';

describe('CustomerSearch', () => {
  const individual: CustomerSummaryDto = {
    id: '0199e65a-35b1-7000-8000-000000000101',
    customerNumber: 'CUS-000001',
    customerType: CustomerSummaryDto.CustomerTypeEnum.Individual,
    displayName: 'Serafim Ivanov Gerasimoff',
    identifier: '6101047500',
    status: CustomerSummaryDto.StatusEnum.Active,
  };
  const legalEntity: CustomerSummaryDto = {
    id: '0199e65a-35b1-7000-8000-000000000102',
    customerNumber: 'CUS-000002',
    customerType: CustomerSummaryDto.CustomerTypeEnum.LegalEntity,
    displayName: 'Starter Financial Services',
    identifier: '123456786',
    status: CustomerSummaryDto.StatusEnum.Closed,
  };
  const page: PagedResponseCustomerSummaryDto = {
    content: [individual, legalEntity],
    page: 0,
    size: 10,
    totalElements: 2,
    totalPages: 1,
    last: true,
  };
  const searchCustomersMock = vi.fn();

  beforeEach(async () => {
    vi.useFakeTimers();
    vi.clearAllMocks();
    searchCustomersMock.mockReturnValue(of(page));

    await TestBed.configureTestingModule({
      imports: [
        CustomerSearch,
        ReactiveFormsModule,
      ],
      providers: [
        provideRouter([]),
        {
          provide: CustomerAPIService,
          useValue: {
            searchCustomers: searchCustomersMock,
          },
        },
      ],
    }).compileComponents();
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('should request and render the initial Customer page', async () => {
    // Arrange
    const fixture = TestBed.createComponent(CustomerSearch);

    // Act
    fixture.detectChanges();
    await vi.advanceTimersByTimeAsync(300);
    fixture.detectChanges();

    // Assert
    expect(searchCustomersMock).toHaveBeenCalledWith({
      page: 0,
      size: 10,
      sort: ['customerNumber,asc'],
    });
    const element = fixture.nativeElement as HTMLElement;
    expect(element.textContent).toContain('CUS-000001');
    expect(element.textContent).toContain('Serafim Ivanov Gerasimoff');
    expect(element.textContent).toContain('Individual');
    expect(element.textContent).toContain('EGN');
    expect(element.textContent).toContain('Starter Financial Services');
    expect(element.textContent).toContain('Legal entity');
    expect(element.textContent).toContain('EIK');
    expect(element.textContent).toContain('CLOSED');
  });

  it('should link only Individual Customers to their details page', async () => {
    // Arrange
    const fixture = TestBed.createComponent(CustomerSearch);

    // Act
    fixture.detectChanges();
    await vi.advanceTimersByTimeAsync(300);
    fixture.detectChanges();
    const element = fixture.nativeElement as HTMLElement;
    const customerLinks = Array.from(
      element.querySelectorAll<HTMLAnchorElement>('.customer-link'),
    );

    // Assert
    expect(customerLinks).toHaveLength(1);
    expect(customerLinks[0]?.textContent?.trim()).toBe('CUS-000001');
    expect(customerLinks[0]?.getAttribute('href')).toBe(
      `/customers/individuals/${individual.id}`,
    );
    expect(element.textContent).toContain('CUS-000002');
  });

  it('should debounce a trimmed query and reset to the first page', async () => {
    // Arrange
    const fixture = TestBed.createComponent(CustomerSearch);
    fixture.detectChanges();
    await vi.advanceTimersByTimeAsync(300);
    searchCustomersMock.mockClear();

    // Act
    fixture.componentInstance['queryControl'].setValue('  Starter  ');
    await vi.advanceTimersByTimeAsync(299);

    // Assert
    expect(searchCustomersMock).not.toHaveBeenCalled();

    // Act
    await vi.advanceTimersByTimeAsync(1);

    // Assert
    expect(searchCustomersMock).toHaveBeenCalledWith({
      query: 'Starter',
      page: 0,
      size: 10,
      sort: ['customerNumber,asc'],
    });
  });

  it('should ignore an older response after a newer query starts', async () => {
    // Arrange
    const olderResponse = new Subject<PagedResponseCustomerSummaryDto>();
    const newerResponse = new Subject<PagedResponseCustomerSummaryDto>();
    const newerCustomer: CustomerSummaryDto = {
      ...legalEntity,
      customerNumber: 'CUS-000010',
      displayName: 'Starter Holdings',
    };
    searchCustomersMock
      .mockReturnValueOnce(olderResponse)
      .mockReturnValueOnce(newerResponse);
    const fixture = TestBed.createComponent(CustomerSearch);
    fixture.detectChanges();
    await vi.advanceTimersByTimeAsync(300);

    // Act
    fixture.componentInstance['queryControl'].setValue('Starter');
    await vi.advanceTimersByTimeAsync(300);
    newerResponse.next({
      content: [newerCustomer],
      page: 0,
      size: 10,
      totalElements: 1,
      totalPages: 1,
      last: true,
    });
    fixture.detectChanges();
    olderResponse.next(page);
    fixture.detectChanges();

    // Assert
    expect(olderResponse.observed).toBe(false);
    const element = fixture.nativeElement as HTMLElement;
    expect(element.textContent).toContain('Starter Holdings');
    expect(element.textContent).not.toContain('CUS-000001');
  });

  it('should show a loading state while the request is pending', async () => {
    // Arrange
    const response = new Subject<PagedResponseCustomerSummaryDto>();
    searchCustomersMock.mockReturnValue(response);
    const fixture = TestBed.createComponent(CustomerSearch);

    // Act
    fixture.detectChanges();
    await vi.advanceTimersByTimeAsync(300);
    fixture.detectChanges();

    // Assert
    const element = fixture.nativeElement as HTMLElement;
    expect(element.textContent).toContain('Loading customers');
    expect(element.querySelector('table')).toBeNull();
  });

  it('should show an empty state for a query with no matches', async () => {
    // Arrange
    searchCustomersMock.mockReturnValue(of({
      content: [],
      page: 0,
      size: 10,
      totalElements: 0,
      totalPages: 0,
      last: true,
    }));
    const fixture = TestBed.createComponent(CustomerSearch);
    fixture.componentInstance['queryControl'].setValue('Unknown');

    // Act
    fixture.detectChanges();
    await vi.advanceTimersByTimeAsync(300);
    fixture.detectChanges();

    // Assert
    const element = fixture.nativeElement as HTMLElement;
    expect(element.textContent).toContain('No customers found');
    expect(element.textContent).toContain('Try a different customer number');
  });

  it('should show a permission message for a forbidden response', async () => {
    // Arrange
    searchCustomersMock.mockReturnValue(
      throwError(() => new HttpErrorResponse({
        status: 403,
      })),
    );
    const fixture = TestBed.createComponent(CustomerSearch);

    // Act
    fixture.detectChanges();
    await vi.advanceTimersByTimeAsync(300);
    fixture.detectChanges();

    // Assert
    expect(fixture.nativeElement.textContent).toContain(
      'You do not have permission to view customers.',
    );
  });

  it('should show a fallback error and retry the current search', async () => {
    // Arrange
    searchCustomersMock
      .mockReturnValueOnce(
        throwError(() => new Error('Customer Service unavailable')),
      )
      .mockReturnValueOnce(of(page));
    const fixture = TestBed.createComponent(CustomerSearch);
    fixture.detectChanges();
    await vi.advanceTimersByTimeAsync(300);
    fixture.detectChanges();
    const element = fixture.nativeElement as HTMLElement;

    // Act
    const retryButton = element.querySelector(
      '.button--primary',
    ) as HTMLButtonElement | null;
    retryButton?.click();
    fixture.detectChanges();

    // Assert
    expect(searchCustomersMock).toHaveBeenCalledTimes(2);
    expect(element.textContent).toContain('CUS-000001');
  });

  it('should request the next page with the current query', async () => {
    // Arrange
    const firstPage: PagedResponseCustomerSummaryDto = {
      ...page,
      totalElements: 3,
      totalPages: 2,
      last: false,
    };
    const secondPage: PagedResponseCustomerSummaryDto = {
      ...page,
      page: 1,
      totalElements: 3,
      totalPages: 2,
      last: true,
    };
    searchCustomersMock
      .mockReturnValueOnce(of(firstPage))
      .mockReturnValueOnce(of(secondPage));
    const fixture = TestBed.createComponent(CustomerSearch);
    fixture.componentInstance['queryControl'].setValue('Starter');
    fixture.detectChanges();
    await vi.advanceTimersByTimeAsync(300);
    fixture.detectChanges();
    const element = fixture.nativeElement as HTMLElement;

    // Act
    const buttons = Array.from(
      element.querySelectorAll<HTMLButtonElement>(
        '.pagination__controls button',
      ),
    );
    buttons[1]?.click();
    fixture.detectChanges();

    // Assert
    expect(searchCustomersMock).toHaveBeenLastCalledWith({
      query: 'Starter',
      page: 1,
      size: 10,
      sort: ['customerNumber,asc'],
    });
    expect(element.textContent).toContain('Page 2 of 2');
  });

  it('should clear the current query and reload the first page', async () => {
    // Arrange
    const fixture = TestBed.createComponent(CustomerSearch);
    fixture.componentInstance['queryControl'].setValue('Starter');
    fixture.detectChanges();
    await vi.advanceTimersByTimeAsync(300);
    searchCustomersMock.mockClear();
    const element = fixture.nativeElement as HTMLElement;

    // Act
    const clearButton = element.querySelector(
      '.search-panel__controls button',
    ) as HTMLButtonElement | null;
    clearButton?.click();
    await vi.advanceTimersByTimeAsync(300);

    // Assert
    expect(searchCustomersMock).toHaveBeenCalledWith({
      page: 0,
      size: 10,
      sort: ['customerNumber,asc'],
    });
  });
});
