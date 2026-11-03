import {HttpErrorResponse} from '@angular/common/http';
import {signal} from '@angular/core';
import {TestBed} from '@angular/core/testing';
import {provideRouter} from '@angular/router';
import {of, Subject, throwError} from 'rxjs';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';
import {AuthSession} from '../../../../core/auth/auth-session';
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

  const individualRegistrationSelector = 'a[href="/customers/individuals/register"]';
  const legalEntityRegistrationSelector = 'a[href="/customers/legal-entities/register"]';

  const realmRoles = signal<readonly string[]>(['CLERK']);
  const searchCustomersMock = vi.fn();
  const authSessionMock = {
    realmRoles: realmRoles.asReadonly(),
    hasRealmRole: vi.fn((role: string) => realmRoles().includes(role)),
  };

  beforeEach(async () => {
    vi.useFakeTimers();
    vi.clearAllMocks();
    realmRoles.set(['CLERK']);
    searchCustomersMock.mockReturnValue(of(page));

    await TestBed.configureTestingModule({
      imports: [CustomerSearch],
      providers: [
        provideRouter([]),
        {
          provide: CustomerAPIService,
          useValue: {searchCustomers: searchCustomersMock},
        },
        {
          provide: AuthSession,
          useValue: authSessionMock,
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
    expect(element.textContent).toContain('Starter Financial Services');
  });

  it.each([
    ['CLERK'],
    ['ADMIN'],
  ])('should show both registration links for the %s role', async (role) => {
    // Arrange
    realmRoles.set([role]);
    const fixture = TestBed.createComponent(CustomerSearch);

    // Act
    fixture.detectChanges();
    await vi.advanceTimersByTimeAsync(300);
    fixture.detectChanges();
    const element = fixture.nativeElement as HTMLElement;

    // Assert
    expect(element.querySelector(individualRegistrationSelector)).not.toBeNull();
    expect(element.querySelector(legalEntityRegistrationSelector)).not.toBeNull();
  });

  it('should hide registration links from a loan officer', async () => {
    // Arrange
    realmRoles.set(['LOAN_OFFICER']);
    const fixture = TestBed.createComponent(CustomerSearch);

    // Act
    fixture.detectChanges();
    await vi.advanceTimersByTimeAsync(300);
    fixture.detectChanges();
    const element = fixture.nativeElement as HTMLElement;

    // Assert
    expect(element.querySelector(individualRegistrationSelector)).toBeNull();
    expect(element.querySelector(legalEntityRegistrationSelector)).toBeNull();
  });

  it('should link each Customer to the details page for its type', async () => {
    // Arrange
    const fixture = TestBed.createComponent(CustomerSearch);

    // Act
    fixture.detectChanges();
    await vi.advanceTimersByTimeAsync(300);
    fixture.detectChanges();
    const element = fixture.nativeElement as HTMLElement;
    const links = Array.from(
      element.querySelectorAll<HTMLAnchorElement>('.customer-link'),
    );

    // Assert
    expect(links).toHaveLength(2);
    expect(links[0]?.getAttribute('href')).toBe(
      `/customers/individuals/${individual.id}`,
    );
    expect(links[1]?.getAttribute('href')).toBe(
      `/customers/legal-entities/${legalEntity.id}`,
    );
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
    searchCustomersMock
      .mockReturnValueOnce(olderResponse)
      .mockReturnValueOnce(newerResponse);
    const fixture = TestBed.createComponent(CustomerSearch);
    fixture.detectChanges();
    await vi.advanceTimersByTimeAsync(300);

    // Act
    fixture.componentInstance['queryControl'].setValue('Starter');
    await vi.advanceTimersByTimeAsync(300);
    newerResponse.next({...page, content: [legalEntity]});
    fixture.detectChanges();
    olderResponse.next(page);
    fixture.detectChanges();

    // Assert
    expect(olderResponse.observed).toBe(false);
    expect(fixture.nativeElement.textContent).toContain('Starter Financial Services');
    expect(fixture.nativeElement.textContent).not.toContain('CUS-000001');
  });

  it('should show a loading state while the request is pending', async () => {
    // Arrange
    searchCustomersMock.mockReturnValue(
      new Subject<PagedResponseCustomerSummaryDto>(),
    );
    const fixture = TestBed.createComponent(CustomerSearch);

    // Act
    fixture.detectChanges();
    await vi.advanceTimersByTimeAsync(300);
    fixture.detectChanges();

    // Assert
    expect(fixture.nativeElement.textContent).toContain('Loading customers');
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
    expect(fixture.nativeElement.textContent).toContain('No customers found');
  });

  it('should show a permission message for a forbidden response', async () => {
    // Arrange
    searchCustomersMock.mockReturnValue(
      throwError(() => new HttpErrorResponse({status: 403})),
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

  it('should retry the current search after an error', async () => {
    // Arrange
    searchCustomersMock
      .mockReturnValueOnce(throwError(() => new Error('Unavailable')))
      .mockReturnValueOnce(of(page));
    const fixture = TestBed.createComponent(CustomerSearch);
    fixture.detectChanges();
    await vi.advanceTimersByTimeAsync(300);
    fixture.detectChanges();

    // Act
    const button = fixture.nativeElement.querySelector(
      '.state-panel .button--primary',
    ) as HTMLButtonElement;
    button.click();
    fixture.detectChanges();

    // Assert
    expect(searchCustomersMock).toHaveBeenCalledTimes(2);
    expect(fixture.nativeElement.textContent).toContain('CUS-000001');
  });

  it('should request the next page with the current query', async () => {
    // Arrange
    searchCustomersMock
      .mockReturnValueOnce(of({...page, totalPages: 2, last: false}))
      .mockReturnValueOnce(of({...page, page: 1, totalPages: 2, last: true}));
    const fixture = TestBed.createComponent(CustomerSearch);
    fixture.componentInstance['queryControl'].setValue('Starter');
    fixture.detectChanges();
    await vi.advanceTimersByTimeAsync(300);
    fixture.detectChanges();

    // Act
    const buttons = fixture.nativeElement.querySelectorAll(
      '.pagination__controls button',
    ) as NodeListOf<HTMLButtonElement>;
    buttons[1]?.click();
    fixture.detectChanges();

    // Assert
    expect(searchCustomersMock).toHaveBeenLastCalledWith({
      query: 'Starter',
      page: 1,
      size: 10,
      sort: ['customerNumber,asc'],
    });
  });

  it('should clear the current query and reload page zero', async () => {
    // Arrange
    const fixture = TestBed.createComponent(CustomerSearch);
    fixture.componentInstance['queryControl'].setValue('Starter');
    fixture.detectChanges();
    await vi.advanceTimersByTimeAsync(300);
    searchCustomersMock.mockClear();

    // Act
    const button = fixture.nativeElement.querySelector(
      '.search-panel__controls button',
    ) as HTMLButtonElement;
    button.click();
    await vi.advanceTimersByTimeAsync(300);

    // Assert
    expect(searchCustomersMock).toHaveBeenCalledWith({
      page: 0,
      size: 10,
      sort: ['customerNumber,asc'],
    });
  });
});
